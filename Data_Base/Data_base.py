import psycopg2
import re

# Database connection details
DB_URL = "dbname='dbname_18rc' user='manohar' password='Xz2itYYNritzFpAMqKNgsFG6pp6WFYKb' host='dpg-d1qtcffdiees73fa7v5g-a.oregon-postgres.render.com' port='5432'"

def get_connection():
    """Connect to PostgreSQL"""
    return psycopg2.connect(DB_URL)

def normalize_text(text: str) -> str:
    """Normalize message: remove extra spaces, lowercase"""
    return re.sub(r"\s+", " ", text).strip().lower()

def get_verdict_from_db(message: str):
    """
    ✅ Check if a message exists in DB.
    Returns: 'spam' / 'ham' if found, else None.
    """
    sql = r"SELECT verdict FROM messages WHERE LOWER(REGEXP_REPLACE(content, '\s+', ' ', 'g')) = LOWER(%s)"
    try:
        with get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(sql, (normalize_text(message),))
                row = cur.fetchone()
                return row[0] if row else None
    except Exception as e:
        print("DB Error (get):", e)
        return None

def save_message_to_db(message: str, verdict: str) -> bool:
    """
    ✅ Save a message + verdict into DB.
    If already exists, it won't insert.
    Returns True if inserted, False if duplicate or error.
    """
    sql = """
        INSERT INTO messages (content, verdict) 
        VALUES (%s, %s)
        ON CONFLICT (content) DO NOTHING
    """
    try:
        with get_connection() as conn:
            with conn.cursor() as cur:
                cur.execute(sql, (message.strip(), verdict))
                conn.commit()
                return cur.rowcount > 0  # True if actually inserted
    except Exception as e:
        print("DB Error (save):", e)
        return False
    
print(get_verdict_from_db("hi ra mam"))
