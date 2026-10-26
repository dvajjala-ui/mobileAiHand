# mobileAiHand architecture review — 26 October 2026

Last updated: 2026-10-26 (Asia/Kolkata)

## Review scope and method

Six independent, fresh-context reviewers used the Elephant-Goldfish Model personas for architecture, comprehension, design critique, document structure, codebase detail, and implementation readiness. They read `PRODUCT_PLAN.md` as the target and selected context from `README.md`, `REPOSITORY_RESEARCH.md`, `tools/laptop-harness/README.md`, Android source, manifest, build files, and tests. They did not read the earlier drift report or the conversation. This is a persona-based review, not a claim that the plugin's complete five-stage workflow passed: the product plan has no formal Detailed Implementation Plan, so its formal implementation-readiness stage is unavailable.

The APK was previously built, and its six small unit tests passed. There is no attached S24 Ultra or model file in this workspace. No Reddit, WhatsApp, ChatGPT, or Settings task has been validated end to end on a phone. All project files are currently untracked in Git.

## Verdict

**Direction: appropriate for a prototype. Readiness: insufficient for unattended posting or sending.** A native Android app with an AccessibilityService, MediaStore, Android share intents, and the system uninstall flow is a sensible phone-only starting point. The code has a real UI, model import, typed action parser, and buildable APK. Its controller still trusts a model to choose cross-app actions and to declare completion; the user-facing task review is not bound to the actual send/post target. The app should be treated as an experiment until the S24 Ultra runs the model and a narrow action succeeds under explicit checks.

## Findings by priority

1. **Release blocker — action scope and approval.** `MainActivity.java:213-215` reviews the requested goal once. `PhoneControlService.java:69-88,136-166` can subsequently click or type in any accessible app, including Send or Publish, without comparing the final contact, community, text, or attachments with the approved request. Text from the screen is inserted into the model prompt (`PhoneControlService.java:67-78`), so an untrusted page or chat can influence the next proposed action. Decide whether the initial approval authorizes final sending or whether a second approval is required; either policy needs controller-enforced task and app scope, final target/payload checks, and a safe stop on mismatch. The current plan promises a preview (`PRODUCT_PLAN.md:18,25-26`) but does not define this contract.
2. **Release blocker — no reliable outcome contract.** A model `done` message becomes the report (`PhoneControlService.java:83-86`), with no independent postcondition. Uninstall reports only that Android opened confirmation (`MainActivity.java:230-235`), so cancel and successful removal are indistinguishable. Define `succeeded`, `failed`, and `uncertain` with task-specific evidence. Ambiguous send/post results must not be retried automatically because that can duplicate the action. This is required by `PRODUCT_PLAN.md:19,25-28`.
3. **Functional defect — swipe capability missing.** The controller offers and calls `dispatchGesture` for swipes (`PhoneControlService.java:73-77,175-183`), but `accessibility_service_config.xml` does not declare `android:canPerformGestures="true"`. Android requires that capability. Scrolling flows can fail even if the model chooses a valid swipe.
4. **Major — model and UI-loop reliability unproven.** The recommended model is a separate roughly 3.1 GB download with an estimated 5.9 GB peak memory need (`README.md:10,18`). It is initialized for each task (`PhoneControlService.java:50-58`). The loop uses fixed sleeps, a 16-action limit, and snapshots silently truncated at 100 nodes or 6,500 characters (`PhoneControlService.java:65-95,120-134`). A malformed model response stops the task (`PhoneControlService.java:79-82`). Define measurable S24 Ultra load time, per-action latency, memory, and task completion thresholds; test loading screens, stale nodes, malformed JSON, interruption, and unavailable controls.
5. **Major — privacy and progress are underspecified.** Accessibility text can include private messages or account details (`PhoneControlService.java:126-130`). Model-written reports and raw invalid responses can be saved to preferences (`PhoneControlService.java:53-57,81-85`). Only the last report persists; there is no task log, deletion UI, or retention rule despite `PRODUCT_PLAN.md:19`. Add a minimal redacted event log and explicit sensitive-field handling before routine use.
6. **Major — guided dry runs and lifecycle are incomplete.** The Reddit and WhatsApp goals explicitly instruct post/send (`MainActivity.java:181-195`), while the first device milestone calls for a draft and photo selection without transmission (`PRODUCT_PLAN.md:30-32`). Model import runs in an Activity-owned executor using fixed filenames; a rotation can create another Activity and import while the old copy continues (`MainActivity.java:45-46,99-101,141-159`). Provide draft/select-only modes and make model import state survive Activity recreation.
7. **Major — tests and project handoff are not ready.** The six Android unit tests cover only action parsing and app-name resolution. There are no real-device or instrumentation checks for permissions, gestures, final recipient, post/send confirmation, cancellation, model import, or outcomes. `PRODUCT_PLAN.md` lacks explicit non-goals, alternatives, risk mitigations, success criteria, and a phased file-level implementation plan. The APK and docs exist locally, but none of the project files are tracked in Git yet.

## Layer-by-layer answer

| Layer | Current assessment | Next decision or work |
| --- | --- | --- |
| User interface | A basic native prompt form and one review dialog exist. Setup, progress, cancellation, draft-only use, final target inspection, and uncertain outcomes need clearer screens. | Decide whether final send/post needs a second tap or whether initial approval plus exact automated checks is enough. Show the exact photo list and recipient, not only a count. |
| Android app | The native APIs chosen for photos and uninstall are appropriate. The Accessibility controller is broad and several responsibilities live together in `MainActivity` and `PhoneControlService`. | Make task states, allowed apps/actions, final checks, retry rules, and outcomes explicit. Fix gesture metadata. Keep a narrow first flow before adding abstractions for every app. |
| Local AI | A MediaPipe-compatible Gemma 3n file is named, but local inference and UI-action quality are unmeasured on the S24 Ultra. | Decide whether offline inference is a hard requirement. Pin one known model and measure import, memory, latency, and task success on the actual phone. |
| Backend and web frontend | None is necessary for the agreed phone-only first release. Reddit, WhatsApp, and ChatGPT use their own apps/accounts/network. | Add a backend only if later features need remote laptop submission, account sync, hosted inference, or an authorized API integration that cannot be handled safely on-device. |
| Infrastructure and distribution | Cloud builds a debug APK; model installation is manual; no CI or release channel is defined. | First version can use sideloading. Later decide model distribution/update integrity, Android permission policy, signed releases, device test setup, and CI after checking project files into Git. |
| Scale | Presently one phone task at a time, at most 16 actions and 30 photos. Server scale is not the current bottleneck. | Set device capacity and recovery limits: model RAM, action latency, photo batch behavior, app backgrounding, and duplicate-send prevention. |

## Decisions to answer before broadening features

1. Which task is the **first complete** end-to-end goal after a verified Settings read? A ChatGPT question is lower consequence than a Reddit post or WhatsApp photo send; draft-only Reddit and photo-selection flows can be tested before transmission.
2. Is **offline/on-device AI mandatory**, or is a phone-only app allowed to use a remote model when local accuracy or speed is inadequate?
3. For a post or message, does one initial approval authorize the send if target and content match exactly, or should the user approve again on the final screen?
4. What counts as success for each action, and what should the app report when the UI changes or the outcome is ambiguous? Should it ever retry a possible send/post?
5. For “today's photos,” do you mean camera photos only or every image with today's capture timestamp, including screenshots and downloads? What should happen above 30 images?
6. Is the first Reddit integration the app UI or an authorized API? Does “GPT” mean the ChatGPT app or a model API? These choices determine account, auth, cost, and verification needs.

## Suggested next validation sequence

1. Correct the gesture capability, add a task-scoped action policy, and define explicit outcome states. Keep post/send disabled while testing the controller.
2. On the S24 Ultra, import the exact `.task` file and run a Settings version-read trial several times. Record model load time, per-action delay, memory behavior, correct answer rate, and failure reports.
3. Add and test a Reddit draft-only path and a WhatsApp photo-selection-only path. Exercise zero, one, 30, and more than 30 photos, partial permission, and ambiguous contacts.
4. Choose one externally visible action, specify its approval and evidence contract, and test success, cancel, app interruption, ambiguous outcome, and duplicate prevention on the phone.
