package com.daon.sdk.face.application;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.FileProvider;

import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.daon.sdk.face.BitmapTools;
import com.daon.sdk.face.DaonFace;
import com.daon.sdk.face.Result;

import java.io.File;
import java.io.IOException;


/**
 * Demonstrate single image quality.
 */
public class PhotoQualityActivity extends EdgeToEdgeActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private static final int REQUEST_PERMISSIONS = 0;

    private String currentPhotoPath;
    private TextView measuresTextView;

    private DaonFace sdk;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_photo_quality);

        measuresTextView = findViewById(R.id.measuresTextView);

        Button takePictureButton = findViewById(R.id.takePictureButton);
        takePictureButton.setOnClickListener(v -> dispatchTakePictureIntent());

        try {
            sdk = new DaonFace(this, DaonFace.OPTION_QUALITY | DaonFace.OPTION_MASK);

            checkPermissions();
        } catch (Exception e) {
            Log.e("DAON", "Error initializing DaonFace", e);
            showMessage(e.getLocalizedMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sdk != null)
            sdk.stop();
    }

    private void showMessage(String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> finish());

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    private void checkPermissions() {

        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_PERMISSIONS) {

            // We have requested multiple permissions, so all of them need
            // to be checked.

            if (verifyPermissions(grantResults)) {
                Snackbar.make(findViewById(android.R.id.content),
                        R.string.permission_granted,
                        Snackbar.LENGTH_SHORT).show();
            } else {
                Snackbar.make(findViewById(android.R.id.content),
                        R.string.permission_not_granted,
                        Snackbar.LENGTH_SHORT).show();
            }

        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean verifyPermissions(int[] grantResults) {

        if(grantResults.length < 1)
            return false;

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }


    private File createImageFile()  {
        String imageFileName = "DAON_PHOTO_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir != null) {
            try {
                if (!storageDir.exists())
                    storageDir.mkdir();

                File image = File.createTempFile(imageFileName, ".jpg", storageDir);

                // Save a file: path for use with ACTION_VIEW intents
                currentPhotoPath = image.getAbsolutePath();

                return image;

            } catch (IOException e) {
                Log.e("DAON", "Exception: " + e.getLocalizedMessage());
            }
        }
        return null;
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // Create the File where the photo should go
        File photoFile = createImageFile();
        if (photoFile != null) {

            Uri photoURI = FileProvider.getUriForFile(this,
                    BuildConfig.APPLICATION_ID + ".provider",
                    photoFile);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            try {
                setPhoto();
            } catch (IOException e) {
                Toast toast = Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG);
                toast.show();
            } catch (OutOfMemoryError e) {
                Toast toast = Toast.makeText(this, "Out of memory", Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    /**
     * Get Exif orientation from the specified JPEG file.
     *
     * @param pathToJpeg path to the JPEG file
     * @return the Exif orientation
     * @throws IOException if an I/O error occurs.
     */
    public static int getExifOrientation(String pathToJpeg) throws IOException {
        ExifInterface ei = new ExifInterface(pathToJpeg);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;

            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;

            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
        }

        return 0;
    }

    private void setPhoto() throws IOException {

        final ImageView photoImageView = findViewById(R.id.photoImageView);

        //TEST
        //Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.test2);

        // Use actual size
        //Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);
        // or resize
        Bitmap bitmap = decodeSampledBitmapFromFile(currentPhotoPath, photoImageView.getWidth(), photoImageView.getHeight());
        if (bitmap == null)
            throw new IOException("Unable to read image. Please try again.");

        // Resize and then rotate image to portrait for display
        final int orientation = getExifOrientation(currentPhotoPath);

        // Change image to portrait orientation
        bitmap = BitmapTools.rotate(bitmap, orientation);

        // It is probably mirrored
        bitmap = BitmapTools.mirror(bitmap);

        photoImageView.setImageBitmap(bitmap);

        // The SDK expects the still image to be in portrait mode with the
        // face up.
        Result measures = sdk.analyze(bitmap);
        if (measures.getQualityResult().hasData()) {

            Rect position = measures.getRecognitionResult().getFacePosition();
            if (!position.isEmpty()) {
                Bitmap crop = BitmapTools.crop(bitmap, position);
                photoImageView.setImageBitmap(crop);
            }
            measuresTextView.setText(formatMeasures(bitmap, measures));
        } else {
            measuresTextView.setText("No data");
        }
    }

    private Bitmap decodeSampledBitmapFromFile(String path,  int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }


    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private String formatMeasures(Bitmap image, Result result) {

        StringBuilder str = new StringBuilder();

        if (!result.getQualityResult().hasFace()) {
            str.append("\nFaces:        None\n");
        } else {
            str.append("\nFaces:        ").append(result.getQualityResult().hasOneFaceOnly() ? "One" : "Multiple");
            str.append("\nCentered:     ").append(result.getQualityResult().isFaceCentered() ? "Yes" : "No");
            str.append("\nFace Angle:   ").append(result.getQualityResult().hasAcceptableFaceAngle() ? "Good" : "Bad");
            str.append("\nEyes Found:   ").append(result.getQualityResult().hasEyes() ? "Yes" : "No");
            str.append("\nEyes Open:    ").append(result.getQualityResult().hasEyesOpen() ? "Yes" : "No");
            str.append("\nEye Distance: ").append(result.getQualityResult().getEyeDistance()).append("px");
        }
        str.append("\nMask:         ").append(result.getQualityResult().hasMask() ? "Yes" : "No");
        str.append("\nLighting:     ").append(result.getQualityResult().hasUniformLighting() ? "Good" : "Bad");
        str.append("\nSharpness:    ").append(result.getQualityResult().hasAcceptableSharpness() ? "Good" : "Bad");
        str.append("\nExposure:     ").append(result.getQualityResult().hasAcceptableExposure() ? "Good" : "Bad");
        str.append("\nGrayscale:    ").append(result.getQualityResult().hasAcceptableGrayscaleDensity() ? "Good" : "Bad");
        str.append("\nSize:         ").append(image.getWidth()).append("x").append(image.getHeight());
        str.append("\nScore:        ").append(result.getQualityResult().getScore());

        return str.toString();
    }


}
