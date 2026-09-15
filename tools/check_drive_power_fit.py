"""Check added component envelopes against the current nominal car assembly."""
import hashlib
import json
from pathlib import Path

import numpy as np
import trimesh
from OCP.BRepPrimAPI import BRepPrimAPI_MakeBox, BRepPrimAPI_MakeCylinder
from OCP.BRepAlgoAPI import BRepAlgoAPI_Cut
from OCP.BRep import BRep_Builder
from OCP.TopoDS import TopoDS_Compound
from OCP.gp import gp_Pnt, gp_Ax2, gp_Dir
from OCP.BRepMesh import BRepMesh_IncrementalMesh
from OCP.StlAPI import StlAPI_Writer
from OCP.STEPControl import STEPControl_Writer, STEPControl_AsIs
from OCP.IFSelect import IFSelect_RetDone
from build_packaging_v05 import intersect, mesh, tube
from layout_zcar_battery import box

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/drive-power-carrier'
EVIDENCE=ROOT/'docs/evidence/drive-power-carrier-v02'


def main():
    DEST.mkdir(parents=True,exist_ok=True);EVIDENCE.mkdir(parents=True,exist_ok=True)
    sources={}
    def record(path):
        sources[path.relative_to(ROOT).as_posix()]=hashlib.sha256(path.read_bytes()).hexdigest()
        return path
    def data(path): return json.loads(record(path).read_text(encoding='utf-8'))
    spec=data(ROOT/'hardware/drive-power-carrier/geometry.json')
    previous=data(ROOT/'docs/evidence/current-service-v01.json')
    shapes={};models={};obstacles={}
    # Board drawing +x,+y map to car +Y,+X. Board underside remains Z29.5.
    pcb=BRepPrimAPI_MakeBox(gp_Pnt(25.75,47,29.5),18,30,1.6).Shape()
    for pad in spec['pads']:
        if pad['ref'] not in ['U1','C2',*[f'H{i}' for i in range(1,10)]]: continue
        x,y=pad['xy_mm']; r=.5 if pad['ref'].startswith('H') else .45
        hole=BRepPrimAPI_MakeCylinder(gp_Ax2(gp_Pnt(25.75+y,47+x,29.4),gp_Dir(0,0,1)),r,1.8).Shape()
        pcb=BRepAlgoAPI_Cut(pcb,hole).Shape()
    shapes['pcb']=pcb
    for ref,item in spec['new_component_positions'].items():
        x,y=item['center_mm']; dx,dy=item['body_mm']; h=item['height_reserve_mm']
        z=29.5-h if item['side']=='B' else 31.1
        if ref.startswith('H'):
            z=28.6;h=3.0  # Unqualified lead/solder reserve through the new hole.
        shapes[ref]=BRepPrimAPI_MakeBox(gp_Pnt(25.75+y-dy/2,47+x-dx/2,z),dy,dx,h).Shape()
    assembly=TopoDS_Compound();builder=BRep_Builder();builder.MakeCompound(assembly)
    for name,shape in shapes.items():
        BRepMesh_IncrementalMesh(shape,.03,False,.1,True).Perform()
        writer=StlAPI_Writer();writer.ASCIIMode=False
        assert writer.Write(shape,str(DEST/(name+'.stl')))
        models[name]=mesh(DEST/(name+'.stl'));assert models[name].is_volume
        builder.Add(assembly,shape)
    step=STEPControl_Writer();assert step.Transfer(assembly,STEPControl_AsIs)==IFSelect_RetDone
    assert step.Write(str(DEST/'added-power-and-pcb.step'))==IFSelect_RetDone
    # Read present files, rather than trusting an earlier fit result.
    for filename in previous['source_sha256']:
        path=ROOT/filename
        if path.suffix != '.stl' or filename=='cad/motor-carrier/pcb.stl': continue
        obstacles[filename]=mesh(record(path));assert obstacles[filename].is_volume
    old=data(ROOT/'docs/evidence/packaging-v05.json')
    xiao=data(ROOT/'docs/evidence/xiao-mount-v01.json')
    harness=data(ROOT/'docs/evidence/control-harness-v01.json')
    for name,route in (old['wire_corridors']|xiao['revised_corridor_definitions']).items():
        if name in ('wire-power','wire-servo','wire-motor'):
            obstacles[name]=tube(route['points_mm'],route['radius_mm'])
    for name,route in harness['routes'].items():
        obstacles['control-'+name]=tube(route['points_mm'],route['radius_mm'])
    for name,bounds in xiao['source_bounding_boxes_mm'].items():
        bounds=np.array(bounds);obstacles['xiao-component-'+name]=box(bounds[1]-bounds[0],bounds.mean(axis=0))
    cavity=mesh(record(ROOT/'cad/packaging-v05/inputs/adjustable-cavity.stl'))
    hits={};outside={}
    for name,m in models.items():
        if name=='pcb':continue
        for other,solid in obstacles.items():
            volume=intersect(m,solid)
            if volume>.001:hits[name+'/'+other]=round(volume,6)
        outside[name]=float(trimesh.boolean.difference([m,cavity],engine='manifold').volume)
    pair_hits={}
    items=list(models.items())
    for i,(name,m) in enumerate(items):
        for other,solid in items[i+1:]:
            volume=intersect(m,solid)
            if volume>.001:pair_hits[name+'/'+other]=round(volume,6)
    bad=models['U3'].copy();bad.apply_translation([0,0,1.6])
    positive=intersect(bad,models['pcb']);assert positive>1
    # Existing component bodies are retained, with a new PCB that adds two holes.
    retained=[m for name,m in obstacles.items() if name.startswith('cad/motor-carrier/') and not name.endswith('wire-antenna.stl')]
    combined=trimesh.util.concatenate([*models.values(),*retained]);combined.export(DEST/'combined-reserves.stl')
    for path in [Path(__file__),ROOT/'tools/build_packaging_v05.py',ROOT/'tools/layout_zcar_battery.py']:
        record(path)
    result={'date':'2026-09-15','contract':'DRIVE-POWER-CARRIER-01 v0.2','pose_mm':[0,3],
            'new_solid_count':len(models)-1,'obstacle_solid_count':len(obstacles),
            'hits_mm3':hits,'new_part_pair_hits_mm3':pair_hits,'outside_cavity_mm3':outside,
            'positive_control_shifted_U3_into_PCB_mm3':positive,'assembly_bounds_mm':combined.bounds.tolist(),
            'lowest_new_component_z_mm':min(m.bounds[0,2] for n,m in models.items() if n!='pcb'),
            'sources_sha256':sources,'pcb_fit_proven_for_actual_components':False,
            'limitations':['Nominal electronics0/camera+3 only; no new removal/adjustment sweep',
                           'SOT23 reserve3.5x3.5x1.5mm covers documented outline/protrusions; actual placement and solder remain unqualified',
                           'H8/H9 lead reserves only, not routed new power wires or strain relief',
                           'Source hypothetical body, battery and other unmeasured dimensions remain assumptions',
                           'No thermal/current/physical/print qualification']}
    (EVIDENCE/'fit.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:result[k] for k in ('new_solid_count','obstacle_solid_count','hits_mm3','new_part_pair_hits_mm3','assembly_bounds_mm')},indent=2),flush=True)
    assert not hits and not pair_hits and max(outside.values())<.001


if __name__=='__main__':main()
