import sqlite3
import json
import sys

DB = r"C:\Users\16602\.local\share\mimocode\mimocode.db"
conn = sqlite3.connect(DB)
conn.row_factory = sqlite3.Row
cursor = conn.cursor()

# Check task table schema
print("=== TASK TABLE SCHEMA ===")
cursor.execute("PRAGMA table_info(task)")
for row in cursor.fetchall():
    print(f"  {row['name']} ({row['type']})")

# Check task_event table schema
print("\n=== TASK_EVENT TABLE SCHEMA ===")
cursor.execute("PRAGMA table_info(task_event)")
for row in cursor.fetchall():
    print(f"  {row['name']} ({row['type']})")

# Check memory_fts table
print("\n=== MEMORY_FTS SCHEMA ===")
cursor.execute("PRAGMA table_info(memory_fts)")
for row in cursor.fetchall():
    print(f"  {row['name']} ({row['type']})")

# Search for user messages containing keywords about rules, decisions
print("\n=== USER MESSAGES WITH RULES/DECISIONS ===")
keywords = ['always', 'never', 'remember', 'rule', 'decision', 'decided', 'prefer', 'use ', 'don\'t', 'must']
for kw in keywords:
    cursor.execute("""
        SELECT m.id, m.session_id, substr(json_extract(p.data, '$.text'), 1, 200) as text_preview
        FROM message m
        JOIN part p ON p.message_id = m.id
        WHERE json_extract(m.data, '$.role') = 'user'
          AND json_extract(p.data, '$.type') = 'text'
          AND json_extract(p.data, '$.text') LIKE ?
        LIMIT 5
    """, (f'%{kw}%',))
    rows = cursor.fetchall()
    if rows:
        print(f"\n  Keyword '{kw}':")
        for row in rows:
            print(f"    [{row['session_id']}] {row['text_preview']}")

# Get project info
print("\n=== PROJECTS ===")
try:
    cursor.execute("SELECT * FROM project LIMIT 10")
    for row in cursor.fetchall():
        print(f"  {dict(row)}")
except Exception as e:
    print(f"  Error: {e}")

conn.close()
