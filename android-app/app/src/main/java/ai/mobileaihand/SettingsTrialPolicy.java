package ai.mobileaihand;

import java.util.List;
import java.util.Locale;

/** A small, fail-closed action policy for the first Android Settings trial. */
final class SettingsTrialPolicy {
    static final String SETTINGS_PACKAGE = "com.android.settings";
    static final int MAX_ACTIONS = 8;

    private SettingsTrialPolicy() { }

    static boolean allows(String packageName, AgentStep step, String selectedLabel,
                          boolean selectedEditable, boolean selectedPassword,
                          List<String> visibleLabels) {
        if (!SETTINGS_PACKAGE.equals(packageName)) return false;
        switch (step.action) {
            case "done": return true;
            case "click":
                return !selectedEditable && !selectedPassword && isNavigationLabel(selectedLabel);
            case "swipe_up":
            case "swipe_down":
                for (String label : visibleLabels) {
                    if (isNavigationLabel(label)) return true;
                }
                return false;
            default: return false;
        }
    }

    static boolean isNavigationLabel(String label) {
        String normalized = normalize(label);
        return normalized.equals("about phone") || normalized.equals("about device")
                || normalized.equals("software information") || normalized.equals("android version");
    }

    static boolean versionVisible(List<String> visibleLabels, String expectedVersion) {
        String version = normalize(expectedVersion);
        if (version.isEmpty()) return false;
        for (int i = 0; i < visibleLabels.size(); i++) {
            String label = normalize(visibleLabels.get(i));
            if (label.equals("android version " + version)
                    || label.equals("android version: " + version)) return true;
            if (!label.equals("android version")) continue;
            for (int j = i + 1; j < visibleLabels.size() && j <= i + 4; j++) {
                String value = normalize(visibleLabels.get(j));
                if (value.equals(version) || value.equals("android " + version)) return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
