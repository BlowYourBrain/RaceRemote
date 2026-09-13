// RaceRemote packaging study 0.1, millimetres. F5 preview of the assembly.
// NOT a finished car: electronics boxes are design budgets, not supplied parts.
// Chassis reference: alexyu132/zcar 4b714e6, GPL-3.0 (reference/NOTICE.md).
use <battery-holder.scad>
part = "assembly";
show_reference = true;
show_battery = true;
show_electronics = true;
show_carrier = true;
exploded = 0; // Vertical display offset only; not an installation path.
$fn = 32;

module envelope(size, corner) { translate(corner) cube(size); }
module battery_box() { envelope([49,18,15],[0.25,46,2.75]); }
// Proposed budgets. No exact board pinout, connector or fastening implied.
module control_budget() { envelope([22,18,10],[13.75,43,27]); }
module power_budget() { envelope([33,16,12],[8.25,67,27]); }
// Front is +Y (steered axle Y=111.125); rear driven axle is Y=21.125.
module camera_budget() { envelope([23,12,21],[13.25,87,32]); }
module driver_budget() { envelope([30,18,10],[9.75,43,40]); }
module charge_budget() { envelope([33,16,8],[8.25,67,42]); }

module carrier() {
    difference() {
        union() {
            envelope([43,46,2],[3.25,40,24]);
            // Feet sit on reserved 0.5 mm adhesive pads over Z=19.75 roof.
            for (x=[4.25,39.25], y=[45,70])
                envelope([6,6,5.75],[x,y,20.25]);
            // Candidate upright, behind the forward-facing camera budget.
            envelope([27,2,29],[11.25,84,26]);
        }
        // Slots allow future straps. Strap hardware and load paths unverified.
        for (x=[6.75,40.25], y=[45,69])
            envelope([2.5,10,3],[x,y,23.5]);
        envelope([17,3,15],[16.25,83.5,32]);
        for (x=[12.75,34.25]) envelope([2,3,15],[x,83.5,32]);
    }
}
module adhesive_budget() {
    for (x=[4.25,39.25], y=[45,70]) envelope([6,6,0.5],[x,y,19.75]);
}

if (part=="carrier_installed") carrier();
else if (part=="carrier_print") translate([-3.25,-40,-20.25]) carrier();
else if (part=="adhesive") adhesive_budget();
else if (part=="battery") battery_box();
else if (part=="control") control_budget();
else if (part=="power") power_budget();
else if (part=="camera") camera_budget();
else if (part=="driver") driver_budget();
else if (part=="charge") charge_budget();
else if (part=="assembly") {
    if (show_reference) color([0.55,0.62,0.67,0.75]) import("reference/zcar-aligned.stl");
    if (show_battery) {
        color("orange") battery_box();
        color("seagreen") support();
        color("purple") guards();
        color([0.2,0.2,0.2]) band_corridor();
    }
    if (show_carrier) translate([0,0,exploded]) {
        color([0.15,0.65,0.62]) carrier();
        color([0.7,0.7,0.7]) adhesive_budget();
    }
    if (show_electronics) translate([0,0,2*exploded]) {
        color([0.2,0.45,0.95,0.8]) control_budget();
        color([0.95,0.75,0.15,0.8]) power_budget();
        color([0.75,0.2,0.7,0.8]) camera_budget();
        color([0.9,0.3,0.25,0.8]) driver_budget();
        color([0.45,0.35,0.85,0.8]) charge_budget();
    }
} else assert(false,"Unknown part");
