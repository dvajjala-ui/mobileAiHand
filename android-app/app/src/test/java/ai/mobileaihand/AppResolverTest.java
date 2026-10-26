package ai.mobileaihand;

import org.junit.Test;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class AppResolverTest {
    @Test public void resolvesUniqueLabel() {
        assertEquals("com.example.notes", AppResolver.resolve(Arrays.asList(
                new AppResolver.Candidate("Notes", "com.example.notes"),
                new AppResolver.Candidate("Camera", "com.example.camera")), "notes"));
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsAmbiguousName() {
        AppResolver.resolve(Arrays.asList(
                new AppResolver.Candidate("Notes", "com.one.notes"),
                new AppResolver.Candidate("Notes", "com.two.notes")), "Notes");
    }

    @Test public void exactPackageWins() {
        assertEquals("com.two.notes", AppResolver.resolve(Arrays.asList(
                new AppResolver.Candidate("Notes", "com.one.notes"),
                new AppResolver.Candidate("Notes", "com.two.notes")), "com.two.notes"));
    }
}
