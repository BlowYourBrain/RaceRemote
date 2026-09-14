"""Check the proposed core against pin/interface contracts, not a charging model."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'hardware/charge-core'
EVIDENCE = ROOT / 'docs/evidence'
CLI = Path(sys.executable).with_name('kicad-cli.exe' if sys.platform == 'win32' else 'kicad-cli')

# SLUSD88A pp.4-5: explicit numeric pin contract, independent of library geometry.
IC_NETS = {
    1:'CORE_DM', 2:'STAT_N', 3:'CE_N', 4:'CHG_GND', 5:'CHG_GND',
    6:'unconnected-(U101-VSET-Pad6)', 7:'TS', 8:'ILIM', 9:'PG_N', 10:'ICHGSET',
    11:'REGN', 12:'BTST', 13:'CHG_BAT', 14:'CHG_BAT', 15:'SYS_INTERNAL',
    16:'SYS_INTERNAL', 17:'SW', 18:'SW', 19:'CHG_GND', 20:'CHG_GND',
    21:'PMID', 22:'PMID', 23:'CHG_VBUS', 24:'CORE_DP', 25:'CHG_GND'}


def run(*args):
    r = subprocess.run([str(CLI), *map(str,args)], cwd=ROOT, capture_output=True, text=True)
    if r.returncode:
        raise RuntimeError(r.stdout + r.stderr)


def netlist(path):
    r = ET.parse(path).getroot()
    pins = {(p.attrib['ref'],p.attrib['pin']): n.attrib['name']
            for n in r.findall('./nets/net') for p in n.findall('node')}
    values = {c.attrib['ref']: c.findtext('value') for c in r.findall('./components/comp')}
    return pins, values


def validate(pins, values):
    for pin, net in IC_NETS.items():
        assert pins.get(('U101',str(pin))) == net, ('U101',pin,net)
    assert len([r for r,p in pins if r == 'U101']) == 25
    pairs = {'L101':('PMID','SW'), 'C101':('CHG_VBUS','CHG_GND'),
             'C102':('PMID','CHG_GND'), 'C103':('PMID','CHG_GND'),
             'C104':('SYS_INTERNAL','CHG_GND'), 'C105':('SYS_INTERNAL','CHG_GND'),
             'C106':('CHG_BAT','CHG_GND'), 'C107':('REGN','CHG_GND'), 'C108':('BTST','SW'),
             'R101':('ICHGSET','CHG_GND'), 'R102':('ILIM','CHG_GND'),
             'R103':('CHG_VBUS','CE_N'), 'R104':('CHG_VBUS','STAT_N'),
             'R105':('CHG_VBUS','PG_N'), 'R106':('REGN','TS'), 'R107':('TS','CHG_GND'),
             'R108':('CORE_DP','CORE_DM'),
             'J101':('CHG_VBUS','CHG_GND'), 'J102':('CHG_BAT','CHG_GND'),
             'J103':('CE_N','PG_N','STAT_N','CHG_GND'), 'J104':('TS','CHG_GND')}
    for ref, nets in pairs.items():
        for i, net in enumerate(nets):
            assert pins.get((ref,str(i+1))) == net, (ref,i+1,net)
    assert set(values) == {'U101',*pairs}, 'Unexpected or missing part'
    for ref, value in {'R101':'953 / 1%', 'R102':'1.37k / 1%', 'R103':'10k / 1%',
                       'R106':'5.23k / 1%', 'R107':'30.1k / 1%', 'C108':'47n / 25V',
                       'L101':'1uH','R108':'0'}.items():
        assert values[ref] == value, (ref,value)
    # The two detector nets are completely local, not a configurable USB passthrough.
    for net,expected in {'CORE_DP':{('U101','24'),('R108','1')},
                         'CORE_DM':{('U101','1'),('R108','2')}}.items():
        assert {pin for pin,n in pins.items() if n==net}==expected,(net,'External detector connection')
    assert {pin for ref,pin in pins if ref=='J101'}=={'1','2'},'J101 must be power-only'


def main():
    erc = EVIDENCE/'charge-core-erc.json'
    run('sch','erc',DEST/'charge-core.kicad_sch','--format','json','--severity-all',
        '--exit-code-violations','-o',erc)
    erc_data = json.loads(erc.read_text())
    assert not any(s['violations'] for s in erc_data['sheets'])
    exported = DEST/'charge-core.net'
    run('sch','export','netlist',DEST/'charge-core.kicad_sch','--format','kicadxml','-o',exported)
    pins, values = netlist(exported)
    validate(pins, values)
    run('sch','export','pdf',DEST/'charge-core.kicad_sch','-o',DEST/'schematic.pdf')

    # These intentionally wrong connections can be electrically plausible to ERC.
    # Confirm the *exported* KiCad connectivity fails our interface contract.
    cases = [
        ('bootstrap_to_ground', 'SW', 'CHG_GND', '101.6 161.29 0'),
        ('lost_ce_pullup', 'CE_N', 'CHG_GND', '177.8 179.07 0'),
        ('battery_terminal_to_input', 'CHG_BAT', 'CHG_VBUS', '241.3 220.98 180'),
        ('local_dcp_bridge_to_ground','CORE_DM','CHG_GND','50.8 125.73 0')]
    faults=[]
    original = (DEST/'charge-core.kicad_sch').read_text(encoding='utf-8')
    for name, old, new, position in cases:
        folder=ROOT/'build/charge-core/negative-controls'/name
        folder.mkdir(parents=True,exist_ok=True)
        token=f'(global_label "{old}" (shape passive) (at {position})'
        assert original.count(token)==1, (name,token)
        mutated=original.replace(token,token.replace('"'+old+'"','"'+new+'"',1))
        path=folder/'charge-core.kicad_sch';path.write_text(mutated,encoding='utf-8')
        for file in ['charge-core.kicad_pro','RaceRemote_Charge.kicad_sym','sym-lib-table','fp-lib-table']:
            shutil.copyfile(DEST/file,folder/file)
        output=folder/'fault.net';output.unlink(missing_ok=True)
        run('sch','export','netlist',path,'--format','kicadxml','-o',output)
        try:
            validate(*netlist(output))
        except AssertionError as error:
            faults.append({'mutation':name,'detected':True,'failed_contract':str(error),
                           'netlist_sha256':hashlib.sha256(output.read_bytes()).hexdigest()})
        else:
            raise AssertionError('Fault was not detected: '+name)

    # Arithmetic only: no SPICE, thermal or battery model is implied.
    current=953/3810
    ntc_nom=1/(1/30100+1/10000)
    open_min=30100*.99/(30100*.99+5230*1.01)
    assert open_min>.7375  # Above maximum cold threshold, resistor-only model.
    arithmetic={
        'ichg_nominal_A':current,
        'ichg_illustrative_A_with_1pct_R_and_25pct_IC':[current*.99*.75,current*1.01*1.25],
        'ichg_accuracy_scope':'TI table: nominal 250mA, VBAT 6.2/7.6V, TJ 0..85C; not an all-cycle bound',
        'precharge_nominal_A':max(current/10,.03),
        'termination_nominal_A':max(current/10,.01),
        'trickle_below_4p4V_pack_typical_A':.1,
        'ilim_nominal_A':1110/1370,
        'ilim_tolerance_scope':'No guaranteed 810mA-setting bound inferred from adjacent table rows',
        'ts_ratio_at_10k_ntc_nominal':ntc_nom/(5230+ntc_nom),
        'ts_ratio_open_ntc_min_with_1pct_resistors':open_min,
        'ts_ratio_short_ntc':0,
        'ts_scope':'Resistor network only; no proof of LW temperature limits, NTC mounting or dynamic faults',
        'ce_voltage_at_4p3V_typical_internal_pulldown':4.3*900000/(900000+10000)}
    files=[DEST/n for n in ['charge-core.kicad_sch','charge-core.kicad_pro','RaceRemote_Charge.kicad_sym',
                           'charge-core.net','bom.csv','schematic.pdf']]
    files += [erc,ROOT/'tools/build_charge_core.py',Path(__file__)]
    files += sorted((DEST/'Charge_Core.pretty').glob('*.kicad_mod'))
    report={'date':'2026-09-14','contract':'CHARGE-CORE-01 v0.2','kicad_version':erc_data['kicad_version'],
            'erc_violations':0,'ic_pins_checked':25,'logical_components':len(values),
            'interface_and_passive_pin_checks':True,'local_detector_net_isolation_checked':True,
            'fault_injections':faults,'arithmetic':arithmetic,
            'sha256':{str(f.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(f.read_bytes()).hexdigest() for f in files},
            'not_verified':['PCB and placement','Cell protection/balancing and charge permission generator',
                            'USB input qualification/protection and 3.3V level interfaces',
                            'Actual local DCP detection/ICO behavior and external detector under physical USB sources',
                            'Capacitor DC bias and inductor saturation','LW pinout, charge ratings and full cycle',
                            'JEITA warm/cool behavior discrepancies in TI Rev A','Thermal/physical assembly']}
    (EVIDENCE/'charge-core-verification.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:report[k] for k in ['erc_violations','ic_pins_checked','logical_components','fault_injections']}))


if __name__=='__main__':main()
