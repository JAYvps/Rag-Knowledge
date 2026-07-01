import sqlite3
import json
import sys

DB = r"C:\Users\16602\.local\share\mimocode\mimocode.db"
conn = sqlite3.connect(DB)
conn.row_factory = sqlite3.Row
cursor = conn.cursor()

# List tables
print("=== TABLES ===")
cursor.execute("SELECT name FROM sqlite_master WHERE type='table'")
for row in cursor.fetchall():
    print(row[0])

print("\n=== SESSIONS (last 30 days) ===")
cursor.execute("""
    SELECT id, project_id, directory, title, time_created,
           time_updated
    FROM session
    ORDER BY time_created DESC
    LIMIT 20
""")
for row in cursor.fetchall():
    print(f"  {row['id']} | proj={row['project_id']} | dir={row['directory']} | title={row['title']} | created={row['time_created']} | updated={row['time_updated']}")

# Count messages per session
print("\n=== MESSAGE COUNTS ===")
cursor.execute("""
    SELECT session_id, COUNT(*) as cnt
    FROM message
    GROUP BY session_id
    ORDER BY cnt DESC
""")
for row in cursor.fetchall():
    print(f"  {row['session_id']}: {row['cnt']} messages")

# Check task table
print("\n=== TASKS ===")
try:
    cursor.execute("""
        SELECT id, session_id, title, status, time_created
        FROM task
        ORDER BY time_created DESC
        LIMIT 20
    """)
    for row in cursor.fetchall():
        print(f"  {row['id']} | ses={row['session_id']} | title={row['title']} | status={row['status']} | created={row['time_created']}")
except Exception as e:
    print(f"  Error: {e}")

# Check actor_registry
print("\n=== ACTOR REGISTRY ===")
try:
    cursor.execute("SELECT * FROM actor_registry LIMIT 10")
    for row in cursor.fetchall():
        print(f"  {dict(row)}")
except Exception as e:
    print(f"  Error: {e}")

conn.close()
