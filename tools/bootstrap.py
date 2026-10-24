"""Download the official Android command line tools and Gradle into ignored .tools/."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[1]
TOOLS = ROOT / '.tools'
TOOLS.mkdir(exist_ok=True)

def fetch(item):
    name, url, target = item
    if target.exists():
        print(f'{name}: already installed', flush=True)
        return
    archive = TOOLS / (name + '.zip')
    if not archive.exists():
        print(f'{name}: downloading official distribution', flush=True)
        urllib.request.urlretrieve(url, archive)
    with zipfile.ZipFile(archive) as z:
        z.extractall(TOOLS / ('android-sdk/cmdline-tools' if name == 'android' else ''))
    if name == 'android':
        (TOOLS / 'android-sdk/cmdline-tools/cmdline-tools').rename(target)
    print(f'{name}: ready', flush=True)

with ThreadPoolExecutor(max_workers=2) as pool:
    list(pool.map(fetch, [
        ('android', 'https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip', TOOLS / 'android-sdk/cmdline-tools/latest'),
        ('gradle', 'https://services.gradle.org/distributions/gradle-8.13-bin.zip', TOOLS / 'gradle-8.13'),
    ]))
