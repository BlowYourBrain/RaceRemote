"""Combined motor/logic carrier proposal; never overwrites either source design.

Run using the local KiCad Python. Existing traction copper is retained except
for the documented VM detour around the new LDO thermal vias.
"""
import csv
import copy
import json
from pathlib import Path
import re
import shutil
import uuid

import pcbnew as p
import build_motor_carrier_pcb as motor
from motor_carrier_identity import uid as motor_uid
from build_xiao_logic_power import uid as logic_uid

ROOT = Path(__file__).resolve().parents[1]
DEST = ROOT / 'hardware/drive-power-carrier'
BUILD = ROOT / 'build/drive-power-carrier'
LIB = 'RaceRemote_DrivePower'
RENAME = {'U1': 'U2', 'U2': 'U3', 'R1': 'R5', 'R2': 'R6', 'R3': 'R7',
          'C1': 'C3', 'C2': 'C4', 'C3': 'C5', 'C4': 'C6', 'J1': 'H8', 'J3': 'H9',
          '#FLG01': '#FLG03'}


def uid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'vea.raceremote/drive-power-carrier/v01/' + name))


def parse(source):
    tokens = re.findall(r'\(|\)|"(?:\\.|[^"\\])*"|[^\s()]+', source)
    stack, root = [], None
    for token in tokens:
        if token == '(':
            node = []
            if stack:
                stack[-1].append(node)
            else:
                root = node
            stack.append(node)
        elif token == ')':
            stack.pop()
        else:
            stack[-1].append(token)
    assert not stack and root
    return root


def render(node):
    return '(' + ' '.join(render(v) if isinstance(v, list) else v for v in node) + ')'


def child(node, name):
    return next((n for n in node if isinstance(n, list) and n[0] == name), None)


def q(value):
    return json.dumps(value)


def schematic():
    a = parse((ROOT / 'hardware/motor-carrier/motor-carrier.kicad_sch').read_text())
    c = parse((ROOT / 'hardware/xiao-logic-power/xiao-logic-power.kicad_sch').read_text())
    # A1 keeps both original diagrams legible without scaling pin geometry.
    child(a, 'uuid')[1] = q(uid('sheet'))
    child(a, 'paper')[1] = '"A1"'
    title = child(a, 'title_block')
    child(title, 'title')[1] = q('TA6586 + XIAO logic power - COMBINED PCB PROPOSAL')
    child(title, 'rev')[1] = '"0.3"'
    child(a, 'lib_symbols').extend(child(c, 'lib_symbols')[1:])

    def rewrite(node, mapping, power=False):
        if not isinstance(node, list):
            return
        if node[0] in ('reference',) and node[1].startswith('"'):
            ref = json.loads(node[1]); node[1] = q(mapping.get(ref, ref))
        if node[0] == 'property' and node[1] == '"Reference"':
            ref = json.loads(node[2]); node[2] = q(mapping.get(ref, ref))
        if node[0] == 'project':
            node[1] = '"drive-power-carrier"'
        if node[0] == 'path' and len(node) > 2 and child(node, 'reference'):
            node[1] = q('/' + uid('sheet'))
        if power and node[0] in ('at', 'xy'):
            # Only instance coordinates are passed here, not cached symbols.
            node[1] = f'{float(node[1]) + 330.2:g}'
        for item in node[1:]:
            if isinstance(item, list):
                rewrite(item, mapping, power)

    for node in a:
        if isinstance(node, list) and node[0] == 'symbol':
            rewrite(node, {})
    # Separate XIAO return landing: H5 remains available for the motor supply.
    originals={motor_uid('H5'),motor_uid('H51wire'),motor_uid('H51label')}
    def clone_ground(node):
        if not isinstance(node,list):return
        if node[0]=='uuid':node[1]=q(uid('H10/'+json.loads(node[1])))
        if node[0] in ('at','xy'):node[2]=f'{float(node[2])+20.32:g}'
        if node[0]=='property' and node[1]=='"Footprint"':node[2]=q(f'{LIB}:WirePad_1mm')
        for item in node[1:]:
            if isinstance(item,list):clone_ground(item)
    extra=[]
    for node in a:
        ident=child(node,'uuid') if isinstance(node,list) else None
        if ident and json.loads(ident[1]) in originals:
            n=copy.deepcopy(node);rewrite(n,{'H5':'H10'});clone_ground(n);extra.append(n)
    assert len(extra)==3
    a.extend(extra)
    omit = set()
    for ref in ('J2', 'J5', '#FLG02'):
        omit.add(logic_uid(ref))
        for ending in ('wire', 'label'):
            omit.add(logic_uid(ref + '1' + ending))
    for node in c[1:]:
        if not isinstance(node, list) or node[0] not in ('symbol', 'wire', 'global_label', 'no_connect', 'text'):
            continue
        ident = child(node, 'uuid')
        if ident and json.loads(ident[1]) in omit:
            continue
        if node[0] == 'text':
            # Replace interface notes with combined-board notes below.
            continue
        rewrite(node, RENAME, power=True)
        if node[0] == 'symbol':
            ref = json.loads(child(node, 'property')[2])
            # The first property is Reference in both source generators.
            fp = next(n for n in node if isinstance(n, list) and n[0] == 'property' and n[1] == '"Footprint"')
            if not ref.startswith('#'):
                fp[2] = q(f'{LIB}:' + fpname(ref))
        a.append(node)
    for i, text in enumerate([
        'COMBINED PROPOSAL: motor at left, logic at right. No physical power/thermal qualification or fabrication release.',
        'H8 = qualified Waveshare +5V. H9 = XIAO BAT0 positive. H10 = XIAO ground. H5 = motor supply return. H3 = Waveshare ground.',
        'VM remains the separately qualified motor supply. Never bridge VM, WAVE_5V, XIAO_BAT or raw battery terminals.',
        'U2 TPS73701 and U3 MAX40200 are on the underside. Package/paste/via manufacture and actual PCB fit remain open.'
    ]):
        a.append(parse(f'(text {q(text)} (at 330 {12.7 + i * 5.08:g} 0)(effects (font (size 1.1 1.1))(justify left top))(uuid "{uid(text)}"))'))
    (DEST / 'drive-power-carrier.kicad_sch').write_text(render(a) + '\n', encoding='utf-8')
    for source in ('motor-carrier', 'xiao-logic-power'):
        for sym in (ROOT / 'hardware' / source).glob('*.kicad_sym'):
            shutil.copyfile(sym, DEST / sym.name)
    (DEST / 'sym-lib-table').write_text('(sym_lib_table (version 7)' + ''.join(
        f'(lib (name "{n}")(type "KiCad")(uri "${{KIPRJMOD}}/{n}.kicad_sym")(options "")(descr "Source proposal symbols"))'
        for n in ('RaceRemote_Motor', 'RaceRemote_LogicPower')) + ')\n', encoding='utf-8')
    pro = DEST / 'drive-power-carrier.kicad_pro'
    if not pro.exists():
        pro.write_text('{"meta":{"filename":"drive-power-carrier.kicad_pro","version":3}}\n', encoding='utf-8')


def fpname(ref):
    if ref == 'U2': return 'TPS73701_DRB_3x3_EP'
    if ref == 'U3': return 'SOT23_5_ADI_90_0174_B'
    if ref == 'R7': return 'R_1206'
    if ref.startswith('R'): return 'R_0805'
    if ref.startswith('C'): return 'C_0805'
    return 'WirePad_1mm'


def v(x, y):
    return p.VECTOR2I(p.FromMM(100 + x), p.FromMM(100 + y))


def size(x, y):
    return p.VECTOR2I(p.FromMM(x), p.FromMM(y))


def pcb():
    motor.DEST = BUILD
    motor.LIB = BUILD / 'RaceRemote_Motor.pretty'
    BUILD.mkdir(parents=True, exist_ok=True)
    motor.main()
    b = p.LoadBoard(str(BUILD / 'motor-carrier.kicad_pcb'))
    settings = b.GetDesignSettings()
    # Fine-pitch fanout proposal; supplier acceptance is not established.
    settings.m_MinClearance = p.FromMM(.15)
    settings.m_TrackMinWidth = p.FromMM(.15)
    settings.m_NetSettings.GetDefaultNetclass().SetClearance(p.FromMM(.15))
    nets = {n.GetNetname(): n for n in b.GetNetInfo().NetsByNetcode().values() if n.GetNetname()}
    for name in ('WAVE_5V', 'LDO_4V', 'FB', 'XIAO_BAT'):
        nets[name] = p.NETINFO_ITEM(b, name); b.Add(nets[name])
    positions = {}
    for f in b.GetFootprints():
        f.SetPath(p.KIID_PATH('/' + uid('sheet') + '/' + motor_uid(f.GetReference())))
    def graphic(owner, kind, start, end, layer, width=.1):
        g = p.PCB_SHAPE(owner); g.SetShape(kind); g.SetStart(v(*start)); g.SetEnd(v(*end))
        g.SetLayer(layer); g.SetWidth(p.FromMM(width)); owner.Add(g)
    def new(ref, value, x, y, side='B', body=(2, 1.25), height=.9, vertical=False):
        f = p.FOOTPRINT(b); f.SetReference(ref); f.SetValue(value); f.SetPosition(v(x, y))
        f.Reference().SetVisible(False); f.Value().SetVisible(False)
        f.SetAttributes(p.FP_THROUGH_HOLE if ref.startswith('H') else p.FP_SMD)
        if side == 'B': f.SetLayer(p.B_Cu)
        f.SetFPID(p.LIB_ID(LIB, fpname(ref)))
        symbol_id=uid('H10/'+motor_uid('H5')) if ref=='H10' else logic_uid(next(k for k,val in RENAME.items() if val==ref))
        f.SetPath(p.KIID_PATH('/' + uid('sheet') + '/' + symbol_id))
        b.Add(f)
        positions[ref] = {'value': value, 'center_mm': [x, y], 'side': side,
                          'body_mm': list(body), 'height_reserve_mm': height, 'vertical': vertical}
        return f
    def pad(f, number, net, x, y, w, h, hole=None):
        a = p.PAD(f); a.SetNumber(str(number)); a.SetPosition(v(x, y)); a.SetSize(size(w, h))
        if hole:
            a.SetAttribute(p.PAD_ATTRIB_PTH); a.SetShape(p.PAD_SHAPE_CIRCLE)
            a.SetDrillSize(size(hole, hole)); ls = p.LSET.AllCuMask(2)
            ls.AddLayer(p.F_Mask); ls.AddLayer(p.B_Mask)
        else:
            a.SetAttribute(p.PAD_ATTRIB_SMD); a.SetShape(p.PAD_SHAPE_ROUNDRECT); a.SetRoundRectRadiusRatio(.15)
            ls = p.LSET(); back = f.GetLayer() == p.B_Cu
            for layer in ([p.B_Cu, p.B_Mask, p.B_Paste] if back else [p.F_Cu, p.F_Mask, p.F_Paste]): ls.AddLayer(layer)
        a.SetLayerSet(ls)
        if net is None:
            net = f'unconnected-({f.GetReference()}-NC-Pad{number})'
            if net not in nets:
                nets[net] = p.NETINFO_ITEM(b, net); b.Add(nets[net])
        a.SetNet(nets[net])
        f.Add(a)
    def outline(f, x, y, w, h, margin=.3):
        back = f.GetLayer() == p.B_Cu
        graphic(f, p.SHAPE_T_RECT, (x-w/2,y-h/2), (x+w/2,y+h/2), p.B_Fab if back else p.F_Fab)
        graphic(f, p.SHAPE_T_RECT, (x-w/2-margin,y-h/2-margin), (x+w/2+margin,y+h/2+margin), p.B_CrtYd if back else p.F_CrtYd, .05)
    u = new('U2', 'TPS73701DRBR', 20.5, 11, body=(3.1,3.1), height=1.05)
    mapping = {1:'LDO_4V',2:None,3:'FB',4:'DRIVE_GND',5:'WAVE_5V',6:None,7:None,8:'WAVE_5V'}
    for pin, dx, dy in [(1,1.4,-.975),(2,1.4,-.325),(3,1.4,.325),(4,1.4,.975),
                        (5,-1.4,.975),(6,-1.4,.325),(7,-1.4,-.325),(8,-1.4,-.975)]:
        pad(u,pin,mapping[pin],20.5+dx,11+dy,.6,.31)
    pad(u,9,'DRIVE_GND',20.5,11,1.5,1.75)
    outline(u,20.5,11,3.4,3.1,.25)
    # ADI 21-0057 K: D/E max3mm plus conservative .25mm protrusion each side.
    # Land pattern 90-0174 B: rectangular1.30x.55, row spacing2.50, pitch.95mm.
    u = new('U3','MAX40200AUK+T',24.6,10.8,body=(3.5,3.5),height=1.5)
    for pin,dx,dy,net in [(1,1.25,-.95,'LDO_4V'),(2,1.25,0,'DRIVE_GND'),(3,1.25,.95,'LDO_4V'),(4,-1.25,.95,None),(5,-1.25,-.95,'XIAO_BAT')]:
        pad(u,pin,net,24.6+dx,10.8+dy,1.3,.55)
    for a in u.Pads(): a.SetShape(p.PAD_SHAPE_RECT)
    outline(u,24.6,10.8,3.5,3.5,.25)
    parts = [
        ('R5','23.2k / 0.1%',23,15.6,'LDO_4V','FB','B',True),
        ('R6','10k / 0.1%',25,15.6,'FB','DRIVE_GND','B',True),
        ('R7','360 / 1% / 0.25W',19.7,16,'LDO_4V','DRIVE_GND','F',False),
        ('C3','4.7u / 10V X7R',19,4.2,'WAVE_5V','DRIVE_GND','B',False),
        ('C4','4.7u / 10V X7R',19.3,15.4,'LDO_4V','DRIVE_GND','B',False),
        ('C5','1u / 10V X7R',23,7.8,'LDO_4V','DRIVE_GND','B',False),
        ('C6','1u / 10V X7R',27.8,6.5,'XIAO_BAT','DRIVE_GND','B',True)]
    for ref,value,x,y,n1,n2,side,vertical in parts:
        large = ref == 'R7'; w,h = (3.2,1.6) if large else (2,1.25)
        if vertical: w,h = h,w
        f = new(ref,value,x,y,side,(w,h),.9,vertical)
        distance = 1.4 if large else .95
        pw,ph = (1.5,1.8) if large else (1.1,1.45)
        for pin, sign, net in [(1,-1,n1),(2,1,n2)]:
            if vertical: pad(f,pin,net,x,y+sign*distance,ph,pw)
            else: pad(f,pin,net,x+sign*distance*(-1 if side=='B' else 1),y,pw,ph)
        outline(f,x,y,(1.45 if vertical else 4.3 if large else 3.0),(3.0 if vertical else 1.8 if large else 1.45),.25)
    for ref,x,y,net in [('H8',28,2,'WAVE_5V'),('H9',28.2,9.5,'XIAO_BAT'),('H10',18,1.7,'DRIVE_GND')]:
        f = new(ref,net,x,y,'F',(.6,.6),.9);pad(f,1,net,x,y,2.2,2.2,1)
        graphic(f,p.SHAPE_T_CIRCLE,(x,y),(x+1.35,y),p.F_CrtYd,.05)

    def track(net, layer, points, width=.3):
        for a,z in zip(points,points[1:]):
            t=p.PCB_TRACK(b);t.SetStart(v(*a));t.SetEnd(v(*z));t.SetWidth(p.FromMM(width));t.SetLayer(layer);t.SetNet(nets[net]);b.Add(t)
    def via(net,x,y,diameter=.6,drill=.3):
        t=p.PCB_VIA(b);t.SetPosition(v(x,y));t.SetWidth(p.FromMM(diameter));t.SetDrill(p.FromMM(drill));t.SetViaType(p.VIATYPE_THROUGH);t.SetLayerPair(p.F_Cu,p.B_Cu);t.SetNet(nets[net]);b.Add(t)
    # Only replace this particular original VM path, not other traction copper.
    removed = []
    for t in list(b.GetTracks()):
        if isinstance(t,p.PCB_VIA) or t.GetNetname() != 'VM' or t.GetLayer()!=p.F_Cu: continue
        endpoints={(round(p.ToMM(z.x)-100,4),round(p.ToMM(z.y)-100,4)) for z in [t.GetStart(),t.GetEnd()]}
        if endpoints in ({(21.25,6),(21.25,13)},{(21.25,13),(28,13)}):
            removed.append(sorted(endpoints));b.Remove(t)
    assert len(removed)==2
    track('VM',p.F_Cu,[(21.25,6),(21.25,7),(18,7),(18,14),(26,14),(28,13)],1.2)
    track('WAVE_5V',p.B_Cu,[(28,2),(19.95,2),(19.95,4.2),(19.1,6),(19.1,10.025)],.6)
    track('WAVE_5V',p.B_Cu,[(19.1,10.025),(18.3,10.025),(18.3,11.975),(19.1,11.975)],.2)
    track('LDO_4V',p.B_Cu,[(21.9,10.025),(21.9,9.05),(25.85,9.05),(25.85,9.85)],.4)
    track('LDO_4V',p.B_Cu,[(23.95,7.8),(23.95,9.05)],.4)
    track('LDO_4V',p.B_Cu,[(25.85,9.85),(26.8,9.85),(26.8,11.75),(25.85,11.75)],.2)
    track('LDO_4V',p.B_Cu,[(21.9,10.025),(21.9,9.4)],.3);via('LDO_4V',21.9,9.4)
    track('LDO_4V',p.F_Cu,[(21.9,9.4),(21.7,12.7)],.4);via('LDO_4V',21.7,12.7)
    track('LDO_4V',p.B_Cu,[(21.7,12.7),(21.9,12.9),(21.9,14.65),(23,14.65)],.4)
    track('LDO_4V',p.B_Cu,[(21.9,14.65),(20.25,15.4)],.4)
    track('LDO_4V',p.B_Cu,[(20.25,15.4),(20.25,16.8),(17.1,16.8),(17.1,16)],.4)
    via('LDO_4V',17.1,16);track('LDO_4V',p.F_Cu,[(17.1,16),(18.3,16)],.4)
    track('FB',p.B_Cu,[(21.9,11.325),(22.425,11.325),(22.425,13),(24,13),(24,16.55),(23,16.55)],.15)
    track('FB',p.B_Cu,[(24,14.65),(25,14.65)],.15)
    track('XIAO_BAT',p.B_Cu,[(23.35,9.85),(23.35,10.4)],.4);via('XIAO_BAT',23.35,10.4)
    track('XIAO_BAT',p.F_Cu,[(23.35,10.4),(26.5,10.4),(28.2,9.5)],.6)
    track('XIAO_BAT',p.B_Cu,[(28.2,9.5),(29.3,9.5),(29.3,5.55),(27.8,5.55)],.4)
    for x in (20.15,20.85):
        for y in (10.55,11.45): via('DRIVE_GND',x,y)

    own = DEST / f'{LIB}.pretty'; own.mkdir(exist_ok=True)
    for f in b.GetFootprints():
        if f.GetReference() not in positions: continue
        template=p.FOOTPRINT(f)
        for a in template.Pads(): a.SetNetCode(0)
        # Retain each orientation-specific pad layout as a distinct footprint.
        name=fpname(f.GetReference()) + '_' + f.GetReference()
        f.SetFPID(p.LIB_ID(LIB,name)); template.SetFPID(p.LIB_ID(LIB,name))
        p.PCB_IO_MGR.FindPlugin(p.PCB_IO_MGR.KICAD_SEXP).FootprintSave(str(own),template)
    (DEST/'fp-lib-table').write_text(f'(fp_lib_table (version 7)(lib (name "RaceRemote_Motor")(type "KiCad")(uri "${{KIPRJMOD}}/../motor-carrier/RaceRemote_Motor.pretty")(options "")(descr "Existing motor carrier"))(lib (name "{LIB}")(type "KiCad")(uri "${{KIPRJMOD}}/{LIB}.pretty")(options "")(descr "Combined proposal footprints")))\n',encoding='utf-8')
    # Schematic assignment matches the per-instance library footprints.
    sch=DEST/'drive-power-carrier.kicad_sch'
    sheet=parse(sch.read_text())
    for node in sheet:
        if not isinstance(node,list) or node[0]!='symbol':continue
        props={json.loads(n[1]):n for n in node if isinstance(n,list) and n[0]=='property'}
        ref=json.loads(props['Reference'][2])
        if ref in positions:props['Footprint'][2]=q(f'{LIB}:'+fpname(ref)+'_'+ref)
    sch.write_text(render(sheet)+'\n',encoding='utf-8')
    p.SaveBoard(str(DEST/'drive-power-carrier.kicad_pcb'),b)
    p.ZONE_FILLER(b).Fill(b.Zones())
    p.SaveBoard(str(DEST/'drive-power-carrier.kicad_pcb'),b)
    data={'board_size_mm':[30,18,1.6],'new_component_positions':positions,'removed_vm_segments':removed,
          'proposed_min_clearance_mm':.15,'thermal_vias':4,'thermal_via_drill_mm':.3,
          'sot23_footprint_status':'ADI21-0057 RevK outline and mirrored manufacturer90-0174 RevB land pattern; physical/process validation pending',
          'sot23_outline_reserve_basis':'3mm D/E max plus conservative .25mm protrusion each side; Amax1.45 plus .05 seating allowance',
          'pin2571c_status':'shipping box change only; no fit/form/function impact per official notice',
          'pads':[],'physical_tests':False,'fabrication_released':False}
    for f in b.GetFootprints():
        for a in f.Pads():
            z=a.GetPosition();data['pads'].append({'ref':f.GetReference(),'pin':a.GetNumber(),'net':a.GetNetname(),
                'xy_mm':[round(p.ToMM(z.x)-100,5),round(p.ToMM(z.y)-100,5)]})
    (DEST/'geometry.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
    print('Built combined candidate:',len(list(b.GetFootprints())),'positions. Native checks pending.')


if __name__ == '__main__':
    DEST.mkdir(parents=True,exist_ok=True)
    schematic()
    pcb()
