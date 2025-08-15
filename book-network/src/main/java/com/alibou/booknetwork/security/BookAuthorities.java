package com.alibou.booknetwork.security;

/**
 * Book Network 애플리케이션 권한 상수 정의
 * 
 * SSGD 스타일의 엔터프라이즈급 권한 체계를 Book Network에 적용한 버전입니다.
 * R(Read), CUD(Create/Update/Delete), CRUD 패턴을 따라 권한을 세분화했습니다.
 * 
 * 권한 명명 규칙:
 * - {DOMAIN}_{ACTION} 형태
 * - R: Read (조회)
 * - CUD: Create/Update/Delete (생성/수정/삭제)
 * - CRUD: Create/Read/Update/Delete (모든 권한)
 * 
 * 도메인 분류:
 * - BOOK: 도서 관리
 * - USER: 사용자 관리
 * - FEEDBACK: 피드백 관리
 * - TRANSACTION: 대출/반납 거래
 * - SYSTEM: 시스템 관리
 * - ADMIN: 관리자 권한
 */
public class BookAuthorities {

    // ================================
    // 도서 관리 권한 (BOOK)
    // ================================
    
    /**
     * 도서 조회 권한
     * - 도서 목록 조회
     * - 도서 상세 정보 조회
     * - 도서 검색
     */
    public static final String BOOK_READ = "BOOK_READ";
    
    /**
     * 도서 생성/수정/삭제 권한
     * - 새 도서 등록
     * - 도서 정보 수정
     * - 도서 삭제
     * - 도서 아카이브/복원
     */
    public static final String BOOK_CUD = "BOOK_CUD";
    
    /**
     * 도서 모든 권한 (조회 + 생성/수정/삭제)
     */
    public static final String BOOK_CRUD = "BOOK_CRUD";
    
    /**
     * 도서 공유 설정 권한
     * - 도서 공유 가능 여부 설정
     * - 도서 공개/비공개 설정
     */
    public static final String BOOK_SHARE_MANAGE = "BOOK_SHARE_MANAGE";
    
    /**
     * 도서 파일 관리 권한
     * - 도서 커버 이미지 업로드/삭제
     * - 도서 첨부 파일 관리
     */
    public static final String BOOK_FILE_MANAGE = "BOOK_FILE_MANAGE";

    // ================================
    // 사용자 관리 권한 (USER)
    // ================================
    
    /**
     * 사용자 조회 권한
     * - 사용자 목록 조회
     * - 사용자 프로필 조회
     */
    public static final String USER_READ = "USER_READ";
    
    /**
     * 사용자 관리 권한
     * - 사용자 정보 수정
     * - 사용자 계정 활성화/비활성화
     * - 사용자 역할 변경
     */
    public static final String USER_MANAGE = "USER_MANAGE";
    
    /**
     * 사용자 생성 권한
     * - 새 사용자 계정 생성
     * - 사용자 초기 설정
     */
    public static final String USER_CREATE = "USER_CREATE";
    
    /**
     * 사용자 삭제 권한
     * - 사용자 계정 삭제
     * - 사용자 데이터 완전 삭제
     */
    public static final String USER_DELETE = "USER_DELETE";

    // ================================
    // 대출/반납 거래 권한 (TRANSACTION)
    // ================================
    
    /**
     * 거래 내역 조회 권한
     * - 대출/반납 내역 조회
     * - 거래 통계 조회
     */
    public static final String TRANSACTION_READ = "TRANSACTION_READ";
    
    /**
     * 도서 대출 권한
     * - 도서 대출 요청
     * - 대출 승인/거부
     */
    public static final String TRANSACTION_BORROW = "TRANSACTION_BORROW";
    
    /**
     * 도서 반납 권한
     * - 도서 반납 처리
     * - 반납 승인
     */
    public static final String TRANSACTION_RETURN = "TRANSACTION_RETURN";
    
    /**
     * 거래 관리 권한
     * - 거래 내역 수정
     * - 거래 취소
     * - 연체 관리
     */
    public static final String TRANSACTION_MANAGE = "TRANSACTION_MANAGE";

    // ================================
    // 피드백 관리 권한 (FEEDBACK)
    // ================================
    
    /**
     * 피드백 조회 권한
     * - 피드백 목록 조회
     * - 피드백 상세 조회
     */
    public static final String FEEDBACK_READ = "FEEDBACK_READ";
    
    /**
     * 피드백 작성 권한
     * - 새 피드백 작성
     * - 피드백 수정 (자신의 피드백만)
     */
    public static final String FEEDBACK_WRITE = "FEEDBACK_WRITE";
    
    /**
     * 피드백 관리 권한
     * - 모든 피드백 수정/삭제
     * - 피드백 숨김/표시 처리
     * - 부적절한 피드백 관리
     */
    public static final String FEEDBACK_MANAGE = "FEEDBACK_MANAGE";

    // ================================
    // 시스템 관리 권한 (SYSTEM)
    // ================================
    
    /**
     * 시스템 설정 조회 권한
     * - 시스템 설정 조회
     * - 시스템 상태 모니터링
     */
    public static final String SYSTEM_READ = "SYSTEM_READ";
    
    /**
     * 시스템 설정 관리 권한
     * - 시스템 설정 변경
     * - 캐시 관리
     * - 배치 작업 관리
     */
    public static final String SYSTEM_MANAGE = "SYSTEM_MANAGE";
    
    /**
     * 시스템 백업/복원 권한
     * - 데이터 백업
     * - 데이터 복원
     * - 시스템 마이그레이션
     */
    public static final String SYSTEM_BACKUP = "SYSTEM_BACKUP";
    
    /**
     * 로그 조회 권한
     * - 시스템 로그 조회
     * - 에러 로그 분석
     * - 감사 로그 조회
     */
    public static final String SYSTEM_LOG_READ = "SYSTEM_LOG_READ";

    // ================================
    // 관리자 권한 (ADMIN)
    // ================================
    
    /**
     * 일반 관리자 권한
     * - 기본적인 관리 작업
     * - 사용자 문의 응대
     */
    public static final String ADMIN_BASIC = "ADMIN_BASIC";
    
    /**
     * 고급 관리자 권한
     * - 시스템 설정 변경
     * - 중요 데이터 관리
     */
    public static final String ADMIN_ADVANCED = "ADMIN_ADVANCED";
    
    /**
     * 최고 관리자 권한
     * - 모든 시스템 접근
     * - 모든 데이터 관리
     * - 시스템 중단/재시작
     */
    public static final String ADMIN_SUPER = "ADMIN_SUPER";
    
    /**
     * 모든 권한
     * - 시스템의 모든 기능에 접근 가능
     * - 개발/테스트 환경에서만 사용 권장
     */
    public static final String ADMIN_ALL = "ADMIN_ALL";

    // ================================
    // 특수 권한 (SPECIAL)
    // ================================
    
    /**
     * API 접근 권한
     * - REST API 호출 권한
     * - 외부 시스템 연동
     */
    public static final String API_ACCESS = "API_ACCESS";
    
    /**
     * 대량 처리 권한
     * - 대량 데이터 처리
     * - 배치 작업 실행
     */
    public static final String BULK_OPERATION = "BULK_OPERATION";
    
    /**
     * 통계 조회 권한
     * - 시스템 통계 조회
     * - 사용자 행동 분석
     */
    public static final String STATISTICS_READ = "STATISTICS_READ";

    // ================================
    // 권한 그룹 (ROLE-BASED)
    // ================================
    
    /**
     * 일반 사용자가 가져야 할 기본 권한들
     */
    public static final String[] USER_BASIC_AUTHORITIES = {
        BOOK_READ,
        FEEDBACK_READ,
        FEEDBACK_WRITE,
        TRANSACTION_READ,
        TRANSACTION_BORROW,
        TRANSACTION_RETURN
    };
    
    /**
     * 도서 관리자가 가져야 할 권한들
     */
    public static final String[] BOOK_MANAGER_AUTHORITIES = {
        BOOK_READ,
        BOOK_CUD,
        BOOK_SHARE_MANAGE,
        BOOK_FILE_MANAGE,
        FEEDBACK_READ,
        FEEDBACK_MANAGE,
        TRANSACTION_READ,
        TRANSACTION_MANAGE
    };
    
    /**
     * 시스템 관리자가 가져야 할 권한들
     */
    public static final String[] SYSTEM_ADMIN_AUTHORITIES = {
        USER_READ,
        USER_MANAGE,
        SYSTEM_READ,
        SYSTEM_MANAGE,
        SYSTEM_LOG_READ,
        ADMIN_BASIC,
        ADMIN_ADVANCED
    };
    
    /**
     * 최고 관리자가 가져야 할 권한들
     */
    public static final String[] SUPER_ADMIN_AUTHORITIES = {
        ADMIN_ALL
    };

    // ================================
    // 유틸리티 메서드
    // ================================
    
    /**
     * 특정 역할에 해당하는 권한 목록을 반환합니다.
     * 
     * @param role 역할 (USER, BOOK_MANAGER, SYSTEM_ADMIN, SUPER_ADMIN)
     * @return 해당 역할의 권한 배열
     */
    public static String[] getAuthoritiesByRole(String role) {
        switch (role.toUpperCase()) {
            case "USER":
                return USER_BASIC_AUTHORITIES;
            case "BOOK_MANAGER":
                return BOOK_MANAGER_AUTHORITIES;
            case "SYSTEM_ADMIN":
                return SYSTEM_ADMIN_AUTHORITIES;
            case "SUPER_ADMIN":
                return SUPER_ADMIN_AUTHORITIES;
            default:
                return USER_BASIC_AUTHORITIES;
        }
    }
    
    /**
     * 모든 권한을 반환합니다.
     * SSGD의 "ALL" 권한과 동일한 기능입니다.
     * 
     * @return 모든 권한 배열
     */
    public static String[] getAllAuthorities() {
        return new String[] {
            // 도서 관리
            BOOK_READ, BOOK_CUD, BOOK_CRUD, BOOK_SHARE_MANAGE, BOOK_FILE_MANAGE,
            // 사용자 관리
            USER_READ, USER_MANAGE, USER_CREATE, USER_DELETE,
            // 거래 관리
            TRANSACTION_READ, TRANSACTION_BORROW, TRANSACTION_RETURN, TRANSACTION_MANAGE,
            // 피드백 관리
            FEEDBACK_READ, FEEDBACK_WRITE, FEEDBACK_MANAGE,
            // 시스템 관리
            SYSTEM_READ, SYSTEM_MANAGE, SYSTEM_BACKUP, SYSTEM_LOG_READ,
            // 관리자 권한
            ADMIN_BASIC, ADMIN_ADVANCED, ADMIN_SUPER, ADMIN_ALL,
            // 특수 권한
            API_ACCESS, BULK_OPERATION, STATISTICS_READ
        };
    }
}