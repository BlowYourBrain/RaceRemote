# Symbol provenance and adaptations

`RaceRemote_USB.kicad_sym` contains selected KiCad community symbols from the
official signed KiCad 10.0.6 Windows distribution, extracted on 2026-09-14:

- `Connector:USB_C_Receptacle_USB2.0_16P` → `USB_C_GCT_16P`. Only the shield
  pin number changes from `SH` to `S1` to match the GCT USB4105 footprint.
  USB pin positions/types and stacked power pins retain the upstream definition.
- `Device:R` → `R`, including the original passive pin definitions.
- `Connector:Conn_01x01_Pin` → `WirePad`, used as a single soldered wire terminal.
- `Mechanical:MountingHole` → `MountingHole`, without electrical pins.

Symbol identifiers and footprint filters are adapted to the project library.
Instance values, references, footprint assignments and wiring belong to this
project. The schematic generator embeds the same definitions as the local library.

These library adaptations are distributed under **CC-BY-SA 4.0 with the KiCad
design exception**: [license text](RaceRemote_USB.pretty/LICENSE.md),
[official library license](https://www.kicad.org/libraries/license/).
They are separate from the zcar meshes.

`WirePad_D0.8mm.kicad_mod` and `MountingHole_2.4mm.kicad_mod` are project-created
footprints extracted from the existing board geometry, not upstream references.
They use the same library license. The PCB and schematic preserve their physical
geometry; the mounting holes are excluded from the component BOM (fasteners still
have to be selected separately).
