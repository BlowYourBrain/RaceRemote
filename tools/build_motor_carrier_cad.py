"""Mechanical proposal from carrier pad coordinates; run with CAD requirements."""
import base64
import hashlib
import json
from pathlib import Path
from OCP.BRepPrimAPI import BRepPrimAPI_MakeBox, BRepPrimAPI_MakeCylinder
from OCP.BRepAlgoAPI import BRepAlgoAPI_Cut
from OCP.gp import gp_Pnt, gp_Ax2, gp_Dir
from OCP.BRep import BRep_Builder
from OCP.TopoDS import TopoDS_Compound
from OCP.STEPControl import STEPControl_Writer, STEPControl_AsIs
from OCP.IFSelect import IFSelect_RetDone
from OCP.BRepMesh import BRepMesh_IncrementalMesh
from OCP.StlAPI import StlAPI_Writer
import numpy as np
import trimesh
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection
from build_packaging_v05 import mesh, intersect, tube
from layout_zcar_battery import box

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'cad/motor-carrier'
HW=ROOT/'hardware/motor-carrier'


def main():
    DEST.mkdir(parents=True,exist_ok=True)
    data=json.loads((HW/'geometry.json').read_text())
    shapes={};colors={}
    # PCB drawing x/y -> car +Y/+X; local z=0 is the underside reservation.
    def block(name,x,y,z,dx,dy,dz,color):
        shapes[name]=BRepPrimAPI_MakeBox(gp_Pnt(25.75+y,47+x,28.6+z),dy,dx,dz).Shape()
        colors[name]=color
    def cylinder(x,y,z,r,h):
        return BRepPrimAPI_MakeCylinder(gp_Ax2(gp_Pnt(25.75+y,47+x,28.6+z),gp_Dir(0,0,1)),r,h).Shape()
    block('pcb',0,0,.9,30,18,1.6,[.12,.47,.3])
    for a in data['pads']:
        if a['ref'].startswith('R') or a['ref']=='C1': continue
        x,y=a['xy_mm'];r=.5 if a['ref'].startswith('H') else .45
        shapes['pcb']=BRepAlgoAPI_Cut(shapes['pcb'],cylinder(x,y,.8,r,1.8)).Shape()
    block('ta6586',7.4,5.775,3.0,9.2,6.45,4.3,[.12,.13,.16])
    for a in data['pads']:
        if a['ref']!='U1':continue
        x,y=a['xy_mm']
        # Maximum-width straight lead envelopes; bend detail remains approximate.
        block('lead'+a['pin'],x-.28,y-.23,0,.56,.46,3.5,[.7,.7,.72])
    for ref in ['R1','R2','R3','R4','C1']:
        x,y=data['component_positions'][ref]['center_mm']
        block(ref,x-1,y-.625,0 if ref=='C1' else 2.5,2,1.25,.9,[.62,.45,.22] if ref=='C1' else [.2,.2,.23])
    # Rubycon ZLH 8 x 11.5 nominal -> D+0.5, L+1.5 maxima; 0.5 seating gap proposal.
    shapes['C2']=cylinder(23,6,3,4.25,13);colors['C2']=[.22,.28,.45]
    for a in data['pads']:
        if a['ref']=='C2':
            x,y=a['xy_mm'];shapes['C2lead'+a['pin']]=cylinder(x,y,0,.3,3)
            colors['C2lead'+a['pin']]=[.7,.7,.72]
    assembly=TopoDS_Compound();builder=BRep_Builder();builder.MakeCompound(assembly)
    models={}
    for n,s in shapes.items():
        builder.Add(assembly,s)
        BRepMesh_IncrementalMesh(s,.03,False,.1,True).Perform()
        writer=StlAPI_Writer();writer.ASCIIMode=False
        assert writer.Write(s,str(DEST/(n+'.stl'))),n
        models[n]=mesh(DEST/(n+'.stl'));assert models[n].is_volume,n
    writer=STEPControl_Writer();assert writer.Transfer(assembly,STEPControl_AsIs)==IFSelect_RetDone
    assert writer.Write(str(DEST/'carrier.step'))==IFSelect_RetDone
    combined=trimesh.util.concatenate(list(models.values()))
    combined.export(DEST/'carrier.stl')
    antenna_points=[[14.59,85.2,26.16],[10,85.2,28],[10,85.2,42],
                    [10,76.5,42],[22,76.5,42],[40,76.5,42],[43,70,42],[47.5,63,42]]
    new_antenna=tube(antenna_points,.65)
    new_antenna.export(DEST/'wire-antenna.stl')
    # Preserve the current 0.6 layout as historical evidence. This scene is a separate proposal.
    source=ROOT/'cad/current-layout/viewer.html'
    rows=json.loads(source.read_text(encoding='utf-8').split('const parts=',1)[1].split(';\nconst adjustmentChecks=',1)[0])
    rows=[r for r in rows if r['id'] not in ['driver','wire-antenna']]
    for r in rows:
        if r['slide']=='camera':
            raw=np.frombuffer(base64.b64decode(r['data']),dtype='<f4').copy().reshape(-1,6)
            raw[:,1]+=3;r['data']=base64.b64encode(raw.tobytes()).decode()
        r['slide']=None
        if r['id'].startswith('wire-'):r['hidden']=True
    for n,m in models.items():
        raw=np.hstack([m.triangles.reshape(-1,3),np.repeat(m.face_normals,3,axis=0)]).astype('<f4')
        rows.append(dict(id='carrier-'+n,label='Плата привода: '+n,color=colors[n],slide=None,level=0,wire=False,hidden=False,count=len(raw),data=base64.b64encode(raw.tobytes()).decode()))
    raw=np.hstack([new_antenna.triangles.reshape(-1,3),np.repeat(new_antenna.face_normals,3,axis=0)]).astype('<f4')
    rows.append(dict(id='new-antenna-wire',label='Новый обход антенного кабеля',color=[.15,.15,.18],slide=None,level=0,wire=False,hidden=False,count=len(raw),data=base64.b64encode(raw.tobytes()).decode()))
    html=(ROOT/'tools/cad_viewer_template.html').read_text(encoding='utf-8')
    html=html.replace('3D-компоновка 0.1','Плата привода — примерка')
    html=html.replace('Габаритная проработка, не готовая машинка. Цветные блоки электроники — резерв места. Их крепления, провода, кузов и ход подвески ещё не проверены.',
        'Кандидат платы TA6586 30 × 18 мм. Высота с выбранным габаритом конденсатора — 16 мм. Показано только положение плат 0 / камеры +3 мм. Антенный кабель обходит конденсатор. Остальные старые провода скрыты: их нужно переложить к новым площадкам. Кузов условный; крепление и реальная посадка ещё открыты.')
    html=html.replace('assembly.scad','carrier.step').replace('Редактировать в OpenSCAD','Открыть STEP').replace('assembly.md','README.md').replace('reference/NOTICE.md','../reference/NOTICE.md')
    html=html.replace('part.visible=true;','part.visible=!part.hidden;').replace('input.checked=true;','input.checked=!part.hidden;')
    html=html.replace('__SCENE_DATA__',json.dumps(rows,ensure_ascii=False)).replace('__ADJUSTMENT_DATA__','null')
    html=html.replace('</script></html>',"document.querySelector('#explode').closest('label').hidden=true;\n</script></html>")
    (DEST/'viewer.html').write_text(html,encoding='utf-8',newline='\n')
    # Check the candidate against solids individually, with conservative XIAO component boxes.
    inputs=[Path(__file__),HW/'geometry.json',source,ROOT/'tools/cad_viewer_template.html']
    obstacles={}
    def read(name,path):
        inputs.append(path);obstacles[name]=mesh(path);assert obstacles[name].is_volume,name
    base=ROOT/'cad/packaging-v05'
    for n in ['buck','buck_plug','pads','charge','frame-relief-proposal','servo-installed','antenna','antenna_support']:
        read(n,base/(n+'.stl'))
    for n in ['cover','usb-base','usb-pcb','usb-connector','usb-components','adjustable-battery','adjustable-support','adjustable-guards','adjustable-band','adjustable-main_fasteners']:
        read(n,base/'inputs'/(n+'.stl'))
    for n in ['cradle','screws','stage_screws','usb_access']:
        read(n,ROOT/'cad/xiao-mount'/(n+'.stl'))
    read('tray',ROOT/'cad/harness-guides/tray.stl');read('cap',ROOT/'cad/harness-guides/cap.stl')
    read('motor',ROOT/'cad/components/motor-installed.stl')
    xp=ROOT/'docs/evidence/xiao-mount-v01.json';inputs.append(xp)
    bounds=json.loads(xp.read_text())['source_bounding_boxes_mm']
    obstacles['xiao']=trimesh.boolean.union([box(np.diff(v,axis=0)[0],np.mean(v,axis=0))for v in bounds.values()],engine='manifold')
    cavity_path=base/'inputs/adjustable-cavity.stl';inputs.append(cavity_path);cavity=mesh(cavity_path)
    hits={f'{n}/{o}':round(intersect(m,om),5)for n,m in models.items()for o,om in obstacles.items()}
    outside={n:round(float(trimesh.boolean.difference([m,cavity],engine='manifold').volume),5)for n,m in models.items()}
    # Existing antenna path was qualified for the old ten-mm driver allocation only.
    antenna_path=ROOT/'cad/xiao-mount/wire-antenna.stl';inputs.append(antenna_path)
    antenna=mesh(antenna_path);assert antenna.is_volume
    wire_hits={n:round(intersect(m,antenna),5)for n,m in models.items()}
    route_hits={n:round(intersect(new_antenna,m),5)for n,m in (obstacles|models).items()if n!='antenna'}
    route_outside=round(float(trimesh.boolean.difference([new_antenna,cavity],engine='manifold').volume),5)
    assert all(np.isfinite(v)for v in [*hits.values(),*outside.values(),*wire_hits.values(),*route_hits.values(),route_outside])
    assert max(hits.values())<.001 and max(outside.values())<.001
    assert wire_hits['C2']>1, 'Positive control: old antenna route must collide with the new capacitor'
    assert max(route_hits.values())<.001 and route_outside<.001
    report={'version':'0.1','date':'2026-09-15','pose_mm':{'electronics':0,'camera':3},
        'bounds_mm':combined.bounds.tolist(),'envelope_mm':combined.extents.tolist(),
        'solid_intersections_mm3':{k:v for k,v in hits.items()if v>.001},
        'outside_hypothetical_body_mm3':{k:v for k,v in outside.items()if v>.001},
        'old_antenna_wire_intersections_mm3':{k:v for k,v in wire_hits.items()if v>.001},
        'new_antenna_route':{'radius_mm':.65,'points_mm':antenna_points,
            'intersections_mm3':{k:v for k,v in route_hits.items()if v>.001},'outside_body_mm3':route_outside,
            'exclusion':'antenna endpoint; wire-wire intersections and strain not checked'},
        'physical_fit_verified':False,'wires_rerouted':False,'mounting_designed':False,
        'sources_sha256':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()for p in inputs}}
    (ROOT/'docs/evidence/motor-carrier-fit-v01.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    # Actual CAD render, with top and side views and labelled axes in car coordinates.
    fig=plt.figure(figsize=(12,5))
    for i,(elev,azim,title)in enumerate([(38,-55,'Carrier candidate: 30 x 18 x 16 mm'),(0,0,'Side: capacitor envelope and underside parts')]):
        ax=fig.add_subplot(1,2,i+1,projection='3d')
        triangles=np.concatenate([m.triangles for m in models.values()])
        facecolors=np.concatenate([np.tile(colors[n],(len(m.faces),1))for n,m in models.items()])
        ax.add_collection3d(Poly3DCollection(triangles,facecolor=facecolors,edgecolor='none'))
        ax.set(xlim=(24,45),ylim=(45,79),zlim=(27,46),xlabel='Car X (mm)',ylabel='Car Y (mm)',zlabel='Z (mm)',title=title)
        ax.view_init(elev,azim);ax.set_box_aspect((21,34,19))
        if i==1:ax.set_xticks([]);ax.set_xlabel('')
    fig.tight_layout();fig.savefig(DEST/'preview.png',dpi=150);plt.close(fig)
    print(json.dumps({k:v for k,v in report.items()if k!='sources_sha256'},indent=2))


if __name__=='__main__':main()
