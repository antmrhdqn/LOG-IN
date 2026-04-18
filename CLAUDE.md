# 프로젝트 개요
- Spring Boot 3.2.4 / Java 17 기반 사내 그룹웨어 "LOG-IN"
- 패키지 루트: com.insider.login
- DB: MySQL, JPA (generate-ddl: false, 직접 DDL 관리)
- 빌드: Gradle

# 현재 진행 중인 작업
휴가(Leave) 도메인의 전면 리팩토링. 기능/프로세스 재설계가 1순위, 코드 구조 개선이 2순위.

# 리팩토링 핵심 원칙
1. 상태값: 기존 7개 한글 문자열 → 4개 Enum (PENDING, APPROVED, REJECTED, CANCELLED)
2. 취소 프로세스: "취소 신청 → 결재" 이중 프로세스 폐기 → 시작일 이전 직접 취소
3. 발생/사용 분리: 특별휴가 부여(Accrual)와 사용(Request)은 별개 트랜잭션
4. 잔여일수 관리: 매번 합산 계산 → LeaveBalance.remainingDays 필드 관리
5. 반차: 0.5일 계산 (BigDecimal 사용)
6. 날짜 타입: String → LocalDate/LocalDateTime 통일
7. 물리 삭제 금지: 상태 변경(논리 삭제)으로 이력 보존
8. 조합 > 상속: LeaveService extends LeaveUtil → LeaveUtil을 @Component로 주입
9. dirty checking 활용: Entity→DTO→Entity 이중 변환 제거

# 참조해야 할 기존 파일
- 기존 leave 패키지: src/main/java/com/insider/login/leave/
- 공통 에러: common/error/ErrorCode.java
- 공통 응답: common/response/ResponseMessage.java
- 캘린더 연동: calendar/entity/Calendar.java, calendar/repository/CalendarRepository.java
- insite 도메인이 leave 테이블을 읽기 전용으로 참조함 (insite/entity/insiteLeave/)

# 새로운 패키지 구조 (leave 하위)
leave/
├── controller/LeaveController.java
├── service/LeaveService.java
├── service/LeaveBalanceService.java   (잔여일수 관리 전담)
├── repository/
│   ├── LeaveRequestRepository.java
│   ├── LeaveBalanceRepository.java
│   └── LeaveAccrualHistoryRepository.java
├── entity/
│   ├── LeaveRequest.java
│   ├── LeaveBalance.java
│   ├── LeaveAccrualHistory.java
│   └── LeaveCalendarMapping.java      (기존 SubmitAndCalendar 대체)
├── dto/
│   ├── LeaveRequestDTO.java
│   ├── LeaveBalanceDTO.java
│   ├── LeaveAccrualDTO.java
│   └── LeaveInfoDTO.java
├── enums/
│   ├── LeaveType.java
│   └── LeaveStatus.java
└── util/
    └── LeaveDaysCalculator.java       (영업일 계산 유틸)

# 네이밍 규칙
- 엔티티: 단수형 (LeaveRequest, LeaveBalance)
- 테이블: SNAKE_CASE (leave_request, leave_balance)
- API: kebab-case (/api/leave-requests)
- Enum: UPPER_SNAKE_CASE (PENDING, APPROVED)
- DTO 필드: camelCase
