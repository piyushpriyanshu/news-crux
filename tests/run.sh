#!/usr/bin/env bash
# Checks run after every build: data files, the web UI script and the foreign-office filter.
set -euo pipefail
cd "$(dirname "$0")/.."
python3 - <<'PY'
import json
for f in ("assets/www/data/kb.json", "assets/www/data/sources.json"):
    json.load(open(f, encoding="utf-8"))
for f, var in (("assets/www/data/kb.js", "window.KB="), ("assets/www/data/pyq.js", "window.PYQ="), ("assets/www/data/pyq_more.js", "window.PYQ_MORE="), ("assets/www/data/pyq_text.js", "window.PYQ_TEXT=")):
    s = open(f, encoding="utf-8").read().strip()
    assert s.startswith(var), f
    json.loads(s[len(var):].rstrip(";"))
assert open("assets/www/data/kb.js", encoding="utf-8").read() == "window.KB=" + open("assets/www/data/kb.json", encoding="utf-8").read() + ";", "kb.js out of sync with kb.json"
print("data files OK")
PY
if command -v node >/dev/null; then node --check assets/www/app.js && echo "app.js OK"; fi
ORGJSON="${ORGJSON:-build/json.jar}"
if [ ! -f "$ORGJSON" ]; then curl -sSfL -o "$ORGJSON" https://repo1.maven.org/maven2/org/json/json/20240303/json-20240303.jar; fi
mkdir -p build/test
javac -nowarn -cp "build/classes:$ORGJSON" -d build/test tests/ForeignTest.java tests/app/newsbreak/core/BulletTest.java tests/PyqTest.java tests/ExamMetaTest.java
java -cp "build/test:build/classes:$ORGJSON" ForeignTest
java -cp "build/test:build/classes:$ORGJSON" app.newsbreak.core.BulletTest
java -cp "build/test:build/classes:$ORGJSON" PyqTest
java -cp "build/test:build/classes:$ORGJSON" ExamMetaTest
