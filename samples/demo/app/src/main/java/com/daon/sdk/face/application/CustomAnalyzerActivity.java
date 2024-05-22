package com.daon.sdk.face.application;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.graphics.RectF;
import android.media.FaceDetector;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.daon.sdk.face.CameraTools;
import com.daon.sdk.face.DaonFace;
import com.daon.sdk.face.YUV;
import com.daon.sdk.face.application.camera.CameraFragment;
import com.daon.sdk.face.application.camera.CameraFragmentFactory;
import com.daon.sdk.face.license.License;
import com.daon.sdk.face.module.CustomAnalyzer;


public class CustomAnalyzerActivity
        extends AppCompatActivity
        implements CameraFragment.CameraImageCallback {

    public static class MyAnalyzer extends CustomAnalyzer {

        int orientation;

        public MyAnalyzer(Context context) {
            orientation = CameraTools.getImageOrientation(context);
        }

        @Override
        public String getName() {
            return "android face detector";
        }

        @Override
        public void analyze(YUV yuv, Bundle data, AnalyzerCallback callback) {

            // Only keep one frame in the queue, so we don't run out of memory
            startAnalyzer(yuv, data, 1, 0, callback);
        }

        @Override
        public Bundle analyze(YUV yuv, Bundle data) {

            Bundle res = new Bundle();

            RectF rect = new RectF(0, 0, 0, 0);

            Bitmap bmp = toRGB565(rotateToPortrait(yuv));

            try {
                final FaceDetector detector = new FaceDetector(bmp.getWidth(), bmp.getHeight(), 1);
                FaceDetector.Face[] detectedFaces = new FaceDetector.Face[1];

                int numberOfFaces = detector.findFaces(bmp, detectedFaces);
                if (numberOfFaces >= 1) {

                    final FaceDetector.Face face = detectedFaces[0];
                    final PointF midPoint = new PointF();
                    face.getMidPoint(midPoint);
                    final float eyesDistance = face.eyesDistance();

                    float x = midPoint.x - eyesDistance;
                    float y = midPoint.y - eyesDistance;
                    float width = 2 * eyesDistance;
                    float height = 2 * eyesDistance;

                    rect = new RectF(x, y, x + width, y + height);
                }

                bmp.recycle();

            } catch (OutOfMemoryError e) {
                Log.e("DAON", e.getLocalizedMessage());
            }

            res.putParcelable("custom.rect", rect);
            return res;
        }

        YUV rotateToPortrait(YUV yuv) {
            if (orientation == 90)
                return yuv.rotate90();
            else if (orientation == 270)
                return yuv.rotate270();
            else
                return yuv;
        }

        Bitmap toRGB565(YUV yuv) {

            byte[] yuv420sp = yuv.getData();
            int width       = yuv.getWidth();
            int height      = yuv.getHeight();

            int frameSize = width * height;
            int[] rgb = new int[width*height];


            for (int j = 0, yp = 0; j < height; j++) {
                int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
                for (int i = 0; i < width; i++, yp++) {
                    int y = (0xff & ((int) yuv420sp[yp])) - 16;
                    if (y < 0) y = 0;
                    if ((i & 1) == 0) {
                        v = (0xff & yuv420sp[uvp++]) - 128;
                        u = (0xff & yuv420sp[uvp++]) - 128;
                    }
                    int y1192 = 1192 * y;
                    int r = (y1192 + 1634 * v);
                    int g = (y1192 - 833 * v - 400 * u);
                    int b = (y1192 + 2066 * u);
                    if (r < 0) r = 0;
                    else if (r > 262143) r = 262143;
                    if (g < 0) g = 0;
                    else if (g > 262143) g = 262143;
                    if (b < 0) b = 0;
                    else if (b > 262143) b = 262143;
                    rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
                }
            }

            return Bitmap.createBitmap(rgb, width, height, Bitmap.Config.RGB_565);
        }


        @Override
        public boolean isSupported(License license) {
            return true;
        }

        @Override
        public void onImageSizeChanged(int width, int height) {

        }

        @Override
        public void onConfigurationChanged(Bundle bundle) {

        }
    }


    private TextView info;
    private DaonFace daonFace;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_custom_analyzer);

        if (null == savedInstanceState) {

            CameraFragment fragment = CameraFragmentFactory.getFragment(getApplicationContext());

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.preview, fragment)
                    .commit();
        }

        info = findViewById(R.id.info);

        try {
            daonFace = new DaonFace(this, DaonFace.OPTION_QUALITY | DaonFace.OPTION_DEVICE_POSITION);
            daonFace.addAnalyzer(new MyAnalyzer(getApplicationContext()));
        } catch (Exception e) {
            Log.e("DAON", "Error initializing DaonFace", e);
            showDialog("Error", e.getLocalizedMessage());
        }
    }

    private void showDialog(String title, String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);

        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, (dialog, which) -> finish());

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.show();
    }

    @Override
    public void onImageAvailable(YUV image) {
        if (daonFace == null)
            return;

        daonFace.analyze(image).addAnalysisListener((result, img) -> {
            Bundle all = result.getBundle();

            StringBuilder sb = new StringBuilder();

            RectF rect = getFacePosition(all);

            sb.append("Face: ")
                    .append(rect.isEmpty() ? "No" : "Yes")
                    .append("\t\tUpright: ")
                    .append(result.isDeviceUpright() ? "Yes" : "No")
                    .append("\t\tQuality: ")
                    .append(result.getQualityResult().getScore());

            info.setText(sb.toString());
        });
    }

    public RectF getFacePosition(Bundle result) {
        RectF rect = result.getParcelable("custom.rect");
        if (rect != null)
            return rect;

        return new RectF(0,0,0,0);
    }

}
