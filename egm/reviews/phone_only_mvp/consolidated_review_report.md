# EGM five-stage review — phone-only Android assistant

Last updated: 2026-10-26 (Asia/Kolkata)

**Target:** [phone_only_mvp.md](../../designs/phone_only_mvp.md), draft dated 26 October 2026. **Preset:** uninterrupted full review. **Context:** root project documentation and current Android source/tests. Each Goldfish reviewer was started with fresh conversation context and its EGM persona/standard. Stages 5, 6, 7, 8, and 9 all completed in order. The target was not edited between stages; this is one review pass, not a remediation/re-audit loop.

## Stage results

| EGM stage | Result | Main reason | Detailed report |
| --- | --- | --- | --- |
| 5 — Initial zoom out | Fail | Too many first-release outcomes before a real-phone feasibility trial; guided flows do not fulfill the plain-language goal. | [Zoom out](zoom_out_report.md) |
| 6 — Comprehension | Fail | Final approval, task policy, evidence, and state contracts leave significant choices to implementers. | [Comprehension](comprehension_report.md) |
| 7 — Critic | Fail | Generic Accessibility actions can commit sends through routes the proposed policy cannot classify; result claims lack correlation. | [Critic](critic_report.md) |
| 8 — Intermediate zoom out | Fail | Earlier scope/sequence findings remain; generic policy abstraction is premature for an untested phone flow. | [Second zoom out](intermediate_zoom_out_report.md) |
| 9 — Implementation readiness | Fail | Phase 1 is not compile-safe and could enable external actions before verification; file contracts and tests are incomplete. | [Implementation readiness](implementation_readiness_report.md) |

## Consolidated actions, in priority order

1. **Blocker — prove a narrow, harmless phone loop before expanding the controller.** Keep the initial milestone a read-only Settings version lookup on the S24 Ultra. Add only the minimum policy needed to keep that trial in Settings and prevent typing or external actions. Measure model load and action behavior there before committing to the broader architecture. Choose one later completed external action; move the others behind separate gates. This resolves the scope and order findings from stages 5, 7, and 8.
2. **Blocker — replace the generic action promise with an enforceable per-task policy.** For the selected workflow, define each recognized screen, allowed package transition, allowed click/type/swipe/back/open-app action, exact text payload, node freshness, and stop condition. Include keyboard submission and ancestor clicks as possible committing actions. Reject opaque controls. Specify `PendingExternalAction` fields, one-use approval, expiry, and final recheck. This addresses stages 6–9. If deterministic guided navigation proves simpler than model-directed clicks, use it for consequential steps.
3. **Blocker — define honest result evidence and recovery.** Bind a newly observed ChatGPT answer, Reddit post, WhatsApp item, or uninstall package change to this task's target, payload, account, and time window. Specify evidence codes, timeouts, status precedence, cancellation, process-death recovery, and a no-auto-retry rule after a possible send/post tap. A model `done` response must never itself establish success. This addresses stages 6, 7, and 9.
4. **Blocker — reorder the implementation into buildable, safe slices.** Phase 1 must update the `MainActivity` caller when changing `PhoneControlService.runTask`, construct the correct `TaskSpec`, and disable unfinished post/send flows. Introduce coordinator/approval ownership together. Do not enable external final taps before outcome handling and device tests exist. Name every added file, especially `PendingExternalAction`, overlay implementation, and test fixtures. This addresses stages 6, 8, and 9.
5. **Major — finish model, media, privacy, and test contracts.** Set S24 Ultra model-load/action-latency and process-survival thresholds. Specify model import atomicity/rollback and Run availability across rotation. Freeze and revalidate the approved photo URI subset; define Android partial-photo permission behavior and rebuild both share-intent payload carriers from that subset. Remove the current persisted model-written `task/report` value and test redaction, retention, gestures, and interruption with expected outcomes. This addresses stages 6, 7, and 9.
6. **Major — align the product claim with the first release.** The draft currently offers guided tasks and Settings-only free-form inspection, while the original goal is to type any supported natural-language request. Either call this a guided feasibility build or specify supported-intent recognition and missing-details questions. Define whether initial Run authorizes sending a ChatGPT prompt, and whether Reddit/WhatsApp require a second final tap. This addresses stages 5–8.

## Traceability matrix

| Consolidated action | Raw finding sources |
| --- | --- |
| 1 — narrow device trial | Stage 5 findings 1–3; Stage 7 release-scope and threshold findings; Stage 8 findings 1–3 |
| 2 — enforceable task policy | Stage 6 findings 1–2, 5; Stage 7 findings 1–2; Stage 8 finding 4; Stage 9 findings 3–4, 6 |
| 3 — outcome and recovery | Stage 6 findings 3–4; Stage 7 findings 3–5; Stage 9 finding 4 |
| 4 — safe, buildable phases | Stage 6 finding 7; Stage 8 finding 5; Stage 9 findings 1–2, 9 |
| 5 — model/media/privacy/tests | Stage 6 findings 6, 8; Stage 7 findings 6–7, 10; Stage 9 findings 5–9 |
| 6 — product claim and consent | Stage 5 findings 1–2; Stage 7 findings 3, 9; Stage 8 findings 1–2 |

## What the result means

The full EGM pass ran successfully as a **review process**. The candidate design did **not** pass its quality gates and should not be treated as an approved implementation plan. The failure is not a rejection of phone-only Android: existing `MediaStore`, share-intent, system uninstall, and local-model building blocks remain appropriate for a prototype. The next revision needs one narrowly tested device path and explicit action/evidence contracts before building out automatic Reddit and WhatsApp completion.

No Android source was changed by this review. The existing debug APK and six unit-test results demonstrate buildability of the earlier prototype only; no real-device task was verified in this pass.
