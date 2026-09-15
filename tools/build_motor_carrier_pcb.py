"""TA6586 two-layer carrier candidate. Run with KiCad Python; no hardware I/O."""
import json
from pathlib import Path
import pcbnew as p
from motor_carrier_identity import uid, footprint

ROOT=Path(__file__).resolve().parents[1]
DEST=ROOT/'hardware/motor-carrier'
LIB=DEST/'RaceRemote_Motor.pretty'


def v(x,y): return p.VECTOR2I(p.FromMM(100+x),p.FromMM(100+y))
def sz(x,y): return p.VECTOR2I(p.FromMM(x),p.FromMM(y))


def main():
    LIB.mkdir(parents=True,exist_ok=True)
    b=p.BOARD(); settings=b.GetDesignSettings();settings.SetBoardThickness(p.FromMM(1.6))
    settings.m_MinClearance=p.FromMM(.25);settings.m_CopperEdgeClearance=p.FromMM(.3)
    settings.m_HoleClearance=p.FromMM(.25);settings.m_TrackMinWidth=p.FromMM(.25)
    settings.m_MinThroughDrill=p.FromMM(.3);settings.m_HoleToHoleMin=p.FromMM(.3)
    settings.m_NetSettings.GetDefaultNetclass().SetClearance(p.FromMM(.25))
    nets={}
    for name in ['CMD_FI','CMD_BI','FI','BI','DRIVE_GND','VM','FO','BO']:
        nets[name]=p.NETINFO_ITEM(b,name);b.Add(nets[name])
    all_layers=p.LSET.AllCuMask(2);all_layers.AddLayer(p.F_Mask);all_layers.AddLayer(p.B_Mask)
    placement={}; footprints={}
    def graphic(owner,shape,start,end,layer,width=.12):
        item=p.PCB_SHAPE(owner);item.SetShape(shape);item.SetStart(v(*start));item.SetEnd(v(*end))
        item.SetLayer(layer);item.SetWidth(p.FromMM(width));owner.Add(item)
    for a,c in zip([(0,0),(30,0),(30,18),(0,18)],[(30,0),(30,18),(0,18),(0,0)]):
        graphic(b,p.SHAPE_T_SEGMENT,a,c,p.Edge_Cuts,.05)
    def new(ref,value,x,y,side='F'):
        f=p.FOOTPRINT(b);f.SetReference(ref);f.SetValue(value);f.SetPosition(v(x,y));
        f.Reference().SetVisible(False);f.Value().SetVisible(False)
        f.SetAttributes(p.FP_SMD if ref in ['R1','R2','R3','R4','C1'] else p.FP_THROUGH_HOLE)
        if side=='B': f.SetLayer(p.B_Cu)
        f.SetFPID(p.LIB_ID(*footprint(ref).split(':')));f.SetPath(p.KIID_PATH('/'+uid('sheet')+'/'+uid(ref)))
        b.Add(f);footprints[ref]=f;placement[ref]={'value':value,'center_mm':[x,y],'side':side}
        return f
    def pad(f,number,net,x,y,width=1.7,height=1.7,drill=.9,smd=False,rect=False,side='F'):
        a=p.PAD(f);a.SetNumber(str(number));a.SetPosition(v(x,y));a.SetSize(sz(width,height))
        a.SetShape(p.PAD_SHAPE_RECT if rect else p.PAD_SHAPE_CIRCLE if not smd else p.PAD_SHAPE_ROUNDRECT)
        a.SetAttribute(p.PAD_ATTRIB_SMD if smd else p.PAD_ATTRIB_PTH)
        if smd:
            layers=p.LSET();layers.AddLayer(p.B_Cu if side=='B' else p.F_Cu)
            layers.AddLayer(p.B_Mask if side=='B' else p.F_Mask);layers.AddLayer(p.B_Paste if side=='B' else p.F_Paste);a.SetLayerSet(layers)
            a.SetRoundRectRadiusRatio(.2)
        else: a.SetDrillSize(sz(drill,drill));a.SetLayerSet(all_layers)
        a.SetNet(nets[net]);f.Add(a)
    def rectangle(f,x0,y0,x1,y1,layer,width=.1):
        graphic(f,p.SHAPE_T_RECT,(x0,y0),(x1,y1),layer,width)
    u=new('U1','TA6586 DIP8',12,9)
    u.GetField(p.FIELD_T_DATASHEET).SetText('https://static.chipdip.ru/lib/923/DOC012923012.pdf')
    # Source pin numbers, top view. Physical 2.54 pitch / 7.62 row span.
    for number,x,y,net in [(1,8.19,5.19,'BI'),(2,10.73,5.19,'FI'),(3,13.27,5.19,'DRIVE_GND'),(4,15.81,5.19,'VM'),
                            (5,15.81,12.81,'FO'),(6,13.27,12.81,'FO'),(7,10.73,12.81,'BO'),(8,8.19,12.81,'BO')]:
        pad(u,number,net,x,y,rect=number==1)
    rectangle(u,7.4,5.775,16.6,12.225,p.F_Fab)
    rectangle(u,7,4,17,14,p.F_CrtYd,.05)
    rectangle(u,7.3,6.4,16.7,11.6,p.F_SilkS)
    graphic(u,p.SHAPE_T_CIRCLE,(7.8,6.8),(8.05,6.8),p.F_SilkS)
    for ref,value,x,y,n1,n2,side in [('R1','100 / 1%',5,3,'CMD_FI','FI','F'),
        ('R2','100 / 1%',5,6,'CMD_BI','BI','F'),('R3','10k / 1%',10.73,2.1,'FI','DRIVE_GND','F'),
        ('R4','10k / 1%',5.1,9,'BI','DRIVE_GND','F'),('C1','100n / 25V',14.54,2.8,'VM','DRIVE_GND','B')]:
        f=new(ref,value,x,y,side)
        # Bottom capacitor polarity-free: pad1 sits nearest VCC pin4.
        direction=-1 if side=='B' else 1
        pad(f,1,n1,x-.95*direction,y,1.1,1.45,smd=True,side=side)
        pad(f,2,n2,x+.95*direction,y,1.1,1.45,smd=True,side=side)
        rectangle(f,x-1,y-.625,x+1,y+.625,p.B_Fab if side=='B' else p.F_Fab)
        rectangle(f,x-1.7,y-1,x+1.7,y+1,p.B_CrtYd if side=='B' else p.F_CrtYd,.05)
    c=new('C2','470u / 16V',23,6)
    pad(c,1,'VM',21.25,6,drill=.9,rect=True);pad(c,2,'DRIVE_GND',24.75,6,drill=.9)
    graphic(c,p.SHAPE_T_CIRCLE,(23,6),(27.25,6),p.F_Fab)
    graphic(c,p.SHAPE_T_CIRCLE,(23,6),(27.5,6),p.F_CrtYd,.05)
    graphic(c,p.SHAPE_T_CIRCLE,(23,6),(27.35,6),p.F_SilkS)
    terminals=[('H1',1.8,3,'CMD_FI'),('H2',1.8,6,'CMD_BI'),('H3',2,9,'DRIVE_GND'),
               ('H4',28,13,'VM'),('H5',28,16,'DRIVE_GND'),('H6',2,12,'FO'),('H7',2,15,'BO')]
    for ref,x,y,net in terminals:
        f=new(ref,net,x,y);pad(f,1,net,x,y,2.2,2.2,1)
        graphic(f,p.SHAPE_T_CIRCLE,(x,y),(x+1.35,y),p.F_CrtYd,.05)
    def trace(net,layer,points,width=.3):
        for a,c in zip(points,points[1:]):
            t=p.PCB_TRACK(b);t.SetStart(v(*a));t.SetEnd(v(*c));t.SetWidth(p.FromMM(width));t.SetLayer(layer);t.SetNet(nets[net]);b.Add(t)
    trace('CMD_FI',p.F_Cu,[(1.8,3),(4.05,3)])
    trace('CMD_BI',p.F_Cu,[(1.8,6),(4.05,6)])
    trace('FI',p.F_Cu,[(5.95,3),(7.5,3),(9.78,2.1)])
    trace('FI',p.F_Cu,[(9.78,2.1),(9.78,4.24),(10.73,5.19)])
    trace('BI',p.F_Cu,[(5.95,6),(8.19,5.19)])
    trace('BI',p.F_Cu,[(5.95,6),(5.95,7.6),(4.15,8),(4.15,9)])
    trace('VM',p.F_Cu,[(15.81,5.19),(18,5.19),(21.25,6)],1.2)
    trace('VM',p.F_Cu,[(21.25,6),(21.25,13),(28,13)],1.2)
    trace('VM',p.B_Cu,[(15.81,5.19),(15.49,2.8)],.6)
    trace('DRIVE_GND',p.B_Cu,[(13.27,5.19),(13.59,2.8)],.6)
    trace('FO',p.B_Cu,[(13.27,12.81),(15.81,12.81),(15.81,16.5),(4.5,16.5),(4.5,12),(2,12)],1.2)
    trace('BO',p.F_Cu,[(10.73,12.81),(8.19,12.81),(8.19,14.5),(2.5,14.5),(2,15)],1.2)
    def zone(net,layer,points,priority):
        z=p.ZONE(b);z.SetLayer(layer);z.SetNet(nets[net]);z.SetLocalClearance(p.FromMM(.25))
        z.SetAssignedPriority(priority);z.SetPadConnection(p.ZONE_CONNECTION_FULL)
        poly=z.Outline();poly.NewOutline()
        for x,y in points: a=v(x,y);poly.Append(a.x,a.y)
        b.Add(z)
    for layer in [p.F_Cu,p.B_Cu]: zone('DRIVE_GND',layer,[(.4,.4),(29.6,.4),(29.6,17.6),(.4,17.6)],0)
    zone('FO',p.B_Cu,[(4.6,11.4),(17.2,11.4),(17.2,17.5),(4.6,17.5)],1)
    zone('BO',p.F_Cu,[(.5,11),(11.9,11),(11.9,16.2),(.5,16.2)],1)
    # Save reusable pad geometry with cleared nets. No imported library license is required.
    seen=set()
    for ref,f in footprints.items():
        name=footprint(ref).split(':')[1]
        if name not in seen:
            template=p.FOOTPRINT(f)
            for a in template.Pads(): a.SetNetCode(0)
            p.PCB_IO_MGR.FindPlugin(p.PCB_IO_MGR.KICAD_SEXP).FootprintSave(str(LIB),template);seen.add(name)
    (DEST/'fp-lib-table').write_text('(fp_lib_table (version 7)(lib (name "RaceRemote_Motor")(type "KiCad")(uri "${KIPRJMOD}/RaceRemote_Motor.pretty")(options "")(descr "Original TA6586 carrier footprints")))\n',encoding='utf-8')
    p.SaveBoard(str(DEST/'motor-carrier.kicad_pcb'),b)
    p.ZONE_FILLER(b).Fill(b.Zones());p.SaveBoard(str(DEST/'motor-carrier.kicad_pcb'),b)
    geometry={'board_size_mm':[30,18,1.6],'component_positions':placement,'pads':[],
              'capacitor_reference':'Rubycon 16ZLH470MEFC8X11.5; candidate, not purchased',
              'capacitor_case_max_mm':[8.5,13],'capacitor_seating_gap_proposal_mm':.5,
              'bottom_capacitor_height_reserve_mm':.9,'total_envelope_height_mm':16.0}
    for f in b.GetFootprints():
        for a in f.Pads():
            pt=a.GetPosition();geometry['pads'].append({'ref':f.GetReference(),'pin':a.GetNumber(),'net':a.GetNetname(),
                 'xy_mm':[round(p.ToMM(pt.x)-100,5),round(p.ToMM(pt.y)-100,5)]})
    (DEST/'geometry.json').write_text(json.dumps(geometry,indent=2)+'\n',encoding='utf-8')
    print('Generated carrier PCB; fourteen components, 30 x 18 mm, not thermally/current qualified')


if __name__=='__main__': main()
