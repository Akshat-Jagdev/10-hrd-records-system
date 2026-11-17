#!/usr/bin/env python3
"""
Synthetic data generator for the HRD Records System demo database.

Writes Flyway migration V3__synthetic_data.sql under
backend/src/main/resources/db/migration/ containing deterministic, purely
synthetic demo data: ~150 employees, 24 months x 5 production departments of
production records, 3 incentive schemes with per-grade rates, and scheme-1
incentive earnings (APPROVED through 2025-11, DRAFT for 2025-12).

DISCLAIMER: every value produced by this script is fabricated sample data.
No real plant, person, payroll or production data is used or implied. Names
are combined at random from lists of common Indian given/surnames; all
monetary and tonnage figures are invented. Deterministic: random.seed(42),
no network access, no external inputs.

Achievement and amount math mirror com.hrd.records.incentive.IncentiveCalculator
exactly:
  achievement = round2(actual * 100 / planned)            (HALF_UP)
  amount      = round2((min(ach, ceiling) - threshold) * rate * gradeFactor)
"""

import calendar
import random
from datetime import date
from decimal import Decimal, ROUND_HALF_UP, getcontext
from pathlib import Path

random.seed(42)
getcontext().prec = 50  # exact products; mirrors BigDecimal arithmetic

PROJECT_ROOT = Path(__file__).resolve().parents[1]
OUT_PATH = PROJECT_ROOT / "backend" / "src" / "main" / "resources" / "db" / "migration" / "V3__synthetic_data.sql"

# ---------------------------------------------------------------- constants

SEED_NOTE = "random.seed(42)"

EMPLOYEE_COUNT = 150
GRADES = ["E0", "E1", "E2", "E3", "E4", "E5", "M1", "M2"]
GRADE_WEIGHTS = [18, 20, 18, 14, 10, 8, 7, 5]
BASIC_PAY_BY_GRADE = {  # deterministic monthly basic pay (INR) per grade
    "E0": Decimal("28000.00"),
    "E1": Decimal("34000.00"),
    "E2": Decimal("42000.00"),
    "E3": Decimal("52000.00"),
    "E4": Decimal("68000.00"),
    "E5": Decimal("88000.00"),
    "M1": Decimal("110000.00"),
    "M2": Decimal("145000.00"),
}

DEPARTMENT_IDS = list(range(1, 9))  # must match V2__demo_users_and_departments.sql
DEPARTMENT_WEIGHTS = [14, 12, 11, 8, 9, 6, 5, 5]  # ids 1-5 production (heavier)
PRODUCTION_DEPARTMENT_IDS = [1, 2, 3, 4, 5]

PRODUCTION_BASE_TONNES = {1: 42000, 2: 38000, 3: 30000, 4: 16000, 5: 24000}
# Fixed seasonal multiplier per calendar month (Jan..Dec).
SEASONAL_MULTIPLIER = [0.97, 0.94, 1.03, 1.01, 1.05, 0.93, 0.87, 0.89, 1.02, 1.07, 1.04, 1.08]

MONTHS = [(2024, m) for m in range(1, 13)] + [(2025, m) for m in range(1, 13)]

DOJ_MIN = date(1990, 1, 1).toordinal()
DOJ_MAX = date(2025, 6, 30).toordinal()

RETIRED_COUNT = 15
TRANSFERRED_COUNT = 5

PRODUCTION_DESIGNATIONS = [
    "Operator", "Furnace Operator", "Roller", "Caster Operator", "Material Handler",
    "Shift Incharge", "Foreman", "Assistant Operator",
]
SUPPORT_DESIGNATIONS = [
    "Technician", "Engineer", "Officer", "Supervisor", "Inspector", "Clerk",
]

FIRST_NAMES = [
    "Rajesh", "Suresh", "Amit", "Ravi", "Vikram", "Anil", "Sunil", "Deepak",
    "Manoj", "Sanjay", "Ashok", "Ramesh", "Mahesh", "Suresh", "Arun", "Prakash",
    "Naveen", "Harish", "Mukesh", "Dinesh", "Sandeep", "Vinod", "Gopal", "Naresh",
    "Priya", "Sunita", "Anita", "Kavita", "Pooja", "Neha", "Asha", "Meena",
    "Rekha", "Shalini", "Divya", "Lakshmi", "Sarita", "Geeta", "Anjali", "Swati",
]
LAST_NAMES = [
    "Sharma", "Verma", "Kumar", "Singh", "Yadav", "Reddy", "Nair", "Iyer",
    "Patel", "Shah", "Gupta", "Mehta", "Joshi", "Rao", "Das", "Bose",
    "Chatterjee", "Banerjee", "Mukherjee", "Pillai", "Menon", "Kulkarni", "Deshpande", "Joshi",
    "Mishra", "Tiwari",
]

# Schemes (ids fixed; earnings reference scheme 1).
SCHEMES = [
    {
        "id": 1,
        "name": "Production Incentive 2024-25",
        "description": "Monthly production incentive for PRODUCTION shops: points above 95% and up to 115% of plan pay the per-point rate times the grade factor.",
        "threshold": Decimal("95.00"),
        "ceiling": Decimal("115.00"),
        "rate": Decimal("850.00"),
        "active": True,
        "from": "2024-01-01",
        "to": "2025-12-31",
    },
    {
        "id": 2,
        "name": "Safety Excellence Bonus",
        "description": "Bonus for months with achievement above 100% and no safety observations; capped at 110% of plan.",
        "threshold": Decimal("100.00"),
        "ceiling": Decimal("110.00"),
        "rate": Decimal("500.00"),
        "active": True,
        "from": "2024-01-01",
        "to": "2025-12-31",
    },
    {
        "id": 3,
        "name": "Energy Savings Scheme",
        "description": "Pilot scheme for energy savings above 98% of plan. Inactive sample: retained for history, excluded from calculations.",
        "threshold": Decimal("98.00"),
        "ceiling": Decimal("108.00"),
        "rate": Decimal("400.00"),
        "active": False,
        "from": "2025-01-01",
        "to": "2025-12-31",
    },
]
GRADE_FACTORS = [  # same factor table for every scheme
    ("E0", Decimal("1.30")), ("E1", Decimal("1.20")), ("E2", Decimal("1.10")),
    ("E3", Decimal("1.05")), ("E4", Decimal("1.00")), ("E5", Decimal("0.95")),
    ("M1", Decimal("0.90")), ("M2", Decimal("0.85")),
]

EARNINGS_SCHEME_ID = 1  # earnings are generated for scheme 1 only
DRAFT_PERIOD = (2025, 12)  # this month's earnings stay DRAFT (pending approval)
DECIDED_BY = "hr.manager"
DECIDED_AT = "2026-01-15T10:00:00Z"

TWO_PLACES = Decimal("0.01")


def round2(value: Decimal) -> Decimal:
    """Scale to 2 decimals, HALF_UP - same as BigDecimal.setScale(2, HALF_UP)."""
    return value.quantize(TWO_PLACES, rounding=ROUND_HALF_UP)


def achievement_pct(planned: Decimal, actual: Decimal) -> Decimal:
    """Mirror of IncentiveCalculator.achievementPct."""
    if planned == 0:
        return Decimal("0.00")
    return round2(actual * Decimal(100) / planned)


def incentive_amount(ach: Decimal, threshold: Decimal, ceiling: Decimal,
                     rate: Decimal, factor: Decimal) -> Decimal:
    """Mirror of IncentiveCalculator.amount."""
    effective_points = min(ach, ceiling) - threshold
    if effective_points <= 0:
        return Decimal("0.00")
    return round2(effective_points * rate * factor)


def month_end(year: int, month: int) -> date:
    return date(year, month, calendar.monthrange(year, month)[1])


def q(text: str) -> str:
    """SQL single-quoted literal (values below are ASCII and apostrophe-free)."""
    return "'" + text.replace("'", "''") + "'"


# ---------------------------------------------------------------- generation

def build_employees():
    employees = []
    for i in range(EMPLOYEE_COUNT):
        full_name = random.choice(FIRST_NAMES) + " " + random.choice(LAST_NAMES)
        grade = random.choices(GRADES, weights=GRADE_WEIGHTS, k=1)[0]
        dept_id = random.choices(DEPARTMENT_IDS, weights=DEPARTMENT_WEIGHTS, k=1)[0]
        designation = random.choice(
            PRODUCTION_DESIGNATIONS if dept_id in PRODUCTION_DEPARTMENT_IDS
            else SUPPORT_DESIGNATIONS
        )
        doj = date.fromordinal(random.randint(DOJ_MIN, DOJ_MAX))
        employees.append({
            "id": i + 1,
            "emp_no": "E%d" % (1001 + i),
            "full_name": full_name,
            "grade": grade,
            "designation": designation,
            "department_id": dept_id,
            "doj": doj,
            "status": "ACTIVE",
            "basic_pay": BASIC_PAY_BY_GRADE[grade],
        })
    # Longest-serving staff have moved on: 15 earliest joiners RETIRED,
    # next 5 TRANSFERRED, remaining ~130 ACTIVE (deterministic, realistic).
    by_seniority = sorted(employees, key=lambda e: (e["doj"], e["id"]))
    for e in by_seniority[:RETIRED_COUNT]:
        e["status"] = "RETIRED"
    for e in by_seniority[RETIRED_COUNT:RETIRED_COUNT + TRANSFERRED_COUNT]:
        e["status"] = "TRANSFERRED"
    return employees


def build_production_records():
    records = {}  # (dept_id, year, month) -> dict
    rec_id = 0
    for dept_id in PRODUCTION_DEPARTMENT_IDS:
        base = PRODUCTION_BASE_TONNES[dept_id]
        for (year, month) in MONTHS:
            rec_id += 1
            planned = round2(Decimal(base) * Decimal(str(SEASONAL_MULTIPLIER[month - 1])))
            actual = round2(planned * Decimal(str(random.uniform(0.85, 1.12))))
            records[(dept_id, year, month)] = {
                "id": rec_id, "department_id": dept_id, "year": year, "month": month,
                "planned": planned, "actual": actual,
            }
    return records


def build_earnings(employees, production):
    scheme = SCHEMES[0]
    factor_by_grade = dict(GRADE_FACTORS)
    earnings = []
    for (year, month) in MONTHS:
        for dept_id in PRODUCTION_DEPARTMENT_IDS:
            rec = production[(dept_id, year, month)]
            ach = achievement_pct(rec["planned"], rec["actual"])
            if ach <= scheme["threshold"]:
                continue  # no rows for periods at/below threshold
            end = month_end(year, month)
            is_draft = (year, month) == DRAFT_PERIOD
            for emp in employees:  # id order -> deterministic insert order
                if emp["department_id"] != dept_id or emp["status"] != "ACTIVE":
                    continue
                if emp["doj"] > end:
                    continue
                amount = incentive_amount(
                    ach, scheme["threshold"], scheme["ceiling"], scheme["rate"],
                    factor_by_grade[emp["grade"]],
                )
                earnings.append({
                    "id": len(earnings) + 1,
                    "employee_id": emp["id"],
                    "scheme_id": EARNINGS_SCHEME_ID,
                    "year": year, "month": month,
                    "achievement": ach, "amount": amount,
                    "status": "DRAFT" if is_draft else "APPROVED",
                    "decided_by": None if is_draft else DECIDED_BY,
                    "decided_at": None if is_draft else DECIDED_AT,
                })
    return earnings


# ---------------------------------------------------------------- SQL emit

def sql_header():
    return "\n".join([
        "-- V3__synthetic_data.sql - DETERMINISTIC SYNTHETIC DEMO DATA.",
        "-- Generated by scripts/generate_synthetic_data.py (%s). DO NOT EDIT BY HAND;" % SEED_NOTE,
        "-- re-run the generator instead.",
        "--",
        "-- DISCLAIMER: all values are fabricated sample data. No real plant, person,",
        "-- payroll or production data is used or implied. Names are random combinations",
        "-- of common Indian names; tonnage, pay and incentive figures are invented.",
        "--",
        "-- Achievement/amount math mirrors IncentiveCalculator (HALF_UP, scale 2).",
        "-- References department ids 1-8 and scheme ids fixed by V2/V3.",
        "",
    ])


def emit_employees(employees):
    lines = ["-- Employees: %d synthetic master records (emp_no E1001.. sequential)." % len(employees)]
    lines.append(
        "INSERT INTO employees (id, emp_no, full_name, grade, designation, department_id, doj, status, basic_pay) VALUES")
    for idx, e in enumerate(employees):
        sep = "," if idx < len(employees) - 1 else ";"
        lines.append(
            "(%d, %s, %s, %s, %s, %d, %s, %s, %s)%s" % (
                e["id"], q(e["emp_no"]), q(e["full_name"]), q(e["grade"]),
                q(e["designation"]), e["department_id"], q(e["doj"].isoformat()),
                q(e["status"]), e["basic_pay"], sep))
    lines.append("")
    return lines


def emit_production(production):
    ordered = sorted(production.values(), key=lambda r: r["id"])
    lines = ["-- Production: PRODUCTION departments 1-5 x %d months (2024-01..2025-12)." % len(MONTHS)]
    lines.append("INSERT INTO production_records (id, department_id, year, month, planned_tonnes, actual_tonnes) VALUES")
    for idx, r in enumerate(ordered):
        sep = "," if idx < len(ordered) - 1 else ";"
        lines.append(
            "(%d, %d, %d, %d, %s, %s)%s" % (
                r["id"], r["department_id"], r["year"], r["month"],
                r["planned"], r["actual"], sep))
    lines.append("")
    return lines


def emit_schemes():
    lines = ["-- Incentive schemes (scheme 3 inactive on purpose)."]
    lines.append(
        "INSERT INTO incentive_schemes (id, name, description, threshold_pct, ceiling_pct, rate_per_point, active, effective_from, effective_to) VALUES")
    for idx, s in enumerate(SCHEMES):
        sep = "," if idx < len(SCHEMES) - 1 else ";"
        lines.append(
            "(%d, %s, %s, %s, %s, %s, %s, %s, %s)%s" % (
                s["id"], q(s["name"]), q(s["description"]), s["threshold"],
                s["ceiling"], s["rate"], "TRUE" if s["active"] else "FALSE",
                q(s["from"]), q(s["to"]), sep))
    lines.append("")
    lines.append("-- Grade rates: identical 8-grade factor table for every scheme.")
    lines.append("INSERT INTO scheme_grade_rates (id, scheme_id, grade, factor) VALUES")
    rate_rows = []
    for s in SCHEMES:
        for grade, factor in GRADE_FACTORS:
            rate_rows.append((s["id"], grade, factor))
    for idx, (scheme_id, grade, factor) in enumerate(rate_rows):
        sep = "," if idx < len(rate_rows) - 1 else ";"
        lines.append("(%d, %d, %s, %s)%s" % (idx + 1, scheme_id, q(grade), factor, sep))
    lines.append("")
    return lines


def emit_earnings(earnings):
    approved = sum(1 for e in earnings if e["status"] == "APPROVED")
    draft = sum(1 for e in earnings if e["status"] == "DRAFT")
    lines = [
        "-- Scheme-1 earnings: one row per eligible ACTIVE employee of a PRODUCTION",
        "-- department for months whose achievement exceeds the 95.00 threshold.",
        "-- %d APPROVED (through 2025-11), %d DRAFT (2025-12, pending approval)." % (approved, draft),
    ]
    lines.append(
        "INSERT INTO incentive_earnings (id, employee_id, scheme_id, year, month, achievement_pct, amount, status, decided_by, decided_at) VALUES")
    for idx, e in enumerate(earnings):
        sep = "," if idx < len(earnings) - 1 else ";"
        decided_by = "NULL" if e["decided_by"] is None else q(e["decided_by"])
        decided_at = "NULL" if e["decided_at"] is None else q(e["decided_at"])
        lines.append(
            "(%d, %d, %d, %d, %d, %s, %s, %s, %s, %s)%s" % (
                e["id"], e["employee_id"], e["scheme_id"], e["year"], e["month"],
                e["achievement"], e["amount"], q(e["status"]), decided_by, decided_at, sep))
    lines.append("")
    return lines


def main():
    employees = build_employees()
    production = build_production_records()
    earnings = build_earnings(employees, production)

    lines = [sql_header()]
    lines += emit_employees(employees)
    lines += emit_production(production)
    lines += emit_schemes()
    lines += emit_earnings(earnings)

    content = "\n".join(lines)
    ascii_only = all(ord(c) < 128 for c in content)
    if not ascii_only:
        raise SystemExit("generated SQL contains non-ASCII characters")
    with open(OUT_PATH, "w", encoding="ascii", newline="\n") as fh:
        fh.write(content)

    status_counts = {}
    for e in employees:
        status_counts[e["status"]] = status_counts.get(e["status"], 0) + 1
    print("Wrote %s" % OUT_PATH)
    print("Summary (all values synthetic, seed=42):")
    print("  employees            : %d  %s" % (len(employees), status_counts))
    print("  production_records   : %d  (5 departments x %d months)" % (len(production), len(MONTHS)))
    print("  incentive_schemes    : %d  (grade rates: %d)" % (len(SCHEMES), len(SCHEMES) * len(GRADE_FACTORS)))
    approved = sum(1 for e in earnings if e["status"] == "APPROVED")
    draft = sum(1 for e in earnings if e["status"] == "DRAFT")
    print("  incentive_earnings   : %d  (APPROVED %d, DRAFT %d)" % (len(earnings), approved, draft))


if __name__ == "__main__":
    main()
