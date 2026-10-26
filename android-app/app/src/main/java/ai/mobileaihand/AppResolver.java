package ai.mobileaihand;

import java.util.ArrayList;
import java.util.List;

/** Resolve a human app name to one installed package before uninstalling. */
final class AppResolver {
    static final class Candidate {
        final String label;
        final String packageName;
        Candidate(String label, String packageName) {
            this.label = label;
            this.packageName = packageName;
        }
    }

    static String resolve(List<Candidate> apps, String query) {
        String wanted = query.trim();
        if (wanted.isEmpty()) throw new IllegalArgumentException("Enter an app name or package ID.");
        for (Candidate app : apps) {
            if (app.packageName.equals(wanted)) return app.packageName;
        }
        List<Candidate> matches = new ArrayList<>();
        for (Candidate app : apps) {
            if (app.label.equalsIgnoreCase(wanted)) matches.add(app);
        }
        if (matches.size() == 1) return matches.get(0).packageName;
        if (matches.isEmpty()) throw new IllegalArgumentException("No installed app matches '" + wanted + "'.");
        throw new IllegalArgumentException("Several apps have that name. Enter the exact package ID.");
    }
}
