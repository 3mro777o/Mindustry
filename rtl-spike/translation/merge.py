"""Merge translated chunks into bundle_ar.properties, validating tokens.
Usage: python3 merge.py <bundles_dir> <tr_dir>
"""
import json, re, sys, glob, collections

bundles, trdir = sys.argv[1], sys.argv[2]

def parse(path):
    lines = open(path, encoding='utf-8').read().split('\n')
    out = collections.OrderedDict(); i = 0
    while i < len(lines):
        m = re.match(r'^([a-zA-Z0-9_.@-]+)\s*=\s?(.*)$', lines[i])
        if m:
            k, v = m.group(1), m.group(2)
            while v.endswith('\\') and not v.endswith('\\\\') and i + 1 < len(lines):
                i += 1; v = v[:-1] + lines[i].lstrip()
            out[k] = v
        i += 1
    return out

TOK = re.compile(r'\{\d+\}|\[[^\[\]]*\]|\\u[0-9A-Fa-f]{4}|\\.')
def tokens(s):
    # "[[" is an escaped literal bracket: the text after it is plain text, not a tag
    s = s.replace('[[', '\x00')
    toks = TOK.findall(s) + ['[['] * s.count('\x00')
    # bracketed readable words like [Latest] / [optional objective] are text, not color tags
    toks = [t for t in toks if not (t.startswith('[') and (' ' in t or any(ord(c) > 127 for c in t) or (len(t) > 2 and t[1].isupper() and not t[1:-1].isupper() and '#' not in t)))]
    return collections.Counter(toks)

en = parse(f'{bundles}/bundle.properties')
existing = parse(f'{bundles}/bundle_ar.properties')

translated = {}
for f in sorted(glob.glob(f'{trdir}/out_*.json')):
    translated.update(json.load(open(f, encoding='utf-8')))

bad = []
for k, v in translated.items():
    if k not in en:
        bad.append((k, 'unknown key')); continue
    if tokens(en[k]) != tokens(v):
        bad.append((k, f'tokens differ: en={dict(tokens(en[k]))} ar={dict(tokens(v))}'))

final = dict(existing)
for k, v in translated.items():
    if not any(b[0] == k for b in bad):
        final[k] = v

with open(f'{bundles}/bundle_ar.properties', 'w', encoding='utf-8') as fp:
    for k in en:
        if k in final:
            v = final[k].replace('\n', '\\n')
            fp.write(f'{k} = {v}\n')

print(f'translated: {len(translated)}, rejected: {len(bad)}, written: {sum(1 for k in en if k in final)} / {len(en)}')
for k, why in bad[:40]:
    print(' -', k, why)
