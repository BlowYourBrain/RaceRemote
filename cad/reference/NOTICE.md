# zcar reference geometry

The unchanged stl/tray1.stl and stl/tray2.stl from the same pinned checkout are included as upstream-tray1.stl and upstream-tray2.stl. Their closed print solids supply the rigidly aligned parts reference-281 and reference-246 in ../motor-harness. Transformations and input hashes are in ../motor-harness/mechanism-alignment.json and ../../docs/evidence/motor-harness-v01.json. No source shape changes or mesh repair; GPL-3.0 continues to apply to these files and derived geometry.

Source: https://github.com/alexyu132/zcar/tree/4b714e63fa30ed2030a8a48ab15f1e4a5fa380c4

File: stl/zcar.stl; SHA256 943f59bfecd42b7eed32457dfd166559b0aec8fc64f6cd9ba6ce526d3f7c0e5b.

Change: rigid translation by [-35.27530288696289, 31.122983932495117, 6.5] mm only; no mesh repair. Upstream mechanism and gearing retained, not validated against new parts.

The unchanged editable SketchUp source zcar.skp and upstream-README.md are included. SketchUp SHA256: 0af2c260b4c20c5eeaa607024aa871b4f70e0addca65296b3c4a7c3578190cdb.

License: GPL-3.0; see LICENSE. The embedded reference in ../assembly-viewer.html comes from this same source. Rebuild with tools/build_cad_assembly.py.
