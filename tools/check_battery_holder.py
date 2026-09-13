"""Export and check holder 0.2 against the pinned, reconstructed zcar meshes.

Requires tools/layout_zcar_battery.py output; no source modification or repair.
Positive controls deliberately collide with the retained guards and band.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np
import trimesh
from layout_zcar_battery import box, overlap, broad_phase, unique_part
from inspect_zcar import inspect


def main(openscad):
    root = Path(__file__).resolve().parents[1]
    checkout = root / "build/upstream/zcar"
    provenance = inspect(checkout)
    previous = json.loads((root / "build/cad-layout/report.json").read_text())
    assert previous["revision"] == provenance["revision"]
    assert previous["source_sha256"] == {k:v["sha256"] for k,v in provenance["files"].items()}
    out = root / "build/cad-holder"
    out.mkdir(parents=True,exist_ok=True)
    source = root / "cad/battery-holder.scad"
    meshes = {}
    for name in ["support_installed","guards_installed","band_corridor","support_print","guard_print"]:
        path = out / f"{name}.stl"
        subprocess.run([openscad,"-o",str(path),"-D",f'part="{name}"',str(source)],check=True,capture_output=True)
        mesh = trimesh.load_mesh(path)
        assert mesh.is_volume, name
        meshes[name] = mesh
    frame = trimesh.load_mesh(root / "build/cad-layout/frame.stl")
    cover = trimesh.load_mesh(root / "build/cad-layout/cover.stl")
    assembly = trimesh.load_mesh(checkout / "stl/zcar.stl").split(only_watertight=False,repair=False)
    fi, original_frame = unique_part(assembly,[49.5,117.375,23.25])
    ci, _ = unique_part(assembly,[49.5,47.05,19])
    origin = original_frame.bounds[0].copy()
    for mesh in assembly: mesh.apply_translation(-origin)
    battery = box([49,18,15],[24.75,55,10.25])
    swept = box([99,18,15],[-.25,55,10.25])
    parts = {"frame":frame,"cover":cover,**{k:meshes[k] for k in ["support_installed","guards_installed","band_corridor"]}}
    checks = {}
    for label,part in parts.items():
        checks[label] = {"battery_overlap_mm3":overlap(battery,part),
                         "lateral_sweep_overlap_mm3":overlap(swept,part)}
        assert checks[label]["battery_overlap_mm3"] < 1e-5, (label,checks[label])
    for label in ["frame","cover","support_installed"]:
        assert checks[label]["lateral_sweep_overlap_mm3"] < 1e-5
    assert checks["guards_installed"]["lateral_sweep_overlap_mm3"] > 1
    assert checks["band_corridor"]["lateral_sweep_overlap_mm3"] > 1
    contacts = {}
    for label in ["support_installed","guards_installed","band_corridor"]:
        part=parts[label]
        contacts[label]={"frame_overlap_mm3":overlap(part,frame),"cover_overlap_mm3":overlap(part,cover),
                         "other_assembly_aabb_overlaps":broad_phase(assembly,part.bounds,{fi,ci})}
        assert contacts[label]["frame_overlap_mm3"] < 1e-5, contacts[label]
        assert contacts[label]["cover_overlap_mm3"] < 1e-5, contacts[label]
        # Broad bounds of a ring/paired guards contain empty space. Report
        # candidates conservatively; do not treat an AABB hit as exact contact.
    for name in ["support_print","guard_print"]:
        assert np.allclose(meshes[name].bounds[0],0,atol=1e-5), meshes[name].bounds
        assert len(meshes[name].split(only_watertight=False,repair=False))==1
    internal_overlap={}
    for a,b in [("support_installed","guards_installed"),("support_installed","band_corridor"),("guards_installed","band_corridor")]:
        internal_overlap[f"{a}/{b}"]=overlap(parts[a],parts[b])
        assert internal_overlap[f"{a}/{b}"]<1e-5
    # Conservative swept volumes of the three un-grooved C-guard bars.
    # Grooves only remove material, so zero overlap proves the real sweep free.
    left_sweeps=[box([7.2,8,22.45],[-3.75,55,9.875]),
                 box([10.85,8,1.2],[-1.925,55,-.75]),
                 box([10.85,8,1.2],[-1.925,55,20.5])]
    guard_removal={}
    for side in ["left","right"]:
        volumes=[]
        for sweep in left_sweeps:
            candidate=sweep.copy()
            if side=="right":
                transform=np.eye(4);transform[0,0]=-1;transform[0,3]=49.5
                candidate.apply_transform(transform)
            volumes.append(candidate)
        guard_removal[side]={name:sum(overlap(v,m) for v in volumes)
                            for name,m in {"frame":frame,"cover":cover,"battery":battery,"support":parts["support_installed"]}.items()}
        assert all(v<1e-5 for v in guard_removal[side].values()),guard_removal
    report={"revision":provenance["revision"],"source_sha256":previous["source_sha256"],
            "design_sha256":hashlib.sha256(source.read_bytes()).hexdigest(),
            "battery_mm":[49,18,15],"battery_checks":checks,"holder_checks":contacts,
            "guard_removal_6mm_sweep_overlap_mm3":guard_removal,
            "holder_internal_overlap_mm3":internal_overlap,
            "band_lowest_z_mm":float(parts["band_corridor"].bounds[0,2]),
            "source_assembly_lowest_z_mm":float(min(m.bounds[0,2] for m in assembly)),
            "exports":{k:{"bounds_mm":v.bounds.tolist(),"volume_mm3":float(v.volume),
                           "sha256":hashlib.sha256((out/f"{k}.stl").read_bytes()).hexdigest()} for k,v in meshes.items()},
            "limits":["Seller dimensions; no physical battery measurement",
                      "No wires, connectors, custom body or new electronics",
                      "Band is a reserved corridor, not an elastic material or force model",
                      "Adhesive/liner/band products and compressed thickness unverified",
                      "No print, retention force, chassis clearance or crash validation"]}
    (out/"report.json").write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
    fig,axes=plt.subplots(1,2,figsize=(12,5),layout="constrained")
    colors=["#386cb0","#888888","#23976c","#923cb6","#222222"]
    for ax,normal,pt,idx,title in [(axes[0],[0,1,0],[0,55,0],[0,2],"Cross-section Y=55 mm"),
                                  (axes[1],[0,0,1],[0,0,4],[0,1],"Horizontal section Z=4 mm")]:
        for (label,mesh),color in zip(parts.items(),colors):
            section=mesh.section(plane_origin=pt,plane_normal=normal)
            if section:
                for i,line in enumerate(section.discrete):
                    ax.plot(line[:,idx[0]],line[:,idx[1]],color=color,label=label if i==0 else None)
        low=battery.bounds[0,idx]; size=battery.extents[idx]
        ax.add_patch(plt.Rectangle(low,*size,color="#ef8a23",alpha=.45,label="Battery envelope"))
        ax.set_aspect("equal");ax.grid(alpha=.2);ax.set_title(title)
        ax.set_xlim(-4,54)
        ax.set_ylim((-4,24) if idx[1]==2 else (40,70))
        ax.set_xlabel("X (mm)");ax.set_ylabel("Z (mm)" if idx[1]==2 else "Y (mm)")
    axes[0].legend(fontsize=7,loc="upper center",bbox_to_anchor=(.5,-.2),ncol=3)
    fig.suptitle("Holder 0.2: removable end guards + elastic band corridor\nProvisional CAD only; remove band and guards before lateral battery extraction")
    fig.savefig(out/"sections.png",dpi=160)
    print(json.dumps({"battery_checks":checks,"holder_checks":contacts},indent=2))


if __name__=="__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--openscad",default="C:/Program Files/OpenSCAD/openscad.com")
    main(parser.parse_args().openscad)
