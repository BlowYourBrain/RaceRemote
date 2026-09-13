// USB-MOUNT-01 proposal 0.3. mm, front +Y; side opening faces +X.
// Connector: GCT USB4105-GF-A maximum envelope from drawing B4.
// Daughterboard/fasteners/cable are design budgets, NOT purchased modules.
use <adjustable-layout.scad>
use <body-study.scad>
use <assembly.scad>
use <battery-holder.scad>
use <components/usb-port-pcb.scad>
part="assembly";
$fn=48;
face_x=59.1;
port_y=59;
pcb_top=23.15;
port_z=pcb_top+3.46/2;

module fixing_holes(z,h,r=1.2) {
    for(y=[51,67]) translate([55.25,y,z]) cylinder(h=h,r=r);
}
module port_base() {
    difference() {
        union() {
            fixed_base();
            // These tabs belong to the stationary base, not the sliding tray.
            for(y=[48,64]) translate([49.75,y,24]) cube([7.75,6,2]);
            for(y=[51,67]) translate([55.25,y,pcb_top]) cylinder(h=24-pcb_top,r=2.25);
        }
        fixing_holes(22.0,4.2);
        // Clearance pocket for the upper receptacle; avoids the moving tray.
        translate([50.8,54.1,23]) cube([8,9.8,3.8]);
    }
}
module port_pcb() {
    difference() {
        translate([50.9,46,22.15]) cube([7.6,26,1]);
        fixing_holes(22,1.4);
        usb_port_drills();
    }
}
module connector() {
    // Maximum external body; electrical contacts are not modelled.
    translate([face_x-7.5,port_y-9.09/2,pcb_top]) cube([7.5,9.09,3.46]);
    usb_port_stakes();
}
module pcb_components() {
    // Resistor body and soldered wire budgets at the routed pad positions.
    usb_port_underside();
}
module fasteners() {
    for(y=[51,67]) translate([55.25,y,0]) {
        translate([0,0,20.4]) cylinder(h=5.6,r=1);
        translate([0,0,26]) cylinder(h=2,r=2);
        translate([0,0,20.55]) cylinder(h=1.6,r=2.31,$fn=6);
    }
}
module opening() {
    translate([56,49,port_z-5.5]) cube([7,20,11]);
}
module port_body() { difference() { body_shell(); opening(); } }
// A raised cosmetic panel, not a flush door. Retention/edge skirt not designed.
module cosmetic_cover() { translate([2,0,0]) intersection() { body_shell(); opening(); } }
module plug_budget() {
    // Assumed overmould 18 x 9 x 24; not a USB normative maximum.
    translate([face_x,port_y-9,port_z-4.5]) cube([24,18,9]);
}
module fingers_budget() {
    translate([62,port_y-20,port_z-10]) cube([28,40,20]);
}
module cable_budget() {
    translate([face_x+24,port_y,port_z]) rotate([0,90,0]) cylinder(h=20,r=3);
}

if(part=="base") port_base();
else if(part=="pcb") port_pcb();
else if(part=="connector") connector();
else if(part=="components") pcb_components();
else if(part=="copper") usb_port_copper();
else if(part=="fasteners") fasteners();
else if(part=="body") port_body();
else if(part=="cover") cosmetic_cover();
else if(part=="plug") plug_budget();
else if(part=="fingers") fingers_budget();
else if(part=="cable") cable_budget();
else if(part=="assembly") {
    color([.57,.64,.69]) import("components/zcar-without-reference-actuators.stl");
    color([.75,.75,.8]) import("components/motor-installed.stl");
    color([.2,.45,.8]) import("components/servo-case-installed.stl");
    color([.2,.65,.45]) import("components/xiao-sense-2023-installed.stl");
    color([.95,.75,.15]) import("components/buck-installed.stl");
    color("orange") battery_box();
    color([.4,.65,.6]) { port_base(); moving_tray(); camera_stage(); support(); guards(); band_corridor(); }
    color([.4,.4,.45]) { main_fasteners(); camera_fasteners(); fasteners(); }
    color([.6,.45,.6]) { adjustable_driver(); adjustable_charge(); }
    color([.1,.5,.3]) { port_pcb(); pcb_components(); }
    color("silver") connector();
    color([.9,.65,.1]) usb_port_copper();
    color([.25,.45,.7,.18]) port_body();
    color([.9,.15,.15,.25]) { plug_budget(); cable_budget(); }
} else assert(false,"Unknown part");
