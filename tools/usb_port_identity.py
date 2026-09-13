"""Stable schematic identities; no electrical net definitions shared with PCB."""
import uuid


def uid(name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'https://vea.raceremote/usb-port/' + name))


ROOT_UUID = uid('sheet')
CONNECTOR = 'USB_C_Receptacle_GCT_USB4105-xx-A_16P_TopMnt_Horizontal'


def footprint(ref):
    name = (CONNECTOR if ref == 'J1' else 'R_0603_1608Metric' if ref.startswith('R')
            else 'WirePad_D0.8mm' if ref.startswith('H') else 'MountingHole_2.4mm')
    return 'RaceRemote_USB:' + name


def symbol_path(ref):
    return '/' + ROOT_UUID + '/' + uid(ref)
