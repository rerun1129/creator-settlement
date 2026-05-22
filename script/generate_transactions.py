#!/usr/bin/env python3
"""Sales/Cancellation seed data generator for creator-settlement.

Usage:
    python script/generate_transactions.py             # 실제 적재
    python script/generate_transactions.py --dry-run   # 분포 미리보기 (DB I/O 없음)
    python script/generate_transactions.py --seed 7    # 시드 변경

기존 script/generate_data.py와 완전히 독립적인 단일 스크립트.
utility(run_sql / load_env / sql_escape)도 자체 정의 — 외부 모듈 의존은 python-dotenv 1개.
"""

from __future__ import annotations

import argparse
import math
import os
import random
import subprocess
import sys
from collections import defaultdict
from datetime import datetime, timedelta
from pathlib import Path

from dotenv import load_dotenv

for _stream in (sys.stdout, sys.stderr):
    try:
        _stream.reconfigure(encoding="utf-8", errors="replace")
    except (AttributeError, OSError):
        pass

PROJECT_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_CONTAINER = "creator-settlement-mysql"

TARGET_SALES = 1_000_000
TARGET_CANCELLATIONS = 50_000
FREE_SALES = 50_000
PAID_SALES = TARGET_SALES - FREE_SALES

FULL_CANCEL = 35_000
PARTIAL_CANCEL = TARGET_CANCELLATIONS - FULL_CANCEL

PAID_START = datetime(2025, 6, 1, 0, 0, 0)
PAID_END = datetime(2026, 5, 22, 23, 59, 59)

STUDENT_RANGE_MAX = 300_000
BATCH = 5_000

PRICE_TIERS = [
    (0.05, 0, 0, 1, True),
    (0.30, 10_000, 30_000, 1_000, False),
    (0.45, 30_000, 100_000, 1_000, False),
    (0.15, 100_000, 200_000, 5_000, False),
    (0.05, 200_000, 500_000, 10_000, False),
]


def load_env() -> dict:
    load_dotenv(PROJECT_ROOT / ".env")
    return {
        "container": os.getenv("DB_CONTAINER", DEFAULT_CONTAINER),
        "user": os.getenv("MYSQL_USER", "creator"),
        "password": os.getenv("MYSQL_PASSWORD"),
        "database": os.getenv("MYSQL_DATABASE", "creator_settlement"),
    }


def sql_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "\\'")


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


def assign_course_prices(rng: random.Random, course_ids: list[int]) -> dict[int, int]:
    n = len(course_ids)
    sizes = [round(n * ratio) for ratio, *_ in PRICE_TIERS]
    while sum(sizes) > n:
        sizes[max(range(len(sizes)), key=lambda i: sizes[i])] -= 1
    while sum(sizes) < n:
        sizes[max(range(len(sizes)), key=lambda i: sizes[i])] += 1

    shuffled = list(course_ids)
    rng.shuffle(shuffled)
    prices: dict[int, int] = {}
    pos = 0
    for size, (_, low, high, unit, is_free) in zip(sizes, PRICE_TIERS):
        for cid in shuffled[pos:pos + size]:
            if is_free:
                prices[cid] = 0
            else:
                slots = (high - low) // unit + 1
                prices[cid] = low + rng.randrange(slots) * unit
        pos += size
    return prices


def generate_daily_weights(start: datetime, end: datetime) -> tuple[list[datetime], list[float]]:
    days = (end.date() - start.date()).days + 1
    bases = [start + timedelta(days=i) for i in range(days)]
    weights = [1.0 + 2.0 * (i / max(days - 1, 1)) for i in range(days)]
    return bases, weights


def random_paid_at(rng: random.Random, bases: list[datetime], weights: list[float], end: datetime) -> datetime:
    day = rng.choices(bases, weights=weights, k=1)[0]
    cap_seconds = min(86399.999999, (end - day).total_seconds())
    if cap_seconds <= 0:
        return day
    return day + timedelta(seconds=rng.uniform(0, cap_seconds))


def generate_sales_rows(
    rng: random.Random, course_prices: dict[int, int]
) -> tuple[list[tuple[int, int, int, datetime]], list[int]]:
    free_ids = [cid for cid, price in course_prices.items() if price == 0]
    paid_ids = [cid for cid, price in course_prices.items() if price > 0]
    paid_weights = [math.exp(rng.gauss(0, 1)) for _ in paid_ids]
    bases, day_weights = generate_daily_weights(PAID_START, PAID_END)

    rows: list[tuple[int, int, int, datetime]] = []
    for _ in range(FREE_SALES):
        cid = rng.choice(free_ids)
        sid = rng.randint(1, STUDENT_RANGE_MAX)
        rows.append((cid, sid, 0, random_paid_at(rng, bases, day_weights, PAID_END)))

    paid_cids = rng.choices(paid_ids, weights=paid_weights, k=PAID_SALES)
    for cid in paid_cids:
        sid = rng.randint(1, STUDENT_RANGE_MAX)
        rows.append((cid, sid, course_prices[cid], random_paid_at(rng, bases, day_weights, PAID_END)))

    rng.shuffle(rows)
    paid_indexes = [i for i, r in enumerate(rows) if r[2] > 0]
    return rows, paid_indexes


def generate_cancellation_plan(rng: random.Random, paid_indexes: list[int]) -> list[tuple[int, float]]:
    available = list(paid_indexes)
    rng.shuffle(available)
    if len(available) < FULL_CANCEL + 1:
        raise SystemExit(f"not enough paid sales for cancellation parents: {len(available)}")

    full_parents = available[:FULL_CANCEL]
    remaining = available[FULL_CANCEL:]

    partial_counts: list[int] = []
    total = 0
    while total < PARTIAL_CANCEL:
        n = rng.randint(1, 3)
        if total + n > PARTIAL_CANCEL:
            n = PARTIAL_CANCEL - total
        partial_counts.append(n)
        total += n

    if len(remaining) < len(partial_counts):
        raise SystemExit(f"not enough sales for partial-refund parents")

    plan: list[tuple[int, float]] = [(idx, 1.0) for idx in full_parents]
    for parent_idx, n in zip(remaining[:len(partial_counts)], partial_counts):
        cumulative = 0.0
        for _ in range(n):
            r = rng.uniform(0.10, 0.40)
            if cumulative + r > 0.90:
                r = max(0.05, 0.90 - cumulative)
            cumulative += r
            plan.append((parent_idx, r))
    return plan


def truncate_target_tables(env: dict) -> None:
    run_sql(
        env,
        "SET FOREIGN_KEY_CHECKS=0; "
        "TRUNCATE cancellation_record; TRUNCATE sales_record; "
        "SET FOREIGN_KEY_CHECKS=1;",
    )


def insert_sales(env: dict, rows: list[tuple[int, int, int, datetime]]) -> None:
    total = len(rows)
    for start in range(0, total, BATCH):
        chunk = rows[start:start + BATCH]
        values = ",".join(
            f"({cid},{sid},{amt},'{paid_at.strftime('%Y-%m-%d %H:%M:%S.%f')}')"
            for cid, sid, amt, paid_at in chunk
        )
        run_sql(env, f"INSERT INTO sales_record (course_id, student_id, payment_amount, paid_at) VALUES {values};")
        done = min(start + BATCH, total)
        if done % (BATCH * 10) == 0 or done == total:
            print(f"[sales] inserted {done}/{total}")


def fetch_sales_ids(env: dict) -> list[int]:
    out = run_sql(env, "SELECT sales_record_id FROM sales_record ORDER BY sales_record_id;", capture=True)
    return [int(line) for line in out.splitlines() if line.strip()]


def build_cancellation_rows(
    rng: random.Random,
    plan: list[tuple[int, float]],
    sales_rows: list[tuple[int, int, int, datetime]],
    sales_ids: list[int],
) -> list[tuple[int, int, datetime]]:
    cancellation_rows: list[tuple[int, int, datetime]] = []
    for parent_idx, ratio in plan:
        _cid, _sid, payment_amount, paid_at = sales_rows[parent_idx]
        if ratio >= 1.0:
            refund = payment_amount
        else:
            refund = max(1, int(round(payment_amount * ratio)))
        max_seconds = min(30 * 86400, (PAID_END - paid_at).total_seconds())
        if max_seconds <= 1:
            cancelled_at = min(paid_at + timedelta(seconds=1), PAID_END)
        else:
            cancelled_at = paid_at + timedelta(seconds=rng.uniform(1, max_seconds))
            if cancelled_at > PAID_END:
                cancelled_at = PAID_END
        cancellation_rows.append((sales_ids[parent_idx], refund, cancelled_at))

    sums: dict[int, int] = defaultdict(int)
    payments: dict[int, int] = {}
    for (parent_idx, _), (sales_id, refund, _ca) in zip(plan, cancellation_rows):
        sums[sales_id] += refund
        payments[sales_id] = sales_rows[parent_idx][2]
    violators = [sid for sid, total in sums.items() if total > payments[sid]]
    if violators:
        raise SystemExit(f"invariant violation: {len(violators)} parents have SUM(refund) > payment")

    return cancellation_rows


def insert_cancellations(env: dict, rows: list[tuple[int, int, datetime]]) -> None:
    total = len(rows)
    for start in range(0, total, BATCH):
        chunk = rows[start:start + BATCH]
        values = ",".join(
            f"({sid},{ref},'{ca.strftime('%Y-%m-%d %H:%M:%S.%f')}')"
            for sid, ref, ca in chunk
        )
        run_sql(env, f"INSERT INTO cancellation_record (sales_record_id, refund_amount, cancelled_at) VALUES {values};")
        print(f"[cancel] inserted {min(start + BATCH, total)}/{total}")


def verify(env: dict) -> dict:
    out = run_sql(
        env,
        "SELECT COUNT(*) FROM sales_record; "
        "SELECT COUNT(*) FROM cancellation_record; "
        "SELECT COUNT(*) FROM sales_record WHERE payment_amount = 0; "
        "SELECT COUNT(*) FROM ("
        "  SELECT s.sales_record_id FROM sales_record s "
        "    JOIN cancellation_record c ON c.sales_record_id = s.sales_record_id "
        "  GROUP BY s.sales_record_id, s.payment_amount "
        "  HAVING SUM(c.refund_amount) > s.payment_amount) v; "
        "SELECT COUNT(*) FROM cancellation_record c "
        "  JOIN sales_record s ON s.sales_record_id = c.sales_record_id "
        "  WHERE s.payment_amount = 0; "
        "SELECT COUNT(*) FROM cancellation_record c "
        "  JOIN sales_record s ON s.sales_record_id = c.sales_record_id "
        "  WHERE c.cancelled_at <= s.paid_at;",
        capture=True,
    )
    lines = [line for line in out.splitlines() if line.strip()]
    return {
        "sales": int(lines[0]),
        "cancellations": int(lines[1]),
        "free_sales": int(lines[2]),
        "refund_sum_violators": int(lines[3]),
        "free_with_cancellation": int(lines[4]),
        "time_violators": int(lines[5]),
    }


def preview(rng: random.Random) -> None:
    course_ids = list(range(1, 10_001))
    prices = assign_course_prices(rng, course_ids)
    free_n = sum(1 for p in prices.values() if p == 0)
    paid_n = len(prices) - free_n
    samples = rng.sample(course_ids, 10)
    print(f"[plan] course: free={free_n}, paid={paid_n}")
    print(f"[plan] price samples (course_id → price): {[(c, prices[c]) for c in samples]}")
    print(f"[plan] sales target: {TARGET_SALES} (free {FREE_SALES} + paid {PAID_SALES})")
    print(f"[plan] cancellation target: {TARGET_CANCELLATIONS} "
          f"(full {FULL_CANCEL} + partial {PARTIAL_CANCEL})")
    print(f"[plan] paid_at range: {PAID_START} ~ {PAID_END}")
    print(f"[plan] student_id range: 1 ~ {STUDENT_RANGE_MAX:,}")
    print(f"[plan] batch size: {BATCH}")


def main() -> None:
    parser = argparse.ArgumentParser(description="Seed sales_record / cancellation_record")
    parser.add_argument("--dry-run", action="store_true", help="distribution preview only, no DB I/O")
    parser.add_argument("--seed", type=int, default=42, help="random seed (default: 42)")
    args = parser.parse_args()

    rng = random.Random(args.seed)

    if args.dry_run:
        preview(rng)
        print("[dry-run] no DB I/O performed.")
        return

    env = load_env()
    if not env["password"]:
        raise SystemExit("MYSQL_PASSWORD missing in .env")

    print("[step] fetching course_ids ...")
    out = run_sql(env, "SELECT course_id FROM course ORDER BY course_id;", capture=True)
    course_ids = [int(line) for line in out.splitlines() if line.strip()]
    if not course_ids:
        raise SystemExit("course table is empty. run generate_data.py first.")
    print(f"[step] courses loaded: {len(course_ids)}")

    prices = assign_course_prices(rng, course_ids)
    free_n = sum(1 for p in prices.values() if p == 0)
    print(f"[step] prices assigned: free={free_n}, paid={len(prices) - free_n}")

    print("[step] generating sales rows in memory ...")
    sales_rows, paid_indexes = generate_sales_rows(rng, prices)
    print(f"[step] sales rows: {len(sales_rows)} (paid candidates: {len(paid_indexes)})")

    print("[step] building cancellation plan ...")
    plan = generate_cancellation_plan(rng, paid_indexes)
    print(f"[step] cancellation plan: {len(plan)} records")

    print("[step] TRUNCATE cancellation_record, sales_record ...")
    truncate_target_tables(env)

    print("[step] INSERT sales rows ...")
    insert_sales(env, sales_rows)

    print("[step] fetching sales_record_id mapping ...")
    sales_ids = fetch_sales_ids(env)
    if len(sales_ids) != len(sales_rows):
        raise SystemExit(f"sales count mismatch: {len(sales_ids)} vs {len(sales_rows)}")

    print("[step] building cancellation rows with real sales_record_ids ...")
    cancellation_rows = build_cancellation_rows(rng, plan, sales_rows, sales_ids)

    print("[step] INSERT cancellation rows ...")
    insert_cancellations(env, cancellation_rows)

    print("[step] verifying ...")
    result = verify(env)
    print(
        f"[done] sales={result['sales']:,}, cancellations={result['cancellations']:,}, "
        f"free_sales={result['free_sales']:,}"
    )
    print(
        f"[invariant] refund>payment violators={result['refund_sum_violators']} (expect 0), "
        f"free_with_cancellation={result['free_with_cancellation']} (expect 0), "
        f"time_violators={result['time_violators']} (expect 0)"
    )


if __name__ == "__main__":
    main()
