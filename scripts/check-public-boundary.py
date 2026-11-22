#!/usr/bin/env python3
"""Tiny source-only S0 pilot review: narrow, heuristic, not a credential scanner certificate."""
from pathlib import Path
import xml.etree.ElementTree as ET
import json

root = Path(__file__).resolve().parents[1]
paths = [root/'README.md'] + list((root/'app').rglob('*')) + list((root/'docs').rglob('*')) + list((root/'contracts').rglob('*')) + list((root/'tests').rglob('*'))
text_paths = [p for p in paths if p.is_file() and p.suffix in ('.md','.xml','.java','.json','.gradle')]
# No private corpus stable name, account-specific identifiers, common hard-coded secret prefixes.
forbidden = ['CONVERSATIONS_CHUNKS_PRIVATE', 'ghp_', 'github_pat_', '-----BEGIN PRIVATE KEY-----',
             'service_role=', 'api_key=', 'access_token=', '1KhKSXTgTKyZLLAodNnZpvE4G-GuLgl2EN4pFVQLoHVY']
for p in text_paths:
    content = p.read_text(encoding='utf-8')
    for pattern in forbidden:
        if pattern in content:
            raise SystemExit(f'FAIL_SCOPED_PUBLIC_BOUNDARY:{p.relative_to(root)}:{pattern[:12]}')

manifest = ET.parse(root/'app/src/main/AndroidManifest.xml').getroot()
uses = [el.attrib.get('{http://schemas.android.com/apk/res/android}name')
        for el in manifest.findall('uses-permission')]
if uses != ['android.permission.INTERNET']:
    raise SystemExit('FAIL_SCOPED_UNEXPECTED_ANDROID_PERMISSION')
if 'android:resizeableActivity="true"' not in (root/'app/src/main/AndroidManifest.xml').read_text():
    raise SystemExit('FAIL_SCOPED_MISSING_RESIZABLE_ACTIVITY')
if json.loads((root/'contracts/capability-request.s0.schema.json').read_text())['additionalProperties'] is not False:
    raise SystemExit('FAIL_SCOPED_SCHEMA_OPEN_FIELDS')
print(f'PASS_SCOPED_PUBLIC_BOUNDARY scanned={len(text_paths)} textual_files, permissions=INTERNET_ONLY')
