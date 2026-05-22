#!/usr/bin/env python3
"""Creator/Course seed data generator for creator-settlement.

Usage:
    python script/generate_data.py             # 실제 적재
    python script/generate_data.py --dry-run   # 분포만 확인
    python script/generate_data.py --force     # sales_record/cancellation_record 까지 비우고 적재
    python script/generate_data.py --seed 7    # 재현용 시드 변경

호스트 Python 라이브러리(PyMySQL / mysql-connector-python)와 MySQL 8.4 / Python 3.14
조합에서 caching_sha2_password 협상이 깨지는 사례가 있어, 컨테이너 내부의 mysql CLI를
`docker exec -i` 로 호출해서 SQL을 stdin으로 전달한다.
"""

from __future__ import annotations

import argparse
import os
import random
import subprocess
import sys
from pathlib import Path

from dotenv import load_dotenv

for stream in (sys.stdout, sys.stderr):
    try:
        stream.reconfigure(encoding="utf-8", errors="replace")
    except (AttributeError, OSError):
        pass

PROJECT_ROOT = Path(__file__).resolve().parent.parent

TARGET_CREATORS = 1000
TARGET_COURSES = 10000
EMPTY_CREATORS = 50
DEFAULT_CONTAINER = "creator-settlement-mysql"

CREATOR_BASE = [
    "code", "dev", "algo", "data", "cloud", "web", "mobile", "spring",
    "python", "java", "kotlin", "react", "vue", "next", "node", "django",
    "fastapi", "ml", "ai", "deep", "rust", "go", "ts", "frontend", "backend",
    "fullstack", "devops", "infra", "test", "qa",
    "kim", "lee", "park", "choi", "jung", "kang", "yoon", "jang", "han",
    "jay", "jin", "hoon", "min", "sue", "young", "joon", "tae", "sung",
    "byte", "stack", "pixel", "lab", "school", "academy", "factory", "studio",
]

CREATOR_SUFFIX = [
    "_master", "_factory", "_lab", "_pro", "_dev", "_school", "_academy",
    "_coding", "_study", "_studio", "_class", "_team", "_works", "_io",
    "", "", "",
]

KOREAN_SURNAME = [
    "김", "이", "박", "최", "정", "강", "윤", "임", "한", "송", "황", "권",
    "안", "조", "장",
]

KOREAN_GIVEN = [
    "영한", "재성", "도현", "민수", "지훈", "현우", "다은", "지윤", "서연",
    "하준", "도윤", "수빈", "예진", "주호", "태경", "성훈", "은지", "혜린",
    "준호", "찬영", "지원", "윤서", "서준", "민재", "수현", "지호",
]

KOREAN_TECH_ADJ = [
    "널널한", "졸린", "게으른", "친절한", "꼼꼼한", "성실한", "노력하는",
    "쉬운", "수상한", "심심한", "느긋한", "낭만있는", "허당", "꿈꾸는",
    "달리는", "현실적인", "혼자하는", "쉬어가는", "야근하는", "퇴근한",
    "잠못드는", "주말출근",
]

KOREAN_PERSONA = ["개발자", "코더", "프로그래머", "엔지니어"]

KOREAN_DEV_PREFIX = [
    "코딩", "데브", "테크", "노마드", "디지털", "스마트", "리얼", "베이직",
    "프로", "심플", "패스트", "심야", "주말", "동네", "혼공", "생활",
    "초보", "꿈꾸는", "낭만",
]

KOREAN_DEV_SUFFIX = [
    "애플", "코더", "랩", "공방", "팩토리", "스쿨", "스토리", "캠프",
    "스튜디오", "노트", "공장", "연구실", "다이어리", "탐험대", "라운지",
    "타임", "키친",
]

KOREAN_OWNED_NOUN = ["코딩", "개발노트", "실험실", "다이어리", "라운지", "스튜디오"]

COURSE_FORMAT = [
    "한 번에 끝내는 {topic}",
    "처음 만나는 {topic}",
    "실무에서 바로 쓰는 {topic}",
    "{topic} 마스터 클래스",
    "{topic} 부트캠프",
    "{topic} 입문",
    "{topic} 심화",
    "{topic} 완전 정복",
    "현업 개발자가 알려주는 {topic}",
    "0부터 시작하는 {topic}",
    "주니어를 위한 {topic}",
    "{topic} 실전 프로젝트",
    "쉽게 배우는 {topic}",
    "딥다이브 {topic}",
    "{topic} 핵심 가이드",
    "{topic} 워크북",
    "{topic} 올인원 패키지",
    "이직을 위한 {topic}",
    "면접까지 챙기는 {topic}",
]

COURSE_TOPIC = [
    "Spring Boot", "Spring Boot 3", "Spring Security", "Spring Data JPA",
    "Spring Batch", "Spring Cloud", "Spring WebFlux",
    "Node.js", "NestJS", "Express", "FastAPI", "Django", "Flask",
    "Java", "Java 17", "Java 21", "Kotlin", "Kotlin Server", "Ruby on Rails",
    "React", "Next.js 14", "Vue 3", "Svelte", "Solid.js", "Angular",
    "TypeScript", "JavaScript ES2024", "Tailwind CSS", "Sass",
    "Redux", "Recoil", "Zustand", "React Query",
    "Flutter", "React Native", "Swift UI", "Jetpack Compose", "Android Kotlin",
    "Pandas", "NumPy", "PyTorch", "TensorFlow", "Hugging Face",
    "LangChain", "OpenAI API", "RAG", "Prompt Engineering", "MLOps",
    "Apache Spark", "Apache Kafka", "Airflow", "dbt",
    "Docker", "Kubernetes", "AWS", "GCP", "Azure", "Terraform",
    "Ansible", "Jenkins", "GitHub Actions", "ArgoCD", "Istio",
    "MySQL", "PostgreSQL", "MongoDB", "Redis", "Elasticsearch", "DynamoDB",
    "자료구조", "알고리즘", "운영체제", "네트워크", "데이터베이스 이론",
    "디자인 패턴", "객체지향 설계", "함수형 프로그래밍", "클린 코드", "리팩토링",
    "Figma", "UX 리서치", "프로덕트 디자인", "디자인 시스템",
    "SEO", "퍼포먼스 마케팅", "GA4", "프로덕트 매니지먼트", "스타트업 기획",
    "테스트 자동화", "TDD", "DDD", "MSA", "헥사고날 아키텍처",
    "코딩 인터뷰", "이직 준비", "포트폴리오 만들기",
]


def generate_creator_names(rng: random.Random, n: int) -> list[str]:
    seen: set[str] = set()
    names: list[str] = []
    while len(names) < n:
        style = rng.random()
        if style < 0.30:
            candidate = f"{rng.choice(KOREAN_SURNAME)}{rng.choice(KOREAN_GIVEN)}"
        elif style < 0.55:
            candidate = f"{rng.choice(KOREAN_TECH_ADJ)}{rng.choice(KOREAN_PERSONA)}"
        elif style < 0.80:
            candidate = f"{rng.choice(KOREAN_DEV_PREFIX)}{rng.choice(KOREAN_DEV_SUFFIX)}"
        elif style < 0.88:
            candidate = f"{rng.choice(KOREAN_GIVEN)}의{rng.choice(KOREAN_OWNED_NOUN)}"
        else:
            base = rng.choice(CREATOR_BASE)
            suffix = rng.choice(CREATOR_SUFFIX)
            if rng.random() < 0.55:
                candidate = f"{base}{suffix}{rng.randint(1, 999)}"
            else:
                candidate = f"{base}{suffix}"
        candidate = candidate[:50]
        original = candidate
        bump = 0
        while candidate in seen:
            bump += 1
            candidate = f"{original}{bump}"[:50]
        seen.add(candidate)
        names.append(candidate)
    return names


def generate_course_distribution(rng: random.Random, active_count: int, total_courses: int) -> list[int]:
    tiers = [
        (0.05, 21, 35),
        (0.20, 11, 20),
        (0.50, 4, 10),
        (0.25, 1, 3),
    ]
    counts: list[int] = []
    used = 0
    for ratio, low, high in tiers[:-1]:
        size = round(active_count * ratio)
        for _ in range(size):
            counts.append(rng.randint(low, high))
        used += size
    last_low, last_high = tiers[-1][1], tiers[-1][2]
    for _ in range(active_count - used):
        counts.append(rng.randint(last_low, last_high))
    if len(counts) != active_count:
        raise RuntimeError(f"distribution size mismatch: {len(counts)} vs {active_count}")

    delta = total_courses - sum(counts)
    indices = list(range(active_count))
    rng.shuffle(indices)
    cursor = 0
    safety_budget = active_count * 200
    while delta != 0:
        if safety_budget <= 0:
            raise RuntimeError("could not reconcile distribution within retry budget")
        safety_budget -= 1
        idx = indices[cursor % active_count]
        cursor += 1
        if delta > 0 and counts[idx] < 50:
            counts[idx] += 1
            delta -= 1
        elif delta < 0 and counts[idx] > 1:
            counts[idx] -= 1
            delta += 1
    rng.shuffle(counts)
    return counts


def generate_course_title(rng: random.Random) -> str:
    title = rng.choice(COURSE_FORMAT).format(topic=rng.choice(COURSE_TOPIC))
    return title[:255]


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


def assert_dependent_tables_clean(env: dict, force: bool) -> None:
    out = run_sql(
        env,
        "SELECT COUNT(*) FROM sales_record; SELECT COUNT(*) FROM cancellation_record;",
        capture=True,
    )
    lines = [line for line in out.splitlines() if line.strip()]
    sales = int(lines[0])
    cancellation = int(lines[1])
    if sales == 0 and cancellation == 0:
        return
    if not force:
        raise SystemExit(
            f"Aborted: sales_record={sales}, cancellation_record={cancellation}. "
            "Re-run with --force to wipe these tables too."
        )
    run_sql(
        env,
        "SET FOREIGN_KEY_CHECKS=0; "
        "TRUNCATE cancellation_record; TRUNCATE sales_record; "
        "SET FOREIGN_KEY_CHECKS=1;",
    )


def truncate_target_tables(env: dict) -> None:
    run_sql(
        env,
        "SET FOREIGN_KEY_CHECKS=0; "
        "TRUNCATE course; TRUNCATE creator; "
        "SET FOREIGN_KEY_CHECKS=1;",
    )


def insert_creators(env: dict, names: list[str]) -> None:
    values = ",".join(f"('{sql_escape(n)}')" for n in names)
    run_sql(env, f"INSERT INTO creator (name) VALUES {values};")


def update_creator_names(env: dict, ids: list[int], names: list[str]) -> None:
    pairs = list(zip(ids, names))
    values = ",".join(f"({cid},'{sql_escape(name)}')" for cid, name in pairs)
    run_sql(
        env,
        f"INSERT INTO creator (creator_id, name) VALUES {values} AS new "
        "ON DUPLICATE KEY UPDATE name = new.name;",
    )


def fetch_creator_ids(env: dict) -> list[int]:
    out = run_sql(env, "SELECT creator_id FROM creator ORDER BY creator_id;", capture=True)
    return [int(line) for line in out.splitlines() if line.strip()]


def insert_courses(env: dict, rows: list[tuple[int, str]]) -> None:
    batch = 1000
    for start in range(0, len(rows), batch):
        chunk = rows[start:start + batch]
        values = ",".join(f"({cid},'{sql_escape(title)}')" for cid, title in chunk)
        run_sql(env, f"INSERT INTO course (creator_id, title) VALUES {values};")


def verify(env: dict) -> tuple[int, int, int]:
    out = run_sql(
        env,
        "SELECT COUNT(*) FROM creator; "
        "SELECT COUNT(*) FROM course; "
        "SELECT COUNT(DISTINCT creator_id) FROM course;",
        capture=True,
    )
    lines = [line for line in out.splitlines() if line.strip()]
    return int(lines[0]), int(lines[1]), int(lines[2])


def run_creators_only(args: argparse.Namespace) -> None:
    rng = random.Random(args.seed)
    new_names = generate_creator_names(rng, TARGET_CREATORS)
    print(f"[plan] creators-only: name 재부여 {TARGET_CREATORS}건")
    print(f"[plan] sample (앞 10): {new_names[:10]}")
    print(f"[plan] sample (뒤 5):  {new_names[-5:]}")

    if args.dry_run:
        print("[dry-run] no DB I/O performed.")
        return

    env = load_env()
    if not env["password"]:
        raise SystemExit("MYSQL_PASSWORD missing in .env")

    ids = fetch_creator_ids(env)
    if len(ids) != TARGET_CREATORS:
        raise SystemExit(
            f"creator count mismatch: {len(ids)} (expected {TARGET_CREATORS}). "
            "전체 시드를 먼저 실행하세요."
        )

    batch = 500
    for start in range(0, len(ids), batch):
        update_creator_names(env, ids[start:start + batch], new_names[start:start + batch])

    print(f"[done] {len(ids)} creator names updated (course 영향 없음)")


def main() -> None:
    parser = argparse.ArgumentParser(description="Seed creator/course tables")
    parser.add_argument("--dry-run", action="store_true",
                        help="distribution preview only, no DB I/O")
    parser.add_argument("--force", action="store_true",
                        help="also truncate sales_record/cancellation_record if non-empty")
    parser.add_argument("--seed", type=int, default=42,
                        help="random seed for reproducibility (default: 42)")
    parser.add_argument("--creators-only", action="store_true",
                        help="creator_id 유지하면서 name만 새로 부여 (course 건드리지 않음)")
    args = parser.parse_args()

    if args.creators_only:
        run_creators_only(args)
        return

    rng = random.Random(args.seed)
    active = TARGET_CREATORS - EMPTY_CREATORS
    counts_active = generate_course_distribution(rng, active, TARGET_COURSES)
    counts_full = counts_active + [0] * EMPTY_CREATORS
    rng.shuffle(counts_full)
    names = generate_creator_names(rng, TARGET_CREATORS)

    if sum(counts_full) != TARGET_COURSES:
        raise SystemExit(f"distribution sum mismatch: {sum(counts_full)} != {TARGET_COURSES}")

    top5 = sorted(counts_full, reverse=True)[:5]
    low5_nonzero = sorted(c for c in counts_full if c > 0)[:5]
    print(f"[plan] creators={TARGET_CREATORS}, courses={sum(counts_full)}, empty={EMPTY_CREATORS}")
    print(f"[plan] top-5 course counts per creator: {top5}")
    print(f"[plan] lowest-5 non-zero course counts: {low5_nonzero}")

    if args.dry_run:
        print("[dry-run] no DB I/O performed.")
        return

    env = load_env()
    if not env["password"]:
        raise SystemExit("MYSQL_PASSWORD missing in .env")

    assert_dependent_tables_clean(env, args.force)
    truncate_target_tables(env)

    insert_creators(env, names)
    creator_ids = fetch_creator_ids(env)
    if len(creator_ids) != TARGET_CREATORS:
        raise SystemExit(f"creator insert mismatch: {len(creator_ids)}")

    rows: list[tuple[int, str]] = []
    for creator_id, course_count in zip(creator_ids, counts_full):
        for _ in range(course_count):
            rows.append((creator_id, generate_course_title(rng)))
    insert_courses(env, rows)

    creator_n, course_n, distinct_n = verify(env)
    print(
        f"[done] creator={creator_n}, course={course_n}, "
        f"creators_with_course={distinct_n}, "
        f"empty_creators={TARGET_CREATORS - distinct_n}"
    )


if __name__ == "__main__":
    main()
