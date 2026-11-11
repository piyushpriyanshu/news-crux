"""Turns probe/report.json into a short list of problems (probe/problems.md); exit code 1 when there are any.

Known blocks: some sites refuse GitHub's servers but answer phones (Indian Express, MEA, Reuters). Those are
listed as notes, not problems.
"""
import json, sys

BLOCKED_FOR_GITHUB = {"IE", "MEA", "Reuters"}
r = json.load(open("probe/report.json"))
problems, notes = [], []
by_code = {}
for f in r["feeds"]:
    if f.get("candidate"):
        continue
    code, url = f.get("code"), f.get("url", "")
    by_code.setdefault(code, []).append(f)
    ok = f.get("http") == 200 and (f.get("raw", f.get("items", 0)) > 0 or f.get("sitemap"))
    if not ok:
        (notes if code in BLOCKED_FOR_GITHUB else problems).append(f"Feed not answering: {code} {url} (HTTP {f.get('http')}{', ' + f['error'] if f.get('error') else ''}, {f.get('items', 0)} items)")
    elif f.get("newestHours", 0) > 96 and "news.google.com" not in url:
        problems.append(f"Feed gone stale: {code} {url} (newest item {f['newestHours']} h old)")
    if f.get("raw", 0) > 0 and f.get("items", 0) == 0 and "keep" not in json.dumps(f):
        notes.append(f"Only Hindi headlines right now (the app skips them): {code} {url}")
for code, fs in by_code.items():
    arts = [a for f in fs for a in (f.get("articles") or []) if a.get("http") == 200 and "/topic/" not in a.get("url", "") and (a.get("new") or {}).get("method") != "not-html"]
    if len(arts) >= 3:
        read = [a for a in arts if (a.get("new") or {}).get("chars", 0) >= 400]
        if not read and code not in ("PTI",):
            problems.append(f"Articles not readable: {code} (0 of {len(arts)} sample pages gave text)")
for c in r.get("coaching", []):
    if c.get("http") != 200:
        problems.append(f"Coaching page not answering: {c.get('url')} (HTTP {c.get('http')})")
days = [c for c in r.get("coaching", []) if "drishtiias" in c.get("url", "") or "insightsonindia" in c.get("url", "")]
for site in ("drishtiias", "insightsonindia"):
    pages = [c for c in days if site in c.get("url", "")]
    if pages and all(c.get("topics", 0) == 0 for c in pages):
        problems.append(f"Coaching day pages give no topics: {site} (today and yesterday)")

out = ["Source check found problems the app may already be feeling. Each line says what failed.\n"] if problems else ["All sources answered.\n"]
out += [f"- {p}" for p in problems]
if notes:
    out += ["", "Notes (not counted as problems):"] + [f"- {n}" for n in notes]
open("probe/problems.md", "w").write("\n".join(out) + "\n")
print("\n".join(out))
sys.exit(1 if problems else 0)
