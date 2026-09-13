"""Generate BQ25886 core proposal; source qualification/cell protection are external.

This is an electrical design for review, not authorization to connect a LiPo.
Uses project-local KiCad symbols. No PCB or physical device is changed.
"""
import csv
import json
import math
from pathlib import Path
import re
import uuid

from build_usb_port_schematic import block

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'hardware/charge-core'
LIBNAME = 'RaceRemote_Charge'


def uid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'https://vea.raceremote/charge-core/' + name))


def quote(value):
    return json.dumps(str(value), ensure_ascii=False)


def main():
    lib = (DEST / (LIBNAME + '.kicad_sym')).read_text(encoding='utf-8')
    names = ['BQ25886RGE', 'R', 'C', 'L', 'Conn_01x02', 'Conn_01x04', 'PWR_FLAG']
    symbols = {n: block(lib, lib.index('(symbol "' + n + '"')) for n in names}
    cached = [s.replace('(symbol "' + n + '"', '(symbol "' + LIBNAME + ':' + n + '"', 1)
              for n, s in symbols.items()]
    out = ['(kicad_sch (version 20250901) (generator "raceremote")',
           f'(uuid "{uid("sheet")}") (paper "A3")',
           '(title_block (title "RaceRemote - BQ25886 charger core PROPOSAL") '
           '(date "2026-09-14") (rev "0.1") (company "vea.raceremote"))',
           '(lib_symbols ' + '\n'.join(cached) + ')']
    bom = []
    pin_positions = {}

    def prop(name, value, x, y, hide=False):
        return (f'(property {quote(name)} {quote(value)} (at {x:g} {y:g} 0) '
                + ('(hide yes) ' if hide else '') + '(effects (font (size 1.27 1.27))))')

    def symbol(name, ref, value, x, y, angle=0, purpose='', selection='Candidate; not purchased'):
        fp = re.search(r'\(property "Footprint" "([^"]*)"', symbols[name])[1] if name == 'BQ25886RGE' else ''
        pins = {}
        for m in re.finditer(r'\(pin (\S+).*?\(number "([^"]+)"', symbols[name], re.S):
            px, py, _ = map(float, re.search(r'\(at ([^)]+)', m[0])[1].split())
            a = math.radians(angle)
            pins[m[2]] = (round(x + px * math.cos(a) - py * math.sin(a), 4),
                          round(y - px * math.sin(a) - py * math.cos(a), 4))
        pin_positions[ref] = pins
        flag = name == 'PWR_FLAG'
        passive = name in ['R', 'C', 'L']
        out.append(f'(symbol (lib_id "{LIBNAME}:{name}") (at {x:g} {y:g} {angle}) '
                   f'(unit 1) (in_bom {"no" if flag else "yes"}) '
                   f'(on_board {"no" if flag else "yes"}) (dnp no) (uuid "{uid(ref)}") '
                   + prop('Reference', ref, x, y - (25.4 if ref == 'U101' else 5.08), flag)
                   + prop('Value', value, x + (10.16 if passive and angle == 0 else 0),
                          y + (0 if passive and angle == 0 else -27.94 if ref == 'U101' else -7.62), flag)
                   + prop('Footprint', fp, x, y, True)
                   + prop('Datasheet', 'https://www.ti.com/lit/gpn/bq25886' if ref == 'U101' else '', x, y, True)
                   + ''.join(f'(pin "{n}" (uuid "{uid(ref + "/" + n)}"))' for n in pins)
                   + f'(instances (project "charge-core" (path "/{uid("sheet")}" '
                   f'(reference "{ref}") (unit 1)))))')
        if not flag:
            bom.append({'reference': ref, 'value': value, 'purpose': purpose, 'selection': selection,
                        'footprint': fp, 'price_rub': '', 'price_status': 'not checked for this core BOM'})

    def wire(a, b):
        out.append(f'(wire (pts (xy {a[0]:g} {a[1]:g}) (xy {b[0]:g} {b[1]:g})) '
                   f'(stroke (width 0) (type default)) (uuid "{uid(str((a,b)))}"))')

    def label(net, x, y, angle=0):
        out.append(f'(global_label {quote(net)} (shape passive) (at {x:g} {y:g} {angle}) '
                   f'(effects (font (size 1.0 1.0)) (justify {"right" if angle == 180 else "left"})) '
                   f'(uuid "{uid(str((net,x,y)))}"))')

    def pin_net(ref, pin, net, dx=10.16, dy=0):
        a = pin_positions[ref][str(pin)]
        end = (round(a[0] + dx, 4), round(a[1] + dy, 4))
        wire(a, end)
        label(net, *end, 180 if dx < 0 else 0)

    def pair(name, ref, value, x, y, top, bottom, purpose):
        symbol(name, ref, value, x, y, purpose=purpose)
        pin_net(ref, 1, top, dx=0, dy=-7.62)
        pin_net(ref, 2, bottom, dx=0, dy=7.62)

    def note(text, x, y, size=1.27):
        out.append(f'(text {quote(text)} (at {x:g} {y:g} 0) '
                   f'(effects (font (size {size} {size})) (justify left top)) '
                   f'(uuid "{uid(text)}"))')

    symbol('BQ25886RGE', 'U101', 'BQ25886RGER', 101.6, 83.82,
           purpose='Standalone boost charger; no cell balancing')
    # Explicit pin numbers verified against SLUSD88A pp.4-5. Stacked duplicates
    # attach to the same wire in the unmodified KiCad symbol.
    for pin, net in [(23,'CHG_VBUS'), (24,'DP'), (1,'DM'), (5,'CHG_GND'),
                     (3,'CE_N'), (10,'ICHGSET'), (8,'ILIM')]:
        pin_net('U101', pin, net, dx=-12.7)
    for pin, net in [(2,'STAT_N'), (9,'PG_N'), (21,'PMID'), (17,'SW'), (12,'BTST'),
                     (15,'SYS_INTERNAL'), (13,'CHG_BAT'), (11,'REGN'), (7,'TS')]:
        pin_net('U101', pin, net, dx=12.7)
    pin_net('U101', 4, 'CHG_GND', dx=0, dy=7.62)
    x,y = pin_positions['U101']['6']
    out.append(f'(no_connect (at {x:g} {y:g}) (uuid "{uid("VSET-NC")}"))')

    # Input and converter energy storage. Ceff requirements, not just nominal
    # markings, must be checked before picking MPNs/footprints and routing.
    pair('C','C101','4.7u / 25V', 177.8,66.04,'CHG_VBUS','CHG_GND','VBUS; effective >=1uF')
    pair('C','C102','22u / 25V', 228.6,66.04,'PMID','CHG_GND','PMID input capacitance')
    pair('C','C103','22u / 25V', 279.4,66.04,'PMID','CHG_GND','PMID; input total effective >=10uF')
    pair('L','L101','1uH', 330.2,66.04,'PMID','SW','Boost inductor; saturation/ripple/MPN pending')
    pair('C','C104','47u / 25V', 177.8,114.3,'SYS_INTERNAL','CHG_GND','SYS effective total C104+C105 >=44uF')
    pair('C','C105','47u / 25V', 228.6,114.3,'SYS_INTERNAL','CHG_GND','SYS effective total C104+C105 >=44uF')
    pair('C','C106','22u / 25V', 279.4,114.3,'CHG_BAT','CHG_GND','BAT effective >=10uF at 8.4V')
    pair('C','C107','10u / 16V', 330.2,114.3,'REGN','CHG_GND','REGN effective >=4.7uF')
    pair('C','C108','47n / 25V', 101.6,149.86,'BTST','SW','Bootstrap: BTST to SW, never GND')

    pair('R','R101','953 / 1%', 50.8,167.64,'ICHGSET','CHG_GND','Nominal 250mA study setting; not LW approval')
    pair('R','R102','1.37k / 1%', 50.8,220.98,'ILIM','CHG_GND','Nominal input limit 810mA; not source qualification')
    pair('R','R103','10k / 1%', 177.8,167.64,'CHG_VBUS','CE_N','Charge disabled unless external open drain sinks CE_N')
    pair('R','R104','10k / 1%', 228.6,167.64,'CHG_VBUS','STAT_N','5V status pull-up; not directly 3.3V GPIO')
    pair('R','R105','10k / 1%', 279.4,167.64,'CHG_VBUS','PG_N','5V power-good pull-up; not charge permission')
    pair('R','R106','5.23k / 1%', 330.2,167.64,'REGN','TS','Reference thermistor divider upper leg')
    pair('R','R107','30.1k / 1%', 330.2,220.98,'TS','CHG_GND','Lower leg in parallel with external 103AT-2 NTC')

    connectors = [
        ('J101','Conn_01x04','QUALIFIED INPUT',177.8,226.06,['CHG_VBUS','CHG_GND','DP','DM'],
         'From source qualification/protection; not a raw USB or battery pinout'),
        ('J102','Conn_01x02','PROTECTED CHARGE PATH',254,220.98,['CHG_BAT','CHG_GND'],
         'To separately designed cell protection; no raw LW connection authorized'),
        ('J103','Conn_01x04','EXTERNAL CHARGE GATE',101.6,220.98,['CE_N','PG_N','STAT_N','CHG_GND'],
         'Open drain CE plus 5V status; gate/level conversion not implemented'),
        ('J104','Conn_01x02','EXTERNAL NTC 103AT-2',203.2,264.16,['TS','CHG_GND'],
         '10k thermistor at battery, not fixed resistor on controller')]
    for ref, name, value, x, y, nets, purpose in connectors:
        symbol(name,ref,value,x,y,purpose=purpose,selection='Logical interface; connector and footprint TBD')
        for i, net in enumerate(nets):
            pin_net(ref,i+1,net,dx=-7.62)

    # ERC source flags: external qualified supply/return; SW fed through L101.
    for i, (net,x) in enumerate([('CHG_VBUS',50.8),('CHG_GND',101.6),('SW',152.4)]):
        ref='#FLG'+str(101+i)
        symbol('PWR_FLAG',ref,'PWR_FLAG',x,259.08)
        pin_net(ref,1,net,dx=0,dy=5.08)

    note('Converter core only - source qualification, cell monitor/balancer and protection are separate.',25.4,20.32,2)
    note('CE_N pulled high: default charge OFF. OTG tied low. VSET floating: nominal 8.4V.\n'
         '250mA is a proposed bench setting; LW charge limits and cell pinout remain unverified.\n'
         'No traction load on SYS_INTERNAL. Do not connect raw LW battery to this unfinished assembly.',25.4,30.48)
    note('C104/C105: >=44uF effective total at operating voltage. Nominal values are placeholders for MPN selection.\n'
         'Ground: short analog returns and power-pad star per TI; high-current loops require PCB review.\n'
         'Flags describe power connectivity for ERC; they do not implement source checks or reverse protection.',25.4,279.4,1.0)
    out.append('(sheet_instances (path "/" (page "1"))) (embedded_fonts no))')
    (DEST/'charge-core.kicad_sch').write_text('\n'.join(out)+'\n',encoding='utf-8')
    (DEST/'sym-lib-table').write_text(
        f'(sym_lib_table (version 7) (lib (name "{LIBNAME}") (type "KiCad") '
        f'(uri "${{KIPRJMOD}}/{LIBNAME}.kicad_sym") (options "") (descr "KiCad 10.0.6 symbol subset")))\n',encoding='utf-8')
    with (DEST/'bom.csv').open('w',encoding='utf-8-sig',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=list(bom[0]));writer.writeheader();writer.writerows(bom)
    print('Generated',DEST/'charge-core.kicad_sch',len(bom),'components')


if __name__=='__main__':
    main()
