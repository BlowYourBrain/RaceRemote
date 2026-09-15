"""Editable circuit proposal, not a PCB or permission to connect a battery."""
import csv
import json
from pathlib import Path
from uuid import NAMESPACE_URL, uuid5

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'hardware/xiao-logic-power'
LIB = 'RaceRemote_LogicPower'


def uid(name):
    return str(uuid5(NAMESPACE_URL, 'vea.raceremote/logic-power/v01/' + name))


def q(value):
    return json.dumps(str(value), ensure_ascii=False)


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    # TPS737 Rev W p3 (DRB); MAX40200 Rev4 p8 (SOT23-5, not WLP).
    pins = {
        'TPS73701_DRB': [
            ('8', 'IN', 'power_in', -20.32, 12.7, 0),
            ('5', 'EN', 'input', -20.32, 2.54, 0),
            ('2', 'NC', 'passive', -20.32, -7.62, 0),
            ('1', 'OUT', 'power_out', 20.32, 12.7, 180),
            ('3', 'FB', 'input', 20.32, 2.54, 180),
            ('6', 'NC', 'passive', 20.32, -7.62, 180),
            ('7', 'NC', 'passive', 20.32, -17.78, 180),
            ('4', 'GND', 'power_in', -5.08, -27.94, 90),
            ('9', 'EP_GND', 'passive', 5.08, -27.94, 90)],
        'MAX40200_AUK': [
            ('1', 'VDD', 'power_in', -20.32, 12.7, 0),
            ('3', 'EN', 'input', -20.32, 2.54, 0),
            ('4', 'NC', 'passive', -20.32, -7.62, 0),
            ('5', 'OUT', 'power_out', 20.32, 12.7, 180),
            ('2', 'GND', 'power_in', 0, -27.94, 90)],
        'R': [('1', '~', 'passive', 0, 5.08, 270), ('2', '~', 'passive', 0, -5.08, 90)],
        'C': [('1', '~', 'passive', 0, 5.08, 270), ('2', '~', 'passive', 0, -5.08, 90)],
        'Terminal': [('1', '~', 'passive', -5.08, 0, 0)],
        'Flag': [('1', '~', 'power_out', 0, 0, 90)],
    }
    symbols = {}
    for name, spec in pins.items():
        ic = name in ('TPS73701_DRB', 'MAX40200_AUK')
        if ic:
            graphics = '(rectangle (start -15.24 20.32)(end 15.24 -22.86)(stroke (width .254)(type default))(fill (type background)))'
        elif name == 'R':
            graphics = '(rectangle (start -1.016 2.54)(end 1.016 -2.54)(stroke (width .254)(type default))(fill (type none)))'
        elif name == 'C':
            graphics = ''.join(f'(polyline (pts (xy -2.54 {y})(xy 2.54 {y}))(stroke (width .254)(type default))(fill (type none)))' for y in (.762, -.762))
        elif name == 'Terminal':
            graphics = '(circle (center 0 0)(radius 1.27)(stroke (width .254)(type default))(fill (type none)))'
        else:
            graphics = '(polyline (pts (xy 0 0)(xy 0 2.54)(xy -1.27 1.27)(xy 1.27 1.27)(xy 0 2.54))(stroke (width .254)(type default))(fill (type none)))'
        pin_text = ''
        for number, label, kind, x, y, angle in spec:
            length = 5.08 if ic else 0 if name == 'Flag' else 3.81 if name == 'Terminal' else 4.318 if name == 'C' else 2.54
            pin_text += f'(pin {kind} line (at {x} {y} {angle})(length {length})(name {q(label)} (effects (font (size 1 1))))(number {q(number)} (effects (font (size 1 1)))))'
        ref = 'U' if ic else 'J' if name == 'Terminal' else '#FLG' if name == 'Flag' else name
        symbols[name] = f'(symbol {q(name)} (pin_names (offset 1.016))(in_bom yes)(on_board yes)(property "Reference" {q(ref)} (at 0 0 0)(effects (font (size 1.27 1.27))))(property "Value" {q(name)} (at 0 0 0)(effects (font (size 1.27 1.27))))(symbol "{name}_0_1" {graphics})(symbol "{name}_1_1" {pin_text}))'
    (DEST / f'{LIB}.kicad_sym').write_text('(kicad_symbol_lib (version 20251024)(generator "raceremote")\n' + '\n'.join(symbols.values()) + ')\n', encoding='utf-8')
    cached = [s.replace(f'(symbol {q(n)}', f'(symbol {q(LIB + ":" + n)}', 1) for n, s in symbols.items()]
    out = ['(kicad_sch (version 20250901)(generator "raceremote")',
           f'(uuid "{uid("sheet")}")(paper "A3")',
           '(title_block (title "XIAO logic supply - automatic reverse blocking proposal") (date "2026-09-15")(rev "0.2")(company "vea.raceremote"))',
           '(lib_symbols ' + '\n'.join(cached) + ')']
    bom, connections = [], []

    def add(name, ref, value, x, y, nets, package=''):
        flag = name == 'Flag'
        ic = name in ('TPS73701_DRB', 'MAX40200_AUK')
        def prop(key, val, px, py, hide=False):
            return f'(property {q(key)} {q(val)} (at {px:g} {py:g} 0)' + ('(hide yes)' if hide else '') + '(effects (font (size 1.27 1.27))))'
        out.append(f'(symbol (lib_id "{LIB}:{name}")(at {x:g} {y:g} 0)(unit 1)(in_bom {"no" if flag else "yes"})(on_board {"no" if flag else "yes"})(dnp no)(uuid "{uid(ref)}")' +
                   prop('Reference', ref, x, y - (27.94 if ic else 7.62), flag) +
                   prop('Value', value, x if ic else x + 16, y - 24.13 if ic else y, flag) +
                   prop('Footprint', '', x, y, True) +
                   ''.join(f'(pin "{p[0]}" (uuid "{uid(ref + "/" + p[0])}"))' for p in pins[name]) +
                   f'(instances (project "xiao-logic-power" (path "/{uid("sheet")}" (reference "{ref}")(unit 1)))))')
        if not flag:
            bom.append({'reference': ref, 'value': value, 'package_proposal': package,
                        'status': 'Logical interface, not purchased connector' if name == 'Terminal' else 'Candidate, not purchased',
                        'price_rub': '50' if ref == 'U2' else '',
                        'price_checked': '2026-09-15; MOQ1; Cheboksary pickup estimate 18 September' if ref == 'U2' else '',
                        'source_url': 'https://www.chipdip.ru/product/max40200auk-t-mikroshema-idealnyy-diod-s-sverhnizkim-maxim-9000587524' if ref == 'U2' else ''})
        for number, label, _, px, py, angle in pins[name]:
            ax, ay = x + px, y - py
            net = nets[number]
            if not flag:
                connections.append({'reference': ref, 'pin': number, 'name': label, 'net': net or 'NC'})
            if net is None:
                out.append(f'(no_connect (at {ax:g} {ay:g})(uuid "{uid(ref + number + "nc")}"))')
                continue
            dx, dy = (-7.62, 0) if angle == 0 else (7.62, 0) if angle == 180 else (0, 7.62) if angle == 90 else (0, -7.62)
            bx, by = ax + dx, ay + dy
            out.append(f'(wire (pts (xy {ax:g} {ay:g})(xy {bx:g} {by:g}))(stroke (width 0)(type default))(uuid "{uid(ref + number + "wire")}"))')
            # Vertical labels avoid adjacent ground pin labels overlapping.
            rotation = 180 if dx < 0 else 0 if dx > 0 else 90
            justify = 'right' if dx < 0 else 'left'
            out.append(f'(global_label {q(net)} (shape passive)(at {bx:g} {by:g} {rotation})(effects (font (size 1 1))(justify {justify}))(uuid "{uid(ref + number + "label")}"))')

    add('TPS73701_DRB', 'U1', 'TPS73701DRBR', 101.6, 88.9,
        {'8': 'WAVE_5V', '5': 'WAVE_5V', '2': None, '1': 'LDO_4V', '3': 'FB', '6': None, '7': None, '4': 'DRIVE_GND', '9': 'DRIVE_GND'}, 'DRB VSON8 + EP, 3x3mm')
    add('MAX40200_AUK', 'U2', 'MAX40200AUK+T', 254, 88.9,
        {'1': 'LDO_4V', '3': 'LDO_4V', '4': None, '5': 'XIAO_BAT', '2': 'DRIVE_GND'}, 'SOT23-5, not WLP / not TPMAX clone')
    for ref, value, x, a, b, package in [
        ('R1', '23.2k / 0.1%', 48.26, 'LDO_4V', 'FB', '0805'),
        ('R2', '10k / 0.1%', 114.3, 'FB', 'DRIVE_GND', '0805'),
        ('R3', '360 / 1% / 0.25W', 190.5, 'LDO_4V', 'DRIVE_GND', '1206, rating to verify')]:
        add('R', ref, value, x, 182.88, {'1': a, '2': b}, package)
    for ref, value, x, net in [('C1', '4.7u / 10V X7R', 48.26, 'WAVE_5V'), ('C2', '4.7u / 10V X7R', 144.78, 'LDO_4V'), ('C3', '1u / 10V X7R', 243.84, 'LDO_4V'), ('C4', '1u / 10V X7R', 340.36, 'XIAO_BAT')]:
        add('C', ref, value, x, 231.14, {'1': net, '2': 'DRIVE_GND'}, '0805; effective C2>=1uF, C3/C4>=0.33uF')
    # J4 is deliberately retired: existing J5 keeps its ground meaning.
    for i, net in [(1, 'WAVE_5V'), (2, 'DRIVE_GND'), (3, 'XIAO_BAT'), (5, 'DRIVE_GND')]:
        add('Terminal', f'J{i}', net, 358.14, 68.58 + (i - 1) * 17.78, {'1': net})
    for i, net in enumerate(['WAVE_5V', 'DRIVE_GND'], 1):
        add('Flag', f'#FLG0{i}', 'PWR_FLAG', 20.32 + i * 35.56, 142.24, {'1': net})
    notes = [
        'PROPOSAL ONLY: no PCB, no battery/charger/protection approval, no measured output current or source handover.',
        'No J4 / USB sense wire. Native XIAO 5V/VBUS and charge-port VBUS stay separate from this circuit.',
        'J3 = XIAO BAT0 positive; J5 = XIAO GND. 2S raw battery NEVER connects to J3.',
        'U2 EN tied locally to VDD. MAX40200 detects reverse bias internally; switching transient limits remain unproven.',
        'U1 EN tied to IN does NOT guarantee reverse protection. Startup overshoot and local capacitor discharge remain open.',
        'R3 keeps LDO load above 10mA; C2>=1uF, C3/C4>=0.33uF effective. Total OUT load capacitance <=100uF.',
        'Use SOT23-5 limits: 175mV max drop at 500mA / 3.3V test point. WLP headline values do not apply.'
    ]
    for i, note in enumerate(notes):
        out.append(f'(text {q(note)} (at 20.32 {12.7 + i * 5.08:g} 0)(effects (font (size 1.1 1.1))(justify left top))(uuid "{uid(note)}"))')
    out.append('(sheet_instances (path "/" (page "1")))(embedded_fonts no))')
    (DEST / 'xiao-logic-power.kicad_sch').write_text('\n'.join(out) + '\n', encoding='utf-8')
    (DEST / 'sym-lib-table').write_text(f'(sym_lib_table (version 7)(lib (name "{LIB}")(type "KiCad")(uri "${{KIPRJMOD}}/{LIB}.kicad_sym")(options "")(descr "Original proposal symbols")))\n', encoding='utf-8')
    project = DEST / 'xiao-logic-power.kicad_pro'
    if not project.exists():
        project.write_text('{"meta":{"filename":"xiao-logic-power.kicad_pro","version":3}}\n', encoding='utf-8')
    for filename, rows in [('bom.csv', bom), ('connections.csv', connections)]:
        with (DEST / filename).open('w', encoding='utf-8', newline='') as f:
            writer = csv.DictWriter(f, fieldnames=list(rows[0]))
            writer.writeheader()
            writer.writerows(rows)
    print(f'Generated {len(bom)} positions / {len(connections)} pins (including NC). No PCB.')


if __name__ == '__main__':
    main()
