package com.daon.sdk.face.application;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.daon.sdk.face.BitmapTools;
import com.daon.sdk.face.Config;
import com.daon.sdk.face.DaonFace;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class BitmapImageActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_PERMISSIONS = 2;

    private DaonFace daonFace;
    private ImageView photo;
    private ImageView faceOne;
    private ImageView faceTwo;
    private ImageView faceThree;
    private ImageView faceFour;
    private TextView faceOneScore;
    private TextView faceTwoScore;
    private TextView faceThreeScore;
    private TextView faceFourScore;
    private TextView facesFound;

    private String currentPhotoPath;
    private View lastView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bitmap);

        try {
            daonFace = new DaonFace(this, DaonFace.OPTION_QUALITY);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Bundle config = new Bundle();
        config.putFloat(Config.QUALITY_THRESHOLD_FACE_FRONTAL, 0.45f);
        //config.putFloat(Config.QUALITY_THRESHOLD_MIN_FACE_SIZE, 0.04f);
        daonFace.setConfiguration(config);

        photo = findViewById(R.id.photoImageView);

        facesFound = findViewById(R.id.facesFoundTextView);

        faceOne = findViewById(R.id.faceOneImageView);
        faceTwo = findViewById(R.id.faceTwoImageView);
        faceThree = findViewById(R.id.faceThreeImageView);
        faceFour = findViewById(R.id.faceFourImageView);

        faceOneScore = findViewById(R.id.faceOneTextView);
        faceTwoScore = findViewById(R.id.faceTwoTextView);
        faceThreeScore = findViewById(R.id.faceThreeTextView);
        faceFourScore = findViewById(R.id.faceFourTextView);

        findViewById(R.id.oneButton).setOnClickListener(v -> analyze(v, R.mipmap.usdl));
        findViewById(R.id.twoButton).setOnClickListener(v -> analyze(v, R.mipmap.va_u21_license));
        findViewById(R.id.threeButton).setOnClickListener(v -> analyze(v, R.mipmap.va_u21_land_left_license)); // 90 to upright
        findViewById(R.id.fourButton).setOnClickListener(v -> analyze(v, R.mipmap.va_u21_land_right_license)); // 270
        findViewById(R.id.fiveButton).setOnClickListener(v -> analyze(v, R.mipmap.va_u21_down_license)); // 180
        findViewById(R.id.sixButton).setOnClickListener(v -> analyze(v, R.mipmap.jp_license));

        findViewById(R.id.cameraButton).setOnClickListener(v -> dispatchTakePictureIntent(v));

        checkPermissions();

        analyze(findViewById(R.id.oneButton), R.mipmap.usdl);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (daonFace != null)
            daonFace.stop();
    }

    public void analyze(View view, int rid) {

        reset(view);

        Bitmap bmp = BitmapFactory.decodeResource(getResources(), rid);
        if (bmp != null) {
            photo.setImageBitmap(bmp);
            analyze(bmp);
        }
    }

    public void analyze(Bitmap bmp) {
        Handler handler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            List<DaonFace.Face> faces = daonFace.analyzeFaces(bmp, DaonFace.ANALYSIS_OPTION_ALL_ORIENTATIONS);
            if (faces != null) {

                handler.post(() -> {
                    log(faces);

                    facesFound.setText(String.format(Locale.US, "Number of faces: %d", faces.size()));

                    if (faces.size() > 0) {
                        setFaceImage(faceOne, faces.get(0));
                        setFaceScore(faceOneScore, faces.get(0));
                    }

                    if (faces.size() > 1) {
                        setFaceImage(faceTwo, faces.get(1));
                        setFaceScore(faceTwoScore, faces.get(1));
                    }

                    if (faces.size() > 2) {
                        setFaceImage(faceThree, faces.get(2));
                        setFaceScore(faceThreeScore, faces.get(2));
                    }

                    if (faces.size() > 3) {
                        setFaceImage(faceFour, faces.get(3));
                        setFaceScore(faceFourScore, faces.get(3));
                    }
                });
            }
        }).start();
    }

    private void setFaceImage(ImageView view, DaonFace.Face face) {

        Bitmap bmp = face.getBitmap();

        // Get the dimensions of the View
        int targetW = view.getWidth();
        int targetH = view.getHeight();

        if (targetW > 0 && targetH > 0) {
            int photoW = bmp.getWidth();
            int photoH = bmp.getHeight();

            // Determine how much to scale down the image
            int scaleFactor = Math.max(1, Math.min(targetW / photoW, targetH / photoH));
            bmp = BitmapTools.resize(bmp, photoW * scaleFactor, photoH * scaleFactor);
        }

        view.setImageBitmap(bmp);
        view.setVisibility(View.VISIBLE);
    }

    private void setFaceScore(TextView view, DaonFace.Face face) {
        view.setText(String.format(Locale.US, "%.2f, %d", face.getScore(), face.getRotation()));
        view.setVisibility(View.VISIBLE);
    }

    private void reset(View view) {

        if (lastView != null)
            ((Button)lastView).setTextColor(Color.GRAY);

        ((Button)view).setTextColor(Color.GREEN);
        lastView = view;

        photo.setImageBitmap(null);
        facesFound.setText("");

        faceOne.setVisibility(View.GONE);
        faceTwo.setVisibility(View.GONE);
        faceThree.setVisibility(View.GONE);
        faceFour.setVisibility(View.GONE);

        faceOneScore.setVisibility(View.GONE);
        faceTwoScore.setVisibility(View.GONE);
        faceThreeScore.setVisibility(View.GONE);
        faceFourScore.setVisibility(View.GONE);
    }

//    private void setSelected(Button button) {
//        button.
//    }

    // Get the image from the camera
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = false;
            Bitmap bmp = BitmapFactory.decodeFile(currentPhotoPath, options);
            if (bmp != null) {
                setCameraPhoto();
                analyze(bmp);
            }
        }
    }

    private void log(List<DaonFace.Face> faces) {

        Log.d("DAON", "Faces: " + faces.size());

        for (DaonFace.Face face : faces) {
            Log.d("DAON", "  Size: " + face.getBitmap().getWidth() + " x " + face.getBitmap().getHeight());
            Log.d("DAON", "  Centered: " + (face.getQuality().isFaceCentered() ? "Yes" : "No"));
            Log.d("DAON", "  Score: " + face.getScore());

            String res = "";
            Bundle all = face.getQuality().getBundle();
            for (String key : all.keySet()) {
                res = res + key + " = " + all.get(key) + "\n  ";
            }
            //Log.d("DAON", "  Data: " + res);
        }
    }

    private void dispatchTakePictureIntent(View v) {

        reset(v);

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // Create the File where the photo should go
        File photoFile = createImageFile();
        if (photoFile != null) {

            if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".provider",
                        photoFile);

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            } else {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            }
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
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

    private void setCameraPhoto() {
        // Get the dimensions of the View
        int targetW = photo.getWidth();
        int targetH = photo.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = calculateInSampleSize(bmOptions, targetW, targetH);

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        photo.setImageBitmap(bitmap);
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

    // Camera permissions
    private void checkPermissions() {
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSIONS) {
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
        if (grantResults.length < 1)
            return false;

        // Verify that each required permission has been granted, otherwise return false.
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

}
