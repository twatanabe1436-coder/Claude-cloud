#!/usr/bin/env python3
"""Build docs/structure_viewer.html: an interactive 3D viewer of the main models.

The page draws atoms with its own small canvas renderer (no external library),
so it works offline and inside sandboxed viewers.
"""

import json
import os
import sys
import warnings

import numpy as np

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
warnings.filterwarnings("ignore")

from btoco2 import (  # noqa: E402
    make_slab,
    midgap_configurations,
    slit_pore_periodic,
    slit_pore_sandwich,
    stepped_wedge,
    surface_configurations,
    surface_sites,
    tilted_plate_wedge,
    wedge_configurations,
)
from btoco2.reactions import interpolate_path, vacancy_healing_endpoints  # noqa: E402
from btoco2.slab import fixed_mask  # noqa: E402

HERE = os.path.dirname(__file__)
BOND = {frozenset(("Ti", "O")): 2.3, frozenset(("C", "O")): 1.6}


def pack(atoms):
    pos = atoms.positions
    sym = atoms.get_chemical_symbols()
    bonds = []
    for i in range(len(atoms)):
        d = np.linalg.norm(pos[i + 1:] - pos[i], axis=1)
        for k in np.where(d < 2.35)[0]:
            j = i + 1 + int(k)
            cut = BOND.get(frozenset((sym[i], sym[j])))
            if cut and d[k] < cut:
                bonds.append([i, j])
    ads = atoms.arrays.get("adsorbate", np.zeros(len(atoms), bool)).astype(int).tolist()
    return {
        "el": "".join({"Ba": "B", "Ti": "T", "O": "O", "C": "C"}[s] for s in sym),
        "xyz": np.round(pos, 3).ravel().tolist(),
        "bonds": bonds,
        "ads": ads,
        "fix": fixed_mask(atoms).astype(int).tolist(),
        "cell": np.round(atoms.cell.array, 3).tolist(),
    }


def entry(key, group, title, desc, atoms, view="side", rep=(1, 1, 1), facts=None, frames=None):
    facts = [["原子数", str(len(atoms))]] + [f for f in (facts or []) if f[0] != "原子数"]
    e = {"key": key, "group": group, "title": title, "desc": desc, "view": view, "rep": list(rep),
         "facts": facts, "n": len(atoms)}
    e["frames"] = [pack(a) for a in (frames or [atoms])]
    return e


models = []

s_b = make_slab(top="BaO", nlayers=7, size=(2, 2))
s_t = make_slab(top="TiO2", nlayers=7, size=(2, 2))
cb = surface_configurations(s_b, surface_sites(s_b))
ct = surface_configurations(s_t, surface_sites(s_t))
models.append(entry(
    "carbonate", "表面", "BaO 終端 + 炭酸塩型 CO2",
    "7 層スラブ (BaO 終端)。CO2 の C を表面の O の上 1.45 Å に置き、O-C-O を 130° に曲げた炭酸塩 (CO3) 型の初期構造。"
    "下 3 層は固定。上には 20 Å の真空。",
    cb["top_O__carbonate_x"], rep=(2, 2, 1),
    facts=[["原子数", "71"], ["セル", "8 × 8 × 32 Å"], ["被覆率", "1/4 ML"]]))
models.append(entry(
    "tio2flat", "表面", "TiO2 終端 + 寝かせた CO2",
    "7 層スラブ (TiO2 終端)。CO2 を表面から 2.9 Å の高さに寝かせて置いた物理吸着の初期構造。"
    "同じようにサイト × 向きの組み合わせで約 30 通りを生成する。",
    ct["top_Ti__flat_x"], rep=(2, 2, 1),
    facts=[["原子数", "75"], ["CO2 の高さ", "2.9 Å"], ["被覆率", "1/4 ML"]]))

p7 = slit_pore_periodic(nlayers=7, top="TiO2", gap=7.0)
models.append(entry(
    "slitp", "スリット細孔", "平行スリット (周期積層型)",
    "1 枚のスラブの上面と、その周期像の下面が向かい合って細孔になる。真空なし。"
    "「並べる」を ON にすると上下にも積み重なった様子 (細孔が周期的に並ぶ) が見える。",
    midgap_configurations(p7)["mid_hollow_Ba__flat_x"], rep=(2, 2, 2),
    facts=[["壁の間隔", "7.0 Å"], ["CO2 が使える幅", "≈ 4 Å"], ["原子数", "75"]]))
sw = slit_pore_sandwich(face_bottom="BaO", face_top="TiO2", gap=8.0)
models.append(entry(
    "slits", "スリット細孔", "平行スリット (サンドイッチ型)",
    "独立した 2 枚のスラブで CO2 を挟み、外側は真空。上下の壁の終端 (ここでは下 BaO / 上 TiO2) やずれを自由に選べる。",
    midgap_configurations(sw)["mid_hollow_O__flat_x"], rep=(2, 2, 1),
    facts=[["壁の間隔", "8.0 Å"], ["壁", "BaO | TiO2"], ["原子数", "143"]]))

w20 = tilted_plate_wedge(angle=20)
wc = list(wedge_configurations(w20, n=5, orientations=("flat_y",)).values())
models.append(entry(
    "plate", "楔形細孔", "傾斜プレート 20°",
    "下は普通のスラブ、上は有限幅のプレートを 20° 傾けて置いた楔。y 方向 (奥行き) にだけ周期的。"
    "隙間は左の頂点で 4 Å、右の開口で 8.8 Å。CO2 は隙間が約 7 Å の位置。プレートは外側 2 層を固定。",
    wc[2], rep=(1, 2, 1),
    facts=[["角度", "20°"], ["隙間", "4.0 → 8.8 Å"], ["原子数", "239"]]))
w30 = tilted_plate_wedge(angle=30)
wc30 = list(wedge_configurations(w30, n=5, orientations=("flat_y",)).values())
models.append(entry(
    "plate30", "楔形細孔", "傾斜プレート 30°",
    "同じモデルで角度を 30° にしたもの。開口側の隙間が 11 Å まで広がる。",
    wc30[1], rep=(1, 2, 1),
    facts=[["角度", "30°"], ["隙間", "4.0 → 11.0 Å"], ["原子数", "239"]]))
st = stepped_wedge(profile="V", terrace=1, h0=0, nx=8)
sc = list(wedge_configurations(st, orientations=("flat_y",)).values())
models.append(entry(
    "step", "楔形細孔", "階段状キャビティ (V 形, 45°)",
    "周期結晶から BaTiO3 の単位ブロックを抜いて作ったテント形の空洞。天井は 1 段ごとに 1 ブロック下がる階段 (平均 45°)。"
    "両端が閉じた楔の頂点。化学量論・電気的中性がそのまま保たれ、真空も端もない。",
    sc[1], rep=(1, 1, 1),
    facts=[["天井の傾き", "45° (階段)"], ["隙間", "6 / 10 / 14 Å"], ["原子数", "443"]]))

r1 = vacancy_healing_endpoints(s_t)
imgs = interpolate_path(r1["IS"], r1["FS"], 7)
models.append(entry(
    "neb", "反応経路", "R1: 酸素欠陥での CO2 → CO",
    "TiO2 終端の表面 O を 1 つ抜いた欠陥に CO2 の O が入り、CO が離れていく経路 (IDPP で補間した初期経路、7 像)。"
    "スライダーで像を切り替えられる。実際の計算ではこの両端を緩和してから CI-NEB を行う。",
    imgs[0], rep=(2, 2, 1), view="tilt",
    facts=[["像の数", "7"], ["動く原子", "C, O, O"], ["原子数", "74"]], frames=imgs))

data = json.dumps(models, separators=(",", ":"), ensure_ascii=False)
tmpl = open(os.path.join(HERE, "viewer_template.html"), encoding="utf-8").read()
out = os.path.join(HERE, "structure_viewer.html")
with open(out, "w", encoding="utf-8") as f:
    f.write(tmpl.replace("/*__DATA__*/[]", data))
print(f"wrote {out} ({os.path.getsize(out) / 1024:.0f} kB, {len(models)} models)")
