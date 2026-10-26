package ai.mobileaihand;

import org.json.JSONException;
import org.json.JSONObject;

/** One model-proposed action. Parsing is separate from execution for validation. */
final class AgentStep {
    final String action;
    final int index;
    final String text;
    final String packageName;
    final String message;

    private AgentStep(String action, int index, String text, String packageName, String message) {
        this.action = action;
        this.index = index;
        this.text = text;
        this.packageName = packageName;
        this.message = message;
    }

    static AgentStep parse(String response) throws JSONException {
        int first = response.indexOf('{');
        int last = response.lastIndexOf('}');
        if (first < 0 || last <= first) throw new JSONException("Model did not return a JSON action");
        JSONObject json = new JSONObject(response.substring(first, last + 1));
        String action = json.getString("action");
        if (!action.equals("click") && !action.equals("type") && !action.equals("swipe_up")
                && !action.equals("swipe_down") && !action.equals("back")
                && !action.equals("open_app") && !action.equals("done")) {
            throw new JSONException("Unsupported action: " + action);
        }
        int index = json.optInt("index", -1);
        if ((action.equals("click") || action.equals("type")) && index < 0) {
            throw new JSONException("Action needs a node index");
        }
        String text = json.optString("text", "");
        if (action.equals("type") && text.isEmpty()) throw new JSONException("Type action needs text");
        String packageName = json.optString("package", "");
        if (action.equals("open_app") && !packageName.matches("[a-zA-Z0-9_.]{3,180}")) {
            throw new JSONException("Open action needs a package ID");
        }
        return new AgentStep(action, index, text, packageName, json.optString("message", ""));
    }
}
