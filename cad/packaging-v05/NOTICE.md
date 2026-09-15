# Sources and changes — packaging 0.5

Retrieved/reviewed 2026-09-15. Source models identify reference geometry, not
the exact ordered supplier revision or a physical fit guarantee.

## SG90

- Source: [FreeCAD/FreeCAD-library, Electrical Parts/Servos/SG-90](https://github.com/FreeCAD/FreeCAD-library/tree/6f071a4bab37fff7784092548e6dbff21499b751/Electrical%20Parts/Servos/SG-90).
- Pinned commit: `6f071a4bab37fff7784092548e6dbff21499b751`; file `Servo-sg90.step`.
- Attribution: FreeCAD Parts Library contributors; source-path commit contributed by WladIMirG.
  The STEP header identifies FreeCAD/OpenCascade and 2015-08-26; no original author identity is inferred from this.
- [Library asset license](https://github.com/FreeCAD/FreeCAD-library/blob/master/LICENSE-Assets):
  Creative Commons Attribution 3.0; full retrieved text in [LICENSE-FreeCAD-Assets](LICENSE-FreeCAD-Assets).
  Ownership remains with the original authors; no endorsement is implied.
- Original bytes retained as `sg90-library.step`; SHA256
  `9ad7b12887580bf68135e7bf544f0fc231ab9af988508184c097b1d1c3e7d734`.
- Changes: tessellation and rigid rotation/translation only, recorded in the JSON report.
  The native 11.8 mm thickness differs from the seller's 12.2 mm dimension.
  The imported asset does not prove that the purchased servo is a genuine TowerPro unit.

## Waveshare

- [Manufacturer product](https://www.waveshare.com/product/dc5-36-to-dc3v3-5.htm),
  [dimension drawing](https://www.waveshare.com/img/devkit/accBoard/DC5-36-TO-DC3V3-5/DC5-36-TO-DC3V3-5-size.jpg),
  [component photograph](https://www.waveshare.com/img/devkit/accBoard/DC5-36-TO-DC3V3-5/DC5-36-TO-DC3V3-5-details-1.jpg).
- No manufacturer STEP was found. `layout.scad` is a new dimensional approximation:
  PCB 33×16 mm, input pitch 3.5 mm, output pitch 2.54 mm; low assembly height 5.7 mm.
  PCB thickness, hole diameters, individual packages, -M terminal heights and mating
  connector are estimates and must be updated from the received module.

## Existing project sources

- XIAO: unchanged [Seeed STEP](../components/xiao-sense-2023.step),
  [manufacturer archive](https://files.seeedstudio.com/wiki/SeeedStudio-XIAO-ESP32S3/res/seeed-studio-xiao-esp32s3-sense-3d_model.zip).
  See [candidate-fit.md](../candidate-fit.md) for import transform and source limits.
  No new license or confirmed OV3660 revision is inferred.
- F130: [dimension drawing](https://static.chipdip.ru/lib/799/DOC059799826.pdf),
  existing size-derived motor mesh; neither photograph nor detailed vendor STEP.
- zcar: alexyu132, commit `4b714e63fa30ed2030a8a48ab15f1e4a5fa380c4`, GPL-3.0.
  [Original source, editable SketchUp and license](../reference/NOTICE.md).
  `mechanics-without-frame.stl` removes the original frame and reference actuator
  bodies from the display; `frame-relief-proposal.stl` subtracts four local pockets.
  `frame-removed-material.stl` and `servo-frame-interference.stl` expose these differences.
  The derived geometry retains the upstream license.
- USB connector/PCB and printed mounts: project sources `cad/usb-port-study.scad`,
  `cad/components/usb-port-pcb.scad`, `hardware/usb-port`; connector dimensions
  documented in [USB study](../usb-port-study.md). The passive port is not the charger.
- `inputs/` preserves the named prior-stage STL exports for repeatable checks;
  `tools/build_packaging_v05.py` and all local sources remain editable.
