#!/usr/bin/env python3
"""Diff the Android palette against the prototype, token by token.

Two sources must agree, hex for hex:

  1. ``prototype/styles.css``           -- ``:root`` / ``:root[data-theme="dark"]``, the source of truth
  2. ``app/.../ui/theme/EvenColors.kt`` -- ``LightEvenColors`` / ``DarkEvenColors``

Run from the repo root (or anywhere -- paths are resolved relative to this file)::

    python app/tools/verify_palette.py

Exits 0 when every token lines up and nothing is missing or extra, 1 otherwise.
"""
from __future__ import annotations

import math
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[2]
CSS = ROOT / "prototype" / "styles.css"
KT = ROOT / "app" / "app" / "src" / "main" / "java" / "com" / "even" / "app" / "ui" / "theme" / "EvenColors.kt"

# Prototype-only chrome: the fake phone frame and the click-through bar around it. They paint
# nothing inside the product, so the Android theme deliberately has no counterpart.
CHROME_ONLY = {
    "--stage",
    "--device-border",
    "--device-shadow",
    "--chrome-bg",
    "--chrome-ink",
    "--chrome-ink-strong",
    "--chrome-line",
}
# Not a colour: a corner radius belongs with the shapes, not with EvenColors.
NON_COLOUR = {"--radius"}


def argb(value: str) -> str:
    """Normalise a CSS colour to an uppercase AARRGGBB string."""
    value = value.strip()
    hex_match = re.fullmatch(r"#([0-9a-fA-F]{6})", value)
    if hex_match:
        return "FF" + hex_match.group(1).upper()
    rgba = re.fullmatch(r"rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)\s*(?:,\s*([\d.]+)\s*)?\)", value)
    if rgba:
        r, g, b = (int(float(rgba.group(i))) for i in (1, 2, 3))
        alpha = float(rgba.group(4)) if rgba.group(4) is not None else 1.0
        a = int(math.floor(alpha * 255 + 0.5))
        return f"{a:02X}{r:02X}{g:02X}{b:02X}"
    raise ValueError(f"not a colour: {value!r}")


def kotlin_name(token: str) -> str:
    """``--paper-alt`` -> ``paperAlt``."""
    head, *rest = token.removeprefix("--").split("-")
    return head + "".join(part.capitalize() for part in rest)


def parse_css() -> tuple[dict[str, str], dict[str, str]]:
    css = CSS.read_text(encoding="utf-8")

    def block(selector: str) -> dict[str, str]:
        body = re.search(re.escape(selector) + r"\s*\{(.*?)\n\}", css, re.S)
        if body is None:
            raise SystemExit(f"{CSS}: no {selector} block")
        return dict(re.findall(r"(--[\w-]+)\s*:\s*([^;]+);", body.group(1)))

    return block(":root"), block(':root[data-theme="dark"]')


def parse_kotlin() -> tuple[dict[str, str], dict[str, str]]:
    kt = KT.read_text(encoding="utf-8")

    def block(name: str) -> dict[str, str]:
        body = re.search(rf"val {name} = EvenColors\((.*?)\n\)", kt, re.S)
        if body is None:
            raise SystemExit(f"{KT}: no {name}")
        found = re.findall(r"(\w+)\s*=\s*Color\(0x([0-9A-Fa-f]{8})\)", body.group(1))
        return {prop: value.upper() for prop, value in found}

    return block("LightEvenColors"), block("DarkEvenColors")


def main() -> int:
    css_l, css_d = parse_css()
    kt_l, kt_d = parse_kotlin()

    problems: list[str] = []

    # 1. a token the dark theme never overrides would silently keep its light value
    expected = {t for t in css_l if t not in CHROME_ONLY and t not in NON_COLOUR}
    for token in sorted(expected - css_d.keys()):
        problems.append(f"styles.css declares {token} in :root but never overrides it for dark")

    # 2. every product token must be in EvenColors, with the identical ARGB
    rows = []
    for token in sorted(expected & css_d.keys()):
        prop = kotlin_name(token)
        want_l, want_d = argb(css_l[token]), argb(css_d[token])
        got_l, got_d = kt_l.get(prop), kt_d.get(prop)
        if got_l is None or got_d is None:
            problems.append(f"EvenColors has no `{prop}` for {token}")
            rows.append((token, css_l[token], css_d[token], prop, "MISSING"))
            continue
        ok = got_l == want_l and got_d == want_d
        if not ok:
            problems.append(
                f"{token} -> {prop}: light css {want_l} vs kt {got_l}, dark css {want_d} vs kt {got_d}"
            )
        rows.append((token, css_l[token], css_d[token], prop, "ok" if ok else "MISMATCH"))

    # 3. nothing invented downstream
    for prop in sorted(set(kt_l) - {kotlin_name(t) for t in expected}):
        problems.append(f"EvenColors.{prop} has no token in styles.css")

    # 4. the one token the platform also needs as an Android resource (cold-start window, splash,
    #    system bars) must not drift from EvenColors
    for res, want in (("values", argb(css_l["--paper"])), ("values-night", argb(css_d["--paper"]))):
        path = ROOT / "app" / "app" / "src" / "main" / "res" / res / "colors.xml"
        found = re.search(r'name="even_paper">\s*(#[0-9a-fA-F]{6})\s*<', path.read_text(encoding="utf-8"))
        if found is None:
            problems.append(f"{path}: no even_paper")
        elif argb(found.group(1)) != want:
            problems.append(f"res/{res}/colors.xml even_paper = {found.group(1)}, expected {want[2:]}")

    width = max(len(r[0]) for r in rows)
    print(f"{'token'.ljust(width)} | {'light':24} | {'dark':24} | {'kotlin':16} |")
    print(f"{'-' * width} | {'-' * 24} | {'-' * 24} | {'-' * 16} |")
    for token, l, d, prop, status in rows:
        print(f"{token.ljust(width)} | {l:24} | {d:24} | {prop:16} | {status}")

    print()
    print(f"{len(css_l)} colour tokens in styles.css :root "
          f"({len(CHROME_ONLY)} prototype-chrome + {len(NON_COLOUR)} non-colour skipped)")
    print(f"{len(rows)} tokens compared across styles.css / EvenColors.kt")
    print(f"skipped on Android: {', '.join(sorted(CHROME_ONLY | NON_COLOUR))}")
    if problems:
        print(f"\n{len(problems)} PROBLEM(S):")
        for p in problems:
            print(f"  - {p}")
        return 1
    print("\nOK — both sources agree, hex for hex.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
