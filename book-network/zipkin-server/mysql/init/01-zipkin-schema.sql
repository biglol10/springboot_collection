-- =============================================================================
-- ZIPKIN 데이터베이스 스키마 초기화
-- =============================================================================
-- Zipkin의 MySQL 저장소를 위한 테이블 스키마
-- OpenZipkin 공식 스키마 기반

USE zipkin;

-- =============================================================================
-- Zipkin Spans 테이블
-- 분산 추적의 핵심 데이터를 저장하는 메인 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS zipkin_spans (
  `trace_id_high` BIGINT NOT NULL DEFAULT 0 COMMENT '추적 ID 상위 64비트',
  `trace_id` BIGINT NOT NULL COMMENT '추적 ID 하위 64비트',
  `id` BIGINT NOT NULL COMMENT '스팬 ID',
  `name` VARCHAR(255) NOT NULL COMMENT '스팬 이름 (operation name)',
  `remote_service_name` VARCHAR(255) COMMENT '원격 서비스 이름',
  `parent_id` BIGINT COMMENT '부모 스팬 ID',
  `debug` BIT(1) COMMENT '디버그 플래그',
  `start_ts` BIGINT COMMENT '시작 타임스탬프 (마이크로초)',
  `duration` BIGINT COMMENT '지속 시간 (마이크로초)'
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED CHARACTER SET=utf8 COLLATE utf8_general_ci;

-- 스팬 테이블 인덱스 (성능 최적화)
ALTER TABLE zipkin_spans ADD PRIMARY KEY(`trace_id_high`, `trace_id`, `id`);
ALTER TABLE zipkin_spans ADD INDEX(`trace_id_high`, `trace_id`) COMMENT '추적 ID 인덱스';
ALTER TABLE zipkin_spans ADD INDEX(`name`) COMMENT '스팬 이름 인덱스';
ALTER TABLE zipkin_spans ADD INDEX(`remote_service_name`) COMMENT '원격 서비스 인덱스';
ALTER TABLE zipkin_spans ADD INDEX(`start_ts`) COMMENT '시작 시간 인덱스';

-- =============================================================================
-- Zipkin Annotations 테이블
-- 스팬의 이벤트 및 타임스탬프 정보 저장
-- =============================================================================
CREATE TABLE IF NOT EXISTS zipkin_annotations (
  `trace_id_high` BIGINT NOT NULL DEFAULT 0 COMMENT '추적 ID 상위 64비트',
  `trace_id` BIGINT NOT NULL COMMENT '추적 ID 하위 64비트',
  `span_id` BIGINT NOT NULL COMMENT '스팬 ID',
  `a_key` VARCHAR(255) NOT NULL COMMENT '어노테이션 키',
  `a_value` BLOB COMMENT '어노테이션 값',
  `a_type` INT NOT NULL COMMENT '어노테이션 타입',
  `a_timestamp` BIGINT COMMENT '어노테이션 타임스탬프',
  `endpoint_ipv4` INT COMMENT '엔드포인트 IPv4',
  `endpoint_ipv6` BINARY(16) COMMENT '엔드포인트 IPv6',
  `endpoint_port` SMALLINT COMMENT '엔드포인트 포트',
  `endpoint_service_name` VARCHAR(255) COMMENT '엔드포인트 서비스명'
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED CHARACTER SET=utf8 COLLATE utf8_general_ci;

-- 어노테이션 테이블 인덱스
ALTER TABLE zipkin_annotations ADD UNIQUE KEY(`trace_id_high`, `trace_id`, `span_id`, `a_key`, `a_timestamp`) COMMENT '고유 인덱스';
ALTER TABLE zipkin_annotations ADD INDEX(`trace_id_high`, `trace_id`, `span_id`) COMMENT '스팬 참조 인덱스';
ALTER TABLE zipkin_annotations ADD INDEX(`trace_id_high`, `trace_id`) COMMENT '추적 인덱스';
ALTER TABLE zipkin_annotations ADD INDEX(`endpoint_service_name`) COMMENT '서비스명 인덱스';
ALTER TABLE zipkin_annotations ADD INDEX(`a_type`) COMMENT '어노테이션 타입 인덱스';
ALTER TABLE zipkin_annotations ADD INDEX(`a_key`) COMMENT '어노테이션 키 인덱스';
ALTER TABLE zipkin_annotations ADD INDEX(`trace_id`, `span_id`, `a_key`) COMMENT '복합 인덱스';

-- =============================================================================
-- Zipkin Dependencies 테이블
-- 서비스 간 의존성 관계 저장
-- =============================================================================
CREATE TABLE IF NOT EXISTS zipkin_dependencies (
  `day` DATE NOT NULL COMMENT '날짜',
  `parent` VARCHAR(255) NOT NULL COMMENT '부모 서비스',
  `child` VARCHAR(255) NOT NULL COMMENT '자식 서비스',
  `call_count` BIGINT COMMENT '호출 횟수',
  `error_count` BIGINT COMMENT '오류 횟수',
  PRIMARY KEY (`day`, `parent`, `child`)
) ENGINE=InnoDB ROW_FORMAT=COMPRESSED CHARACTER SET=utf8 COLLATE utf8_general_ci;

-- 의존성 테이블 인덱스
ALTER TABLE zipkin_dependencies ADD INDEX(`day`) COMMENT '날짜 인덱스';
ALTER TABLE zipkin_dependencies ADD INDEX(`parent`) COMMENT '부모 서비스 인덱스';
ALTER TABLE zipkin_dependencies ADD INDEX(`child`) COMMENT '자식 서비스 인덱스';

-- =============================================================================
-- 성능 최적화를 위한 추가 인덱스
-- =============================================================================

-- 트레이스 조회 최적화 (가장 자주 사용되는 쿼리)
ALTER TABLE zipkin_spans ADD INDEX idx_trace_lookup (`trace_id_high`, `trace_id`, `start_ts` DESC);

-- 서비스별 스팬 조회 최적화
ALTER TABLE zipkin_annotations ADD INDEX idx_service_spans (`endpoint_service_name`, `a_timestamp` DESC);

-- 시간 범위 검색 최적화
ALTER TABLE zipkin_spans ADD INDEX idx_time_range (`start_ts`, `name`);

-- 에러 스팬 조회 최적화
ALTER TABLE zipkin_annotations ADD INDEX idx_error_traces (`a_key`, `a_value`(100), `trace_id_high`, `trace_id`);

-- =============================================================================
-- 데이터 보존 정책을 위한 프로시저
-- =============================================================================

DELIMITER //

-- 오래된 추적 데이터 정리 프로시저 (7일 이상 된 데이터 삭제)
CREATE PROCEDURE CleanupOldTraces(IN days_to_keep INT)
BEGIN
    DECLARE cutoff_timestamp BIGINT;
    DECLARE deleted_spans INT DEFAULT 0;
    DECLARE deleted_annotations INT DEFAULT 0;
    
    -- 삭제할 타임스탬프 계산 (마이크로초 단위)
    SET cutoff_timestamp = (UNIX_TIMESTAMP(DATE_SUB(NOW(), INTERVAL days_to_keep DAY)) * 1000000);
    
    -- 트랜잭션 시작
    START TRANSACTION;
    
    -- 오래된 어노테이션 삭제
    DELETE FROM zipkin_annotations 
    WHERE a_timestamp < cutoff_timestamp;
    SET deleted_annotations = ROW_COUNT();
    
    -- 오래된 스팬 삭제
    DELETE FROM zipkin_spans 
    WHERE start_ts < cutoff_timestamp;
    SET deleted_spans = ROW_COUNT();
    
    -- 오래된 의존성 데이터 삭제
    DELETE FROM zipkin_dependencies 
    WHERE day < DATE_SUB(CURDATE(), INTERVAL days_to_keep DAY);
    
    -- 결과 로깅
    INSERT INTO cleanup_log (cleanup_date, deleted_spans, deleted_annotations, cutoff_timestamp)
    VALUES (NOW(), deleted_spans, deleted_annotations, cutoff_timestamp);
    
    COMMIT;
    
    SELECT CONCAT('Deleted ', deleted_spans, ' spans and ', deleted_annotations, ' annotations') AS result;
END //

-- 인덱스 최적화 프로시저
CREATE PROCEDURE OptimizeZipkinTables()
BEGIN
    -- 테이블 분석 및 최적화
    ANALYZE TABLE zipkin_spans;
    ANALYZE TABLE zipkin_annotations;
    ANALYZE TABLE zipkin_dependencies;
    
    OPTIMIZE TABLE zipkin_spans;
    OPTIMIZE TABLE zipkin_annotations;
    OPTIMIZE TABLE zipkin_dependencies;
    
    SELECT 'Zipkin tables optimized successfully' AS result;
END //

DELIMITER ;

-- =============================================================================
-- 정리 로그 테이블
-- =============================================================================
CREATE TABLE IF NOT EXISTS cleanup_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cleanup_date DATETIME NOT NULL,
    deleted_spans INT NOT NULL,
    deleted_annotations INT NOT NULL,
    cutoff_timestamp BIGINT NOT NULL,
    INDEX idx_cleanup_date (cleanup_date)
) ENGINE=InnoDB;

-- =============================================================================
-- 초기 데이터 및 설정
-- =============================================================================

-- 관리용 사용자 생성 (선택사항)
CREATE USER IF NOT EXISTS 'zipkin_readonly'@'%' IDENTIFIED BY 'readonly123';
GRANT SELECT ON zipkin.* TO 'zipkin_readonly'@'%';

-- 성능 모니터링을 위한 뷰
CREATE VIEW v_service_summary AS
SELECT 
    endpoint_service_name as service_name,
    COUNT(DISTINCT CONCAT(trace_id_high, '-', trace_id)) as trace_count,
    COUNT(*) as span_count,
    MIN(a_timestamp) as first_seen,
    MAX(a_timestamp) as last_seen
FROM zipkin_annotations 
WHERE endpoint_service_name IS NOT NULL
GROUP BY endpoint_service_name;

-- 일일 추적 통계 뷰
CREATE VIEW v_daily_trace_stats AS
SELECT 
    DATE(FROM_UNIXTIME(start_ts/1000000)) as trace_date,
    COUNT(DISTINCT CONCAT(trace_id_high, '-', trace_id)) as unique_traces,
    COUNT(*) as total_spans,
    AVG(duration) as avg_duration_us,
    MAX(duration) as max_duration_us,
    MIN(duration) as min_duration_us
FROM zipkin_spans 
WHERE start_ts IS NOT NULL AND duration IS NOT NULL
GROUP BY DATE(FROM_UNIXTIME(start_ts/1000000));

-- =============================================================================
-- 스키마 버전 정보
-- =============================================================================
CREATE TABLE IF NOT EXISTS schema_version (
    version VARCHAR(10) NOT NULL,
    applied_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    description TEXT
);

INSERT INTO schema_version (version, description) 
VALUES ('1.0.0', 'Initial Zipkin schema with performance optimizations');

-- 초기화 완료 메시지
SELECT 'Zipkin MySQL schema initialized successfully!' as status;
SELECT 'Remember to run CleanupOldTraces(7) procedure regularly to maintain performance' as note;