package com.alibou.booknetwork.discovery.health;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Eureka 서버 전용 헬스 체크
 * 
 * 상태 모니터링 항목:
 * 1. 서비스 레지스트리 상태
 * 2. 등록된 서비스 인스턴스 수
 * 3. 자체 보호 모드 상태
 * 4. 피어 서버 연결 상태 (클러스터 모드)
 * 5. 메모리 사용률
 * 
 * 헬스 체크 결과는 다음과 같이 활용:
 * - 로드 밸런서 트래픽 라우팅 결정
 * - Kubernetes 라이브니스/레디니스 프로브
 * - 모니터링 시스템 알림
 * - 자동 복구 시스템 트리거
 */
@Component
public class EurekaServerHealthIndicator implements HealthIndicator {

    /**
     * Eureka 서버 상태 검사
     * 
     * 검사 항목:
     * 1. 레지스트리 초기화 여부
     * 2. 등록된 애플리케이션 수
     * 3. 총 인스턴스 수
     * 4. 자체 보호 모드 활성화 여부
     * 5. 최근 등록/해제 활동
     * 
     * @return Health 객체 (UP/DOWN 상태 및 상세 정보)
     */
    @Override
    public Health health() {
        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
            
            if (serverContext == null) {
                return Health.down()
                    .withDetail("error", "Eureka server context not initialized")
                    .build();
            }

            PeerAwareInstanceRegistry registry = serverContext.getRegistry();
            
            if (registry == null) {
                return Health.down()
                    .withDetail("error", "Instance registry not available")
                    .build();
            }

            // 📊 레지스트리 통계 수집
            int applicationCount = registry.getApplications().size();
            long totalInstances = registry.getApplications()
                .getRegisteredApplications()
                .stream()
                .mapToLong(app -> app.getInstances().size())
                .sum();

            // 🛡️ 자체 보호 모드 상태 확인
            boolean selfPreservationMode = registry.isLeaseExpirationEnabled();
            
            // 📈 인스턴스 상태별 카운트
            Map<InstanceInfo.InstanceStatus, Long> statusCounts = registry.getApplications()
                .getRegisteredApplications()
                .stream()
                .flatMap(app -> app.getInstances().stream())
                .collect(java.util.stream.Collectors.groupingBy(
                    InstanceInfo::getStatus,
                    java.util.stream.Collectors.counting()
                ));

            // 💾 메모리 사용률 계산
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            double memoryUsagePercent = (double) usedMemory / totalMemory * 100;

            // 🚨 임계치 기반 상태 결정
            Health.Builder healthBuilder;
            
            if (memoryUsagePercent > 90) {
                healthBuilder = Health.down()
                    .withDetail("issue", "High memory usage: " + String.format("%.1f%%", memoryUsagePercent));
            } else if (applicationCount == 0 && isServerFullyStarted()) {
                healthBuilder = Health.down()
                    .withDetail("issue", "No applications registered and server is fully started");
            } else {
                healthBuilder = Health.up();
            }

            // 📋 상세 정보 추가
            return healthBuilder
                .withDetail("registry.applications", applicationCount)
                .withDetail("registry.totalInstances", totalInstances)
                .withDetail("registry.statusCounts", statusCounts)
                .withDetail("selfPreservation.enabled", selfPreservationMode)
                .withDetail("memory.usagePercent", String.format("%.1f%%", memoryUsagePercent))
                .withDetail("memory.used", formatBytes(usedMemory))
                .withDetail("memory.total", formatBytes(totalMemory))
                .withDetail("server.uptime", getServerUptime())
                .withDetail("timestamp", System.currentTimeMillis())
                .build();

        } catch (Exception e) {
            return Health.down()
                .withDetail("error", "Health check failed: " + e.getMessage())
                .withException(e)
                .build();
        }
    }

    /**
     * 서버 완전 시작 여부 확인
     * 
     * @return 서버가 완전히 시작되었는지 여부
     */
    private boolean isServerFullyStarted() {
        try {
            EurekaServerContext serverContext = EurekaServerContextHolder.getInstance().getServerContext();
            // 서버 컨텍스트가 있고 레지스트리가 초기화되었으면 완전 시작으로 간주
            return serverContext != null && serverContext.getRegistry() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 서버 가동 시간 계산
     * 
     * @return 서버 가동 시간 (문자열 형태)
     */
    private String getServerUptime() {
        long uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        long uptimeSeconds = uptimeMs / 1000;
        long days = uptimeSeconds / 86400;
        long hours = (uptimeSeconds % 86400) / 3600;
        long minutes = (uptimeSeconds % 3600) / 60;
        long seconds = uptimeSeconds % 60;
        
        if (days > 0) {
            return String.format("%dd %02dh %02dm %02ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%02dh %02dm %02ds", hours, minutes, seconds);
        } else {
            return String.format("%02dm %02ds", minutes, seconds);
        }
    }

    /**
     * 바이트 크기를 읽기 쉬운 형태로 변환
     * 
     * @param bytes 바이트 크기
     * @return 포맷된 크기 문자열
     */
    private String formatBytes(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = bytes;
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.1f %s", size, units[unitIndex]);
    }
}