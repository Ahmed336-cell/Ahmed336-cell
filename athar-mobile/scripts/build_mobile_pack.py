#!/usr/bin/env python3
"""Build the offline Athar Mobile corpus from an athar-ledger public/data directory."""

from __future__ import annotations

import json
import sys
from pathlib import Path


def decode_grades(raw: dict, ref: str) -> dict | None:
    scholars = raw["scholars"]
    labels = raw["grades"]
    arr = raw["map"].get(ref)
    if ref.startswith("bukhari:"):
        return {"g": "في صحيح البخاري", "s": None}
    if ref.startswith("muslim:"):
        return {"g": "في صحيح مسلم", "s": None}
    if not isinstance(arr, list) or len(arr) < 2:
        return None
    si, gi = arr[0], arr[1]
    if 0 <= si < len(scholars) and 0 <= gi < len(labels):
        return {"g": labels[gi], "s": scholars[si]}
    return None


def main() -> int:
    if len(sys.argv) < 2:
        print("usage: build_mobile_pack.py /path/to/athar-ledger/public/data [out_dir]")
        return 2
    src = Path(sys.argv[1])
    out = Path(sys.argv[2]) if len(sys.argv) > 2 else Path(__file__).resolve().parents[1] / "app/src/main/assets/corpus"
    out.mkdir(parents=True, exist_ok=True)

    passages: list[dict] = []
    meta_src = json.loads((src / "meta.json").read_text(encoding="utf-8"))
    for i in range(int(meta_src.get("shards", 5))):
        passages.extend(json.loads((src / f"passages_{i}.json").read_text(encoding="utf-8")))

    mobile = [p for p in passages if p.get("t") == "q"]
    for pref in ("nawawi:", "qudsi:"):
        mobile.extend(p for p in passages if str(p.get("r", "")).startswith(pref))
    buk = sorted(
        (p for p in passages if str(p.get("r", "")).startswith("bukhari:")),
        key=lambda p: len(p.get("n", "")),
    )
    mobile.extend(buk[:40])

    grades_raw = json.loads((src / "grades.json").read_text(encoding="utf-8"))
    grade_map = {}
    for p in mobile:
        if p.get("t") != "h":
            continue
        decoded = decode_grades(grades_raw, p["r"])
        if decoded:
            grade_map[p["r"]] = decoded

    pack = [{"t": p["t"], "r": p["r"], "n": p["n"], "m": p.get("m", 1)} for p in mobile]
    meta = {
        "version": 1,
        "name": "athar-mobile-core",
        "quran": sum(1 for p in pack if p["t"] == "q"),
        "hadith": sum(1 for p in pack if p["t"] == "h"),
        "passages": len(pack),
        "source": "https://github.com/Ahmedanwernouser/athar-ledger",
    }
    (out / "passages.json").write_text(json.dumps(pack, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    (out / "grades.json").write_text(json.dumps({"map": grade_map}, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    (out / "meta.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(meta, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
