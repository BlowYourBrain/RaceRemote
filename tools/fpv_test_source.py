"""Local synthetic MJPEG source; not a camera or Wi-Fi performance benchmark.

uv run --with pillow==12.1.1 python tools/fpv_test_source.py --fps 60 --seconds 20
Use adb reverse tcp:8000 tcp:8000 and http://127.0.0.1:8000/stream on Android.
"""

import argparse
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from io import BytesIO
import time

from PIL import Image, ImageDraw


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--fps", type=int, choices=[30, 60], default=60)
    parser.add_argument("--seconds", type=int, default=20)
    parser.add_argument("--port", type=int, default=8000)
    args = parser.parse_args()

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            if self.path != "/stream":
                self.send_error(404)
                return
            self.send_response(200)
            self.send_header("Content-Type", "multipart/x-mixed-replace; boundary=frame")
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            started = time.monotonic()
            try:
                for sequence in range(args.fps * args.seconds):
                    delay = started + sequence / args.fps - time.monotonic()
                    if delay > 0:
                        time.sleep(delay)
                    image = Image.new("RGB", (640, 480), (20, 25, 40))
                    draw = ImageDraw.Draw(image)
                    draw.text((30, 30), f"SYNTHETIC VIDEO / {args.fps} FPS target", fill="white", font_size=28)
                    draw.text((30, 90), f"FRAME {sequence:05d}", fill="white", font_size=42)
                    x = (sequence * 9) % 560
                    draw.rectangle((x, 220, x + 80, 380), fill="orange")
                    buffer = BytesIO()
                    image.save(buffer, "JPEG", quality=75)
                    jpeg = buffer.getvalue()
                    self.wfile.write(f"\r\n--frame\r\nContent-Type: image/jpeg\r\nContent-Length: {len(jpeg)}\r\n\r\n".encode() + jpeg)
                    self.wfile.flush()
                # Stall while the socket remains open, exercising the freshness deadline.
                print(f"Stalling after {sequence + 1} frames / {time.monotonic() - started:.3f}s", flush=True)
                time.sleep(5)
            except (BrokenPipeError, ConnectionResetError, ConnectionAbortedError):
                pass
            self.close_connection = True

    server = ThreadingHTTPServer(("127.0.0.1", args.port), Handler)
    print(f"Synthetic MJPEG on 127.0.0.1:{args.port}; Ctrl-C to stop", flush=True)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == "__main__":
    main()
