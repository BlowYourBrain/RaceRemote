// Diagnostic reference only: no chassis modification or printable servo.
// See README.md and ../reference/NOTICE.md for source and evidence limits.
show_upstream=true;
show_candidate=true;
color([.65,.68,.7,.25]) import("frame.stl");
if(show_upstream) color([.2,.45,.8,.65]) import("upstream-servo.stl");
if(show_candidate) color([1,.65,.1,.15]) import("../components/servo-case-installed.stl");
color([.9,.1,.15]) import("interference.stl");
