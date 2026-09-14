"""Generate the logical permit latch candidate; no charger power actuator."""
import csv
import json
from pathlib import Path
import re
import uuid
from build_usb_port_schematic import block

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'hardware/charge-permit'
LIB = 'RaceRemote_Permit'


def uid(value):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'https://vea.raceremote/charge-permit/' + str(value)))


def q(value):
    return json.dumps(str(value), ensure_ascii=False)


def ic(name, left, right):
    out = [f'(symbol "{name}" (pin_names (offset 1.016)) (in_bom yes) (on_board yes)',
           '(property "Reference" "U" (at 0 27.94 0) (effects (font (size 1.27 1.27))))',
           f'(property "Value" "{name}" (at 0 30.48 0) (effects (font (size 1.27 1.27))))',
           '(property "Footprint" "" (at 0 0 0) (effects (font (size 1.27 1.27)) hide))',
           f'(symbol "{name}_0_1" (rectangle (start -17.78 25.4) (end 17.78 -17.78) '
           '(stroke (width 0.254) (type default)) (fill (type background))))',
           f'(symbol "{name}_1_1"']
    for side, pins in [(-1, left), (1, right)]:
        for row, (number, label, typ) in enumerate(pins):
            x, y, angle = side * 25.4, 20.32 - row * 10.16, 0 if side < 0 else 180
            out.append(f'(pin {typ} line (at {x:g} {y:g} {angle}) (length 7.62) '
                       f'(name {q(label)} (effects (font (size 1.27 1.27)))) '
                       f'(number "{number}" (effects (font (size 1.27 1.27)))))')
    return '\n'.join(out) + '))'


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    source = (ROOT / 'hardware/charge-core/RaceRemote_Charge.kicad_sym').read_text(encoding='utf-8')
    symbols = {n: block(source, source.index('(symbol "' + n + '"'))
               for n in ['R', 'C', 'Conn_01x02', 'Conn_01x04', 'PWR_FLAG']}
    # Package-specific pin numbers, NOT schematic drawing order.
    symbols['SN74LVC1G74DCT'] = ic('SN74LVC1G74DCT',
        [(1, 'CLK', 'input'), (2, 'D', 'input'), (6, '~{CLR}', 'input'), (7, '~{PRE}', 'input')],
        [(5, 'Q', 'output'), (3, '~{Q}', 'output'), (8, 'VCC', 'power_in'), (4, 'GND', 'power_in')])
    symbols['SN74LVC2G17DCK'] = ic('SN74LVC2G17DCK',
        [(1, '1A', 'input'), (3, '2A', 'input'), (2, 'GND', 'power_in')],
        [(6, '1Y', 'output'), (4, '2Y', 'output'), (5, 'VCC', 'power_in')])
    symbols['TPS3808G33DBV'] = ic('TPS3808G33DBV',
        [(5, 'SENSE', 'input'), (3, '~{MR}', 'input'), (2, 'GND', 'power_in')],
        [(1, '~{RESET}', 'open_collector'), (4, 'CT', 'input'), (6, 'VDD', 'power_in')])
    (DEST / (LIB + '.kicad_sym')).write_text('(kicad_symbol_lib (version 20251024) (generator "raceremote")\n'
        + '\n'.join(symbols.values()) + ')\n', encoding='utf-8')
    cache = [s.replace('(symbol "' + n + '"', '(symbol "' + LIB + ':' + n + '"', 1)
             for n, s in symbols.items()]
    out = ['(kicad_sch (version 20250901) (generator "raceremote")',
           f'(uuid "{uid("sheet")}") (paper "A3")',
           '(title_block (title "RaceRemote - hardware permit latch PROPOSAL") '
           '(date "2026-09-14") (rev "0.1") (company "vea.raceremote"))',
           '(lib_symbols ' + '\n'.join(cache) + ')']
    positions, bom = {}, []

    def prop(name, value, x, y, hide=False):
        return f'(property {q(name)} {q(value)} (at {x:g} {y:g} 0) ' + ('(hide yes) ' if hide else '') + \
               '(effects (font (size 1.27 1.27))))'

    def symbol(name, ref, value, x, y, purpose='', datasheet=''):
        pins = {}
        for m in re.finditer(r'\(pin (\S+).*?\(number "([^"]+)"', symbols[name], re.S):
            px, py, _ = map(float, re.search(r'\(at ([^)]+)', m[0])[1].split())
            pins[m[2]] = (round(x + px, 4), round(y - py, 4))
        positions[ref] = pins
        flag, passive, chip = name == 'PWR_FLAG', name in ['R', 'C'], ref.startswith('U')
        out.append(f'(symbol (lib_id "{LIB}:{name}") (at {x:g} {y:g} 0) (unit 1) '
                   f'(in_bom {"no" if flag else "yes"}) (on_board {"no" if flag else "yes"}) '
                   f'(dnp no) (uuid "{uid(ref)}") ' + prop('Reference', ref, x, y - (30.48 if chip else 5.08), flag)
                   + prop('Value', value, x + (12.7 if passive else 0), y if passive else y - (33.02 if chip else 7.62), flag)
                   + prop('Footprint', '', x, y, True) + prop('Datasheet', datasheet, x, y, True)
                   + ''.join(f'(pin "{n}" (uuid "{uid(ref + "/" + n)}"))' for n in pins)
                   + f'(instances (project "charge-permit" (path "/{uid("sheet")}" (reference "{ref}") (unit 1)))))')
        if not flag:
            bom.append({'reference': ref, 'value': value, 'purpose': purpose, 'footprint': '',
                        'selection': 'Candidate; footprints, actuator and physical qualification pending'})

    def net(ref, pin, name, dx=12.7, dy=0):
        a = positions[ref][str(pin)]
        b = (round(a[0] + dx, 4), round(a[1] + dy, 4))
        out.append(f'(wire (pts (xy {a[0]:g} {a[1]:g}) (xy {b[0]:g} {b[1]:g})) '
                   f'(stroke (width 0) (type default)) (uuid "{uid((ref, pin, "wire"))}"))')
        out.append(f'(global_label {q(name)} (shape passive) (at {b[0]:g} {b[1]:g} {180 if dx < 0 else 0}) '
                   f'(effects (font (size 1 1)) (justify {"right" if dx < 0 else "left"})) '
                   f'(uuid "{uid((ref, pin, "net"))}"))')

    def pair(name, ref, value, x, y, top, bottom, purpose):
        symbol(name, ref, value, x, y, purpose)
        net(ref, 1, top, 0, -7.62)
        net(ref, 2, bottom, 0, 7.62)

    def nc(ref, pin):
        x, y = positions[ref][str(pin)]
        out.append(f'(no_connect (at {x:g} {y:g}) (uuid "{uid((ref, pin, "NC"))}"))')

    def note(value, x, y, size=1.27):
        out.append(f'(text {q(value)} (at {x:g} {y:g} 0) (effects (font (size {size} {size})) '
                   f'(justify left top)) (uuid "{uid(value)}"))')

    symbol('TPS3808G33DBV', 'U403', 'TPS3808G33DBVR', 76.2, 88.9,
           'AUX supervisor, CT open: nominal 20ms reset release', 'https://www.ti.com/lit/ds/symlink/tps3808.pdf')
    symbol('SN74LVC2G17DCK', 'U402', 'SN74LVC2G17DCKR', 203.2, 88.9,
           'Schmitt buffers for open-drain fault bus and ARM', 'https://www.ti.com/lit/gpn/sn74lvc2g17')
    symbol('SN74LVC1G74DCT', 'U401', 'SN74LVC1G74DCTR', 330.2, 88.9,
           'Positive-edge ARM, asynchronous fault clear; Q pin5 is permit', 'https://www.ti.com/lit/gpn/sn74lvc1g74')
    for ref, left, right in [
        ('U403', [(5, 'AUX_3V3'), (3, 'AUX_3V3'), (2, 'USB_GND')], [(1, 'FAULT_BUS_N'), (6, 'AUX_3V3')]),
        ('U402', [(1, 'FAULT_BUS_N'), (3, 'ARM'), (2, 'USB_GND')], [(6, 'CLR_N'), (4, 'CLK'), (5, 'AUX_3V3')]),
        ('U401', [(1, 'CLK'), (2, 'AUX_3V3'), (6, 'CLR_N'), (7, 'AUX_3V3')],
         [(5, 'PERMIT_Q'), (8, 'AUX_3V3'), (4, 'USB_GND')])]:
        for pin, name in left:
            net(ref, pin, name, -7.62)
        for pin, name in right:
            net(ref, pin, name, 7.62)
    nc('U403', 4)
    nc('U401', 3)
    for ref, x in [('C403', 76.2), ('C402', 203.2), ('C401', 330.2)]:
        pair('C', ref, '100n / 16V', x, 144.78, 'AUX_3V3', 'USB_GND', 'Local IC bypass; place at corresponding IC')
    for ref, value, x, top, bottom, purpose in [
        ('R401', '10k / 1%', 76.2, 'AUX_3V3', 'FAULT_BUS_N', 'Only pull-up for shared open-drain error bus'),
        ('R402', '10k / 1%', 203.2, 'ARM', 'USB_GND', 'Default ARM LOW when MCU input floats'),
        ('R403', '47k / 1%', 330.2, 'PERMIT_Q', 'USB_GND', 'Pull-down at VCC=0 only; not brownout proof')]:
        pair('R', ref, value, x, 195.58, top, bottom, purpose)
    for ref, name, value, x, nets, purpose in [
        ('J401', 'Conn_01x02', 'FROM AUX', 63.5, ['AUX_3V3', 'USB_GND'], 'Single source TPS709; no battery/raw USB'),
        ('J402', 'Conn_01x02', 'FAULT SOURCES', 177.8, ['FAULT_BUS_N', 'USB_GND'], 'External watchdog and OC: open drain only'),
        ('J403', 'Conn_01x04', 'MCU INTERFACE', 279.4, ['ARM', 'FAULT_BUS_N', 'PERMIT_Q', 'USB_GND'], 'ARM edge, open-drain revoke/readback, Q observation'),
        ('J404', 'Conn_01x02', 'TO FUTURE GATE', 381, ['PERMIT_Q', 'USB_GND'], 'Logical output only; no direct CD/CE or SOURCE_ALLOW')]:
        symbol(name, ref, value, x, 246.38, purpose)
        for i, name in enumerate(nets):
            net(ref, i + 1, name, -7.62)
    for i, (name, x) in enumerate([('AUX_3V3', 165.1), ('USB_GND', 292.1)]):
        ref = '#FLG' + str(401 + i)
        symbol('PWR_FLAG', ref, 'PWR_FLAG', x, 119.38)
        net(ref, 1, name, 0, 5.08)
    note('CHARGE-PERMIT-01 v0.1 - logical latch candidate, NOT a complete charger shutdown', 25.4, 17.78, 2)
    note('FAULT_BUS_N LOW clears Q even with ARM HIGH. Releasing fault alone does not re-arm.\n'
         'D and PRE tied HIGH. Only a new ARM rising edge after reset recovery may set Q. Q-bar intentionally unused.\n'
         'External fault sources / MCU revoke must sink or release; never drive shared bus HIGH.', 25.4, 27.94)
    note('AUX 3.0..3.6V proposal; supervisor falling nominal 3.07V. Below IC valid rails, outputs are NOT guaranteed.\n'
         'No MCU/watchdog/INA300/charger actuator here. PCB, timing, transient and deep-brownout qualification are open.', 25.4, 274.32, 1)
    out.append('(sheet_instances (path "/" (page "1"))) (embedded_fonts no))')
    (DEST / 'charge-permit.kicad_sch').write_text('\n'.join(out) + '\n', encoding='utf-8')
    (DEST / 'charge-permit.kicad_pro').write_text(json.dumps({'meta': {'filename': 'charge-permit.kicad_pro', 'version': 3}}, indent=2) + '\n')
    (DEST / 'sym-lib-table').write_text(f'(sym_lib_table (version 7) (lib (name "{LIB}") (type "KiCad") '
        f'(uri "${{KIPRJMOD}}/{LIB}.kicad_sym") (options "") (descr "Own TI pin symbols and attributed passives")))\n')
    (DEST / 'fp-lib-table').write_text('(fp_lib_table (version 7))\n')
    with (DEST / 'bom.csv').open('w', encoding='utf-8-sig', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=list(bom[0]))
        writer.writeheader()
        writer.writerows(bom)
    print('Generated', len(bom), 'components')


if __name__ == '__main__':
    main()
