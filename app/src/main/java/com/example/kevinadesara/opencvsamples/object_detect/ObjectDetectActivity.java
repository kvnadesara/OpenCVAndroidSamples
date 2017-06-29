package com.example.kevinadesara.opencvsamples.object_detect;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.kevinadesara.opencvsamples.R;

import org.opencv.android.CameraBridgeViewBase;

import butterknife.BindView;

public class ObjectDetectActivity extends AppCompatActivity {

    @BindView(R.id.java_camera_view)
    CameraBridgeViewBase mOpencvCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_object_detect);
    }
}
