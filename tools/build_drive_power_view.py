"""Offline, static-pose interactive CAD view and a render of the actual meshes."""
import base64
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT/'cad/drive-power-carrier'


def main():
    source = ROOT/'cad/current-service/viewer.html'
    template = ROOT/'tools/cad_viewer_template.html'
    specpath = ROOT/'hardware/drive-power-carrier/geometry.json'
    spec = json.loads(specpath.read_text())
    rows = json.loads(source.read_text(encoding='utf-8').split('const parts=',1)[1].split(';\nconst adjustmentChecks=',1)[0])
    assert len([r for r in rows if r['id']=='carrier-pcb']) == 1
    rows = [r for r in rows if r['id']!='carrier-pcb']
    for row in rows:
        row['slide'] = None; row['level'] = 0
    colors = {'pcb':[.13,.48,.28], 'U2':[.87,.37,.1], 'U3':[.8,.2,.18]}
    meshes = {}; inputs = [source,template,specpath,Path(__file__)]
    for name in ['pcb', *spec['new_component_positions']]:
        path = DEST/(name+'.stl'); inputs.append(path)
        m = trimesh.load_mesh(path); meshes[name] = m
        color = colors.setdefault(name, [.78,.57,.23] if name.startswith('H') else [.2,.35,.65])
        raw = np.hstack([m.triangles.reshape(-1,3),np.repeat(m.face_normals,3,axis=0)]).astype('<f4')
        rows.append(dict(id='power-'+name,label='Общая плата: '+name,color=color,slide=None,level=0,
                         wire=False,hidden=False,count=len(raw),data=base64.b64encode(raw.tobytes()).decode()))
    page = template.read_text(encoding='utf-8').replace('3D-компоновка 0.1','Привод и питание XIAO')
    begin = page.index('<p class="note">'); end = page.index('</p>',begin)+4
    page = page[:begin]+'''<p class="note">Общая плата 30 × 18 мм: новые детали питания находятся снизу. Только начальное положение 0/+3. Корпуса — резервы места; новые провода H8/H9, нагрев и снятие платы ещё не проверены. Печать отложена.</p>'''+page[end:]
    page = page.replace('assembly.scad','added-power-and-pcb.step').replace('Редактировать в OpenSCAD','STEP новых деталей')
    page = page.replace('assembly.md','README.md').replace('reference/NOTICE.md','../packaging-v05/NOTICE.md')
    page = page.replace('part.visible=true;','part.visible=!part.hidden;').replace('input.checked=true;','input.checked=!part.hidden;')
    page = page.replace('__SCENE_DATA__',json.dumps(rows,ensure_ascii=False)).replace('__ADJUSTMENT_DATA__','null')
    page = page.replace('</script></html>',"document.querySelector('#explode').closest('label').hidden=true;\n</script></html>")
    page = page.replace('Разнесение деталей служит только для просмотра.','Детали можно скрывать флажками слева.')
    (DEST/'viewer.html').write_text(page,encoding='utf-8',newline='\n')
    # Show added underside parts at large scale; second view includes the existing driver and mount.
    old = {}; oldcolors = {}
    for path in sorted((ROOT/'cad/motor-carrier').glob('*.stl')):
        if path.stem in ('pcb','carrier','wire-antenna'): continue
        inputs.append(path); old[path.stem] = trimesh.load_mesh(path)
        oldcolors[path.stem] = [.3,.3,.32] if path.stem in ('ta6586','C2') else [.7,.67,.57]
    for name in ('tray','clamp'):
        path = ROOT/'cad/motor-carrier-mount'/(name+'.stl');inputs.append(path)
        old[name] = trimesh.load_mesh(path);oldcolors[name] = [.62,.67,.72]
    fig = plt.figure(figsize=(13,5.4))
    for i in range(2):
        ax = fig.add_subplot(1,2,i+1,projection='3d')
        shown = meshes if i==0 else meshes|{n:m for n,m in old.items() if n not in ('tray','clamp')}
        palette = colors|oldcolors
        triangles = np.concatenate([m.triangles for m in shown.values()])
        facecolors = np.concatenate([np.tile(palette[n],(len(m.faces),1)) for n,m in shown.items()])
        ax.add_collection3d(Poly3DCollection(triangles,facecolor=facecolors,edgecolor='none'))
        ax.set(xlim=(23,46),ylim=(45,79),zlim=(27,34) if i==0 else (26,46),
               xlabel='Car X (mm)',ylabel='Car Y (mm)',zlabel='Z (mm)',
               title='New components below PCB — provisional envelopes' if i==0 else 'Side view — holder hidden to expose underside')
        ax.set_box_aspect((23,34,7 if i==0 else 20));ax.view_init(-38,-45) if i==0 else ax.view_init(2,0)
        if i==1: ax.set_xticks([]);ax.set_xlabel('')
        if i==0:
            for name in ('U2','U3'):
                pt=meshes[name].centroid;ax.text(*pt,name,color='black',fontsize=11)
    fig.tight_layout();fig.savefig(DEST/'preview.png',dpi=150);plt.close(fig)
    report = {'date':'2026-09-15','new_parts':11,'viewer_pose_mm':[0,3],
              'source_sha256':{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in inputs},
              'artifact_sha256':{(DEST/n).relative_to(ROOT).as_posix():hashlib.sha256((DEST/n).read_bytes()).hexdigest() for n in ('viewer.html','preview.png')}}
    (ROOT/'docs/evidence/drive-power-carrier-v02/view.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8',newline='\n')
    print('Generated interactive nominal-pose viewer and two CAD mesh views.')


if __name__=='__main__': main()
