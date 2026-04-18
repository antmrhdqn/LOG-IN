# Leave 도메인 리팩토링 요약

## 핵심 변경 원칙
| # | 원칙 | Before | After |
|---|------|--------|-------|
| 1 | 상태값 | 7개 한글 문자열 (대기, 승인, 반려, 취소, 취소신청대기, 취소신청승인, 취소신청반려) | 4개 Enum (PENDING, APPROVED, REJECTED, CANCELLED) |
| 2 | 취소 프로세스 | 취소 신청 → 결재 이중 프로세스 | 시작일 이전 직접 취소 |
| 3 | 잔여일수 | 매번 합산 계산 | LeaveBalance.remainingDays 필드 관리 |
| 4 | 날짜 타입 | String (YYYY-MM-DD) | LocalDate / LocalDateTime |
| 5 | 반차 계산 | int (0 또는 1) | BigDecimal 0.5 |
| 6 | 상속 구조 | LeaveService extends LeaveUtil | LeaveDaysCalculator를 @Component로 주입 |
| 7 | dirty checking | Entity→DTO→Entity 이중 변환 | 엔티티 직접 수정 (save 호출 최소화) |

---

## 삭제된 파일

### Entity (4개)
| 파일 | 이유 |
|------|------|
| `leave/entity/LeaveSubmit.java` | `leave/entity/LeaveRequest.java`로 대체 |
| `leave/entity/Leaves.java` | `leave/entity/LeaveBalance.java`로 대체 |
| `leave/entity/LeaveAccrual.java` | `leave/entity/LeaveAccrualHistory.java`로 대체 |
| `leave/entity/SubmitAndCalendar.java` | `leave/entity/LeaveCalendarMapping.java`로 대체 |

### Repository (5개)
| 파일 | 이유 |
|------|------|
| `leave/repository/LeaveSubmitRepository.java` | 엔티티 삭제에 따른 제거 |
| `leave/repository/LeaveRepository.java` | 엔티티 삭제에 따른 제거 |
| `leave/repository/LeaveAccrualRepository.java` | 엔티티 삭제에 따른 제거 |
| `leave/repository/SubmitAndCalendarRepository.java` | 엔티티 삭제에 따른 제거 |
| `leave/repository/LeaveCalendarRepository.java` | CalendarRepository 중복 — 미사용 |

### DTO (4개)
| 파일 | 이유 |
|------|------|
| `leave/dto/LeaveSubmitDTO.java` | `leave/dto/LeaveRequestDTO.java`로 대체 |
| `leave/dto/LeavesDTO.java` | `leave/dto/LeaveBalanceDTO.java`로 대체 |
| `leave/dto/SubmitAndCalendarDTO.java` | 매핑 엔티티 단순화로 DTO 불필요 |
| `leave/dto/LeaveMemberDTO.java` | 미사용 |

### Service / Controller / Util (4개)
| 파일 | 이유 |
|------|------|
| `leave/service/LeaveService.java` (구) | 전면 재작성으로 대체 |
| `leave/controller/LeaveController.java` (구) | 전면 재작성으로 대체 |
| `leave/util/LeaveUtil.java` | `leave/util/LeaveDaysCalculator.java`로 대체 |
| `leave/util/LeaveSubmitBuilder.java` | 빌더 패턴 → @Builder Lombok으로 대체 |

---

## 신규 생성된 파일

### Enum (2개)
| 파일 | 설명 |
|------|------|
| `leave/enums/LeaveType.java` | ANNUAL / AM_HALF / PM_HALF / CONDOLENCE / OFFICIAL / SICK |
| `leave/enums/LeaveStatus.java` | PENDING / APPROVED / REJECTED / CANCELLED + 상태 전이 검증 |

### Entity (4개)
| 파일 | 테이블 |
|------|--------|
| `leave/entity/LeaveRequest.java` | `leave_request` |
| `leave/entity/LeaveBalance.java` | `leave_balance` |
| `leave/entity/LeaveAccrualHistory.java` | `leave_accrual_history` |
| `leave/entity/LeaveCalendarMapping.java` | `leave_calendar_mapping` |

### Repository (4개)
| 파일 |
|------|
| `leave/repository/LeaveRequestRepository.java` |
| `leave/repository/LeaveBalanceRepository.java` |
| `leave/repository/LeaveAccrualHistoryRepository.java` |
| `leave/repository/LeaveCalendarMappingRepository.java` |

### DTO (4개)
| 파일 | 설명 |
|------|------|
| `leave/dto/LeaveRequestDTO.java` | 신청 요청/응답 공용 (Builder 패턴) |
| `leave/dto/LeaveBalanceDTO.java` | 잔여일수 응답 |
| `leave/dto/LeaveAccrualDTO.java` | 특별휴가 부여 요청 (재정의) |
| `leave/dto/LeaveInfoDTO.java` | 사원별 휴가 현황 요약 (재정의, BigDecimal) |

### Service (2개)
| 파일 | 설명 |
|------|------|
| `leave/service/LeaveService.java` | 휴가 신청/결재/취소/부여 전체 흐름 |
| `leave/service/LeaveBalanceService.java` | 잔여일수 조회/차감/복원/부여 전담 |

### Util (1개)
| 파일 | 설명 |
|------|------|
| `leave/util/LeaveDaysCalculator.java` | 영업일 계산 + 날짜 유효성 검증 (@Component) |

### 기타
| 파일 | 설명 |
|------|------|
| `src/main/resources/sql/leave_refactoring.sql` | 테이블 백업 / 신규 DDL / 마이그레이션 / 인덱스 |

### 테스트 (2개)
| 파일 | 테스트 수 |
|------|----------|
| `leave/service/LeaveServiceTest.java` | 13개 (Mockito 단위 테스트) |
| `leave/util/LeaveDaysCalculatorTest.java` | 7개 (순수 단위 테스트) |

---

## 변경된 파일

### `common/error/ErrorCode.java`
| 코드 | Enum 이름 | 메시지 | 비고 |
|------|-----------|--------|------|
| L001 | `INSUFFICIENT_LEAVE_DAYS` | 잔여 휴가 일수가 부족합니다 | 유지 |
| L002 | `INVALID_LEAVE_PERIOD` | 종료일이 시작일보다 빠를 수 없습니다 | 유지 |
| L003 | `LEAVE_NOT_FOUND` | 해당 휴가 신청 내역을 찾을 수 없습니다 | 유지 |
| L004 | `LEAVE_BALANCE_NOT_FOUND` | 해당 연도의 휴가 잔여일수 정보를 찾을 수 없습니다 | **신규** |
| L005 | `LEAVE_DUPLICATE_DATE` | 해당 날짜에 이미 신청된 휴가가 있습니다 | **신규** |
| L006 | `LEAVE_PAST_DATE` | 과거 날짜로는 휴가를 신청할 수 없습니다 | **신규** |
| L007 | `LEAVE_HALF_DAY_DATE_MISMATCH` | 반차는 시작일과 종료일이 같아야 합니다 | **신규** |
| L008 | `LEAVE_INVALID_STATUS_TRANSITION` | 현재 상태에서는 해당 처리를 할 수 없습니다 | **신규** |
| L009 | `LEAVE_CANCEL_AFTER_START` | 이미 시작된 휴가는 취소할 수 없습니다 | **신규** |
| L010 | `LEAVE_ALREADY_FINISHED` | 이미 종결된 신청 건입니다 | **신규** |

### `leave/repository/LeaveDepartmentRepository.java`
- ID 타입 `String` → `Integer` 버그 수정

### `insite/entity/insiteLeave/InsiteLeaveSubmit.java`
- 매핑 테이블: `LEAVE_SUBMIT` → `leave_request`
- 컬럼: `LEAVE_SUB_NO / LEAVE_SUB_START_DATE / LEAVE_SUB_STATUS` → `leave_request_id / start_date / status`
- PK 타입: `int` → `Long`

### `insite/entity/insiteLeave/InsiteLeaves.java`
- 매핑 테이블: `LEAVES` → `leave_balance`
- 컬럼: `LEAVE_NO / LEVAE_DAYS` → `leave_balance_id / remaining_days`
- `leave_type`, `year` 컬럼 추가

---

## API 변경 사항

### 기존 URL → 새 URL

| 기능 | 기존 (LeaveController) | 새 (LeaveController) |
|------|----------------------|---------------------|
| 내 신청 목록 조회 | `GET /leaveSubmits?page=&size=` | `GET /api/leave/requests?memberId=&page=&size=` |
| 휴가 신청 | `POST /leaveSubmits` | `POST /api/leave/requests` |
| 취소 | `DELETE /leaveSubmits/{id}` | `PATCH /api/leave/requests/{id}/cancel` |
| 결재 처리(승인/반려) | `PUT /leaveSubmits` | `PATCH /api/leave/requests/{id}/approve` |
| 결재 대기 목록 | `GET /leaveSubmits` (전체) | `GET /api/leave/requests/pending` |
| 반려 처리 | `PUT /leaveSubmits` (status로 구분) | `PATCH /api/leave/requests/{id}/reject` |
| 특별휴가 부여 목록 | `GET /leaveAccruals` | `GET /api/leave/accruals` |
| 특별휴가 부여 | `POST /leaveAccruals` | `POST /api/leave/accruals` |
| 잔여일수 조회 | `GET /leaves` (전체 목록) | `GET /api/leave/balances?memberId=` |
| 전 사원 잔여 현황 | `GET /leaves` | `GET /api/leave/balances/all` |

### 상태값 변경

| 기존 문자열 | 새 Enum |
|------------|---------|
| `"대기"` | `PENDING` |
| `"승인"` | `APPROVED` |
| `"반려"` | `REJECTED` |
| `"취소"` | `CANCELLED` |
| `"취소신청대기"` | *(폐기)* → `CANCELLED`로 통합 |
| `"취소신청승인"` | *(폐기)* → `CANCELLED`로 통합 |
| `"취소신청반려"` | *(폐기)* → `APPROVED` 유지로 처리 |

---

## DB 테이블 변경

| 기존 테이블 | 새 테이블 | 주요 변경 |
|------------|----------|----------|
| `LEAVE_SUBMIT` | `leave_request` | PK: int → BIGINT, 상태: VARCHAR → Enum, 날짜: VARCHAR → DATE/DATETIME, 반차 0.5일 지원 |
| `LEAVES` | `leave_balance` | 잔여일수 필드 분리 (total/used/remaining), DECIMAL(4,1), year 컬럼 추가 |
| `LEAVE_ACCRUAL` | `leave_accrual_history` | Immutable 이력 테이블, leave_type Enum 저장 |
| `SUBMIT_AND_CALENDAR` | `leave_calendar_mapping` | PK: BIGINT, leave_request_id FK |
