"""Generate an INA300 latched overcurrent interface; not a finished charger or PCB."""
import csv
import json
from pathlib import Path
import re
import uuid
from build_charge_permit import ic, q
from build_usb_port_schematic import block

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'hardware/charge-overcurrent'
LIB = 'RaceRemote_Overcurrent'


def uid(value):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'https://vea.raceremote/charge-overcurrent/' + str(value)))


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    source = (ROOT / 'hardware/charge-core/RaceRemote_Charge.kicad_sym').read_text(encoding='utf-8')
    symbols = {n: block(source, source.index('(symbol "' + n + '"'))
               for n in ['R', 'C', 'Conn_01x02', 'PWR_FLAG']}
    symbols['INA300DSQ'] = ic('INA300DSQ',
        [(1, 'IN+', 'input'), (2, 'IN-', 'input'), (4, 'ENABLE', 'input'),
         (6, 'LATCH', 'input'), (9, 'VS', 'power_in'), (8, 'GND', 'power_in')],
        [(5, '~{ALERT}', 'open_collector'), (3, 'LIMIT', 'input'), (7, 'DELAY', 'input'),
         (10, 'HYS', 'input'), (11, 'EP', 'power_in')]).replace('(end 17.78 -17.78)', '(end 17.78 -38.1)')
    (DEST / (LIB + '.kicad_sym')).write_text('(kicad_symbol_lib (version 20251024) (generator "raceremote")\n'
        + '\n'.join(symbols.values()) + ')\n', encoding='utf-8')
    cache = [s.replace('(symbol "' + n + '"', '(symbol "' + LIB + ':' + n + '"', 1)
             for n, s in symbols.items()]
    out = ['(kicad_sch (version 20250901) (generator "raceremote")',
           f'(uuid "{uid("sheet")}") (paper "A4")',
           '(title_block (title "RaceRemote - latched overcurrent PROPOSAL") '
           '(date "2026-09-14") (rev "0.2") (company "vea.raceremote"))',
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
                   + f'(instances (project "charge-overcurrent" (path "/{uid("sheet")}" (reference "{ref}") (unit 1)))))')
        if not flag:
            bom.append({'reference': ref, 'value': value, 'purpose': purpose, 'footprint': '',
                        'selection': 'Candidate; no PCB or physical overcurrent qualification'})

    def net(ref, pin, name, dx=7.62, dy=0):
        a = positions[ref][str(pin)]
        b = (round(a[0] + dx, 4), round(a[1] + dy, 4))
        out.append(f'(wire (pts (xy {a[0]:g} {a[1]:g}) (xy {b[0]:g} {b[1]:g})) '
                   f'(stroke (width 0) (type default)) (uuid "{uid((ref, pin, "wire"))}"))')
        out.append(f'(global_label {q(name)} (shape passive) (at {b[0]:g} {b[1]:g} {180 if dx < 0 else 0}) '
                   '(effects (font (size 1 1)) (justify ' + ('right' if dx < 0 else 'left') + ')) '
                   f'(uuid "{uid((ref, pin, "net"))}"))')

    def pair(name, ref, value, x, top, bottom, purpose):
        symbol(name, ref, value, x, 154.94, purpose)
        net(ref, 1, top, 0, -7.62)
        net(ref, 2, bottom, 0, 7.62)

    def note(value, x, y, size=1.27):
        out.append(f'(text {q(value)} (at {x:g} {y:g} 0) (effects (font (size {size} {size})) '
                   f'(justify left top)) (uuid "{uid(value)}"))')

    symbol('INA300DSQ', 'U701', 'INA300AIDSQR', 101.6, 88.9,
           'Always enabled and latched; no MCU clear/disable; not a current limiter', 'https://www.ti.com/lit/gpn/ina300')
    for pin, name in [(1, 'SHUNT_UPSTREAM_P'), (2, 'SHUNT_DOWNSTREAM_P'), (4, 'AUX_3V3'),
                      (6, 'AUX_3V3'), (9, 'AUX_3V3'), (8, 'USB_GND')]:
        net('U701', pin, name, -7.62)
    for pin, name in [(5, 'FAULT_BUS_N'), (3, 'OC_LIMIT'), (7, 'USB_GND'), (11, 'USB_GND')]:
        net('U701', pin, name)
    x, y = positions['U701']['10']
    out.append(f'(no_connect (at {x:g} {y:g}) (uuid "{uid("HYS-open")}"))')
    pair('C', 'C701', '100n / 16V', 38.1, 'AUX_3V3', 'USB_GND', 'Local bypass at VS')
    pair('R', 'R701', '1.74k / 1%', 101.6, 'OC_LIMIT', 'USB_GND', 'Nominal 34.8mV threshold; 50us mode')
    pair('R', 'R702', 'RL2512FK-070R1L', 144.78, 'SHUNT_UPSTREAM_P', 'SHUNT_DOWNSTREAM_P',
         '0.1 ohm 1% 1W at70C +/-600ppm/C; two-terminal Kelvin layout required; placement pending')
    for ref, value, y, nets, purpose in [
        ('J701', 'FROM USB AUX', 63.5, ['AUX_3V3', 'USB_GND'], 'Not CHG_BIAS, XIAO or battery'),
        ('J702', 'BENCH SERIES PATH', 101.6, ['SHUNT_UPSTREAM_P', 'SHUNT_DOWNSTREAM_P'],
         'Positive charge flow pin1 to pin2; NOT actual battery connector or final BQ BAT/MID placement'),
        ('J703', 'TO PERMIT J402', 139.7, ['FAULT_BUS_N', 'USB_GND'],
         'Only existing R401 pulls up; no push-pull or second pull-up')]:
        symbol('Conn_01x02', ref, value, 228.6, y, purpose)
        for i, name in enumerate(nets): net(ref, i + 1, name, -7.62)
    for ref, name, x in [('#FLG701', 'AUX_3V3', 165.1), ('#FLG702', 'USB_GND', 254)]:
        symbol('PWR_FLAG', ref, 'PWR_FLAG', x, 38.1)
        net(ref, 1, name, 0, 5.08)
    note('CHARGE-OC-01 v0.2 - latched fault into permit; bench proposal, NOT complete protection', 12.7, 12.7, 1.5)
    note('ENABLE/LATCH tied to AUX. HYS open, DELAY grounded. No MCU reset/disable or automatic retry.\n'
         'Shunt power path is provisional: placement relative to BQ BAT/MID/CBSET and pack protection remains open.', 12.7, 22.86, 1)
    note('Nominal trip 348mA; comparator setting 50us.\n'
         'NOT a guaranteed peak-current limit or turnoff time.\n'
         'Supply cycling/recovery, shunt heat, cell balance\n'
         'and real CD/current behavior remain unverified.', 12.7, 180.34, 1)
    out.append('(sheet_instances (path "/" (page "1"))) (embedded_fonts no))')
    (DEST / 'charge-overcurrent.kicad_sch').write_text('\n'.join(out) + '\n', encoding='utf-8')
    (DEST / 'charge-overcurrent.kicad_pro').write_text(json.dumps({'meta': {'filename': 'charge-overcurrent.kicad_pro', 'version': 3}}, indent=2) + '\n')
    (DEST / 'sym-lib-table').write_text(f'(sym_lib_table (version 7) (lib (name "{LIB}") (type "KiCad") '
        f'(uri "${{KIPRJMOD}}/{LIB}.kicad_sym") (options "") (descr "Own TI pin symbol and attributed passives")))\n')
    (DEST / 'fp-lib-table').write_text('(fp_lib_table (version 7))\n')
    with (DEST / 'bom.csv').open('w', encoding='utf-8-sig', newline='') as f:
        writer = csv.DictWriter(f, fieldnames=list(bom[0])); writer.writeheader(); writer.writerows(bom)
    print('Generated', len(bom), 'components')


if __name__ == '__main__': main()
