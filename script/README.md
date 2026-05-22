# Seed Data Scripts

개발/시연용 데이터를 로컬 docker MySQL에 적재한다. 두 단계:

- **Phase 1** `generate_data.py` — Creator 1,000 + Course 10,000
- **Phase 2** `generate_transactions.py` — Sales 1,000,000 + Cancellation 50,000

두 스크립트는 완전히 독립이며 서로 import하지 않는다. (utility 함수 코드 중복 허용)

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

- `sales_record` **1,000,000건**
  - 무료(0원) 50,000건 + 유료 950,000건
  - `paid_at` ∈ [2025-06-01, 2026-05-22 23:59:59], 일자별 weight 선형 1→3 (최근일수록 증가)
  - `student_id` ∈ [1, 300,000] random
- `cancellation_record` **50,000건**
  - **유료 sales에만** 발생 (무료 sales 취소 없음)
  - 완전 취소 35,000건(70%) — `refund_amount = payment_amount`, 부모 sales 1건당 1건
  - 부분 환불 15,000건(30%) — 부모 sales 약 7,500개에 1~3건씩 누적, 누적 합 < `payment_amount`
  - `cancelled_at = paid_at + uniform(1초, min(30일, NOW − paid_at))`

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
3. sales 1,000,000 메모리 생성 (course 가중 + 시간 가중)
4. cancellation 50,000 plan 생성 (부모 sales 인덱스 + ratio)
5. `SET FOREIGN_KEY_CHECKS=0` → TRUNCATE cancellation → TRUNCATE sales → `SET FOREIGN_KEY_CHECKS=1`
6. sales batch INSERT (5,000 × 200 batch)
7. `SELECT sales_record_id` 재조회 → 부모 인덱스 → 실제 id 매핑
8. cancellation batch INSERT (5,000 × 10 batch)
9. 카운트·invariant 검증 출력

**예상 소요**: 1~2분 / peak 메모리 ~100MB.

## 정상 case invariant (스크립트가 자동 검증)

- `payment_amount ≥ 0` (무료 = 0, 유료 > 0)
- `refund_amount > 0` 항상
- 같은 sales의 SUM(refund) ≤ `payment_amount`
- `paid_at < cancelled_at ≤ NOW()`
- 모든 시각 ∈ [2025-06-01, NOW()]
- 무료 sales는 cancellation 없음

## 검증 SQL

```sql
SELECT COUNT(*) FROM sales_record;                                  -- 1000000
SELECT COUNT(*) FROM cancellation_record;                           -- 50000
SELECT COUNT(*) FROM sales_record WHERE payment_amount = 0;         -- 50000

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
