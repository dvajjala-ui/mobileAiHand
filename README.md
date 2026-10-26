# mobileAiHand for Android

This is a **phone-only prototype**. It takes a prompt on the Android phone, runs a local model on that phone, and uses Android Accessibility to operate other apps. A laptop is useful for installing or building the APK, but is not part of the running system.

The current app has guided entry for Reddit posts, today's photos on WhatsApp, uninstalling an app, and asking in the ChatGPT app. The uninstall flow uses Android's system confirmation. For the other tasks, the local agent reads visible UI text and tries the next action. They still need tests on a real phone; an APK build does not prove an app task succeeded.

## Put it on your phone

1. Build the debug APK using the instructions below, copy `android-app/app/build/outputs/apk/debug/app-debug.apk` to your S24 Ultra, and install it. The generated APK is ignored by Git and is not included in a repository clone. Android may ask you to allow installation from the app you used to open it. This APK is for development, not a Play Store release.
2. Download the MediaPipe-compatible model file [`gemma-3n-E2B-it-int4.task`](https://huggingface.co/google/gemma-3n-E2B-it-litert-preview) onto your phone. Google's [AI Edge Gallery model list](https://github.com/google-ai-edge/gallery/blob/main/model_allowlist.json) lists this exact file for MediaPipe LLM Inference. It is about 3.1 GB and has an estimated 5.9 GB peak memory use. Keep at least 7 GB free while importing because the downloaded file is copied into the app's private storage. A standard GGUF file will not work in this build.
3. Open **mobileAiHand** and tap **Import a local model**. Select the downloaded `.task` file and wait until copying finishes.
4. Tap **Enable phone control**, find **mobileAiHand** in Android Accessibility settings, and enable it. Return to the app.
5. Start with **Any phone task** → “Open Settings and tell me the Android version.” Review the request and tap **Run**. Keep the phone unlocked while the agent works, then return to mobileAiHand to read its report.
6. Use the guided tasks after the basic task works. For uninstalling, enter the installed app's name or package ID and approve Android's system confirmation. WhatsApp requires **full photo access**; the app counts images captured during the phone's current calendar day before opening the share flow. It currently stops if more than 30 photos match, since multi-batch sending is not ready.

The imported model and phone actions stay on the device. Asking a question in the **ChatGPT app** still uses that app's network and account; the local model only decides how to operate its UI. Reddit and WhatsApp also need their installed apps and signed-in accounts.

The S24 Ultra is a reasonable target for the 3n E2B model, but its success on this phone has not been measured yet. The app currently sets the model context for that specific model. Other `.task` models may need a different context setting or agent prompt before they work well.

## Build from source

Use Android Studio with JDK 17 or newer and Android SDK 35. Open `android-app`, or run:

```sh
cd android-app
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The APK is written to `android-app/app/build/outputs/apk/debug/app-debug.apk`. The build uses Gradle 8.9 and MediaPipe Tasks GenAI 0.10.35. The model is downloaded separately and is not bundled in the APK. The old Python/laptop experiment remains in [tools/laptop-harness](tools/laptop-harness/README.md) as a development reference.

The [EGM development hub](egm/README.md) holds the dated architecture findings, five-stage design review, issue backlog, and next product checkpoints.

## Current limits

- The agent reads Android Accessibility text and element bounds. Apps with unlabeled controls, image-only screens, or protected views may stop it; screenshot understanding is not implemented.
- The app caps an agent run at 16 actions and reports when it cannot complete a task. It does not yet verify Reddit posts or WhatsApp sends through their service APIs.
- Photo sharing is limited to 30 images in one batch. Photos are selected by capture date, not download date.
- The cloud workspace can build and test the APK but has no attached S24 Ultra, so real device behavior remains unverified.

Last updated: 2026-10-26 (Asia/Kolkata)
