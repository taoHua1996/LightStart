"""Small ADB UI inspection helper. Always pass an explicitly selected device."""
import argparse
import os
from pathlib import Path
import re
import subprocess
import shutil
import sys
import xml.etree.ElementTree as ET

sys.stdout.reconfigure(encoding='utf-8')

parser = argparse.ArgumentParser()
parser.add_argument('serial')
parser.add_argument('action', choices=['dump', 'click', 'screenshot'])
parser.add_argument('value', nargs='?')
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
adb = root / '.tools/android-sdk/platform-tools/adb.exe'
if not adb.exists():
    sdk = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    candidate = Path(sdk) / 'platform-tools/adb.exe' if sdk else None
    resolved = str(candidate) if candidate and candidate.exists() else shutil.which('adb')
    if not resolved:
        raise SystemExit('ADB not found. Configure ANDROID_HOME or add platform-tools to PATH.')
    adb = Path(resolved)

def run(*command):
    return subprocess.run([str(adb), '-s', args.serial, *command], check=True, capture_output=True).stdout

if args.action == 'screenshot':
    path = Path(args.value)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(run('exec-out', 'screencap', '-p'))
    print(path.resolve())
else:
    run('shell', 'uiautomator', 'dump', '/sdcard/qingping-window.xml')
    document = ET.fromstring(run('shell', 'cat', '/sdcard/qingping-window.xml'))
    nodes = list(document.iter('node'))
    if args.action == 'dump':
        for node in nodes:
            if node.get('text') or node.get('content-desc'):
                print({k: node.get(k) for k in ['text', 'content-desc', 'resource-id', 'bounds', 'checked']})
    else:
        matches = [n for n in nodes if n.get('text') == args.value and n.get('enabled') == 'true']
        if len(matches) != 1:
            raise SystemExit(f'Expected one visible match, found {len(matches)}')
        bounds = list(map(int, re.findall(r'\d+', matches[0].get('bounds'))))
        run('shell', 'input', 'tap', str((bounds[0] + bounds[2]) // 2), str((bounds[1] + bounds[3]) // 2))
        print('Clicked:', args.value)
