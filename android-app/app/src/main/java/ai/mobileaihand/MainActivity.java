package ai.mobileaihand;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.database.Cursor;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.util.Locale;

/** Phone-only entry point for the first, Settings-only device trial. */
public final class MainActivity extends Activity {
    private static final int PICK_MODEL = 50;
    private TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable importWatcher = new Runnable() {
        @Override public void run() {
            showStatus(ModelStore.status());
            if (ModelStore.isImporting()) handler.postDelayed(this, 500);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        // Older builds saved model-written messages in this key. Do not show them again.
        getSharedPreferences("task", MODE_PRIVATE).edit().remove("report").apply();
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);

        TextView heading = new TextView(this);
        heading.setText("mobileAiHand · Settings trial");
        heading.setTextSize(22);
        content.addView(heading);
        TextView explanation = new TextView(this);
        explanation.setText("This early build tests a local model in Android Settings. Reddit, WhatsApp, ChatGPT, and general phone actions are paused until task-specific checks are ready.");
        content.addView(explanation);
        status = new TextView(this);
        content.addView(status);

        button(content, "Enable phone control", () ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        button(content, "Import a local model", this::pickModel);
        button(content, "Read Android version in Settings", this::prepareTrial);
        button(content, "Stop current trial", () -> {
            PhoneControlService service = PhoneControlService.instance;
            if (service != null) service.stopTask();
            showStatus("Stop requested. Return to this app for the result.");
        });
    }

    @Override protected void onResume() {
        super.onResume();
        PhoneControlService service = PhoneControlService.instance;
        if (ModelStore.isImporting()) {
            watchImport();
        } else if (status != null) {
            showStatus(service == null ? "Enable mobileAiHand in Android Accessibility settings."
                    : service.isRunning() ? service.getReport() : getSharedPreferences("task", MODE_PRIVATE)
                    .getString("settings_trial_report", "Ready. Import a compatible model to begin."));
        }
    }

    @Override protected void onDestroy() {
        handler.removeCallbacks(importWatcher);
        super.onDestroy();
    }

    private void showStatus(String message) {
        if (status != null) runOnUiThread(() -> status.setText(message));
    }

    private void button(LinearLayout content, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view -> action.run());
        content.addView(button);
    }

    private void watchImport() {
        handler.removeCallbacks(importWatcher);
        handler.post(importWatcher);
    }

    private void pickModel() {
        PhoneControlService service = PhoneControlService.instance;
        if (ModelStore.isImporting() || (service != null && service.isRunning())) {
            showStatus("Wait until the current import or trial finishes.");
            return;
        }
        Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        picker.setType("*/*");
        picker.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(picker, PICK_MODEL);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_MODEL || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String modelName = "";
        try (Cursor cursor = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) modelName = cursor.getString(0);
        } catch (Exception ex) {
            showStatus("Could not read the selected model name.");
            return;
        }
        if (modelName == null || !modelName.toLowerCase(Locale.ROOT).endsWith(".task")) {
            showStatus("Select a MediaPipe .task model file, not GGUF.");
            return;
        }
        if (!ModelStore.importModel(this, uri)) {
            showStatus("A model import is already running.");
            return;
        }
        watchImport();
    }

    private void prepareTrial() {
        if (ModelStore.isImporting()) { showStatus("Wait for model import to finish."); return; }
        PhoneControlService service = PhoneControlService.instance;
        if (service == null || service.isRunning()) {
            showStatus("Enable phone control, or wait for the current trial to finish.");
            return;
        }
        File model = ModelStore.modelFile(this);
        if (!model.isFile()) { showStatus("Import a compatible local model first."); return; }
        new AlertDialog.Builder(this).setTitle("Review Settings trial")
                .setMessage("Open Android's About phone screen and use the local model to find the Android version. The trial may open safe Settings pages or scroll, but cannot type, switch apps, post, send, or uninstall.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Run trial", (dialog, which) -> runSettingsTrial(service, model))
                .show();
    }

    private void runSettingsTrial(PhoneControlService service, File model) {
        Intent settings = new Intent(Settings.ACTION_DEVICE_INFO_SETTINGS);
        settings.setPackage(SettingsTrialPolicy.SETTINGS_PACKAGE);
        try {
            startActivity(settings);
            service.runSettingsTrial(model.getAbsolutePath());
            showStatus("Local model is starting. Return here for the verified result.");
        } catch (Exception ex) {
            showStatus("Could not start the Settings trial on this phone.");
        }
    }
}
