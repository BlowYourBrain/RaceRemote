"""Generate a battery-referenced BQ29209 bench interface, not a complete BMS."""
import csv
import json
from pathlib import Path
import re
import uuid
from build_charge_permit import ic, q
from build_usb_port_schematic import block

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT/'hardware/pack-balancer'
LIB = 'RaceRemote_Balancer'


def uid(value):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'https://vea.raceremote/pack-balancer/'+str(value)))


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    source = (ROOT/'hardware/charge-core/RaceRemote_Charge.kicad_sym').read_text(encoding='utf-8')
    symbols = {n: block(source, source.index('(symbol "'+n+'"'))
               for n in ['R', 'C', 'Conn_01x04', 'PWR_FLAG']}
    symbols['BQ29209DRB'] = ic('BQ29209DRB',
        [(1, 'VC2', 'input'), (2, 'VC1', 'input'), (3, 'VC1_CB', 'passive'), (4, 'CD', 'passive')],
        [(8, 'OUT', 'output'), (7, 'VDD', 'power_in'), (6, '~{CB_EN}', 'input'),
         (5, 'GND', 'power_in'), (9, 'EP', 'power_in')]).replace('(end 17.78 -17.78)', '(end 17.78 -27.94)')
    (DEST/(LIB+'.kicad_sym')).write_text('(kicad_symbol_lib (version 20251024) (generator "raceremote")\n'
        +'\n'.join(symbols.values())+')\n', encoding='utf-8')
    cached = [s.replace('(symbol "'+n+'"', '(symbol "'+LIB+':'+n+'"', 1) for n, s in symbols.items()]
    out = ['(kicad_sch (version 20250901) (generator "raceremote")',
           f'(uuid "{uid("sheet")}") (paper "A3")',
           '(title_block (title "RaceRemote - battery-side balancer BENCH PROPOSAL") '
           '(date "2026-09-14") (rev "0.1") (company "vea.raceremote"))',
           '(lib_symbols '+'\n'.join(cached)+')']
    positions, bom = {}, []

    def prop(name, value, x, y, hide=False):
        return f'(property {q(name)} {q(value)} (at {x:g} {y:g} 0) '+('(hide yes) ' if hide else '')+ \
               '(effects (font (size 1.27 1.27))))'

    def symbol(name, ref, value, x, y, purpose=''):
        pins = {}
        for m in re.finditer(r'\(pin (\S+).*?\(number "([^"]+)"', symbols[name], re.S):
            px, py, _ = map(float, re.search(r'\(at ([^)]+)', m[0])[1].split())
            pins[m[2]] = (round(x+px, 4), round(y-py, 4))
        positions[ref] = pins
        flag, chip = name == 'PWR_FLAG', ref.startswith('U')
        out.append(f'(symbol (lib_id "{LIB}:{name}") (at {x:g} {y:g} 0) (unit 1) '
                   f'(in_bom {"no" if flag else "yes"}) (on_board {"no" if flag else "yes"}) '
                   f'(dnp no) (uuid "{uid(ref)}") '+prop('Reference', ref, x, y-(30.48 if chip else 5.08), flag)
                   +prop('Value', value, x+ (15.24 if name in ['R', 'C'] else 0),
                         y if name in ['R', 'C'] else y-(33.02 if chip else 7.62), flag)
                   +prop('Footprint', '', x, y, True)
                   +prop('Datasheet', 'https://www.ti.com/lit/gpn/bq29209' if chip else '', x, y, True)
                   +''.join(f'(pin "{p}" (uuid "{uid(ref+"/"+p)}"))' for p in pins)
                   +f'(instances (project "pack-balancer" (path "/{uid("sheet")}" (reference "{ref}") (unit 1)))))')
        if not flag:
            bom.append(dict(reference=ref, value=value, purpose=purpose, footprint='',
                            selection='Bench candidate, not ordered or physically qualified'))

    def net(ref, pin, name, dx=10.16, dy=0):
        a = positions[ref][str(pin)]
        b = round(a[0]+dx, 4), round(a[1]+dy, 4)
        out.append(f'(wire (pts (xy {a[0]:g} {a[1]:g}) (xy {b[0]:g} {b[1]:g})) '
                   f'(stroke (width 0) (type default)) (uuid "{uid((ref,pin,"wire"))}"))')
        out.append(f'(global_label {q(name)} (shape passive) (at {b[0]:g} {b[1]:g} {180 if dx<0 else 0}) '
                   '(effects (font (size 1 1)) (justify '+('right' if dx<0 else 'left')+')) '
                   f'(uuid "{uid((ref,pin,"net"))}"))')

    def pair(name, ref, value, x, y, a, b, purpose):
        symbol(name, ref, value, x, y, purpose)
        net(ref, 1, a, 0, -7.62)
        net(ref, 2, b, 0, 7.62)

    def note(value, x, y, size=1.27):
        out.append(f'(text {q(value)} (at {x:g} {y:g} 0) (effects (font (size {size} {size})) '
                   f'(justify left top)) (uuid "{uid(value)}"))')

    symbol('BQ29209DRB', 'U801', 'BQ29209DRBR', 121.92, 86.36,
           '4.30V secondary OVP and small balancing; NOT 4.2V charge regulation or primary BMS')
    for p, n in [(1, 'BAL_VC2'), (2, 'BAL_VC1'), (3, 'BAL_VC1_CB'), (4, 'BAL_DELAY')]:
        net('U801', p, n, -10.16)
    for p, n in [(8, 'BAL_OVP_RAW'), (7, 'BAL_VDD'), (6, 'BAL_EN_N'),
                 (5, 'BAT_RAW_N'), (9, 'BAT_RAW_N')]:
        net('U801', p, n)
    for ref, value, x, a, b, purpose in [
        ('R801', '100 / 1%', 40.64, 'BAT_RAW_P', 'BAL_VDD', 'Supply filter RVD, also upper balance path'),
        ('R802', '100 / 1%', 111.76, 'BAT_RAW_P', 'BAL_VC2', 'Top sense filter, recommended 100..1000 ohm'),
        ('R803', '100 / 1%', 182.88, 'BAT_MID', 'BAL_VC1', 'Bottom sense filter, not internal switch resistance'),
        ('R804', '120 / 1%', 254., 'BAT_MID', 'BAL_VC1_CB', 'RCB: use datasheet 7.6 guidance; not 250mA bypass'),
        ('R805', '100k / 1%', 325.12, 'BAL_VDD', 'BAL_EN_N', 'Default balance OFF; external sink must reference BAT_RAW_N')]:
        pair('R', ref, value, x, 172.72, a, b, purpose)
    for ref, value, x, a, b, purpose in [
        ('C801', '100n / 16V', 40.64, 'BAL_VDD', 'BAT_RAW_N', 'Local supply filter'),
        ('C802', '100n / 16V', 111.76, 'BAL_VC2', 'BAL_VC1', 'Differential cell filter per Figure 9, NOT to ground'),
        ('C803', '100n / 16V', 182.88, 'BAL_VC1', 'BAT_RAW_N', 'Bottom-cell filter'),
        ('C804', '100n / 16V / 10%', 254., 'BAL_DELAY', 'BAT_RAW_N', 'Nominal0.9s OVP delay, not full shutdown latency')]:
        pair('C', ref, value, x, 233.68, a, b, purpose)
    for ref, value, y, names, purpose in [
        ('J801', 'CELL-SIDE BENCH NODES', 68.58, ['BAT_RAW_P', 'BAT_MID', 'BAT_RAW_N', 'BAT_RAW_N'],
         'Logical 2S emulator nodes only; not LW connector order; pin4 repeats raw negative'),
        ('J802', 'BATTERY-DOMAIN IO ONLY', 116.84, ['BAL_EN_N', 'BAL_OVP_RAW', 'BAL_VDD', 'BAT_RAW_N'],
         'No direct MCU/USB/permit wiring; level and ground crossing not designed')]:
        symbol('Conn_01x04', ref, value, 345.44, y, purpose)
        for p, n in enumerate(names, 1): net(ref, p, n, -10.16)
    for ref, name, x in [('#FLG801', 'BAL_VDD', 233.68), ('#FLG802', 'BAT_RAW_N', 274.32)]:
        symbol('PWR_FLAG', ref, 'PWR_FLAG', x, 48.26)
        net(ref, 1, name, 0, 5.08)
    note('PACK-BAL-01 v0.1 - stand-alone battery-side test block, NOT a complete charger', 12.7, 12.7, 1.6)
    note('BAT_RAW_N is cell negative BEFORE a low-side protection switch. Never equate it with USB_GND/PACK_MINUS.\n'
         'BAL_EN_N defaults OFF. External enable/OVP interfaces need battery-domain isolation/level translation.', 12.7, 24.13, 1.2)
    note('OUT is active HIGH (up to9.5V in specified conditions), not open drain. No direct FAULT_BUS_N connection.\n'
         'OVP is secondary4.30V, not a4.20V cell regulator. BQ25886+A is still incomplete.\n'
         'No fuse/FET actuator, low-voltage enable policy, PCB, physical balancing or LW charge performed.', 12.7, 264.16, 1.1)
    out.append('(sheet_instances (path "/" (page "1"))) (embedded_fonts no))')
    (DEST/'pack-balancer.kicad_sch').write_text('\n'.join(out)+'\n', encoding='utf-8')
    (DEST/'pack-balancer.kicad_pro').write_text(json.dumps({'meta': {'filename': 'pack-balancer.kicad_pro', 'version': 3}}, indent=2)+'\n')
    (DEST/'sym-lib-table').write_text(f'(sym_lib_table (version 7) (lib (name "{LIB}") (type "KiCad") '
        f'(uri "${{KIPRJMOD}}/{LIB}.kicad_sym") (options "") (descr "Own TI pin symbol and attributed passives")))\n')
    (DEST/'fp-lib-table').write_text('(fp_lib_table (version 7))\n')
    with (DEST/'bom.csv').open('w', encoding='utf-8-sig', newline='') as f:
        w = csv.DictWriter(f, fieldnames=list(bom[0])); w.writeheader(); w.writerows(bom)
    print('Generated', len(bom), 'components')


if __name__ == '__main__': main()
