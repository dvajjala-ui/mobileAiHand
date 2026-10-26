package ai.mobileaihand;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** A phone-only prompt UI. No laptop or network service is required at runtime. */
public final class MainActivity extends Activity {
    private static final int PICK_MODEL = 50;
    private static final int IMAGES_PERMISSION = 51;
    private static final String[] ACTIONS = {
            "Any phone task", "Post to Reddit", "Send today's photos on WhatsApp",
            "Uninstall an app", "Ask in the ChatGPT app"
    };
    private Spinner action;
    private EditText target, title, body, prompt;
    private TextView status;
    private final java.util.concurrent.ExecutorService files = Executors.newSingleThreadExecutor();
    private final AtomicBoolean importing = new AtomicBoolean(false);

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        content.setPadding(pad, pad, pad, pad);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
        TextView heading = new TextView(this);
        heading.setText("mobileAiHand · on this phone");
        heading.setTextSize(22);
        content.addView(heading);
        status = new TextView(this);
        content.addView(status);
        button(content, "Enable phone control", () -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        button(content, "Import a local model", this::pickModel);
        action = new Spinner(this);
        action.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, ACTIONS));
        content.addView(action);
        target = field(content, "Target: community, contact, or app package");
        title = field(content, "Reddit post title");
        body = field(content, "Reddit post body");
        prompt = field(content, "Task or question");
        prompt.setMinLines(3);
        button(content, "Review and run", this::prepare);
        button(content, "Stop current task", () -> {
            if (PhoneControlService.instance != null) PhoneControlService.instance.stopTask();
            showStatus("Stop requested. Return to this app to see the result.");
        });
        action.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                target.setVisibility(position == 1 || position == 2 || position == 3 ? View.VISIBLE : View.GONE);
                title.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                body.setVisibility(position == 1 ? View.VISIBLE : View.GONE);
                prompt.setVisibility(position == 0 || position == 4 ? View.VISIBLE : View.GONE);
                target.setHint(position == 1 ? "Reddit community" : position == 2
                        ? "WhatsApp contact" : "Installed app name or package ID");
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        PhoneControlService service = PhoneControlService.instance;
        if (status != null) showStatus(service == null ? "Enable phone control in Android Accessibility settings."
                : service.isRunning() ? service.getReport() : getSharedPreferences("task", MODE_PRIVATE)
                    .getString("report", "Ready. Import a compatible model to begin."));
    }

    @Override protected void onDestroy() {
        files.shutdown();
        super.onDestroy();
    }

    private void showStatus(String message) { runOnUiThread(() -> status.setText(message)); }
    private EditText field(LinearLayout content, String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        content.addView(input);
        return input;
    }
    private void button(LinearLayout content, String label, Runnable action) {
        Button button = new Button(this);
        button.setText(label);
        button.setOnClickListener(view -> action.run());
        content.addView(button);
    }

    private void pickModel() {
        if (importing.get() || (PhoneControlService.instance != null && PhoneControlService.instance.isRunning())) {
            showStatus("Wait until the current model import or phone task finishes.");
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
        try (Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) modelName = cursor.getString(0);
        }
        if (modelName == null || !modelName.toLowerCase(java.util.Locale.ROOT).endsWith(".task")) {
            showStatus("Select a MediaPipe .task model file, not GGUF.");
            return;
        }
        if (!importing.compareAndSet(false, true)) return;
        showStatus("Copying model into private app storage…");
        files.execute(() -> {
            File destination = new File(getFilesDir(), "local-model.task");
            File temporary = new File(getFilesDir(), "local-model.task.part");
            try (InputStream source = getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(temporary)) {
                if (source == null) throw new IllegalStateException("Cannot open selected model");
                byte[] buffer = new byte[1024 * 1024];
                int count;
                while ((count = source.read(buffer)) != -1) output.write(buffer, 0, count);
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                showStatus("Model copied. Compatibility will be checked on the first task.");
            } catch (Exception ex) {
                temporary.delete();
                showStatus("Model import failed: " + ex.getMessage());
            } finally {
                importing.set(false);
            }
        });
    }

    private void prepare() {
        if (importing.get()) { showStatus("Wait for model import to finish."); return; }
        int selected = action.getSelectedItemPosition();
        String recipient = target.getText().toString().trim();
        String resolvedTarget = recipient;
        String request = prompt.getText().toString().trim();
        String goal;
        ArrayList<Uri> photos = null;
        if (selected == 0) {
            if (request.isEmpty()) { showStatus("Enter a task."); return; }
            goal = request;
        } else if (selected == 1) {
            String community = recipient.replaceFirst("^r/", "");
            String postTitle = title.getText().toString().trim();
            String postBody = body.getText().toString().trim();
            if (!community.matches("[A-Za-z0-9_]{1,21}") || postTitle.isEmpty() || postBody.isEmpty()) {
                showStatus("Enter a valid community, title, and body."); return;
            }
            goal = "In the Reddit app, post to r/" + community + " with title '" + postTitle
                    + "' and body '" + postBody + "'. Verify the community and text before posting, then check that the post appears.";
        } else if (selected == 2) {
            if (recipient.isEmpty()) { showStatus("Enter the exact WhatsApp contact."); return; }
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, IMAGES_PERMISSION);
                showStatus("Allow full photo access, then tap Review and run again.");
                return;
            }
            photos = PhotoInventory.today(this);
            if (photos.isEmpty()) { showStatus("No accessible photos were taken today."); return; }
            if (photos.size() > 30) { showStatus("More than 30 photos found. Batch sending is not implemented yet."); return; }
            goal = "In WhatsApp's share flow, select the exact contact '" + recipient + "', then send the "
                    + photos.size() + " attached photos. Check the conversation for the sent attachments. "
                    + "If the recipient is ambiguous, stop without sending.";
        } else if (selected == 3) {
            try { resolvedTarget = resolveInstalledApp(recipient); }
            catch (IllegalArgumentException ex) { showStatus(ex.getMessage()); return; }
            goal = "Open Android's uninstall confirmation for " + resolvedTarget;
        } else {
            if (request.isEmpty()) { showStatus("Enter a question for ChatGPT."); return; }
            goal = "In the ChatGPT app, send this exact question and report its answer: " + request;
        }
        if (selected != 3 && PhoneControlService.instance == null) {
            showStatus("Enable mobileAiHand in Android Accessibility settings first."); return;
        }
        if (selected != 3 && !new File(getFilesDir(), "local-model.task").isFile()) {
            showStatus("Import a MediaPipe-compatible local model first."); return;
        }
        ArrayList<Uri> preparedPhotos = photos;
        String preparedTarget = resolvedTarget;
        String description = selected == 2 ? goal + "\nPhotos selected by capture date: " + photos.size() : goal;
        new AlertDialog.Builder(this).setTitle("Review phone task")
                .setMessage(description).setNegativeButton("Cancel", null)
                .setPositiveButton("Run", (dialog, which) -> runTask(selected, preparedTarget, goal, preparedPhotos)).show();
    }

    private String resolveInstalledApp(String query) {
        PackageManager packages = getPackageManager();
        List<AppResolver.Candidate> installed = new ArrayList<>();
        for (ApplicationInfo info : packages.getInstalledApplications(0)) {
            if (!info.packageName.equals(getPackageName())) {
                installed.add(new AppResolver.Candidate(
                        String.valueOf(info.loadLabel(packages)), info.packageName));
            }
        }
        return AppResolver.resolve(installed, query);
    }

    private void runTask(int selected, String target, String goal, ArrayList<Uri> photos) {
        if (selected == 3) {
            Intent uninstall = new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + target));
            try { startActivity(uninstall); showStatus("Android opened the uninstall confirmation."); }
            catch (Exception ex) { showStatus("Could not open uninstall flow: " + ex.getMessage()); }
            return;
        }
        PhoneControlService service = PhoneControlService.instance;
        if (service == null || service.isRunning()) { showStatus("Phone control is unavailable or busy."); return; }
        try {
            if (selected == 1 || selected == 4) {
                String packageName = selected == 1 ? "com.reddit.frontpage" : "com.openai.chatgpt";
                Intent launch = getPackageManager().getLaunchIntentForPackage(packageName);
                if (launch == null) { showStatus("Required app is not installed: " + packageName); return; }
                startActivity(launch);
            } else if (selected == 2) {
                Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE);
                share.setType("image/*");
                share.setPackage("com.whatsapp");
                share.putParcelableArrayListExtra(Intent.EXTRA_STREAM, photos);
                share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                ClipData clip = ClipData.newRawUri("photo", photos.get(0));
                for (int i = 1; i < photos.size(); i++) clip.addItem(new ClipData.Item(photos.get(i)));
                share.setClipData(clip);
                startActivity(share);
            }
            service.runTask(goal, new File(getFilesDir(), "local-model.task").getAbsolutePath());
            showStatus("Local model is starting. Return to mobileAiHand later to see the report.");
        } catch (Exception ex) { showStatus("Could not start task: " + ex.getMessage()); }
    }
}
