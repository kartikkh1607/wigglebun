"""Render design/refresh/mockup.html to previews/*.png with headless Chrome.

    python design/refresh/render.py [chrome_path]

Serves the repo on localhost so the page can load the app's bundled fonts (Chrome blocks fonts
from file:// pages). Mockups only: nothing here is used by the app.
"""
import functools
import http.server
import os
import shutil
import subprocess
import sys
import threading

HERE = os.path.dirname(os.path.abspath(__file__))
REPO = os.path.dirname(os.path.dirname(HERE))
OUT = os.path.join(HERE, "previews")

# (query, file name, CSS width, CSS height); rendered at 2x.
BOARDS = [
    ("a", "direction_a_honey_loft.png", 1380, 1262),
    ("b", "direction_b_garden_studio.png", 1380, 1262),
    ("c", "direction_c_bold_playroom.png", 1380, 1262),
    ("icons", "icons.png", 1260, 760),
]


class QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, *args):
        pass

CANDIDATES = [
    r"C:\Program Files\Google\Chrome\Application\chrome.exe",
    r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe",
    "/usr/bin/google-chrome", "/usr/bin/chromium", "/usr/bin/chromium-browser",
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
]


def find_chrome():
    if len(sys.argv) > 1:
        return sys.argv[1]
    for c in CANDIDATES:
        if os.path.exists(c):
            return c
    found = shutil.which("chrome") or shutil.which("google-chrome") or shutil.which("chromium")
    if not found:
        sys.exit("No Chrome/Chromium found; pass its path as the first argument.")
    return found


def main():
    chrome = find_chrome()
    handler = functools.partial(QuietHandler, directory=REPO)
    server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    port = server.server_address[1]
    os.makedirs(OUT, exist_ok=True)
    try:
        for query, name, width, height in BOARDS:
            url = f"http://127.0.0.1:{port}/design/refresh/mockup.html?d={query}"
            out = os.path.join(OUT, name)
            subprocess.run([
                chrome, "--headless=new", "--disable-gpu", "--hide-scrollbars",
                "--force-device-scale-factor=2", f"--window-size={width},{height}",
                "--virtual-time-budget=5000", f"--screenshot={out}", url,
            ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            print("wrote", os.path.relpath(out, REPO))
    finally:
        server.shutdown()


if __name__ == "__main__":
    main()
