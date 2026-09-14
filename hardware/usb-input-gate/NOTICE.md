# Symbol provenance

`TPS25200DRV` is an original project symbol drawn from Texas Instruments
SLVSCJ0F Table 4-1. Package leads 1–6 keep the manufacturer's numbering;
the exposed ground pad is represented as schematic pin 7. No footprint is
assigned. ILIM is a passive programming connection in ERC, not a digital
output or a simulated regulator.

`TPS70933DBV` is also an original project symbol, following TI SBVS186H
Table 5-1, plain TPS709 DBV pinout (not TPS709A/B). EN is explicitly left
unconnected for the enabled mode documented in section 7.4; NC is unconnected.
No physical footprint or regulator simulation model is assigned.

`R`, `C`, `Conn_01x02`, `Conn_01x04`, and `PWR_FLAG` are unchanged subsets of
the official KiCad 10.0.6 Windows distribution, reused from
[`hardware/charge-core`](../charge-core/NOTICE.md).
Terms: CC-BY-SA 4.0 with the KiCad design exception;
[license text](../usb-port/RaceRemote_USB.pretty/LICENSE.md).
Schematic wiring, generation, calculations and verification are project work.
