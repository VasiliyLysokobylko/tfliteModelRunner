package com.onpositive.dldemos.tools;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.OpenableColumns;

import com.onpositive.dldemos.data.ContentType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {

    private static Logger logger = new Logger(Utils.class);

    public static String getFileName(Context context, Uri uri) {
        String fileName = null;
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        if (uri.getScheme().equals("content")) {
            Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    fileName = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                cursor.close();
            }
        }
        if (fileName == null) {
            fileName = uri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }
        fileName = timeStamp + "_" + fileName;
        logger.log("Selected file name: " + fileName);
        return fileName;
    }

    public static String createThumbnail(Activity activity, String sourceFilePath, ContentType ct) {
        String thumbnailPath = null;
        try {
            Bitmap preview;
            if (ct == ContentType.VIDEO) {
                Bitmap screen = ThumbnailUtils.createVideoThumbnail(sourceFilePath, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
                preview = ThumbnailUtils.extractThumbnail(screen, 240, 240);
            } else
                preview = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(sourceFilePath), 320, 320);

            File thumbnailFile = createImageFile(activity);
            OutputStream out = new FileOutputStream(thumbnailFile);
            preview.compress(Bitmap.CompressFormat.JPEG, 50, out);
            out.flush();
            out.close();
            thumbnailPath = thumbnailFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(e.getMessage());
        }
        return thumbnailPath;
    }

    public static File createImageFile(Activity activity) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        logger.log("Empty image file created");
        return image;
    }

    public static File createVideoFile(Activity activity) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "VIDEO_" + timeStamp + "_";
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_MOVIES);
        File video = File.createTempFile(
                imageFileName,
                ".mp4",
                storageDir
        );
        logger.log("Empty video file created");
        return video;
    }
}
