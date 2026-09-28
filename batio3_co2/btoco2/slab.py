"""BaTiO3 bulk and (001) slab builders.

All builders construct the perovskite layer by layer so that the termination,
stoichiometry and layer tags are explicit:

    BaO  layer : Ba (0, 0)      O (1/2, 1/2)
    TiO2 layer : Ti (1/2, 1/2)  O (1/2, 0)  O (0, 1/2)

Layers are stacked along z with spacing a/2 (cubic, paraelectric reference).
``atoms.get_tags()`` holds the layer index counted from the top (1 = outermost
layer), which is what the constraint and site-finding helpers rely on.
"""

from __future__ import annotations

import numpy as np
from ase import Atoms
from ase.constraints import FixAtoms

LAYER_SITES = {
    "BaO": [("Ba", 0.0, 0.0), ("O", 0.5, 0.5)],
    "TiO2": [("Ti", 0.5, 0.5), ("O", 0.5, 0.0), ("O", 0.0, 0.5)],
}

# Default lattice constant (Angstrom). Replace it with your own converged value
# (vc-relax of cubic BaTiO3 with the same functional / pseudopotentials):
# PBE ~4.03, PBEsol ~3.99-4.00, experiment (cubic, 400 K) ~4.00.
A_DEFAULT = 4.00
Z_BOTTOM = 1.0  # z of the lowest atomic layer in cells with vacuum (A)


def other_termination(term: str) -> str:
    check_termination(term)
    return "BaO" if term == "TiO2" else "TiO2"


def check_termination(term: str) -> None:
    if term not in LAYER_SITES:
        raise ValueError(f"termination must be 'BaO' or 'TiO2', got {term!r}")


def bulk_cubic(a: float = A_DEFAULT) -> Atoms:
    """5-atom cubic Pm-3m BaTiO3 cell (Ba at the corner)."""
    return Atoms(
        "BaTiO3",
        scaled_positions=[
            (0.0, 0.0, 0.0),
            (0.5, 0.5, 0.5),
            (0.5, 0.5, 0.0),
            (0.5, 0.0, 0.5),
            (0.0, 0.5, 0.5),
        ],
        cell=[a, a, a],
        pbc=True,
    )


def layer_sequence(nlayers: int, top: str) -> list[str]:
    """Layer types from bottom to top for a slab whose outermost layer is ``top``.

    Odd ``nlayers`` -> symmetric, non-stoichiometric slab (same termination on
    both sides). Even ``nlayers`` -> stoichiometric, asymmetric slab (BaO on one
    side, TiO2 on the other).
    """
    check_termination(top)
    other = other_termination(top)
    return [top if (nlayers - 1 - i) % 2 == 0 else other for i in range(nlayers)]


def stack_layers(seq, a, size=(1, 1), z0=0.0, dz=None) -> Atoms:
    """Stack the given layer types (bottom -> top) into an Atoms object.

    Tags are the layer index from the top (1 = last layer in ``seq``).
    The cell is (nx*a, ny*a, 0); callers set the z vector.
    """
    nx, ny = size
    dz = a / 2.0 if dz is None else dz
    symbols, positions, tags = [], [], []
    n = len(seq)
    for i, lt in enumerate(seq):
        z = z0 + i * dz
        for ix in range(nx):
            for iy in range(ny):
                for sym, fx, fy in LAYER_SITES[lt]:
                    symbols.append(sym)
                    positions.append(((ix + fx) * a, (iy + fy) * a, z))
                    tags.append(n - i)
    atoms = Atoms(symbols, positions=positions, cell=[nx * a, ny * a, 0.0], pbc=[True, True, False])
    atoms.set_tags(tags)
    return atoms


def make_slab(
    a: float = A_DEFAULT,
    nlayers: int = 7,
    top: str = "TiO2",
    size=(2, 2),
    vacuum: float = 20.0,
    nfix: int | None = None,
) -> Atoms:
    """(001) slab with vacuum along z, centred in the cell.

    Parameters
    ----------
    nlayers : number of atomic layers (7 = 3.5 unit cells is the usual minimum).
    top     : termination of the upper (adsorption) surface, 'TiO2' or 'BaO'.
    size    : lateral supercell (nx, ny). (2, 2) = 8x8 A, i.e. 1/4 ML CO2.
    vacuum  : vacuum thickness (A) above the slab. Keep >= 15 A after adsorption.
    nfix    : number of bottom layers fixed at bulk positions (default nlayers//2).
    """
    seq = layer_sequence(nlayers, top)
    slab = stack_layers(seq, a, size)
    thickness = (nlayers - 1) * a / 2.0
    cell = slab.cell.array.copy()
    cell[2] = [0.0, 0.0, thickness + vacuum]
    slab.set_cell(cell)
    # slab at the bottom of the cell, all vacuum above: the dipole-correction
    # sawtooth then sits in one contiguous vacuum region without wrapping
    slab.positions[:, 2] += Z_BOTTOM
    slab.pbc = True
    nfix = nlayers // 2 if nfix is None else nfix
    fix_bottom_layers(slab, nfix)
    slab.info.update(
        kind="slab",
        a=a,
        nlayers=nlayers,
        termination_top=top,
        termination_bottom=seq[0],
        z_top=float(slab.positions[:, 2].max()),
        z_bottom=float(slab.positions[:, 2].min()),
        vacuum_axis=2,
    )
    return slab


def fix_bottom_layers(atoms: Atoms, nfix: int) -> None:
    """Fix the ``nfix`` lowest layers (largest tags)."""
    tags = atoms.get_tags()
    nl = tags.max()
    mask = tags > nl - nfix
    _merge_fix(atoms, mask)


def _merge_fix(atoms: Atoms, mask) -> None:
    """Add ``mask`` to any existing FixAtoms constraint."""
    mask = np.asarray(mask, bool)
    old = fixed_mask(atoms)
    atoms.set_constraint(FixAtoms(mask=old | mask) if (old | mask).any() else None)


def fixed_mask(atoms: Atoms) -> np.ndarray:
    mask = np.zeros(len(atoms), bool)
    for c in atoms.constraints:
        if isinstance(c, FixAtoms):
            mask[c.index] = True
    return mask


def composition(atoms: Atoms, exclude_adsorbate: bool = True) -> dict:
    syms = atoms.get_chemical_symbols()
    if exclude_adsorbate and "adsorbate" in atoms.arrays:
        syms = [s for s, ad in zip(syms, atoms.arrays["adsorbate"]) if not ad]
    out: dict[str, int] = {}
    for s in syms:
        out[s] = out.get(s, 0) + 1
    return out


def layer_indices(atoms: Atoms, tag: int) -> np.ndarray:
    return np.where(atoms.get_tags() == tag)[0]


def _xy_mic(d, cell2):
    """Minimum-image in-plane vectors for an (N, 2) array of displacements."""
    inv = np.linalg.inv(cell2)
    f = d @ inv
    f -= np.round(f)
    return f @ cell2


def surface_sites(atoms: Atoms, face: str = "top", indices=None, center=None) -> dict:
    """High-symmetry adsorption sites of a (001) perovskite face.

    Returns {name: (x, y, z_surface)} with one representative per site type,
    chosen closest to ``center`` (default: cell centre) so that the adsorbate
    is far from the periodic boundary.

    Site types: ``top_<X>`` on each outer-layer species, ``bridge_<X>-<Y>``
    between in-plane nearest neighbours (d < 0.75 a) and ``hollow_<X>`` above a
    second-layer atom that is not covered by an outer-layer atom.

    ``face='bottom'`` uses the lowest layer (e.g. the pore-facing side of an
    upper wall). ``indices`` = (outer_layer_indices, second_layer_indices)
    overrides the tag-based layer selection (used by the pore builders).
    """
    a = atoms.info.get("a", A_DEFAULT)
    cell2 = atoms.cell.array[:2, :2]
    if center is None:
        center = 0.5 * (cell2[0] + cell2[1])
    center = np.asarray(center, float)[:2]

    if indices is None:
        tags = atoms.get_tags()
        if face == "top":
            outer, second = layer_indices(atoms, 1), layer_indices(atoms, 2)
        else:
            nl = tags.max()
            outer, second = layer_indices(atoms, nl), layer_indices(atoms, nl - 1)
    else:
        outer, second = map(np.asarray, indices)

    pos = atoms.positions
    syms = np.array(atoms.get_chemical_symbols())
    z_surf = float(pos[outer, 2].mean())
    xy = pos[outer, :2]

    candidates: dict[str, list] = {}

    def add(name, p):
        candidates.setdefault(name, []).append(np.asarray(p, float))

    for i in outer:
        add(f"top_{syms[i]}", pos[i, :2])

    for ii, i in enumerate(outer):
        d = _xy_mic(xy - pos[i, :2], cell2)
        dist = np.linalg.norm(d, axis=1)
        for jj in np.where((dist > 0.1) & (dist < 0.75 * a))[0]:
            j = outer[jj]
            pair = "-".join(sorted([syms[i], syms[j]]))
            add(f"bridge_{pair}", pos[i, :2] + 0.5 * d[jj])

    for k in second:
        d = np.linalg.norm(_xy_mic(xy - pos[k, :2], cell2), axis=1)
        if d.min() > 0.3:
            add(f"hollow_{syms[k]}", pos[k, :2])

    sites = {}
    for name, pts in candidates.items():
        pts = np.array(pts)
        dist = np.linalg.norm(_xy_mic(pts - center, cell2), axis=1)
        best = pts[np.argmin(dist)]
        # wrap into the cell
        f = np.linalg.solve(cell2.T, best) % 1.0
        best = f @ cell2
        sites[name] = (float(best[0]), float(best[1]), z_surf)
    return dict(sorted(sites.items()))
