"""Package existing print meshes, in millimetres, without slicer settings/G-code."""
from hashlib import sha256
from itertools import combinations
from pathlib import Path
import json
import xml.etree.ElementTree as ET
from zipfile import ZipFile, ZipInfo, ZIP_DEFLATED

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.collections import PolyCollection
import numpy as np
import trimesh

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "cad/motor-carrier-mount"
CORE = "http://schemas.microsoft.com/3dmanufacturing/core/2015/02"
REL = "http://schemas.openxmlformats.org/package/2006/relationships"
CONTENT = "http://schemas.openxmlformats.org/package/2006/content-types"
PARTS = [("tray", (70, 86)), ("pcb_gauge", (133.25, 86)),
         ("clamp", (133.25, 126))]


def digest(path):
    return sha256(path.read_bytes()).hexdigest()


def xml(root):
    return ET.tostring(root, encoding="utf-8", xml_declaration=True)


def main():
    model = ET.Element("model", {"xmlns": CORE, "unit": "millimeter",
                                "xml:lang": "en-US"})
    ET.SubElement(model, "metadata", {"name": "Title"}).text = "RaceRemote motor holder fit kit 0.1"
    resources = ET.SubElement(model, "resources")
    build = ET.SubElement(model, "build")
    meshes, rows = {}, []
    for oid, (name, xy) in enumerate(PARTS, 1):
        source = OUT / f"{name}_print.stl"
        mesh = trimesh.load_mesh(source)
        assert mesh.is_volume and len(mesh.split()) == 1, name
        assert np.isfinite(mesh.vertices).all(), name
        # Translation only: keep dimensions, move numerical STL floor to exactly Z=0.
        translation = np.array([*xy, 0.0]) - mesh.bounds[0]
        mesh.apply_translation(translation)
        assert (mesh.bounds[0] >= 0).all()
        assert (mesh.bounds[1] <= [220, 215, 250]).all()  # reserve rear 5 mm too
        meshes[name] = mesh
        obj = ET.SubElement(resources, "object", {"id": str(oid), "name": name, "type": "model"})
        m = ET.SubElement(obj, "mesh")
        vertices = ET.SubElement(m, "vertices")
        for vertex in mesh.vertices:
            ET.SubElement(vertices, "vertex", dict(zip("xyz", (format(v, ".12g") for v in vertex))))
        triangles = ET.SubElement(m, "triangles")
        for face in mesh.faces:
            ET.SubElement(triangles, "triangle", dict(zip(("v1", "v2", "v3"), map(str, face))))
        ET.SubElement(build, "item", {"objectid": str(oid)})
        rows.append({"name": name, "source": source.relative_to(ROOT).as_posix(),
                     "sha256": digest(source), "translation_mm": translation.tolist(),
                     "bounds_mm": mesh.bounds.tolist(), "size_mm": mesh.extents.tolist(),
                     "volume_mm3": float(mesh.volume), "triangles": len(mesh.faces)})

    gaps = []
    for (a, ma), (b, mb) in combinations(meshes.items(), 2):
        separation = np.maximum(np.maximum(ma.bounds[0, :2] - mb.bounds[1, :2],
                                           mb.bounds[0, :2] - ma.bounds[1, :2]), 0)
        gap = float(np.linalg.norm(separation))
        assert gap >= 9.99999, (a, b, gap)
        gaps.append({"parts": [a, b], "xy_bounding_box_gap_mm": gap})

    types = ET.Element("Types", {"xmlns": CONTENT})
    for ext, mime in [("rels", "application/vnd.openxmlformats-package.relationships+xml"),
                      ("model", "application/vnd.ms-package.3dmanufacturing-3dmodel+xml")]:
        ET.SubElement(types, "Default", {"Extension": ext, "ContentType": mime})
    rels = ET.Element("Relationships", {"xmlns": REL})
    ET.SubElement(rels, "Relationship", {"Id": "rel0", "Target": "/3D/3dmodel.model",
                  "Type": "http://schemas.microsoft.com/3dmanufacturing/2013/01/3dmodel"})
    path = OUT / "print-kit.3mf"
    with ZipFile(path, "w") as archive:
        for name, payload in [("[Content_Types].xml", xml(types)),
                              ("_rels/.rels", xml(rels)), ("3D/3dmodel.model", xml(model))]:
            info = ZipInfo(name, date_time=(2026, 9, 15, 0, 0, 0))
            info.compress_type = ZIP_DEFLATED
            archive.writestr(info, payload)

    # Independent consumer validates scale, triangle topology and placed geometry.
    loaded = trimesh.load_scene(path)
    copies = loaded.to_geometry().split()
    assert len(copies) == 3
    unmatched = list(copies)
    for name, mesh in meshes.items():
        matches = [copy for copy in unmatched if np.allclose(copy.bounds, mesh.bounds, atol=1e-7, rtol=0)]
        assert len(matches) == 1, name
        copy = matches[0]
        assert copy.is_volume and len(copy.faces) == len(mesh.faces), name
        assert np.isclose(copy.volume, mesh.volume, rtol=1e-9, atol=1e-7), name
        unmatched.remove(copy)

    fig, ax = plt.subplots(figsize=(8, 7))
    for (name, mesh), color in zip(meshes.items(), ["#2b81b5", "#edaa31", "#4aa982"]):
        faces = mesh.triangles[mesh.face_normals[:, 2] > 0.01]
        faces = faces[np.argsort(faces[:, :, 2].mean(axis=1))]
        ax.add_collection(PolyCollection(faces[:, :, :2], facecolors=color,
                                        edgecolors="#263b49", linewidths=0.15))
        bounds = mesh.bounds
        ax.text(bounds[:, 0].mean(), bounds[1, 1] + 3, name, ha="center", fontsize=10)
    ax.set(xlim=(60, 160), ylim=(76, 148), aspect="equal", xlabel="X (mm)", ylabel="Y (mm)",
           title="RaceRemote: motor holder fit kit / top view\n3 separate parts, scale 1:1, Z=0; no G-code")
    ax.grid(alpha=0.2)
    fig.tight_layout()
    preview = OUT / "print-kit.png"
    fig.savefig(preview, dpi=150)
    plt.close(fig)
    report = {"contract": "MOTOR-PRINT-KIT-01", "version": "0.1", "date": "2026-09-15",
              "units": "mm", "printer_reference": "Creality K1C 220x220x250; rear 5 mm reserved",
              "parts": rows, "xy_gaps": gaps,
              "checks": {"source_closed_single_solids": True, "bed_bounds": True,
                         "trimesh_3mf_roundtrip_bounds_topology_volume": True},
              "slicer_import_verified": False, "sliced": False, "printed": False,
              "limitations": ["No material/nozzle/process profile embedded", "Not sequential-print clearance",
                              "No brim/support/toolpath validation", "No physical fit or retention validation"],
              "artifacts": {str(p.relative_to(ROOT).as_posix()): digest(p) for p in [path, preview]},
              "generator_sha256": digest(Path(__file__)), "trimesh_version": trimesh.__version__}
    target = ROOT / "docs/evidence/motor-print-kit-v01.json"
    target.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps({"three_parts": len(copies), "checks": report["checks"],
                      "3mf": str(path), "gaps": gaps}))


if __name__ == "__main__":
    main()
