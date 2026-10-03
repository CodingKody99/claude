#!/usr/bin/env python3
"""Walks a database through every migration and checks the result against
what Room will demand.

Why this exists: Room compares the schema it finds on the device with the
schema its entities imply, and a mismatch is an IllegalStateException on the
first launch after an update — with the user's lists sitting in that
database. The usual way to catch it, Room's own migration test helper, needs
an instrumented or Robolectric test; this reproduces the chain in plain
SQLite instead, so it runs in CI with the unit tests.

It also guards against itself: if AppDatabase.kt gains a migration or a
statement that is not mirrored below, the check fails rather than quietly
testing less than it claims to.
"""
import re
import sqlite3
import sys
from pathlib import Path

DB = Path("app/src/main/java/io/github/codingkody99/einkaufsliste/data/AppDatabase.kt")

# The starting point: the schema of the very first released version.
V1_ITEMS = (
    "CREATE TABLE IF NOT EXISTS `shopping_items` ("
    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
    "`name` TEXT NOT NULL, "
    "`quantity` TEXT NOT NULL, "
    "`category` TEXT NOT NULL, "
    "`is_checked` INTEGER NOT NULL, "
    "`created_at` INTEGER NOT NULL)"
)

# Mirrors the migrations in AppDatabase.kt, keyed by the version they produce.
MIGRATIONS = {
    2: [
        "CREATE TABLE IF NOT EXISTS `category_overrides` ("
        "`name_key` TEXT NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`name_key`))",
    ],
    3: [
        "CREATE TABLE IF NOT EXISTS `shopping_lists` ("
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, "
        "`position` INTEGER NOT NULL, `created_at` INTEGER NOT NULL)",
        "INSERT INTO `shopping_lists` (`id`, `name`, `position`, `created_at`) "
        "VALUES (1, 'Einkaufsliste', 0, 1700000000000)",
        "ALTER TABLE `shopping_items` ADD COLUMN `list_id` INTEGER NOT NULL DEFAULT 1",
        "CREATE INDEX IF NOT EXISTS `index_shopping_items_list_id` "
        "ON `shopping_items` (`list_id`)",
    ],
    4: [
        "CREATE TABLE IF NOT EXISTS `recipes` ("
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, "
        "`source_url` TEXT, `ingredients_text` TEXT NOT NULL, `created_at` INTEGER NOT NULL)",
    ],
    5: [
        "ALTER TABLE `recipes` ADD COLUMN `steps` TEXT NOT NULL DEFAULT ''",
    ],
    6: [
        "CREATE TABLE IF NOT EXISTS `sync_settings` ("
        "`id` INTEGER NOT NULL, `household_id` TEXT, PRIMARY KEY(`id`))",
    ],
}

# What the Kotlin entities imply: column -> (type affinity, notnull, pk position).
EXPECTED = {
    "shopping_items": {
        "id": ("INTEGER", 1, 1), "list_id": ("INTEGER", 1, 0),
        "name": ("TEXT", 1, 0), "quantity": ("TEXT", 1, 0),
        "category": ("TEXT", 1, 0), "is_checked": ("INTEGER", 1, 0),
        "created_at": ("INTEGER", 1, 0),
    },
    "shopping_lists": {
        "id": ("INTEGER", 1, 1), "name": ("TEXT", 1, 0),
        "position": ("INTEGER", 1, 0), "created_at": ("INTEGER", 1, 0),
    },
    "recipes": {
        "id": ("INTEGER", 1, 1), "name": ("TEXT", 1, 0),
        "source_url": ("TEXT", 0, 0), "ingredients_text": ("TEXT", 1, 0),
        "steps": ("TEXT", 1, 0), "created_at": ("INTEGER", 1, 0),
    },
    "category_overrides": {"name_key": ("TEXT", 1, 1), "category": ("TEXT", 1, 0)},
    "sync_settings": {"id": ("INTEGER", 1, 1), "household_id": ("TEXT", 0, 0)},
}

LATEST = max(MIGRATIONS)
problems = []


def check_in_step_with_kotlin():
    """The mirror above is only worth anything if it is complete."""
    if not DB.exists():
        problems.append(f"{DB} nicht gefunden")
        return
    source = DB.read_text()

    declared = {int(b) for _, b in re.findall(r"Migration\((\d+),\s*(\d+)\)", source)}
    mirrored = set(MIGRATIONS)
    for v in sorted(declared - mirrored):
        problems.append(f"AppDatabase.kt migriert auf v{v}, hier fehlt der Schritt")
    for v in sorted(mirrored - declared):
        problems.append(f"Hier steht ein Schritt auf v{v}, den AppDatabase.kt nicht hat")

    version = re.search(r"version\s*=\s*(\d+)", source)
    if version and int(version.group(1)) != LATEST:
        problems.append(
            f"AppDatabase.kt ist v{version.group(1)}, geprüft wird bis v{LATEST}"
        )

    # Every execSQL in a migration needs a counterpart. The seed callback for
    # fresh installs runs instead of the migrations, so it is not counted.
    body = source[: source.find("private val seedFirstList")]
    in_kotlin = body.count("execSQL(")
    here = sum(len(v) for v in MIGRATIONS.values())
    if in_kotlin != here:
        problems.append(
            f"{in_kotlin} execSQL-Aufrufe in AppDatabase.kt, {here} Anweisungen hier"
        )


def upgrade_from(start):
    """A database as it was at `start`, with data in it, taken to the latest."""
    db = sqlite3.connect(":memory:")
    db.execute(V1_ITEMS)
    for v in range(2, start + 1):
        for sql in MIGRATIONS[v]:
            db.execute(sql)

    if start >= 3:
        db.execute(
            "INSERT INTO shopping_lists (name, position, created_at) "
            "VALUES ('Drogerie', 1, 1)"
        )
    db.execute(
        "INSERT INTO shopping_items (name, quantity, category, is_checked, created_at) "
        "VALUES ('Tomaten', '500 g', 'OBST_GEMUESE', 0, 1)"
    )
    if start >= 5:
        db.execute(
            "INSERT INTO recipes (name, source_url, ingredients_text, steps, created_at) "
            "VALUES ('Pfannkuchen', NULL, 'Mehl\nMilch', 'Alles verruehren', 1)"
        )
    before = db.execute("SELECT name, quantity, category FROM shopping_items").fetchall()

    for v in range(start + 1, LATEST + 1):
        for sql in MIGRATIONS[v]:
            db.execute(sql)

    local = []
    for table, columns in EXPECTED.items():
        info = db.execute(f"PRAGMA table_info(`{table}`)").fetchall()
        if not info:
            local.append(f"Tabelle {table} fehlt")
            continue
        found = {row[1]: (row[2], row[3], row[5]) for row in info}
        for column in sorted(set(columns) | set(found)):
            if columns.get(column) != found.get(column):
                local.append(
                    f"{table}.{column}: Room erwartet {columns.get(column)}, "
                    f"da ist {found.get(column)}"
                )

    after = db.execute("SELECT name, quantity, category FROM shopping_items").fetchall()
    if before != after:
        local.append(f"Daten haben sich verändert: {before} -> {after}")

    indices = db.execute("PRAGMA index_list(`shopping_items`)").fetchall()
    if not any(row[1] == "index_shopping_items_list_id" for row in indices):
        local.append("Index index_shopping_items_list_id fehlt")

    # The sharing settings are one row by construction, not by convention.
    db.execute("INSERT OR REPLACE INTO sync_settings (id, household_id) VALUES (1, 'AAAABBBBCCCC')")
    db.execute("INSERT OR REPLACE INTO sync_settings (id, household_id) VALUES (1, 'DDDDEEEEFFFF')")
    rows = db.execute("SELECT id, household_id FROM sync_settings").fetchall()
    if rows != [(1, "DDDDEEEEFFFF")]:
        local.append(f"sync_settings sollte genau eine Zeile halten, hält: {rows}")

    print(f"  v{start} -> v{LATEST}: " + ("ok" if not local else "FEHLER"))
    for line in local:
        print(f"      {line}")
    problems.extend(f"v{start}->v{LATEST}: {line}" for line in local)


print(f"Migrationen prüfen (bis v{LATEST})")
check_in_step_with_kotlin()
for start in range(1, LATEST):
    upgrade_from(start)

print()
if problems:
    print(f"{len(problems)} Problem(e):")
    for line in problems:
        print(f"  - {line}")
    sys.exit(1)
print("Alle Upgrade-Pfade sauber.")
