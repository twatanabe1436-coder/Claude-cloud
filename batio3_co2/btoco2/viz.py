"""Quick PNG previews (side + top view) using ASE's matplotlib renderer."""

from __future__ import annotations

import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt  # noqa: E402
import numpy as np  # noqa: E402
from ase.visualize.plot import plot_atoms  # noqa: E402

# muted, colour-blind-safe element colours (Ba green, Ti grey-blue, O red, C black)
COLORS = {"Ba": "#3f9b6b", "Ti": "#8a9bb0", "O": "#d9473f", "C": "#222222"}
RADII = {"Ba": 1.05, "Ti": 0.62, "O": 0.62, "C": 0.55}


def _style(atoms):
    syms = atoms.get_chemical_symbols()
    colors = [COLORS.get(s, "#999999") for s in syms]
    radii = [RADII.get(s, 0.6) for s in syms]
    if "adsorbate" in atoms.arrays:
        radii = [r * (1.25 if ad else 1.0) for r, ad in zip(radii, atoms.arrays["adsorbate"])]
    return colors, radii


def preview(atoms, path, title=None, views=("side", "top"), repeat=None):
    a = atoms.repeat(repeat) if repeat else atoms
    if repeat and "adsorbate" in atoms.arrays:
        a.set_array("adsorbate", np.tile(atoms.arrays["adsorbate"], int(np.prod(repeat))))
    colors, radii = _style(a)
    rot = {"side": "-90x,20y", "top": "0x", "side_y": "-90x,110y"}
    fig, axes = plt.subplots(1, len(views), figsize=(5.2 * len(views), 4.6))
    axes = np.atleast_1d(axes)
    for ax, v in zip(axes, views):
        plot_atoms(a, ax, rotation=rot[v], colors=colors, radii=radii, show_unit_cell=2)
        ax.set_axis_off()
        ax.set_title({"side": "side view (x-z)", "top": "top view (x-y)", "side_y": "side view (y-z)"}[v],
                     fontsize=10)
    if title:
        fig.suptitle(title, fontsize=12)
    fig.tight_layout()
    fig.savefig(path, dpi=110)
    plt.close(fig)
