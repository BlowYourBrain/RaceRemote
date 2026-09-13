"""Inspect a pinned upstream checkout without repairing or modifying its meshes.

Run with: uv run --with trimesh==5.1.0 --with numpy==2.4.6
    --with networkx==3.6.1 python tools/inspect_zcar.py build/upstream/zcar
    --output docs/evidence/zcar-mesh-audit.json
"""

import argparse
import hashlib
import json
from pathlib import Path
import subprocess

import trimesh


REVISION = "4b714e63fa30ed2030a8a48ab15f1e4a5fa380c4"
FILES = ("stl/zcar.stl", "stl/tray1.stl", "stl/tray2.stl")


def inspect(root):
    revision = subprocess.check_output(
        ["git", "-C", str(root), "rev-parse", "HEAD"], text=True
    ).strip()
    if revision != REVISION:
        raise ValueError(f"Expected {REVISION}, got {revision}")
    changes = subprocess.check_output(
        ["git", "-C", str(root), "status", "--porcelain", "--", *FILES], text=True
    )
    if changes.strip():
        raise ValueError("Source meshes have local changes")
    result = {
        "source": "https://github.com/alexyu132/zcar",
        "revision": revision,
        "trimesh_version": trimesh.__version__,
        "units": "mm inferred from upstream dimensions; STL has no unit metadata",
        "method": "Default load processing merges vertices; split repair disabled",
        "scope": "Mesh topology and bounds only; no battery fit or print validation",
        "files": {},
    }
    for name in FILES:
        path = root / name
        mesh = trimesh.load_mesh(path)
        parts = mesh.split(only_watertight=False, repair=False)
        result["files"][name] = {
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "watertight": bool(mesh.is_watertight),
            "extents": mesh.extents.round(4).tolist(),
            "component_count": len(parts),
            "watertight_components": sum(bool(p.is_watertight) for p in parts),
            "components": [
                {"index": i, "extents": p.extents.round(4).tolist(),
                 "watertight": bool(p.is_watertight)}
                for i, p in enumerate(parts)
            ] if name != "stl/zcar.stl" else [],
        }
    return result


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("checkout", type=Path)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    report = inspect(args.checkout)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {args.output}")
