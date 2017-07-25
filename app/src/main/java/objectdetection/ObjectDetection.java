package objectdetection;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.ellipse;
import static org.opencv.imgproc.Imgproc.equalizeHist;
import static org.opencv.imgproc.Imgproc.filter2D;
import static org.opencv.imgproc.Imgproc.rectangle;
import static org.opencv.imgproc.Imgproc.resize;

/**
 * Created by hnguyen on 7/13/2017.
 */

public class ObjectDetection {
    private final int DISPLAY_SCALE_FACTOR = 1;
    private final int SCREEN_PROCESS_THRESHOLD = 800;
    private final double LOG_IMAGE_PERCENT = 0.8;

    private String LOG_CASCADE_NAME = "";
    private String LABEL_CASCADE_NAME = "";

    private final Scalar LOG_LINE_COLOR = new Scalar(255,0,255);
    private final Scalar LABEL_LINE_COLOR = new Scalar(0, 255, 0);

    private final Size LABEL_MIN_SIZE = new Size(40, 40);
    private final Size LABEL_MAX_SIZE = new Size(80, 80);
    private final Size LOG_MIN_SIZE = new Size(40, 40);
    private final Size LOG_MAX_SIZE = new Size(9999, 9999);

    private final int LOG_LINE_THIN = 4;
    private final int LABEL_LINE_THIN = 3;
    private final Mat kernel = new Mat(3,3, CvType.CV_32F){
        {
            put(0,0,0);
            put(0,1,-1);
            put(0,2,0);

            put(1,0,-1);
            put(1,1,5);
            put(1,2,-1);

            put(2,0,0);
            put(2,1,-1);
            put(2,2,0);
        }
    };
    private CascadeClassifier log_cascade;
    private CascadeClassifier label_cascade;

    private Mat srcImage = new Mat();
    //
    private boolean canDetect = true;
    private final int CV_HAAR_SCALE_IMAGE = 2;
    public ObjectDetection(Bitmap image, String LOG_CASCADE_NAME, String LABEL_CASCADE_NAME){
        this.LOG_CASCADE_NAME = LOG_CASCADE_NAME;
        this.LABEL_CASCADE_NAME = LABEL_CASCADE_NAME;

        log_cascade = new CascadeClassifier(LOG_CASCADE_NAME);
        label_cascade = new CascadeClassifier(LABEL_CASCADE_NAME);

        Bitmap bmp32 = image.copy(Bitmap.Config.ARGB_8888, true);
        Utils.bitmapToMat(bmp32, srcImage);
        if(!log_cascade.load(LOG_CASCADE_NAME)){
            canDetect &= false;
        }

        if(!label_cascade.load(LABEL_CASCADE_NAME)){
            canDetect &= false;
        }
    }

    public Bitmap detectObject(){
        if(canDetect) {
            Mat resultMap = DetectAndDisplayLog(srcImage);
            Bitmap bitmap = Bitmap.createBitmap(resultMap.cols(), resultMap.rows(), Bitmap.Config.ARGB_8888);
            Utils.matToBitmap(resultMap, bitmap);
            return bitmap;
        }
        else return null;
    }


    private Mat DetectAndDisplayLog(Mat frame){
        MatOfRect logs = new MatOfRect();
        Mat frame_gray = new Mat();
        Mat frame_resize = new Mat();
        Mat display_frame = new Mat();

        int w = frame.cols();
        int h = frame.rows();
        int scale_factor = 1;

        resize(frame, display_frame, new Size(w / DISPLAY_SCALE_FACTOR, h / DISPLAY_SCALE_FACTOR));
        while (h > SCREEN_PROCESS_THRESHOLD || w > SCREEN_PROCESS_THRESHOLD) {
            h /= 2;
            w /= 2;
            scale_factor *= 2;
        }

        resize(frame, frame_resize, new Size(w, h));
        cvtColor(frame_resize, frame_gray, COLOR_BGR2GRAY);
        //blur(frame_gray, frame_gray, Size(10,10));

        equalizeHist(frame_gray, frame_gray);
        log_cascade.detectMultiScale(
                frame_gray,
                logs,
                1.08,
                4,
                0 | CV_HAAR_SCALE_IMAGE,
                LOG_MIN_SIZE,
                LOG_MAX_SIZE
        );


        for(Rect rect: logs.toArray()){
            Point center = new Point(
                    (rect.x + rect.width*0.5)*scale_factor / DISPLAY_SCALE_FACTOR,
                    (rect.y + rect.height*0.5)*scale_factor / DISPLAY_SCALE_FACTOR
            );

            ellipse(
                    display_frame,
                    center,
                    new Size(
                            rect.width*0.5*scale_factor / DISPLAY_SCALE_FACTOR,
                            rect.height*0.5*scale_factor / DISPLAY_SCALE_FACTOR
                    ),
                    0,
                    0,
                    360,
                    LOG_LINE_COLOR,
                    LOG_LINE_THIN,
                    8,
                    0
            );

            double x0 = rect.x * scale_factor + rect.width * scale_factor * ( 1 - LOG_IMAGE_PERCENT) / 2;
            double y0 = rect.y * scale_factor + rect.height * scale_factor * ( 1 - LOG_IMAGE_PERCENT ) / 2;
            double width = rect.width * scale_factor * LOG_IMAGE_PERCENT;
            double height = rect.height * scale_factor * LOG_IMAGE_PERCENT;

            Rect window = new Rect((int)x0, (int)y0, (int)width, (int)height);
            Mat log_frame = frame.submat(window).clone();
            DetectAndDisplayLabel(log_frame,x0, y0, display_frame, DISPLAY_SCALE_FACTOR);
        }

        return display_frame;
    }

    private void DetectAndDisplayLabel(Mat frame, double x0, double y0, Mat frame_resize, int scale_factor){
        MatOfRect labels = new MatOfRect();
        Mat frame_gray = new Mat();
        Mat sharpened = new Mat();

        int w = frame.cols();
        int h = frame.rows();

        cvtColor(frame, frame_gray, COLOR_BGR2GRAY);
        filter2D(frame_gray, sharpened, frame_gray.depth(), kernel);

        label_cascade.detectMultiScale(
                sharpened,
                labels,
                1.01,
                15,
                0 | CV_HAAR_SCALE_IMAGE,
                LABEL_MIN_SIZE,
                LABEL_MAX_SIZE
        );

        for(Rect rect: labels.toArray()){
            double x = (rect.x + x0)/scale_factor;
            double y = (rect.y + y0)/scale_factor;
            double width = rect.width / scale_factor;
            double height = rect.height / scale_factor;
            Rect window = new Rect((int)x, (int)y, (int)width, (int)height);
            rectangle(
                    frame_resize,
                    window.tl(),
                    window.br(),
                    LABEL_LINE_COLOR,
                    LABEL_LINE_THIN,
                    8,
                    0
            );
        }
    }


}
