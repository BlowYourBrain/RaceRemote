"""Independent TA6586 schematic connectivity; original simple symbols, KiCad 10."""
import csv
import json
from pathlib import Path
from motor_carrier_identity import uid, footprint

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'hardware/motor-carrier'
LIB = 'RaceRemote_Motor'


def q(value):
    return json.dumps(str(value), ensure_ascii=False)


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    pins = {
        'TA6586': [('1','BI','input',-12.7,2.54,0), ('2','FI','input',-12.7,7.62,0),
                    ('3','GND','power_in',0,-15.24,90), ('4','VCC','power_in',0,15.24,270),
                    ('5','FO','output',12.7,7.62,180), ('6','FO','passive',12.7,2.54,180),
                    ('7','BO','output',12.7,-2.54,180), ('8','BO','passive',12.7,-7.62,180)],
        'R': [('1','~','passive',0,5.08,270),('2','~','passive',0,-5.08,90)],
        'C': [('1','~','passive',0,5.08,270),('2','~','passive',0,-5.08,90)],
        'CP': [('1','+','passive',0,5.08,270),('2','-','passive',0,-5.08,90)],
        'WirePad': [('1','~','passive',-5.08,0,0)],
        'Flag': [('1','~','power_out',0,0,90)]}
    symbols = {}
    for name, pin_list in pins.items():
        graphics = ''
        if name == 'TA6586':
            graphics = '(rectangle (start -7.62 10.16)(end 7.62 -10.16)(stroke (width 0.254)(type default))(fill (type background)))'
        elif name == 'R':
            graphics = '(rectangle (start -1.016 2.54)(end 1.016 -2.54)(stroke (width 0.254)(type default))(fill (type none)))'
        elif name in ['C','CP']:
            graphics = ''.join(f'(polyline (pts (xy -2.54 {y})(xy 2.54 {y}))(stroke (width 0.254)(type default))(fill (type none)))' for y in [.762,-.762])
        elif name == 'WirePad':
            graphics = '(circle (center 0 0)(radius 1.27)(stroke (width .254)(type default))(fill (type none)))'
        else:
            graphics = '(polyline (pts (xy 0 0)(xy 0 2.54)(xy -1.27 1.27)(xy 1.27 1.27)(xy 0 2.54))(stroke (width .254)(type default))(fill (type none)))'
        ptext = ''
        for number, label, kind, x, y, angle in pin_list:
            length = 5.08 if name == 'TA6586' else 0 if name == 'Flag' else 3.81 if name == 'WirePad' else 4.318 if name in ['C','CP'] else 2.54
            ptext += f'(pin {kind} line (at {x} {y} {angle})(length {length})(name {q(label)} (effects (font (size 1.0 1.0))))(number {q(number)} (effects (font (size 1.0 1.0)))))'
        symbols[name] = f'(symbol {q(name)} (pin_names (offset 1.016)) (in_bom yes)(on_board yes)(property "Reference" "{ "U" if name == "TA6586" else "#FLG" if name == "Flag" else "H" if name == "WirePad" else "R" if name == "R" else "C"}" (at 0 0 0)(effects (font (size 1.27 1.27))))(property "Value" {q(name)} (at 0 0 0)(effects (font (size 1.27 1.27))))(symbol "{name}_0_1" {graphics})(symbol "{name}_1_1" {ptext}))'
    (DEST / f'{LIB}.kicad_sym').write_text('(kicad_symbol_lib (version 20251024)(generator "raceremote")\n' + '\n'.join(symbols.values()) + ')\n', encoding='utf-8')
    cached = [s.replace(f'(symbol {q(n)}', f'(symbol {q(LIB+":"+n)}', 1) for n,s in symbols.items()]
    out = ['(kicad_sch (version 20250901)(generator "raceremote")', f'(uuid "{uid("sheet")}")(paper "A4")',
           '(title_block (title "TA6586 motor carrier - PROPOSAL") (date "2026-09-15")(rev "0.1")(company "vea.raceremote"))',
           '(lib_symbols ' + '\n'.join(cached) + ')']
    positions, bom = {}, []
    def prop(name,value,x,y,hide=False):
        return f'(property {q(name)} {q(value)} (at {x:g} {y:g} 0)' + ('(hide yes)' if hide else '') + '(effects (font (size 1.27 1.27))))'
    def add(name,ref,value,x,y):
        positions[ref] = {n:(x+px,y-py) for n,_,_,px,py,_ in pins[name]}
        flag = name == 'Flag'
        reference_y = y - (20.32 if name == 'TA6586' else 7.62 if name != 'WirePad' else 2.54)
        value_y = y - 17.78 if name == 'TA6586' else y
        value_x = x if name == 'TA6586' else x+12.7
        out.append(f'(symbol (lib_id "{LIB}:{name}")(at {x:g} {y:g} 0)(unit 1)(in_bom {"no" if flag else "yes"})(on_board {"no" if flag else "yes"})(dnp no)(uuid "{uid(ref)}")' +
            prop('Reference',ref,x,reference_y,flag) + prop('Value',value,value_x,value_y,flag) +
            prop('Footprint','' if flag else footprint(ref),x,y,True) +
            prop('Datasheet','https://static.chipdip.ru/lib/923/DOC012923012.pdf' if ref=='U1' else '',x,y,True) +
            ''.join(f'(pin "{n}" (uuid "{uid(ref+"/"+n)}"))' for n in positions[ref]) +
            f'(instances (project "motor-carrier" (path "/{uid("sheet")}" (reference "{ref}")(unit 1)))))')
        if not flag:
            bom.append({'reference':ref,'value':value,'footprint':footprint(ref),
                'mpn_reference':'16ZLH470MEFC8X11.5' if ref=='C2' else 'TA6586' if ref=='U1' else '',
                'status':'Existing inventory' if ref=='U1' else 'Integral PCB solder pad, not a purchased connector' if ref.startswith('H') else 'Candidate, not purchased',
                'price_rub':''})
    def connect(ref,pin,net,dx=7.62,dy=0):
        a=positions[ref][str(pin)]; b=(a[0]+dx,a[1]+dy)
        out.append(f'(wire (pts (xy {a[0]:g} {a[1]:g})(xy {b[0]:g} {b[1]:g}))(stroke (width 0)(type default))(uuid "{uid(ref+str(pin)+"wire")}"))')
        out.append(f'(global_label {q(net)} (shape passive)(at {b[0]:g} {b[1]:g} {180 if dx < 0 else 0})(effects (font (size 1.0 1.0))(justify {"right" if dx < 0 else "left"}))(uuid "{uid(ref+str(pin)+"label")}"))')
    add('TA6586','U1','TA6586 DIP8',101.6,73.66)
    for pin,net in {1:'BI',2:'FI',3:'DRIVE_GND',4:'VM',5:'FO',6:'FO',7:'BO',8:'BO'}.items():
        connect('U1',pin,net,dx=-10.16 if pin in [1,2] else 0 if pin in [3,4] else 10.16,dy=7.62 if pin==3 else -7.62 if pin==4 else 0)
    for ref,value,x,a,b in [('R1','100 / 1%',38.1,'CMD_FI','FI'),('R3','10k / 1%',83.82,'FI','DRIVE_GND'),
                           ('R2','100 / 1%',129.54,'CMD_BI','BI'),('R4','10k / 1%',175.26,'BI','DRIVE_GND')]:
        add('R',ref,value,x,137.16); connect(ref,1,a,dx=0,dy=-7.62);connect(ref,2,b,dx=0,dy=7.62)
    for ref,name,value,x in [('C1','C','100n / 25V',160.02),('C2','CP','470u / 16V',203.2)]:
        add(name,ref,value,x,76.2);connect(ref,1,'VM',dx=0,dy=-7.62);connect(ref,2,'DRIVE_GND',dx=0,dy=7.62)
    for ref,net,x,y in [('H1','CMD_FI',251.46,116.84),('H2','CMD_BI',251.46,129.54),('H3','DRIVE_GND',251.46,142.24),
                        ('H4','VM',38.1,48.26),('H5','DRIVE_GND',38.1,63.5),('H6','FO',251.46,48.26),('H7','BO',251.46,60.96)]:
        add('WirePad',ref,net,x,y);connect(ref,1,net,dx=-7.62)
    for ref,net,x in [('#FLG1','VM',101.6),('#FLG2','DRIVE_GND',152.4)]:
        add('Flag',ref,'PWR_FLAG',x,177.8);connect(ref,1,net,dx=0,dy=5.08)
    for i,text in enumerate(['Existing TA6586; paired outputs 5+6 and 7+8. Carrier is not a complete power/protection circuit.',
        'FI/BI pulldowns are on the IC side of series resistors. Motor current never returns through MCU wiring.',
        'VM/DRIVE_GND from a separately qualified supply path. No direct battery/charger ground equivalence is implied.',
        '7 A is NOT a board rating. Copper, current, temperature, power sequencing and actual logic levels need bench checks.']):
        out.append(f'(text {q(text)} (at 20.32 {20.32+i*5.08:g} 0)(effects (font (size 1.1 1.1))(justify left top))(uuid "{uid(text)}"))')
    out.append('(sheet_instances (path "/" (page "1")))(embedded_fonts no))')
    (DEST/'motor-carrier.kicad_sch').write_text('\n'.join(out)+'\n',encoding='utf-8')
    (DEST/'sym-lib-table').write_text(f'(sym_lib_table (version 7)(lib (name "{LIB}")(type "KiCad")(uri "${{KIPRJMOD}}/{LIB}.kicad_sym")(options "")(descr "Original carrier symbols")))\n',encoding='utf-8')
    if not (DEST/'motor-carrier.kicad_pro').exists():
        (DEST/'motor-carrier.kicad_pro').write_text('{"meta":{"filename":"motor-carrier.kicad_pro","version":3}}\n',encoding='utf-8')
    with (DEST/'bom.csv').open('w',encoding='utf-8-sig',newline='') as f:
        w=csv.DictWriter(f,fieldnames=list(bom[0]));w.writeheader();w.writerows(bom)
    print('Generated motor carrier schematic:',len(bom),'components')


if __name__=='__main__': main()
