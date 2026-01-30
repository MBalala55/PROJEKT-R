#!/usr/bin/env python3
"""Check the schema of seed.db to verify foreign keys and indices."""

import sqlite3
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent.parent
SEED_DB = PROJECT_ROOT / "android-app" / "app" / "src" / "main" / "assets" / "database" / "seed.db"

if not SEED_DB.exists():
    print(f"ERROR: {SEED_DB} not found!")
    exit(1)

conn = sqlite3.connect(SEED_DB)
cursor = conn.cursor()

print("=" * 60)
print("Checking Polje table schema")
print("=" * 60)

# Check foreign keys
print("\nForeign keys in Polje table:")
cursor.execute("PRAGMA foreign_key_list(Polje)")
for row in cursor.fetchall():
    print(f"  {row}")

# Check indices
print("\nIndices on Polje table:")
cursor.execute("SELECT name, sql FROM sqlite_master WHERE type='index' AND tbl_name='Polje'")
for row in cursor.fetchall():
    print(f"  {row}")

# Check table schema
print("\nTable creation SQL:")
cursor.execute("SELECT sql FROM sqlite_master WHERE type='table' AND name='Polje'")
row = cursor.fetchone()
if row:
    print(f"  {row[0]}")

conn.close()
