// USB-MOUNT-01 proposal 0.4. mm, front +Y; side opening faces +X.
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
magnet_d=3; // Proposed disc envelope, no purchased SKU or force claim.
magnet_pocket_d=3.3;
cap_gap=.3; // Per-side guide clearance; validate on a print coupon.

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
module x_cylinder(x,y,z,h,d) {
    translate([x,y,z]) rotate([0,90,0]) cylinder(h=h,d=d);
}
module magnet_pockets(x) {
    for(y=[46.75,71.25]) x_cylinder(x,y,port_z,1.21,magnet_pocket_d);
}
// Integral surround on the PROPOSED printed body, not the PCB/base.
// Top/bottom webs join the hypothetical shell; their middle is relieved for
// the PCB, solder and M2 hardware. A purchased body needs its own adapter.
module cover_bezel() {
    difference() {
        union() {
            translate([59.9,44.5,port_z-7.5]) cube([2.6,29,15]);
            translate([55.5,44.5,port_z+5.5]) cube([6,29,2]);
            translate([58.5,44.5,port_z-7.5]) cube([3,29,2]);
        }
        translate([55,49,port_z-5.5]) cube([8,20,11]);
        magnet_pockets(61.3);
    }
}
module port_body() {
    union() {
        difference() { body_shell(); opening(); }
        cover_bezel();
    }
}
module cosmetic_cover() {
    difference() {
        union() {
            // Face down for printing. No flexible snap features required.
            translate([63,43,port_z-9]) cube([2.4,32,18]);
            difference() {
                translate([60.7,43,port_z-9]) cube([2.31,32,18]);
                translate([60.6,44.5-cap_gap,port_z-7.5-cap_gap])
                    cube([2.5,29+2*cap_gap,15+2*cap_gap]);
            }
            // Three hard stops maintain the 0.5 mm face gap; magnets do not
            // locate the lid or take the lateral load by themselves.
            for(y=[50,68]) translate([62.5,y-.7,port_z+6]) cube([.51,1.4,1]);
            translate([62.5,58.3,port_z-7]) cube([.51,1.4,1]);
            // Lower finger lip, accessible with the body left in place.
            translate([62,55,port_z-10.5]) cube([3.4,8,1.8]);
        }
        magnet_pockets(62.99);
    }
}
module bezel_magnets() {
    for(y=[46.75,71.25]) x_cylinder(61.4,y,port_z,1,magnet_d);
}
module cover_magnets() {
    for(y=[46.75,71.25]) x_cylinder(63.1,y,port_z,1,magnet_d);
}
module bezel_coupon() {
    // Same guide/magnet surfaces, without the body-specific joining webs.
    intersection() {
        cover_bezel();
        translate([59.9,44,port_z-8]) cube([3,30,16]);
    }
}
// Small pocket gauge: labels are the added DIAMETER, not radial clearance.
module magnet_coupon() {
    difference() {
        cube([26,9,2.4]);
        for(i=[0:2]) {
            translate([5+8*i,4.5,1.2]) cylinder(h=1.3,d=3.1+.2*i);
            translate([2+8*i,.4,2.05]) linear_extrude(height=.5)
                text(str(.1+.2*i),size=1.5,font="Liberation Sans");
        }
    }
}
module plug_budget() {
    // Assumed overmould 18 x 9 x 24; not a USB normative maximum.
    translate([face_x,port_y-9,port_z-4.5]) cube([24,18,9]);
}
module fingers_budget() {
    translate([66.4,port_y-20,port_z-10]) cube([23.6,40,20]);
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
else if(part=="bezel") cover_bezel();
else if(part=="bezel_coupon") bezel_coupon();
else if(part=="bezel_magnets") bezel_magnets();
else if(part=="cover_magnets") cover_magnets();
else if(part=="magnet_coupon") magnet_coupon();
else if(part=="plug") plug_budget();
else if(part=="fingers") fingers_budget();
else if(part=="cable") cable_budget();
else if(part=="lid_detail") {
    color([.3,.6,.6]) cover_bezel();
    color("silver") bezel_magnets();
    translate([8,0,0]) {
        color([.25,.45,.7]) cosmetic_cover();
        color("silver") cover_magnets();
    }
}
else if(part=="assembly" || part=="assembly_closed") {
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
    if(part=="assembly_closed") color([.25,.45,.7]) cosmetic_cover();
    else color([.9,.15,.15,.25]) { plug_budget(); cable_budget(); }
} else assert(false,"Unknown part");
