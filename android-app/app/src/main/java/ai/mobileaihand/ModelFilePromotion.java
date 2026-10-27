package ai.mobileaihand;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** Promotes a copied model only after it has loaded successfully. */
final class ModelFilePromotion {
    interface Validator {
        void validate(File candidate) throws Exception;
    }

    private ModelFilePromotion() { }

    static void replaceAfterValidation(File candidate, File destination,
                                       Validator validator) throws Exception {
        if (!candidate.isFile() || candidate.length() == 0) {
            throw new IOException("Model copy is empty or missing");
        }
        validator.validate(candidate);
        try {
            Files.move(candidate.toPath(), destination.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(candidate.toPath(), destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
