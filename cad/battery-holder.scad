// Provisional holder 0.2, mm. Geometry only: no retention/print validation.
// Original RaceRemote geometry; imported frame/cover retain upstream GPL-3.0.
part = "layout";
frame_width = 49.5;
floor_z = 1;
roof_z = 19.75;
adhesive = 0.20; // Reserved compressed layer, product not selected.
pad_top = 2.25;
liner = 0.50; // Reserved compressed soft liner, product not selected.
battery = [49,18,15]; // Seller dimensions, not measured user packs.
center_y = 55;
guide_clearance = 0.50;
pad_x = 2.25;
guard_clearance = 0.15;
guard_wall = 1.20;
guard_engagement = 3.50;
guard_width = 8;
band_width = 3;
band_thickness = 1; // Space reservation, not an elastic material model.
groove_depth = 0.40;
groove_width = 3.50;
show_retention = true;
extraction_offset = 0;
$fn = 32;

assert(pad_top > floor_z + adhesive);
assert(pad_top + liner + battery[2] <= 18.25, "Battery crosses roof underside");
assert(battery[0] <= frame_width, "Recheck frame and guards for wider packs");
assert(band_width < guard_width);
assert(band_width < groove_width && groove_width < guard_width);
assert(groove_depth < guard_wall);

module rounded_plate(w,d,h,r=0.6) {
    hull() for(x=[r,w-r],y=[r,d-r]) translate([x,y,0]) cylinder(r=r,h=h);
}
module support() {
    y0 = center_y-battery[1]/2-guide_clearance-1;
    depth = battery[1]+2*guide_clearance+2;
    translate([pad_x,y0,floor_z+adhesive]) {
        rounded_plate(frame_width-2*pad_x,depth,pad_top-floor_z-adhesive);
        // Longitudinal guides; the two lateral ends remain open for sliding.
        for(y=[0,depth-1]) translate([0,y,0])
            rounded_plate(frame_width-2*pad_x,1,4.25-floor_z-adhesive,0.4);
    }
}
module left_guard() {
    lo = -guard_clearance;
    hi = roof_z+guard_clearance;
    difference() {
      translate([0,center_y-guard_width/2,0]) union() {
        translate([lo-guard_wall,0,lo-guard_wall])
            cube([guard_wall,guard_width,hi-lo+2*guard_wall]);
        for(z=[lo-guard_wall,hi]) translate([lo-guard_wall,0,z])
            cube([guard_wall+guard_clearance+guard_engagement,guard_width,guard_wall]);
      }
      // Recess keeps the band centered on the removable guard.
      translate([lo-guard_wall-0.01,center_y-groove_width/2,lo-guard_wall-0.01])
          cube([groove_depth+0.01,groove_width,hi-lo+2*guard_wall+0.02]);
      translate([lo-guard_wall-0.01,center_y-groove_width/2,hi+guard_wall-groove_depth])
          cube([guard_wall+guard_clearance+guard_engagement+0.02,groove_width,groove_depth+0.01]);
      translate([lo-guard_wall-0.01,center_y-groove_width/2,lo-guard_wall-0.01])
          cube([guard_wall+guard_clearance+guard_engagement+0.02,groove_width,groove_depth+0.01]);
    }
}
module guards() {
    left_guard();
    translate([frame_width,0,0]) mirror([1,0,0]) left_guard();
}
module band_corridor() {
    // Rectangular swept reservation outside both guards. A real elastic band
    // will curve/drape; this is NOT its rest length, preload, or contact shape.
    lo=-guard_clearance-guard_wall+groove_depth;
    hi=roof_z+guard_clearance+guard_wall-groove_depth;
    difference() {
        translate([lo-band_thickness,center_y-band_width/2,lo-band_thickness])
            cube([frame_width-2*lo+2*band_thickness,band_width,hi-lo+2*band_thickness]);
        translate([lo,center_y-band_width/2-1,lo])
            cube([frame_width-2*lo,band_width+2,hi-lo]);
    }
}

if(part=="support_installed") support();
else if(part=="guards_installed") guards();
else if(part=="band_corridor") band_corridor();
else if(part=="support_print")
    translate([-pad_x,-(center_y-battery[1]/2-guide_clearance-1),-floor_z-adhesive]) support();
else if(part=="guard_print")
    // Lay the broad side on the bed: two identical parts, mirror via rotation.
    translate([guard_clearance+guard_wall,roof_z+guard_clearance+guard_wall,center_y+guard_width/2])
        rotate([90,0,0]) mirror([0,1,0]) left_guard();
else if(part=="layout") {
    color([0.15,0.4,0.7,0.35]) import("../build/cad-layout/frame.stl");
    color([0.5,0.5,0.5,0.3]) import("../build/cad-layout/cover.stl");
    color("seagreen") support();
    color([1,0.5,0.1]) translate([(frame_width-battery[0])/2+extraction_offset,center_y-battery[1]/2,pad_top+liner]) cube(battery);
    if(show_retention) {
        color("purple") guards();
        color([0.2,0.2,0.2,0.6]) band_corridor();
    }
} else assert(false,"Unknown part");
