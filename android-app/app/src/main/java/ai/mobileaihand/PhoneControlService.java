package ai.mobileaihand;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** Text-tree Android controller, inspired by Mobilerun Portal's accessibility approach. */
public final class PhoneControlService extends AccessibilityService {
    static volatile PhoneControlService instance;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private volatile boolean running;
    private volatile String report = "No task has run yet.";

    @Override protected void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) { }
    @Override public void onInterrupt() { stop.set(true); }
    @Override public void onDestroy() {
        stop.set(true);
        instance = null;
        worker.shutdownNow();
        super.onDestroy();
    }

    boolean isRunning() { return running; }
    String getReport() { return report; }
    void stopTask() { stop.set(true); }

    synchronized void runTask(String goal, String modelPath) {
        if (running) throw new IllegalStateException("A phone task is already running");
        running = true;
        stop.set(false);
        report = "Starting local model…";
        worker.execute(() -> {
            try (LocalModel model = new LocalModel(this, modelPath)) {
                executeLoop(goal, model);
            } catch (Exception ex) {
                report = "Task stopped: " + ex.getMessage();
            } finally {
                running = false;
                getSharedPreferences("task", MODE_PRIVATE).edit().putString("report", report).apply();
            }
        });
    }

    private void executeLoop(String goal, LocalModel model) throws Exception {
        String history = "";
        String matchingApps = matchingApps(goal);
        Thread.sleep(1200);
        for (int turn = 0; turn < 16 && !stop.get(); turn++) {
            Snapshot snapshot = snapshot();
            if (snapshot.nodes.isEmpty()) throw new IllegalStateException("No accessible screen. Unlock the phone and enable mobileAiHand Accessibility.");
            String prompt = "You control an Android phone through its accessibility tree. Goal: " + goal + "\n"
                    + "Installed apps matching the request: " + matchingApps + "\n"
                    + "Current UI nodes (index, text, description, bounds):\n" + snapshot.description + "\n"
                    + "Recent actions: " + history + "\n"
                    + "Reply with ONLY one JSON object for the NEXT action. Allowed: "
                    + "{\"action\":\"click\",\"index\":N}, {\"action\":\"type\",\"index\":N,\"text\":\"...\"}, "
                    + "{\"action\":\"swipe_up\"}, {\"action\":\"swipe_down\"}, {\"action\":\"back\"}, "
                    + "{\"action\":\"open_app\",\"package\":\"...\"}, "
                    + "{\"action\":\"done\",\"message\":\"...\"}. "
                    + "Never claim completion without seeing evidence in the UI. If blocked, return done with the reason.";
            String response = model.generate(prompt);
            AgentStep step;
            try { step = AgentStep.parse(response); }
            catch (JSONException ex) { throw new IllegalStateException("Local model returned an invalid action: " + response, ex); }
            if (step.action.equals("done")) {
                report = step.message.isEmpty() ? "Agent stopped; verify the result on your phone."
                        : "Agent report (check the app): " + step.message;
                return;
            }
            act(step, snapshot);
            history = (history + "; " + step.action + (step.index >= 0 ? " node " + step.index : ""));
            if (history.length() > 500) history = history.substring(history.length() - 500);
            report = "Step " + (turn + 1) + "/16: " + step.action;
            Thread.sleep(900);
        }
        report = stop.get() ? "Stopped by user." : "Stopped after 16 steps; verify the result on your phone.";
    }

    private String matchingApps(String goal) {
        PackageManager packages = getPackageManager();
        String lowerGoal = goal.toLowerCase(Locale.ROOT);
        StringBuilder matches = new StringBuilder();
        int found = 0;
        for (ApplicationInfo app : packages.getInstalledApplications(0)) {
            String label = String.valueOf(app.loadLabel(packages));
            if (label.length() < 3 || !lowerGoal.contains(label.toLowerCase(Locale.ROOT))
                    || packages.getLaunchIntentForPackage(app.packageName) == null) continue;
            if (found++ > 0) matches.append(", ");
            matches.append(label).append(" (").append(app.packageName).append(")");
            if (found >= 12) break;
        }
        return found == 0 ? "none found by name" : matches.toString();
    }

    private Snapshot snapshot() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        Snapshot result = new Snapshot();
        if (root != null) collect(root, result, 0);
        return result;
    }

    private void collect(AccessibilityNodeInfo node, Snapshot output, int depth) {
        if (node == null || output.nodes.size() >= 100 || output.description.length() > 6500 || depth > 20) return;
        int index = output.nodes.size();
        output.nodes.add(node);
        Rect bounds = new Rect();
        node.getBoundsInScreen(bounds);
        String label = String.valueOf(node.getText() == null ? "" : node.getText());
        String description = String.valueOf(node.getContentDescription() == null ? "" : node.getContentDescription());
        output.description.append(index).append(" ").append(label.replace('\n', ' '))
                .append(" / ").append(description.replace('\n', ' '))
                .append(" ").append(bounds.toShortString())
                .append(node.isClickable() ? " clickable" : "")
                .append(node.isEditable() ? " editable" : "").append('\n');
        for (int i = 0; i < node.getChildCount(); i++) collect(node.getChild(i), output, depth + 1);
    }

    private void act(AgentStep step, Snapshot snapshot) {
        switch (step.action) {
            case "click": {
                AccessibilityNodeInfo node = nodeAt(snapshot, step.index);
                while (node != null && !node.isClickable()) node = node.getParent();
                if (node == null || !node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                    throw new IllegalStateException("Could not click the selected UI node");
                }
                break;
            }
            case "type": {
                AccessibilityNodeInfo node = nodeAt(snapshot, step.index);
                if (!node.isEditable()) throw new IllegalStateException("Selected UI node is not editable");
                Bundle args = new Bundle();
                args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, step.text);
                if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)) {
                    throw new IllegalStateException("Could not enter text in the selected field");
                }
                break;
            }
            case "swipe_up": swipe(true); break;
            case "swipe_down": swipe(false); break;
            case "back": performGlobalAction(GLOBAL_ACTION_BACK); break;
            case "open_app": {
                Intent intent = getPackageManager().getLaunchIntentForPackage(step.packageName);
                if (intent == null) throw new IllegalStateException("App not installed: " + step.packageName);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                break;
            }
            default: throw new IllegalStateException("Unsupported action");
        }
    }

    private AccessibilityNodeInfo nodeAt(Snapshot snapshot, int index) {
        if (index >= snapshot.nodes.size()) throw new IllegalStateException("Selected UI node is no longer available");
        return snapshot.nodes.get(index);
    }

    private void swipe(boolean up) {
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        float x = metrics.widthPixels / 2f;
        float start = up ? metrics.heightPixels * .78f : metrics.heightPixels * .26f;
        float end = up ? metrics.heightPixels * .26f : metrics.heightPixels * .78f;
        Path path = new Path(); path.moveTo(x, start); path.lineTo(x, end);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 450)).build();
        if (!dispatchGesture(gesture, null, null)) throw new IllegalStateException("Swipe was rejected");
    }

    private static final class Snapshot {
        final List<AccessibilityNodeInfo> nodes = new ArrayList<>();
        final StringBuilder description = new StringBuilder();
    }
}
