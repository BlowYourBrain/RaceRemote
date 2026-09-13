# Project continuity

Preferred purchasing sources: Ozon, Wildberries, AliExpress, and Chip and Dip (chipdip.ru). Search these first. Other shops in older price tables are research references or fallback candidates, not accepted sellers. Verify the exact variant, seller, price conditions and shipping when available. AliExpress challenged the automation browser even when the user tried solving its CAPTCHA; avoid repeated challenges and do not claim the technical cause is known. Mark inaccessible offer details unverified and continue other research.

Before working on this project, read `docs/project-brief.md` and `docs/contracts.md`. For chassis work, also read `docs/chassis-candidates.md`.

The user explicitly requires recording key points and contracts as work progresses:

- Record accepted requirements, decisions, constraints, significant findings and unresolved questions in the project documents during the same task.
- After changing repository files, verify the changes and create a Git commit before finishing the task. This is an explicit user requirement and includes documentation changes. Commit only files belonging to the task; do not include unrelated user changes. Creating the commit does not require asking again. Pushing is a separate action.
- Keep user decisions separate from agent proposals and unverified assumptions. Do not silently promote a candidate component or suggested value to an agreed contract.
- For each interface contract, record its status, participants, units/ranges, failure behavior, version and verification evidence when known. Leave unknown values explicitly open.
- When a decision changes, update the canonical description and briefly record what changed and why. Keep source links and tested revisions for externally derived facts.
- Do not claim hardware compatibility, reliability or performance based only on a code review or successful software build.

The fleet must use one repeatable target configuration, including the first target prototype. The user now explicitly requires designing around the three existing LW LiPo 601844HP batteries (7.4 V, 600 mAh, 20C), with quick battery replacement. This supersedes the earlier generic preference against reusing inventory for batteries; other existing hardware remains experimental inventory. Camera is optional and bodies are customizable.

Confirmed scope: indoor home racing on a flat surface, Android and iPhone. On loss of control commands, stop the car. Camera-only failure must preserve otherwise working control; do not add a video-dependent driving interlock. KMP is the proposed application direction, not an already completed migration.

Application identity: use `vea.raceremote` for applicationId, namespace, Kotlin packages, tests and future platform identifiers. First target is Android; iOS can be stubbed/deferred. Samsung SM-G973F / Android 12 and ESP32 on COM4 are available. Soldering iron, multimeter and 3D printer are confirmed. The user confirmed the WROOM-32U external antenna is NOT connected and explicitly requested a nearby-phone-hotspot experiment without it. That experiment is authorized; do not demand an antenna before executing it. It connected but still timed out; repeat reliability measurements with a suitable antenna. Read docs/control-bench.md and docs/control-v1.md for implementation and evidence.
