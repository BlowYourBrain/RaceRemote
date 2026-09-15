// Printed routing guides, nominal 0/+3 mm. Slot sizes are candidate tie/lacing space.
use <xiao-mount.scad>
part="assembly";
$fn=48;
module power_guide() {
    difference() {
        translate([-2.5,57.1,27]) cube([5,5.8,7.8]);
        // Follows the existing inclined power corridor (7 mm rise in 48 mm).
        translate([0,55,32.8125-5*7/48]) rotate([-81.7029,0,0]) cylinder(r=1.25,h=11);
        translate([-1.25,56.9,32.2]) cube([2.5,6.2,4]);
        // Pass a removable tie under the saddle. Width 2.8, thickness 1.3.
        translate([-3,58.6,29.3]) cube([6,2.8,1.3]);
    }
}
module servo_guide() {
    difference() {
        union() {
            translate([31.5,82.5,41]) cube([15,3,1.2]);
            translate([41.5,82.5,37.3]) cube([5,3,4.9]);
        }
        translate([44,82,39.5]) rotate([-90,0,0]) cylinder(r=1.25,h=4);
        translate([42.75,82,39.5]) cube([2.5,4,4]);
        // Lacing aperture in the wing; the wire is laid into an open saddle.
        translate([39.5,82.6,40.8]) cube([1.3,2.8,1.6]);
    }
}
module tray_with_guide(){union(){tray_clearance();power_guide();}}
module cap_with_guide(){union(){cap();servo_guide();}}
if(part=="power_guide")power_guide();
else if(part=="servo_guide")servo_guide();
else if(part=="tray")tray_with_guide();
else if(part=="cap")cap_with_guide();
else if(part=="tray_print")translate([2.5,-30,-26.3])tray_with_guide();
else if(part=="cap_print")translate([-13.75,45.5,-82])rotate([90,0,0])cap_with_guide();
else {
    color([.4,.65,.6])tray_with_guide();
    color([.25,.5,.8])cradle();
    color("orange")cap_with_guide();
    color([.15,.6,.4])translate([0,3,0])import("components/xiao-sense-2023-installed.stl");
    color([.8,.1,.1])import("packaging-v05/wire-power.stl");
    color([.1,.3,.8])import("xiao-mount/wire-servo.stl");
}
