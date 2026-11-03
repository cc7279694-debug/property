"""Host SQLite smoke of the actual Kotlin SQL, using only an in-memory synthetic ledger."""
from pathlib import Path
import datetime, hashlib, json, re, sqlite3

root = Path.cwd()
source = root / 'app/src/main/java/com/cooper/lithic/data/media/GenerationPhotoStorageReader.kt'
raw = source.read_bytes()
text = raw.decode('utf-8')
def sql(name):
    match = re.search(r'const val ' + name + r' = """(.*?)"""', text, re.S)
    assert match, name
    return match.group(1)
refs = sql('COVER_REFERENCES')
history = sql('HISTORICAL_BYTES').replace('$COVER_REFERENCES', refs)
missing = sql('MISSING_REFERENCE_COUNT').replace('$COVER_REFERENCES', refs)
assert '$' not in history and '$' not in missing
db = sqlite3.connect(':memory:')
db.executescript('''
CREATE TABLE local_media_assets (id TEXT PRIMARY KEY, byteLength INTEGER NOT NULL);
CREATE TABLE long_term_projects (id TEXT PRIMARY KEY, coverAssetId TEXT);
CREATE TABLE subscriptions (id TEXT PRIMARY KEY, coverAssetId TEXT);
''')
def value(query): return db.execute(query).fetchone()[0]
results = []
def check(name, expected):
    actual = value(history)
    assert isinstance(actual, int) and actual == expected, (name, actual, expected)
    results.append(dict(case=name, expected=expected, actual=actual))
check('empty', 0)
db.executemany('INSERT INTO local_media_assets VALUES(?,?)', [('a', 100), ('b', 200), ('c', 17)])
db.execute('INSERT INTO long_term_projects VALUES(?,?)', ('p', 'a'))
db.execute('INSERT INTO subscriptions VALUES(?,?)', ('s', 'a'))
check('shared_project_and_subscription', 217)
db.execute('INSERT INTO long_term_projects VALUES(?,?)', ('p2', 'b'))
check('distinct_references_leave_only_c', 17)
db.execute('UPDATE long_term_projects SET coverAssetId=NULL WHERE id=?', ('p',))
check('shared_subscription_keeps_a_referenced', 17)
db.execute('UPDATE subscriptions SET coverAssetId=NULL WHERE id=?', ('s',))
check('remove_last_reference_to_a', 117)
db.execute('UPDATE long_term_projects SET coverAssetId=NULL')
check('all_historical', 317)
db.execute('UPDATE subscriptions SET coverAssetId=?', ('missing',))
assert value(missing) == 1
results.append(dict(case='missing_reference_detected_by_existing_gate', actual=1))
db.execute('DELETE FROM local_media_assets')
db.executemany('INSERT INTO local_media_assets VALUES(?,?)', [('a', 9223372036854775807), ('b', 1)])
try:
    value(history)
    raise AssertionError('SQLite overflow must not be presented as zero')
except sqlite3.OperationalError as exc:
    assert 'overflow' in str(exc).lower()
    results.append(dict(case='integer_sum_overflow', error=str(exc)))
db.close()
out = root / 'docs/evidence/v1-b3-20261103/desktop-1/sqlite-smoke.json'
out.write_text(json.dumps(dict(status='HOST_SQLITE_PASS', observedUtc=datetime.datetime.now(datetime.timezone.utc).isoformat(),
    sqliteVersion=sqlite3.sqlite_version, sourceSha256=hashlib.sha256(raw).hexdigest(), cases=results,
    database='IN_MEMORY_SYNTHETIC_ONLY', androidExecution='NOT_RUN'), indent=2)+'\n', encoding='utf-8')
print(json.dumps(dict(status='HOST_SQLITE_PASS', cases=len(results), androidExecution='NOT_RUN')))
