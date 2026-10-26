# Stage 7 — feature-specification critic review

Last updated: 2026-10-26 (Asia/Kolkata)

**Result:** Fail against the EGM Feature Specification Standard. Three fresh-context reviewers checked the feature range (lines 1–84): design critic, structural validator, and style editor. The headings, tables, code block, and relative links are sound; the behavioral contracts and some alternatives are not.

| Severity | Design lines | Finding | Required revision |
| --- | --- | --- | --- |
| Blocker | 22–24, 52, 70–71 | The proposed action policy does not define every route that can commit an external action: clicks, keyboard submission, gestures, or opaque controls. | Define a guided per-screen action allowlist and a one-use authorization for every final send/publish path. Block an action when it cannot be classified. |
| Major | 24, 36 | Removing an approval overlay and then tapping permits a screen or recipient change between the check and the action. | Specify screen identity, node freshness, final recheck, approval expiry, and manual fallback when the app cannot give enough evidence. |
| Major | 24, 32, 82 | It is unclear whether the initial Run approval authorizes the ChatGPT question to leave the device. | State this explicitly in the preview and task policy; require that the exact authorized question is what is entered and sent. |
| Major | 25, 31–36, 52 | A nonempty ChatGPT response or visible Reddit/WhatsApp item might be an earlier item, and WhatsApp submission is not the same as delivery. | Correlate new evidence with the task ID, target/account/payload, and time window. Define what `succeeded` means for each service. |
| Major | 25, 52, 83 | Stop, process death, and interruption after a possible final tap have no durable transition/recovery rule. | Check cancellation immediately before every action and recover an unresolved final action as `uncertain`, with no automatic retry. |
| Major | 22, 34, 38, 81 | Photo URIs and permission may change after preview. | Freeze and revalidate the selected set before share/send; stop on a missing or changed item. |
| Major | 52, 75 | The new redacted log design does not explicitly remove the current preference path that saves model-generated or exception text. | Replace that persistence path and ensure all error/report messages are redacted before display or storage. |
| Major | 56–64 | Alternatives omit deterministic guided navigation with the model limited to interpretation. | Compare it with model-directed Accessibility on UI drift, implementation effort, and ability to enforce policy. |
| Major | 11, 82 | The first-release scope says both Reddit publish and WhatsApp send, while the acceptance gate calls for one chosen action. | Name the first completed action and move the other to a later gate. |
| Major | 80, 82 | Device feasibility has no numerical load/latency/memory limits, and `uncertain` ChatGPT outcome can appear to pass the complete-action gate. | Define pass/fail limits and require a correlated successful response for that gate. |
| Major | 23–24, 36, 80–81 | Terms such as “enough information,” “match,” “clear error,” and “safe outcome” do not specify observable behavior. | List exact checked fields, status, user-visible reason, and allowed next action for each failure. |
