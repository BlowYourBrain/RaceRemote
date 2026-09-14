"""Generate the TPS25200 input-gate proposal; source permission is external."""
import csv
import json
from pathlib import Path
import re
import uuid

from build_usb_port_schematic import block

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'hardware/usb-input-gate'
LIB = 'RaceRemote_Input'


def uid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'https://vea.raceremote/usb-input-gate/' + name))


def q(value):
    return json.dumps(str(value), ensure_ascii=False)


def make_ic():
    # Own symbol. Numeric pinout: TI SLVSCJ0F Table 4-1, top view.
    # EP is numbered 7 in this schematic, not an additional package lead.
    pins = [(6,'IN','power_in',-22.86,10.16,0),
            (4,'EN','input',-22.86,0,0),
            (1,'OUT','power_out',22.86,10.16,180),
            (3,'~{FAULT}','open_collector',22.86,0,180),
            (2,'ILIM','passive',22.86,-10.16,180),
            (5,'GND','power_in',-5.08,-22.86,90),
            (7,'EP','power_in',5.08,-22.86,90)]
    out = ['(symbol "TPS25200DRV" (pin_names (offset 1.016)) (in_bom yes) (on_board yes)',
           '(property "Reference" "U" (at 0 22.86 0) (effects (font (size 1.27 1.27))))',
           '(property "Value" "TPS25200DRV" (at 0 20.32 0) (effects (font (size 1.27 1.27))))',
           '(property "Footprint" "" (at 0 0 0) (effects (font (size 1.27 1.27)) hide))',
           '(symbol "TPS25200DRV_0_1" (rectangle (start -17.78 17.78) (end 17.78 -17.78) '
           '(stroke (width 0.254) (type default)) (fill (type background))))',
           '(symbol "TPS25200DRV_1_1"']
    for n, name, typ, x, y, angle in pins:
        out.append(f'(pin {typ} line (at {x:g} {y:g} {angle}) (length 5.08) '
                   f'(name {q(name)} (effects (font (size 1.27 1.27)))) '
                   f'(number "{n}" (effects (font (size 1.27 1.27)))))')
    return '\n'.join(out) + '))'


def main():
    DEST.mkdir(parents=True, exist_ok=True)
    source = (ROOT/'hardware/charge-core/RaceRemote_Charge.kicad_sym').read_text(encoding='utf-8')
    names = ['R','C','Conn_01x02','Conn_01x04','PWR_FLAG']
    symbols = {n: block(source, source.index('(symbol "'+n+'"')) for n in names}
    symbols['TPS25200DRV'] = make_ic()
    (DEST/(LIB+'.kicad_sym')).write_text('(kicad_symbol_lib (version 20251024) '
        '(generator "raceremote")\n'+'\n'.join(symbols.values())+')\n',encoding='utf-8')
    cached = [s.replace('(symbol "'+n+'"','(symbol "'+LIB+':'+n+'"',1) for n,s in symbols.items()]
    out = ['(kicad_sch (version 20250901) (generator "raceremote")',
           f'(uuid "{uid("sheet")}") (paper "A3")',
           '(title_block (title "RaceRemote - USB input gate PROPOSAL") '
           '(date "2026-09-14") (rev "0.1") (company "vea.raceremote"))',
           '(lib_symbols '+'\n'.join(cached)+')']
    positions, bom = {}, []

    def prop(name,value,x,y,hide=False):
        return (f'(property {q(name)} {q(value)} (at {x:g} {y:g} 0) '
                + ('(hide yes) ' if hide else '') + '(effects (font (size 1.27 1.27))))')

    def symbol(name,ref,value,x,y,purpose=''):
        pins = {}
        for m in re.finditer(r'\(pin (\S+).*?\(number "([^"]+)"',symbols[name],re.S):
            px,py,_ = map(float,re.search(r'\(at ([^)]+)',m[0])[1].split())
            pins[m[2]] = (round(x+px,4),round(y-py,4))
        positions[ref] = pins
        flag = name == 'PWR_FLAG'
        passive = name in ['R','C']
        out.append(f'(symbol (lib_id "{LIB}:{name}") (at {x:g} {y:g} 0) (unit 1) '
                   f'(in_bom {"no" if flag else "yes"}) (on_board {"no" if flag else "yes"}) '
                   f'(dnp no) (uuid "{uid(ref)}") '
                   + prop('Reference',ref,x,y-(25.4 if ref=='U201' else 5.08),flag)
                   + prop('Value',value,x+(12.7 if passive else 0),
                          y if passive else y-(27.94 if ref=='U201' else 7.62),flag)
                   + prop('Footprint','',x,y,True)
                   + prop('Datasheet','https://www.ti.com/lit/gpn/tps25200' if ref=='U201' else '',x,y,True)
                   + ''.join(f'(pin "{n}" (uuid "{uid(ref+"/"+n)}"))' for n in pins)
                   + f'(instances (project "usb-input-gate" (path "/{uid("sheet")}" '
                   f'(reference "{ref}") (unit 1)))))')
        if not flag:
            bom.append({'reference':ref,'value':value,'purpose':purpose,
                        'footprint':'','selection':'Proposal; MPN/PCB pending' if ref!='U201'
                        else 'TPS25200DRVR candidate; WSON-6 2x2mm plus EP; not purchased'})

    def net(ref,pin,name,dx=12.7,dy=0):
        a=positions[ref][str(pin)]; b=(round(a[0]+dx,4),round(a[1]+dy,4))
        out.append(f'(wire (pts (xy {a[0]:g} {a[1]:g}) (xy {b[0]:g} {b[1]:g})) '
                   f'(stroke (width 0) (type default)) (uuid "{uid(str((ref,pin)))}"))')
        out.append(f'(global_label {q(name)} (shape passive) (at {b[0]:g} {b[1]:g} '
                   f'{180 if dx<0 else 0}) (effects (font (size 1 1)) '
                   f'(justify {"right" if dx<0 else "left"})) (uuid "{uid(ref+"net"+str(pin))}"))')

    def pair(name,ref,value,x,y,top,bottom,purpose):
        symbol(name,ref,value,x,y,purpose)
        net(ref,1,top,0,-7.62); net(ref,2,bottom,0,7.62)

    def note(text,x,y,size=1.27):
        out.append(f'(text {q(text)} (at {x:g} {y:g} 0) (effects (font (size {size} {size})) '
                   f'(justify left top)) (uuid "{uid(text)}"))')

    symbol('TPS25200DRV','U201','TPS25200DRVR',152.4,91.44,'Gated USB power with current limit and OVP clamp')
    for pin,name in [(6,'USB_RAW_VBUS'),(4,'GATE_EN')]:net('U201',pin,name,-12.7)
    for pin,name in [(1,'GATED_VBUS'),(3,'INPUT_FAULT_N'),(2,'ILIM_SET')]:net('U201',pin,name)
    for pin in [5,7]:net('U201',pin,'USB_GND',0,10.16)
    pair('C','C201','100n / 50V',50.8,91.44,'USB_RAW_VBUS','USB_GND','Local input bypass; effective >=100nF')
    pair('C','C202','22u / 25V',279.4,91.44,'GATED_VBUS','USB_GND','Output bypass proposal; DC bias/transient sizing pending')
    pair('R','R201','82.5k / 1%',330.2,91.44,'ILIM_SET','USB_GND','Eq1 estimate 1.064..1.295A; nominal 1.176A')
    pair('R','R202','47k / 1%',50.8,157.48,'GATE_EN','USB_GND','Default OFF when external permit is absent/high impedance')
    pair('R','R203','4.7k / 1%',152.4,157.48,'SOURCE_ALLOW','GATE_EN','Series input to enable; external 3.3V logic')
    pair('R','R204','10k / 1%',254,157.48,'AUX_3V3','INPUT_FAULT_N','Open-drain fault pull-up to external always-available logic rail')
    for ref,name,value,x,y,nets,purpose in [
        ('J201','Conn_01x02','RAW USB POWER',63.5,223.52,['USB_RAW_VBUS','USB_GND'],'From passive port; logical interface, not a USB-C pinout'),
        ('J202','Conn_01x02','TO INPUT INTEGRATION',182.88,223.52,['GATED_VBUS','USB_GND'],'No battery/traction/service-USB backfeed; voltage compatibility still open'),
        ('J203','Conn_01x04','SOURCE LOGIC',292.1,223.52,['AUX_3V3','SOURCE_ALLOW','INPUT_FAULT_N','USB_GND'],'External source detector/policy; no source recognition on this sheet')]:
        symbol(name,ref,value,x,y,purpose)
        for i,n in enumerate(nets):net(ref,i+1,n,-7.62)
    for i,(name,x) in enumerate([('USB_RAW_VBUS',50.8),('USB_GND',152.4)]):
        ref='#FLG'+str(201+i);symbol('PWR_FLAG',ref,'PWR_FLAG',x,259.08);net(ref,1,name,0,5.08)
    note('USB-INPUT-GATE-01 v0.1 - source-qualified power switch only',25.4,20.32,2)
    note('SOURCE_ALLOW must remain LOW until a >=1.5A source budget is verified. No SDP/unknown-source permission.\n'
         'AUX_3V3 and source policy are external and must start before this gate. Battery charge permission is separate.\n'
         'No PCB or hardware validation. This is not a complete 2S charger.',25.4,30.48)
    note('OFF discharges OUT internally; never backfeed from the 2S pack, traction or another USB supply.\n'
         '5.55V clamp is conditional, NOT a universal transient ceiling: existing core contract ends at 5.5V.\n'
         'EP shown as pin 7; connect to GND and verify thermal footprint before PCB layout.',25.4,275,1)
    out.append('(sheet_instances (path "/" (page "1"))) (embedded_fonts no))')
    (DEST/'usb-input-gate.kicad_sch').write_text('\n'.join(out)+'\n',encoding='utf-8')
    (DEST/'usb-input-gate.kicad_pro').write_text(json.dumps({'meta':{'filename':'usb-input-gate.kicad_pro','version':3}},indent=2)+'\n')
    (DEST/'sym-lib-table').write_text(f'(sym_lib_table (version 7) (lib (name "{LIB}") (type "KiCad") '
        f'(uri "${{KIPRJMOD}}/{LIB}.kicad_sym") (options "") (descr "Own IC and attributed KiCad passives")))\n')
    (DEST/'fp-lib-table').write_text('(fp_lib_table (version 7))\n')
    with (DEST/'bom.csv').open('w',encoding='utf-8-sig',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=list(bom[0]));writer.writeheader();writer.writerows(bom)
    print('Generated',len(bom),'components:',DEST)


if __name__ == '__main__':main()
