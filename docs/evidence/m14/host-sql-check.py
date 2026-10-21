import hashlib,json,re,sqlite3
from pathlib import Path
root=Path.cwd()
def query(path,name):
    text=(root/path).read_text(encoding='utf-8')
    pattern=r'@Query\((?:"""(.*?)"""|"([^"\n]*)")\)\s*suspend fun (\w+)\('
    for match in re.finditer(pattern,text,re.S):
        if match.group(3)==name:
            return match.group(1) or match.group(2)
    raise AssertionError(name)
conn=sqlite3.connect(':memory:'); conn.row_factory=sqlite3.Row
conn.execute('PRAGMA foreign_keys=ON')
schema=json.loads((root/'app/schemas/com.cooper.lithic.data.local.LithicDatabase/2.json').read_text())['database']
for entity in schema['entities']:
    conn.execute(entity['createSql'].replace('${TABLE_NAME}',entity['tableName']))
    for index in entity['indices']:
        conn.execute(index['createSql'].replace('${TABLE_NAME}',entity['tableName']))
cat='app/src/main/java/com/cooper/lithic/data/local/CategoryDao.kt'
proj='app/src/main/java/com/cooper/lithic/data/local/ProjectDao.kt'
search='app/src/main/java/com/cooper/lithic/data/local/SearchDao.kt'
for i,name in [('source','source'),('target','target')]:
    conn.execute('INSERT INTO categories VALUES (?,?,?,?,?)',(i,'PROJECT',name,name,0))
for n in range(105):
    name=['100% camera','under_score','O\'Reilly','x!y'][n%4]
    conn.execute('INSERT INTO long_term_projects VALUES (?,?,?,?,?,?,?,?,?,?)',(f'p{n:03}',name,'source',n,n+100,'OTHER',None,None,'2026-01-01T00:00:00Z','2026-01-01T00:00:00Z'))
conn.commit()
page=query(proj,'pageHistoryProjects'); seen=[]; days=None; after=None
while True:
    rows=conn.execute(page,dict(asOf=99999,afterHeldDays=days,afterId=after,limit=50)).fetchall()
    if not rows: break
    seen.extend(x['id'] for x in rows); last=rows[-1]; days=last['endedOn']-last['startedOn']+1; after=last['id']
assert len(seen)==len(set(seen))==105
literal=query(search,'pageNames')
for text,expected in [('100%',27),('under_',26),("O'Reilly",26),('x!y',26),("' OR 1=1 --",0)]:
    pattern='%'+text.replace('!','!!').replace('%','!%').replace('_','!_')+'%'
    rows=conn.execute(literal,dict(pattern=pattern,afterType=None,afterId=None,limit=51)).fetchall()
    assert len(rows)==expected,(text,len(rows))
before=[tuple(x) for x in conn.execute('SELECT * FROM long_term_projects ORDER BY id')]
conn.execute("CREATE TRIGGER injected BEFORE DELETE ON categories WHEN OLD.id='source' BEGIN SELECT RAISE(ABORT,'synthetic delete failure'); END")
conn.commit()
move=query(cat,'moveProjectReferences'); delete=query(cat,'deleteIfUnchanged')
try:
    with conn:
        assert conn.execute(move,dict(sourceId='source',targetId='target')).rowcount==105
        conn.execute(delete,dict(id='source',type='PROJECT',expectedName='source',expectedNormalizedName='source'))
except sqlite3.IntegrityError: pass
else: raise AssertionError('Injected failure did not fail')
assert before==[tuple(x) for x in conn.execute('SELECT * FROM long_term_projects ORDER BY id')]
assert conn.execute("SELECT COUNT(*) FROM categories WHERE id='source'").fetchone()[0]==1
conn.execute('DROP TRIGGER injected');conn.commit()
with conn:
    assert conn.execute(move,dict(sourceId='source',targetId='target')).rowcount==105
    assert conn.execute(delete,dict(id='source',type='PROJECT',expectedName='source',expectedNormalizedName='source')).rowcount==1
assert list(conn.execute('PRAGMA foreign_key_check'))==[]
after_rows=[tuple(x) for x in conn.execute('SELECT * FROM long_term_projects ORDER BY id')]
assert all(a[:2]+a[3:]==b[:2]+b[3:] for a,b in zip(before,after_rows))
result={'evidenceLevel':'HOST_SQLITE_ONLY_NOT_ANDROID_ROOM','sqliteVersion':sqlite3.sqlite_version,'history105Unique':True,'literalLikeCases':5,'deleteFailureRollback':True,'merge105ReferencesOnly':True,'foreignKeyViolations':0,'sqlSha256':{n:hashlib.sha256(q.encode()).hexdigest() for n,q in [('history',page),('search',literal),('move',move),('delete',delete)]}}
print(json.dumps(result,ensure_ascii=False,indent=2))

