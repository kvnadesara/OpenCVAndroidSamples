package com.example.kevinadesara.opencvsamples.object_detect;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSeekBar;
import android.support.v7.widget.AppCompatTextView;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.SeekBar;

import com.example.kevinadesara.opencvsamples.R;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.HashMap;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static org.opencv.imgproc.Imgproc.MORPH_RECT;

public class ObjectDetectActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SeekBar.OnSeekBarChangeListener {

    @BindView(R.id.java_camera_view)
    CameraBridgeViewBase mOpenCvCameraView;

    @BindView(R.id.sbHueMax)
    AppCompatSeekBar sbHueMax;
    @BindView(R.id.lblHueMaxProgress)
    AppCompatTextView lblHueMaxProgress;

    @BindView(R.id.sbHueMin)
    AppCompatSeekBar sbHueMin;
    @BindView(R.id.lblHueMinProgress)
    AppCompatTextView lblHueMinProgress;

    @BindView(R.id.sbSatMax)
    AppCompatSeekBar sbSatMax;
    @BindView(R.id.lblSatMaxProgress)
    AppCompatTextView lblSatMaxProgress;

    @BindView(R.id.sbSatMin)
    AppCompatSeekBar sbSatMin;
    @BindView(R.id.lblSatMinProgress)
    AppCompatTextView lblSatMinProgress;

    @BindView(R.id.sbValueMax)
    AppCompatSeekBar sbValueMax;
    @BindView(R.id.lblValueMaxProgress)
    AppCompatTextView lblValueMaxProgress;

    @BindView(R.id.sbValueMin)
    AppCompatSeekBar sbValueMin;
    @BindView(R.id.lblValueMinProgress)
    AppCompatTextView lblValueMinProgress;

    private boolean useMorphOps = true;

    private HashMap<AppCompatSeekBar, AppCompatTextView> seekBar2LabelMapping;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Timber.d("OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    private Mat hsv;
    private Mat threshold;
    private Mat cameraFeed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_object_detect);
        ButterKnife.bind(this);
        setupControls();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 999);
        }
    }

    private void setupControls() {
        sbHueMax.setOnSeekBarChangeListener(this);
        sbHueMin.setOnSeekBarChangeListener(this);
        sbSatMax.setOnSeekBarChangeListener(this);
        sbSatMin.setOnSeekBarChangeListener(this);
        sbValueMax.setOnSeekBarChangeListener(this);
        sbValueMin.setOnSeekBarChangeListener(this);

        seekBar2LabelMapping = new HashMap<AppCompatSeekBar, AppCompatTextView>() {{
            put(sbHueMax, lblHueMaxProgress);
            put(sbHueMin, lblHueMinProgress);
            put(sbSatMax, lblSatMaxProgress);
            put(sbSatMin, lblSatMinProgress);
            put(sbValueMax, lblValueMaxProgress);
            put(sbValueMin, lblValueMinProgress);
        }};

        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 999) {
            if (grantResults.length == 1) {
                loadOpenCV();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadOpenCV();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private void loadOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            Timber.d("Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback);
        } else {
            Timber.d("OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onCameraViewStarted(int width, int height) {
        hsv = new Mat(height, width, CvType.CV_8UC4);
        threshold = new Mat(height, width, CvType.CV_8UC4);
        cameraFeed = new Mat(height, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {
        hsv.release();
        threshold.release();
        cameraFeed.release();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    private void morphOps(Mat thresh) {
        //create structuring element that will be used to "dilate" and "erode" image.
        //the element chosen here is a 3px by 3px rectangle

        Mat erodeElement = Imgproc.getStructuringElement(MORPH_RECT, new Size(2, 2));
        //dilate with larger element so make sure object is nicely visible
        Mat dilateElement = Imgproc.getStructuringElement(MORPH_RECT, new Size(8, 8));

        Imgproc.erode(thresh, thresh, erodeElement);
        Imgproc.erode(thresh, thresh, erodeElement);

        Imgproc.dilate(thresh, thresh, dilateElement);
        Imgproc.dilate(thresh, thresh, dilateElement);
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //convert frame from RGBA to BGR colorspace
        Imgproc.cvtColor(inputFrame.rgba(), cameraFeed, Imgproc.COLOR_RGBA2BGR);

        //convert frame from BGR to HSV colorspace
        Imgproc.cvtColor(cameraFeed, hsv, Imgproc.COLOR_BGR2HSV);

        //filter HSV image between values and store filtered image to
        //threshold matrix
        Core.inRange(hsv, new Scalar(sbHueMin.getProgress(), sbSatMin.getProgress(), sbValueMin.getProgress()),
                new Scalar(sbHueMax.getProgress(), sbSatMax.getProgress(), sbValueMax.getProgress()), threshold);

        //perform morphological operations on thresholded image to eliminate noise
        //and emphasize the filtered object(s)
        if (useMorphOps) {
            morphOps(threshold);
        }
        return threshold;
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        AppCompatTextView lblProgress = seekBar2LabelMapping.get(seekBar);
        lblProgress.setText(String.format(Locale.US, "%03d / %03d", progress, seekBar.getMax()));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}
}
