// Packaging proposal 0.5; dimensions in mm. Not a validated manufactured assembly.
// Exact imported references and dimensional assumptions are documented in README.md.
use <../body-study.scad>
use <../battery-holder.scad>
use <../adjustable-layout.scad>
use <../usb-port-study.scad>
part="assembly";
electronics_slide=0;
camera_slide=3;
$fn=48;

// Board length runs fore/aft. Input terminal faces front/left of camera; header faces up.
module buck_local(which="all") {
    if(which=="pcb" || which=="all") color("darkgreen") difference() {
        cube([16,33,1.2]);
        // Coordinates read from Waveshare's dimensioned top view; hole dia is an estimate.
        for(x=[2.55,6.05,9.55,13.05]) translate([x,3.6,-.1]) cylinder(d=1.2,h=1.4);
        for(i=[0:5]) translate([1.6+2.54*i,31.4,-.1]) cylinder(d=1.0,h=1.4);
    }
    if(which=="components" || which=="all") {
        // Visible arrangement follows the vendor photo. Package sizes are approximate.
        color("gray") translate([2.9,19.1,1.2]) cube([10.2,10.4,4.5]);
        color("black") translate([6,8.6,1.2]) cube([4.5,4.5,1.2]);
        color("silver") for(p=[[1.5,8],[11.8,8],[1.2,23],[12.3,23]])
            translate([p[0],p[1],1.2]) cube([1.9,3.2,1.3]);
    }
    if(which=="terminals" || which=="all") {
        // -M terminal/header outlines are conservative estimates, not a vendor STEP.
        color("green") difference() {
            translate([1,0,1.2]) cube([14,7.4,10]);
            for(x=[2.75,6.25,9.75,13.25])
                translate([x,3.6,9.4]) cylinder(d=2.5,h=2);
        }
        color("gold") translate([.4,30.1,1.2]) cube([15.2,2.5,2.5]);
        color("silver") for(i=[0:5])
            translate([1.28+2.54*i,31.08,-1.5]) cube([.64,.64,11]);
    }
    if(which=="plug") {
        // Reserve for a vertical mating housing/strain relief, not a selected connector.
        translate([-.2,29.7,3.7]) cube([16.4,3.4,10]);
    }
}
module buck(which="all") { translate([19.75,79.5+electronics_slide,29.6]) rotate([0,0,180]) buck_local(which); }

module tray() { // Enlarged rear bay; cantilever/load capacity not validated.
    translate([0,electronics_slide,0]) difference() {
        translate([-1.25,30,26.3]) cube([52,48,1]);
        for(x=[1.25,48.25],y=[49,69]) translate([x,y,26]) cylinder(d=2.4,h=2);
        // Future straps through these slots secure populated boards without fake screw holes.
        for(x=[2,20.5],y=[51,69]) translate([x,y,26]) cube([1.6,5,2]);
        for(x=[24,45],y=[50,67]) translate([x,y,26]) cube([1.6,5,2]);
    }
}
module pads() {
    translate([0,electronics_slide,0]) {
        // Low dielectric support strips leave space for -M solder tails underneath.
        for(x=[4.0,17.5]) translate([x,55,27.3]) cube([2,17,2.3]);
        for(x=[26,41]) translate([x,51,27.3]) cube([2,21,1.3]);
        for(x=[9,38]) translate([x,33,27.3]) cube([2,10,1.3]);
    }
}
module driver_reserve() { translate([25.75,47+electronics_slide,28.6]) cube([18,30,10]); }
module charge_reserve() { translate([8.25,30+electronics_slide,28.6]) cube([33,16,8]); }
module antenna() { // Antenna actually supplied with XIAO is not dimensionally identified yet.
    translate([46,48,37]) cube([1,27,7]); // Adjustable 27x7x1 FPC allocation, not RF keepout validation.
}
module antenna_support() {
    // Uprights stand beyond the sliding tray, on the fixed base side rail.
    for(y=[55,73]) translate([52, y, 26]) cube([1.2,1.2,10]);
    for(y=[55,73]) translate([47,y,35]) cube([6.2,1.2,1]);
    translate([47,55,36]) cube([1.2,19.2,1]);
}
module service_usb() {
    // XIAO source STEP USB points toward -X. Access reserved only with body removed.
    translate([-3.5,79+camera_slide,27]) cube([17,9,9]);
}
module camera(){translate([0,camera_slide,0]) import("../components/xiao-sense-2023-installed.stl");}
module camera_support(){translate([0,camera_slide,0]) camera_stage();}

if(part=="buck") buck();
else if(part=="buck_pcb") buck("pcb");
else if(part=="buck_components") buck("components");
else if(part=="buck_terminals") buck("terminals");
else if(part=="buck_plug") buck("plug");
else if(part=="tray") tray();
else if(part=="pads") pads();
else if(part=="driver") driver_reserve();
else if(part=="charge") charge_reserve();
else if(part=="antenna") antenna();
else if(part=="antenna_support") antenna_support();
else if(part=="service_usb") service_usb();
else if(part=="camera_support") camera_support();
else if(part=="assembly") {
    color([.65,.67,.7]) import("mechanics-without-frame.stl");
    color([.55,.65,.7]) import("frame-relief-proposal.stl");
    color([.7,.72,.74]) import("../components/motor-installed.stl");
    color([.17,.38,.75]) import("servo-installed.stl");
    color([.14,.6,.34]) camera();
    color([.4,.7,.62]) { port_base(); tray(); pads(); camera_support(); support(); guards(); antenna_support(); }
    color([.4,.4,.42]) {main_fasteners(); translate([0,camera_slide,0]) camera_fasteners();}
    color([1,.54,.14]) battery_box();
    buck();
    color([.4,.3,.6,.45]) driver_reserve();
    color([.5,.3,.6,.45]) charge_reserve();
    color([.1,.1,.12]) antenna();
    color([.8,.2,.1]) import("wire-power.stl");
    color([.15,.25,.6]) import("wire-servo.stl");
    color([.1,.1,.1]) import("wire-antenna.stl");
    color([.8,.45,.1]) import("wire-motor.stl");
    color([.5,.25,.75]) import("wire-control.stl");
    color([.1,.5,.3]) {port_pcb(); pcb_components();}
    color("silver") connector();
    color([.1,.4,.7,.13]) port_body();
    color([.1,.4,.7]) cosmetic_cover();
}
