# EGM product-development hub

Last updated: 2026-10-26 (Asia/Kolkata)

This folder records what mobileAiHand is trying to build, what the current Android prototype proves, and what must be checked before automatic external actions are enabled. The repo's [main README](../README.md) is the installation guide; the [product plan](../PRODUCT_PLAN.md) and [repository research](../REPOSITORY_RESEARCH.md) give the original context.

## Current assessment

The phone-only Android direction fits the user's Samsung S24 Ultra goal. The debug APK builds and six unit tests pass. No local model run, Settings task, Reddit post, WhatsApp send, or ChatGPT action has been verified on the actual phone. The Accessibility controller can currently act across apps after an initial task review and trusts a model-written completion message. Treat it as a prototype until the checkpoints below are met.

The full EGM five-stage **review process completed**, but the [candidate design](designs/phone_only_mvp.md) failed all five quality gates. A failed review does not mean abandoning Android; it identifies contracts and tests that must be specified before implementation. EGM is a development-review plugin, not the on-phone model.

## Document map

| Document | Purpose |
| --- | --- |
| [Checkpoint plan](CHECKPOINTS.md) | Ordered, testable product-development milestones and stop/go gates. |
| [Issue backlog](ISSUES.md) | Prioritized work items tied to review findings and checkpoints. |
| [Decision log](DECISIONS.md) | Confirmed choices, provisional design assumptions, and open product questions. |
| [Design candidate](designs/phone_only_mvp.md) | Feature specification and a draft detailed implementation plan. It is not approved for implementation as written. |
| [Five-stage consolidated review](reviews/phone_only_mvp/consolidated_review_report.md) | Results and traceability across EGM stages 5–9. Links to every stage report. |
| [Initial architecture review](reviews/initial/2026-10-26_EGM_REVIEW.md) | Six fresh-context reviewer findings on the existing plan and Android code. |
| [Plan-to-code drift report](reviews/initial/2026-10-26_DRIFT_REPORT.md) | Differences between the first product plan and prototype. |

## Recommended development order

1. Prove one harmless, read-only Settings task with the local model on the S24 Ultra. Apply a minimal Settings-only action guard first.
2. Define task-specific allowed actions and evidence for **one** complete external workflow. Keep the model advisory where deterministic Android APIs or guided navigation can perform a step.
3. Add final target/content checks, honest `uncertain` results, interruption handling, and no-duplicate-send behavior before Reddit publishing or WhatsApp sending.
4. Test on the phone, then broaden the natural-language prompt interface. No backend or web frontend is needed for the agreed phone-only first release.

When a decision or finding changes, update its linked issue and the **Last updated** line in the affected document. Rerun the relevant EGM review stages after revising the design; these reports describe the 26 October 2026 draft, not later code.
