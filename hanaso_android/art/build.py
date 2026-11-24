"""Hanaso のイラストを書き出す。

    python art/build.py                  # res/drawable/art_*.xml と ui/art/ArtResources.kt を作り直す
    python art/build.py --preview DIR    # 確認用の SVG (場面・人物・リーグの一覧) を DIR に書く

人物は people.py、場面は scenes.py。画像を直したら、このスクリプトを流して両方をコミットする。
"""

import argparse
import sys
from pathlib import Path

from people import CANVAS, MOODS, PEOPLE, character_group
from scenes import CHAR_SIZE, CHAR_X, CHAR_Y, H, SCENES, W, confetti, league_bg, league_fg
from vector import Drawing, G, P, circle, rect

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res/drawable"
KT = ROOT / "app/src/main/java/io/github/twatanabe1436/hanaso/ui/art/ArtResources.kt"
PERSON = {p.id: p for p in PEOPLE}

# リーグ画面: 表彰台に立つ 3 人 (人物 id, x, y, 大きさ)。順位は 1 位・2 位・3 位
LEAGUE_H = 320
PODIUM = (("alex", 260, 40, 200), ("jamie", 102, 92, 175), ("marco", 448, 118, 165))


def drawables():
    out = {}
    for p in PEOPLE:
        for m in MOODS:
            out[f"art_char_{p.id}_{m}"] = Drawing(CANVAS, CANVAS, character_group(p, m), dp=(120, 120))
    for sid, (fn, _) in SCENES.items():
        bg, fg = fn()
        out[f"art_scene_{sid}_bg"] = Drawing(W, H, bg, dp=(360, 200))
        out[f"art_scene_{sid}_fg"] = Drawing(W, H, fg, dp=(360, 200))
    out["art_confetti"] = Drawing(W, H, confetti(), dp=(360, 200))
    out["art_league_bg"] = Drawing(W, LEAGUE_H, league_bg(), dp=(360, 160))
    out["art_league_fg"] = Drawing(W, LEAGUE_H, league_fg(), dp=(360, 160))
    return out


def kotlin():
    lines = [
        "// art/build.py で生成。手で直さない",
        "package io.github.twatanabe1436.hanaso.ui.art",
        "",
        "import androidx.annotation.DrawableRes",
        "import io.github.twatanabe1436.hanaso.R",
        "",
        "/** イラストの画像 (人物・場面) */",
        "internal object ArtResources {",
        "    /** 場面 id → 相手の人物 id */",
        "    val partner: Map<String, String> = mapOf(",
    ]
    lines += [f'        "{sid}" to "{pid}",' for sid, (_, pid) in SCENES.items()]
    lines += [
        "    )",
        "",
        "    /** リーグの表彰台に立つ人物 (1 位・2 位・3 位) と位置 (x, y, 大きさ。720x320 の座標) */",
        "    val podium: List<Placement> = listOf(",
    ]
    lines += [f'        Placement("{pid}", {x}f, {y}f, {s}f),' for pid, x, y, s in PODIUM]
    lines += [
        "    )",
        "",
        "    @DrawableRes",
        "    fun character(person: String, mood: Mood): Int? = when (person) {",
    ]
    for p in PEOPLE:
        lines.append(f'        "{p.id}" -> when (mood) {{')
        for m in MOODS:
            lines.append(f"            Mood.{m.upper()} -> R.drawable.art_char_{p.id}_{m}")
        lines.append("        }")
    lines += ["        else -> null", "    }", ""]
    for fn, suffix in (("sceneBackground", "bg"), ("sceneForeground", "fg")):
        lines += ["    @DrawableRes", f"    fun {fn}(scene: String): Int? = when (scene) {{"]
        lines += [f'        "{sid}" -> R.drawable.art_scene_{sid}_{suffix}' for sid in SCENES]
        lines += ["        else -> null", "    }", ""]
    lines[-1] = "}"
    return "\n".join(lines) + "\n"


def composite(sid, mood="normal", extra=()):
    fn, pid = SCENES[sid]
    bg, fg = fn()
    return [*bg, G(character_group(PERSON[pid], mood), tx=CHAR_X, ty=CHAR_Y, s=CHAR_SIZE / CANVAS), *fg, *extra]


def previews(out_dir):
    out_dir.mkdir(parents=True, exist_ok=True)
    ids = list(SCENES)
    for name, chunk in (("scenes_a", ids[:10]), ("scenes_b", ids[10:])):
        nodes = []
        for i, sid in enumerate(chunk):
            x, y = (i % 2) * 362, (i // 2) * 202
            nodes.append(G(G(composite(sid), s=0.5), tx=x, ty=y, clip=rect(0, 0, 360, 200, 12)))
        rows = (len(chunk) + 1) // 2
        (out_dir / f"{name}.svg").write_text(Drawing(722, rows * 202, P(rect(0, 0, 722, rows * 202), "#F4F1FB"), nodes).svg(), encoding="utf-8")
    moods = [G(G(composite(sid, m), s=0.5), tx=(i % 3) * 362, ty=0, clip=rect(0, 0, 360, 200, 12)) for i, (sid, m) in enumerate((("cafe", "normal"), ("cafe", "happy"), ("cafe", "puzzled")))]
    (out_dir / "moods.svg").write_text(Drawing(1084, 200, moods).svg(), encoding="utf-8")
    league = league_bg()
    for pid, x, y, s in PODIUM:
        league.append(G(character_group(PERSON[pid], "happy"), tx=x, ty=y, s=s / CANVAS))
    league += league_fg()
    summary = G(G(composite("restaurant", "happy", confetti()), s=0.5), tx=0, ty=170, clip=rect(0, 0, 360, 200, 12))
    (out_dir / "league_summary.svg").write_text(Drawing(720, 370, G(league, s=0.5), summary).svg(), encoding="utf-8")
    print("previews:", out_dir)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--preview", type=Path)
    args = ap.parse_args()
    if args.preview:
        previews(args.preview)
        return
    for old in RES.glob("art_*.xml"):
        old.unlink()
    total = 0
    for name, d in drawables().items():
        text = d.vector()
        (RES / f"{name}.xml").write_text(text, encoding="utf-8", newline="\n")
        total += len(text.encode("utf-8"))
    KT.parent.mkdir(parents=True, exist_ok=True)
    KT.write_text(kotlin(), encoding="utf-8", newline="\n")
    print(f"drawables: {len(list(RES.glob('art_*.xml')))} files, {total // 1024} KB")


if __name__ == "__main__":
    sys.exit(main())
