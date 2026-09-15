"""Visualize the checked body path without changing the historical harness viewer."""
import base64
import hashlib
import json
from pathlib import Path
import numpy as np
import trimesh

ROOT = Path(__file__).resolve().parents[1]


def main():
    source = ROOT / "cad/control-harness/viewer.html"
    evidence = ROOT / "docs/evidence/current-service-v01.json"
    report = json.loads(evidence.read_text())
    assert not any(p["hits_mm3"] for p in report["body_samples"])
    page = source.read_text(encoding="utf-8")
    start = page.index("const parts=")
    end = page.index(";\nconst adjustmentChecks=", start)
    parts = json.loads(page[start + len("const parts="):end])
    for part in parts:
        if part["id"] in ["body", "usb_cover"]:
            part["hidden"] = False
            part["level"] = 0
            part["slide"] = None
            # Rebuild moving wireframe directly from checked mesh feature edges.
            name = "usb-body" if part["id"] == "body" else "usb-cover"
            model = trimesh.load_mesh(ROOT / f"cad/packaging-v05/inputs/{name}.stl")
            edges = model.face_adjacency_edges[model.face_adjacency_angles > .05]
            points = model.vertices[edges].reshape(-1, 3)
            assert len(points) > 0 and np.allclose([points.min(axis=0), points.max(axis=0)], model.bounds), name
            raw = np.hstack([points, np.tile([0, 0, 1], (len(points), 1))]).astype('<f4')
            part.update(data=base64.b64encode(raw.tobytes()).decode(), count=len(raw), wire=True)
    page = page[:start] + "const parts=" + json.dumps(parts, ensure_ascii=False) + page[end:]
    page = page.replace("Проводка управления", "Снятие кузова")
    start = page.index('<p class="note">')
    end = page.index('</p>', start) + 4
    page = page[:start] + '''<p class="note">Маршрут условного кузова с новой платой и проводами: вверх 2 мм → вправо 1 мм → вверх до 65 мм. Проверены 265 положений через 0,25 мм. Питание выключено, внешние USB-кабели отключены. Реальный кузов, пальцы, магниты и усилия ещё не проверены.</p>
<label for="service-step">Снятие кузова: <output id="service-value"></output></label>
<input id="service-step" type="range" min="0" max="264" step="1" value="0">
<button id="service-start">Установлен</button><button id="service-end">Поднят</button>
<p id="service-position" role="status"></p>''' + page[end:]
    poses = [p["offset_mm"] for p in report["body_samples"]]
    page = page.replace("const parts=", "const servicePoses=" + json.dumps(poses) + ";\nlet serviceIndex=0;\nconst parts=", 1)
    substitutions = {
        "uniform float dz;uniform float dy;": "uniform float dz;uniform float dy;uniform vec3 serviceShift;",
        "p+vec3(0.,dy,dz)": "p+vec3(0.,dy,dz)+serviceShift",
        "const dy=gl.getUniformLocation": "const serviceShift=gl.getUniformLocation(program,'serviceShift');\nconst dy=gl.getUniformLocation",
        "gl.uniform3fv(col,part.color);": "gl.uniform3fv(col,part.color);gl.uniform3fv(serviceShift,['body','usb_cover'].includes(part.id)?servicePoses[serviceIndex]:[0,0,0]);",
        "center=[24.75,58,20+explosion]": "center=[24.75,58,20+explosion+servicePoses[serviceIndex][2]*.4]",
    }
    for old, new in substitutions.items():
        assert page.count(old) == 1, old
        page = page.replace(old, new)
    js = '''
function updateService(index){
 serviceIndex=index;document.querySelector('#service-step').value=index;
 document.querySelector('#service-value').value=`${index+1} / ${servicePoses.length}`;
 const [x,y,z]=servicePoses[index];
 document.querySelector('#service-position').textContent=`Смещение кузова: X +${x}, Y ${y}, Z +${z} мм. Проверенное положение.`;
 render();
}
document.querySelector('#service-step').oninput=e=>updateService(Number(e.target.value));
document.querySelector('#service-start').onclick=()=>updateService(0);
document.querySelector('#service-end').onclick=()=>updateService(servicePoses.length-1);
updateService(0);
'''
    page = page.replace("</script></html>", js + "</script></html>")
    target = ROOT / "cad/current-service/viewer.html"
    target.parent.mkdir(exist_ok=True)
    target.write_text(page, encoding="utf-8", newline="\n")
    sources = [source, evidence, Path(__file__)]
    result = {"body_pose_count": len(poses), "display_body_feature_edges_from_checked_meshes": True,
              "source_sha256": {p.relative_to(ROOT).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},
              "artifact_sha256": {target.relative_to(ROOT).as_posix(): hashlib.sha256(target.read_bytes()).hexdigest()}}
    (ROOT / "docs/evidence/current-service-viewer-v01.json").write_text(
        json.dumps(result, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(json.dumps({"body_poses": len(poses), "moving_geometry_matched": True}))


if __name__ == "__main__":
    main()
