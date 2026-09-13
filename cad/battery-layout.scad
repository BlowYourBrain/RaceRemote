// RaceRemote provisional packaging study, millimetres.
// Generate the two upstream-derived meshes with tools/layout_zcar_battery.py.
// Their GPL-3.0 license and source are saved next to those generated meshes.
// "battery_dummy" exports a printable size gauge, not a battery holder.
part = "layout"; // [layout,battery_dummy]
battery_size = [49, 18, 15]; // Seller estimate; replace after measuring all packs.
battery_center = [24.75, 55, 10.25];
extraction_offset = 0; // Preview lateral removal: 0 through -50 mm.

assert(min(battery_size) > 0, "Battery dimensions must be positive");
assert(part == "layout" || part == "battery_dummy", "Unknown part");

if (part == "battery_dummy") {
    // Origin at the lower corner for printing. Wires/connectors not represented.
    cube(battery_size);
} else {
    color([0.15, 0.4, 0.7, 0.6])
        import("../build/cad-layout/frame.stl");
    color([0.5, 0.5, 0.5, 0.3])
        import("../build/cad-layout/cover.stl");
    color([1, 0.5, 0.1])
        translate(battery_center + [extraction_offset, 0, 0])
            cube(battery_size, center=true);
}
