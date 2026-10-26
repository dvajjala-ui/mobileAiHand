# GitHub references for mobileAiHand

Reviewed the public README, license, and relevant source at the linked repository HEADs on 26 October 2026. This is implementation research, not a claim that their phone workflows ran in this cloud workspace.

| Project | What its code demonstrates | Use for mobileAiHand |
| --- | --- | --- |
| [Mobilerun](https://github.com/droidrun/mobilerun) (MIT) | A natural-language runner with Android device selection, screenshots, UI trees, taps, and a Python/CLI interface. Its Android [Portal](https://github.com/droidrun/mobilerun-portal) is an accessibility service with local control APIs. | Reference for the agent action loop and accessibility control. Its Python CLI remains a laptop comparison harness because the user chose phone-only runtime. |
| [phone-harness](https://github.com/ShawnPana/phone-harness/blob/main/src/phone_harness/android.py) (MIT) | Android control over ADB: screenshot, UIAutomator tree, taps, typing, app launch, and persistent selection of one phone. | Adopt explicit device selection. Its low-level helpers are a possible fallback when the agent cannot reliably locate a control. |
| [DroidMind](https://github.com/hyperb1iss/droidmind/tree/main/droidmind/tools) (Apache-2.0) | MCP tools for device files, packages, intents, UI, and shell, with command risk checks. | Reference for explicit app management actions such as finding the right package before uninstalling. Not needed as a second agent runtime yet. |
| [mobile-use](https://github.com/minitap-ai/mobile-use/tree/main/minitap/mobile_use) (Apache-2.0) | A structured agent with Android controller, planner, and typed output. Its [photo organizer example](https://github.com/minitap-ai/mobile-use/blob/main/minitap/mobile_use/sdk/examples/simple_photo_organizer.py) navigates a gallery by date. | Reference for task-specific steps and checking the result. Compare it with Mobilerun on a real phone before changing runners. |
| [OGAM](https://github.com/off-grid-ai/OGAM) (MIT for the main repository) | A React Native phone app with local models, voice input, and tool calls. Its README describes phone-to-Mac sync as a Pro feature, and `pro` is a separate Git submodule. | Reference for a later native prompt app and offline model option. Treat Pro sync as a product idea, not reusable MIT source. It is not a cross-app phone controller. |
| [uiautomator2](https://github.com/openatx/uiautomator2) (MIT) and [scrcpy](https://github.com/Genymobile/scrcpy) (Apache-2.0) | Android UI element automation and screen mirroring/control respectively. | Useful lower-level device tools, but neither interprets a natural-language task by itself. |
| [Maestro](https://github.com/mobile-dev-inc/maestro) and [Appium](https://github.com/appium/appium) (both Apache-2.0) | Repeatable mobile UI automation flows and cross-platform device drivers. | Use for validating specific app tasks once a phone is available, rather than as the main assistant. |
| [Google AI Edge Gallery](https://github.com/google-ai-edge/gallery) (Apache-2.0) | An on-device Android model app. Its [model catalog](https://github.com/google-ai-edge/gallery/blob/main/model_allowlist.json) identifies Gemma 3n E2B `.task` as compatible with MediaPipe LLM Inference and gives file size and estimated peak memory. | Use MediaPipe Tasks GenAI in the phone app and recommend that exact model for the S24 Ultra. |
| [Elephant-Goldfish Model](https://github.com/google/elephant-goldfish-model) (Apache-2.0) | A Codex plugin for design documents and reviews by coding agents; it does not run on Android or provide a language model. | Use its `egm-drift-report` skill and fresh-context reviewers to assess the app. See the [EGM development hub](egm/README.md), [drift report](egm/reviews/initial/2026-10-26_DRIFT_REPORT.md), and [architecture review](egm/reviews/initial/2026-10-26_EGM_REVIEW.md). |
| [REA](https://github.com/morluto/rea) (MIT), [Ghidra](https://github.com/NationalSecurityAgency/ghidra) (Apache-2.0), [Reverse-Engineering](https://github.com/mytechnotalent/Reverse-Engineering) (Apache-2.0) | Tools and learning material for analyzing binaries and closed software. | No need to decompile these open-source projects. Use published source and documented interfaces; consider binary analysis only for a specific unavailable behavior with a lawful test artifact. |

The user also mentioned “Jarvish” without a repository URL. GitHub has several projects with that name, so none is selected as a technical dependency.

## Implementation decisions from the survey

1. The user chose phone-only operation. The native Android app runs MediaPipe inference and its own AccessibilityService; the laptop harness is retained only for development comparisons.
2. Keep the app's accessibility actions narrow and typed, following Portal and DroidMind. The local model can propose click, type, swipe, back, open-app, or done; it cannot execute arbitrary shell commands.
3. Use Android MediaStore to pick images captured today and Android's system uninstall flow. Third-party app interactions still require a real-phone trial and stronger result verification.
4. OGAM remains a reference for a polished native prompt interface and offline experience; its Pro submodule is separate from the open main repository.

The current environment lacks a connected Android device. It can build the phone APK and run local tests, but it cannot validate model quality or app task success on a phone.

Last updated: 2026-10-26 (Asia/Kolkata)
