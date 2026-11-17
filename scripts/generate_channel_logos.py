#!/usr/bin/env python3
"""Create a small offline bundle of *actual* channel logos, not text-only tiles.

Input: owner's editable playlist + iptv-org public channel logo metadata.
Output: custom/logo_pack.json, updated tvg-logo/tvg-id in playlist/vietnam.m3u,
        and custom/logo_provenance.json with source URLs.
Run only to refresh the bundled images; updating M3U alone needs no APK rebuild.
Requires Pillow; previews should remain drawable without an Internet connection.
"""
import base64
from io import BytesIO
import json
import re
import urllib.request
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
PLAYLIST = ROOT / "playlist/vietnam.m3u"
LOGOS = "https://iptv-org.github.io/api/logos.json"

def normal(name):
    return re.sub(r"[^\w]", "", name.lower())

def channel_id(name):
    s = name.lower().replace("hd", "").strip()
    m = re.match(r"vtv(\d+)", s)
    if m:
        if "tay nam bo" in s: return "VTV5TayNamBo.vn"
        return "VTV" + m.group(1) + ".vn"
    m = re.match(r"htv(\d+)", s)
    if m: return "HTV" + m.group(1) + ".vn"
    if "htv the thao" in s: return "HTVSports.vn"
    m = re.match(r"thvl(\d+)", s)
    if m: return "VinhLongTV" + m.group(1) + ".vn"
    m = re.match(r"dong nai (\d+)", s)
    if m: return "DongNaiTV" + m.group(1) + ".vn"
    m = re.match(r"dong thap", s)
    if m: return "DongThapTV.vn"
    m = re.match(r"can tho (\d+)", s)
    if m: return "CanThoTV" + (m.group(1) if m.group(1) != "1" else "") + ".vn"
    m = re.match(r"da nang (\d+)", s)
    if m: return "DaNangTV" + m.group(1) + ".vn"
    if "an ninh" in s: return "AnNinhTV.vn"
    if "qpvn" in s: return "QPVN.vn"
    if "vietnam today" in s: return "VietnamToday.vn"
    names = {
        "tay ninh": "TayNinhTV.vn",
        "ha noi": "HanoiTV1.vn",
        "thai nguyen": "ThaiNguyenTV.vn",
        "cao bang": "CaoBangTV.vn",
        "lao cai": "LaoCaiTV.vn",
        "hung yen": "HungYenTV.vn",
        "nghe an": "NgheAnTV.vn",
        "ha tinh": "HaTinhTV.vn",
        "dien bien": "DienBienTV.vn",
        "dak lak": "DakLakTV.vn",
        "quang ngai": "QuangNgaiTV.vn",
    }
    for needle, channel in names.items():
        if needle in s: return channel
    return ""

def load_url(url, limit=3_000_000):
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (TendooIPTV-LogoBundle/1.0)"})
    with urllib.request.urlopen(req, timeout=15) as r:
        raw = r.read(limit+1)
        if len(raw)>limit: raise ValueError("Logo exceeds file-size limit")
        return raw

def main():
    data=json.loads(load_url(LOGOS,12_000_000).decode())
    # Prefer metadata marked in use, with PNG from Commons or Imgur when available.
    by_id={}
    for row in data:
        if not row.get("in_use") or not row.get("channel"): continue
        key = row["channel"]
        by_id.setdefault(key,[]).append(row)
    lines=PLAYLIST.read_text(encoding="utf-8").splitlines()
    logos={}
    provenance={}
    updated=[]
    found=0
    missing=[]
    for line in lines:
        if not line.startswith("#EXTINF"):
            updated.append(line)
            continue
        name=line.rsplit(",",1)[-1].strip()
        cid=channel_id(name)
        options=by_id.get(cid,[])
        # Prefer PNG, then WebP, then other raster formats. Download and validate.
        options=sorted(options,key=lambda r:(0 if r.get("format","").upper()=="PNG" else 1,
                0 if "wikimedia.org" in r.get("url","") else 1))
        decoded=None
        used=""
        for opt in options[:5]:
            try:
                raw=load_url(opt["url"])
                im=Image.open(BytesIO(raw))
                im.load()
                if im.width < 20 or im.height < 12: continue
                im=im.convert("RGBA")
                im.thumbnail((220,118),Image.Resampling.LANCZOS)
                # Preserve the exact aspect ratio and transparent edges of original broadcasters' logos.
                buffer=BytesIO()
                im.save(buffer,format="PNG",optimize=True,compress_level=9)
                compressed=buffer.getvalue()
                if len(compressed)>42_000: continue
                decoded=base64.b64encode(compressed).decode("ascii")
                used=opt["url"]
                break
            except Exception as e:
                print(name,"logo source failed:",str(e)[:70])
        if decoded:
            found+=1
            logos[name]=decoded
            provenance[name]={"channel_id":cid,"logo_url":used}
            meta=line.rsplit(",",1)[0]
            meta=re.sub(r'\s+tvg-id="[^"]*"', "",meta)
            meta=re.sub(r'\s+tvg-logo="[^"]*"', "",meta)
            meta += f' tvg-id="{cid}" tvg-logo="{used}"'
            line=meta+","+name
        else:
            missing.append(name)
        updated.append(line)
    (ROOT/"custom/logo_pack.json").write_text(
        json.dumps(logos,ensure_ascii=True,separators=(",",":")),encoding="utf-8")
    (ROOT/"custom/logo_provenance.json").write_text(
        json.dumps(provenance,ensure_ascii=False,indent=2),encoding="utf-8")
    PLAYLIST.write_text("\n".join(updated)+"\n",encoding="utf-8")
    print(f"Bundled real logos for {found} of {len(logos)+len(missing)} channels, total",
        (ROOT/"custom/logo_pack.json").stat().st_size,"bytes")
    print("Missing logos:",missing)

if __name__ == "__main__":
    main()
