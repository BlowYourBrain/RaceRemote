"""Generate a charge-only PI3USB9201 interface proposal; MCU policy is external."""
import csv
import json
from pathlib import Path
import re
import uuid
from build_usb_port_schematic import block

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'hardware/usb-detector'
LIB='RaceRemote_Detector'

def uid(value):
    return str(uuid.uuid5(uuid.NAMESPACE_URL,'https://vea.raceremote/usb-detector/'+str(value)))

def q(value):return json.dumps(str(value),ensure_ascii=False)

def make_ic():
    # DS41358 Rev3-2 p2: connector side is D+/D-, NOT USB+/USB-.
    pins=[(12,'VDD','power_in',-25.4,20.32,0),(3,'SCL','input',-25.4,10.16,0),
          (4,'SDA','bidirectional',-25.4,0,0),(5,'~{INTB}','open_collector',-25.4,-10.16,0),
          (11,'~{ENB}','input',-25.4,-20.32,0),(8,'D+','bidirectional',25.4,20.32,180),
          (7,'D-','bidirectional',25.4,10.16,180),(1,'USB+','bidirectional',25.4,0,180),
          (2,'USB-','bidirectional',25.4,-10.16,180),(6,'NC','no_connect',25.4,-20.32,180),
          (9,'GND','power_in',-5.08,-33.02,90),(10,'ADDR','input',5.08,-33.02,90)]
    out=['(symbol "PI3USB9201ZTA" (pin_names (offset 1.016)) (in_bom yes) (on_board yes)',
         '(property "Reference" "U" (at 0 30.48 0) (effects (font (size 1.27 1.27))))',
         '(property "Value" "PI3USB9201ZTA" (at 0 33.02 0) (effects (font (size 1.27 1.27))))',
         '(property "Footprint" "" (at 0 0 0) (effects (font (size 1.27 1.27)) hide))',
         '(symbol "PI3USB9201ZTA_0_1" (rectangle (start -17.78 27.94) (end 17.78 -27.94) '
         '(stroke (width 0.254) (type default)) (fill (type background))))',
         '(symbol "PI3USB9201ZTA_1_1"']
    for n,name,typ,x,y,a in pins:
        out.append(f'(pin {typ} line (at {x:g} {y:g} {a}) (length {5.08 if n in [9,10] else 7.62}) '
                   f'(name {q(name)} (effects (font (size 1.27 1.27)))) '
                   f'(number "{n}" (effects (font (size 1.27 1.27)))))')
    return '\n'.join(out)+'))'

def main():
    DEST.mkdir(parents=True,exist_ok=True)
    source=(ROOT/'hardware/charge-core/RaceRemote_Charge.kicad_sym').read_text(encoding='utf-8')
    symbols={n:block(source,source.index('(symbol "'+n+'"')) for n in ['R','C','Conn_01x02','Conn_01x04','PWR_FLAG']}
    symbols['PI3USB9201ZTA']=make_ic()
    (DEST/(LIB+'.kicad_sym')).write_text('(kicad_symbol_lib (version 20251024) (generator "raceremote")\n'
        +'\n'.join(symbols.values())+')\n',encoding='utf-8')
    cache=[s.replace('(symbol "'+n+'"','(symbol "'+LIB+':'+n+'"',1) for n,s in symbols.items()]
    out=['(kicad_sch (version 20250901) (generator "raceremote")',f'(uuid "{uid("sheet")}") (paper "A3")',
         '(title_block (title "RaceRemote - PI3USB9201 detector PROPOSAL") '
         '(date "2026-09-14") (rev "0.1") (company "vea.raceremote"))','(lib_symbols '+'\n'.join(cache)+')']
    positions={};bom=[]
    def prop(name,value,x,y,hide=False):
        return f'(property {q(name)} {q(value)} (at {x:g} {y:g} 0) '+('(hide yes) ' if hide else '')+'(effects (font (size 1.27 1.27))))'
    def symbol(name,ref,value,x,y,purpose=''):
        pins={}
        for m in re.finditer(r'\(pin (\S+).*?\(number "([^"]+)"',symbols[name],re.S):
            px,py,_=map(float,re.search(r'\(at ([^)]+)',m[0])[1].split());pins[m[2]]=(round(x+px,4),round(y-py,4))
        positions[ref]=pins;flag=name=='PWR_FLAG';passive=name in ['R','C']
        out.append(f'(symbol (lib_id "{LIB}:{name}") (at {x:g} {y:g} 0) (unit 1) '
                   f'(in_bom {"no" if flag else "yes"}) (on_board {"no" if flag else "yes"}) '
                   f'(dnp no) (uuid "{uid(ref)}") '+prop('Reference',ref,x,y-(33.02 if ref=='U301' else 5.08),flag)
                   +prop('Value',value,x+(12.7 if passive else 0),y if passive else y-(35.56 if ref=='U301' else 7.62),flag)
                   +prop('Footprint','',x,y,True)+prop('Datasheet','https://www.diodes.com/datasheet/download/PI3USB9201.pdf' if ref=='U301' else '',x,y,True)
                   +''.join(f'(pin "{n}" (uuid "{uid(ref+"/"+n)}"))' for n in pins)
                   +f'(instances (project "usb-detector" (path "/{uid("sheet")}" (reference "{ref}") (unit 1)))))')
        if not flag:bom.append({'reference':ref,'value':value,'purpose':purpose,'footprint':'',
                               'selection':'Candidate; MCU, footprints and physical integration pending'})
    def net(ref,pin,name,dx=12.7,dy=0):
        a=positions[ref][str(pin)];b=(round(a[0]+dx,4),round(a[1]+dy,4))
        out.append(f'(wire (pts (xy {a[0]:g} {a[1]:g}) (xy {b[0]:g} {b[1]:g})) '
                   f'(stroke (width 0) (type default)) (uuid "{uid((ref,pin,"wire"))}"))')
        out.append(f'(global_label {q(name)} (shape passive) (at {b[0]:g} {b[1]:g} {180 if dx<0 else 0}) '
                   f'(effects (font (size 1 1)) (justify {"right" if dx<0 else "left"})) (uuid "{uid((ref,pin,"net"))}"))')
    def pair(name,ref,value,x,y,top,bottom,purpose):
        symbol(name,ref,value,x,y,purpose);net(ref,1,top,0,-7.62);net(ref,2,bottom,0,7.62)
    def note(text,x,y,size=1.27):
        out.append(f'(text {q(text)} (at {x:g} {y:g} 0) (effects (font (size {size} {size})) '
                   f'(justify left top)) (uuid "{uid(text)}"))')

    symbol('PI3USB9201ZTA','U301','PI3USB9201ZTAEX',127,91.44,'BC1.2/legacy detector; charge-only sink, not a power source')
    for pin,name in [(12,'AUX_3V3'),(3,'BC_SCL'),(4,'BC_SDA'),(5,'BC_INT_N'),(11,'BC_EN_N')]:net('U301',pin,name,-12.7)
    for pin,name in [(8,'PORT_DP'),(7,'PORT_DM')]:net('U301',pin,name)
    for pin in [9,10]:net('U301',pin,'USB_GND',0,7.62)
    for pin in [1,2,6]:
        x,y=positions['U301'][str(pin)];out.append(f'(no_connect (at {x:g} {y:g}) (uuid "{uid(("NC",pin))}"))')
    pair('C','C301','100n / 16V',254,91.44,'AUX_3V3','USB_GND','Local bypass; included in total AUX load capacitance')
    pair('R','R301','47k / 1%',50.8,180.34,'AUX_3V3','BC_EN_N','Default detector disabled; MCU open drain sinks to enable')
    pair('R','R302','4.7k / 1%',152.4,180.34,'AUX_3V3','BC_SCL','Single 3.3V I2C pull-up; 100kHz proposal')
    pair('R','R303','4.7k / 1%',254,180.34,'AUX_3V3','BC_SDA','Single 3.3V I2C pull-up; no 5V MCU pull-ups')
    pair('R','R304','10k / 1%',355.6,180.34,'AUX_3V3','BC_INT_N','Active-low interrupt; not source permission')
    for ref,name,value,x,y,nets,purpose in [
        ('J301','Conn_01x02','FROM USB AUX',63.5,243.84,['AUX_3V3','USB_GND'],'3.3V from TPS709 upstream of main gate; not battery or raw USB'),
        ('J302','Conn_01x02','USB PORT DATA',177.8,243.84,['PORT_DP','PORT_DM'],'External connector D+/D- only; no CORE_DP/CORE_DM path'),
        ('J303','Conn_01x04','TO SOURCE MCU',330.2,243.84,['BC_SCL','BC_SDA','BC_INT_N','BC_EN_N'],'Controller must also share J301 ground; no MCU or SOURCE_ALLOW generator implemented')]:
        symbol(name,ref,value,x,y,purpose)
        for i,n in enumerate(nets):net(ref,i+1,n,-7.62)
    for i,(name,x) in enumerate([('AUX_3V3',304.8),('USB_GND',355.6)]):
        ref='#FLG'+str(301+i);symbol('PWR_FLAG',ref,'PWR_FLAG',x,121.92);net(ref,1,name,0,5.08)
    note('USB-BC-01 v0.1 - PI3USB9201 candidate interface; source MCU still required',25.4,20.32,2)
    note('ADDR tied to GND: 7-bit address 0x5F (write 0xBE/read 0xBF on wire). Default ENB HIGH disables detector.\n'
         'USB+/USB- are unused device-side pins. Physical connector uses D+/D- pins 8/7. Never join them to BQ25886 core.\n'
         'INTB and read-to-clear status are events, not durable source permission. See README for initialization and fault contract.',25.4,30.48)
    note('AUX: proposed 3.0..3.6V. 100kHz I2C requires measured bus capacitance/rise time. No second supply on AUX.\n'
         'Schematic/ERC only: controller, CC detection, brownout/watchdog, ESD/layout and physical source tests are open.',25.4,279.4,1)
    out.append('(sheet_instances (path "/" (page "1"))) (embedded_fonts no))')
    (DEST/'usb-detector.kicad_sch').write_text('\n'.join(out)+'\n',encoding='utf-8')
    (DEST/'usb-detector.kicad_pro').write_text(json.dumps({'meta':{'filename':'usb-detector.kicad_pro','version':3}},indent=2)+'\n')
    (DEST/'sym-lib-table').write_text(f'(sym_lib_table (version 7) (lib (name "{LIB}") (type "KiCad") '
                                   f'(uri "${{KIPRJMOD}}/{LIB}.kicad_sym") (options "") (descr "Own PI3USB symbol and attributed passives")))\n')
    (DEST/'fp-lib-table').write_text('(fp_lib_table (version 7))\n')
    with (DEST/'bom.csv').open('w',encoding='utf-8-sig',newline='') as f:
        w=csv.DictWriter(f,fieldnames=list(bom[0]));w.writeheader();w.writerows(bom)
    print('Generated',len(bom),'components')

if __name__=='__main__':main()
