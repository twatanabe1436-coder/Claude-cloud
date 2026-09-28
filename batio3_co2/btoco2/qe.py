"""Minimal, transparent Quantum ESPRESSO input writers (pw.x and neb.x).

The writers produce plain-text inputs you can read and edit by hand. Settings
that matter for this system:

* vdW: CO2 physisorption and confinement are dominated by dispersion.
  Default PBE + D3(BJ) (``vdw='d3'``); alternatives ``'rvv10'``, ``'rev-vdw-df2'``.
* Dipole correction for slabs with vacuum (``tefield``/``dipfield``); the
  sawtooth is placed automatically in the middle of the largest vacuum gap.
  Fully periodic pores (no vacuum) must NOT use it.
* SCF mixing: plain Broyden, beta = 0.3. In tests on a 5-layer TiO2-terminated
  slab (QE 7.5, pslibrary PAW) plain mixing converged in 25 iterations while
  ``local-TF`` needed 65 or more (or stalled), so local-TF is not the default.
* Fixed atoms are written as ``0 0 0`` from ASE ``FixAtoms`` constraints.
"""

from __future__ import annotations

import math
import os

import numpy as np
from ase import Atoms
from ase.data import atomic_masses, atomic_numbers

from .slab import fixed_mask

# SSSP efficiency (v1.x) file names. Check them against your own library.
PSEUDOS_SSSP = {
    "Ba": "Ba.pbe-spn-kjpaw_psl.1.0.0.UPF",
    "Ti": "ti_pbe_v1.4.uspp.F.UPF",
    "O": "O.pbe-n-kjpaw_psl.0.1.UPF",
    "C": "C.pbe-n-kjpaw_psl.1.0.0.UPF",
}
# pslibrary 1.0.0 PAW (can be generated locally with tools/make_pslibrary_paw.sh)
PSEUDOS_PSL = {
    "Ba": "Ba.pbe-spn-kjpaw_psl.1.0.0.UPF",
    "Ti": "Ti.pbe-spn-kjpaw_psl.1.0.0.UPF",
    "O": "O.pbe-n-kjpaw_psl.1.0.0.UPF",
    "C": "C.pbe-n-kjpaw_psl.1.0.0.UPF",
}
PSEUDO_SETS = {"sssp": PSEUDOS_SSSP, "psl": PSEUDOS_PSL}
# default (ecutwfc, ecutrho) in Ry. The largest requirement wins:
#   SSSP efficiency: O 50/400 (PAW 0.1), Ti GBRV 35/280, Ba 30/240, C 45/360
#   pslibrary 1.0.0: Ti.spn suggests 52/575 (!), O 47/323, C 40/326, Ba 37/176
# Too small an ecutrho gives "negative rho" of ~1 e and SCF that never converges.
CUTOFFS = {"sssp": (50.0, 400.0), "psl": (55.0, 600.0)}

VDW = {
    "d3": {"vdw_corr": "dft-d3", "dftd3_version": 4},  # D3 with Becke-Johnson damping
    "d3zero": {"vdw_corr": "dft-d3", "dftd3_version": 3},
    "rvv10": {"input_dft": "rvv10"},
    "rev-vdw-df2": {"input_dft": "rev-vdw-df2"},
    "none": {},
}


def fmt(v) -> str:
    if isinstance(v, bool):
        return ".true." if v else ".false."
    if isinstance(v, str):
        return f"'{v}'"
    if isinstance(v, float):
        return f"{v:.10g}"
    return str(v)


def namelist(name: str, d: dict) -> str:
    lines = [f"&{name}"]
    for k, v in d.items():
        if v is None:
            continue
        lines.append(f"  {k} = {fmt(v)}")
    lines.append("/")
    return "\n".join(lines)


def auto_kpts(atoms: Atoms, kdens: float = 28.0, vacuum_axes=()) -> tuple:
    """n_i = max(1, round(kdens / |a_i|)); 1 along vacuum directions."""
    lens = atoms.cell.lengths()
    k = [max(1, int(round(kdens / L))) for L in lens]
    for ax in vacuum_axes:
        k[ax] = 1
    return tuple(k)


def dipole_settings(atoms: Atoms, axis: int = 2) -> dict:
    """Place the dipole-correction sawtooth in the middle of the largest vacuum gap."""
    c = atoms.cell.lengths()[axis]
    f = np.sort(atoms.get_scaled_positions(wrap=True)[:, axis])
    gaps = np.diff(np.concatenate([f, [f[0] + 1.0]]))
    i = int(np.argmax(gaps))
    gap = gaps[i]
    mid = (f[i] + gap / 2.0) % 1.0
    eopreg = min(0.1, 0.4 * gap)
    if gap * c < 8.0:
        raise ValueError(f"largest vacuum gap is only {gap * c:.1f} A; increase the vacuum")
    return {
        "edir": axis + 1,
        "emaxpos": round((mid - eopreg / 2.0) % 1.0, 4),
        "eopreg": round(eopreg, 4),
        "eamp": 0.0,
    }


def species_block(symbols, pseudos) -> str:
    lines = ["ATOMIC_SPECIES"]
    for s in symbols:
        if s not in pseudos:
            raise KeyError(f"no pseudopotential given for {s}")
        lines.append(f"  {s:2s} {atomic_masses[atomic_numbers[s]]:10.4f}  {pseudos[s]}")
    return "\n".join(lines)


def cell_block(atoms: Atoms) -> str:
    lines = ["CELL_PARAMETERS angstrom"]
    for v in atoms.cell.array:
        lines.append("  " + " ".join(f"{x:15.9f}" for x in v))
    return "\n".join(lines)


def positions_block(atoms: Atoms, flags: bool = True, header: str = "ATOMIC_POSITIONS angstrom") -> str:
    fixed = fixed_mask(atoms)
    lines = [header]
    for s, p, fx in zip(atoms.get_chemical_symbols(), atoms.positions, fixed):
        line = f"  {s:2s} " + " ".join(f"{x:14.8f}" for x in p)
        if flags:
            line += "  0 0 0" if fx else "  1 1 1"
        lines.append(line)
    return "\n".join(lines)


def kpoints_block(kpts, gamma: bool = False) -> str:
    if gamma:
        return "K_POINTS gamma"
    return "K_POINTS automatic\n  {} {} {} 0 0 0".format(*kpts)


def unique_species(atoms: Atoms) -> list[str]:
    seen = []
    for s in atoms.get_chemical_symbols():
        if s not in seen:
            seen.append(s)
    return seen


def build_namelists(
    atoms: Atoms,
    calculation: str = "relax",
    prefix: str = "pwscf",
    pseudo_dir: str = "./pseudo",
    outdir: str = "./tmp",
    ecutwfc: float = 50.0,
    ecutrho: float = 400.0,
    vdw: str = "d3",
    dipole: bool | None = None,
    spin: dict | None = None,
    hubbard_u: dict | None = None,
    smearing: tuple = ("mv", 0.01),
    conv_thr: float = 1.0e-8,
    forc_conv_thr: float = 1.0e-3,
    extra: dict | None = None,
    neb: bool = False,
) -> dict:
    """Return {'CONTROL': {...}, 'SYSTEM': {...}, 'ELECTRONS': {...}, 'IONS': {...}, 'CELL': {...}}.

    spin      : e.g. {'tot_magnetization': 2} (O2 triplet) or
                {'starting_magnetization': {'Ti': 0.3}} (reduced / defective slab).
    hubbard_u : e.g. {'Ti': 3.0}  -> Ti-3d U with ortho-atomic projectors (QE >= 7.1 syntax).
    extra     : {'SYSTEM': {...}, 'ELECTRONS': {...}, ...} merged last.
    """
    species = unique_species(atoms)
    if dipole is None:
        dipole = atoms.info.get("vacuum_axis") is not None
    control = {
        "calculation": None if neb else calculation,
        "prefix": prefix,
        "pseudo_dir": pseudo_dir,
        "outdir": outdir,
        "tprnfor": None if neb else True,
        "forc_conv_thr": None if neb or calculation == "scf" else forc_conv_thr,
        "etot_conv_thr": None if neb or calculation == "scf" else 1.0e-5,
        "nstep": None if neb else 300,
        "tstress": True if calculation == "vc-relax" else None,
        "disk_io": "low",
    }
    system = {
        "ibrav": 0,
        "nat": len(atoms),
        "ntyp": len(species),
        "ecutwfc": ecutwfc,
        "ecutrho": ecutrho,
        "occupations": "smearing",
        "smearing": smearing[0],
        "degauss": smearing[1],
    }
    system.update(VDW[vdw])
    if dipole:
        control.update(tefield=True, dipfield=True)
        system.update(dipole_settings(atoms, atoms.info.get("vacuum_axis", 2)))
    if spin:
        system["nspin"] = 2
        if "tot_magnetization" in spin:
            system["tot_magnetization"] = spin["tot_magnetization"]
        for s, m in spin.get("starting_magnetization", {}).items():
            if s in species:
                system[f"starting_magnetization({species.index(s) + 1})"] = m
        if "tot_magnetization" not in spin and not spin.get("starting_magnetization"):
            raise ValueError("spin needs tot_magnetization or starting_magnetization")
    electrons = {
        "conv_thr": conv_thr,
        "mixing_beta": 0.3,
        "mixing_mode": "plain",
        "electron_maxstep": 200,
    }
    nl = {"CONTROL": control, "SYSTEM": system, "ELECTRONS": electrons}
    if not neb and calculation in ("relax", "vc-relax", "md"):
        nl["IONS"] = {"ion_dynamics": "bfgs"}
    if not neb and calculation == "vc-relax":
        nl["CELL"] = {"cell_dynamics": "bfgs", "press_conv_thr": 0.2}
    for k, v in (extra or {}).items():
        nl.setdefault(k, {}).update(v)
    nl["_hubbard"] = hubbard_u
    return nl


def hubbard_block(hubbard_u: dict | None) -> str:
    if not hubbard_u:
        return ""
    lines = ["HUBBARD {ortho-atomic}"]
    for s, u in hubbard_u.items():
        orb = {"Ti": "3d", "Ba": "5d"}.get(s, "3d")
        lines.append(f"  U {s}-{orb} {u}")
    return "\n".join(lines)


def write_pw(
    path: str,
    atoms: Atoms,
    pseudos: dict = PSEUDOS_SSSP,
    kpts=None,
    gamma: bool = False,
    **kw,
) -> str:
    """Write a pw.x input file. Extra keyword arguments go to :func:`build_namelists`."""
    nl = build_namelists(atoms, **kw)
    hub = nl.pop("_hubbard")
    if kpts is None:
        vac = () if atoms.info.get("vacuum_axis") is None else (atoms.info["vacuum_axis"],)
        kpts = auto_kpts(atoms, vacuum_axes=vac)
    parts = [namelist(k, v) for k, v in nl.items()]
    parts += [
        species_block(unique_species(atoms), pseudos),
        cell_block(atoms),
        positions_block(atoms),
        kpoints_block(kpts, gamma),
    ]
    if hub:
        parts.append(hubbard_block(hub))
    text = "\n".join(parts) + "\n"
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w") as f:
        f.write(text)
    return text


def write_neb(
    path: str,
    images: list[Atoms],
    pseudos: dict = PSEUDOS_SSSP,
    kpts=None,
    gamma: bool = False,
    num_of_images: int | None = None,
    ci_scheme: str = "auto",
    path_thr: float = 0.05,
    nstep_path: int = 200,
    opt_scheme: str = "broyden",
    k_max: float = 0.3,
    k_min: float = 0.2,
    **kw,
) -> str:
    """neb.x input with every given image written explicitly.

    ``images[0]`` / ``images[-1]`` must be relaxed end points (same atom
    order, same settings). Intermediate images (e.g. IDPP) are given as
    INTERMEDIATE_IMAGE blocks. Constraints (fixed atoms) are taken from the
    first image, as neb.x does.

    Recommended two-stage run: (1) ci_scheme='no-CI', path_thr ~0.2-0.3 to
    get a reasonable path, then (2) restart with ci_scheme='auto', path_thr 0.05.
    """
    first = images[0]
    for im in images[1:]:
        if im.get_chemical_symbols() != first.get_chemical_symbols():
            raise ValueError("all images must have identical atom order")
    nl = build_namelists(first, neb=True, **kw)
    hub = nl.pop("_hubbard")
    if kpts is None:
        vac = () if first.info.get("vacuum_axis") is None else (first.info["vacuum_axis"],)
        kpts = auto_kpts(first, vacuum_axes=vac)
    path_nl = {
        "restart_mode": "from_scratch",
        "string_method": "neb",
        "nstep_path": nstep_path,
        "num_of_images": num_of_images or len(images),
        "opt_scheme": opt_scheme,
        "CI_scheme": ci_scheme,
        "path_thr": path_thr,
        "ds": 1.0,
        "k_max": k_max,
        "k_min": k_min,
    }
    pos = ["BEGIN_POSITIONS", "FIRST_IMAGE", positions_block(first, flags=True)]
    for im in images[1:-1]:
        pos += ["INTERMEDIATE_IMAGE", positions_block(im, flags=False)]
    pos += ["LAST_IMAGE", positions_block(images[-1], flags=False), "END_POSITIONS"]

    engine = [namelist(k, v) for k, v in nl.items()]
    engine += [species_block(unique_species(first), pseudos), "\n".join(pos),
               kpoints_block(kpts, gamma), cell_block(first)]
    if hub:
        engine.append(hubbard_block(hub))
    text = "\n".join(
        ["BEGIN", "BEGIN_PATH_INPUT", namelist("PATH", path_nl), "END_PATH_INPUT",
         "BEGIN_ENGINE_INPUT"] + engine + ["END_ENGINE_INPUT", "END"]
    ) + "\n"
    os.makedirs(os.path.dirname(os.path.abspath(path)), exist_ok=True)
    with open(path, "w") as f:
        f.write(text)
    return text


def estimate_cost(atoms: Atoms, valence=None) -> dict:
    """Rough size indicators (electrons, bands) to judge feasibility."""
    valence = valence or {"Ba": 10, "Ti": 12, "O": 6, "C": 4}
    ne = sum(valence[s] for s in atoms.get_chemical_symbols())
    return {"natoms": len(atoms), "electrons": ne, "bands~": int(math.ceil(ne / 2 * 1.2)),
            "volume_A3": round(atoms.get_volume(), 1)}
