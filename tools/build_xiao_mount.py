"""Native STEP contact review and removable XIAO holder, proposal only."""
import json
import hashlib
import subprocess
from pathlib import Path
from OCP.STEPControl import STEPControl_Reader
from OCP.IFSelect import IFSelect_RetDone
from OCP.TopExp import TopExp_Explorer
from OCP.TopAbs import TopAbs_SOLID
from OCP.Bnd import Bnd_Box
from OCP.BRepBndLib import BRepBndLib
import numpy as np
import trimesh
from build_packaging_v05 import intersect, mesh, shifted, tube

ROOT=Path(__file__).resolve().parents[1]
CAD=ROOT/'cad/xiao-mount'
SCAD='C:/Program Files/OpenSCAD/openscad.com'

def main():
    CAD.mkdir(exist_ok=True)
    src=ROOT/'cad/xiao-mount.scad'
    generated={}
    for n in ['cradle','cap','screws','usb_access','cradle_print','cap_print','tray_clearance','stage_screws']:
        p=CAD/f'{n}.stl'
        r=subprocess.run([SCAD,'-o',str(p),'-D',f'part="{n}"',str(src)],capture_output=True,text=True,check=True)
        assert 'WARNING' not in r.stderr and 'ERROR' not in r.stderr,r.stderr
        generated[n]=mesh(p)
        assert generated[n].is_volume,n
    assert len(generated['cradle'].split())==1
    assert len(generated['cap'].split())==1
    step=ROOT/'cad/components/xiao-sense-2023.step'
    previous=json.loads((ROOT/'docs/evidence/candidate-fit.json').read_text())
    assert hashlib.sha256(step.read_bytes()).hexdigest()==previous['source_step_sha256']
    offset=np.array(previous['translation_mm'])+[0,3,0]
    reader=STEPControl_Reader();assert reader.ReadFile(str(step))==IFSelect_RetDone
    reader.TransferRoots(); explorer=TopExp_Explorer(reader.OneShape(),TopAbs_SOLID)
    boxes={}
    while explorer.More():
        bounds=Bnd_Box();BRepBndLib.AddOptimal_s(explorer.Current(),bounds,False,False)
        v=np.array(bounds.Get()).reshape(2,3)+offset
        boxes[str(len(boxes))]=trimesh.creation.box(v[1]-v[0],transform=trimesh.transformations.translation_matrix(v.mean(axis=0)))
        explorer.Next()
    assert len(boxes)==103
    assert np.allclose(boxes['37'].extents,[20.955,1.25,17.78],atol=.005)
    checked=['cradle','cap','screws','stage_screws','usb_access']
    hits={n:{i:intersect(generated[n],b) for i,b in boxes.items()} for n in checked}
    hits={n:{i:v for i,v in d.items() if v>.001}for n,d in hits.items()}
    d=ROOT/'cad/packaging-v05'
    obstacles={n:mesh(d/f'{n}.stl') for n in ['buck','buck_plug','tray','pads','driver','charge','antenna','antenna_support','frame-relief-proposal','servo-installed']}
    for n in ['usb-base','usb-pcb','usb-connector','usb-components','cover','adjustable-battery','adjustable-main_fasteners']:
        obstacles[n]=mesh(d/f'inputs/{n}.stl')
    obstacles['tray']=generated['tray_clearance']
    obstacles['motor']=mesh(ROOT/'cad/components/motor-installed.stl')
    raw=json.loads((ROOT/'docs/evidence/packaging-v05.json').read_text())['wire_corridors']
    corridors={n:{'radius_mm':p['radius_mm'],'points_mm':p['points_mm']}for n,p in raw.items()}
    # Revised reservations: actual named U.FL source is at the left/lower PCB corner.
    # GPIO and actuator endpoints remain allocations, not a physical pin assignment.
    corridors['wire-antenna']['points_mm']=[[14.59,85.2,26.16],[10,85.2,28],[10,85.2,42],
                                          [10,76.5,42],[22,74.5,42],[43,70,42],[47.5,63,42]]
    corridors['wire-servo']['points_mm']=[[35,68,38.6],[44,68,39.5],[44,90,39.5],
                                        [44,92,34],[45,95,19],[44,89,16],[43,78,14]]
    corridors['wire-control']['points_mm']=[[33.9,85,38],[39,84,38],[41,79,40],[36,74,39]]
    for n,p in corridors.items():obstacles[n]=tube(p['points_mm'],p['radius_mm'])
    for n in ['wire-antenna','wire-servo','wire-control']:obstacles[n].export(CAD/f'{n}.stl')
    fixed_hits={n:{f:intersect(generated[n],fm)for f,fm in obstacles.items()}for n in checked}
    fixed_hits={n:{f:v for f,v in h.items()if v>.001}for n,h in fixed_hits.items()}
    cavity=mesh(d/'inputs/adjustable-cavity.stl')
    outside={n:float(trimesh.boolean.difference([generated[n],cavity],engine='manifold').volume)for n in checked if n!='usb_access'}
    wire_checks={}
    for n in ['wire-antenna','wire-servo','wire-control']:
        m=obstacles[n]
        wire_checks[n]={'holder':{f:intersect(m,generated[f])for f in checked},
                        'pcb_boxes':{i:intersect(m,b)for i,b in boxes.items()},
                        'other_obstacles':{f:intersect(m,fm)for f,fm in obstacles.items()if f!=n},
                        'outside_body_mm3':float(trimesh.boolean.difference([m,cavity],engine='manifold').volume),
                        'length_mm':float(np.linalg.norm(np.diff(corridors[n]['points_mm'],axis=0),axis=1).sum())}
        wire_checks[n]['pcb_boxes']={i:v for i,v in wire_checks[n]['pcb_boxes'].items()if v>.001}
        wire_checks[n]['other_obstacles']={i:v for i,v in wire_checks[n]['other_obstacles'].items()if v>.001}
    cap_path=[]
    for xyz in [[0,0,float(z)]for z in np.arange(0,1.101,.1)]+[[0,float(y),1.1]for y in np.arange(.5,8.01,.5)]:
        moved=generated['cap'].copy();moved.apply_translation(xyz)
        cp={n:intersect(moved,m)for n,m in {**boxes,**obstacles,'cradle':generated['cradle']}.items()}
        cap_path.append({'offset_mm':xyz,'overlaps_mm3':{n:v for n,v in cp.items()if v>.001}})
    report={'version':'0.1','date':'2026-09-15','physical_fit_verified':False,
            'step_solids':len(boxes),'reference_pcb_index':37,'reference_usb_shell_index':18,
            'step_translation_mm':offset.tolist(),'source_step_sha256':previous['source_step_sha256'],
            'named_reference_ufl':{'name':'U.FL-R-SMT-1_10_ v1','solid_index':35,
                                   'native_bounds_mm':[[-8.4517,1,-12.3881],[-5.4517,2.25,-9.2881]],
                                   'identification':'STEPCAF/XCAF label traversal; no claim of exact received board'},
            'source_bounding_boxes_mm':{i:b.bounds.tolist()for i,b in boxes.items()},
            'reference_bbox_intersections_mm3':hits,'packaging_intersections_mm3':fixed_hits,
            'outside_body_mm3':outside,
            'cradle_cap_overlap_mm3':intersect(generated['cradle'],generated['cap']),
            'usb_cradle_overlap_mm3':intersect(generated['usb_access'],generated['cradle']),
            'print_bounds_mm':{n:generated[n].bounds.tolist()for n in ['cradle_print','cap_print']},
            'revised_corridor_definitions':{n:corridors[n]for n in ['wire-antenna','wire-servo','wire-control']},
            'revised_wire_checks':wire_checks,'cap_removal_samples':cap_path,
            'limitations':['Reference STEP from 2023; actual OV3660 revision not measured',
                           'Individual solid boxes are conservative, not exact component contact checks',
                           'Pads, screw lengths, thread/nut retention, clamping force and print tolerances unqualified',
                           'USB plug allocation estimated; body must be removed; wire routing may need adjustment',
                           'Full adjustment/body-removal checks not transferred from the prior holder']}
    report['source_hashes']={p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()
                             for p in [src,ROOT/'cad/xiao-mount-assembly.scad',Path(__file__),step,
                                       ROOT/'docs/evidence/packaging-v05.json',ROOT/'docs/evidence/candidate-fit.json']}
    (ROOT/'docs/evidence/xiao-mount-v01.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print(json.dumps({k:report[k]for k in ['reference_bbox_intersections_mm3','packaging_intersections_mm3','outside_body_mm3','cradle_cap_overlap_mm3','usb_cradle_overlap_mm3','print_bounds_mm']},indent=2),flush=True)
    assert all(not v for v in hits.values()) and all(not v for v in fixed_hits.values())
    assert max(outside.values())<.001
    assert report['cradle_cap_overlap_mm3']<.001 and report['usb_cradle_overlap_mm3']<.001
    assert all(not p['overlaps_mm3'] for p in cap_path)
    for n,v in wire_checks.items():
        assert not v['pcb_boxes'] and max(v['holder'].values())<.001 and v['outside_body_mm3']<.001,n
        # Deliberate connection-end allocations touch their destination reserves.
        permitted={'antenna'} if n=='wire-antenna' else {'driver'}
        assert set(v['other_obstacles']) <= permitted,(n,v['other_obstacles'])

if __name__=='__main__':main()
