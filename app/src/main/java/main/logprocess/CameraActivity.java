package main.logprocess;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import objectdetection.LabelDetection;

public class CameraActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static String CAMERA_TAG = "CameraActivity";
    CascadeClassifier labelCascadeClassifier;
    JavaCameraView javaCameraView;
    private final int framePerDetect = 3;
    private int frameCount = 0;
    Mat mRgba;
    private Scalar CONTOUR_COLOR;

    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case BaseLoaderCallback.SUCCESS: {
                    initializeOpenCVDependencies();
                    break;
                }
                default: {
                    super.onManagerConnected(status);
                    break;
                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        javaCameraView = (JavaCameraView)findViewById(R.id.camera_view);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(javaCameraView != null)
            javaCameraView.disableView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (OpenCVLoader.initDebug()){
            Log.i(CAMERA_TAG, "OpenCV loaded successfully");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else {
            Log.i(CAMERA_TAG, "OpenCV not loaded");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, baseLoaderCallback);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height,width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
//        Mat mGray = inputFrame.gray();

        Mat mRgbaT = mRgba.t();
        Core.flip(mRgba.t(), mRgbaT, 1);
        Imgproc.resize(mRgbaT, mRgbaT, mRgba.size());

//        Mat mGrayT = mGray.t();
//        Core.flip(mGray.t(), mGrayT, 1);
//        Imgproc.resize(mGrayT, mGrayT, mGray.size());

        LabelDetection labelDetection = new LabelDetection(mRgbaT, labelCascadeClassifier);

        return labelDetection.detectLabel();

    }

    private void increaseFrameCount() {
        if(frameCount < framePerDetect)
            frameCount ++;
        else
            frameCount = 0;
    }

    private void initializeOpenCVDependencies() {
        final InputStream islabel;
        File labelCascadeFile = null;
        try {
            islabel = getResources().getAssets().open("1.xml");
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            labelCascadeFile = new File(cascadeDir, "1.xml");
            FileOutputStream os;
            os = new FileOutputStream(labelCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = islabel.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            islabel.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        labelCascadeClassifier = new CascadeClassifier(labelCascadeFile.getAbsolutePath());
        javaCameraView.enableFpsMeter();
        javaCameraView.enableView();
    }

    private Mat textDetection(Mat mRgba, Mat mGray){
        CONTOUR_COLOR = new Scalar(255);
        MatOfKeyPoint keypoint = new MatOfKeyPoint();
        List<KeyPoint> listpoint = new ArrayList<KeyPoint>();
        KeyPoint kpoint = new KeyPoint();
        Mat mask = Mat.zeros(mGray.size(), CvType.CV_8UC4);
        int rectanx1;
        int rectany1;
        int rectanx2;
        int rectany2;

        //
        Scalar zeos = new Scalar(0,0,0);
        List<MatOfPoint> contour1 = new ArrayList<MatOfPoint>();
        List<MatOfPoint> contour2 = new ArrayList<MatOfPoint>();
        Mat kernel = new Mat(1,50,CvType.CV_8UC4, Scalar.all(255));
        Mat morbyte = new Mat();
        Mat hierarchy = new Mat();

        Rect rectan2 = new Rect();
        Rect rectan3 = new Rect();
        int imgsize = mRgba.height() * mRgba.width();

        //

        FeatureDetector detector = FeatureDetector.create(FeatureDetector.MSER);
        detector.detect(mGray, keypoint);
        listpoint = keypoint.toList();

        for(int ind=0; ind< listpoint.size(); ind++){
            kpoint = listpoint.get(ind);
            rectanx1 = (int) (kpoint.pt.x - 0.5 * kpoint.size);
            rectany1 = (int) (kpoint.pt.y - 0.5 * kpoint.size);

            rectanx2 = (int) (kpoint.size);
            rectany2 = (int) (kpoint.size);
            if (rectanx1 <=0)
                rectanx1 = 1;
            if(rectany1 <=0)
                rectany1 = 1;
            if ((rectanx1 + rectanx2) > mGray.width())
                rectanx2 = mGray.width() - rectanx1;
            if ((rectany1 + rectany2) > mGray.height())
                rectany2 = mGray.height() - rectany1;
            Rect rectant = new Rect(rectanx1, rectany1, rectanx2, rectany2);
            Mat roi = new Mat(mask, rectant);
            roi.setTo(CONTOUR_COLOR);
        }

        Imgproc.morphologyEx(mask, morbyte, Imgproc.MORPH_DILATE, kernel);
        Imgproc.findContours(morbyte, contour2, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        for(int ind=0; ind< contour2.size(); ind++){
            rectan3 = Imgproc.boundingRect(contour2.get(ind));
            if(rectan3.area() > 0.5 * imgsize || rectan3.area() < 100 || rectan3.width / rectan3.height < 2){
                Mat roi = new Mat(morbyte, rectan3);
                roi.setTo(zeos);
            }
            else {
                Imgproc.rectangle(mRgba, rectan3.br(), rectan3.tl(), CONTOUR_COLOR);
            }
        }
        return mRgba;
    }
}
