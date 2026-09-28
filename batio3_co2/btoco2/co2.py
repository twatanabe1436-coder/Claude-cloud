"""CO2 geometries and placement on surfaces / in pores.

Orientation convention: the molecule is first built in the x-z plane, then
tilted (linear molecule only) about y and finally rotated about z by ``phi``
(the in-plane direction of the O...O axis, 0 = along x, 90 = along y).

* ``bend`` = 180      : linear CO2.
* ``bend`` < 180 and ``cdown=True``  : bent CO2 with C pointing to the surface
  (carbonate-like starting guess on a lattice O, e.g. BaO termination).
* ``bend`` < 180 and ``cdown=False`` : bent CO2 with both O pointing to the
  surface (bidentate on cations, e.g. Ti on the TiO2 termination).
"""

from __future__ import annotations

from dataclasses import dataclass

import numpy as np
from ase import Atoms

R_CO2 = 1.17  # C=O in CO2 (PBE ~1.176 A)
R_CO = 1.14  # C-O in CO
R_OO = 1.23  # O=O in O2


@dataclass(frozen=True)
class Orientation:
    bend: float = 180.0  # O-C-O angle (deg)
    tilt: float = 0.0  # angle of the O...O axis out of the surface plane (deg, linear only)
    phi: float = 0.0  # in-plane azimuth of the O...O axis (deg)
    cdown: bool = False  # bent: C towards the surface
    height: float = 2.9  # height of the lowest CO2 atom above the surface plane (A)
    min_dist: float = 2.3  # minimum allowed distance to substrate atoms (A), None = no check


# A compact, physically motivated set of starting orientations.
ORIENTATIONS = {
    "flat_x": Orientation(phi=0.0),
    "flat_y": Orientation(phi=90.0),
    "flat_diag": Orientation(phi=45.0),
    "tilt45_x": Orientation(tilt=45.0, phi=0.0, height=2.6),
    "upright": Orientation(tilt=90.0, height=2.4),
    # chemisorption guesses (min_dist relaxed on purpose)
    "carbonate_x": Orientation(bend=130.0, phi=0.0, cdown=True, height=1.45, min_dist=1.3),
    "carbonate_diag": Orientation(bend=130.0, phi=45.0, cdown=True, height=1.45, min_dist=1.3),
    "bidentate_x": Orientation(bend=130.0, phi=0.0, cdown=False, height=2.0, min_dist=1.8),
    "bidentate_y": Orientation(bend=130.0, phi=90.0, cdown=False, height=2.0, min_dist=1.8),
}

# Which orientations make sense on which kind of site. Chemisorbed (carbonate)
# guesses only on lattice O, bidentate guesses on/between cations.
SITE_RULES = {
    "carbonate": ("top_O",),
    "bidentate": ("top_Ti", "top_Ba", "bridge_O-Ti", "bridge_Ba-O", "hollow"),
}


def co2_molecule(o: Orientation = Orientation()) -> Atoms:
    """CO2 (order C, O, O) centred at the C atom, oriented according to ``o``."""
    half = np.radians(o.bend) / 2.0
    # bent molecule in the x-z plane with the two O below C (O-down)
    p = np.array(
        [
            [0.0, 0.0, 0.0],
            [R_CO2 * np.sin(half), 0.0, -R_CO2 * np.cos(half)],
            [-R_CO2 * np.sin(half), 0.0, -R_CO2 * np.cos(half)],
        ]
    )
    if o.cdown:
        p[:, 2] *= -1.0
    mol = Atoms("CO2", positions=p)
    if o.tilt:
        mol.rotate(-o.tilt, "y", center=(0, 0, 0))
    if o.phi:
        mol.rotate(o.phi, "z", center=(0, 0, 0))
    return mol


def place_molecule(
    host: Atoms,
    mol: Atoms,
    xy,
    z_surface: float,
    height: float,
    direction: int = +1,
    min_dist: float | None = None,
    max_shift: float = 2.0,
) -> Atoms | None:
    """Put ``mol`` above (direction=+1) or below (-1) a surface plane.

    The molecule is translated so that its atom closest to the surface sits
    ``height`` A from the plane, with its centre (first atom) at ``xy``. If
    ``min_dist`` is set, the molecule is pushed away from the surface in 0.1 A
    steps (up to ``max_shift``) until all molecule-substrate distances are
    >= min_dist; returns None if that fails.
    """
    m = mol.copy()
    if direction < 0:
        m.positions[:, 2] *= -1.0
    m.positions[:, :2] += np.asarray(xy[:2]) - m.positions[0, :2]
    if direction > 0:
        m.positions[:, 2] += z_surface + height - m.positions[:, 2].min()
    else:
        m.positions[:, 2] += z_surface - height - m.positions[:, 2].max()

    for _ in range(int(round(max_shift / 0.1)) + 1):
        combo = host + m
        if min_dist is None or _min_dist_to_host(combo, len(host)) >= min_dist:
            flags = np.zeros(len(combo), bool)
            flags[len(host):] = True
            if "adsorbate" in host.arrays:
                flags[: len(host)] = host.arrays["adsorbate"]
            combo.set_array("adsorbate", flags)
            combo.set_constraint(host.constraints)
            combo.info.update(host.info)
            return combo
        m.positions[:, 2] += 0.1 * direction
    return None


def _min_dist_to_host(atoms: Atoms, nhost: int) -> float:
    idx_host = np.arange(nhost)
    dmin = np.inf
    for i in range(nhost, len(atoms)):
        d = atoms.get_distances(i, idx_host, mic=True)
        dmin = min(dmin, d.min())
    return float(dmin)


def min_dist_adsorbate_substrate(atoms: Atoms) -> float:
    ads = np.where(atoms.arrays["adsorbate"])[0]
    sub = np.where(~atoms.arrays["adsorbate"])[0]
    return float(min(atoms.get_distances(i, sub, mic=True).min() for i in ads))


def _allowed(orient_name: str, site_name: str) -> bool:
    for key, sites in SITE_RULES.items():
        if orient_name.startswith(key):
            return any(site_name.startswith(s) for s in sites)
    return True


def surface_configurations(slab: Atoms, sites: dict, orientations=None, direction=+1) -> dict:
    """All (site x orientation) CO2 starting structures on one surface.

    Returns {'<site>__<orientation>': Atoms}. Combinations that make no
    chemical sense (see SITE_RULES) or that cannot be placed without clashes
    are skipped.
    """
    orientations = ORIENTATIONS if orientations is None else orientations
    out = {}
    for sname, (x, y, zs) in sites.items():
        for oname, o in orientations.items():
            if not _allowed(oname, sname):
                continue
            atoms = place_molecule(slab, co2_molecule(o), (x, y), zs, o.height, direction, o.min_dist)
            if atoms is not None:
                atoms.info.update(site=sname, orientation=oname)
                out[f"{sname}__{oname}"] = atoms
    return out


def random_configurations(
    host: Atoms,
    n: int,
    z_range,
    seed: int = 0,
    min_dist: float = 2.4,
    xy_region=None,
    max_trials: int = 2000,
) -> list[Atoms]:
    """Random CO2 positions/orientations inside a z window (e.g. inside a pore).

    Useful as a cheap global search: generate many, pre-relax them (e.g. with a
    machine-learned potential or a low-cutoff DFT), then refine the lowest ones.
    ``xy_region`` = ((xmin, xmax), (ymin, ymax)) in Cartesian A (default: whole cell).
    """
    rng = np.random.default_rng(seed)
    cell2 = host.cell.array[:2, :2]
    out = []
    for _ in range(max_trials):
        if len(out) >= n:
            break
        v = rng.normal(size=3)
        v /= np.linalg.norm(v)
        mol = co2_molecule()
        # align molecular axis (x) with v
        mol.rotate((1, 0, 0), v, center=(0, 0, 0))
        if xy_region is None:
            f = rng.random(2)
            xy = f @ cell2
        else:
            xy = np.array([rng.uniform(*xy_region[0]), rng.uniform(*xy_region[1])])
        z = rng.uniform(*z_range)
        mol.positions += np.array([xy[0], xy[1], z]) - mol.positions[0]
        combo = host + mol
        if _min_dist_to_host(combo, len(host)) < min_dist:
            continue
        flags = np.zeros(len(combo), bool)
        flags[len(host):] = True
        combo.set_array("adsorbate", flags)
        combo.set_constraint(host.constraints)
        combo.info.update(host.info)
        out.append(combo)
    return out


def gas_molecule(name: str, box=(15.0, 15.5, 16.0)) -> Atoms:
    """Isolated CO2, CO or O2 in a slightly anisotropic box (breaks symmetry)."""
    if name == "CO2":
        mol = Atoms("CO2", positions=[(0, 0, 0), (R_CO2, 0, 0), (-R_CO2, 0, 0)])
    elif name == "CO":
        mol = Atoms("CO", positions=[(0, 0, 0), (R_CO, 0, 0)])
    elif name == "O2":
        mol = Atoms("O2", positions=[(0, 0, 0), (R_OO, 0, 0)])
    else:
        raise ValueError(name)
    mol.set_cell(box)
    mol.center()
    mol.pbc = True
    mol.info.update(kind="molecule", name=name)
    return mol
