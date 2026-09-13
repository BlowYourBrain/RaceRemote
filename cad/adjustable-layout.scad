// Adjustable packaging proposal 0.3; mm. NOT print-ready.
// Two independently clamped stages; adjust with the car powered off.
use <assembly.scad>
use <body-study.scad>
use <battery-holder.scad>
part="assembly";
electronics_slide=0; // Proposed nominal limits -3 .. +8 mm.
camera_slide=0;      // Same limits; compatible combinations checked separately.
$fn=48;

module slot(x,a,b) {
    hull() for(y=[a,b]) translate([x,y,23.8]) cylinder(h=2.4,r=1.2);
}
module fixed_base() {
    difference() {
        union() {
            envelope([60,48,2],[-5.25,40,24]);
            for(x=[4.25,39.25],y=[45,70]) envelope([6,6,5.75],[x,y,20.25]);
        }
        // Opening for the low independently movable camera support.
        envelope([28,17,3],[10.75,72,23.5]);
        for(x=[1.25,48.25]) { slot(x,46,57); slot(x,66,77); }
        for(x=[-2.75,52.25]) slot(x,75,86);
    }
}
module moving_tray() {
    translate([0,electronics_slide,0]) difference() {
        envelope([52,36,1],[-1.25,40,26.3]);
        for(x=[1.25,48.25],y=[49,69]) translate([x,y,26]) cylinder(h=2,r=1.2);
    }
}
module camera_stage() {
    translate([0,camera_slide,0]) difference() {
        union() {
            // Tabs carry two screws; bracket descends through the base opening.
            envelope([60,2,1],[-5.25,77,29.6]);
            for(x=[-2.75,52.25]) {
                translate([x,78,29.6]) cylinder(h=1,r=2.5);
                translate([x,78,26]) cylinder(h=3.6,r=1.8);
            }
            envelope([27,2,23],[11.25,77,21]);
            envelope([23,12,1],[13.25,79,21]);
        }
        // Keep central camera volume and upper bracket window clear.
        envelope([17,3,13],[16.25,76.5,28]);
        for(x=[-2.75,52.25]) translate([x,78,25.8]) cylinder(h=5,r=1.2);
    }
}
module screw_budget(x,y,head_z=27.3) {
    // M2-class space reservation, including nut/head; not a chosen SKU.
    translate([x,y,0]) union() {
        translate([0,0,22.4]) cylinder(h=head_z-22.4,r=1);
        translate([0,0,head_z]) cylinder(h=2,r=2.5);
        translate([0,0,22.4]) cylinder(h=1.6,r=2.31,$fn=6);
    }
}
module main_fasteners() {
    for(x=[1.25,48.25],y=[49,69]) screw_budget(x,y+electronics_slide);
}
module camera_fasteners() {
    for(x=[-2.75,52.25]) screw_budget(x,78+camera_slide,30.6);
}
module adjustable_camera() { envelope([23,12,21],[13.25,79+camera_slide,22]); }
module adjustable_driver() { envelope([30,18,10],[9.75,40+electronics_slide,27.6]); }
module adjustable_power() { envelope([33,16,5.7],[8.25,60+electronics_slide,27.6]); }
module adjustable_charge() { envelope([33,16,8],[8.25,60+electronics_slide,34.3]); }

if(part=="carrier_installed") fixed_base();
else if(part=="carrier_print") translate([5.25,-40,-20.25]) fixed_base();
else if(part=="tray") moving_tray();
else if(part=="camera_mount") camera_stage();
else if(part=="main_fasteners") main_fasteners();
else if(part=="camera_fasteners") camera_fasteners();
else if(part=="adhesive") adhesive_budget();
else if(part=="battery") battery_box();
else if(part=="power") adjustable_power();
else if(part=="camera") adjustable_camera();
else if(part=="driver") adjustable_driver();
else if(part=="charge") adjustable_charge();
else if(part=="body") body_shell();
else if(part=="cavity") body_volume(1);
else if(part=="assembly") {
    color([.55,.62,.67]) import("reference/zcar-aligned.stl");
    color("orange") battery_box();
    color("seagreen") support();
    color("purple") guards();
    color([.2,.2,.2]) band_corridor();
    color([.15,.65,.62]) fixed_base();
    color([.4,.8,.6]) moving_tray();
    color([.25,.6,.4]) camera_stage();
    color([.4,.4,.45]) { main_fasteners(); camera_fasteners(); }
    color([.95,.75,.15]) adjustable_power();
    color([.75,.2,.7]) adjustable_camera();
    color([.9,.3,.25]) adjustable_driver();
    color([.45,.35,.85]) adjustable_charge();
    color([.25,.45,.7,.18]) body_shell();
} else assert(false,"Unknown part");
