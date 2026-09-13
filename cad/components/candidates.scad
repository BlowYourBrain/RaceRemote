// Models derived from the specifically documented candidates, millimetres.
// Details without a dimensioned source are deliberately omitted.
part="motor";
$fn=128;
module motor_case(length=25.3,diameter=20.4,flat_height=15.1) {
    intersection() {
        rotate([0,90,0]) cylinder(h=length,d=diameter);
        translate([0,-diameter/2,-flat_height/2]) cube([length,diameter,flat_height]);
    }
}
module motor() {
    // Front case face X=0; shaft points in -X, 11 mm from that face.
    // Front boss, rear cap details and electrical terminals not modelled.
    union() {
        motor_case();
        translate([-11,0,0]) rotate([0,90,0]) cylinder(h=11.1,d=2);
    }
}
if(part=="motor") motor();
else if(part=="motor_upper_case") motor_case(25.6,20.7,15.4);
else if(part=="servo_case") cube([23,29,12.2]); // Body envelope only; lying flat.
else if(part=="buck") cube([33,16,5.7]); // Published low module outline; no -M terminals.
else if(part=="bearing") difference() { cylinder(h=5,d=16); translate([0,0,-.1]) cylinder(h=5.2,d=5); }
else assert(false,"Unknown candidate");
