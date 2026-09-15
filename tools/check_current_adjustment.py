"""Independent stage sweep for current XIAO cradle and integral harness saddles."""
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
from build_packaging_v05 import intersect,mesh,shifted
from layout_zcar_battery import box

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'cad/packaging-v05'
XIAO=ROOT/'cad/xiao-mount'
GUIDES=ROOT/'cad/harness-guides'

def main():
    inputs={}
    def read(p):
        inputs[p.relative_to(ROOT).as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
        m=mesh(p);assert m.is_volume,p
        return m
    e={n:read(BASE/f'{n}.stl')for n in ['buck','buck_plug','pads','driver','charge']}
    e['tray']=read(GUIDES/'tray.stl')
    e['main_screws']=read(BASE/'inputs/adjustable-main_fasteners.stl')
    c={n:read(XIAO/f'{n}.stl')for n in ['cradle','screws','stage_screws']}
    c['cap']=read(GUIDES/'cap.stl')
    evidence=ROOT/'docs/evidence/xiao-mount-v01.json'
    inputs[evidence.relative_to(ROOT).as_posix()]=hashlib.sha256(evidence.read_bytes()).hexdigest()
    component_bounds=json.loads(evidence.read_text())['source_bounding_boxes_mm']
    # Union retains the per-component bounding approximation without using one large box.
    c['camera_components']=trimesh.boolean.union([box(np.array(v)[1]-np.array(v)[0],np.array(v).mean(axis=0))for v in component_bounds.values()],engine='manifold')
    assert c['camera_components'].is_volume
    fixed={n:read(BASE/f'{n}.stl')for n in ['frame-relief-proposal','servo-installed','antenna','antenna_support']}
    for n in ['cover','usb-base','usb-pcb','usb-connector','usb-components','adjustable-battery','adjustable-support','adjustable-guards','adjustable-band']:
        fixed[n]=read(BASE/f'inputs/{n}.stl')
    fixed['motor']=read(ROOT/'cad/components/motor-installed.stl')
    cavity=read(BASE/'inputs/adjustable-cavity.stl')
    service=read(XIAO/'usb_access.stl')
    def prep(group,delta):
        moved={n:shifted(m,delta)for n,m in group.items()}
        hits={f'{n}/{f}':intersect(m,fm)for n,m in moved.items()for f,fm in fixed.items()}
        outside={n:float(trimesh.boolean.difference([m,cavity],engine='manifold').volume)for n,m in moved.items()}
        return moved,hits,outside
    ec={i:prep(e,i)for i in range(-3,9)}
    cc={i:prep(c,i-3)for i in range(-3,9)}
    rows=[]
    for ei,(em,eh,eo)in ec.items():
        for ci,(cm,ch,co)in cc.items():
            hits=eh|ch|{f'{en}/{cn}':intersect(ev,cv)for en,ev in em.items()for cn,cv in cm.items()}
            outside=eo|co
            # Both shifted camera screws must remain on their existing slot centre-lines.
            slot_ok=(75<=81+ci-3<=86 and 75<=77+ci-3<=86)
            usb=shifted(service,ci-3)
            uh={n:intersect(usb,m)for n,m in (fixed|em|cm).items()}
            rows.append({'electronics_mm':ei,'camera_mm':ci,'slot_constraints_clear':slot_ok,
                         'mechanical_clear':slot_ok and max(hits.values())<.001,
                         'hypothetical_body_clear':max(outside.values())<.001,
                         'service_usb_solids_clear':max(uh.values())<.001,
                         'overlaps_mm3':{k:round(v,5)for k,v in hits.items()if v>.001},
                         'outside_body_mm3':{k:round(v,5)for k,v in outside.items()if v>.001},
                         'service_usb_hits_mm3':{k:round(v,5)for k,v in uh.items()if v>.001},
                         'wire_routing_verified':ei==0 and ci==3})
    good=[p for p in rows if p['mechanical_clear']and p['hypothetical_body_clear']]
    nominal=next(p for p in rows if p['electronics_mm']==0 and p['camera_mm']==3)
    inputs[Path(__file__).relative_to(ROOT).as_posix()]=hashlib.sha256(Path(__file__).read_bytes()).hexdigest()
    report={'version':'0.6','date':'2026-09-15','physical_fit_verified':False,'sample_step_mm':1,
            'nominal':nominal,'feasible_pose_count':len(good),'poses':rows,
            'feasible_pairs_mm':[[p['electronics_mm'],p['camera_mm']]for p in good],
            'source_sha256':inputs,
            'limits':['Rigid solids only; routed wires qualified only for nominal 0/+3 by prior harness audit',
                      'Wire length, flexibility and fastening must be rechecked after moving stages, with power off',
                      'No continuous stage-travel or all-position body-removal proof',
                      '103 per-solid STEP boxes are conservative; actual OV3660 revision unmeasured',
                      'Charger/driver and mating USB shell are allocations; body is hypothetical',
                      'Slot range uses screw centres, no clamp-force or printer tolerance qualification']}
    (ROOT/'docs/evidence/current-adjustment-v06.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({'nominal':nominal,'feasible_pairs':report['feasible_pairs_mm']},indent=2),flush=True)
    assert nominal['mechanical_clear']and nominal['hypothetical_body_clear']and nominal['service_usb_solids_clear']
    assert not next(p for p in rows if p['electronics_mm']==0 and p['camera_mm']==0)['slot_constraints_clear']

if __name__=='__main__':main()
