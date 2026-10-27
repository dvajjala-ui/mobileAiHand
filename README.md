# mobileAiHand for Android

This is a **phone-only Settings trial** for a Samsung S24 Ultra. It imports a local MediaPipe model, opens Android's About phone screen, and lets the model find the Android version through a narrow Accessibility controller. The current runnable app cannot type, open another app, send photos, post to Reddit, ask ChatGPT, or uninstall anything. Those guided workflows are paused until task-specific checks and real-device tests are ready. The [EGM development hub](egm/README.md) tracks their checkpoints and issues.

## Put the trial on your phone

1. Build the debug APK using the instructions below. Copy `android-app/app/build/outputs/apk/debug/app-debug.apk` to your S24 Ultra and install it. The generated APK is ignored by Git and is not included in a repository clone. This is a development APK, not a Play Store release.
2. Download the MediaPipe-compatible [`gemma-3n-E2B-it-int4.task`](https://huggingface.co/google/gemma-3n-E2B-it-litert-preview) file onto your phone. Google's [AI Edge Gallery model list](https://github.com/google-ai-edge/gallery/blob/main/model_allowlist.json) lists this exact file for MediaPipe LLM Inference. It is about 3.1 GB and has an estimated 5.9 GB peak memory use. Keep at least 7 GB free while importing because the file is copied into app-private storage. A GGUF file will not work.
3. Open **mobileAiHand**, tap **Import a local model**, and select the `.task` file. The import continues if the screen rotates. After copying, the app loads the candidate with MediaPipe before replacing the previous model. A failed copy or compatibility load leaves the previous model file in place. This check can take time and still needs a real-phone trial.
4. Tap **Enable phone control**, find **mobileAiHand** in Android Accessibility settings, and enable it. Return to the app.
5. Tap **Read Android version in Settings**, review the narrow trial description, and tap **Run trial**. Keep the phone unlocked. When it finishes, return to mobileAiHand for the result. The app reports a verified version only if the expected Android version appears next to an **Android version** label in two successive Settings accessibility snapshots. The result also shows model-load, median decision, and total times for the phone trial.

The trial recognizes English Settings labels such as **About phone**, **Software information**, and **Android version**. It may stop safely if this phone's UI uses different labels or hides the version. The S24 Ultra and model have not yet been tested together in this workspace. A successful build and unit tests do not prove the phone task succeeds.

The local model and Settings observations stay on the device. This APK requests no internet, photo-library, notification, or all-package-list permission. The earlier Reddit, WhatsApp, ChatGPT, and uninstall source paths remain in Git history and the [product plan](PRODUCT_PLAN.md), but are not callable from this safety trial.

## Build from source

Use Android Studio with JDK 17 or newer and Android SDK 35. Open `android-app`, or run:

```sh
cd android-app
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The APK is written to `android-app/app/build/outputs/apk/debug/app-debug.apk`. The build uses Gradle 8.9 and MediaPipe Tasks GenAI 0.10.35. In this managed cloud workspace, `./scripts/setup-cloud-android.sh` installs the build tools into `.local/`, then builds and tests. The old Python/laptop experiment remains in [tools/laptop-harness](tools/laptop-harness/README.md) as a development comparison.

## Current limits

- The trial reads Accessibility text, not screenshots. It stops on unrecognized navigation, an unexpected app, invalid model actions, a changed screen, or eight navigation steps without verified version evidence.
- The app now reports model-load, decision, and total times, but actual S24 Ultra values, memory behavior, and task success are unmeasured. Record them at [checkpoint C1](egm/CHECKPOINTS.md).
- Model import checks storage, copy completeness, and whether MediaPipe can load the candidate before replacing the previous model. Successful task inference on the S24 Ultra is still unverified.

Last updated: 2026-10-27 (Asia/Kolkata)
