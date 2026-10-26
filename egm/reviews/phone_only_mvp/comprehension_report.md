# Stage 6 — comprehension test

Last updated: 2026-10-26 (Asia/Kolkata)

**Result:** Fail. The feature range (lines 1–84) and detailed implementation range (lines 85–111) leave behaviorally significant choices to a new implementer.

| Severity | Design lines | Finding and required clarification |
| --- | --- | --- |
| Blocker | 23–24, 33–36, 52, 92–94 | `PendingExternalAction` is undefined. Specify unique target/account identity, payload and attachment comparison, final-control detection, approval creation/expiry, and conditions permitting the final tap. |
| Blocker | 21–23, 52, 92–94 | Allowed packages and action categories have no per-task matrix. Define permitted package transitions, click/type/swipe/back/open-app behavior, Android system/share UI handling, and a rule that typed content matches approved fields. |
| Major | 25, 31–36, 52, 80–83, 99–101 | A visible response, post, or attachment is not necessarily the result of this task. Specify correlation evidence, timeouts, evidence codes, and mapping to `succeeded`, `failed`, `uncertain`, and `cancelled`. |
| Major | 24–25, 52, 83, 92, 99–100 | States lack a legal transition table. Define cancel during inference/final approval, Activity recreation, process death, and interruption after a possible final tap. |
| Major | 23, 52, 74, 93–95 | Stale node, screen change, node role, and bounded timeout are not testable. Specify snapshot age/identity, role checks, event wait, and fresh-node requirements. |
| Major | 34, 38, 81, 106 | The photo set is not frozen at a defined point. Specify removal/count behavior and what happens at midnight, permission loss, or a missing photo before sharing. |
| Major | 87, 92–94, 100, 106 | Phase 1 changes the service interface without updating its `MainActivity` caller. It also requires final approval before Phase 2 creates the coordinator that owns approval. Reorder or add compile-safe temporary contracts. |
| Major | 80, 105 | Model import does not specify free-space threshold, atomic commit, failed compatibility load behavior, or whether the old model remains usable. |
