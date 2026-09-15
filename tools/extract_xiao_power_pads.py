"""Extract rear battery solder pads from cached vendor PCB, preserving provenance."""
import hashlib
import json
from pathlib import Path
import pcbnew as p

ROOT=Path(__file__).resolve().parents[1]


def main():
    vendor=next((ROOT/'build/xiao-control-harness/vendor').rglob('*.kicad_pcb'))
    board=p.LoadBoard(str(vendor))
    fps={f.GetReference():f for f in board.GetFootprints()}
    reference=ROOT/'docs/evidence/xiao-mount-v01.json'
    boxes=json.loads(reference.read_text())['source_bounding_boxes_mm']
    bounds=boxes['37'];cx=(bounds[0][0]+bounds[1][0])/2;cz=(bounds[0][2]+bounds[1][2])/2
    origin=fps['U9'].GetPosition();ox=p.ToMM(origin.x);oy=p.ToMM(origin.y)
    def xyz(x,y):return [round(cx+oy-y,5),bounds[0][1],round(cz+x-ox,5)]
    pads={}
    for ref,net in [('BAT0','VBAT'),('GND0','GND')]:
        f=fps[ref];items=list(f.Pads());assert len(items)==1
        a=items[0];assert a.GetNumber()=='1' and a.GetNetname().split('/')[-1]==net
        assert a.GetLayerSet().Contains(p.B_Cu) and not a.GetLayerSet().Contains(p.F_Cu)
        x,y=p.ToMM(a.GetPosition().x),p.ToMM(a.GetPosition().y)
        pads[ref]={'pin':'1','net':a.GetNetname(),'side':'B.Cu','vendor_xy_mm':[x,y],
                   'vendor_pad_size_mm':[p.ToMM(a.GetSize().x),p.ToMM(a.GetSize().y)],
                   'candidate_xyz_mm':xyz(x,y)}
    # Same independent reference anchors as the earlier control-pad transform.
    usb=fps['USB0'].GetPosition();antenna=fps['ANT1'].GetPosition()
    usbxyz=xyz(p.ToMM(usb.x),p.ToMM(usb.y));antxyz=xyz(p.ToMM(antenna.x),p.ToMM(antenna.y))
    assert abs(usbxyz[0]-boxes['18'][0][0])<.02
    assert abs(antxyz[0]-(boxes['35'][0][0]+boxes['35'][1][0])/2)<.2
    assert abs(antxyz[2]-(boxes['35'][0][2]+boxes['35'][1][2])/2)<.2
    result={'date':'2026-09-15','contract':'XIAO-POWER-HARNESS-01 v0.1','pads':pads,
            'vendor_url':'https://files.seeedstudio.com/wiki/SeeedStudio-XIAO-ESP32S3/new-res/202003751_XIAO%20ESP32S3_v1.4_SCH%26PCB_260226.zip',
            'vendor_filename':vendor.name,'vendor_sha256':hashlib.sha256(vendor.read_bytes()).hexdigest(),
            'revision_warning':'URL1.4 contains PCB1.5; transformed to STEP2023; received hardware unknown',
            'transform':'carX=STEP PCB centreX+U9originY-padY; carZ=STEP centreZ+padX-U9originX; back plane carY82',
            'physical_identification':False,
            'source_sha256':{q.relative_to(ROOT).as_posix():hashlib.sha256(q.read_bytes()).hexdigest() for q in [reference,Path(__file__)]}}
    dest=ROOT/'cad/xiao-power-harness';dest.mkdir(exist_ok=True)
    (dest/'pad-reference.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps(pads,indent=2))


if __name__=='__main__':main()
