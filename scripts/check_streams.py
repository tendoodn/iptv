#!/usr/bin/env python3
"""Non-destructive IPTV source health probe. HTTP status is not proof of video playback."""
import concurrent.futures
import ipaddress
import json
import re
import socket
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
USER = ROOT / "playlist/vietnam.m3u"
UPSTREAM = "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/vn.m3u"

def parse(text, origin):
    entries = []
    name, group, tvg = None, "", ""
    for line in text.replace("\r", "").split("\n"):
        line = line.strip()
        if line.startswith("#EXTINF"):
            name = line.rsplit(",", 1)[-1]
            group = re.search(r'group-title="([^"]*)"', line)
            tvg = re.search(r'tvg-id="([^"]*)"', line)
            group = group.group(1) if group else ""
            tvg = tvg.group(1) if tvg else ""
        elif name is not None and line and not line.startswith("#"):
            parsed = urllib.parse.urlparse(line)
            if parsed.scheme == "https" and parsed.port in (None, 443):
                entries.append({"name": name, "group": group, "tvg": tvg,
                                "url": line, "origin": origin})
            name = None
    return entries

def safe_url(url):
    parsed = urllib.parse.urlparse(url)
    host = parsed.hostname
    if parsed.scheme != "https" or not host or parsed.port not in (None, 443):
        return False
    try:
        ip = ipaddress.ip_address(host)
        return ip.is_global
    except ValueError:
        return True

def check(entry):
    url = entry["url"]
    if not safe_url(url):
        return dict(entry, state="skipped", code=0, reason="Unsafe or non-HTTPS endpoint")
    try:
        req = urllib.request.Request(url, headers={
            "User-Agent": "Mozilla/5.0 (Linux; Android TV 11) AppleWebKit/537.36",
            "Accept": "application/vnd.apple.mpegurl, application/x-mpegURL, */*",
            "Accept-Encoding": "identity",
            "Range": "bytes=0-32767",
        })
        with urllib.request.urlopen(req, timeout=9) as resp:
            chunk = resp.read(32768)
            code = resp.status
            valid = b"#EXTM3U" in chunk or b"#EXT-X-" in chunk
            return dict(entry, state="manifest-ok" if valid else "not-hls",
                        code=code, reason=resp.headers.get("Content-Type", ""))
    except Exception as e:
        reason = str(e)
        return dict(entry, state="unreachable", code=0, reason=reason[:130])

def main():
    original = parse(USER.read_text(encoding="utf-8"), "owner")
    try:
        with urllib.request.urlopen(UPSTREAM, timeout=13) as stream:
            external = parse(stream.read().decode("utf-8-sig"), "iptv-org")
    except Exception as e:
        print("External source unavailable:", e)
        external = []
    unique = {}
    for item in original + external:
        unique.setdefault(item["url"], item)
    print("Probing", len(unique), "distinct public URLs", "owner", len(original),
          "iptv-org", len(external))
    with concurrent.futures.ThreadPoolExecutor(max_workers=18) as pool:
        results = list(pool.map(check, unique.values()))
    report = {
        "disclaimer": "Remote GET is a hint, NOT a guarantee of media playback on a Vietnam TV.",
        "owner_count": len(original),
        "upstream_count": len(external),
        "results": results,
    }
    (ROOT / "iptv-health-report.json").write_text(
        json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")
    for state in ["manifest-ok", "not-hls", "unreachable", "skipped"]:
        chosen = [r for r in results if r["state"] == state]
        print("\n", state, len(chosen))
        for item in chosen[:80 if state == "manifest-ok" else 16]:
            print(item["origin"], repr(item["name"]), item["url"][:120], item["reason"][:70])

if __name__ == "__main__":
    main()
