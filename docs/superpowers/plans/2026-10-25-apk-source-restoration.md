# APK source restoration implementation plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** Restore a buildable Android controller matching the surviving September APK and UDP firmware.

**Architecture:** Translate recovered packet/session/client logic into Kotlin beside the existing WebSocket client. Probe `/status` before choosing transport, preserving explicit connection, identity checks, STOP, lifecycle cleanup and independent video.

**Tech Stack:** Existing Kotlin 1.8.10, Android API 31+, Compose, OkHttp, JVM JUnit; task-local JDK 17.

**Spec:** `recovery/apk-2026-09-26/protocol.md` and its readable decompiled classes.

## Global constraints

- UDP control port 3334; packet size 32; HELLO deadline 4000 ms; ACK freshness 200 ms.
- No automatic transport fallback, actuator activation, replacement of original APK backup, or fabricated original source.
- Keep existing dependencies and WebSocket tests; identify reconstructed code and unverified physical behavior.

## Review focus

- Delayed/replayed ACKs must never renew control freshness (task 1).
- Wrong chip/attempt/token/axes must be ignored (task 1).
- HTTP redirects, oversized/malformed status or discovery mismatch must not ARM (task 2).
- Cancellation during probe and disconnect must close transport without reconnecting (task 2).
- Discovery v1/v2 duplicates must prefer the newer supported response; target changes must retain video isolation (task 3).

### Task 1: UDP codec, session and socket client

Files: create `app/src/main/java/vea/raceremote/control/UdpControlPacket.kt`, `UdpControlSession.kt`, `UdpControlClient.kt`; test `app/src/test/java/vea/raceremote/control/UdpControlTest.kt`.

Interfaces: `UdpControlPacket(kind, attempt, token, sequence, throttle, steering, chipId, basis=0)` encode/decode; session `next(now)`, `receive(packet, now)`, `input`, `stop`, `snapshot`; socket client `input`, `snapshot`, `close` and bind callback.

- [ ] Write tests for wire bytes, malformed boundaries, handshake, replay/mismatch, original-send-time freshness and STOP.
- [ ] Run tests and observe missing implementation failure.
- [ ] Translate recovered classes, preserving deadlines, pending ring and socket cleanup.
- [ ] Run focused tests and real XIAO null-backend socket smoke; commit.

### Task 2: Status probe and transport connection

Files: create `ControlEndpointProbe.kt`, `CarControlConnection.kt`; test `ControlEndpointProbeTest.kt`, `CarControlConnectionTest.kt` in JVM control tests.

Interfaces: `ControlEndpoint(transport, port, chipId)`; `ControlEndpointProbe.load(host,http,port=80)`; connection exposes existing `CarConnectionState` flow and `connect`, `setInput`, `disconnect`, `close`.

- [ ] Write failing fixture tests for status validation, expected identity/transport/port and canceled probe.
- [ ] Implement recovered probe and connection adapter using existing WS client and task 1 UDP client.
- [ ] Verify focused tests, including rejection before ARM and no late connection after STOP; commit.

### Task 3: Discovery, screen and build/device verification

Files: modify `DiscoveryPacket.kt`, `CarDiscovery.kt`, `CarControlScreen.kt`, affected JVM/discovery and Android test HTTP fixtures; document evidence in `docs/apk-source-restoration.md`.

Interfaces: append transport/version defaults to `DiscoveredCar`; discovery query supports versions 1/2; screen calls task 2 adapter with selected identity/transport/port and WiFi socket binding.

- [ ] Add failing discovery v2 parsing/preference tests; update affected fixtures to advertise status.
- [ ] Restore discovery and screen integration, retaining input reset, lifecycle STOP and video target key.
- [ ] Run JVM suite, build app/test APKs, inspect signatures before any replacement installation.
- [ ] Verify actual XIAO control and watchdog; run Samsung tests if connected, explicitly record any remaining limitation.
- [ ] Review whole change, fix material findings, commit and push branch `update`.
