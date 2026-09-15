"""Conductive-ground topology only; no transistor, charger or battery simulation."""
import hashlib
import itertools
import json
from pathlib import Path
from collections import deque
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]


def path(edges,start,finish):
    adjacency={}
    for a,b,label in edges:
        adjacency.setdefault(a,[]).append((b,label))
        adjacency.setdefault(b,[]).append((a,label))
    queue=deque([(start,[])])
    visited={start}
    while queue:
        node,steps=queue.popleft()
        if node==finish:return steps
        for other,label in adjacency.get(node,[]):
            if other not in visited:
                visited.add(other);queue.append((other,steps+[{'from':node,'to':other,'connection':label}]))
    return None


def topology(protection_closed,drive_on,charge_attached,service_attached,shared_host,
             charge_ground='protected',raw_monitor_wire=False):
    edges=[
        ('PACK_P','PROTECTED_P','proposed common positive; fuse and positive cutoff not modelled'),
        ('PROTECTED_N','DRIVE_GND','proposed dedicated supply return to carrier H5'),
        ('DRIVE_GND','XIAO_GND','carrier H10 and GND0 wire'),
        ('DRIVE_GND','WAVE_OUT_GND','carrier H3 and Waveshare output wire'),
        ('DRIVE_GND','WAVE_IN_GND','proposed input return; wiring not yet built'),
        ('XIAO_GND','USB_SERVICE_GND','native XIAO service ground'),
        ('CHARGER_GND','USB_CHARGE_GND','proposed non-isolated charger input ground'),
        ('CHARGER_GND','PROTECTED_N' if charge_ground=='protected' else 'RAW_N','charger ground assignment'),
        ('RAW_N','RAW_MONITOR_GND','battery-side monitor ground; no signal crossing assigned')]
    if protection_closed:edges.append(('RAW_N','PROTECTED_N','ideal protection return contact'))
    if drive_on:edges.append(('PROTECTED_P','VM','ideal positive drive switch'))
    if charge_attached:edges.append(('USB_CHARGE_GND','HOST_CHARGE_GND','charge USB cable'))
    if service_attached:edges.append(('USB_SERVICE_GND','HOST_SERVICE_GND','service USB cable'))
    if shared_host:edges.append(('HOST_CHARGE_GND','HOST_SERVICE_GND','external common host/hub ground'))
    if raw_monitor_wire:edges.append(('RAW_MONITOR_GND','DRIVE_GND','incorrect direct raw-side reference wire'))
    return edges


def main():
    source=ROOT/'docs/evidence/drive-power-carrier-v03/netlist.xml'
    nets=ET.parse(source).findall('.//nets/net')
    ground=next(n for n in nets if n.attrib['name']=='DRIVE_GND')
    ground_pins={(n.attrib['ref'],n.attrib['pin']) for n in ground.findall('node')}
    assert {('H3','1'),('H5','1'),('H10','1'),('U1','3')}.issubset(ground_pins)
    for ref,expected in [('H4','VM'),('H8','WAVE_5V'),('H9','XIAO_BAT')]:
        found=[n.attrib['name'] for n in nets for pin in n.findall('node') if pin.attrib['ref']==ref]
        assert found==[expected],(ref,found)
    rows=[]
    for state in itertools.product([False,True],repeat=5):
        closed,on,charge,service,shared=state
        edges=topology(*state)
        return_path=path(edges,'RAW_N','PROTECTED_N')
        supply_path=path(edges,'PACK_P','VM')
        assert (return_path is not None)==closed
        assert (supply_path is not None)==on
        rows.append(dict(protection_closed=closed,drive_on=on,charge_usb=charge,service_usb=service,
                         shared_host=shared,raw_to_protected_ground_path=return_path,
                         pack_positive_to_VM_path=supply_path))
    # A concrete miswire can defeat an otherwise open return contact through a
    # real cable path. Keep drive OFF here: opening only positive does not isolate grounds.
    example_state=(False,False,True,True,True)
    wrong=path(topology(*example_state,charge_ground='raw'),'RAW_N','PROTECTED_N')
    assert wrong and any(s['connection']=='external common host/hub ground' for s in wrong)
    no_service=path(topology(False,False,True,False,True,charge_ground='raw'),'RAW_N','PROTECTED_N')
    assert no_service is None
    wrong_monitor=path(topology(False,False,False,False,False,raw_monitor_wire=True),'RAW_N','PROTECTED_N')
    assert wrong_monitor
    # Exhaustively identify, rather than hide, the state dependence of this miswire.
    bad_cases=[]
    for on,charge,service,shared in itertools.product([False,True],repeat=4):
        bypass=path(topology(False,on,charge,service,shared,charge_ground='raw'),'RAW_N','PROTECTED_N')
        assert (bypass is not None)==(charge and service and shared)
        if bypass is not None:bad_cases.append(dict(drive_on=on,charge_usb=charge,service_usb=service,shared_host=shared))
    inputs=[Path(__file__),source,ROOT/'docs/xiao-usb-power.md',ROOT/'hardware/pack-balancer/README.md',ROOT/'docs/charge-sense-paths.md']
    report={'contract':'POWER-GROUND-01 v0.1','date':'2026-09-15',
            'status':'proposed boundary condition; complete power circuit NOT qualified',
            'model':'undirected galvanic wiring and ideal contacts; not voltage/current or protection-FET simulation',
            'carrier_ground_pins_verified':sorted([list(x) for x in ground_pins]),
            'proposed_protected_bus_cases':rows,'raw_charger_ground_counterexample':wrong,
            'raw_ground_miswire_bypass_states':bad_cases,'direct_raw_monitor_counterexample':wrong_monitor,
            'omitted_paths':['battery sources and midpoint conductors','charger CBSET/MID/BAT internal paths',
                             'MOSFET body diodes, leakage, shutdown and transient behaviour',
                             'sensor signal ESD/protection paths','shield/chassis connections not yet assigned',
                             'positive-rail converter and GPIO backfeed, fuse/current limits'],
            'physical_tests':False,'ready_to_wire_LW':False,
            'source_sha256':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs}}
    out=ROOT/'docs/evidence/power-ground-v01.json'
    out.write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Verified 32 proposed contact states and16 miswire states; USB and raw-monitor bypass paths exposed.')
    print('Complete protection/power integration: NOT qualified.')


if __name__=='__main__':main()
