// Candidate removable cradle for the pinned 2023 XIAO STEP; nominal camera +3 mm.
// Units mm. No received-board, clamping-force or print-fit qualification.
part="assembly";
use <adjustable-layout.scad>
$fn=48;
module screw_hole(x) {
    translate([x,77,43]) rotate([-90,0,0]) cylinder(d=2.4,h=11);
}
module cradle() {
    difference() {
        union() {
            // Rear plate, bottom shelf, side guides around (not across) USB.
            translate([11.75,80.4,21]) cube([22.65,1.3,24.5]);
            translate([11.75,81.7,21]) cube([23.25,3.1,.8]);
            translate([11.75,81.7,22]) cube([.9,1.75,17.98]);
            for(z=[22,36.2]) translate([34.025,81.7,z]) cube([.975,1.75,z==22?3.5:3.78]);
            // Independent sliding-stage arms, right screw moved behind USB access.
            translate([-5.25,80,29.6]) cube([17,2,1]);
            translate([33,79,29.6]) cube([1.4,2.7,1]);
            translate([33,78,29.6]) cube([13.4,2,1]);
            translate([44.4,76,29.6]) cube([10.35,2,1]);
            translate([44.4,77,29.6]) cube([2,3,1]);
            for(p=[[-2.75,81],[52.25,77]]) {
                translate([p[0],p[1],29.6]) cylinder(r=2.5,h=1);
                translate([p[0],p[1],26]) cylinder(r=1.8,h=3.6);
            }
        }
        for(p=[[-2.75,81],[52.25,77]]) translate([p[0],p[1],25.8]) cylinder(d=2.4,h=5);
        for(x=[16.5,29]) screw_hole(x);
    }
}
module cap() {
    difference() {
        union() {
            translate([13.75,82,40]) cube([18,2.45,5.5]);
            for(x=[15,27.5]) translate([x,83.45,38.9]) cube([3,1,1.1]);
        }
        for(x=[16.5,29]) screw_hole(x);
    }
}
module clamp_screws() {
    // Geometric M2-class screw/nut allocations, lengths/SKU not selected.
    for(x=[16.5,29]) {
        translate([x,78.5,43]) rotate([-90,0,0]) cylinder(r=1,h=6);
        translate([x,84.45,43]) rotate([-90,0,0]) cylinder(r=2,h=1.6);
        translate([x,78.8,43]) rotate([-90,0,0]) cylinder(r=2.31,h=1.6,$fn=6);
    }
}
module service_plug() {
    // Reference USB opening is toward +X. Approximate mating shell/cable allocation.
    translate([35.36,80.5,26.2]) cube([24,7.5,10]);
}
module tray_clearance() {
    difference() {
        import("packaging-v05/tray.stl");
        // Local edge relief for the rearward right mounting post at nominal pose.
        translate([50,74,26]) cube([1.1,7,2]);
    }
}
module stage_screws() {
    screw_budget(-2.75,81,30.6);
    screw_budget(52.25,77,30.6);
}
if(part=="cradle") cradle();
else if(part=="cap") cap();
else if(part=="screws") clamp_screws();
else if(part=="usb_access") service_plug();
else if(part=="tray_clearance") tray_clearance();
else if(part=="stage_screws") stage_screws();
else if(part=="cradle_print") translate([5.25,45.5,-74.5]) rotate([90,0,0]) cradle();
else if(part=="cap_print") translate([-13.75,45.5,-82]) rotate([90,0,0]) cap();
else {
    color([.15,.6,.4]) translate([0,3,0]) import("components/xiao-sense-2023-installed.stl");
    color([.35,.6,.8]) cradle();
    color("orange") cap();
    color("silver") clamp_screws();
    color([.7,.7,.7,.25]) service_plug();
}
