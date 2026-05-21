import psycopg2

conn = psycopg2.connect('postgresql://neondb_owner:npg_WBdT7SkUl6sr@ep-floral-surf-anlxaj3n-pooler.c-6.us-east-1.aws.neon.tech:5432/neondb?sslmode=require')
cur = conn.cursor()
cur.execute("""CREATE TABLE IF NOT EXISTS dropdown_selection (
    id SERIAL PRIMARY KEY,
    selection_key VARCHAR(200) UNIQUE,
    value VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)""")
conn.commit()
cur.execute("SELECT table_name FROM information_schema.tables WHERE table_name='dropdown_selection'")
print('Table exists:', cur.fetchone())
cur.close()
conn.close()
print('Done!')
