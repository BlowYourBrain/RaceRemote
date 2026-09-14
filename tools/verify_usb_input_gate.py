"""Verify exported connectivity and calculations, not analog or USB compliance."""
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT/'hardware/usb-input-gate'
EVIDENCE = ROOT/'docs/evidence'
CLI = Path(sys.executable).with_name('kicad-cli.exe' if sys.platform=='win32' else 'kicad-cli')

# Independent numeric interface expectations: TI SLVSCJ0F Table 4-1.
IC_NETS = {1:'GATED_VBUS',2:'ILIM_SET',3:'INPUT_FAULT_N',4:'GATE_EN',
           5:'USB_GND',6:'USB_RAW_VBUS',7:'USB_GND'}
# SBVS186H Table 5-1: plain TPS709 DBV. EN float is explicit TI mode, not forgotten wiring.
LDO_NETS = {1:'USB_RAW_VBUS',2:'USB_GND',3:'unconnected-(U202-EN-Pad3)',
            4:'unconnected-(U202-NC-Pad4)',5:'AUX_3V3'}
CONNECTIONS = {
    'C201':('USB_RAW_VBUS','USB_GND'), 'C202':('GATED_VBUS','USB_GND'),
    'R201':('ILIM_SET','USB_GND'), 'R202':('GATE_EN','USB_GND'),
    'R203':('SOURCE_ALLOW','GATE_EN'), 'R204':('AUX_3V3','INPUT_FAULT_N'),
    'J201':('USB_RAW_VBUS','USB_GND'), 'J202':('GATED_VBUS','USB_GND'),
    'J203':('AUX_3V3','SOURCE_ALLOW','INPUT_FAULT_N','USB_GND'),
    'C203':('USB_RAW_VBUS','USB_GND'),'C204':('AUX_3V3','USB_GND')}


def run(*args):
    result = subprocess.run([str(CLI),*map(str,args)],cwd=ROOT,capture_output=True,text=True)
    if result.returncode:raise RuntimeError(result.stdout+result.stderr)


def read_netlist(path):
    root=ET.parse(path).getroot()
    pins={(p.attrib['ref'],p.attrib['pin']):n.attrib['name']
          for n in root.findall('./nets/net') for p in n.findall('node')}
    values={c.attrib['ref']:c.findtext('value') for c in root.findall('./components/comp')}
    return pins,values


def validate(pins,values):
    for pin,net in IC_NETS.items():assert pins.get(('U201',str(pin)))==net,('U201',pin,net)
    assert len([ref for ref,pin in pins if ref=='U201'])==7
    for pin,net in LDO_NETS.items():assert pins.get(('U202',str(pin)))==net,('U202',pin,net)
    assert len([ref for ref,pin in pins if ref=='U202'])==5
    for ref,nets in CONNECTIONS.items():
        for i,net in enumerate(nets):assert pins.get((ref,str(i+1)))==net,(ref,i+1,net)
    assert set(values)=={'U201','U202',*CONNECTIONS}
    for ref,value in {'U201':'TPS25200DRVR','R201':'82.5k / 1%','R202':'47k / 1%',
                      'R203':'4.7k / 1%','R204':'10k / 1%',
                      'C201':'100n / 50V','C202':'22u / 25V',
                      'U202':'TPS70933DBVR','C203':'1u / 50V','C204':'4.7u / 16V'}.items():
        assert values[ref]==value,(ref,value)


def current_estimate(r_kohm):
    # SLVSCJ0F Eq1 is an approximation, not guaranteed specs for every R value.
    return [97399/(r_kohm*1.01)**1.015-30,98322/r_kohm**1.003,
            96754/(r_kohm*.99)**.985+30]


def main():
    sch=DEST/'usb-input-gate.kicad_sch'
    erc=EVIDENCE/'usb-input-gate-erc.json'
    run('sch','erc',sch,'--format','json','--severity-all','--exit-code-violations','-o',erc)
    erc_data=json.loads(erc.read_text())
    assert not any(s['violations'] for s in erc_data['sheets'])
    output=DEST/'usb-input-gate.net'
    run('sch','export','netlist',sch,'--format','kicadxml','-o',output)
    pins,values=read_netlist(output);validate(pins,values)
    run('sch','export','pdf',sch,'-o',DEST/'usb-input-gate.pdf')

    # Export actual mutated schematics, rather than mutating only Python maps.
    original=sch.read_text(encoding='utf-8')
    faults=[]
    for name,old,new,position in [
        ('enable_pulldown_to_supply','USB_GND','AUX_3V3','50.8 168.91 0'),
        ('output_terminal_bypasses_gate','GATED_VBUS','USB_RAW_VBUS','170.18 254 180'),
        ('ilim_return_to_output','USB_GND','GATED_VBUS','330.2 102.87 0'),
        ('aux_depends_on_closed_gate','USB_RAW_VBUS','GATED_VBUS','121.92 203.2 180'),
        ('aux_output_cap_on_raw_input','AUX_3V3','USB_RAW_VBUS','254 196.85 0'),
        ('ldo_enable_tied_to_raw',None,'USB_RAW_VBUS','134.62 213.36')]:
        if old is None:
            token=f'(no_connect (at {position}) '
            replacement=(f'(global_label "{new}" (shape passive) (at {position} 180) '
                         '(effects (font (size 1 1)) (justify right)) ')
        else:
            token=f'(global_label "{old}" (shape passive) (at {position})'
            replacement=token.replace('"'+old+'"','"'+new+'"',1)
        assert original.count(token)==1,(name,token)
        changed=original.replace(token,replacement)
        folder=ROOT/'build/usb-input-gate/negative-controls'/name
        folder.mkdir(parents=True,exist_ok=True)
        path=folder/sch.name;path.write_text(changed,encoding='utf-8')
        for file in ['usb-input-gate.kicad_pro','RaceRemote_Input.kicad_sym','sym-lib-table','fp-lib-table']:
            shutil.copyfile(DEST/file,folder/file)
        exported=folder/'fault.net'
        run('sch','export','netlist',path,'--format','kicadxml','-o',exported)
        try:validate(*read_netlist(exported))
        except AssertionError as error:
            faults.append({'mutation':name,'detected':True,'failed_contract':str(error),
                           'netlist_sha256':hashlib.sha256(exported.read_bytes()).hexdigest()})
        else:raise AssertionError('Undetected wiring error: '+name)

    # Check equation units against TI's worked 42.2k example, pp16-17.
    example=current_estimate(42.2)
    assert abs(example[0]-2130)<1 and abs(example[2]-2479)<1
    current=current_estimate(82.5)
    assert current[0]>1000 and current[2]<1300
    en_open_max=2e-6*47000*1.01
    rdown_min=47000*.99; rseries_max=4700*1.01
    parallel=1/(1/rdown_min+1/rseries_max)
    en_high_min=2.8*rdown_min/(rdown_min+rseries_max)-2e-6*parallel
    en_low_max=.3+2e-6*4700*1.01  # Conservative with pull-down omitted in this bound.
    assert en_open_max<.6 and en_low_max<.6 and en_high_min>1.9
    arithmetic={'ilim_estimated_min_nom_max_mA':current,'rilim_kohm':82.5,'resistor_tolerance_percent':1,
                'scope':'TI Eq1 approximation plus resistor tolerance; not measured or an absolute transient limit',
                'en_open_max_V_leakage_only':en_open_max,'en_low_max_V_for_source_low_0p3V':en_low_max,
                'en_high_min_V_for_source_high_2p8V':en_high_min,
                'enable_scope':'Static resistor model; IEN +/-2uA table conditions. No timing, PCB leakage or brownout proof',
                'proposed_min_source_budget_A':1.5,'proposed_other_input_budget_A':.05,
                'estimated_headroom_A':1.5-current[2]/1000-.05,
                'aux_proposed_load_budget_A':.01,
                'aux_estimated_static_voltage_V':[3.3*.99-.01-.05,3.3*1.01+.01+.05],
                'aux_voltage_scope':'Conservative sum of SBVS186H DC/line/load rows at their test conditions; not a guaranteed cross-condition bound',
                'aux_nominal_dissipation_5p25V_10mA_W':(5.25-3.3)*.01,
                'aux_nominal_dissipation_20V_10mA_W':(20-3.3)*.01,
                'aux_illustrative_delta_T_at_20V_C':(20-3.3)*.01*212.1,
                'aux_thermal_scope':'Pass-element estimate with TI JEDEC thetaJA; not enclosure/PCB thermal proof or 20V charging permission',
                'aux_current_limit_scope':'10mA is a design load budget only; TPS709 internal limit is 200..500mA at TI test conditions',
                'aux_raw_input_nominal_cap_uF':1.1,
                'aux_cap_scope':'C201+C203 nominal only; whole-port capacitance, charge and peak inrush not verified'}
    assert arithmetic['estimated_headroom_A']>.15
    files=[DEST/n for n in ['usb-input-gate.kicad_sch','usb-input-gate.kicad_pro',
                           'RaceRemote_Input.kicad_sym','usb-input-gate.net','bom.csv','usb-input-gate.pdf']]
    files += [erc,ROOT/'tools/build_usb_input_gate.py',Path(__file__)]
    report={'date':'2026-09-14','contract':'USB-INPUT-GATE-01 v0.2 / USB-AUX-01 v0.1',
            'kicad_version':erc_data['kicad_version'],'erc_violations':0,'ic_pins_including_EP_checked':12,
            'logical_components':len(values),'fault_injections':faults,'arithmetic':arithmetic,
            'sha256':{str(f.relative_to(ROOT)).replace('\\','/'):hashlib.sha256(f.read_bytes()).hexdigest() for f in files},
            'not_verified':['PCB/thermal footprint and manufacture','Source classification, brownout/permit policy and physical AUX operation',
                            'USB total current/inrush and CC/BC1.2 integration','Core 5.5V vs clamp/transient compatibility',
                            'Output backfeed isolation and battery protection','Capacitor MPN and effective capacitance',
                            'Physical default-off, overcurrent, OVP and charge-cycle measurements']}
    (EVIDENCE/'usb-input-gate-verification.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:report[k] for k in ['erc_violations','ic_pins_including_EP_checked','logical_components','fault_injections','arithmetic']}))


if __name__=='__main__':main()
