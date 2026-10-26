# Phone-only Android task assistant — design candidate

**Status:** Draft for EGM review, 26 October 2026. Decisions labeled provisional need product confirmation before an externally visible action is enabled.

## Problem

The user wants to type a request on a Samsung S24 Ultra and have the phone carry it out: ask in ChatGPT, prepare or publish a Reddit post, share today's photos through WhatsApp, or uninstall an app. The current [prototype](../../README.md) builds and uses an on-device model plus Android Accessibility, but no task has been verified on a real phone. Its model can act across apps after one initial dialog, and a model-written `done` message is treated as the report. The [review](../reviews/initial/2026-10-26_EGM_REVIEW.md) identifies this as insufficient for reliable sending or publishing.

## Goals and non-goals

The first release runs entirely on one Android phone. It accepts a task on that phone, shows the exact request, executes only task-scoped actions, and reports `succeeded`, `failed`, `uncertain`, or `cancelled` with observable evidence. It supports a Settings version-read trial, ChatGPT-app question, Reddit draft, WhatsApp photo selection, Android-system uninstall, then guarded Reddit publish and WhatsApp send. A compatible local model is the first inference path; whether local inference is fast and accurate enough is a device-test gate, not an established fact.

The first release does not accept prompts from a laptop, support iPhone, run a server, promise offline behavior for third-party apps, batch more than 30 photos, or claim success from a model assertion alone. General cross-app execution is deferred until guided tasks have an enforceable action policy. Reddit API and direct GPT API integrations are deferred.

## Current implementation and dependencies

The Android app has a programmatic prompt form in `MainActivity`, an `AccessibilityService` loop in `PhoneControlService`, a MediaPipe `LlmInference` wrapper, a `MediaStore.DATE_TAKEN` query, Android share intents, and the system `ACTION_DELETE` uninstall flow. The debug APK builds for Android SDK 35 and arm64; six unit tests cover JSON action parsing and installed-app name resolution. The recommended model is the separate `gemma-3n-E2B-it-int4.task` file, about 3.1 GB with a published estimate near 5.9 GB peak memory. The S24 Ultra, model, Reddit, WhatsApp, and ChatGPT accounts are not connected to this workspace. See [repository research](../../REPOSITORY_RESEARCH.md) for reference projects and [product plan](../../PRODUCT_PLAN.md) for original intent.

## User flow and action contract

1. The user chooses a guided task and enters required fields. The app refuses missing or ambiguous target data; it does not guess a recipient, community, or package. The app displays the exact target, content, photo count and thumbnails when relevant, and likely external app before the user taps **Run**.
2. The app records a typed `TaskSpec` with a unique ID, allowed package(s), permitted action category, expected payload, photo URI list if needed, and a maximum of 16 agent actions. A free-form prompt can inspect Settings during the feasibility trial but cannot enter a publish/send flow.
3. The executor checks every proposed action outside the model. It rejects package changes beyond the task's allowed list, password fields, missing or stale nodes, and final Send/Post without an approved `PendingExternalAction`. Screen text is treated as untrusted task data. If an app does not expose enough information to check its final target and payload, the executor stops and asks the user to finish manually in that app.
4. **Provisional approval rule:** the initial **Run** approval authorizes navigation and draft entry. Immediately before a Reddit publish or WhatsApp send, an Accessibility overlay shows the final target and payload summary and requires a second explicit tap. The executor removes the overlay, re-reads the target screen and package, compares them with the approved task, and only then taps the final control. A mismatch cancels the automated send. The system's own confirmation remains the required final step for uninstall.
5. The app observes the resulting screen or Android package state. It reports `succeeded` only with a defined observable postcondition; it reports `uncertain` when an external action may have happened but evidence is missing. It never automatically retries a potentially completed send or post. A user-visible local task history stores status transitions and evidence codes, without raw UI text, prompt bodies, credentials, or photo contents. The user can delete it.

## Task-specific postconditions

| Task | Allowed integration | Succeeded when | Uncertain or blocked when |
| --- | --- | --- | --- |
| Settings version read | Android Settings Accessibility UI | Exact version string is read from a Settings screen and shown with a screen label. | Version text is unavailable or conflicts with a second observation. |
| Ask in ChatGPT | Installed ChatGPT app UI | Exact question was entered and a nonempty response appears after a screen transition. | Signed out, response never appears, or content is inaccessible. An initial task review is required because the question leaves the device through ChatGPT. |
| Reddit draft | Installed Reddit app UI | Correct community, title, and body appear in an editor without submission. | Any field cannot be checked or the app changes account/community. |
| WhatsApp photo selection | Android `MediaStore` plus WhatsApp share UI | Selected contact and photo count are visible; nothing has been sent. | Contact identity or count is ambiguous or unavailable. |
| Uninstall | Android `ACTION_DELETE` system UI | The selected package is absent after system confirmation returns. | User cancels, app remains installed, or result cannot be observed. Cancel is `cancelled` when Android exposes it. |
| Reddit publish / WhatsApp send | Same UI as the draft/selection task, then a final approval | Final target and payload match the task, the user approves, and a new post or sent attachment is visible after the action. | Target/payload cannot be checked, app UI is opaque, result is ambiguous, or interruption occurs after the final tap. No automatic retry. |

For photo selection, "today" means `DATE_TAKEN` falls between midnight and the next midnight in the phone's current time zone. The preview shows each image, including screenshots if they meet that rule, so the user can remove unwanted items. Zero images is a clear empty state; more than 30 blocks sending in this release. This is a provisional definition of “photos” pending user confirmation.

## Architecture and data boundaries

```text
Native task form -> TaskSpec + initial review -> TaskCoordinator
                                              -> ModelStore / LocalModel
                                              -> PhoneControlService snapshot
                                              -> TaskPolicy -> Android UI action
                                              -> OutcomeVerifier -> local TaskLog -> status UI
Android MediaStore -> photo preview -> TaskSpec
Android system uninstall confirmation -> package check -> OutcomeVerifier
```

`TaskSpec` is immutable after initial approval. `TaskCoordinator` owns one active task and state transitions: `created`, `approved`, `running`, `waiting_final_approval`, `succeeded`, `failed`, `uncertain`, `cancelled`. `PhoneControlService` supplies snapshots with package ID, visible labels, node roles, and bounds; it proposes typed actions via the local model and sends them through `TaskPolicy`. The model never chooses a task status. `OutcomeVerifier` uses task-specific checks and evidence codes. `TaskLog` keeps only timestamp, task ID, kind, state, action code, evidence code, and error category in app-private storage; it removes records older than seven days and supports Delete history. No raw screen text, model prompt/response, photo URI, title, message body, or credential is logged. Data sent through Reddit, WhatsApp, or ChatGPT is governed by those apps and their accounts.

The first release has no backend and no web frontend. It serves one phone and one task at a time. The current caps of 16 agent actions and 30 photos are explicit product limits, not server scalability limits. API-based integrations or laptop submission require a separate design for authentication, networking, and sync.

## Alternatives considered

| Alternative | Decision and reason |
| --- | --- |
| Laptop/ADB controller | Deferred because the user requires phone-only operation even with the laptop off. The existing laptop harness remains a development comparison tool. |
| Direct APIs for Reddit and GPT | Deferred for the first guided release because account authorization, token handling, API limits, and ChatGPT-history behavior require separate integration contracts. Android UI control matches the requested app-account workflows but must fail safely when UI state is opaque. |
| Remote model instead of local MediaPipe | Deferred for the first device feasibility trial because the current prototype and privacy goal favor local inference. If the S24 Ultra cannot meet the acceptance thresholds below, compare remote inference as a phone-only alternative before building more app flows. |
| A universal agent for every installed app | Deferred because text-only Accessibility cannot reliably identify every screen, recipient, or action. Guided task policies provide a testable first release. |
| Service API proof for every send | Deferred because the initial integrations use consumer app UI. The app reports `uncertain` whenever screen evidence is insufficient instead of claiming a service receipt. |

## Risks, mitigations, and limits

| Risk | Mitigation or test |
| --- | --- |
| A page or chat contains instructions to the model | Treat UI text as untrusted; check package, task scope, node role, and consequential action outside the model. Test a screen with adversarial text. |
| Wrong recipient or changed post/attachments | Final overlay approval and exact screen comparison; block when details are not accessible. Test wrong and ambiguous targets. |
| Duplicate submission after timeout | Never auto-retry after a final tap; mark `uncertain` and require user inspection. |
| Large model crashes or stalls on S24 Ultra | Measure load, memory behavior, latency, and repeated task runs before enabling third-party sends. |
| Third-party UI changes or hides controls | Wait for screen changes, detect stale nodes, stop with an actionable reason, and test supported app versions on a real phone. |
| Private screen text appears in reports | Keep inference local, omit raw text from persisted log, redact sensitive fields and model errors, and test with a credential-like field. |
| Accessibility or package-visibility policy blocks Play distribution | Ship a sideloaded debug trial first; evaluate store policy and narrower package queries before a public release. |

## Acceptance gates

1. **Device feasibility:** On the S24 Ultra, the exact model imports and five consecutive offline Settings version-read trials return the version visible in Android Settings. Record model load time, per-step latency, memory/process termination, and any incorrect action; a crash or wrong version fails the gate. The app must give a clear error for a corrupt model or low storage.
2. **Read-only third-party trials:** Reddit drafts show exact community/title/body without publishing; WhatsApp photo selection shows the intended contact and count without sending. Test zero, one, 30, and more than 30 matching images; partial photo permission, local midnight, and ambiguous contacts must produce safe outcomes.
3. **First complete action:** Ask a question in the ChatGPT app and display the observed answer or `uncertain`. Test signed-out and no-network states. Then enable one chosen Reddit or WhatsApp external action only after the final approval and outcome rules are implemented and tested on device.
4. **Failure behavior:** Malformed model JSON, stale node, changed package, hidden final recipient, cancel, stop during inference, 16-action cap, and interruption after a possible final tap all end without an unapproved external action or automatic duplicate. Saved history contains no raw screen text or task payload.

## Detailed Implementation Plan

This is a proposed sequence. Each phase must build and keep existing parser/resolver tests passing. The source paths below are relative to the repository root.

### Phase 1 — make the current controller safe and observable

1. Modify `android-app/app/src/main/res/xml/accessibility_service_config.xml` to declare `canPerformGestures=true`, retaining window-content retrieval. Modify `android-app/app/src/main/AndroidManifest.xml` to remove unused `POST_NOTIFICATIONS`; keep `READ_MEDIA_IMAGES` and current package visibility for the sideload trial, documenting why.
2. Add `android-app/app/src/main/java/ai/mobileaihand/TaskSpec.java` with immutable task kind, UUID, target package/identity, exact user payload, photo URI list, allowed packages, and action cap. Add `TaskState.java` with the states named above and legal transitions. Free-form inspection is limited to Settings and cannot perform final send/post/uninstall actions.
3. Add `android-app/app/src/main/java/ai/mobileaihand/TaskPolicy.java`. It receives `TaskSpec`, current package, snapshot node metadata, and `AgentStep`; it rejects a changed package, password/edit fields for reading, out-of-bounds/stale nodes, and any final Send/Post click without a matching `PendingExternalAction` and final user approval. It returns an allow, stop, or require-approval decision; the service cannot call `performAction` or `dispatchGesture` before this decision.
4. Modify `android-app/app/src/main/java/ai/mobileaihand/PhoneControlService.java` to take `TaskSpec`, include active package in snapshots, redact password nodes, enforce `TaskPolicy` before every action, wait for accessibility screen changes with a bounded timeout instead of only fixed sleeps, and stop after 16 actions. Remove raw model output from persisted errors. Use an Accessibility overlay for final approval and re-read the underlying UI after dismissal before the final click; if the package or payload changed or cannot be checked, stop for manual completion.
5. Modify `android-app/app/src/main/java/ai/mobileaihand/AgentStep.java` to parse one complete JSON action and reject trailing content, unsupported fields, and bad node indexes. Add `TaskPolicyTest.java` and expand `AgentStepTest.java` with changed package, stale node, password node, send/post block, invalid JSON, and approval cancellation cases.

### Phase 2 — make outcome and privacy contracts explicit

6. Add `android-app/app/src/main/java/ai/mobileaihand/OutcomeVerifier.java` with the task postconditions above and a typed evidence code. A model `done` can request verification but cannot set `succeeded`. After a possible send/post tap with insufficient evidence, return `uncertain` and block automatic retry.
7. Add `android-app/app/src/main/java/ai/mobileaihand/TaskLog.java` storing the allowed metadata fields as app-private JSON lines, pruning entries older than seven days and offering deletion. Add `TaskCoordinator.java` to own the one-active-task state, cancellation, final approval, verification, and log writes; survive Activity recreation without starting a second task.
8. Modify `android-app/app/src/main/java/ai/mobileaihand/MainActivity.java` to show task status, evidence, and Delete history; distinguish system uninstall confirmation from confirmed package removal. Add `OutcomeVerifierTest.java` and `TaskLogTest.java` for confirmed, failed, uncertain, cancelled, redaction, retention, and duplicate-risk cases.

### Phase 3 — make model and guided flows usable on the phone

9. Add `android-app/app/src/main/java/ai/mobileaihand/ModelStore.java` for app-scoped import with a unique temporary file, available-space check, atomic replacement, and compatibility load check. Modify `MainActivity.java` to use it and disable Run until import completes even across Activity recreation. Modify `LocalModel.java` to surface model-load/generation errors as typed failures; retain the current 4096-token setting for the recommended model pending S24 measurement.
10. Modify `android-app/app/src/main/java/ai/mobileaihand/PhotoInventory.java` to return ID, URI, and timestamp for preview and removal while preserving the local-day `DATE_TAKEN` boundary. Modify `MainActivity.java` to expose draft-only Reddit and select-only WhatsApp modes, thumbnails and photo removal, exact target review, and clear zero/>30 states. Use ChatGPT app UI only for the first completed agent action; keep Reddit/WhatsApp final actions behind the final approval and evidence checks.
11. Modify `android-app/app/build.gradle` to add Android instrumentation test support. Add tests under `android-app/app/src/androidTest/java/ai/mobileaihand/` for model import rotation, permissions, gestures, uninstall cancel/completion, and final approval overlay with a local test Activity. On the S24 Ultra, run the acceptance gates above with the installed apps and accounts; record app versions, Android version, model version, and pass/fail evidence without storing private payloads.

### Phase 4 — document and deliver the first tested build

12. Update `README.md` with the exact tested phone/model setup, approval and outcome behavior, and known app-version limits. Update `PRODUCT_PLAN.md` to mark implemented decisions and remaining work; update `REPOSITORY_RESEARCH.md` only if a reference changes an implementation choice. Add `.github/workflows/android.yml` to build the APK and run unit tests after the source is committed. Keep `tools/laptop-harness/` as an optional comparison tool; do not move it into the phone runtime.

Last updated: 2026-10-26 (Asia/Kolkata)
