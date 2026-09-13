// Concrete candidate references at nominal adjustable-stage position.
// See candidate-fit.md: published STEP is from 2023, not confirmed OV3660.
use <adjustable-layout.scad>
use <body-study.scad>
use <assembly.scad>
use <battery-holder.scad>
color([.57,.64,.69]) import("components/zcar-without-reference-actuators.stl");
color([.75,.75,.8]) import("components/motor-installed.stl");
color([.2,.45,.8]) import("components/servo-case-installed.stl");
color([.2,.65,.45]) import("components/xiao-sense-2023-installed.stl");
color([.95,.75,.15]) import("components/buck-installed.stl");
color("orange") battery_box();
color([.4,.65,.6]) { fixed_base(); moving_tray(); camera_stage(); support(); guards(); band_corridor(); }
color([.4,.4,.45]) { main_fasteners(); camera_fasteners(); }
color([.6,.45,.6]) { adjustable_driver(); adjustable_charge(); }
color([.25,.45,.7,.18]) body_shell();
