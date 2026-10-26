package ai.mobileaihand;

import android.content.Context;
import com.google.mediapipe.tasks.genai.llminference.LlmInference;

/** Runs a user-imported MediaPipe-compatible model entirely on this phone. */
final class LocalModel implements AutoCloseable {
    private final LlmInference inference;

    LocalModel(Context context, String modelPath) {
        LlmInference.LlmInferenceOptions options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                // Gemma 3n E2B's cataloged context length includes the UI tree and response.
                .setMaxTokens(4096)
                .build();
        inference = LlmInference.createFromOptions(context, options);
    }

    String generate(String prompt) {
        return inference.generateResponse(prompt);
    }

    @Override public void close() {
        inference.close();
    }
}
