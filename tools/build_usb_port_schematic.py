"""Generate the independently connected passive USB-C schematic (KiCad 10).

Uses project-local symbols; never reads PCB nets to construct the schematic.
Run with ordinary Python or KiCad Python. ERC/parity are separate CLI checks.
"""
from pathlib import Path
import re

from usb_port_identity import ROOT_UUID, footprint, uid

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'hardware/usb-port'


def block(text, start):
    depth = 0
    for token in re.finditer(r'"(?:\\.|[^"\\])*"|[()]', text[start:]):
        if token[0] == '(':
            depth += 1
        elif token[0] == ')':
            depth -= 1
        if depth == 0:
            return text[start:start + token.end()]
    raise ValueError('Unclosed symbol')


def main():
    lib = (DEST / 'RaceRemote_USB.kicad_sym').read_text(encoding='utf-8')
    names = ['USB_C_GCT_16P', 'R', 'WirePad', 'MountingHole']
    symbols = {name: block(lib, lib.index('(symbol "' + name + '"')) for name in names}
    cached = [s.replace('(symbol "' + n + '"', '(symbol "RaceRemote_USB:' + n + '"', 1)
              for n, s in symbols.items()]
    out = ['(kicad_sch (version 20250901) (generator "raceremote")',
           f'(uuid "{ROOT_UUID}") (paper "A4")',
           '(title_block (title "RaceRemote - passive USB-C charge port") '
           '(date "2026-09-14") (rev "0.2") (company "vea.raceremote"))',
           '(lib_symbols ' + '\n'.join(cached) + ')']

    def prop(name, value, x, y, hide=False):
        return (f'(property "{name}" "{value}" (at {x:g} {y:g} 0) '
                + ('(hide yes) ' if hide else '')
                + '(effects (font (size 1.27 1.27))))')

    def symbol(name, ref, value, x, y, angle=0):
        # Pin UUIDs are stable but connectivity comes solely from wires below.
        pins = re.findall(r'\(number "([^"]+)"', symbols[name])
        out.append(f'(symbol (lib_id "RaceRemote_USB:{name}") (at {x:g} {y:g} {angle}) '
                   f'(unit 1) (in_bom {"no" if ref.startswith("M") else "yes"}) '
                   f'(on_board yes) (dnp no) (uuid "{uid(ref)}") '
                   + prop('Reference', ref, x, y - (21.59 if ref == 'J1' else 5.08))
                   + prop('Value', value, x + (7.62 if ref.startswith('R') else 0),
                          y + (0 if ref.startswith('R') else -2.54 if ref != 'J1' else -24.13))
                   + prop('Footprint', footprint(ref), x, y, True)
                   + prop('Datasheet', 'https://gct.co/files/drawings/usb4105.pdf' if ref == 'J1' else '', x, y, True)
                   + ''.join(f'(pin "{n}" (uuid "{uid(ref + "/pin/" + n)}"))' for n in pins)
                   + f'(instances (project "usb-port" (path "/{ROOT_UUID}" '
                   f'(reference "{ref}") (unit 1)))))')

    def wire(a, b):
        out.append(f'(wire (pts (xy {a[0]:g} {a[1]:g}) (xy {b[0]:g} {b[1]:g})) '
                   f'(stroke (width 0) (type default)) (uuid "{uid(str((a, b)))}"))')

    def label(name, x, y):
        out.append(f'(global_label "{name}" (shape passive) (at {x:g} {y:g} 0) '
                   f'(effects (font (size 1.27 1.27)) (justify left)) '
                   f'(uuid "{uid(str((name, x, y)))}"))')

    def lead(name, a, b):
        wire(a, b)
        label(name, *b)

    def note(text, x, y, font=1.27):
        escaped = text.replace('\\', '\\\\').replace('"', '\\"').replace('\n', '\\n')
        out.append(f'(text "{escaped}" (at {x:g} {y:g} 0) '
                   f'(effects (font (size {font} {font})) (justify left top)) '
                   f'(uuid "{uid(text)}"))')

    # Official symbol: power pins stacked; signal pins individually exposed.
    # Coordinates are schematic positions, not derived from PCB pad positions.
    symbol('USB_C_GCT_16P', 'J1', 'USB4105-GF-A', 50.8, 76.2)
    for signal, dy in [('VBUS', -15.24), ('CC1', -10.16), ('CC2', -7.62),
                       ('DM', -2.54), ('DM', 0), ('DP', 2.54), ('DP', 5.08)]:
        lead(signal, (66.04, 76.2 + dy), (78.74, 76.2 + dy))
    for x in [43.18, 50.8]:
        lead('GND', (x, 99.06), (x, 105.41))
    for y in [88.9, 91.44]:
        out.append(f'(no_connect (at 66.04 {y}) (uuid "{uid("NC" + str(y))}"))')

    # R1 is rotated 180 degrees to preserve its actual physical pad numbering.
    symbol('R', 'R1', '5.1k 1%', 114.3, 76.2, 180)
    symbol('R', 'R2', '5.1k 1%', 144.78, 76.2)
    for x, cc in [(114.3, 'CC1'), (144.78, 'CC2')]:
        lead(cc, (x, 72.39), (x, 66.04))
        lead('GND', (x, 80.01), (x, 88.9))

    # Same six individually soldered terminals as the assembly contract.
    for i, net in enumerate(['VBUS', 'DP', 'CC1', 'GND', 'DM', 'CC2']):
        y = 53.34 + i * 12.7
        symbol('WirePad', 'H' + str(i + 1), net, 203.2, y)
        lead(net, (208.28, y), (220.98, y))
    for i, x in enumerate([114.3, 144.78]):
        symbol('MountingHole', 'M' + str(i + 1), 'M2 clearance', x, 116.84)

    note('USB-C input', 35.56, 35.56, 2)
    note('Independent sink resistors', 101.6, 35.56, 2)
    note('Harness to separate charger', 190.5, 35.56, 2)
    note('No battery connection here. VBUS target: 5 V from USB source.\n'
         'A8/B8 intentionally NC. SHIELD is S1 on the GCT footprint.\n'
         'CC sense at charger: high impedance, including while unpowered; no extra Rd.\n'
         'DP/DM: source classification only. Available current must be detected.\n'
         'Input protection, current limit, reverse blocking and 2S charger: separate board.', 35.56, 134.62)
    out.append('(sheet_instances (path "/" (page "1"))) (embedded_fonts no))')
    (DEST / 'usb-port.kicad_sch').write_text('\n'.join(out) + '\n', encoding='utf-8')
    (DEST / 'sym-lib-table').write_text(
        '(sym_lib_table (version 7) (lib (name "RaceRemote_USB") (type "KiCad") '
        '(uri "${KIPRJMOD}/RaceRemote_USB.kicad_sym") (options "") '
        '(descr "KiCad symbols adapted for the GCT footprint and wire terminals")))\n', encoding='utf-8')
    print('Generated', DEST / 'usb-port.kicad_sch')


if __name__ == '__main__':
    main()
