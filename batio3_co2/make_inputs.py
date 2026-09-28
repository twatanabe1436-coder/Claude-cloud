#!/usr/bin/env python3
"""Generate Quantum ESPRESSO inputs for CO2 on / in BaTiO3 and analyse the results.

Every calculation gets its own directory containing
    pw.in              pw.x input (run it from inside that directory)
    structure.extxyz   the same structure with tags / constraints / adsorbate flags
    meta.json          what the calculation is and which references it needs
    preview.png        (with --png) side + top view

Typical workflow (see README.md):
    python make_inputs.py refs                         # CO2, CO, O2, bulk vc-relax
    python make_inputs.py surface --term TiO2 BaO      # open surfaces, all sites x orientations
    python make_inputs.py slit --gaps 6 7 8 10 12      # slit pores
    python make_inputs.py wedge-plate --angles 10 20 30
    python make_inputs.py wedge-step --terrace 1 2
    python make_inputs.py neb-endpoints --term TiO2 BaO
    ... run pw.x ...
    python make_inputs.py analyze runs                 # adsorption energies table
    python make_inputs.py neb --is runs/neb/R1_vac_TiO2/IS --fs runs/neb/R1_vac_TiO2/FS
    python make_inputs.py plot-neb runs/neb/R1_vac_TiO2/neb
"""

from __future__ import annotations

import argparse
import csv
import glob
import json
import os
import re
import sys

import numpy as np
from ase.io import read, write

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from btoco2 import (  # noqa: E402
    gas_molecule,
    make_slab,
    midgap_configurations,
    slit_pore_periodic,
    slit_pore_sandwich,
    stepped_wedge,
    surface_configurations,
    surface_sites,
    tilted_plate_wedge,
    wall_configurations,
    wedge_configurations,
)
from btoco2.co2 import random_configurations  # noqa: E402
from btoco2.qe import CUTOFFS, PSEUDO_SETS, auto_kpts, estimate_cost, write_neb, write_pw  # noqa: E402
from btoco2.reactions import (  # noqa: E402
    c_o2_fs_from_is,
    c_o_fs_from_co,
    co_dissociation_fs_from_is,
    interpolate_path,
    load_relaxed,
    make_o_vacancy,
    o2_formation_endpoints,
    vacancy_healing_endpoints,
)
from btoco2.slab import bulk_cubic  # noqa: E402

RY = 13.605693122994  # eV


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------
def emit(args, path, atoms, meta, qe=None, png=None):
    """Write pw.in + structure.extxyz + meta.json (+ preview.png) into ``path``."""
    qe = dict(qe or {})
    os.makedirs(path, exist_ok=True)
    pseudo_dir = os.path.relpath(os.path.abspath(args.pseudo_dir), os.path.abspath(path))
    prefix = re.sub(r"[^A-Za-z0-9_]", "_", os.path.basename(os.path.normpath(path)))[:40] or "pwscf"
    kw = dict(ecutwfc=args.ecutwfc, ecutrho=args.ecutrho, vdw=args.vdw)
    kw.update(qe)
    kpts = kw.pop("kpts", None)
    if kpts is None and "vacuum_axis" in atoms.info:
        kpts = auto_kpts(atoms, args.kdens, (atoms.info["vacuum_axis"],))
    elif kpts is None and atoms.info.get("kind") != "molecule":
        kpts = auto_kpts(atoms, args.kdens)
        if atoms.info.get("kind", "").startswith(("slit", "wedge")):
            kpts = (kpts[0], kpts[1], 1)  # insulating walls: no dispersion across the pore
    write_pw(os.path.join(path, "pw.in"), atoms, pseudos=PSEUDO_SETS[args.pseudo_set],
             pseudo_dir=pseudo_dir, prefix=prefix, kpts=kpts, **kw)
    info_backup = atoms.info
    atoms.info = {k: v for k, v in atoms.info.items() if isinstance(v, (int, float, str, bool))}
    write(os.path.join(path, "structure.extxyz"), atoms)
    atoms.info = info_backup
    meta = dict(meta)
    meta["qe"] = {k: v for k, v in qe.items() if k in ("spin", "hubbard_u", "calculation", "gamma", "kpts")}
    meta["size"] = estimate_cost(atoms)
    with open(os.path.join(path, "meta.json"), "w") as f:
        json.dump(meta, f, indent=1, default=str)
    if args.png or png:
        from btoco2.viz import preview

        preview(atoms, os.path.join(path, "preview.png"), title=os.path.basename(path))
    return path


def select(args, configs):
    """Apply --orient / --sites filters (substring match on the configuration name)."""
    out = {}
    for name, at in configs.items():
        o = at.info.get("orientation", "")
        site = at.info.get("site", name)
        if getattr(args, "orient", None) and o and o not in args.orient:
            continue
        if getattr(args, "sites", None) and not any(k in site for k in args.sites):
            continue
        out[name] = at
    return out


def emit_set(args, root, empty, configs, gas="CO2", qe=None):
    configs = select(args, configs)
    emit(args, os.path.join(root, "empty"), empty, {"kind": "reference"}, qe)
    for name, atoms in configs.items():
        emit(args, os.path.join(root, name), atoms, {"kind": "adsorption", "ref": "../empty", "gas": [gas]}, qe)
    print(f"{root}: empty + {len(configs)} configurations  ({len(empty)} substrate atoms)")


# ---------------------------------------------------------------------------
# sub-commands: input generation
# ---------------------------------------------------------------------------
def cmd_refs(args):
    root = os.path.join(args.out, "refs")
    emit(args, f"{root}/CO2", gas_molecule("CO2"), {"kind": "gas"}, {"gamma": True})
    emit(args, f"{root}/CO", gas_molecule("CO"), {"kind": "gas"}, {"gamma": True})
    emit(args, f"{root}/O2", gas_molecule("O2"), {"kind": "gas"},
         {"gamma": True, "spin": {"tot_magnetization": 2}})
    b = bulk_cubic(args.a)
    b.info["kind"] = "bulk"
    emit(args, f"{root}/bulk", b, {"kind": "bulk"}, {"calculation": "vc-relax", "kpts": (8, 8, 8)})
    print(f"{root}: CO2, CO, O2 (triplet), bulk vc-relax")


def cmd_surface(args):
    for term in args.term:
        slab = make_slab(args.a, args.nlayers, term, tuple(args.size), args.vacuum)
        confs = surface_configurations(slab, surface_sites(slab))
        if args.random:
            zs = slab.info["z_top"]
            for i, at in enumerate(random_configurations(slab, args.random, (zs + 2.6, zs + 4.0), seed=args.seed)):
                confs[f"random{i:03d}"] = at
        emit_set(args, os.path.join(args.out, "surface", f"{term}_{args.nlayers}L_{args.size[0]}x{args.size[1]}"),
                 slab, confs)


def cmd_slit(args):
    for term in args.term:
        for gap in args.gaps:
            for off in args.offsets:
                offset = (off, off)
                if args.mode == "periodic":
                    pore = slit_pore_periodic(args.a, args.nlayers, term, tuple(args.size), gap, offset)
                else:
                    pore = slit_pore_sandwich(args.a, args.nlayers, term, args.nlayers, args.face_top or term,
                                              tuple(args.size), gap, offset, args.vacuum)
                confs = midgap_configurations(pore)
                confs.update(wall_configurations(pore, "bottom"))
                if pore.info["wall_top"] != pore.info["wall_bottom"] or offset != (0.0, 0.0):
                    confs.update(wall_configurations(pore, "top"))
                tag = f"{args.mode}_{term}_{args.nlayers}L_g{gap:.1f}" + (f"_off{off:g}" if off else "")
                emit_set(args, os.path.join(args.out, "slit", tag), pore, confs)


def cmd_wedge_plate(args):
    for ang in args.angles:
        pore = tilted_plate_wedge(args.a, ang, args.apex_gap, args.plate_width, args.plate_layers,
                                  args.plate_face, bottom_face=args.bottom_face, ny=args.ny)
        confs = wedge_configurations(pore, n=args.npos)
        emit_set(args, os.path.join(args.out, "wedge_plate", f"ang{ang:g}_apex{args.apex_gap:g}"), pore, confs)


def cmd_wedge_step(args):
    for t in args.terrace:
        pore = stepped_wedge(args.a, args.nx, args.ny, t, args.h0, args.profile, args.floor_face, args.wall)
        confs = wedge_configurations(pore)
        emit_set(args, os.path.join(args.out, "wedge_step", f"{args.profile}_t{t}_h{args.h0}_nx{args.nx}"),
                 pore, confs)


def cmd_neb_endpoints(args):
    spin_ti = {"spin": {"starting_magnetization": {"Ti": 0.3}}}
    spin_o = {"spin": {"starting_magnetization": {"O": 0.2, "C": 0.2}}}
    for term in args.term:
        slab = make_slab(args.a, args.nlayers, term, tuple(args.size), args.vacuum)
        root = os.path.join(args.out, "neb")
        qe_v = dict(spin_ti, **({"hubbard_u": {"Ti": args.u}} if args.u else {}))

        # R1: CO2 + V_O -> CO + O_O
        r1 = vacancy_healing_endpoints(slab)
        d = f"{root}/R1_vac_{term}"
        emit(args, f"{d}/slab_vac", make_o_vacancy(slab)[0], {"kind": "reference"}, qe_v)
        emit(args, f"{d}/IS", r1["IS"], {"kind": "neb_end", "ref": "../slab_vac", "gas": ["CO2"]}, qe_v)
        emit(args, f"{d}/FS", r1["FS"], {"kind": "neb_end", "ref": "../slab_vac", "gas": ["CO2"]}, qe_v)

        # R2: CO2* -> CO* + O*   (IS: carbonate on BaO, bidentate on TiO2 -- replace by your best state)
        sites = surface_sites(slab)
        confs = surface_configurations(slab, sites)
        is_name = "top_O__carbonate_x" if term == "BaO" else "top_Ti__bidentate_x"
        IS = confs[is_name]
        FS2 = co_dissociation_fs_from_is(IS)
        d = f"{root}/R2_CO2_CO+O_{term}"
        emit(args, f"{d}/slab", slab, {"kind": "reference"})
        for lab, at in (("IS", IS), ("FS", FS2)):
            emit(args, f"{d}/{lab}", at, {"kind": "neb_end", "ref": "../slab", "gas": ["CO2"], "from": is_name})

        # R3: CO* + O* -> C* + O* + O*
        FS3 = c_o_fs_from_co(FS2)
        d = f"{root}/R3_CO_C+O_{term}"
        for lab, at in (("IS", FS2), ("FS", FS3)):
            emit(args, f"{d}/{lab}", at, {"kind": "neb_end", "ref": "../../R2_CO2_CO+O_" + term + "/slab",
                                          "gas": ["CO2"]}, spin_o)

        # R4: O* + O* -> O2*
        r4 = o2_formation_endpoints(slab)
        d = f"{root}/R4_2O_O2_{term}"
        for lab in ("IS", "FS"):
            emit(args, f"{d}/{lab}", r4[lab], {"kind": "neb_end", "ref": "../../R2_CO2_CO+O_" + term + "/slab",
                                               "gas": ["O2"]}, spin_o)

        # R5: concerted CO2* -> C* + O2*  (for comparison only)
        d = f"{root}/R5_CO2_C+O2_{term}"
        for lab, at in (("IS", IS), ("FS", c_o2_fs_from_is(IS))):
            emit(args, f"{d}/{lab}", at, {"kind": "neb_end", "ref": "../../R2_CO2_CO+O_" + term + "/slab",
                                          "gas": ["CO2"]}, spin_o)
        print(f"{root}: R1-R5 end points for {term} (relax IS and FS first, then run 'neb')")


def cmd_neb(args):
    ends = []
    for d in (args.is_dir, args.fs_dir):
        tmpl = read(os.path.join(d, "structure.extxyz"))
        out = os.path.join(d, "pw.out")
        at = load_relaxed(out, tmpl) if os.path.exists(out) and not args.unrelaxed else tmpl
        if not os.path.exists(out) and not args.unrelaxed:
            sys.exit(f"{out} not found (relax the end points first, or pass --unrelaxed for a dry run)")
        ends.append(at)
    meta = json.load(open(os.path.join(args.is_dir, "meta.json")))
    images = interpolate_path(ends[0], ends[1], args.nimages, method=args.interp)
    out = args.out_dir or os.path.join(os.path.dirname(os.path.normpath(args.is_dir)), "neb")
    os.makedirs(out, exist_ok=True)
    qe = dict(meta.get("qe", {}))
    qe.pop("calculation", None)
    pseudo_dir = os.path.relpath(os.path.abspath(args.pseudo_dir), os.path.abspath(out))
    kpts = qe.pop("kpts", None) or auto_kpts(images[0], args.kdens, (2,))
    write_neb(os.path.join(out, "neb.in"), images, pseudos=PSEUDO_SETS[args.pseudo_set], kpts=kpts,
              pseudo_dir=pseudo_dir, prefix="neb", ecutwfc=args.ecutwfc, ecutrho=args.ecutrho, vdw=args.vdw,
              ci_scheme=args.ci, path_thr=args.path_thr, **qe)
    write(os.path.join(out, "initial_path.extxyz"), images)
    print(f"{out}/neb.in written ({args.nimages} images, CI_scheme={args.ci})")


# ---------------------------------------------------------------------------
# analysis
# ---------------------------------------------------------------------------
def final_energy(d):
    """(energy in eV, converged flag) from d/pw.out, or (None, False)."""
    out = os.path.join(d, "pw.out")
    if not os.path.exists(out):
        return None, False
    txt = open(out, errors="ignore").read()
    m = re.findall(r"^!\s+total energy\s+=\s+(-?\d+\.\d+)\s+Ry", txt, re.M)
    if not m:
        return None, False
    fin = re.search(r"Final (?:enthalpy|energy)\s+=\s+(-?\d+\.\d+)\s+Ry", txt)
    e = float(fin.group(1) if fin else m[-1]) * RY
    ok = ("End of BFGS" in txt or "bfgs converged" in txt or "End of self-consistent" in txt) \
        and "convergence NOT achieved" not in txt and "The maximum number of steps has been reached" not in txt
    return e, ok


def cmd_analyze(args):
    gas = {}
    for g in ("CO2", "CO", "O2"):
        e, _ = final_energy(os.path.join(args.root, "refs", g))
        if e is not None:
            gas[g] = e
    rows = []
    for mfile in sorted(glob.glob(os.path.join(args.root, "**", "meta.json"), recursive=True)):
        d = os.path.dirname(mfile)
        meta = json.load(open(mfile))
        e, ok = final_energy(d)
        row = {"dir": os.path.relpath(d, args.root), "kind": meta.get("kind"), "E_eV": e, "converged": ok,
               "E_ads_eV": None}
        if e is not None and "ref" in meta:
            eref, okr = final_energy(os.path.normpath(os.path.join(d, meta["ref"])))
            if eref is not None and all(g in gas for g in meta.get("gas", [])):
                row["E_ads_eV"] = e - eref - sum(gas[g] for g in meta["gas"])
                row["converged"] = ok and okr
        rows.append(row)
    done = [r for r in rows if r["E_eV"] is not None]
    print(f"{len(done)}/{len(rows)} calculations have energies; gas refs: {sorted(gas)}")
    for r in sorted((r for r in done if r["E_ads_eV"] is not None), key=lambda r: (os.path.dirname(r["dir"]),
                                                                                    r["E_ads_eV"])):
        flag = "" if r["converged"] else "  (not converged)"
        print(f"  {r['dir']:60s} E_ads = {r['E_ads_eV']:8.3f} eV{flag}")
    if gas.keys() >= {"CO2", "CO", "O2"}:
        print(f"  gas phase  CO2 -> CO + 1/2 O2 : {gas['CO'] + 0.5 * gas['O2'] - gas['CO2']:.3f} eV"
              "  (exp. ~ +2.9 eV; PBE O2 overbinding shifts this)")
    with open(os.path.join(args.root, "summary.csv"), "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()) if rows else ["dir"])
        w.writeheader()
        w.writerows(rows)
    print(f"-> {os.path.join(args.root, 'summary.csv')}")


def cmd_plot_neb(args):
    import matplotlib

    matplotlib.use("Agg")
    import matplotlib.pyplot as plt

    dat = sorted(glob.glob(os.path.join(args.dir, "*.dat")))
    dat = [f for f in dat if not f.endswith("_old.dat")]
    if not dat:
        sys.exit(f"no neb.x *.dat file in {args.dir}")
    d = np.loadtxt(dat[0])
    fig, ax = plt.subplots(figsize=(5.5, 4))
    intf = dat[0][:-4] + ".int"
    if os.path.exists(intf):
        it = np.loadtxt(intf)
        ax.plot(it[:, 0], it[:, 1], "-", color="#3f6fb5", lw=1.5)
    ax.plot(d[:, 0], d[:, 1], "o", color="#3f6fb5", ms=6)
    i = int(np.argmax(d[:, 1]))
    ax.annotate(f"Ea = {d[i, 1]:.2f} eV", (d[i, 0], d[i, 1]), textcoords="offset points", xytext=(0, 8),
                ha="center")
    ax.set_xlabel("reaction coordinate (normalised)")
    ax.set_ylabel("E - E(IS) (eV)")
    ax.spines[["top", "right"]].set_visible(False)
    fig.tight_layout()
    png = os.path.join(args.dir, "neb_profile.png")
    fig.savefig(png, dpi=130)
    print(f"forward barrier {d[:, 1].max():.3f} eV, reaction energy {d[-1, 1]:.3f} eV -> {png}")


# ---------------------------------------------------------------------------
def main():
    common = argparse.ArgumentParser(add_help=False)
    g = common.add_argument_group("common")
    g.add_argument("--out", default="runs", help="output root directory")
    g.add_argument("--a", type=float, default=4.00, help="BaTiO3 lattice constant (use your vc-relax value)")
    g.add_argument("--pseudo-set", default="sssp", choices=sorted(PSEUDO_SETS))
    g.add_argument("--pseudo-dir", default="pseudo", help="directory with the UPF files")
    g.add_argument("--ecutwfc", type=float, default=None, help="Ry (default: per pseudo set, sssp 50 / psl 55)")
    g.add_argument("--ecutrho", type=float, default=None, help="Ry (default: per pseudo set, sssp 400 / psl 600)")
    g.add_argument("--vdw", default="d3", choices=["d3", "d3zero", "rvv10", "rev-vdw-df2", "none"])
    g.add_argument("--kdens", type=float, default=28.0, help="k-point density: n_i = round(kdens/|a_i|)")
    g.add_argument("--png", action="store_true", help="also write preview.png for every structure")

    slab = argparse.ArgumentParser(add_help=False)
    s = slab.add_argument_group("slab")
    s.add_argument("--term", nargs="+", default=["TiO2", "BaO"], choices=["TiO2", "BaO"])
    s.add_argument("--nlayers", type=int, default=7)
    s.add_argument("--size", type=int, nargs=2, default=[2, 2])
    s.add_argument("--vacuum", type=float, default=20.0)
    s.add_argument("--orient", nargs="+", default=None,
                   help="only these CO2 orientations (flat_x flat_y flat_diag tilt45_x upright carbonate_x "
                        "carbonate_diag bidentate_x bidentate_y)")
    s.add_argument("--sites", nargs="+", default=None, help="only sites whose name contains one of these strings")

    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="cmd", required=True)

    sub.add_parser("refs", parents=[common], help="gas molecules + bulk vc-relax").set_defaults(f=cmd_refs)

    q = sub.add_parser("surface", parents=[common, slab], help="CO2 on open (001) surfaces")
    q.add_argument("--random", type=int, default=0, help="add N random CO2 placements")
    q.add_argument("--seed", type=int, default=0)
    q.set_defaults(f=cmd_surface)

    q = sub.add_parser("slit", parents=[common, slab], help="slit pores (parallel walls)")
    q.add_argument("--gaps", type=float, nargs="+", default=[6.0, 7.0, 8.0, 10.0, 12.0])
    q.add_argument("--mode", default="periodic", choices=["periodic", "sandwich"])
    q.add_argument("--offsets", type=float, nargs="+", default=[0.0], help="lateral wall offset (units of a)")
    q.add_argument("--face-top", default=None, choices=["TiO2", "BaO"], help="sandwich: upper wall termination")
    q.set_defaults(f=cmd_slit, nlayers=7, vacuum=15.0)

    q = sub.add_parser("wedge-plate", parents=[common], help="tilted plate over a slab (wedge pore)")
    q.add_argument("--angles", type=float, nargs="+", default=[10.0, 20.0, 30.0])
    q.add_argument("--apex-gap", type=float, default=4.0)
    q.add_argument("--plate-width", type=int, default=4)
    q.add_argument("--plate-layers", type=int, default=4)
    q.add_argument("--plate-face", default="BaO", choices=["TiO2", "BaO"])
    q.add_argument("--bottom-face", default="TiO2", choices=["TiO2", "BaO"])
    q.add_argument("--ny", type=int, default=2)
    q.add_argument("--npos", type=int, default=5, help="CO2 positions along the wedge")
    q.set_defaults(f=cmd_wedge_plate)

    q = sub.add_parser("wedge-step", parents=[common], help="stepped wedge cavity (fully periodic)")
    q.add_argument("--terrace", type=int, nargs="+", default=[1, 2])
    q.add_argument("--nx", type=int, default=8)
    q.add_argument("--ny", type=int, default=2)
    q.add_argument("--h0", type=int, default=0)
    q.add_argument("--profile", default="V", choices=["V", "sawtooth"])
    q.add_argument("--floor-face", default="TiO2", choices=["TiO2", "BaO"])
    q.add_argument("--wall", type=int, default=3)
    q.set_defaults(f=cmd_wedge_step)

    q = sub.add_parser("neb-endpoints", parents=[common, slab], help="IS/FS guesses for R1-R5")
    q.add_argument("--u", type=float, default=0.0, help="Hubbard U on Ti-3d for the vacancy path (eV)")
    q.set_defaults(f=cmd_neb_endpoints)

    q = sub.add_parser("neb", parents=[common], help="neb.x input from relaxed IS/FS directories")
    q.add_argument("--is", dest="is_dir", required=True)
    q.add_argument("--fs", dest="fs_dir", required=True)
    q.add_argument("--nimages", type=int, default=7)
    q.add_argument("--ci", default="auto", choices=["auto", "no-CI", "manual"])
    q.add_argument("--path-thr", type=float, default=0.05)
    q.add_argument("--interp", default="idpp", choices=["idpp", "linear"])
    q.add_argument("--out-dir", default=None)
    q.add_argument("--unrelaxed", action="store_true", help="dry run with the unrelaxed guesses")
    q.set_defaults(f=cmd_neb)

    q = sub.add_parser("analyze", help="collect energies, adsorption energies -> summary.csv")
    q.add_argument("root", nargs="?", default="runs")
    q.set_defaults(f=cmd_analyze)

    q = sub.add_parser("plot-neb", help="plot a neb.x energy profile")
    q.add_argument("dir")
    q.set_defaults(f=cmd_plot_neb)

    args = p.parse_args()
    if hasattr(args, "pseudo_set"):
        wfc, rho = CUTOFFS[args.pseudo_set]
        args.ecutwfc = args.ecutwfc or wfc
        args.ecutrho = args.ecutrho or rho
    args.f(args)


if __name__ == "__main__":
    main()
