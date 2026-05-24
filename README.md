# 크리에이터 정산 API

## 프로젝트 개요

크리에이터 정산 도메인의 백엔드 API 서버. 
크리에이터가 강의를 판매하면 플랫폼이 수수료를 차감하고 월 단위로 정산금을 지급하는 프로세스

**제공 기능**
- 판매 / 취소 사실의 등록 및 조회
- 크리에이터별 월별 정산 조회
- 운영자용 기간 정산 집계
- (선택) 정산 확정/지급 상태 관리
- (선택) 수수료율 변경 이력 관리
- (선택) 정산 내역 엑셀/CSV 다운로드


## 기술 스택

- **언어 / 프레임워크**: Java 21, Spring Boot 3.5.14
- **데이터 접근**: Spring Data JPA (Hibernate)
- **데이터베이스**: MySQL 8.4
- **빌드**: Gradle 9.5.1
- **테스트**: JUnit 5, AssertJ, Mockito
- **컨테이너**: Docker, Docker Compose
- **환경**: 단일 서버 (수평 확장 미고려)


## 실행 방법

### 사전 요구사항
- Docker
- Docker Compose

### 실행

```bash
# 빌드 및 실행
docker compose up --build

# 백그라운드 실행
docker compose up -d --build

# 종료
docker compose down

# 데이터 포함 완전 삭제
docker compose down -v
```

기동 후 `http://localhost:9090` 에서 API에 접근 가능.

### 환경 변수
주요 설정은 `.env` 파일 또는 `docker-compose.yml`에서 관리:

### 시드 데이터 (선택)
Creator / Course / Sales / Cancellation / Settlement 샘플 데이터가 필요한 경우, REST API가 아닌 `script/` 디렉터리의 파이썬 스크립트로 직접 적재한다 (Creator·Course는 등록 API를 제공하지 않음). 사용법은 `script/README.md` 참고.


## 요구사항 해석 및 가정

### 인증/인가
- 인증 처리는 범위 외. Creator/Operator의 로그인 ID는 호출 측에서 직접 파라미터로 전달한다고 가정한다.

### 정산 단위
- 정산은 결제 건별이 아니라 **(Creator, 월) 단위**로 이뤄진다 (월 단위 송금 모델).
- 월 경계는 KST 기준 1일 00:00:00 ~ 말일 23:59:59 (DB serverTimezone·JVM TZ가 `Asia/Seoul`인 운영 환경 전제. 코드 레벨에 별도 ZoneId 변환은 두지 않음).
- 운영자는 월 마감 후 특정 일자에 정산을 확정·송금한다.

### 산정 기준 비대칭
- SaleRecord는 **결제 일시** 기준으로 월에 귀속.
- CancellationRecord는 **취소 일시** 기준으로 월에 귀속.
- 한 거래의 판매·취소가 서로 다른 월에 속할 수 있다.

### 통화
- 단일 통화(KRW) 가정. 환율, 다국 통화 처리는 없음.

### 결제
- PG 연동은 범위 외. 판매·취소는 직접 API 호출로 등록.
- 판매 및 취소 내역 등록을 요청하는 주체는 PG사로 고정하여 PG사의 결제 및 취소 요청 시각이 paidAt이나 cancelledAt의 SSOT가 되어야 함.
- PG 서버 자체의 시계가 틀리면 정산 시점에 영향을 받지만 이건 PG사의 책임 영역이고, 주요 PG(이니시스/토스페이먼츠/포트원 등)는 NTP·감사로그가 갖춰져 있음.

### 판매 내역 등록
- 판매 내역 등록 시 정산 대상은 크리에이터이지만, 요청 측은 크리에이터 id를 직접 제공하지 않는다.
- 판매 내역 목록 조회 시 크리에이터별 필터링이 가능해야 한다.
- 강의가 삭제되거나 귀속 대상이 변경될 수 있다면, 강의 id만으로는 판매 시점의 정확한 귀속 크리에이터를 식별할 수 없다.
- 본 설계에서는 강의가 물리 삭제되지 않으며(soft delete만 허용, 완전 소거 없음) 귀속 대상 또한 변경되지 않는다고 가정한다. 이 가정 하에서는 SalesRecord의 강의 id를 통해 항상 크리에이터를 역추적할 수 있다.
- 따라서 SalesRecord가 크리에이터 id를 별도 컬럼으로 보유하지 않아도 무방하다. 후속 샘플 데이터 작성 시에도 "강의 id로 크리에이터를 조회하지 못하는 케이스"는 다루지 않는다.

### 동일 강의/학생 중복
- 동일 학생에 대해서 특정 강의가 중복으로 결제되면 안된다고 가정함. (선물 등의 대리 결제가 있을수는 있지만 현재 정의된 시나리오에 특별히 존재하지 않음)
- 부분 환불된 강의는 아직 완전히 취소되지 않은 강의이므로 중복 결제 체크 대상에 포함된다고 가정함.

### 외부 신뢰 채널 (External SSOT)
- 식별자 변조에 대한 재검증은 현재 시나리오 구조에서 불가능(인증/인가 계층 부재 => 본 시스템은 백엔드 단독으로 신원·소유권을 판별할 수단을 갖지 않음).
- 판매 등록 요청의 course_id, student_id, 취소 등록 요청의 sales_record_id는 외부 신뢰 채널을 통해 변조 없이 전달됨.


## 설계 결정과 이유

### 1. Settlement: Lazy Calculation / Confirm-time Snapshot
PENDING 상태 Settlement는 DB에 저장하지 않는다. 월별 조회(`GET /api/settlements`) 시점에 SaleRecord / CancellationRecord 사실 기록을 집계해 매 호출 계산하여 반환한다. 
운영자가 `POST /api/settlements/confirm`을 호출하는 시점에 산정값과 적용 수수료율이 그 행에 처음 INSERT 되어 CONFIRMED 스냅샷으로 박제된다.
- 사실 기록(SaleRecord / CancellationRecord)을 SSOT로 유지. PENDING 단계에서 별도 동기화·반정규화 부담이 없음.
- 상태 관리(PENDING / CONFIRMED / PAID)와 라이프사이클이 확정 시점 INSERT 1회로 자연스럽게 결합.

### 2. 확정 시 스냅샷 박제
Settlement가 CONFIRMED 되는 순간 산정값과 적용 수수료율을 그 행에 '값으로' 보존한다. 
PENDING 동안은 조회 시점마다 현재 FeePolicy 기준으로 재계산되며(정책 변경 시 PENDING 응답이 달라질 수 있음), CONFIRMED 이후로는 행에 박힌 fee_rate가 그대로 사용된다.
- 확정된 정산 금액은 회계 정합성 차원에서 변경되면 안 됨.
- 늦은 환불이나 FeePolicy 변경에도 확정된 과거 정산 금액이 흔들리지 않음.

### 3. CancellationRecord를 SaleRecord와 분리
판매와 취소를 같은 테이블에 압축하지 않고 별개의 사실 기록으로 분리한다.
- 부분 환불 (한 판매에 여러 취소) 지원.
- 결제 월 ≠ 취소 월 시나리오를 자연스럽게 표현.

### 4. 사실 기록의 불변성
SaleRecord, CancellationRecord는 등록 후 수정·삭제 불가. 변경이 필요하면 새 기록을 추가한다.
- 감사 추적성 확보.
- 과거 정산의 재계산 가능성 보장.

### 5. FeePolicy를 Entity로 모델링
수수료율을 단순 상수가 아니라 (수수료율, 효력 기간)을 가진 엔티티로 관리한다.
- 수수료율이 변경되어도 과거 정산은 '당시' 정책 그대로 유지.
- Settlement는 수수료율을 값으로 복사·보존하여 정책 행에 직접 의존하지 않음.

### 6. 늦은 환불 처리
CONFIRMED 이후 도착하는 환불·정정은 본 Settlement(스냅샷)를 수정하지 않는다. 
CancellationRecord는 자신의 `cancelledAt`이 속한 월의 PENDING 정산 계산에 자연 귀속되므로 다음 정산 사이클에서 반영된다.
- 확정된 정산의 무결성 유지.
- 별도 보정 정산 행을 생성하지 않고 사실 기록 + Lazy 재계산으로 일관 처리.


## 미구현 / 제약사항

### 미구현 (범위 외)
- 인증/인가 (Spring Security, JWT 등)
- PG(결제 시스템) 연동
- 프론트엔드 UI
- 실제 송금 API 연동 (PAID 상태 전이는 기록만 다룸)
- 다중 통화·환율 처리
- 강의(Course) 본체 관리 (콘텐츠, 가격 정책 등은 외부 책임)

### 선택 구현 항목
- 정산 확정 상태 관리 (PENDING → CONFIRMED → PAID 전이 API)
- 동일 기간 중복 정산 방지
- 정산 내역 엑셀 다운로드
- 수수료율 변경 이력 관리

### 운영상 제약
- 단일 서버, 단일 MySQL 인스턴스 가정 (수평 확장·읽기 복제·샤딩 미적용).
- 초기 수수료율은 20%로 고정되며, FeePolicy를 통해 변경 가능.


## AI 활용 범위

### 환경 세팅
- 백엔드 초기 환경 구성(SpringBoot, JPA)
- 컨테이너 환경 구성(Docker, Docker Compose)

### 코드 베이스 활용
- 코드 초안 작성
- 코드 커밋 이전 리뷰

### 테스트 데이터 스크립트 작성
- 강의, 크리에이터, 판매 및 취소 내역, 정산 데이터의 정합성에 맞는 데이터 생성 파이썬 스크립트

### 검증 시나리오 제작을 위한 http 요청 파일 작성
- 요구 사항에 적힌 샘플 데이터 검증 시나리오
- 정상, 엣지, 실패 케이스에 해당하는 요청 시나리오


## API 목록 및 예시

### 엔드포인트 목록

| 도메인 | Method | Path | 설명 | 응답 | 요청 파라미터 |
|---|---|---|---|---|---|
| Sales | POST | `/api/sales` | 매출 등록 | 201 | Body: `courseId`, `studentId`, `paymentAmount`, `paidAt` |
| Sales | POST | `/api/sales/cancellations` | 환불(취소) 등록 | 201 | Body: `salesRecordId`, `refundAmount`, `cancelledAt` |
| Sales | GET | `/api/sales` | 매출 목록 조회 | 200 | Query: `creatorId`(선택), `from`, `toExclusive` |
| Settlement | GET | `/api/settlements` | 월별 정산 조회 | 200 | Query: `creatorId`, `yearMonth`(yyyy-MM) |
| Settlement | GET | `/api/settlements/aggregate` | 기간별 정산 집계 | 200 | Query: `from`(yyyy-MM-dd), `to`(yyyy-MM-dd) |
| Settlement | GET | `/api/settlements/aggregate/download` | 정산 집계 엑셀 다운로드 | 200 | Query: `from`(yyyy-MM-dd), `to`(yyyy-MM-dd) |
| Settlement | POST | `/api/settlements/confirm` | 정산 확정 (PENDING→CONFIRMED) | 201 | Body: `creatorId`, `yearMonth`(yyyy-MM), `confirmedAt` |
| Settlement | POST | `/api/settlements/pay` | 정산 지급 (CONFIRMED→PAID) | 204 | Body: `creatorId`, `yearMonth`(yyyy-MM), `paidAt` |
| FeePolicy | POST | `/api/fee-policies` | 수수료 정책 등록 | 201 | Body: `rate`(0.0~1.0), `effectiveFrom`(미래 일자) |
| FeePolicy | GET | `/api/fee-policies` | 수수료 정책 목록 | 200 | (없음) |

### 샘플 흐름

`creatorId=1`, `courseId=100`, `studentId=500`, 2026-04월 결제 50,000원 / 환불 20,000원 시나리오 (수수료율 20%).

**① 매출 등록** — `POST /api/sales` → `201 Created` (본문 없음)
**② 환불 등록** — `POST /api/sales/cancellations` → `201 Created` (본문 없음)
**③ 월별 정산 조회** — `GET /api/settlements?creatorId=1&yearMonth=2026-04`
  ```json
  {
    "creatorId": 1, "yearMonth": "2026-04", "status": "PENDING",
    "totalSales": 50000, "totalRefund": 20000, "netSales": 30000,
    "feeRate": 0.2000, "platformFee": 6000, "expectedPayout": 24000,
    "salesCount": 1, "cancellationCount": 1,
    "confirmedAt": null, "paidAt": null
  }
  ```
**④ 정산 확정** — `POST /api/settlements/confirm` → `201 Created` (본문 없음, `status`가 `CONFIRMED`로 전이, `confirmedAt` 업데이트)
**⑤ 정산 지급** — `POST /api/settlements/pay` → `204 No Content` (본문 없음, `status`가 `PAID`로 전이, `paidAt` 업데이트)

### 에러 응답

모든 에러는 공통 형식으로 반환됩니다.
  ```json
  { "status": 400, "code": "VALIDATION", "message": "yearMonth: 형식이 올바르지 않습니다" }
  ```
| code | HTTP | 발생 상황 |
|---|---|---|
| `VALIDATION` | 400 | 요청 형식 검증 실패 (Bean Validation, 타입 불일치, JSON 파싱) |
| `BAD_REQUEST` | 400 | 도메인 규칙 위반 (미정의 도메인 메시지) |
| `<도메인 에러 코드>` | 도메인별 | 도메인 정의 예외 (예: 이미 확정된 정산, 미존재 정산 등) |
| `INTERNAL` | 500 | 예상치 못한 서버 오류 |


## 데이터 모델 설명
- ERD -> schema/ERD.pdf 참고
- DB Schema -> schema/schema.sql 참고


## 테스트 실행 방법
테스트는 H2 인메모리 DB로 자체 격리 실행되어 도커 기동 여부와 무관하다. JDK 21이 설치되어 있으면 **프로젝트 루트 디렉토리에서** 다음 명령으로 실행한다.
  ```bash
  # macOS / Linux
  ./gradlew test

  # Windows
  .\gradlew.bat test
  ```
테스트 리포트: `<프로젝트 루트>/build/reports/tests/test/index.html`

### 샘플 데이터 시나리오 동작 HTTP 요청 테스트 방법
- SQL Seed 데이터 INSERT -> http/scenarios/settlement/full-settlement-scenario.seed.sql
- HTTP 요청 파일 -> http/scenarios/settlement/full-settlement-scenario.http


## 기본 테스트 외 추가 테스트 케이스

- 정산 상태 전이 무결성
  - **추가 이유** => 회계 처리가 완료된 데이터가 되돌려지는 시나리오 방지
  - 도메인·어플리케이션·HTTP 3개 레이어에서 일관 보장
  - PENDING/CONFIRMED/PAID 역방향, 건너뛰기 전이 방지(PENDING→pay, PAID→confirm 등)
  - 멱등성 위반 방지(이미 CONFIRMED인데 다시 confirm)

- 당월 confirm/pay 상태 전이 가드(서버 KST 기준)
  - **추가 이유** => 운영자가 confirmedAt/paidAt을 임의 미래로 요청해도 서버 KST targetMonth 기준으로 막아, 결제·환불이 계속 누적되는 동안 회계 확정·지급되는 사고 방지
  - 현재 진행 중인 월 또는 미래 월에 confirm/pay 요청 시 차단

- FeePolicy 당월 등록 건 미적용 (다음 달 1일부터 효력)
  - **추가 이유** => 운영자가 월 중간/월초에 정책 변경해도 본 월 정산에는 영향 없고 다음 달부터 적용. 진행 중인 월의 정책 혼재 방지

- 결제·취소 월 비대칭
  - **추가 이유** => 한 거래의 판매·취소가 서로 다른 월에 속하는 경우와 늦은 환불 보정 정산을 커버하기 위함
  - 환불은 paidAt(결제월)이 아닌 cancelledAt(취소월) 기준으로 월 귀속

- 다회 부분 환불 누적 한도
  - **추가 이유** => 부분 환불 누적이 총액을 넘어 매출이 비정상적인 음수를 취하는 경우를 막기 위함 
  - 한 결제에 여러 번 부분 환불 시 단일/누적 합계가 결제 초과하면 차단
  - 잔여액이 정확히 0에 일치(boundary)하면 통과

- 무료 강의 환불 불가
  - **추가 이유** => 무료 강의 환불은 별도 차단해 의미 없는 CancellationRecord 무한 생성 방지

- confirm 이전 정산 조회와 이후 정산 조회 환경
  - **추가 이유** => 운영자의 confirm 이후 정산 데이터 스냅샷 로직에 대한 정상 동작 확인
  - confirm 이전 상태에서는 미저장 시 sales 집계 쿼리로 산출
  - confirm 이후에는 settlement에 INSERT하여 정산 조회를 settlement 테이블 활용 
  - pay로의 즉시 상태 전이를 통한 INSERT 시도는 불가하고 404로 차단

- 월 경계 정확 처리
  - **추가 이유** => 월 경계 이후에 정산 상태 전이가 가능, 수수료율 변경 적용 분기 등의 중요한 비즈니스 결정이 나눠지므로
  - 말일 23:59 vs 다음 달 1일 00:00 같은 월 boundary가 다른 yearMonth로 정확히 분류됨을 JPA 합계 쿼리와 도메인 집계 양쪽에서 단언

- 빈 데이터·0원 응답
  - **추가 이유** => 샘플 데이터 시나리오의 요청 및 조회 내용이 없어도 400대 에러가 아닌 200 정상 응답 확인
  - 시스템 초기 상태(크리에이터 0명), 활동 없는 월, 환불 이력 없는 sale 등 모든 빈 케이스에서 빈 배열·0원 정상 응답 (예외나 null 반환 없음)

- 잘못된 입력 차단
  - **추가 이유** => 외부 요청에 대한 최소한의 신뢰를 통한 무분별한 validation 분기를 차단
  - yearMonth/from 포맷 오류, 필수 필드 누락, from > to 같은 기간 역전을 모두 400 VALIDATION으로 일관 응답

- 동일 학생·강의 재등록 정책
  - **추가 이유** => 이미 결제한 강의의 판매가 재등록되어 중복 결제가 되는 케이스 방지(강의 선물 등의 비즈니스 요구사항이 있는 경우가 없다고 가정)
  - 활성 결제 또는 부분 환불 상태가 남아 있으면 재등록 차단. 전액 환불(더 이상 active 아님) 후에는 재등록 허용

- 수수료율 정책 중복 등록 차단
  - **추가 이유** => 수수료율 변경 관리 중에 문제 발생 시 서비스 혼란이 발생할 수 있으므로 추가
  - 동일 effectiveFrom으로 FeePolicy 중복 등록 시 UNIQUE 제약 + application 사전 체크 이중 방어

- 도메인 invariant boundary
  - **추가 이유** => 기본적인 비즈니스 경계값 체크 및 예상치못한 회귀 버그 발생 시 최저한의 산식 결과 안전망 구축
  - FeeRate의 0%/100% 경계와 1초과/음수 거부
  - Money·Cancellations·OccurredAt null 거부
  - fallback defaultRate(20%) 동작

- HALF_UP 반올림 분수 boundary
  - **추가 이유** => 회계상 1원 차이가 정산에 누적되지 않도록 보장
  - 수수료 산식 결과가 정확히 .5인 분수에서 HALF_UP(올림)으로 동작 & RoundingMode 회귀(HALF_EVEN 등) 즉시 검출

- 응답 직렬화 회귀 안전망 추가
  - **추가 이유** => 신규 필드 추가 후 매핑 누락 방지
  - PAID 상태 settlement 조회 응답 JSON에 paidAt 필드 노출
