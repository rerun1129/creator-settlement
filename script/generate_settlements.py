#!/usr/bin/env python3
"""Settlement seed data generator for creator-settlement.

Usage:
    python script/generate_settlements.py             # 실제 적재
    python script/generate_settlements.py --dry-run   # 집계 후 INSERT 직전까지 (DB write 없음)
    python script/generate_settlements.py --seed 7    # 시드 변경 (시각 분포용)

전제:
  - course / creator / sales_record / cancellation_record 적재 완료
  - fee_policy V5 초기값 (rate=0.2000, effective_from='2020-01-01') 단일 정책 그대로 적용

생성 정책:
  - (creator_id, year_month) 단위 1행
  - 거래(sales 또는 cancellation) 발생한 조합만 생성 (빈 월 스킵)
  - year_month > '202604' (진행 중인 월) 은 정산 대상 아님 → 행 생성하지 않음
  - year_month <= '202603' → PAID  (confirmed_at + paid_at 모두 채움)
  - year_month == '202604' → CONFIRMED (confirmed_at만 채움, paid_at NULL)
  - confirmed_at = 정산 월의 **다음 달 1일** + 임의 시간 (00:00:00.000000 ~ 23:59:59.999999)
  - paid_at      = 정산 월의 **다음 달 15일** + 임의 시간 (PAID 한정)
  - 산식: net = sales - refund, fee = net<0 ? 0 : ROUND(net*0.2, 0, HALF_UP), payout = net - fee

기존 script/generate_data.py, script/generate_transactions.py와 완전히 독립적인 단일 스크립트.
utility(run_sql / load_env)도 자체 정의 — 외부 모듈 의존은 python-dotenv 1개.
"""

from __future__ import annotations

import argparse
import os
import random
import subprocess
import sys
from datetime import datetime, timedelta
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path

from dotenv import load_dotenv

for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8", errors="replace")
    except (AttributeError, OSError):
        pass

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_CONTAINER = "creator-settlement-mysql"

FEE_RATE = Decimal("0.2000")
PAID_MAX_YM = "202603"
SETTLEMENT_MAX_YM = "202604"
BATCH = 1_000


def load_env() -> dict:
    load_dotenv(PROJECT_ROOT / ".env")
    return {
        "container": os.getenv("DB_CONTAINER", DEFAULT_CONTAINER),
        "user": os.getenv("MYSQL_USER", "creator"),
        "password": os.getenv("MYSQL_PASSWORD"),
        "database": os.getenv("MYSQL_DATABASE", "creator_settlement"),
    }


def run_sql(env: dict, sql: str, capture: bool = False) -> str:
    cmd = [
        "docker", "exec", "-i", env["container"],
        "mysql", "--default-character-set=utf8mb4",
        "-u", env["user"], f"-p{env['password']}",
        env["database"],
    ]
    if capture:
        cmd.insert(5, "-BN")
    proc = subprocess.run(
        cmd,
        input=sql.encode("utf-8"),
        capture_output=True,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(
            "mysql failed:\n"
            + proc.stderr.decode("utf-8", errors="replace")
        )
    return proc.stdout.decode("utf-8", errors="replace")


def fetch_sales_aggregation(env: dict) -> dict[tuple[int, str], tuple[int, int]]:
    out = run_sql(
        env,
        "SELECT c.creator_id, DATE_FORMAT(s.paid_at, '%Y%m'), "
        "COUNT(*), CAST(SUM(s.payment_amount) AS CHAR) "
        "FROM sales_record s "
        "JOIN course c ON c.course_id = s.course_id "
        "GROUP BY c.creator_id, DATE_FORMAT(s.paid_at, '%Y%m');",
        capture=True,
    )
    result: dict[tuple[int, str], tuple[int, int]] = {}
    for line in out.splitlines():
        parts = line.split("\t")
        if len(parts) != 4:
            continue
        creator_id = int(parts[0])
        ym = parts[1]
        cnt = int(parts[2])
        total = int(Decimal(parts[3]))
        result[(creator_id, ym)] = (cnt, total)
    return result


def fetch_cancellation_aggregation(env: dict) -> dict[tuple[int, str], tuple[int, int]]:
    out = run_sql(
        env,
        "SELECT c.creator_id, DATE_FORMAT(can.cancelled_at, '%Y%m'), "
        "COUNT(*), CAST(SUM(can.refund_amount) AS CHAR) "
        "FROM cancellation_record can "
        "JOIN sales_record s ON s.sales_record_id = can.sales_record_id "
        "JOIN course c ON c.course_id = s.course_id "
        "GROUP BY c.creator_id, DATE_FORMAT(can.cancelled_at, '%Y%m');",
        capture=True,
    )
    result: dict[tuple[int, str], tuple[int, int]] = {}
    for line in out.splitlines():
        parts = line.split("\t")
        if len(parts) != 4:
            continue
        creator_id = int(parts[0])
        ym = parts[1]
        cnt = int(parts[2])
        total = int(Decimal(parts[3]))
        result[(creator_id, ym)] = (cnt, total)
    return result


def calc_platform_fee(net_sales: int) -> int:
    if net_sales < 0:
        return 0
    return int((Decimal(net_sales) * FEE_RATE).quantize(Decimal(1), rounding=ROUND_HALF_UP))


def next_month_day(ym: str, day: int) -> datetime:
    year = int(ym[:4])
    month = int(ym[4:])
    if month == 12:
        return datetime(year + 1, 1, day)
    return datetime(year, month + 1, day)


def random_time_on(rng: random.Random, base: datetime) -> datetime:
    return base + timedelta(
        seconds=rng.randint(0, 86_399),
        microseconds=rng.randint(0, 999_999),
    )


def build_settlement_rows(
    rng: random.Random,
    sales_agg: dict[tuple[int, str], tuple[int, int]],
    cancel_agg: dict[tuple[int, str], tuple[int, int]],
) -> list[tuple]:
    keys = set(sales_agg.keys()) | set(cancel_agg.keys())
    rows: list[tuple] = []
    for creator_id, ym in sorted(keys):
        if ym > SETTLEMENT_MAX_YM:
            continue
        sales_count, total_sales = sales_agg.get((creator_id, ym), (0, 0))
        cancel_count, total_refund = cancel_agg.get((creator_id, ym), (0, 0))
        net_sales = total_sales - total_refund
        platform_fee = calc_platform_fee(net_sales)
        expected_payout = net_sales - platform_fee

        if ym <= PAID_MAX_YM:
            status = "PAID"
            confirmed_at = random_time_on(rng, next_month_day(ym, 1))
            paid_at = random_time_on(rng, next_month_day(ym, 15))
        else:
            status = "CONFIRMED"
            confirmed_at = random_time_on(rng, next_month_day(ym, 1))
            paid_at = None

        rows.append((
            creator_id, ym, status,
            total_sales, total_refund, net_sales,
            FEE_RATE, platform_fee, expected_payout,
            sales_count, cancel_count,
            confirmed_at, paid_at,
        ))
    return rows


def truncate_settlement(env: dict) -> None:
    run_sql(env, "TRUNCATE settlement;")


def insert_settlements(env: dict, rows: list[tuple]) -> None:
    total = len(rows)
    for start in range(0, total, BATCH):
        chunk = rows[start:start + BATCH]
        values_parts: list[str] = []
        for (creator_id, ym, status, total_sales, total_refund, net_sales,
             fee_rate, platform_fee, expected_payout,
             sales_count, cancel_count,
             confirmed_at, paid_at) in chunk:
            confirmed_str = f"'{confirmed_at.strftime('%Y-%m-%d %H:%M:%S.%f')}'"
            paid_str = "NULL" if paid_at is None else f"'{paid_at.strftime('%Y-%m-%d %H:%M:%S.%f')}'"
            values_parts.append(
                f"({creator_id},'{ym}','{status}',"
                f"{total_sales},{total_refund},{net_sales},"
                f"{fee_rate},{platform_fee},{expected_payout},"
                f"{sales_count},{cancel_count},"
                f"{confirmed_str},{paid_str})"
            )
        values = ",".join(values_parts)
        run_sql(
            env,
            "INSERT INTO settlement (creator_id, `year_month`, status, "
            "total_sales, total_refund, net_sales, "
            "fee_rate, platform_fee, expected_payout, "
            "sales_count, cancellation_count, "
            "confirmed_at, paid_at) VALUES " + values + ";"
        )
        done = min(start + BATCH, total)
        print(f"[settlement] inserted {done}/{total}")


def summarize_rows(rows: list[tuple]) -> None:
    if not rows:
        print("[summary] no settlement rows to generate.")
        return
    by_ym: dict[str, int] = {}
    by_status: dict[str, int] = {}
    negative_net = 0
    for (_cid, ym, status, _ts, _tr, net_sales,
         _fr, _fee, _payout, _sc, _cc, _ca, _pa) in rows:
        by_ym[ym] = by_ym.get(ym, 0) + 1
        by_status[status] = by_status.get(status, 0) + 1
        if net_sales < 0:
            negative_net += 1
    print(f"[summary] total settlement rows: {len(rows)}")
    print(f"[summary] by status: {by_status}")
    print(f"[summary] negative net_sales rows (platform_fee forced to 0): {negative_net}")
    print("[summary] by year_month:")
    for ym in sorted(by_ym):
        print(f"           {ym}: {by_ym[ym]}")


def verify(env: dict) -> dict:
    total = int(run_sql(env, "SELECT COUNT(*) FROM settlement;", capture=True).strip() or 0)
    status_rows = run_sql(
        env,
        "SELECT status, COUNT(*) FROM settlement GROUP BY status ORDER BY status;",
        capture=True,
    )
    status_dist: dict[str, int] = {}
    for line in status_rows.splitlines():
        parts = line.split("\t")
        if len(parts) == 2:
            status_dist[parts[0]] = int(parts[1])
    net_violators = int(run_sql(
        env,
        "SELECT COUNT(*) FROM settlement WHERE net_sales <> total_sales - total_refund;",
        capture=True,
    ).strip() or 0)
    fee_violators = int(run_sql(
        env,
        "SELECT COUNT(*) FROM settlement WHERE net_sales < 0 AND platform_fee <> 0;",
        capture=True,
    ).strip() or 0)
    paid_time_violators = int(run_sql(
        env,
        "SELECT COUNT(*) FROM settlement WHERE status = 'PAID' "
        "AND (confirmed_at IS NULL OR paid_at IS NULL);",
        capture=True,
    ).strip() or 0)
    confirmed_time_violators = int(run_sql(
        env,
        "SELECT COUNT(*) FROM settlement WHERE status = 'CONFIRMED' "
        "AND (confirmed_at IS NULL OR paid_at IS NOT NULL);",
        capture=True,
    ).strip() or 0)
    boundary_paid_violators = int(run_sql(
        env,
        f"SELECT COUNT(*) FROM settlement WHERE `year_month` <= '{PAID_MAX_YM}' AND status <> 'PAID';",
        capture=True,
    ).strip() or 0)
    boundary_confirmed_violators = int(run_sql(
        env,
        f"SELECT COUNT(*) FROM settlement WHERE `year_month` > '{PAID_MAX_YM}' AND status <> 'CONFIRMED';",
        capture=True,
    ).strip() or 0)
    unfinished_month_violators = int(run_sql(
        env,
        f"SELECT COUNT(*) FROM settlement WHERE `year_month` > '{SETTLEMENT_MAX_YM}';",
        capture=True,
    ).strip() or 0)
    return {
        "total": total,
        "status_dist": status_dist,
        "net_violators": net_violators,
        "fee_violators": fee_violators,
        "paid_time_violators": paid_time_violators,
        "confirmed_time_violators": confirmed_time_violators,
        "boundary_paid_violators": boundary_paid_violators,
        "boundary_confirmed_violators": boundary_confirmed_violators,
        "unfinished_month_violators": unfinished_month_violators,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description="Seed settlement table from sales_record/cancellation_record")
    parser.add_argument("--dry-run", action="store_true", help="aggregate + build only, skip TRUNCATE/INSERT")
    parser.add_argument("--seed", type=int, default=42, help="random seed for time-of-day distribution (default: 42)")
    args = parser.parse_args()

    rng = random.Random(args.seed)

    env = load_env()
    if not env["password"]:
        raise SystemExit("MYSQL_PASSWORD missing in .env")

    print("[step] aggregating sales_record by (creator_id, year_month) ...")
    sales_agg = fetch_sales_aggregation(env)
    print(f"[step] sales groups: {len(sales_agg)}")

    print("[step] aggregating cancellation_record by (creator_id, year_month) ...")
    cancel_agg = fetch_cancellation_aggregation(env)
    print(f"[step] cancellation groups: {len(cancel_agg)}")

    if not sales_agg and not cancel_agg:
        raise SystemExit("no sales_record/cancellation_record found. run generate_transactions.py first.")

    print("[step] building settlement rows ...")
    rows = build_settlement_rows(rng, sales_agg, cancel_agg)
    summarize_rows(rows)

    if args.dry_run:
        print("[dry-run] skipping TRUNCATE/INSERT.")
        return

    print("[step] TRUNCATE settlement ...")
    truncate_settlement(env)

    print("[step] INSERT settlement rows ...")
    insert_settlements(env, rows)

    print("[step] verifying ...")
    result = verify(env)
    print(f"[done] settlement rows: {result['total']:,}")
    print(f"[done] status distribution: {result['status_dist']}")
    print(
        f"[invariant] net_sales mismatch={result['net_violators']} (expect 0), "
        f"fee_on_negative_net={result['fee_violators']} (expect 0), "
        f"paid_missing_time={result['paid_time_violators']} (expect 0), "
        f"confirmed_time_violators={result['confirmed_time_violators']} (expect 0), "
        f"boundary_paid_violators={result['boundary_paid_violators']} (expect 0), "
        f"boundary_confirmed_violators={result['boundary_confirmed_violators']} (expect 0), "
        f"unfinished_month_violators={result['unfinished_month_violators']} (expect 0)"
    )


if __name__ == "__main__":
    main()
