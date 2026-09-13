// Body-constrained concept 0.2. Millimetres; front is +Y.
// Proposed outer shape, NOT a scan/model of a purchased Mini-Z shell.
// Electronics are budgets, NOT measured boards. No print-ready shell/mounts.
use <battery-holder.scad>
use <assembly.scad>
part = "assembly";
$fn = 64;

module power_budget_low() { envelope([33,16,5.7],[8.25,60,27]); }
module charge_budget_low() { envelope([33,16,8],[8.25,60,33.7]); }
module driver_budget_low() { envelope([30,18,10],[9.75,40,27]); }
// One proposed compact ESP32 + camera assembly replaces two separate boxes.
module camera_control_budget() { envelope([23,12,21],[13.25,78,22]); }
module low_carrier() {
    difference() {
        union() {
            envelope([43,36,2],[3.25,40,24]);
            for (x=[4.25,39.25], y=[45,70])
                envelope([6,6,5.75],[x,y,20.25]);
            envelope([27,2,23],[11.25,76,21]);
            envelope([23,12,1],[13.25,78,21]);
        }
        for (x=[6.75,40.25], y=[45,63])
            envelope([2.5,10,3],[x,y,23.5]);
        envelope([17,3,13],[16.25,75.5,28]);
    }
}

// Y, roof Z, half-width at roof. Outer size 70 x 155 x 47;
// original wheel bottom Z=-7 gives overall concept height 54 mm.
stations = [[-20,16,32],[-10,30,34],[20,30,34],[40,47,25],
            [92,47,25],[110,27,34],[132,27,34],[135,16,32]];
module section(s, inset=0) {
    top=s[1]-inset;
    translate([24.75,s[0],0]) hull() {
        translate([-35+inset,0,inset ? -4 : 0]) cube([70-2*inset,0.01,1]);
        translate([-35+inset,0,min(24,top-2)]) cube([70-2*inset,0.01,0.01]);
        translate([-s[2]+inset,0,top-0.01]) cube([2*(s[2]-inset),0.01,0.01]);
    }
}
module body_volume(inset=0) {
    for (i=[0:len(stations)-2]) hull() {
        a=stations[i]; b=stations[i+1];
        section([a[0]+(i==0 ? inset : 0),a[1],a[2]],inset);
        section([b[0]-(i==len(stations)-2 ? inset : 0),b[1],b[2]],inset);
    }
}
module wheel_openings() {
    // Static illustrative arches, NOT full steering/suspension envelopes.
    for (y=[21.125,111.125]) {
        translate([-20,y,5.5]) rotate([0,90,0]) cylinder(h=90,r=14);
        envelope([90,28,15],[-20,y-14,-9.5]);
    }
}
module body_shell() {
    difference() { body_volume(); body_volume(1); wheel_openings(); }
}

if (part=="carrier_installed") low_carrier();
else if (part=="carrier_print") translate([-3.25,-40,-20.25]) low_carrier();
else if (part=="adhesive") adhesive_budget();
else if (part=="battery") battery_box();
else if (part=="power") power_budget_low();
else if (part=="camera") camera_control_budget();
else if (part=="driver") driver_budget_low();
else if (part=="charge") charge_budget_low();
else if (part=="body") body_shell();
else if (part=="cavity") body_volume(1);
else if (part=="assembly") {
    color([0.55,0.62,0.67]) import("reference/zcar-aligned.stl");
    color("orange") battery_box();
    color("seagreen") support();
    color("purple") guards();
    color([0.2,0.2,0.2]) band_corridor();
    color([0.15,0.65,0.62]) low_carrier();
    color([0.95,0.75,0.15]) power_budget_low();
    color([0.75,0.2,0.7]) camera_control_budget();
    color([0.9,0.3,0.25]) driver_budget_low();
    color([0.45,0.35,0.85]) charge_budget_low();
    color([0.25,0.45,0.7,0.18]) body_shell();
} else assert(false,"Unknown part");
