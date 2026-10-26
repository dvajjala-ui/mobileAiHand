package ai.mobileaihand;

import org.json.JSONException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AgentStepTest {
    @Test public void parsesOneSupportedAction() throws JSONException {
        AgentStep step = AgentStep.parse("```json\n{\"action\":\"type\",\"index\":4,\"text\":\"Hello\"}\n```");
        assertEquals("type", step.action);
        assertEquals(4, step.index);
        assertEquals("Hello", step.text);
    }

    @Test(expected = JSONException.class) public void rejectsUnsupportedAction() throws JSONException {
        AgentStep.parse("{\"action\":\"shell\",\"text\":\"rm -rf /\"}");
    }

    @Test(expected = JSONException.class) public void requiresNodeForClick() throws JSONException {
        AgentStep.parse("{\"action\":\"click\"}");
    }
}
