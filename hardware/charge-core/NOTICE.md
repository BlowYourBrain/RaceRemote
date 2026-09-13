# KiCad library provenance

The project-local library contains unmodified symbol definitions selected from
the official signed KiCad 10.0.6 Windows distribution on 2026-09-14:

- `Battery_Management:BQ25886RGE`;
- `Device:R`, `Device:C`, `Device:L`;
- `Connector_Generic:Conn_01x02`, `Connector_Generic:Conn_01x04`;
- `power:PWR_FLAG`.

The symbols are embedded in the schematic with the local `RaceRemote_Charge`
library prefix. References, instance values and wiring are project design work.
The inductor-side power flag describes supply through L101 for ERC; no pin types
were changed to make checks pass. Unassigned passive/interface footprints remain
open design work, not an approved manufacturable PCB.

`Charge_Core.pretty/Texas_RGE0024H_VQFN-24-1EP_4x4mm_P0.5mm_EP2.7x2.7mm.kicad_mod`
is the unmodified footprint from the same distribution. `Package_DFN_QFN` in the
project footprint table points to this one-file subset, not a complete library.

Library terms: **CC-BY-SA 4.0 with the KiCad design exception**.
[Bundled license text](../usb-port/RaceRemote_USB.pretty/LICENSE.md),
[official KiCad library license](https://www.kicad.org/libraries/license/).
Library assets are separate from the zcar GPL meshes.
