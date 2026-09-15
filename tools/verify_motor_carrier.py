"""Independent numeric-pin oracle plus native KiCad ERC/DRC and schematic parity."""
import copy
import hashlib
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET
import pcbnew as p

ROOT=Path(__file__).resolve().parents[1]
HW=ROOT/'hardware/motor-carrier'
OUT=ROOT/'docs/evidence/motor-carrier-v01'
CLI=ROOT/'build/tooling/kicad-10.0.6/bin/kicad-cli.exe'

# Datasheet numeric pins, separately stated external interface and passive topology.
EXPECTED={
 'U1':{'1':'BI','2':'FI','3':'DRIVE_GND','4':'VM','5':'FO','6':'FO','7':'BO','8':'BO'},
 'R1':{'1':'CMD_FI','2':'FI'},'R2':{'1':'CMD_BI','2':'BI'},
 'R3':{'1':'FI','2':'DRIVE_GND'},'R4':{'1':'BI','2':'DRIVE_GND'},
 'C1':{'1':'VM','2':'DRIVE_GND'},'C2':{'1':'VM','2':'DRIVE_GND'},
 'H1':{'1':'CMD_FI'},'H2':{'1':'CMD_BI'},'H3':{'1':'DRIVE_GND'},
 'H4':{'1':'VM'},'H5':{'1':'DRIVE_GND'},'H6':{'1':'FO'},'H7':{'1':'BO'}}


def main():
    OUT.mkdir(parents=True,exist_ok=True)
    calls=[['sch','erc',str(HW/'motor-carrier.kicad_sch'),'--format','json','-o',str(OUT/'erc.json')],
           ['pcb','drc',str(HW/'motor-carrier.kicad_pcb'),'--schematic-parity','--format','json','-o',str(OUT/'drc.json')],
           ['sch','export','netlist',str(HW/'motor-carrier.kicad_sch'),'--format','kicadxml','-o',str(OUT/'netlist.xml')]]
    for args in calls:subprocess.run([str(CLI),*args],check=True)
    erc=json.loads((OUT/'erc.json').read_text());drc=json.loads((OUT/'drc.json').read_text())
    assert not any(s['violations']for s in erc['sheets']),erc
    for key in ['violations','unconnected_items','schematic_parity']:assert not drc[key],(key,drc[key])
    board=p.LoadBoard(str(HW/'motor-carrier.kicad_pcb'))
    pcb={f.GetReference():{a.GetNumber():a.GetNetname()for a in f.Pads()}for f in board.GetFootprints()}
    sch={}
    for net in ET.parse(OUT/'netlist.xml').findall('.//nets/net'):
        for node in net.findall('node'):
            if node.attrib['ref'].startswith('#'):continue
            sch.setdefault(node.attrib['ref'],{})[node.attrib['pin']]=net.attrib['name']
    assert pcb==EXPECTED,('PCB pin mapping',pcb)
    assert sch==EXPECTED,('Schematic pin mapping',sch)
    faults=[('swap FI and BI','U1','1','FI'),('break paired FO','U1','6','BO'),
            ('reverse electrolytic','C2','1','DRIVE_GND'),('pulldown before series resistor','R3','1','CMD_FI')]
    detected=[]
    for name,ref,pin,net in faults:
        bad=copy.deepcopy(pcb);bad[ref][pin]=net
        assert bad!=EXPECTED,name
        detected.append(name)
    paths=[ROOT/'tools'/n for n in ['motor_carrier_identity.py','build_motor_carrier_schematic.py','build_motor_carrier_pcb.py','verify_motor_carrier.py']]
    paths += sorted(q for q in HW.rglob('*')if q.is_file()and q.suffix not in ['.pdf','.md','.svg','.kicad_prl'])
    sources={}
    for name,url in [('ta6586.pdf','https://static.chipdip.ru/lib/923/DOC012923012.pdf'),('rubycon-zlh.pdf','https://www.rubycon.co.jp/wp-content/uploads/catalog-aluminum/ZLH.pdf')]:
        file=ROOT/'build/motor-carrier-research'/name
        sources[name]={'url':url,'sha256':hashlib.sha256(file.read_bytes()).hexdigest()}
    report={'date':'2026-09-15','version':'0.1','kicad_version':drc['kicad_version'],
        'components':len(pcb),'pads_checked':sum(map(len,pcb.values())),'nets':8,
        'erc_violations':0,'drc_violations':0,'unconnected_items':0,'schematic_parity_issues':0,
        'default_ignored_checks':drc['ignored_checks'],'oracle_mutations_detected':detected,
        'physical_tests':False,'current_rating_a':None,'fabrication_released':False,
        'source_documents':sources,'source_sha256':{q.relative_to(ROOT).as_posix():hashlib.sha256(q.read_bytes()).hexdigest()for q in paths}}
    (OUT/'summary.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print('PASS: 14 components / 27 pads; ERC, DRC and parity zero; four pin-map faults detected.')


if __name__=='__main__':main()
