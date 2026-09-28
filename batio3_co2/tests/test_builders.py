"""Geometry sanity checks for the structure builders (run: python -m pytest tests)."""

import os
import sys

import numpy as np
import pytest
from ase.neighborlist import neighbor_list

sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))

from btoco2 import (  # noqa: E402
    bulk_cubic,
    composition,
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
    wedge_gap_at,
)
from btoco2.co2 import min_dist_adsorbate_substrate  # noqa: E402
from btoco2.qe import dipole_settings, write_neb, write_pw  # noqa: E402
from btoco2.reactions import (  # noqa: E402
    c_o2_fs_from_is,
    c_o_fs_from_co,
    co_dissociation_fs_from_is,
    interpolate_path,
    o2_formation_endpoints,
    vacancy_healing_endpoints,
)
from btoco2.slab import fixed_mask  # noqa: E402

A = 4.0


def min_pair_distance(atoms):
    _, _, d = neighbor_list("ijd", atoms, 2.5)
    return d.min() if len(d) else np.inf


def substrate_ok(atoms):
    """No two substrate atoms closer than the Ti-O bond a/2."""
    return min_pair_distance(atoms) >= A / 2 - 1e-6


@pytest.mark.parametrize("top,nl,expect", [("TiO2", 7, {"Ti": 16, "Ba": 12, "O": 44}),
                                           ("BaO", 7, {"Ti": 12, "Ba": 16, "O": 40}),
                                           ("TiO2", 6, {"Ti": 12, "Ba": 12, "O": 36})])
def test_slab_composition(top, nl, expect):
    s = make_slab(A, nl, top, (2, 2))
    assert composition(s) == expect
    assert substrate_ok(s)
    assert s.info["termination_top"] == top


def test_sites_and_configs_no_clash():
    for top in ("TiO2", "BaO"):
        s = make_slab(A, 7, top, (2, 2))
        sites = surface_sites(s)
        assert any(k.startswith("top_O") for k in sites)
        for name, c in surface_configurations(s, sites).items():
            dmin = min_dist_adsorbate_substrate(c)
            assert dmin >= (1.3 if "carbonate" in name else 1.8), name


def test_opened_crack_recovers_bulk():
    """Even layer count + gap a/2 + no offset must reproduce bulk BaTiO3."""
    p = slit_pore_periodic(A, 6, "TiO2", (1, 1), gap=A / 2)
    b = bulk_cubic(A)
    for at in (p, b):
        _, _, d = neighbor_list("ijd", at, 3.0)
        at.info["cn"] = len(d) / len(at)
    assert p.info["cn"] == pytest.approx(b.info["cn"])
    assert composition(p) == {"Ba": 3, "Ti": 3, "O": 9}  # 6 layers = 3 unit cells


@pytest.mark.parametrize("gap", [6.0, 8.0, 12.0])
def test_slit_periodic(gap):
    p = slit_pore_periodic(A, 7, "TiO2", (2, 2), gap, offset=(0.5, 0.0))
    assert substrate_ok(p)
    assert p.info["z_wall_top"] - p.info["z_wall_bottom"] == pytest.approx(gap)
    for c in list(midgap_configurations(p).values()) + list(wall_configurations(p, "top").values()):
        assert min_dist_adsorbate_substrate(c) >= 1.3


def test_sandwich_and_dipole():
    s = slit_pore_sandwich(A, 5, "BaO", 5, "TiO2", (2, 2), gap=8.0)
    assert substrate_ok(s)
    d = dipole_settings(s)
    assert 0 < d["emaxpos"] < 1 and d["emaxpos"] + d["eopreg"] <= 1.0
    # sawtooth region must be > 3 A away from every atom
    c = s.cell.lengths()[2]
    lo, hi = d["emaxpos"] * c, (d["emaxpos"] + d["eopreg"]) * c
    z = s.positions[:, 2]
    assert np.all((z < lo - 3.0) | (z > hi + 3.0))


@pytest.mark.parametrize("angle", [10.0, 20.0, 35.0])
def test_tilted_plate(angle):
    w = tilted_plate_wedge(A, angle=angle, apex_gap=4.0)
    assert substrate_ok(w)
    comp = composition(w)
    assert comp["O"] == comp["Ba"] + 2 * comp["Ti"]  # charge neutral (Ba2+ Ti4+ O2-)
    assert wedge_gap_at(w, w.info["x_apex"]) == pytest.approx(4.0)
    assert wedge_gap_at(w, w.info["x_open"]) == pytest.approx(w.info["gap_open"])
    confs = wedge_configurations(w)
    assert confs
    for c in confs.values():
        assert min_dist_adsorbate_substrate(c) >= 2.4


@pytest.mark.parametrize("profile,terrace,h0", [("V", 1, 0), ("V", 2, 1), ("sawtooth", 1, 1)])
def test_stepped_wedge_stoichiometric(profile, terrace, h0):
    st = stepped_wedge(A, nx=8, ny=2, terrace=terrace, h0=h0, profile=profile)
    comp = composition(st)
    assert comp["Ba"] == comp["Ti"] and comp["O"] == 3 * comp["Ti"]
    assert substrate_ok(st)
    assert fixed_mask(st).sum() > 0
    for c in wedge_configurations(st).values():
        assert min_dist_adsorbate_substrate(c) >= 2.4


def test_reaction_endpoints_consistent(tmp_path):
    for top in ("TiO2", "BaO"):
        s = make_slab(A, 7, top, (2, 2))
        r1 = vacancy_healing_endpoints(s)
        assert r1["IS"].get_chemical_symbols() == r1["FS"].get_chemical_symbols()
        assert len(r1["IS"]) == len(s) + 2  # one O removed, CO2 added
        IS = surface_configurations(s, surface_sites(s))["top_Ti__bidentate_x" if top == "TiO2"
                                                         else "top_O__carbonate_x"]
        FS2 = co_dissociation_fs_from_is(IS)
        FS3 = c_o_fs_from_co(FS2)
        FS5 = c_o2_fs_from_is(IS)
        r4 = o2_formation_endpoints(s)
        for a_, b_ in ((IS, FS2), (FS2, FS3), (IS, FS5), (r4["IS"], r4["FS"])):
            assert a_.get_chemical_symbols() == b_.get_chemical_symbols()
            assert min_dist_adsorbate_substrate(b_) > 1.1
        images = interpolate_path(r1["IS"], r1["FS"], 5)
        txt = write_neb(str(tmp_path / f"neb_{top}.in"), images)
        assert txt.count("INTERMEDIATE_IMAGE") == 3
        write_pw(str(tmp_path / f"is_{top}.in"), r1["IS"], spin={"starting_magnetization": {"Ti": 0.3}},
                 hubbard_u={"Ti": 3.0})


def test_refresh_levels_after_relaxation():
    from btoco2 import refresh_levels

    s = make_slab(A, 7, "BaO", (2, 2))
    top = s.get_tags() == 1
    s.positions[top, 2] += 0.15  # mimic outward relaxation of the surface layer
    refresh_levels(s)
    assert s.info["z_top"] == pytest.approx(s.positions[top, 2].mean())
    c = surface_configurations(s, surface_sites(s))["top_O__carbonate_x"]
    assert c.positions[-3:, 2].min() == pytest.approx(s.info["z_top"] + 1.45)

    p = slit_pore_periodic(A, 7, "TiO2", (2, 2), gap=8.0)
    tags = p.get_tags()
    p.positions[tags == 1, 2] += 0.2
    p.positions[tags == tags.max(), 2] -= 0.2
    refresh_levels(p)
    assert p.info["gap"] == pytest.approx(8.0 - 0.4)
    for at in midgap_configurations(p).values():
        z = at.positions[-3, 2]  # C atom at the new mid-gap
        assert z == pytest.approx(0.5 * (p.info["z_wall_bottom"] + p.info["z_wall_top"]))
