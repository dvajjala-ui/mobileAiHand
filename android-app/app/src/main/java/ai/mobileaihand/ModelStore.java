package ai.mobileaihand;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/** One app-scoped model import; Activity recreation cannot start a second copy. */
final class ModelStore {
    private static final long RESERVE_BYTES = 256L * 1024 * 1024;
    private static final ExecutorService worker = Executors.newSingleThreadExecutor();
    private static final AtomicBoolean importing = new AtomicBoolean(false);
    private static volatile String status = "No model import has run yet.";

    private ModelStore() { }

    static File modelFile(Context context) { return new File(context.getFilesDir(), "local-model.task"); }
    static boolean isImporting() { return importing.get(); }
    static String status() { return status; }

    static boolean importModel(Context context, Uri uri) {
        if (!importing.compareAndSet(false, true)) return false;
        Context app = context.getApplicationContext();
        status = "Copying model into private app storage…";
        worker.execute(() -> copyModel(app, uri));
        return true;
    }

    private static void copyModel(Context context, Uri uri) {
        File destination = modelFile(context);
        File temporary = new File(context.getFilesDir(),
                "local-model-" + UUID.randomUUID() + ".task.part");
        try {
            long size = selectedSize(context, uri);
            long free = context.getFilesDir().getUsableSpace();
            if (size > 0 && (free <= RESERVE_BYTES || size > free - RESERVE_BYTES)) {
                status = "Not enough free space to copy the model. Keep the previous model or free storage.";
                return;
            }
            try (InputStream source = context.getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(temporary)) {
                if (source == null) throw new IllegalStateException("Cannot open model");
                byte[] buffer = new byte[1024 * 1024];
                int count;
                while ((count = source.read(buffer)) != -1) output.write(buffer, 0, count);
                output.getFD().sync();
            }
            if (size > 0 && temporary.length() != size) {
                throw new IllegalStateException("Incomplete model copy");
            }
            try {
                Files.move(temporary.toPath(), destination.toPath(),
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            status = "Model copied. Compatibility will be checked by the Settings trial.";
        } catch (Exception ex) {
            status = "Model import failed. The previous model, if any, is still available.";
        } finally {
            temporary.delete();
            importing.set(false);
        }
    }

    private static long selectedSize(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[]{OpenableColumns.SIZE}, null, null, null)) {
            return cursor != null && cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getLong(0) : -1;
        } catch (Exception ex) {
            return -1;
        }
    }
}
