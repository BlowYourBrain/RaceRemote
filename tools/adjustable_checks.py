"""Sample two adjustment axes; separate mechanical clearance from shell fit.

Only rigid translations are checked. Cached intersections exploit invariance
under a common translation, without treating sampled positions as a sweep.
"""
import numpy as np
import trimesh
from layout_zcar_battery import box, overlap

MAIN={'tray','power','driver','charge','main_fasteners'}
CAMERA={'camera','camera_mount','camera_fasteners'}


def check_positions(meshes, parts, frame, cover, excluded):
    names=['carrier_installed','adhesive','battery','support','guards','band']+sorted(MAIN|CAMERA)
    originals={n:meshes[n] for n in names}
    originals.update(frame=frame,cover=cover)
    originals.update({f'reference_box_{i}':box(p.extents+.002,p.bounds.mean(axis=0))
                      for i,p in enumerate(parts) if i not in excluded})
    fixed=['frame','cover']+[n for n in originals if n.startswith('reference_box_')]
    cache={}

    def hit(a,b,da,db):
        key=(a,b,db-da)
        if key not in cache:
            ma=originals[a]; mb=originals[b]
            shifted=mb.bounds+np.array([0,db-da,0])
            if np.any(np.minimum(ma.bounds[1],shifted[1])-np.maximum(ma.bounds[0],shifted[0])<=1e-6):
                cache[key]=0.
            else:
                moved=mb.copy(); moved.apply_translation([0,db-da,0])
                cache[key]=overlap(ma,moved)
        return cache[key]

    cavity_cache={}
    def outside(name,dy):
        key=(name,dy)
        if key not in cavity_cache:
            moved=meshes[name].copy(); moved.apply_translation([0,dy,0])
            cavity_cache[key]=float(trimesh.boolean.difference([moved,meshes['cavity']],engine='manifold').volume)
        return cavity_cache[key]

    rows=[]
    poses=[(e,c) for e in range(-3,9) for c in range(-3,9)]+[(-4,0),(0,9)]
    for e,c in poses:
        offsets={n:e if n in MAIN else c if n in CAMERA else 0 for n in names}
        hits={}
        for i,a in enumerate(names):
            for b in names[i+1:]:
                v=hit(a,b,offsets[a],offsets[b])
                if v>1e-4: hits[f'{a}/{b}']=round(v,6)
        for a in sorted(MAIN|CAMERA):
            for b in fixed:
                v=hit(a,b,offsets[a],0)
                if v>1e-4: hits[f'{a}/{b}']=round(v,6)
        outside_body={n:round(outside(n,offsets[n]),6) for n in sorted(MAIN|CAMERA)
                      if outside(n,offsets[n])>1e-3}
        rows.append(dict(electronics_mm=e,camera_mm=c,mechanical_clear=not hits,
                         hypothetical_body_clear=not outside_body,
                         mechanical_collisions_mm3=hits,outside_hypothetical_body_mm3=outside_body))
    nominal=next(r for r in rows if r['electronics_mm']==r['camera_mm']==0)
    assert nominal['mechanical_clear'] and nominal['hypothetical_body_clear'],nominal
    for e,c in [(-4,0),(0,9),(8,-3)]:
        bad=next(r for r in rows if (r['electronics_mm'],r['camera_mm'])==(e,c))
        assert not bad['mechanical_clear'],bad
    return dict(step_mm=1,nominal_range_mm=[-3,8],poses=rows,
                limitation='Discrete rigid positions only; no continuous sweep, tolerances, wiring or real shell evidence')
