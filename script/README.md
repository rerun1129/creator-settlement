# Seed Data Scripts

개발/시연용 데이터를 로컬 docker MySQL에 적재한다. 세 단계:

- **Phase 1** `generate_data.py` — Creator 1,000 + Course 10,000
- **Phase 2** `generate_transactions.py` — Sales 3,000,000 + Cancellation 150,000
- **Phase 3** `generate_settlements.py` — Settlement (creator, target_month) 단위, 거래 있는 조합만

세 스크립트는 완전히 독립이며 서로 import하지 않는다. (utility 함수 코드 중복 허용)

## 공통 사전 조건

1. docker-compose로 MySQL 컨테이너 기동 (컨테이너명 기본값 `creator-settlement-mysql`).
   - `docker-compose.yml`에 `./schema:/docker-entrypoint-initdb.d:ro` 마운트가 있어 **빈 볼륨 첫 기동 시** `schema/V1__init.sql` 자동 실행.
   - 이미 볼륨이 있고 테이블이 없다면 `docker compose down -v && docker compose up -d mysql`로 깨끗하게 재생성.
2. 프로젝트 루트 `.env` 의 `MYSQL_USER` / `MYSQL_PASSWORD` / `MYSQL_DATABASE` 값을 사용한다.
3. 호스트에서 `docker` CLI 실행 가능해야 한다.
4. 호스트 의존성 설치: `pip install -r script/requirements.txt` (`python-dotenv` 1개)

## 공통 적재 방식

호스트 Python 드라이버(PyMySQL · mysql-connector-python)와 MySQL 8.4 / Python 3.14 조합에서 `caching_sha2_password` 협상이 깨지는 사례가 있어, 컨테이너 내부의 `mysql` CLI를 `docker exec -i creator-settlement-mysql mysql ...` 형태로 호출해 SQL을 stdin으로 전달한다.

> 호스트에 별도 `mysqld` 서비스가 떠 있으면 docker mysql과 listen 주소(IPv4 / IPv6)가 갈려 외부 클라이언트(IntelliJ DataSource 등) 연결이 깨질 수 있다(`auth_gssapi_client` 오류 등). 적재 스크립트는 컨테이너 내부 경로로 들어가므로 영향 없지만, 동일 환경에서 IDE 연결이 실패하면 다음을 점검: `Get-NetTCPConnection -LocalPort 3306 -State Listen`.

## 공통 환경변수 오버라이드

| 변수 | 기본값 | 용도 |
|------|--------|------|
| `DB_CONTAINER` | `creator-settlement-mysql` | docker exec 대상 컨테이너명 |
| `MYSQL_USER` / `MYSQL_PASSWORD` / `MYSQL_DATABASE` | `.env` 값 | 컨테이너 mysql CLI 인증 |

## 전체 실행 순서

```bash
python script/generate_data.py
python script/generate_transactions.py
python script/generate_settlements.py
```

---

# Phase 1: Creator / Course (`generate_data.py`)

## 산출 데이터

- Creator: **1,000명** (시중 강사 톤 한글 + 일부 영문 ID 닉네임 — 실명풍 / 형용사+개발자 / 코딩애플 류 조어풍 등 혼합, VARCHAR(50) unique)
- Course: **10,000건** (시중 강의명 톤 한글 title, VARCHAR(255))
- Creator당 Course 분포 (long-tail, `random.seed(42)` 재현 가능)
  - 0개: 50명 / 1~3개 약 238명 / 4~10개 약 475명 / 11~20개 약 190명 / 21~35개 약 47명
  - 합계가 정확히 10,000건이 되도록 자동 보정

## 실행

```bash
python script/generate_data.py
```

옵션:

| 옵션 | 설명 |
|------|------|
| `--dry-run` | 분포만 출력, DB I/O 없음 |
| `--force` | `sales_record` / `cancellation_record`에 데이터가 남아 있어도 함께 TRUNCATE |
| `--seed N` | 난수 시드 변경 (기본 42) |
| `--creators-only` | `creator_id` 유지하면서 `name`만 새로 부여(`INSERT … ON DUPLICATE KEY UPDATE`). course 무손상 |

## 멱등성

1. `sales_record` / `cancellation_record` 가 비어 있는지 확인 (비어 있지 않으면 `--force` 없이는 중단)
2. `course`, `creator` 를 `TRUNCATE`
3. Creator 1,000건 단일 multi-value `INSERT`
4. `creator_id` 재조회
5. 분포대로 Course 10,000건을 1,000건 단위 multi-value `INSERT`
6. 카운트 검증 출력 (`creator=1000`, `course=10000`, `creators_with_course=950`)

## 검증 SQL

```sql
SELECT COUNT(*) FROM creator;                    -- 1000
SELECT COUNT(*) FROM course;                     -- 10000
SELECT COUNT(DISTINCT creator_id) FROM course;   -- 950

SELECT creator_id, COUNT(*) AS course_n
FROM course GROUP BY creator_id
ORDER BY course_n DESC LIMIT 10;
```

---

# Phase 2: Sales / Cancellation (`generate_transactions.py`)

**Phase 1이 먼저 적재돼 있어야 함** (course_id 1~10,000 참조).

## 산출 데이터

- `sales_record` **3,000,000건**
  - 무료(0원) 150,000건 + 유료 2,850,000건
  - `paid_at` ∈ [2025-06-01, 2026-05-24 23:59:59], 일자별 weight 선형 1→3 (최근일수록 증가)
  - `student_id` ∈ [1, 300,000] random
- `cancellation_record` **150,000건**
  - **유료 sales에만** 발생 (무료 sales 취소 없음)
  - 완전 취소 105,000건(70%) — `refund_amount = payment_amount`, 부모 sales 1건당 1건
  - 부분 환불 45,000건(30%) — 부모 sales 약 22,500개에 1~3건씩 누적, 누적 합 < `payment_amount`
  - `cancelled_at = paid_at + uniform(1초, min(30일, NOW − paid_at))`

## creator 활성 월 분포 (sparse)

각 creator마다 활성 월 set을 부여 → **비활성 월에는 그 creator의 어떤 course에서도 sales 발생하지 않음**. 결과적으로 settlement 테이블에서도 자연스럽게 빈 월이 생겨 (creator, target_month) 조합 일부가 누락됨.

| 분류 | creator 비율 | 휴지기 길이 |
|------|--------------|------------|
| 풀가동 | 60% (~600명) | 0개월 |
| 가벼운 휴지기 | 25% (~250명) | 1~3개월 |
| 중간 휴지기 | 12% (~120명) | 4~7개월 |
| 무거운 휴지기 | 3% (~30명) | 8~10개월 |

평균 활성 월: ~10.5 / 12개월. 비활성 월은 creator마다 균등 무작위로 선택.

## course 가격 분포 (시드 시점 1회 추첨, 동일 course의 모든 sales는 같은 가격)

| 등급 | course 비율 | 가격 범위 | 반올림 단위 |
|------|-------------|----------|------------|
| 무료 | 5% (500) | 0 | — |
| 입문 | 30% (~2,850) | 10,000~30,000원 | 1,000 |
| 일반 | 45% (~4,275) | 30,000~100,000원 | 1,000 |
| 중급 | 15% (~1,425) | 100,000~200,000원 | 5,000 |
| 고급 | 5% (~475) | 200,000~500,000원 | 10,000 |

유료 course의 sales 인기는 `weight = exp(rng.gauss(0,1))` long-tail. 일부 hot course가 많은 sales를 흡수.

## 실행

```bash
python script/generate_transactions.py
```

옵션:

| 옵션 | 설명 |
|------|------|
| `--dry-run` | 분포 미리보기, DB I/O 없음 |
| `--seed N` | 난수 시드 변경 (기본 42) |

> 매 실행마다 cancellation → sales 순으로 무조건 TRUNCATE 후 재INSERT. creator/course는 절대 건드리지 않는다.

## 멱등성

1. `SELECT course_id FROM course` (10,000 id 확인)
2. course별 가격 추첨 → 무료/유료 분리
3. sales 3,000,000 메모리 생성 (course 가중 + 시간 가중)
4. cancellation 150,000 plan 생성 (부모 sales 인덱스 + ratio)
5. `SET FOREIGN_KEY_CHECKS=0` → TRUNCATE cancellation → TRUNCATE sales → `SET FOREIGN_KEY_CHECKS=1`
6. sales batch INSERT (5,000 × 600 batch)
7. `SELECT sales_record_id` 재조회 → 부모 인덱스 → 실제 id 매핑
8. cancellation batch INSERT (5,000 × 30 batch)
9. 카운트·invariant 검증 출력

**예상 소요**: 5~8분 / peak 메모리 ~300MB.

## 정상 case invariant (스크립트가 자동 검증)

- `payment_amount ≥ 0` (무료 = 0, 유료 > 0)
- `refund_amount > 0` 항상
- 같은 sales의 SUM(refund) ≤ `payment_amount`
- `paid_at < cancelled_at ≤ NOW()`
- 모든 시각 ∈ [2025-06-01, NOW()]
- 무료 sales는 cancellation 없음

## 검증 SQL

```sql
SELECT COUNT(*) FROM sales_record;                                  -- 3000000
SELECT COUNT(*) FROM cancellation_record;                           -- 150000
SELECT COUNT(*) FROM sales_record WHERE payment_amount = 0;         -- 150000

SELECT MIN(paid_at), MAX(paid_at) FROM sales_record;
SELECT MIN(cancelled_at), MAX(cancelled_at) FROM cancellation_record;

-- invariant: SUM(refund) ≤ payment per sales (위반=0)
SELECT COUNT(*) FROM (
  SELECT s.sales_record_id
  FROM sales_record s JOIN cancellation_record c ON c.sales_record_id = s.sales_record_id
  GROUP BY s.sales_record_id, s.payment_amount
  HAVING SUM(c.refund_amount) > s.payment_amount
) v;                                                                -- 0

-- 무료 sales 취소 없음 (위반=0)
SELECT COUNT(*) FROM cancellation_record c
JOIN sales_record s ON s.sales_record_id = c.sales_record_id
WHERE s.payment_amount = 0;                                         -- 0

-- 시간 invariant: paid_at < cancelled_at (위반=0)
SELECT COUNT(*) FROM cancellation_record c
JOIN sales_record s ON s.sales_record_id = c.sales_record_id
WHERE c.cancelled_at <= s.paid_at;                                  -- 0

-- 분포 확인: 가격 등급별 sales 분포
SELECT
  CASE
    WHEN payment_amount = 0           THEN '무료'
    WHEN payment_amount < 30000       THEN '입문'
    WHEN payment_amount < 100000      THEN '일반'
    WHEN payment_amount < 200000      THEN '중급'
    ELSE                                   '고급'
  END AS tier,
  COUNT(*) AS sales_n
FROM sales_record GROUP BY tier ORDER BY sales_n DESC;
```

---

# Phase 3: Settlement (`generate_settlements.py`)

**Phase 1 + Phase 2 적재 완료 후 실행.** 또한 `fee_policy` 1건(rate=0.2000, effective_from='2020-01-01')이 V5 마이그레이션으로 미리 적재돼 있어야 한다.

## 산출 데이터

- `settlement` — **(creator_id, `target_month`)** 단위 1행
  - 거래(sales 또는 cancellation 어느 쪽이든) 있는 조합만 생성 (빈 월 스킵)
  - 정산 대상 월: **2025-06 ~ 2026-04** (11개월). 진행 중인 월(오늘 2026-05-24 기준 2026-05)은 정산 대상 아님 → 행 생성하지 않음.
  - Phase 2의 creator 활성 월 분포(평균 활성 ~10.5/12)에 따라 일부 (creator, target_month) 조합은 자연스럽게 누락됨 → 약 **~9K건** 예상

## 상태 / 시각 정책

| `target_month` | `status` | `confirmed_at` | `paid_at` |
|--------------|----------|----------------|-----------|
| `<= 202603` (25/06 ~ 26/03, 10개월) | `PAID` | 정산 월 **다음 달 1일** + 임의 시간 | 정산 월 **다음 달 15일** + 임의 시간 |
| `== 202604` (26/04, 1개월) | `CONFIRMED` | 정산 월 **다음 달 1일** + 임의 시간 | `NULL` |
| `>= 202605` (진행 중) | — | 행 생성하지 않음 | — |

예: `target_month='202506'` → `confirmed_at` ∈ [2025-07-01 00:00:00, 2025-07-01 23:59:59.999999], `paid_at` ∈ [2025-07-15 00:00:00, 2025-07-15 23:59:59.999999]. `target_month='202604'` → `confirmed_at` ∈ 2026-05-01 일자 내 임의 시각.

## 산식

```
total_sales        = SUM(sales_record.payment_amount)        -- paid_at의 월로 그룹
total_refund       = SUM(cancellation_record.refund_amount)  -- cancelled_at의 월로 그룹
net_sales          = total_sales - total_refund
fee_rate           = 0.2000  (fee_policy V5 초기값)
platform_fee       = net_sales < 0 ? 0 : ROUND(net_sales * 0.2, 0, HALF_UP)
expected_payout    = net_sales - platform_fee     -- 음수 그대로 유지 (정산액 0 이하도 적재)
sales_count        = COUNT(sales_record 행)
cancellation_count = COUNT(cancellation_record 행)
```

> 도메인 `MonthlySettlementCalculator`와 일치. 결제는 결제 월에, 취소는 취소 월에 집계되는 비대칭 정책 그대로.

## 실행

```bash
python script/generate_settlements.py
```

옵션:

| 옵션 | 설명 |
|------|------|
| `--dry-run` | 집계 + 행 생성까지만 수행, TRUNCATE/INSERT 생략 |
| `--seed N` | 시각 분포용 난수 시드 변경 (기본 42) |

## 멱등성

1. `sales_record JOIN course` 로 (creator_id, target_month) 집계 (count, SUM(payment_amount))
2. `cancellation_record JOIN sales_record JOIN course` 로 (creator_id, target_month) 집계 (count, SUM(refund_amount))
3. 두 집계의 key 합집합을 순회하며 settlement 행 in-memory 생성
4. `TRUNCATE settlement`
5. 1,000건 단위 multi-value INSERT
6. 카운트 + invariant 검증 출력

## 검증 SQL

```sql
SELECT COUNT(*) FROM settlement;

SELECT status, COUNT(*) FROM settlement GROUP BY status;
-- PAID      : 25/06 ~ 26/03 행 (10/11 ≒ 91%)
-- CONFIRMED : 26/04 행      (1/11 ≒ 9%)

-- 산식 invariant (모두 0)
SELECT COUNT(*) FROM settlement WHERE net_sales <> total_sales - total_refund;
SELECT COUNT(*) FROM settlement WHERE net_sales < 0 AND platform_fee <> 0;

-- 상태별 시각 invariant (모두 0)
SELECT COUNT(*) FROM settlement WHERE status = 'PAID'      AND (confirmed_at IS NULL OR paid_at IS NULL);
SELECT COUNT(*) FROM settlement WHERE status = 'CONFIRMED' AND (confirmed_at IS NULL OR paid_at IS NOT NULL);

-- 경계 invariant (모두 0)
SELECT COUNT(*) FROM settlement WHERE `target_month` <= '202603' AND status <> 'PAID';
SELECT COUNT(*) FROM settlement WHERE `target_month` =  '202604' AND status <> 'CONFIRMED';
SELECT COUNT(*) FROM settlement WHERE `target_month` >  '202604';   -- 진행 중인 월은 정산 대상 아님

-- 월별 / 상태별 분포
SELECT `target_month`, status, COUNT(*) AS n
FROM settlement
GROUP BY `target_month`, status
ORDER BY `target_month`, status;

-- 정산액(net) 분포 — 음수 행도 그대로 유지되어야 함
SELECT
  CASE
    WHEN expected_payout <  0     THEN '음수'
    WHEN expected_payout =  0     THEN '0'
    WHEN expected_payout <  100000 THEN '~10만'
    WHEN expected_payout < 1000000 THEN '~100만'
    ELSE                                '100만+'
  END AS bucket,
  COUNT(*) AS n
FROM settlement GROUP BY bucket ORDER BY n DESC;
```
