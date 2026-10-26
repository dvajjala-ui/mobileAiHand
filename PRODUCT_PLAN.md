# mobileAiHand: initial product plan

## Goal

Accept a plain-language task from a phone or laptop, carry it out on the user's phone, and report what happened. Initial examples are posting to Reddit, sending today's photos to a WhatsApp contact, uninstalling an app, and asking a question of GPT.

## First implementation target

Start with a phone-only Android app on the user's S24 Ultra. Its accessibility service can inspect visible UI and perform actions across apps, its media APIs can find photos by capture date, and MediaPipe LLM Inference can run a compatible model locally. iPhone support needs a separate design and device test.

The native app uses Mobilerun Portal's accessibility-control approach as a reference and Google AI Edge Gallery's published MediaPipe model catalog for the local model choice. The earlier [Mobilerun](https://github.com/droidrun/mobilerun) laptop runner remains a comparison harness. Use [Maestro](https://github.com/mobile-dev-inc/maestro) for repeatable app-flow tests when a real device is available.

## Workflow

1. The Android app accepts and previews a task on the phone.
2. The controller turns the request into explicit steps and asks for missing details, such as a Reddit community or WhatsApp contact.
3. Use Android APIs for media selection and uninstall confirmation, and accessibility control for third-party app interfaces.
4. Show a preview before an externally visible action, such as publishing a post or sending photos. The user may later opt into a narrower standing approval rule.
5. Execute the steps, inspect the resulting app state, and report success, failure, or uncertainty. Keep a task log that does not contain photo contents or credentials by default.

## Action-specific path

| Task | Initial path | Required detail / constraint |
| --- | --- | --- |
| Post to Reddit | Draft content; use Reddit's authorized API where suitable, with UI control as a fallback | Community, title, content, account authorization, and a preview before publishing. Communities can reject posts. |
| Send today's photos on WhatsApp | Query Android media by capture date, select the files, then use the share flow or WhatsApp UI | Recipient and local date boundary. Android media permission is required. Verify the recipient and attachment count before sending. |
| Uninstall an app | Open Android's system uninstall flow for the selected package, or use ADB from an explicitly paired laptop | Confirm the exact app. A regular Android app needs system confirmation; an authorized ADB host can uninstall directly. |
| Send a prompt to GPT | Use an authorized model API, or control the ChatGPT app if the user wants the interaction in that app | Model API calls do not automatically appear in the user's ChatGPT app history. |

## First real-device milestone

Install the debug APK and a compatible local model on the S24 Ultra. Run "Open Settings and tell me the Android version" entirely on the phone and verify the answer. Then test a Reddit draft without publishing it and select today's photos without sending them. Complete send/post actions only after the basic device loop works.

## Decisions needed before implementation

- Whether remote laptop submission is needed later; the first version runs entirely on the phone.
- Whether the local model can reliably choose actions in Reddit, WhatsApp, and ChatGPT on the user's S24 Ultra.
- Which of the example tasks should be the first complete end-to-end action.

The repository started empty and now contains a buildable Android prototype. Following the EGM review, the runnable app was narrowed to a Settings-only local-model trial; model-driven Reddit, WhatsApp, ChatGPT, and uninstall flows are paused until their checks are implemented. No device or app accounts are attached to this cloud workspace, so real phone actions cannot yet be verified here.

Last updated: 2026-10-26 (Asia/Kolkata)
