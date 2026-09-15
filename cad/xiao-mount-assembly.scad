// Nominal 0/+3 mm packaging with candidate XIAO cradle. Prior scene remains unchanged.
use <xiao-mount.scad>
color([.62,.66,.7]) import("packaging-v05/mechanics-without-frame.stl");
color([.52,.63,.7]) import("packaging-v05/frame-relief-proposal.stl");
color([.67,.69,.72]) import("components/motor-installed.stl");
color([.15,.37,.8]) import("packaging-v05/servo-installed.stl");
color([1,.5,.1]) import("packaging-v05/inputs/adjustable-battery.stl");
color([.4,.65,.6]) {
    import("packaging-v05/inputs/usb-base.stl");
    import("packaging-v05/inputs/adjustable-support.stl");
    import("packaging-v05/inputs/adjustable-guards.stl");
    import("packaging-v05/antenna_support.stl");
    tray_clearance();
}
color([.15,.6,.4]) translate([0,3,0]) import("components/xiao-sense-2023-installed.stl");
color([.25,.5,.8]) cradle();
color("orange") cap();
color("silver") {clamp_screws();stage_screws();import("packaging-v05/inputs/adjustable-main_fasteners.stl");}
color([.1,.3,.2]) import("packaging-v05/buck_pcb.stl");
color([.55,.57,.6]) import("packaging-v05/buck_components.stl");
color([.4,.7,.2]) import("packaging-v05/buck_terminals.stl");
color([.4,.25,.7,.4]) {import("packaging-v05/driver.stl");import("packaging-v05/charge.stl");}
color([.1,.1,.12]) {import("packaging-v05/antenna.stl");import("xiao-mount/wire-antenna.stl");}
color([.1,.3,.8]) import("xiao-mount/wire-servo.stl");
color([.5,.25,.75]) import("xiao-mount/wire-control.stl");
color([.9,.2,.1]) import("packaging-v05/wire-power.stl");
color([.8,.45,.1]) import("packaging-v05/wire-motor.stl");
color([.2,.4,.65]) import("packaging-v05/inputs/usb-cover.stl");
color([.5,.6,.5]) {import("packaging-v05/inputs/usb-pcb.stl");import("packaging-v05/inputs/usb-connector.stl");}
color([.1,.4,.7,.1]) import("packaging-v05/inputs/usb-body.stl");
