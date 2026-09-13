# Project continuity

Delivery city: Cheboksary, explicitly confirmed by the user. Do not ask for the city again; shipping method and a specific pickup point/address remain unselected.

Preferred purchasing sources: Ozon, Wildberries, AliExpress, and Chip and Dip (chipdip.ru). Search these first. Other shops in older price tables are research references or fallback candidates, not accepted sellers. Verify the exact variant, seller, price conditions and shipping when available. AliExpress challenged the automation browser even when the user tried solving its CAPTCHA; avoid repeated challenges and do not claim the technical cause is known. Mark inaccessible offer details unverified and continue other research.

Before working on this project, read `docs/project-brief.md` and `docs/contracts.md`. For chassis work, also read `docs/chassis-candidates.md`.

AliExpress fallback verified on Windows: the user's ordinary Chrome product tab can expose its loaded text through UIAutomationClient AutomationElement (window handle) -> Document -> TextPattern.DocumentRange.GetText. This worked where curl and the separate MCP browser received CAPTCHA. Locate the current product window and document each time; scope reads to the product document and the actual address-bar control, since arbitrary Edit controls can contain unrelated drafts. The MCP browser is separate and list_pages can recreate it after the user closes it. See docs/aliexpress-shortlist.md for the observed SKU and conditional OV2640/OV3660 shipment.

The user explicitly requires recording key points and contracts as work progresses:

- Record accepted requirements, decisions, constraints, significant findings and unresolved questions in the project documents during the same task.
- After changing repository files, verify the changes and create a Git commit before finishing the task. This is an explicit user requirement and includes documentation changes. Commit only files belonging to the task; do not include unrelated user changes. Creating the commit does not require asking again. Pushing is a separate action.
- Keep user decisions separate from agent proposals and unverified assumptions. Do not silently promote a candidate component or suggested value to an agreed contract.
- For each interface contract, record its status, participants, units/ranges, failure behavior, version and verification evidence when known. Leave unknown values explicitly open.
- When a decision changes, update the canonical description and briefly record what changed and why. Keep source links and tested revisions for externally derived facts.
- Do not claim hardware compatibility, reliability or performance based only on a code review or successful software build.

The fleet must use one repeatable target configuration, including the first target prototype. The user now explicitly requires designing around the three existing LW LiPo 601844HP batteries (7.4 V, 600 mAh, 20C), with quick battery replacement. This supersedes the earlier generic preference against reusing inventory for batteries; other existing hardware remains experimental inventory. Camera is optional and bodies are customizable.

The model-car body must constrain component packaging from the start and give the car a finished appearance. A generic CAD shell is a proposed envelope, not evidence that a purchasable Mini-Z body fits. Read cad/body-study.md for the current alternative and its unverified electronics assumptions. In the aligned zcar scene front is +Y (steered axle Y=111.125 mm), rear driven axle Y=21.125 mm.

The user proposed longitudinally adjustable electronics to accommodate different cabin positions. Continue with cad/adjustable-layout.md (0.3): separate adjustable power-electronics tray and camera mount. The nominal 11 mm range, screw details and hypothetical shell are agent proposals with discrete CAD checks, not validated physical compatibility or an unlimited Mini-Z body interface.

Confirmed scope: indoor home racing on a flat surface, Android and iPhone. On loss of control commands, stop the car. Camera-only failure must preserve otherwise working control; do not add a video-dependent driving interlock. KMP is the proposed application direction, not an already completed migration.

The cars are intended as finished gifts for friends. Each must charge through its own USB-C port from an ordinary available charger; do not substitute a shared hobby charger for this product requirement. The user already has a charger (model unknown). A baseline 5 V USB charging design without mandatory PD/QC is the proposed implementation; the built-in 2S charger and USB/power integration remain unfinished.

The user permits a separate charge-only USB-C and additional electronics when needed for a well-finished product; a shared charge/data port is no longer required. Prioritize easy cable insertion/removal and the ability to hide or disguise the port. Rear placement and exhaust styling are examples, not mandatory geometry. The current agent proposal is a chassis-mounted charging port with a removable cosmetic cover and the native XIAO service USB under the body. See docs/usb-c-power.md; neither the mount nor the charger is implemented.

Application identity: use `vea.raceremote` for applicationId, namespace, Kotlin packages, tests and future platform identifiers. First target is Android; iOS can be stubbed/deferred. Samsung SM-G973F / Android 12 and ESP32 on COM4 are available. Soldering iron, multimeter and 3D printer are confirmed. The user confirmed the WROOM-32U external antenna is NOT connected and explicitly requested a nearby-phone-hotspot experiment without it. That experiment is authorized; do not demand an antenna before executing it. It connected but still timed out; repeat reliability measurements with a suitable antenna. Read docs/control-bench.md and docs/control-v1.md for implementation and evidence.
