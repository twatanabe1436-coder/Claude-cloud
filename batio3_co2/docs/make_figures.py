#!/usr/bin/env python3
"""Regenerate the README figures (docs/img/*.png)."""

import os
import sys
import warnings

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
from ase.visualize.plot import plot_atoms  # noqa: E402

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
from btoco2.viz import COLORS, RADII  # noqa: E402

OUT = os.path.join(os.path.dirname(__file__), "img")
os.makedirs(OUT, exist_ok=True)


def draw(ax, atoms, title, rotation="-90x,20y", repeat=None):
    a = atoms
    ads = atoms.arrays.get("adsorbate", np.zeros(len(atoms), bool)).astype(bool)
    if repeat:
        a = atoms.repeat(repeat)
        ads = np.tile(ads, int(np.prod(repeat)))
    syms = a.get_chemical_symbols()
    colors = [COLORS[s] for s in syms]
    radii = [RADII[s] * (1.3 if f else 1.0) for s, f in zip(syms, ads)]
    plot_atoms(a, ax, rotation=rotation, colors=colors, radii=radii, show_unit_cell=2)
    ax.set_title(title, fontsize=10)
    ax.set_axis_off()


def legend(fig):
    from matplotlib.lines import Line2D

    h = [Line2D([], [], marker="o", ls="", ms=9, mfc=COLORS[s], mec="k", label=s) for s in ("Ba", "Ti", "O", "C")]
    fig.legend(handles=h, loc="lower center", ncol=4, frameon=False, fontsize=9)


def save(fig, name):
    fig.savefig(os.path.join(OUT, name), dpi=100, bbox_inches="tight")
    plt.close(fig)
    print("wrote", name)


# 1. open surfaces --------------------------------------------------------------
fig, axs = plt.subplots(1, 3, figsize=(12, 4.2))
s_b = make_slab(top="BaO", nlayers=7, size=(2, 2))
s_t = make_slab(top="TiO2", nlayers=7, size=(2, 2))
cb = surface_configurations(s_b, surface_sites(s_b))
ct = surface_configurations(s_t, surface_sites(s_t))
for ax, (at, t) in zip(axs, [(cb["top_O__carbonate_x"], "BaO-term: carbonate guess on O"),
                             (ct["top_Ti__bidentate_x"], "TiO2-term: bent CO2 on Ti"),
                             (ct["hollow_Ba__flat_diag"], "TiO2-term: flat CO2 (physisorption)")]):
    draw(ax, at, t, repeat=(2, 1, 1))
legend(fig)
save(fig, "surface.png")

# 2. slit pores -----------------------------------------------------------------
fig, axs = plt.subplots(1, 3, figsize=(12, 5))
p7 = slit_pore_periodic(nlayers=7, top="TiO2", gap=7.0)
p10 = slit_pore_periodic(nlayers=6, top="TiO2", gap=10.0)
sw = slit_pore_sandwich(face_bottom="BaO", face_top="TiO2", gap=8.0)
draw(axs[0], midgap_configurations(p7)["mid_hollow_Ba__flat_x"], "periodic, TiO2|TiO2, gap 7 A\n(2 periods shown)",
     repeat=(2, 1, 2))
draw(axs[1], midgap_configurations(p10)["mid_hollow_Ba__flat_x"], "periodic, TiO2|BaO, gap 10 A\n(2 periods shown)",
     repeat=(2, 1, 2))
draw(axs[2], midgap_configurations(sw)["mid_hollow_O__flat_x"], "sandwich + vacuum, BaO|TiO2, gap 8 A",
     repeat=(2, 1, 1))
legend(fig)
save(fig, "slit.png")

# 3. wedge pores ----------------------------------------------------------------
fig, axs = plt.subplots(1, 3, figsize=(14, 5))
for ax, ang in zip(axs[:2], (15, 30)):
    w = tilted_plate_wedge(angle=ang)
    confs = list(wedge_configurations(w, n=3, orientations=("flat_y",)).values())
    draw(ax, confs[len(confs) // 2], f"tilted plate {ang} deg (periodic in y)\n"
                                    f"gap {w.info['apex_gap']:.0f} -> {w.info['gap_open']:.1f} A")
st = stepped_wedge(profile="V", terrace=1, h0=0, nx=8)
confs = list(wedge_configurations(st, orientations=("flat_y",)).values())
draw(axs[2], confs[1], "stepped wedge cavity (fully periodic)\nroof slope 1/1 = 45 deg, stoichiometric")
legend(fig)
save(fig, "wedge.png")

# 4. NEB R1 ---------------------------------------------------------------------
fig, axs = plt.subplots(1, 3, figsize=(12, 4.2))
r1 = vacancy_healing_endpoints(make_slab(top="TiO2", nlayers=7, size=(2, 2)))
imgs = interpolate_path(r1["IS"], r1["FS"], 7)
for ax, (im, t) in zip(axs, [(imgs[0], "IS: CO2 at O vacancy"), (imgs[3], "IDPP image 4/7"),
                             (imgs[-1], "FS: vacancy healed + CO")]):
    draw(ax, im, t, rotation="-90x,-20y")
legend(fig)
save(fig, "neb_R1.png")
