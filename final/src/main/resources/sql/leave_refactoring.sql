-- =============================================================================
-- Leave 도메인 리팩토링 DDL
-- 실행 순서: 1.백업 → 2.신규 테이블 생성 → 3.데이터 마이그레이션 → 4.인덱스 생성
-- 주의: 운영 환경에서는 트랜잭션 내에서 실행 후 검증 완료 시 COMMIT
-- =============================================================================

-- =============================================================================
-- STEP 1. 기존 테이블 백업 (데이터 보존)
-- =============================================================================

RENAME TABLE LEAVE_SUBMIT       TO LEAVE_SUBMIT_BAK;
RENAME TABLE LEAVES             TO LEAVES_BAK;
RENAME TABLE LEAVE_ACCRUAL      TO LEAVE_ACCRUAL_BAK;
RENAME TABLE SUBMIT_AND_CALENDAR TO SUBMIT_AND_CALENDAR_BAK;


-- =============================================================================
-- STEP 2. 신규 테이블 생성
-- =============================================================================

-- 사원별 연도별 휴가 잔여일수
CREATE TABLE IF NOT EXISTS leave_balance (
    leave_balance_id BIGINT          NOT NULL AUTO_INCREMENT,
    member_id        INT             NOT NULL COMMENT '사원 사번',
    leave_type       VARCHAR(20)     NOT NULL COMMENT 'LeaveType Enum (ANNUAL/CONDOLENCE/...)',
    total_days       DECIMAL(4, 1)   NOT NULL COMMENT '부여 일수',
    used_days        DECIMAL(4, 1)   NOT NULL DEFAULT 0.0 COMMENT '사용 일수',
    remaining_days   DECIMAL(4, 1)   NOT NULL COMMENT '잔여 일수',
    year             INT             NOT NULL COMMENT '귀속 연도',
    accrual_date     DATE            NOT NULL COMMENT '부여일',
    expiry_date      DATE            NULL COMMENT '만료일 (연차만 사용)',
    PRIMARY KEY (leave_balance_id),
    CONSTRAINT uq_leave_balance UNIQUE (member_id, leave_type, year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='사원별 휴가 잔여일수';


-- 휴가 신청 내역
CREATE TABLE IF NOT EXISTS leave_request (
    leave_request_id BIGINT          NOT NULL AUTO_INCREMENT,
    member_id        INT             NOT NULL COMMENT '신청자 사번',
    leave_type       VARCHAR(20)     NOT NULL COMMENT 'LeaveType Enum',
    status           VARCHAR(20)     NOT NULL COMMENT 'LeaveStatus Enum (PENDING/APPROVED/REJECTED/CANCELLED)',
    start_date       DATE            NOT NULL COMMENT '휴가 시작일',
    end_date         DATE            NOT NULL COMMENT '휴가 종료일',
    use_days         DECIMAL(4, 1)   NOT NULL COMMENT '실제 차감 일수',
    reason           VARCHAR(500)    NULL COMMENT '신청 사유',
    applied_at       DATETIME        NOT NULL COMMENT '신청 일시',
    approver_id      INT             NULL COMMENT '결재자 사번',
    approved_at      DATETIME        NULL COMMENT '결재 일시',
    reject_reason    VARCHAR(500)    NULL COMMENT '반려 사유',
    cancelled_at     DATETIME        NULL COMMENT '취소 일시',
    PRIMARY KEY (leave_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='휴가 신청 내역';


-- 특별휴가 부여 이력 (Immutable 이력 테이블)
CREATE TABLE IF NOT EXISTS leave_accrual_history (
    accrual_id      BIGINT          NOT NULL AUTO_INCREMENT,
    member_id       INT             NOT NULL COMMENT '대상자 사번',
    leave_type      VARCHAR(20)     NOT NULL COMMENT 'LeaveType Enum',
    accrual_days    DECIMAL(4, 1)   NOT NULL COMMENT '부여 일수',
    accrual_reason  VARCHAR(300)    NOT NULL COMMENT '부여 사유',
    granted_by      INT             NOT NULL COMMENT '부여자(관리자) 사번',
    granted_at      DATETIME        NOT NULL COMMENT '부여 일시',
    PRIMARY KEY (accrual_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='특별휴가 부여 이력';


-- 휴가 신청 ↔ 캘린더 이벤트 매핑
CREATE TABLE IF NOT EXISTS leave_calendar_mapping (
    id               BIGINT  NOT NULL AUTO_INCREMENT,
    leave_request_id BIGINT  NOT NULL COMMENT 'leave_request.leave_request_id FK',
    calendar_no      INT     NOT NULL COMMENT 'CALENDAR.CALENDAR_No FK',
    PRIMARY KEY (id),
    CONSTRAINT uq_leave_calendar UNIQUE (leave_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='휴가-캘린더 매핑';


-- =============================================================================
-- STEP 3. 데이터 마이그레이션
-- =============================================================================

-- 3-1. leave_balance: 기존 LEAVES 테이블의 연차 정보를 연도별로 집계하여 이관
--      LEAVES 테이블은 단순히 "잔여 일수" 스냅샷이므로
--      현재 연도(YEAR(NOW()))의 연차 잔여로 간주하여 삽입
INSERT INTO leave_balance (member_id, leave_type, total_days, used_days, remaining_days, year, accrual_date)
SELECT
    l.MEMBER_ID,
    'ANNUAL'                          AS leave_type,
    l.LEVAE_DAYS                      AS total_days,
    0.0                               AS used_days,
    l.LEVAE_DAYS                      AS remaining_days,
    YEAR(NOW())                       AS year,
    CONCAT(YEAR(NOW()), '-01-01')     AS accrual_date
FROM LEAVES_BAK l
ON DUPLICATE KEY UPDATE
    total_days     = VALUES(total_days),
    remaining_days = VALUES(remaining_days);


-- 3-2. leave_request: 기존 LEAVE_SUBMIT에서 '취소 신청' 건을 제외하고 이관
--      기존 상태값 매핑:
--        '대기'           → PENDING
--        '승인'           → APPROVED
--        '반려'           → REJECTED
--        '취소'           → CANCELLED
--        '취소신청대기'   → CANCELLED (취소 이중 프로세스 폐기)
--        '취소신청승인'   → CANCELLED
--        '취소신청반려'   → APPROVED  (취소 반려 = 원 신청이 유효)
INSERT INTO leave_request (
    member_id, leave_type, status, start_date, end_date,
    use_days, reason, applied_at, approver_id, approved_at
)
SELECT
    ls.MEMBER_ID,
    CASE ls.LEAVE_SUB_TYPE
        WHEN '연차'       THEN 'ANNUAL'
        WHEN '오전반차'   THEN 'AM_HALF'
        WHEN '오후반차'   THEN 'PM_HALF'
        WHEN '경조사휴가' THEN 'CONDOLENCE'
        WHEN '공가'       THEN 'OFFICIAL'
        WHEN '병가'       THEN 'SICK'
        ELSE 'ANNUAL'
    END AS leave_type,
    CASE ls.LEAVE_SUB_STATUS
        WHEN '대기'         THEN 'PENDING'
        WHEN '승인'         THEN 'APPROVED'
        WHEN '반려'         THEN 'REJECTED'
        WHEN '취소'         THEN 'CANCELLED'
        WHEN '취소신청대기' THEN 'CANCELLED'
        WHEN '취소신청승인' THEN 'CANCELLED'
        WHEN '취소신청반려' THEN 'APPROVED'
        ELSE 'PENDING'
    END AS status,
    ls.LEAVE_SUB_START_DATE,
    ls.LEAVE_SUB_END_DATE,
    DATEDIFF(ls.LEAVE_SUB_END_DATE, ls.LEAVE_SUB_START_DATE) + 1  AS use_days,
    ls.LEAVE_SUB_REASON,
    COALESCE(ls.LEAVE_SUB_APPLY_DATE, NOW())                       AS applied_at,
    NULLIF(ls.LEAVE_SUB_APPROVER, 0),
    CASE WHEN ls.LEAVE_SUB_STATUS IN ('승인','반려') THEN ls.LEAVE_SUB_PROCESS_DATE ELSE NULL END
FROM LEAVE_SUBMIT_BAK ls
WHERE ls.REF_LEAVE_SUB_NO = 0;    -- 원본 신청 건만 이관 (취소 신청 행 제외)


-- 3-3. leave_accrual_history: 기존 LEAVE_ACCRUAL 이관
INSERT INTO leave_accrual_history (member_id, leave_type, accrual_days, accrual_reason, granted_by, granted_at)
SELECT
    la.RECIPIENT_ID,
    'CONDOLENCE'          AS leave_type,   -- 기존 특별휴가는 CONDOLENCE로 통합
    la.LEAVE_ACCRUAL_DAYS,
    COALESCE(la.LEAVE_ACCRUAL_REASON, ''),
    COALESCE(la.GRANTOR_ID, 0),
    COALESCE(la.ACCRUAL_DATE, NOW())
FROM LEAVE_ACCRUAL_BAK la;


-- 3-4. leave_calendar_mapping: 기존 SUBMIT_AND_CALENDAR 이관
--      leave_request의 새 ID와 매핑해야 하므로 원본 LEAVE_SUB_NO 기준으로 조인
INSERT INTO leave_calendar_mapping (leave_request_id, calendar_no)
SELECT
    lr.leave_request_id,
    sc.CALENDAR_NO
FROM SUBMIT_AND_CALENDAR_BAK sc
JOIN LEAVE_SUBMIT_BAK ls ON sc.LEAVE_SUB_NO = ls.LEAVE_SUB_NO
JOIN leave_request lr
    ON lr.member_id   = ls.MEMBER_ID
    AND lr.start_date = ls.LEAVE_SUB_START_DATE
    AND lr.end_date   = ls.LEAVE_SUB_END_DATE;


-- =============================================================================
-- STEP 4. 인덱스 생성
-- =============================================================================

-- leave_balance
CREATE INDEX idx_leave_balance_member_year
    ON leave_balance (member_id, year);

-- leave_request
CREATE INDEX idx_leave_request_member
    ON leave_request (member_id);

CREATE INDEX idx_leave_request_status
    ON leave_request (status);

CREATE INDEX idx_leave_request_date_range
    ON leave_request (member_id, start_date, end_date);

-- leave_accrual_history
CREATE INDEX idx_accrual_history_member
    ON leave_accrual_history (member_id);

-- leave_calendar_mapping
CREATE INDEX idx_leave_calendar_request
    ON leave_calendar_mapping (leave_request_id);


-- =============================================================================
-- 검증 쿼리 (마이그레이션 완료 후 실행)
-- =============================================================================
-- SELECT COUNT(*) FROM LEAVES_BAK;
-- SELECT COUNT(*) FROM leave_balance;
-- SELECT COUNT(*) FROM LEAVE_SUBMIT_BAK WHERE REF_LEAVE_SUB_NO = 0;
-- SELECT COUNT(*) FROM leave_request;
-- SELECT COUNT(*) FROM LEAVE_ACCRUAL_BAK;
-- SELECT COUNT(*) FROM leave_accrual_history;
