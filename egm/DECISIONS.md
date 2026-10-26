# Product and engineering decisions

Last updated: 2026-10-26 (Asia/Kolkata)

`Confirmed` reflects explicit user choices or a current implementation constraint. `Provisional` is a reviewable recommendation, not a user-approved requirement. `Open` must be resolved before its checkpoint enables external actions.

| ID | Status | Decision or question | Basis / next action |
| --- | --- | --- | --- |
| D-01 | Confirmed | The first runtime is Android phone-only; the user's target is a Samsung S24 Ultra. | The user explicitly chose phone-only operation and Android. The laptop harness is a development reference. |
| D-02 | Confirmed | The user wants to request tasks in natural language on the phone, including Reddit, today's photos through WhatsApp, uninstall, and ChatGPT. | Guided forms are a prototype path, not a replacement for the eventual plain-language interface. |
| D-03 | Provisional | Try MediaPipe-compatible Gemma 3n E2B locally before adding a remote model. | The current app uses it, but memory, latency, and accuracy have not been measured on the S24 Ultra. Phone-only does not itself require offline inference. Resolve at C1. |
| D-04 | Provisional | Use no app-owned backend or web frontend for the first phone-only milestone. | Android's native UI, MediaStore, and other installed apps cover the current trial. Revisit only if remote submissions, account sync, hosted inference, or API integrations are chosen. |
| D-05 | Provisional | Use the ChatGPT app UI, not a direct GPT API, for the first complete model-driven task. | It matches the user's app-history example and avoids new API credentials; success still requires a newly observed answer. Resolve before C5. |
| D-06 | Open | Which action is the first complete publish/send workflow: Reddit post or WhatsApp photos? | Enable one at C6, based on real-device reliability and user priority; keep the other disabled until separately tested. |
| D-07 | Open | Should initial task approval authorize a final send/post after exact automated checks, or should the user tap a second final approval? | The design candidate assumes a second approval. The user wants automation, so confirm the desired tradeoff before implementing C6. In either case, require exact task scope and stop on mismatch. |
| D-08 | Open | Is offline/local inference mandatory, or may the phone call a remote model if local accuracy or latency fails C1? | The user asked whether a local model is needed, but has not made offline a hard constraint. |
| D-09 | Provisional | “Today's photos” uses `MediaStore.DATE_TAKEN` and phone-local midnight; preview includes matching screenshots until removed. | The current code queries by capture time. Confirm whether the user means camera-only photos before C4/C6. |
| D-10 | Provisional | Reddit app UI and WhatsApp share UI are the first integration paths; authorized APIs are later alternatives. | This avoids new OAuth/backend work during device feasibility trials, but UI changes must fail safely. |
| D-11 | Open | What is acceptable evidence for `succeeded` versus `uncertain` after a send/post? | A screen observation can show local submission but may not prove service acceptance or WhatsApp delivery. Define exact semantics before C6. |

The [design candidate](designs/phone_only_mvp.md) contains assumptions for review. The [consolidated EGM report](reviews/phone_only_mvp/consolidated_review_report.md) identifies which assumptions are still too vague to implement safely.
