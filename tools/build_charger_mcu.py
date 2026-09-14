"""Generate the candidate MCU interface, not a complete charger or PCB."""
import csv
import json
from pathlib import Path
import re
import uuid
from build_charge_permit import ic, q
from build_usb_port_schematic import block

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'hardware/charger-mcu'
LIB = 'RaceRemote_ChargerMCU'


def uid(value):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'https://vea.raceremote/charger-mcu/' + str(value)))


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    with (ROOT / 'docs/charger-mcu-pins.csv').open(encoding='utf-8-sig') as f:
        rows = list(csv.DictReader(f))
    source = (ROOT / 'hardware/charge-core/RaceRemote_Charge.kicad_sym').read_text(encoding='utf-8')
    symbols = {n: block(source, source.index('(symbol "' + n + '"'))
               for n in ['R', 'C', 'Conn_01x02', 'Conn_01x04', 'PWR_FLAG']}
    pins = [(int(r['pin']), r['port'], 'power_in' if r['mode'] == 'power' else 'bidirectional') for r in rows]
    symbols['CH32V003F4P6'] = ic('CH32V003F4P6', pins[:10], pins[10:]).replace('(end 17.78 -17.78)', '(end 17.78 -76.2)')
    symbols['SN74LVC3G14DCT'] = ic('SN74LVC3G14DCT',
        [(1, '1A', 'input'), (3, '2A', 'input'), (6, '3A', 'input'), (4, 'GND', 'power_in')],
        [(7, '~{1A}', 'output'), (5, '~{2A}', 'output'), (2, '~{3A}', 'output'), (8, 'VCC', 'power_in')])
    (DEST / (LIB + '.kicad_sym')).write_text('(kicad_symbol_lib (version 20251024) (generator "raceremote")\n'
        + '\n'.join(symbols.values()) + ')\n', encoding='utf-8')
    cache = [s.replace('(symbol "' + n + '"', '(symbol "' + LIB + ':' + n + '"', 1) for n, s in symbols.items()]
    out = ['(kicad_sch (version 20250901) (generator "raceremote")',
           f'(uuid "{uid("sheet")}") (paper "A3")',
           '(title_block (title "RaceRemote - charger MCU interface PROPOSAL") '
           '(date "2026-09-14") (rev "0.2") (company "vea.raceremote"))',
           '(lib_symbols ' + '\n'.join(cache) + ')']
    positions, bom = {}, []

    def prop(name, value, x, y, hide=False):
        return f'(property {q(name)} {q(value)} (at {x:g} {y:g} 0) ' + ('(hide yes) ' if hide else '') + \
               '(effects (font (size 1.27 1.27))))'

    def symbol(name, ref, value, x, y, purpose='', datasheet=''):
        pp = {}
        for m in re.finditer(r'\(pin (\S+).*?\(number "([^"]+)"', symbols[name], re.S):
            px, py, _ = map(float, re.search(r'\(at ([^)]+)', m[0])[1].split())
            pp[m[2]] = (round(x + px, 4), round(y - py, 4))
        positions[ref] = pp
        flag, passive, chip = name == 'PWR_FLAG', name in ['R', 'C'], ref.startswith('U')
        out.append(f'(symbol (lib_id "{LIB}:{name}") (at {x:g} {y:g} 0) (unit 1) '
                   f'(in_bom {"no" if flag else "yes"}) (on_board {"no" if flag else "yes"}) '
                   f'(dnp no) (uuid "{uid(ref)}") ' + prop('Reference', ref, x, y - (30.48 if chip else 5.08), flag)
                   + prop('Value', value, x + (12.7 if passive else 0), y if passive else y - (33.02 if chip else 7.62), flag)
                   + prop('Footprint', '', x, y, True) + prop('Datasheet', datasheet, x, y, True)
                   + ''.join(f'(pin "{n}" (uuid "{uid(ref + "/" + n)}"))' for n in pp)
                   + f'(instances (project "charger-mcu" (path "/{uid("sheet")}" (reference "{ref}") (unit 1)))))')
        if not flag:
            bom.append({'reference': ref, 'value': value, 'purpose': purpose, 'footprint': '',
                        'selection': 'Candidate; firmware, PCB and electrical qualification pending'})

    def net(ref, pin, name, dx=7.62, dy=0):
        a = positions[ref][str(pin)]
        b = (round(a[0] + dx, 4), round(a[1] + dy, 4))
        out.append(f'(wire (pts (xy {a[0]:g} {a[1]:g}) (xy {b[0]:g} {b[1]:g})) '
                   f'(stroke (width 0) (type default)) (uuid "{uid((ref, pin, "wire"))}"))')
        out.append(f'(global_label {q(name)} (shape passive) (at {b[0]:g} {b[1]:g} {180 if dx < 0 else 0}) '
                   '(effects (font (size 1 1)) (justify ' + ('right' if dx < 0 else 'left') + ')) '
                   f'(uuid "{uid((ref, pin, "net"))}"))')

    def pair(name, ref, value, x, y, top, bottom, purpose):
        symbol(name, ref, value, x, y, purpose)
        net(ref, 1, top, 0, -7.62)
        net(ref, 2, bottom, 0, 7.62)

    def note(value, x, y, size=1.27):
        out.append(f'(text {q(value)} (at {x:g} {y:g} 0) (effects (font (size {size} {size})) '
                   f'(justify left top)) (uuid "{uid(value)}"))')

    symbol('CH32V003F4P6', 'U601', 'CH32V003F4P6', 101.6, 76.2,
           'USB source/charge MCU; GPIO names from allocation, no firmware yet',
           'https://www.wch-ic.com/downloads/CH32V003DS0_PDF.html')
    for r in rows: net('U601', r['pin'], r['proposed_signal'], -7.62 if int(r['pin']) <= 10 else 7.62)
    symbol('SN74LVC3G14DCT', 'U602', 'SN74LVC3G14DCTR', 269.24, 76.2,
           'Three Schmitt inverters: released MCU pins default all requests LOW', 'https://www.ti.com/lit/gpn/sn74lvc3g14')
    for p, n in [(1, 'ARM_N'), (3, 'WD_HEARTBEAT_N'), (6, 'SOURCE_REQUEST_N'), (4, 'USB_GND')]: net('U602', p, n, -7.62)
    for p, n in [(7, 'ARM'), (5, 'WD_HEARTBEAT'), (2, 'SOURCE_REQUEST'), (8, 'AUX_3V3')]: net('U602', p, n)
    for ref, x, n in [('R601', 228.6, 'ARM_N'), ('R602', 284.48, 'WD_HEARTBEAT_N'), ('R603', 340.36, 'SOURCE_REQUEST_N')]:
        pair('R', ref, '10k / 1%', x, 134.62, 'AUX_3V3', n, 'Pull-up at inverter input; MCU open drain only')
    for ref, x, val in [('C601', 38.1, '100n / 16V'), ('C602', 76.2, '1u / 16V'), ('C603', 114.3, '100n / 16V')]:
        pair('C', ref, val, x, 190.5, 'AUX_3V3', 'USB_GND', 'Local AUX bypass')
    pair('R', 'R604', '10k / 1%', 165.1, 190.5, 'AUX_3V3', 'RESET_N', 'Debug reset pull-up; NRST option bytes still required')
    for ref, x, y, value, ns, purpose in [
        ('J601', 50.8, 241.3, 'FROM USB AUX', ['AUX_3V3', 'USB_GND'], 'Only U202 AUX source; never parallel programmer supply'),
        ('J602', 127, 241.3, 'TO PERMIT J403', ['ARM', 'FAULT_BUS_N', 'PERMIT_Q', 'USB_GND'], 'Requires permit v0.4 R402=47k'),
        ('J603', 203.2, 241.3, 'TO WATCHDOG J502', ['WD_HEARTBEAT', 'USB_GND'], 'No watchdog disable connection'),
        ('J604', 279.4, 241.3, 'SOURCE POLICY', ['AUX_3V3', 'SOURCE_REQUEST', 'INPUT_FAULT_N', 'USB_GND'], 'Request only; CC/brownout policy before SOURCE_ALLOW still required'),
        ('J605', 355.6, 241.3, 'TO BC J303', ['BC_SCL', 'BC_SDA', 'BC_INT_N', 'BC_EN_N'], 'No extra I2C or INT pull-ups'),
        ('J606', 228.6, 190.5, 'CC RESERVE', ['CC_OUT1', 'CC_OUT2', 'USB_GND', 'USB_GND'], 'No raw CC pins; source detector interface pending'),
        ('J607', 304.8, 190.5, 'DEBUG LOGICAL', ['SWIO', 'RESET_N', 'AUX_3V3', 'USB_GND'], 'Not WCH-LinkE connector pinout; external-target power mode unverified'),
        ('J608', 381, 190.5, 'DIAGNOSTIC RESERVE', ['CHARGER_FAULT', 'AUX_DIAGNOSTIC', 'VBUS_SENSE', 'CELL1_SENSE'], 'No raw battery or VBUS; divider/isolation not implemented')]:
        symbol('Conn_01x02' if len(ns) == 2 else 'Conn_01x04', ref, value, x, y, purpose)
        for i, n in enumerate(ns): net(ref, i + 1, n, -7.62)
    for ref, n, x in [('#FLG601', 'AUX_3V3', 350.52), ('#FLG602', 'USB_GND', 386.08)]:
        symbol('PWR_FLAG', ref, 'PWR_FLAG', x, 35.56)
        net(ref, 1, n, 0, 5.08)
    note('CHARGE-MCU-01 v0.2 / CHARGE-IO-01 v0.1 - interface proposal; no MCU firmware or PCB', 12.7, 12.7, 1.5)
    note('ARM_N / WD_HEARTBEAT_N / SOURCE_REQUEST_N: open drain. Release = output LOW.\n'
         'FAULT_BUS_N: open drain + input readback; never drive HIGH. No INA300 clear/disable GPIO.', 12.7, 22.86, 1)
    note('AUX 3.0..3.6V. Settled logic only: startup, brownout, GPIO mode/edges, load and charge current unverified.\n'
         'J604 is NOT direct SOURCE_ALLOW. J607/J608 are logical interfaces, not measured connector pinouts.', 12.7, 264.16, 1)
    out.append('(sheet_instances (path "/" (page "1"))) (embedded_fonts no))')
    (DEST / 'charger-mcu.kicad_sch').write_text('\n'.join(out) + '\n', encoding='utf-8')
    (DEST / 'charger-mcu.kicad_pro').write_text(json.dumps({'meta': {'filename': 'charger-mcu.kicad_pro', 'version': 3}}, indent=2) + '\n')
    (DEST / 'sym-lib-table').write_text(f'(sym_lib_table (version 7) (lib (name "{LIB}") (type "KiCad") '
        f'(uri "${{KIPRJMOD}}/{LIB}.kicad_sym") (options "") (descr "Own WCH/TI symbols and attributed passives")))\n')
    (DEST / 'fp-lib-table').write_text('(fp_lib_table (version 7))\n')
    with (DEST / 'bom.csv').open('w', encoding='utf-8-sig', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=list(bom[0])); writer.writeheader(); writer.writerows(bom)
    print('Generated', len(bom), 'components')


if __name__ == '__main__': main()
