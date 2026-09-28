"""Pore models built from BaTiO3 (001) walls.

Four models, from simplest to most elaborate:

1. ``slit_pore_periodic``   : one slab + gap, no vacuum. The pore is the gap
   between the top of the slab and the bottom of its periodic image
   (an infinite stack of slit pores). A lateral ``offset`` shears the cell to
   change the registry of the two walls.
2. ``slit_pore_sandwich``   : two independent slabs with the gap between them
   and vacuum outside (walls can differ in termination / registry).
3. ``tilted_plate_wedge``   : a flat slab plus a finite plate (ribbon,
   periodic only along the hinge axis y) tilted by ``angle`` -> wedge-shaped
   gap whose width grows linearly away from the apex.
4. ``stepped_wedge``        : fully periodic crystal containing a wedge /
   tent-shaped cavity cut from whole BaTiO3 unit blocks (always
   stoichiometric, no edges, no vacuum). The roof is a staircase with slope
   1/terrace, i.e. angle = atan(1/terrace).

Tag convention for all pore models: bottom wall layers are tagged 1, 2, 3 ...
from the pore-facing layer outwards; top wall layers 101, 102, ... likewise.
(For ``slit_pore_periodic`` the top wall is the periodic image of the slab
bottom, so the slab keeps the plain slab tags: 1 = pore-facing top layer.)
"""

from __future__ import annotations

import math

import numpy as np
from ase import Atoms
from ase.constraints import FixAtoms

from .co2 import ORIENTATIONS, co2_molecule, place_molecule, _min_dist_to_host
from .slab import A_DEFAULT, Z_BOTTOM, _merge_fix, layer_sequence, stack_layers, surface_sites

TOP_WALL = 100


# ---------------------------------------------------------------------------
# 1. periodic slit pore
# ---------------------------------------------------------------------------
def slit_pore_periodic(
    a: float = A_DEFAULT,
    nlayers: int = 7,
    top: str = "TiO2",
    size=(2, 2),
    gap: float = 8.0,
    offset=(0.0, 0.0),
    nfix_center: int | None = None,
) -> Atoms:
    """Infinite stack of slit pores (no vacuum, no dipole correction needed).

    gap     : distance between the two facing atomic planes (A). The free
              width available to CO2 is roughly gap - 2*1.5 A (O vdW radius).
    nlayers : odd -> both walls have termination ``top``;
              even -> TiO2 wall faces a BaO wall (a crack opened in the crystal;
              gap = a/2 with offset (0, 0) gives back bulk BaTiO3).
    offset  : lateral shift (in units of a) of the upper wall relative to the
              lower one, implemented by shearing the c vector.
    """
    seq = layer_sequence(nlayers, top)
    atoms = stack_layers(seq, a, size)
    t = (nlayers - 1) * a / 2.0
    cell = atoms.cell.array.copy()
    cell[2] = [offset[0] * a, offset[1] * a, t + gap]
    atoms.set_cell(cell)
    atoms.pbc = True

    nfix_center = (1 if nlayers < 9 else 3) if nfix_center is None else nfix_center
    tags = atoms.get_tags()
    mid = (nlayers + 1) / 2.0
    order = np.argsort(np.abs(np.arange(1, nlayers + 1) - mid), kind="stable")
    fixed_tags = np.arange(1, nlayers + 1)[order[:nfix_center]]
    _merge_fix(atoms, np.isin(tags, fixed_tags))

    atoms.info.update(
        kind="slit_periodic",
        a=a,
        gap=gap,
        offset=tuple(offset),
        wall_bottom=seq[-1],
        wall_top=seq[0],
        z_wall_bottom=t,
        z_wall_top=t + gap,
    )
    return atoms


# ---------------------------------------------------------------------------
# 2. sandwich slit pore
# ---------------------------------------------------------------------------
def slit_pore_sandwich(
    a: float = A_DEFAULT,
    nlayers_bottom: int = 5,
    face_bottom: str = "TiO2",
    nlayers_top: int = 5,
    face_top: str = "TiO2",
    size=(2, 2),
    gap: float = 8.0,
    offset=(0.0, 0.0),
    vacuum: float = 15.0,
    nfix: int = 2,
) -> Atoms:
    """Two slabs facing each other across ``gap``; vacuum outside (use dipole correction)."""
    seq_b = layer_sequence(nlayers_bottom, face_bottom)
    seq_t = layer_sequence(nlayers_top, face_top)[::-1]  # bottom layer = face_top
    bot = stack_layers(seq_b, a, size)
    tb = (nlayers_bottom - 1) * a / 2.0
    top = stack_layers(seq_t, a, size, z0=tb + gap)
    top.positions[:, 0] += offset[0] * a
    top.positions[:, 1] += offset[1] * a
    # stack_layers tags from the uppermost layer; convert so 101 = pore-facing layer
    top.set_tags(TOP_WALL + nlayers_top - top.get_tags() + 1)
    atoms = bot + top
    tt = (nlayers_top - 1) * a / 2.0
    cell = atoms.cell.array.copy()
    cell[2] = [0.0, 0.0, tb + gap + tt + vacuum]
    atoms.set_cell(cell)
    atoms.positions[:, 2] += Z_BOTTOM
    atoms.pbc = True
    tags = atoms.get_tags()
    fix = ((tags < TOP_WALL) & (tags > nlayers_bottom - nfix)) | (tags > TOP_WALL + nlayers_top - nfix)
    _merge_fix(atoms, fix)
    atoms.info.update(
        kind="slit_sandwich",
        a=a,
        gap=gap,
        offset=tuple(offset),
        wall_bottom=face_bottom,
        wall_top=face_top,
        z_wall_bottom=Z_BOTTOM + tb,
        z_wall_top=Z_BOTTOM + tb + gap,
        vacuum_axis=2,
    )
    return atoms


# ---------------------------------------------------------------------------
# 3. tilted plate over a flat slab
# ---------------------------------------------------------------------------
def tilted_plate_wedge(
    a: float = A_DEFAULT,
    angle: float = 20.0,
    apex_gap: float = 4.0,
    plate_width: int = 4,
    plate_layers: int = 4,
    plate_face: str = "BaO",
    plate_free_layers: int = 2,
    bottom_layers: int = 5,
    bottom_face: str = "TiO2",
    bottom_nfix: int = 2,
    ny: int = 2,
    xgap: float = 8.0,
    vacuum: float = 15.0,
) -> Atoms:
    """Wedge pore: a plate tilted by ``angle`` (deg) about the y axis above a slab.

    Under 3D periodic boundary conditions two infinite, non-parallel planes
    must intersect, so the tilted wall is a finite plate (ribbon): periodic
    along y (the hinge axis), ``plate_width`` unit cells wide along its own
    x', separated from its periodic image by >= ``xgap`` A of vacuum.

    Geometry (x-z plane)::

                                    ____/  plate (tilted by angle)
                               ____/    /
                          ____/     ___/
                         /     ___/   gap(x) = apex_gap + (x - x_apex) tan(angle)
                        /_____/  <- apex
          ====================================  bottom slab (periodic in x, y)

    The plate is mostly frozen (only ``plate_free_layers`` pore-facing layers
    relax): the wedge angle is an imposed boundary condition, a free plate
    would simply fall onto the slab.
    """
    th = math.radians(angle)
    seq_p = layer_sequence(plate_layers, plate_face)[::-1]  # bottom (pore-facing) = plate_face
    plate = stack_layers(seq_p, a, (plate_width, ny))
    plate.set_tags(TOP_WALL + plate_layers - plate.get_tags() + 1)
    xp, zp = plate.positions[:, 0].copy(), plate.positions[:, 2].copy()
    plate.positions[:, 0] = xp * math.cos(th) - zp * math.sin(th)
    plate.positions[:, 2] = xp * math.sin(th) + zp * math.cos(th)

    t_p = (plate_layers - 1) * a / 2.0
    xmin, xmax = plate.positions[:, 0].min(), plate.positions[:, 0].max()
    nx = int(math.ceil((xmax - xmin + xgap) / a))
    lx = nx * a

    seq_b = layer_sequence(bottom_layers, bottom_face)
    bot = stack_layers(seq_b, a, (nx, ny))
    t_b = (bottom_layers - 1) * a / 2.0
    z_surf = Z_BOTTOM + t_b
    bot.positions[:, 2] += Z_BOTTOM

    # centre the plate in x; apex = lower edge of the pore-facing layer
    x_shift = 0.5 * (lx - (xmax - xmin)) - xmin
    plate.positions[:, 0] += x_shift
    plate.positions[:, 2] += z_surf + apex_gap
    x_apex = x_shift  # face atom at x' = 0 maps to x = x_shift

    atoms = bot + plate
    ztop = atoms.positions[:, 2].max()
    atoms.set_cell([lx, ny * a, ztop + vacuum])
    atoms.pbc = True

    tags = atoms.get_tags()
    fix = ((tags < TOP_WALL) & (tags > bottom_layers - bottom_nfix)) | (tags > TOP_WALL + plate_free_layers)
    _merge_fix(atoms, fix)

    face_len = (plate_width - 0.5) * a  # extent of the pore-facing layer along x'
    atoms.info.update(
        kind="wedge_plate",
        a=a,
        angle=angle,
        apex_gap=apex_gap,
        wall_bottom=bottom_face,
        wall_top=plate_face,
        z_wall_bottom=z_surf,
        x_apex=x_apex,
        z_apex=z_surf + apex_gap,
        x_open=x_apex + face_len * math.cos(th),
        gap_open=apex_gap + face_len * math.sin(th),
        plate_thickness=t_p,
        vacuum_axis=2,
    )
    return atoms


def wedge_gap_at(atoms: Atoms, x: float) -> float:
    """Vertical plane-to-plane gap of a ``tilted_plate_wedge`` at position x."""
    i = atoms.info
    return i["apex_gap"] + (x - i["x_apex"]) * math.tan(math.radians(i["angle"]))


# ---------------------------------------------------------------------------
# 4. stepped wedge cavity in a periodic crystal
# ---------------------------------------------------------------------------
_BLOCKS = {
    # unit block = 5 atoms, (symbol, fx, fy, fz); defines which layer is at the block bottom
    "TiO2": [("Ba", 0.0, 0.0, 0.0), ("O", 0.5, 0.5, 0.0),
             ("Ti", 0.5, 0.5, 0.5), ("O", 0.5, 0.0, 0.5), ("O", 0.0, 0.5, 0.5)],
    "BaO": [("Ti", 0.5, 0.5, 0.0), ("O", 0.5, 0.0, 0.0), ("O", 0.0, 0.5, 0.0),
            ("Ba", 0.0, 0.0, 0.5), ("O", 0.5, 0.5, 0.5)],
}


def stepped_wedge(
    a: float = A_DEFAULT,
    nx: int = 8,
    ny: int = 2,
    terrace: int = 1,
    h0: int = 0,
    profile: str = "V",
    floor_face: str = "TiO2",
    wall: int = 3,
    nfix_layers: int = 2,
) -> Atoms:
    """Periodic crystal with a wedge-shaped cavity made of removed BaTiO3 blocks.

    The cavity has a flat floor and a staircase roof: its height (in blocks)
    in column ix is ``h(ix) = h0 + d(ix) // terrace`` with
    d = min(ix, nx-1-ix) for profile='V' (tent-shaped pore with two wedge tips)
    or d = ix for profile='sawtooth' (one wedge tip, closed by a cliff).
    Plane-to-plane gap in a column: h*a + a/2 (h=1: ~6 A, h=2: ~10 A ...).
    With h0 = 0 the tips are closed (true apex); h0 >= 1 leaves a narrow neck.
    Mean roof angle = atan(1/terrace): terrace=1 -> 45 deg, 2 -> 26.6 deg, 3 -> 18.4 deg.

    Every removed unit is a complete, neutral BaTiO3 block, so the model is
    exactly stoichiometric and non-polar. floor_face selects the floor
    termination (the roof then has the complementary one).
    ``wall`` = crystal thickness (blocks) above the highest roof point; the
    region farthest from the cavity (``nfix_layers`` atomic layers) is fixed.
    """
    if profile == "V":
        d = [min(ix, nx - 1 - ix) for ix in range(nx)]
    elif profile == "sawtooth":
        d = list(range(nx))
    else:
        raise ValueError("profile must be 'V' or 'sawtooth'")
    h = [h0 + di // terrace for di in d]
    z0 = 1  # first removed block row (row 0 = floor block)
    nz = z0 + max(h) + wall

    block = _BLOCKS[floor_face]
    symbols, pos, col = [], [], []
    for ix in range(nx):
        for iy in range(ny):
            for iz in range(nz):
                if z0 <= iz < z0 + h[ix]:
                    continue
                for s, fx, fy, fz in block:
                    symbols.append(s)
                    pos.append(((ix + fx) * a, (iy + fy) * a, (iz + fz) * a))
                    col.append(ix)
    atoms = Atoms(symbols, positions=pos, cell=[nx * a, ny * a, nz * a], pbc=True)

    # tags: floor layers 1, 2, ... going down; roof layers 101, 102 ... going up (per column)
    z = atoms.positions[:, 2]
    col = np.array(col)
    z_floor = z0 * a - a / 2.0
    tags = np.zeros(len(atoms), int)
    below = z <= z_floor + 1e-6
    tags[below] = 1 + np.round((z_floor - z[below]) / (a / 2)).astype(int)
    z_roof = np.array([(z0 + h[ix]) * a for ix in range(nx)])
    above = ~below
    tags[above] = TOP_WALL + 1 + np.round((z[above] - z_roof[col[above]]) / (a / 2)).astype(int)
    atoms.set_tags(tags)

    # fix the atomic layers farthest from the cavity: middle of the crystal between the
    # highest roof point and the periodic image of the floor
    lz = nz * a
    z_mid = 0.5 * (z_roof.max() + z_floor + lz)
    levels = np.unique(np.round(z / (a / 2)).astype(int))
    dist = np.abs(((levels * a / 2 - z_mid + lz / 2) % lz) - lz / 2)
    fix_levels = levels[np.argsort(dist)[:nfix_layers]]
    _merge_fix(atoms, np.isin(np.round(z / (a / 2)).astype(int), fix_levels))

    atoms.info.update(
        kind="wedge_stepped",
        a=a,
        profile=profile,
        terrace=terrace,
        angle=math.degrees(math.atan(1.0 / terrace)),
        h_blocks=h,
        wall_bottom=floor_face,
        wall_top="BaO" if floor_face == "TiO2" else "TiO2",
        z_wall_bottom=z_floor,
        z_roof=z_roof.tolist(),
        gaps=[hh * a + a / 2.0 if hh > 0 else 0.0 for hh in h],
    )
    return atoms


# ---------------------------------------------------------------------------
# CO2 placement inside pores
# ---------------------------------------------------------------------------
def wall_sites(pore: Atoms, wall: str = "bottom", center=None) -> dict:
    """High-symmetry sites of the pore-facing layer of one wall."""
    tags = pore.get_tags()
    if pore.info["kind"] == "slit_periodic":
        nl = tags.max()
        idx = (np.where(tags == 1)[0], np.where(tags == 2)[0]) if wall == "bottom" else (
            np.where(tags == nl)[0], np.where(tags == nl - 1)[0])
    else:
        t0 = 1 if wall == "bottom" else TOP_WALL + 1
        idx = (np.where(tags == t0)[0], np.where(tags == t0 + 1)[0])
    sites = surface_sites(pore, indices=idx, center=center)
    if pore.info["kind"] == "slit_periodic" and wall == "top":
        # the top wall is the periodic image of the slab bottom: shift by the c vector
        c = pore.cell.array[2]
        zt = pore.info["z_wall_top"]
        sites = {k: (x + c[0], y + c[1], zt) for k, (x, y, _) in sites.items()}
    return sites


def midgap_configurations(pore: Atoms, orientations=None, min_dist: float = 2.4, center=None) -> dict:
    """CO2 (C atom) at mid-gap above each bottom-wall site, for every orientation that fits."""
    orientations = ORIENTATIONS if orientations is None else orientations
    info = pore.info
    if info["kind"] in ("slit_periodic", "slit_sandwich"):
        z_mid = 0.5 * (info["z_wall_bottom"] + info["z_wall_top"])
    else:
        raise ValueError("use wedge_configurations() for wedge models")
    out = {}
    for sname, (x, y, _) in wall_sites(pore, "bottom", center).items():
        for oname, o in orientations.items():
            if o.bend != 180.0:
                continue  # chemisorbed guesses belong on a wall, not mid-gap
            atoms = _put_center(pore, co2_molecule(o), (x, y, z_mid), min_dist)
            if atoms is not None:
                atoms.info.update(site=sname, orientation=oname, position="midgap")
                out[f"mid_{sname}__{oname}"] = atoms
    return out


def wall_configurations(pore: Atoms, wall: str = "bottom", orientations=None, center=None) -> dict:
    """CO2 adsorbed on one pore wall (same site x orientation logic as open surfaces)."""
    from .co2 import surface_configurations

    if wall == "top" and pore.info["kind"].startswith("wedge"):
        raise ValueError("the upper wall of a wedge is not flat; use wedge_configurations()")
    sites = wall_sites(pore, wall, center)
    direction = +1 if wall == "bottom" else -1
    out = surface_configurations(pore, sites, orientations, direction=direction)
    return {f"{wall}_{k}": v for k, v in out.items()}


def wedge_configurations(pore: Atoms, n: int = 5, min_gap: float = 5.5, orientations=("flat_y", "flat_x"),
                         min_dist: float = 2.4) -> dict:
    """CO2 at mid-gap along the wedge, from the narrow end (where it still fits) to the open end."""
    info = pore.info
    a = info["a"]
    y = pore.cell[1, 1] / 2.0 + a / 4.0
    cands = []  # (x, z_bottom, z_top)
    if info["kind"] == "wedge_plate":
        th = math.radians(info["angle"])
        x0 = info["x_apex"] + max(0.0, (min_gap - info["apex_gap"]) / math.tan(th))
        x1 = info["x_open"] - 1.0
        for x in np.linspace(x0, x1, n):
            zb = info["z_wall_bottom"]
            cands.append((x, zb, zb + wedge_gap_at(pore, x)))
    elif info["kind"] == "wedge_stepped":
        for ix, g in enumerate(info["gaps"]):
            if g >= min_gap:
                cands.append(((ix + 0.5) * a, info["z_wall_bottom"], info["z_roof"][ix]))
        if info["profile"] == "V":
            cands = cands[: (len(cands) + 1) // 2]  # the two halves are mirror images
    else:
        raise ValueError("not a wedge model")
    out = {}
    for x, zb, zt in cands:
        for oname in orientations:
            o = ORIENTATIONS[oname]
            atoms = _put_center(pore, co2_molecule(o), (x, y, 0.5 * (zb + zt)), min_dist)
            if atoms is not None:
                g = zt - zb
                atoms.info.update(orientation=oname, gap_local=g, x=x)
                out[f"x{x:05.1f}_gap{g:04.1f}__{oname}"] = atoms
    return out


def _put_center(host: Atoms, mol: Atoms, center, min_dist: float) -> Atoms | None:
    m = mol.copy()
    m.positions += np.asarray(center) - m.positions[0]
    combo = host + m
    if _min_dist_to_host(combo, len(host)) < min_dist:
        return None
    flags = np.zeros(len(combo), bool)
    flags[len(host):] = True
    combo.set_array("adsorbate", flags)
    combo.set_constraint(host.constraints)
    combo.info.update(host.info)
    return combo


def empty_reference(atoms: Atoms) -> Atoms:
    """The same cell without adsorbates (reference for E_ads in the same pore)."""
    if "adsorbate" not in atoms.arrays:
        return atoms.copy()
    keep = ~atoms.arrays["adsorbate"].astype(bool)
    fixed = np.zeros(len(atoms), bool)
    for c in atoms.constraints:
        if isinstance(c, FixAtoms):
            fixed[c.index] = True
    ref = atoms[keep]
    ref.set_constraint(FixAtoms(mask=fixed[keep]) if fixed[keep].any() else None)
    del ref.arrays["adsorbate"]
    return ref
