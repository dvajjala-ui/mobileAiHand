package ai.mobileaihand;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ModelFilePromotionTest {
    @Rule public TemporaryFolder files = new TemporaryFolder();

    private static void write(File file, String contents) throws Exception {
        Files.write(file.toPath(), contents.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    @Test public void incompatibleCandidateCannotReplacePreviousModel() throws Exception {
        File previous = files.newFile("local-model.task");
        File candidate = files.newFile("candidate.task");
        write(previous, "working model");
        write(candidate, "incompatible model");

        assertThrows(IllegalArgumentException.class, () ->
                ModelFilePromotion.replaceAfterValidation(candidate, previous, file -> {
                    throw new IllegalArgumentException("cannot load");
                }));

        assertEquals("working model", read(previous));
        assertTrue(candidate.exists());
    }

    @Test public void validatedCandidateReplacesPreviousModel() throws Exception {
        File previous = files.newFile("local-model.task");
        File candidate = files.newFile("candidate.task");
        write(previous, "old model");
        write(candidate, "working replacement");

        ModelFilePromotion.replaceAfterValidation(candidate, previous, file ->
                assertEquals("working replacement", read(file)));

        assertEquals("working replacement", read(previous));
        assertFalse(candidate.exists());
    }

    @Test public void emptyCandidateIsRejectedBeforeValidation() throws Exception {
        File previous = files.newFile("local-model.task");
        File candidate = files.newFile("candidate.task");
        write(previous, "working model");

        assertThrows(java.io.IOException.class, () ->
                ModelFilePromotion.replaceAfterValidation(candidate, previous, file -> {
                    throw new AssertionError("validation should not run");
                }));

        assertEquals("working model", read(previous));
    }
}
