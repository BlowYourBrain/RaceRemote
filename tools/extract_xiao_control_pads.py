"""Read vendor KiCad copper-pad coordinates; do not redistribute vendor PCB."""
import hashlib
import json
from pathlib import Path
import re
import pcbnew as p

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/control-harness'


def main():
    vendor=next((ROOT/'build/xiao-control-harness/vendor').rglob('*.kicad_pcb'))
    board=p.LoadBoard(str(vendor));edge=next(f for f in board.GetFootprints()if f.GetReference()=='U9')
    ref=json.loads((ROOT/'docs/evidence/xiao-mount-v01.json').read_text())
    bounds=ref['source_bounding_boxes_mm']['37']
    cx=(bounds[0][0]+bounds[1][0])/2;cz=(bounds[0][2]+bounds[1][2])/2
    ox=p.ToMM(edge.GetPosition().x);oy=p.ToMM(edge.GetPosition().y)
    def transform(x,y):return [round(cx+oy-y,5),83.25,round(cz+x-ox,5)]
    pads={}
    for name,number,net in [('D0','1','D0{slash}A0'),('D1','2','D1{slash}A1'),('GND','13','GND')]:
        candidates=[a for a in edge.Pads()if a.GetNumber()==number and a.GetAttribute()==p.PAD_ATTRIB_PTH and abs(p.ToMM(a.GetSize().x)-1.4)<.001]
        assert len(candidates)==1
        a=candidates[0];assert a.GetNetname().endswith(net)
        x=p.ToMM(a.GetPosition().x);y=p.ToMM(a.GetPosition().y)
        pads[name]={'vendor_ref':'U9','pin':number,'net':a.GetNetname(),'vendor_xy_mm':[x,y],
                    'candidate_xyz_mm':transform(x,y),'pad_diameter_mm':1.4}
    pins=Path('C:/Users/Evgeny/.platformio/packages/framework-arduinoespressif32/variants/XIAO_ESP32S3/pins_arduino.h')
    text=pins.read_text()
    for name,num in [('D0',1),('D1',2)]:assert re.search(r'\b'+name+r'\s*=\s*'+str(num)+r'\s*;',text)
    firmware=ROOT.parent/'RC_CAR_ESP32/board/xiao_sense.h';text=firmware.read_text()
    assert 'motor_fi = D0;'in text and 'motor_bi = D1;'in text
    anchors={f.GetReference():transform(p.ToMM(f.GetPosition().x),p.ToMM(f.GetPosition().y))for f in board.GetFootprints()if f.GetReference()in ['ANT1','USB0']}
    # Orientation cross-check: USB0 footprint origin tracks shell's rear X edge;
    # ANT1 centre agrees with the named STEP U.FL to within0.2mm in the PCB plane.
    usb=ref['source_bounding_boxes_mm']['18'];ufl=ref['source_bounding_boxes_mm']['35']
    assert abs(anchors['USB0'][0]-usb[0][0])<.02
    assert abs(anchors['ANT1'][0]-(ufl[0][0]+ufl[1][0])/2)<.2
    assert abs(anchors['ANT1'][2]-(ufl[0][2]+ufl[1][2])/2)<.2
    report={'version':'0.1','date':'2026-09-15',
      'vendor_url':'https://files.seeedstudio.com/wiki/SeeedStudio-XIAO-ESP32S3/new-res/202003751_XIAO%20ESP32S3_v1.4_SCH%26PCB_260226.zip',
      'vendor_file':vendor.name,'archive_label_mismatch':'URL1.4 contains Sense1.5; actual purchased board revision unknown',
      'vendor_archive_sha256':hashlib.sha256((ROOT/'build/xiao-control-harness/vendor.zip').read_bytes()).hexdigest(),
      'vendor_pcb_sha256':hashlib.sha256(vendor.read_bytes()).hexdigest(),
      'transform_description':'carX=STEP PCB centreX+U9originY-padY; carZ=STEP centreZ+padX-U9originX; copper frontY83.25',
      'anchors_xyz_mm':anchors,'pads':pads,'physical_pad_identification':False,
      'firmware_profile_sha256':hashlib.sha256(firmware.read_bytes()).hexdigest(),
      'arduino_pin_header_sha256':hashlib.sha256(pins.read_bytes()).hexdigest(),
      'extractor_sha256':hashlib.sha256(Path(__file__).read_bytes()).hexdigest()}
    DEST.mkdir(exist_ok=True)
    (DEST/'xiao-pad-reference.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps(pads,indent=2))


if __name__=='__main__':main()
