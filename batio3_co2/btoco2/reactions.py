"""Initial/final-state builders for CI-NEB of CO2 dissociation on BaTiO3(001).

A single NEB can only connect two local minima with the same atoms, so
"CO2 -> CO + 1/2 O2" and "CO2 -> C + O2" are split into elementary steps:

    (R1)  CO2* + V_O  -> CO* + O_lattice   (vacancy healing; most relevant)
    (R2)  CO2*        -> CO* + O*          (stoichiometric surface)
    (R3)  CO*         -> C*  + O*
    (R4)  O*  + O*    -> O2*               (then O2* -> O2(g))
    (R5)  CO2*        -> C*  + O2*         (concerted; for comparison only,
                                            expect a very high barrier)

The ``1/2 O2`` in CO2 -> CO + 1/2 O2 is not a single-image object: its
energetics come from (R2) + (R4) + desorption, or thermodynamically from
E(CO) + 1/2 E(O2) - E(CO2) with gas-phase reference calculations.

All builders keep the atom order  [substrate..., C, O_a, O_b(, ...)]  so IS
and FS are directly usable as NEB end points. The geometries are *starting
guesses*: relax IS and FS with pw.x first, then build the NEB from the
relaxed structures (see ``load_relaxed``).
"""

from __future__ import annotations

import numpy as np
from ase import Atoms
from ase.constraints import FixAtoms
from ase.io import read

from .co2 import R_CO, R_CO2, R_OO
from .slab import _xy_mic, fixed_mask, surface_sites

SURF, SUB = 1, 2  # tags of outermost and second layers (slab convention)


def _layer(atoms, tag, symbol=None):
    t = atoms.get_tags()
    s = np.array(atoms.get_chemical_symbols())
    m = t == tag
    if "adsorbate" in atoms.arrays:
        m &= ~atoms.arrays["adsorbate"].astype(bool)
    if symbol:
        m &= s == symbol
    return np.where(m)[0]


def _nearest(atoms, idx, xy):
    cell2 = atoms.cell.array[:2, :2]
    d = np.linalg.norm(_xy_mic(atoms.positions[idx, :2] - np.asarray(xy)[:2], cell2), axis=1)
    return idx[np.argsort(d)]


def _append(host: Atoms, symbols: str, positions) -> Atoms:
    mol = Atoms(symbols, positions=positions)
    out = host + mol
    flags = np.zeros(len(out), bool)
    if "adsorbate" in host.arrays:
        flags[: len(host)] = host.arrays["adsorbate"]
    flags[len(host):] = True
    out.set_array("adsorbate", flags)
    out.set_constraint(host.constraints)
    out.info.update(host.info)
    return out


def _unit(v):
    v = np.asarray(v, float)
    return v / np.linalg.norm(v)


# ---------------------------------------------------------------------------
# (R1) CO2 + oxygen vacancy -> CO + healed surface
# ---------------------------------------------------------------------------
def make_o_vacancy(slab: Atoms, center=None) -> tuple[Atoms, np.ndarray, np.ndarray]:
    """Remove the outermost-layer O closest to ``center``.

    Returns (defective slab, vacancy position, in-plane unit vector w pointing
    from the vacancy to the nearest hollow site).
    """
    cell2 = slab.cell.array[:2, :2]
    center = 0.5 * (cell2[0] + cell2[1]) if center is None else np.asarray(center)[:2]
    io = _nearest(slab, _layer(slab, SURF, "O"), center)[0]
    v = slab.positions[io].copy()
    # hollow sites = second-layer atoms not covered by an outer-layer atom
    sub, outer = _layer(slab, SUB), _layer(slab, SURF)
    cands = []
    for k in sub:
        d_outer = np.linalg.norm(_xy_mic(slab.positions[outer, :2] - slab.positions[k, :2], cell2), axis=1)
        if d_outer.min() > 0.3:
            cands.append(_xy_mic((slab.positions[k, :2] - v[:2])[None], cell2)[0])
    cands = np.array(cands)
    w2 = cands[np.argmin(np.linalg.norm(cands, axis=1))]
    w = np.array([*_unit(w2), 0.0])
    keep = np.ones(len(slab), bool)
    keep[io] = False
    fixed = fixed_mask(slab)
    out = slab[keep]
    out.set_constraint(FixAtoms(mask=fixed[keep]) if fixed[keep].any() else None)
    out.info = dict(slab.info, vacancy=v.tolist())
    return out, v, w


def vacancy_healing_endpoints(slab: Atoms, center=None) -> dict:
    """(R1) CO2* at a surface O vacancy -> CO* + O filling the vacancy.

    IS: bent CO2 (140 deg) with O_b pointing into the vacancy, C and O_a
        leaning towards the neighbouring hollow site.
    FS: O_b at the lattice position (vacancy healed), CO upright (C-down)
        over the hollow site, ~2.6 A above the surface.
    Use a spin-polarised calculation (and optionally DFT+U on Ti): the
    neutral vacancy leaves two electrons on Ti.
    """
    vs, v, w = make_o_vacancy(slab, center)
    z = np.array([0.0, 0.0, 1.0])
    zs = v[2]
    a = slab.info.get("a", 4.0)
    alpha, gamma = np.radians(35.0), np.radians(75.0)
    ob = v + 0.9 * z
    c = ob + R_CO2 * (np.sin(alpha) * w + np.cos(alpha) * z)
    oa = c + R_CO2 * (np.sin(gamma) * w + np.cos(gamma) * z)
    IS = _append(vs, "COO", [c, oa, ob])

    hollow = v + 0.5 * a * w
    c_f = np.array([hollow[0], hollow[1], zs + 2.6])
    FS = _append(vs, "COO", [c_f, c_f + R_CO * z, v])
    for im, lab in ((IS, "IS"), (FS, "FS")):
        im.info.update(reaction="CO2+V_O->CO+O_O", state=lab)
    return {"IS": IS, "FS": FS, "moving": [len(vs), len(vs) + 1, len(vs) + 2]}


# ---------------------------------------------------------------------------
# (R2)-(R5) generic final-state builders starting from an adsorbed IS
# ---------------------------------------------------------------------------
def o_anchor_site(slab: Atoms, near_xy):
    """Where a single O adatom binds: atop Ti on TiO2 termination (Ti=O, 1.7 A),
    atop lattice O on BaO termination (peroxide O-O, 1.5 A)."""
    term = slab.info.get("termination_top", "TiO2")
    sym, h = ("Ti", 1.7) if term == "TiO2" else ("O", 1.5)
    i = _nearest(slab, _layer(slab, SURF, sym), near_xy)[0]
    return slab.positions[i] + np.array([0, 0, h]), i


def co_dissociation_fs_from_is(IS: Atoms, cleave: int | None = None) -> Atoms:
    """(R2) CO2* -> CO* + O*. ``IS`` = relaxed or guessed CO2* (adsorbate = last 3 atoms C, O, O).

    ``cleave``: index (0 or 1 within the two O atoms) of the O that stays on
    the surface; default = the lower one.
    """
    n = len(IS)
    ic, io1, io2 = n - 3, n - 2, n - 1
    if cleave is None:
        cleave = 0 if IS.positions[io1, 2] <= IS.positions[io2, 2] else 1
    i_leave = (io1, io2)[cleave]
    i_co = (io1, io2)[1 - cleave]
    FS = IS.copy()
    anchor, iatom = o_anchor_site(IS, IS.positions[i_leave])
    FS.positions[i_leave] = anchor
    # CO upright over the hollow site next to the anchor, away from the O adatom
    sites = surface_sites(IS, center=IS.positions[ic, :2])
    hol = [np.array(p) for k, p in sites.items() if k.startswith("hollow")]
    zs = IS.positions[_layer(IS, SURF), 2].mean()
    target = hol[0] if hol else IS.positions[ic] + np.array([IS.info.get("a", 4.0) / 2, 0, 0])
    FS.positions[ic] = [target[0], target[1], zs + 2.6]
    FS.positions[i_co] = FS.positions[ic] + [0, 0, R_CO]
    if np.linalg.norm(FS.positions[ic, :2] - anchor[:2]) < 1.5:
        FS.positions[[ic, i_co], 0] += IS.info.get("a", 4.0) / 2
    FS.info.update(reaction="CO2*->CO*+O*", state="FS")
    return FS


def c_o_fs_from_co(IS: Atoms) -> Atoms:
    """(R3) CO* -> C* + O*. IS adsorbate = [..., C, O, (other atoms kept fixed in place)].

    Expects C and its O as the first two adsorbate atoms (as produced by the
    builders above). C goes to the nearest hollow at 1.3 A, O to its anchor.
    """
    ads = np.where(IS.arrays["adsorbate"])[0]
    ic = ads[0]
    io = ads[np.argmin([np.linalg.norm(IS.positions[j] - IS.positions[ic]) if j != ic else 99 for j in ads])]
    FS = IS.copy()
    zs = IS.positions[_layer(IS, SURF), 2].mean()
    sites = surface_sites(IS, center=IS.positions[ic, :2])
    hol = [np.array(p) for k, p in sites.items() if k.startswith("hollow")]
    FS.positions[ic] = [hol[0][0], hol[0][1], zs + 1.3]
    anchor, _ = o_anchor_site(IS, IS.positions[io] + [IS.info.get("a", 4.0) / 2, 0, 0])
    FS.positions[io] = anchor
    FS.info.update(reaction="CO*->C*+O*", state="FS")
    return FS


def o2_formation_endpoints(slab: Atoms, center=None) -> dict:
    """(R4) O* + O* -> O2*. Two O adatoms on neighbouring anchor sites -> O2 flat at 2.6 A.

    Spin: the product O2 is a triplet; run IS and FS spin-polarised with
    tot_magnetization unset (starting_magnetization on O) and compare.
    """
    a = slab.info.get("a", 4.0)
    cell2 = slab.cell.array[:2, :2]
    center = 0.5 * (cell2[0] + cell2[1]) if center is None else np.asarray(center)[:2]
    p1, i1 = o_anchor_site(slab, center)
    p2, i2 = o_anchor_site(slab, p1[:2] + [a, 0])
    if i1 == i2:
        p2, _ = o_anchor_site(slab, p1[:2] + [0, a])
    IS = _append(slab, "OO", [p1, p2])
    mid = 0.5 * (p1 + p2)
    zs = slab.positions[_layer(slab, SURF), 2].mean()
    u = _unit(np.r_[(p2 - p1)[:2], 0.0])
    FS = _append(slab, "OO", [[*(mid[:2] - 0.5 * R_OO * u[:2]), zs + 2.6],
                              [*(mid[:2] + 0.5 * R_OO * u[:2]), zs + 2.6]])
    IS.info.update(reaction="2O*->O2*", state="IS")
    FS.info.update(reaction="2O*->O2*", state="FS")
    return {"IS": IS, "FS": FS}


def c_o2_fs_from_is(IS: Atoms) -> Atoms:
    """(R5) concerted CO2* -> C* + O2*: C to a hollow (1.3 A), O2 flat 3.0 A above it."""
    n = len(IS)
    ic, io1, io2 = n - 3, n - 2, n - 1
    FS = IS.copy()
    zs = IS.positions[_layer(IS, SURF), 2].mean()
    sites = surface_sites(IS, center=IS.positions[ic, :2])
    hol = [np.array(p) for k, p in sites.items() if k.startswith("hollow")]
    h = hol[0]
    FS.positions[ic] = [h[0], h[1], zs + 1.3]
    FS.positions[io1] = [h[0] - R_OO / 2, h[1], zs + 4.3]
    FS.positions[io2] = [h[0] + R_OO / 2, h[1], zs + 4.3]
    FS.info.update(reaction="CO2*->C*+O2*", state="FS")
    return FS


# ---------------------------------------------------------------------------
# path helpers
# ---------------------------------------------------------------------------
def load_relaxed(pw_out: str, template: Atoms) -> Atoms:
    """Final geometry of a pw.x relax, with tags / constraints / arrays from ``template``."""
    last = read(pw_out, format="espresso-out", index=-1)
    if last.get_chemical_symbols() != template.get_chemical_symbols():
        raise ValueError("atom order in output differs from the template")
    out = template.copy()
    out.positions = last.positions
    out.set_cell(last.cell)
    return out


def interpolate_path(IS: Atoms, FS: Atoms, n_images: int = 7, method: str = "idpp") -> list[Atoms]:
    """Total ``n_images`` images including end points (IDPP by default, minimum image convention)."""
    from ase.mep import NEB

    if IS.get_chemical_symbols() != FS.get_chemical_symbols():
        raise ValueError("IS and FS must have identical atom order")
    images = [IS.copy()] + [IS.copy() for _ in range(n_images - 2)] + [FS.copy()]
    for im in images:
        im.set_constraint(IS.constraints)
    neb = NEB(images)
    neb.interpolate(method=method, mic=True)
    return images
