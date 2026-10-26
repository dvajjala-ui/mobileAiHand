package ai.mobileaihand;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.Calendar;

/** Finds images captured during today's calendar day in the phone's time zone. */
final class PhotoInventory {
    static ArrayList<Uri> today(Context context) {
        Calendar start = Calendar.getInstance();
        start.set(Calendar.HOUR_OF_DAY, 0);
        start.set(Calendar.MINUTE, 0);
        start.set(Calendar.SECOND, 0);
        start.set(Calendar.MILLISECOND, 0);
        long from = start.getTimeInMillis();
        start.add(Calendar.DAY_OF_MONTH, 1);
        long to = start.getTimeInMillis();
        ArrayList<Uri> result = new ArrayList<>();
        String[] columns = {MediaStore.Images.Media._ID};
        String selection = MediaStore.Images.Media.DATE_TAKEN + ">=? AND "
                + MediaStore.Images.Media.DATE_TAKEN + "<?";
        try (Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, columns, selection,
                new String[]{Long.toString(from), Long.toString(to)},
                MediaStore.Images.Media.DATE_TAKEN + " ASC")) {
            if (cursor != null) while (cursor.moveToNext()) {
                result.add(ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(0)));
            }
        }
        return result;
    }
}
