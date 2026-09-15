"""Independent pin oracle and native checks for the combined PCB proposal.

Run with KiCad Python. No fabrication outputs or hardware actions.
"""
import copy
import csv
import hashlib
import json
from pathlib import Path
import subprocess
import xml.etree.ElementTree as ET
import pcbnew as p

ROOT = Path(__file__).resolve().parents[1]
HW = ROOT / 'hardware/drive-power-carrier'
OUT = ROOT / 'docs/evidence/drive-power-carrier-v03'
CLI = ROOT / 'build/tooling/kicad-10.0.6/bin/kicad-cli.exe'

# Deliberately separate from the generator's net assignment and rename table.
EXPECTED = {
    'U1': {'1':'BI','2':'FI','3':'DRIVE_GND','4':'VM','5':'FO','6':'FO','7':'BO','8':'BO'},
    'R1': {'1':'CMD_FI','2':'FI'}, 'R2': {'1':'CMD_BI','2':'BI'},
    'R3': {'1':'FI','2':'DRIVE_GND'}, 'R4': {'1':'BI','2':'DRIVE_GND'},
    'C1': {'1':'VM','2':'DRIVE_GND'}, 'C2': {'1':'VM','2':'DRIVE_GND'},
    'H1': {'1':'CMD_FI'}, 'H2': {'1':'CMD_BI'}, 'H3': {'1':'DRIVE_GND'},
    'H4': {'1':'VM'}, 'H5': {'1':'DRIVE_GND'}, 'H6': {'1':'FO'}, 'H7': {'1':'BO'},
    'U2': {'1':'LDO_4V','2':'NC','3':'FB','4':'DRIVE_GND','5':'WAVE_5V',
           '6':'NC','7':'NC','8':'WAVE_5V','9':'DRIVE_GND'},
    'U3': {'1':'LDO_4V','2':'DRIVE_GND','3':'LDO_4V','4':'NC','5':'XIAO_BAT'},
    'R5': {'1':'LDO_4V','2':'FB'}, 'R6': {'1':'FB','2':'DRIVE_GND'},
    'R7': {'1':'LDO_4V','2':'DRIVE_GND'},
    'C3': {'1':'WAVE_5V','2':'DRIVE_GND'}, 'C4': {'1':'LDO_4V','2':'DRIVE_GND'},
    'C5': {'1':'LDO_4V','2':'DRIVE_GND'}, 'C6': {'1':'XIAO_BAT','2':'DRIVE_GND'},
    'H8': {'1':'WAVE_5V'}, 'H9': {'1':'XIAO_BAT'}, 'H10': {'1':'DRIVE_GND'},
}


def normalize(net):
    return 'NC' if net.startswith('unconnected-') else net


def pad_map(board):
    return {f.GetReference(): {a.GetNumber(): normalize(a.GetNetname()) for a in f.Pads()}
            for f in board.GetFootprints()}


def pad_geometry(board):
    return {(f.GetReference(), a.GetNumber()):
            (a.GetPosition().x, a.GetPosition().y, a.GetSize().x, a.GetSize().y,
             a.GetDrillSize().x, a.GetDrillSize().y, a.GetShape())
            for f in board.GetFootprints() for a in f.Pads()}


def track_geometry(board):
    return {(t.GetNetname(), t.GetLayer(), t.GetWidth(),
             tuple(sorted(((t.GetStart().x, t.GetStart().y), (t.GetEnd().x, t.GetEnd().y)))))
            for t in board.GetTracks() if not isinstance(t, p.PCB_VIA)}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    schfile, pcbfile = [str(HW / ('drive-power-carrier.' + ext)) for ext in ('kicad_sch','kicad_pcb')]
    for args in [
        ['sch','erc',schfile,'--format','json','-o',str(OUT/'erc.json')],
        ['pcb','drc',pcbfile,'--schematic-parity','--format','json','-o',str(OUT/'drc.json')],
        ['sch','export','netlist',schfile,'--format','kicadxml','-o',str(OUT/'netlist.xml')],
        ['sch','export','pdf',schfile,'-o',str(HW/'schematic.pdf')],
    ]:
        subprocess.run([str(CLI), *args], check=True)
    erc = json.loads((OUT/'erc.json').read_text())
    drc = json.loads((OUT/'drc.json').read_text())
    assert not any(s['violations'] for s in erc['sheets'])
    for key in ('violations','unconnected_items','schematic_parity'):
        assert not drc[key], (key,drc[key])
    sch = {}
    for net in ET.parse(OUT/'netlist.xml').findall('.//nets/net'):
        nodes = net.findall('node')
        for node in nodes:
            ref, pin = node.attrib['ref'], node.attrib['pin']
            if ref.startswith('#'): continue
            if normalize(net.attrib['name']) == 'NC':
                assert len(nodes) == 1 and '+no_connect' in node.attrib['pintype']
            assert pin not in sch.setdefault(ref,{})
            sch[ref][pin] = normalize(net.attrib['name'])
    board = p.LoadBoard(pcbfile)
    actual = pad_map(board)
    # Preserve every existing combined-board landing, not only the old TA6586 set.
    base_bytes=subprocess.check_output(['git','show','957e015:hardware/drive-power-carrier/drive-power-carrier.kicad_pcb'],cwd=ROOT)
    base_path=ROOT/'build/drive-power-carrier/previous-v02.kicad_pcb'
    base_path.write_bytes(base_bytes)
    baseline=p.LoadBoard(str(base_path))
    baseline_pads=pad_geometry(baseline)
    assert len(baseline_pads)==57
    assert all(pad_geometry(board)[key]==value for key,value in baseline_pads.items())
    assert track_geometry(board)==track_geometry(baseline)
    assert sch == EXPECTED, ('schematic',sch)
    assert actual == EXPECTED, ('PCB',actual)
    old = p.LoadBoard(str(ROOT/'hardware/motor-carrier/motor-carrier.kicad_pcb'))
    oldpads, newpads = pad_geometry(old), pad_geometry(board)
    assert len(oldpads) == 27
    assert all(newpads[key] == value for key,value in oldpads.items())
    removed = track_geometry(old) - track_geometry(board)
    assert len(removed) == 2 and all(t[:3] == ('VM',p.F_Cu,p.FromMM(1.2)) for t in removed), removed
    def segment(a,b):
        return tuple(sorted(tuple(p.FromMM(v+100) for v in xy) for xy in (a,b)))
    assert {t[3] for t in removed} == {segment((21.25,6),(21.25,13)),segment((21.25,13),(28,13))}
    spec = json.loads((HW/'geometry.json').read_text())
    assert len(spec['pads']) == len(newpads)
    for item in spec['pads']:
        loc = newpads[item['ref'],item['pin']][:2]
        assert item['xy_mm'] == [round(p.ToMM(v)-100,5) for v in loc]
        assert normalize(item['net']) == EXPECTED[item['ref']][item['pin']]
    fps = {f.GetReference(): f for f in board.GetFootprints()}
    # Independently transcribed 90-0174 Rev B, rotated/mirrored for bottom placement.
    u3 = fps['U3']
    expected_land = {'1':(1.25,-.95),'2':(1.25,0),'3':(1.25,.95),
                     '4':(-1.25,.95),'5':(-1.25,-.95)}
    for a in u3.Pads():
        delta = a.GetPosition()-u3.GetPosition()
        assert (round(p.ToMM(delta.x),5),round(p.ToMM(delta.y),5)) == expected_land[a.GetNumber()]
        assert (a.GetSize().x,a.GetSize().y) == (p.FromMM(1.3),p.FromMM(.55))
        assert a.GetShape() == p.PAD_SHAPE_RECT
    assert spec['new_component_positions']['U3']['body_mm'] == [3.5,3.5]
    assert spec['new_component_positions']['U3']['height_reserve_mm'] >= 1.45
    for ref,item in spec['new_component_positions'].items():
        f = fps[ref]
        assert item['center_mm'] == [round(p.ToMM(v)-100,5) for v in (f.GetPosition().x,f.GetPosition().y)]
        assert item['side'] == ('B' if f.GetLayer() == p.B_Cu else 'F')
    faults = [('swap motor inputs','U1','1','FI'), ('reverse MAX40200','U3','5','LDO_4V'),
              ('ground local diode enable','U3','3','DRIVE_GND'), ('break LDO exposed ground','U2','9','WAVE_5V'),
              ('bridge BAT to motor','H9','1','VM'), ('preload after diode','R7','1','XIAO_BAT'),
              ('XIAO return on motor supply','H10','1','VM')]
    for name,ref,pin,net in faults:
        bad = copy.deepcopy(actual); bad[ref][pin] = net
        assert bad != EXPECTED, name
    with (HW/'connections.csv').open('w',encoding='utf-8',newline='') as f:
        writer = csv.writer(f); writer.writerow(['reference','pin','net'])
        for ref in sorted(sch):
            for pin,net in sorted(sch[ref].items()): writer.writerow([ref,pin,net])
    motor_bom = {r['reference']:r for r in csv.DictReader((ROOT/'hardware/motor-carrier/bom.csv').open(encoding='utf-8-sig'))}
    with (HW/'bom.csv').open('w',encoding='utf-8',newline='') as f:
        writer = csv.writer(f); writer.writerow(['reference','value','footprint','side','mpn_reference','status'])
        for ref,fp in sorted(fps.items()):
            status = ('Integral PCB solder pad; not purchased connector' if ref.startswith('H')
                      else 'Existing inventory' if ref == 'U1' else 'Candidate; not purchased')
            mpn = motor_bom.get(ref,{}).get('mpn_reference','')
            if ref in ('U2','U3'): mpn = fp.GetValue()
            fid = fp.GetFPID()
            footprint = str(fid.GetLibNickname()) + ':' + str(fid.GetLibItemName())
            writer.writerow([ref,fp.GetValue(),footprint,'B' if fp.GetLayer()==p.B_Cu else 'F',mpn,status])
    for side in ('F','B'):
        args = [str(CLI),'pcb','export','svg',pcbfile,'--layers',f'{side}.Cu,{side}.SilkS,{side}.Fab,Edge.Cuts',
                '--mode-single','--fit-page-to-board','--exclude-drawing-sheet','-o',str(HW/('top.svg' if side=='F' else 'bottom.svg'))]
        if side=='B': args.append('--mirror')
        subprocess.run(args, check=True)
        svg = HW/('top.svg' if side=='F' else 'bottom.svg')
        svg.write_text('\n'.join(line.rstrip() for line in svg.read_text(encoding='utf-8').splitlines())+'\n',encoding='utf-8')
    paths = [q for q in HW.rglob('*') if q.is_file() and q.suffix not in ('.kicad_prl','.md')]
    paths += [Path(__file__), ROOT/'tools/build_drive_power_carrier.py']
    paths += [ROOT/'hardware/motor-carrier/motor-carrier.kicad_pcb',
              ROOT/'hardware/motor-carrier/motor-carrier.kicad_sch',
              ROOT/'hardware/xiao-logic-power/xiao-logic-power.kicad_sch']
    report = {'date':'2026-09-15','contract':'DRIVE-POWER-CARRIER-01 v0.3',
              'positions':len(actual),'pins_including_nc':sum(map(len,actual.values())),
              'working_nets':sorted({v for pins in actual.values() for v in pins.values()}-{'NC'}),
              'erc_violations':0,'drc_violations':0,'unconnected_items':0,'schematic_parity_issues':0,
              'kicad_version':drc['kicad_version'],'default_ignored_drc_checks':drc['ignored_checks'],
              'original_pad_geometries_unchanged':27,'original_track_segments_removed':2,
              'previous_v02_commit':'957e015','previous_v02_pcb_sha256':hashlib.sha256(base_bytes).hexdigest(),
              'previous_combined_pad_geometries_unchanged':57,'previous_combined_tracks_unchanged':True,
              'cad_pad_coordinates_and_new_component_centers_match_pcb':True,
              'u3_land_matches_reviewed_90_0174_rev_b':True,
              'u3_current_manufacturer_land_revision_confirmed':False,
              'oracle_mutations_detected':[f[0] for f in faults],
              'physical_tests':False,'thermal_qualification':False,'fabrication_released':False,
              'source_sha256':{q.relative_to(ROOT).as_posix():hashlib.sha256(q.read_bytes()).hexdigest() for q in paths}}
    (OUT/'summary.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print('PASS: 26 positions / 58 pins; ERC, DRC, parity zero; seven oracle faults detected. No physical qualification.')


if __name__ == '__main__': main()
