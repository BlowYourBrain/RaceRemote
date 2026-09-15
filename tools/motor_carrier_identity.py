"""Stable identifiers shared by schematic and PCB, not shared electrical nets."""
import uuid


def uid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'https://vea.raceremote/motor-carrier/' + name))


def footprint(ref):
    name = 'TA6586_DIP8' if ref == 'U1' else 'CP_D8_P3.5' if ref == 'C2' else 'C_0805' if ref == 'C1' else 'R_0805' if ref.startswith('R') else 'WirePad_1mm'
    return 'RaceRemote_Motor:' + name
