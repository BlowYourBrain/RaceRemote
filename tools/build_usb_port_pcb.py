"""Generate the passive USB-C daughterboard; run with KiCad's Python.

This board exposes VBUS, GND, CC1/2 and D+/D- to a separate charger.
It does not charge a battery or authorize more input current by itself.
"""
import hashlib
import json
import math
from pathlib import Path
import uuid

import pcbnew as p

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'hardware/usb-port'
LIB = DEST / 'RaceRemote_USB.pretty'
CONNECTOR = 'USB_C_Receptacle_GCT_USB4105-xx-A_16P_TopMnt_Horizontal'
FOOTPRINT_SHA = 'b48648d0fd08d0fded58487a7c36cd5881ef33451fb6e3f4295d4c6e54d31075'


def vec(x, y):
    return p.VECTOR2I(p.FromMM(100 + x), p.FromMM(100 + y))


def size(x, y):
    return p.VECTOR2I(p.FromMM(x), p.FromMM(y))


def main():
    assert hashlib.sha256((LIB / (CONNECTOR + '.kicad_mod')).read_bytes()).hexdigest() == FOOTPRINT_SHA
    b = p.BOARD()
    settings = b.GetDesignSettings()
    settings.SetBoardThickness(p.FromMM(1))
    settings.m_MinClearance = p.FromMM(.15)
    settings.m_HoleClearance = p.FromMM(.15)
    settings.m_NetSettings.GetDefaultNetclass().SetClearance(p.FromMM(.15))
    settings.m_CopperEdgeClearance = p.FromMM(.2)
    settings.m_TrackMinWidth = p.FromMM(.15)
    settings.m_MinThroughDrill = p.FromMM(.25)
    settings.m_ViasMinSize = p.FromMM(.5)
    settings.m_ViasMinAnnularWidth = p.FromMM(.1)
    settings.m_HoleToHoleMin = p.FromMM(.25)
    net = {}
    pad_layers = p.LSET.AllCuMask(2)
    pad_layers.AddLayer(p.F_Mask); pad_layers.AddLayer(p.B_Mask)
    for name in ['GND', 'VBUS', 'CC1', 'CC2', 'DP', 'DM']:
        net[name] = p.NETINFO_ITEM(b, name)
        b.Add(net[name])

    # Coordinates use the library footprint axes: X across the mouth, +Y out.
    outline = [(-13, -4.525), (13, -4.525), (13, 3.075), (-13, 3.075)]
    for a, c in zip(outline, outline[1:] + outline[:1]):
        line = p.PCB_SHAPE()
        line.SetShape(p.SHAPE_T_SEGMENT)
        line.SetStart(vec(*a)); line.SetEnd(vec(*c))
        line.SetLayer(p.Edge_Cuts); line.SetWidth(p.FromMM(.05)); b.Add(line)

    connector = p.FootprintLoad(str(LIB), CONNECTOR)
    connector.SetReference('J1'); connector.SetValue('USB4105-GF-A'); connector.SetPosition(vec(0, 0))
    connector.Reference().SetVisible(False); connector.Value().SetVisible(False)
    pin_nets = {'A1': 'GND', 'B12': 'GND', 'A12': 'GND', 'B1': 'GND', 'S1': 'GND',
                'A4': 'VBUS', 'B9': 'VBUS', 'A9': 'VBUS', 'B4': 'VBUS',
                'A5': 'CC1', 'B5': 'CC2', 'A6': 'DP', 'B6': 'DP', 'A7': 'DM', 'B7': 'DM'}
    for pad in connector.Pads():
        if pad.GetNumber() in pin_nets: pad.SetNet(net[pin_nets[pad.GetNumber()]])
    b.Add(connector)

    # Independent Rd resistors on CC1 and CC2. Main-board sense inputs must
    # stay high impedance; adding another pair of Rd is not permitted.
    resistors = []
    for ref, x, signal, cc_pad in [('R1', -1.5, 'CC1', '1'), ('R2', 1.5, 'CC2', '2')]:
        f = p.FootprintLoad(str(LIB), 'R_0603_1608Metric')
        f.SetReference(ref); f.SetValue('5.1k 1%'); f.SetPosition(vec(x, .4))
        b.Add(f)
        f.Flip(vec(x, .4), p.FLIP_DIRECTION_LEFT_RIGHT)
        # A flip reverses pad X; assign by final physical side, not pad number.
        pads = sorted(f.Pads(), key=lambda a: a.GetPosition().x)
        for i, pad in enumerate(pads): pad.SetNet(net[signal if i == (0 if x < 0 else 1) else 'GND'])
        f.Reference().SetVisible(False); f.Value().SetVisible(False)
        resistors.append(f)

    # Bare wire solder terminals; no unverified connector housing is implied.
    terminals = [('H1', -11.6, -3, 'VBUS'), ('H2', -11.6, -.8, 'DP'), ('H3', -11.6, 1.4, 'CC1'),
                 ('H4', 11.6, -3, 'GND'), ('H5', 11.6, -.8, 'DM'), ('H6', 11.6, 1.4, 'CC2')]
    for ref, x, y, signal in terminals:
        f = p.FOOTPRINT(b); f.SetReference(ref); f.SetValue(signal); f.SetPosition(vec(x, y))
        f.Reference().SetVisible(False); f.Value().SetVisible(False)
        pad = p.PAD(f); pad.SetNumber('1'); pad.SetAttribute(p.PAD_ATTRIB_PTH)
        pad.SetShape(p.PAD_SHAPE_CIRCLE); pad.SetSize(size(1.6, 1.6)); pad.SetDrillSize(size(.8, .8))
        pad.SetLayerSet(pad_layers); pad.SetPosition(vec(x, y)); pad.SetNet(net[signal])
        f.Add(pad); b.Add(f)

    # Keep copper/tracks/vias away from the proposed metal nut/head envelopes.
    for i, x in enumerate([-8, 8]):
        f = p.FOOTPRINT(b); f.SetReference('M' + str(i+1)); f.SetValue('M2 clearance')
        f.SetPosition(vec(x, -.175)); f.Reference().SetVisible(False); f.Value().SetVisible(False)
        pad = p.PAD(f); pad.SetAttribute(p.PAD_ATTRIB_NPTH); pad.SetShape(p.PAD_SHAPE_CIRCLE)
        pad.SetSize(size(2.4, 2.4)); pad.SetDrillSize(size(2.4, 2.4)); pad.SetPosition(vec(x, -.175))
        pad.SetLayerSet(pad_layers); f.Add(pad); b.Add(f)
        z = p.ZONE(b); z.SetLayerSet(p.LSET.AllCuMask(2)); z.SetIsRuleArea(True)
        z.SetDoNotAllowTracks(True); z.SetDoNotAllowVias(True); z.SetDoNotAllowZoneFills(True)
        z.SetDoNotAllowPads(False)  # NPTH belongs inside its own keepout.
        poly = z.Outline(); poly.NewOutline()
        for step in range(64):
            a = 2*math.pi*step/64
            q = vec(x+2.5*math.cos(a), -.175+2.5*math.sin(a)); poly.Append(q.x, q.y)
        b.Add(z)

    def trace(signal, layer, points, width=.15):
        for a, c in zip(points, points[1:]):
            t = p.PCB_TRACK(b); t.SetStart(vec(*a)); t.SetEnd(vec(*c)); t.SetWidth(p.FromMM(width))
            t.SetLayer(layer); t.SetNet(net[signal]); b.Add(t)

    def via(signal, x, y, diameter=.5, drill=.25):
        v = p.PCB_VIA(b); v.SetPosition(vec(x,y)); v.SetWidth(p.FromMM(diameter)); v.SetDrill(p.FromMM(drill))
        v.SetViaType(p.VIATYPE_THROUGH); v.SetLayerPair(p.F_Cu,p.B_Cu); v.SetNet(net[signal]); b.Add(v)

    for signal, x, y in [('DP', -.25,-2.4),('DP', .75,-2.4),('DM', -.75,-1.5),('DM', .25,-1.5)]:
        trace(signal,p.F_Cu,[(x,-3.68),(x,y)]); via(signal,x,y)
    trace('DP',p.B_Cu,[(-.25,-2.4),(.75,-2.4)])
    trace('DP',p.F_Cu,[(.75,-2.4),(1.1,-2),(1.1,-.5)])
    via('DP',1.1,-.5)
    trace('DP',p.B_Cu,[(1.1,-.5),(-5.25,-.5),(-6,-3),(-10.5,-3),(-10.5,-.8),(-11.6,-.8)])
    trace('DM',p.B_Cu,[(-.75,-1.5),(.25,-1.5),(5.25,-1.5),(6,-3),(10.5,-3),(10.5,-.8),(11.6,-.8)])

    for signal, x in [('CC1',-1.25),('CC2',1.75)]:
        trace(signal,p.F_Cu,[(x,-3.68),(x,1.5)]); via(signal,x,1.5)
    # Use imported resistor pad coordinates to avoid hidden flip assumptions.
    for f, signal, source, side in [(resistors[0],'CC1',(-1.25,1.5),-1),(resistors[1],'CC2',(1.75,1.5),1)]:
        pad = next(q for q in f.Pads() if q.GetNetname()==signal)
        xy = (p.ToMM(pad.GetPosition().x)-100,p.ToMM(pad.GetPosition().y)-100)
        trace(signal,p.B_Cu,[source,xy])
        trace(signal,p.B_Cu,[source,(source[0],2.6),(side*10.8,2.6),(side*11.6,1.4)])

    for x in [-2.4,2.4]:
        trace('VBUS',p.F_Cu,[(x,-3.68),(x,-3.5)],.4); via('VBUS',x,-3.5,.65,.3)
    trace('VBUS',p.B_Cu,[(-2.4,-3.5),(2.4,-3.5)],.4)
    trace('VBUS',p.B_Cu,[(-2.4,-3.5),(-2,-3.0),(-2,-1.5),(-4.85,-1.5)],.4)
    via('VBUS',-4.85,-1.5,.65,.3)
    trace('VBUS',p.F_Cu,[(-4.85,-1.5),(-6,-3.25),(-11.6,-3)],.4)
    for x in [-3.2,3.2]:
        trace('GND',p.F_Cu,[(x,-3.68),(x,-3.5)],.3); via('GND',x,-3.5,.5,.25)

    for layer in [p.F_Cu,p.B_Cu]:
        z=p.ZONE(b); z.SetLayer(layer); z.SetNet(net['GND']); z.SetLocalClearance(p.FromMM(.15))
        z.SetPadConnection(p.ZONE_CONNECTION_FULL)
        poly=z.Outline(); poly.NewOutline()
        for x,y in outline:
            v=vec(x,y); poly.Append(v.x,v.y)
        b.Add(z)

    path=DEST/'usb-port.kicad_pcb'
    p.SaveBoard(str(path),b)
    # SaveBoard writes the project settings above; do not replace them with
    # a partial project JSON before a later SaveBoard silently overwrites it.
    (DEST/'fp-lib-table').write_text('(fp_lib_table (version 7) (lib (name "RaceRemote_USB")(type "KiCad")(uri "${KIPRJMOD}/RaceRemote_USB.pretty")(options "")(descr "Project-local KiCad library references")))\n',encoding='utf-8')
    p.ZONE_FILLER(b).Fill(b.Zones()); p.SaveBoard(str(path),b)
    # Export interface geometry directly from the final pad locations.
    # KiCad (100,100) -> car (X=55.425,Y=59), with axes swapped.
    def car_xy(item):
        q=item.GetPosition()
        return [55.425+p.ToMM(q.y)-100,59+p.ToMM(q.x)-100]
    holes=[]; copper=[]; pins=[]
    for f in b.GetFootprints():
        for pad in f.Pads():
            xy=car_xy(pad); d=pad.GetDrillSize(); s=pad.GetSize()
            record={'reference':f.GetReference(),'pad':pad.GetNumber(),'net':pad.GetNetname(),
                    'center_mm':xy,'size_mm':[p.ToMM(s.y),p.ToMM(s.x)],
                    'drill_mm':[p.ToMM(d.y),p.ToMM(d.x)]}
            if d.x and f.GetReference()[0]!='M': holes.append(record)
            if f.GetReference()=='J1' and d.x: pins.append(record)
            if pad.GetNetname() or (f.GetReference()=='J1' and pad.GetNumber()):
                record['front']=pad.IsOnLayer(p.F_Cu); record['back']=pad.IsOnLayer(p.B_Cu)
                copper.append(record)
    geometry={'connector_face_x_mm':59.1,'pcb_bounds_mm':[[50.9,46,22.15],[58.5,72,23.15]],
              'old_face_x_mm':58.5,'old_min_smd_edge_clearance_mm':-.33,
              'new_min_smd_edge_clearance_mm':min(r['center_mm'][0]-r['size_mm'][0]/2-50.9 for r in copper if r['reference']=='J1'),
              'holes':holes,'copper':copper,'terminal_order':terminals,'connector_pin_nets':pin_nets,
              'library_sha256':FOOTPRINT_SHA,'kicad_version':p.Version()}
    (DEST/'geometry.json').write_text(json.dumps(geometry,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    scad=['// Generated from KiCad pads by tools/build_usb_port_pcb.py; mm.',
          'module usb_oval(xy,wh,z,h) {',
          ' r=min(wh)/2; hull() for(x=[-wh[0]/2+r,wh[0]/2-r],y=[-wh[1]/2+r,wh[1]/2-r])',
          ' translate([xy[0]+x,xy[1]+y,z]) cylinder(h=h,r=r,$fn=32);','}',
          'module usb_port_drills() {']
    for r in holes: scad.append(f' usb_oval({r["center_mm"]},{r["drill_mm"]},22,1.4);')
    scad.append('}\nmodule usb_port_copper() {')
    seen=set()
    for r in copper:
        for key,z in [('front',23.15),('back',22.115)]:
            identity=(tuple(r['center_mm']),tuple(r['size_mm']),z)
            if r[key] and identity not in seen:
                seen.add(identity)
                # Bounding pad solids, conservative at rounded corners.
                x,y=r['center_mm']; w,h=r['size_mm']
                scad.append(f' translate([{x-w/2},{y-h/2},{z}]) cube([{w},{h},0.035]);')
    scad.append('}\nmodule usb_port_stakes() {')
    for r in pins: scad.append(f' usb_oval({r["center_mm"]},{r["drill_mm"]},22.05,1.1);')
    scad.append('}\nmodule usb_port_underside() {')
    for f in resistors:
        x,y=car_xy(f)
        scad.append(f' translate([{x-.4},{y-.8},21.35]) cube([.8,1.6,.8]);')
    for _,u,v,_ in terminals:
        scad.append(f' translate([{55.425+v},{59+u},20.65]) cylinder(h=1.5,r=1,$fn=32);')
    scad.append('}')
    (ROOT/'cad/components/usb-port-pcb.scad').write_text('\n'.join(scad)+'\n',encoding='utf-8')
    print('Generated',path,'KiCad',p.Version())


if __name__=='__main__': main()
