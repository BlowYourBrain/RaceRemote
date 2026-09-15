// MOTOR-MOUNT-01 v0.1. Candidate, mm; nominal electronics0/camera+3 only.
// Right fixed lip, left removable clamp. No threaded plastic holes.
use <harness-guides.scad>
part="assembly";
pcb_side_gap=.3;
pcb_top_gap=.3;
$fn=48;
module nut_pocket(x,y) {
    translate([x,y,27.6]) rotate([0,0,30]) cylinder(r=4.4/sqrt(3),h=1.8,$fn=6);
    // Slide the nut in from the left before fitting the neighbouring buck.
    // Hex across-corners span exceeds4mm across-flats: opening must clear it too.
    translate([19.8,y-2.55,27.6])cube([3.2,5.1,1.8]);
}
module holder() {
    difference() {
        union() {
            // Edge support at PCB underside Z29.5. Middle remains open for C1/tails.
            translate([25.75,52,27.2])cube([.9,14,2.3]);
            translate([42.8,52,27.2])cube([2.4,14,2.3]);
            // Candidate print clearances over nominal1.6 PCB; tune after gauge print.
            translate([43.75+pcb_side_gap,52,29.5])cube([1.45-pcb_side_gap,14,1.6+pcb_top_gap]);
            translate([42.8,52,31.1+pcb_top_gap])cube([2.4,14,1.2]);
            // Left nut towers, separated from PCB edge by .25.
            for(y=[57,63])translate([20, y-2.8,27.2])cube([5.5,5.6,3.9]);
            // Stops restrain fore/aft sliding on short, unpopulated edges.
            for(y=[46,77.2])translate([36, y,27.2])cube([4,.8,4]);
        }
        for(y=[57,63]) {
            translate([23,y,27])cylinder(d=2.4,h=5);
            nut_pocket(23,y);
        }
    }
}
module tray() {
    difference() {
        union(){tray_with_guide();holder();}
        // Continue screw clearance through the pre-existing tray floor.
        for(y=[57,63])translate([23,y,26])cylinder(d=2.4,h=7);
    }
}
module clamp() {
    difference() {
        translate([20.2,54.2,31.1])cube([6.6,11.6,1.6]);
        for(y=[57,63])translate([23,y,30.9])cylinder(d=2.4,h=2);
    }
}
module screws() {
    for(y=[57,63]) {
        // M2x6 under-head length; head envelope D3.8 x2, unthreaded CAD cylinder.
        translate([23,y,26.7])cylinder(d=2,h=6);
        translate([23,y,32.7])cylinder(d=3.8,h=2);
    }
}
module nuts() {
    for(y=[57,63])difference() {
        translate([23,y,27.7])rotate([0,0,30])cylinder(r=4/sqrt(3),h=1.6,$fn=6);
        translate([23,y,27.6])cylinder(d=2.0,h=1.8);
    }
}
module other_pads() {
    difference() {
        import("packaging-v05/pads.stl");
        translate([25,50,27])cube([19,23,2]);
    }
}
if(part=="holder")holder();
else if(part=="tray")tray();
else if(part=="clamp")clamp();
else if(part=="screws")screws();
else if(part=="nuts")nuts();
else if(part=="pads")other_pads();
else if(part=="tray_print")translate([2.5,-30,-26.3])tray();
else if(part=="clamp_print")translate([-20.2,-54.2,-31.1])clamp();
else if(part=="pcb_gauge_print")cube([18,30,1.6]);
else {
    color([.35,.65,.6])tray();
    color([1,.55,.15])clamp();
    color("silver"){screws();nuts();}
    %color([.15,.5,.3])import("motor-carrier/carrier.stl");
}
