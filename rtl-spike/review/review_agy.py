"""Review the Arabic Mindustry translation with Gemini 4 Argon (Medium) through the Antigravity CLI (subscription login).
Usage: python3 review_agy.py <bundles_dir> <glossary.md> <out_dir> [--test] [--jobs N]
Writes review_XX.json per batch: a list of {key, severity, problem, suggestion}.
"""
import json, os, re, sys, subprocess, concurrent.futures

AGY = os.path.expanduser("~/.local/bin/agy")
MODEL = os.environ.get("AGY_MODEL", "Gemini 4 Argon (Medium)")
BATCH = 250

bundles, glossary_path, out = sys.argv[1], sys.argv[2], sys.argv[3]
test = "--test" in sys.argv
jobs = int(sys.argv[sys.argv.index("--jobs") + 1]) if "--jobs" in sys.argv else 3
os.makedirs(out, exist_ok=True)
work = os.path.join(out, "empty_workdir")  # agy runs here, so it has no project files to touch
os.makedirs(work, exist_ok=True)

def parse(path):
    lines = open(path, encoding="utf-8").read().split("\n")
    d = {}; i = 0
    while i < len(lines):
        m = re.match(r"^([a-zA-Z0-9_.@-]+)\s*=\s?(.*)$", lines[i])
        if m:
            k, v = m.group(1), m.group(2)
            while v.endswith("\\") and not v.endswith("\\\\") and i + 1 < len(lines):
                i += 1; v = v[:-1] + lines[i].lstrip()
            d[k] = v
        i += 1
    return d

en = parse(f"{bundles}/bundle.properties")
ar = parse(f"{bundles}/bundle_ar.properties")
glossary = open(glossary_path, encoding="utf-8").read()
pairs = [{"key": k, "en": en[k], "ar": ar[k]} for k in en if k in ar]
if test: pairs = pairs[:20]
# batch by size: the whole prompt is one argv string, limited to ~128KB on Linux
batches, cur, size = [], [], 0
for e in pairs:
    n = len(json.dumps(e, ensure_ascii=False).encode())
    if cur and size + n > 80000:
        batches.append(cur); cur, size = [], 0
    cur.append(e); size += n
if cur: batches.append(cur)

INSTRUCTIONS = """You are a senior Arabic video-game localization reviewer. Do not use any tools, do not read or write files; just answer.
Review the Arabic (Modern Standard Arabic) translation of the game Mindustry (factory building + tower defense) given below as a JSON array of {key, en, ar}.
The team's glossary (terms chosen on purpose) is given below before the entries.
Report ONLY real problems: wrong or lost meaning; mistranslated game terms; a term translated differently from the glossary; ungrammatical or unnatural Arabic; text much longer than the English for short UI labels/buttons; broken placeholders ({0}), color tags ([accent], [], ...), \\n or icon characters.
Do NOT report style preferences when the existing translation is already correct and natural.
For suggestions: keep every placeholder, tag, \\n and icon exactly; no tashkeel/harakat; keep brand names, keyboard keys and logic-code identifiers in English.
Output ONLY a JSON array (possibly empty) of objects {"key", "severity" ("high"|"medium"|"low"), "problem" (short, English), "suggestion" (full corrected Arabic value)}. No prose, no markdown fences."""

def run(i, batch):
    path = f"{out}/review_{i:02d}.json"
    if os.path.exists(path): return i, "cached"
    prompt = INSTRUCTIONS + "\n\nGLOSSARY:\n" + glossary + "\n\nENTRIES:\n" + json.dumps(batch, ensure_ascii=False)
    for attempt in range(3):
        try:
            p = subprocess.run([AGY, "-p", prompt, "--model", MODEL, "--print-timeout", "25m"],
                               stdin=subprocess.DEVNULL, capture_output=True, text=True, cwd=work, timeout=1600)
        except subprocess.TimeoutExpired:
            continue
        text = p.stdout
        a, b = text.find("["), text.rfind("]")
        if a < 0 or b < a:
            open(f"{out}/raw_{i:02d}_{attempt}.txt", "w").write(text + "\n---STDERR---\n" + p.stderr[-2000:])
            continue
        try:
            issues = json.loads(text[a:b + 1])
        except json.JSONDecodeError:
            open(f"{out}/raw_{i:02d}_{attempt}.txt", "w").write(text)
            continue
        json.dump(issues, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
        return i, f"{len(issues)} issues"
    return i, "FAILED (see raw_*.txt)"

print(f"{len(pairs)} entries in {len(batches)} batches, model {MODEL}", flush=True)
with concurrent.futures.ThreadPoolExecutor(jobs) as ex:
    for i, status in ex.map(lambda a: run(*a), enumerate(batches)):
        print(f"batch {i}: {status}", flush=True)
