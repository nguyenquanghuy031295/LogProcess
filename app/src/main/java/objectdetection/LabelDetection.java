package objectdetection;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.equalizeHist;
import static org.opencv.imgproc.Imgproc.filter2D;
import static org.opencv.imgproc.Imgproc.rectangle;
import static org.opencv.imgproc.Imgproc.resize;

/**
 * Created by hnguyen on 8/23/2017.
 */

public class LabelDetection {
    private final int DISPLAY_SCALE_FACTOR = 1;
    private final int SCREEN_PROCESS_THRESHOLD = 800;

    private final Scalar LABEL_LINE_COLOR = new Scalar(0, 255, 0);

    private final Size LABEL_MIN_SIZE = new Size(40, 40);
    private final Size LABEL_MAX_SIZE = new Size(80, 80);

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
    private CascadeClassifier label_cascade;

    private Mat srcMat = new Mat();
    private boolean canDetect = true;
    private final int CV_HAAR_SCALE_IMAGE = 2;

    public LabelDetection(Mat srcMat, CascadeClassifier label_cascade){
        this.label_cascade = label_cascade;

        this.srcMat = srcMat;

        if(label_cascade == null){
            canDetect &= false;
        }
    }

    public Mat detectLabel() {
        if(canDetect){
            Mat result = DetectAndDisplayLabel(srcMat);
            return result;
        }
        else return null;
    }

    private Mat DetectAndDisplayLabel(Mat frame){
        MatOfRect labels = new MatOfRect();
        Mat frame_gray = new Mat();
        Mat frame_resize = new Mat();
        Mat display_frame = new Mat();
        Mat sharpened = new Mat();

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
        filter2D(frame_gray, sharpened, frame_gray.depth(), kernel);

        label_cascade.detectMultiScale(
                sharpened,
                labels,
                1.2,
                30,
                0 | CV_HAAR_SCALE_IMAGE,
                LABEL_MIN_SIZE,
                LABEL_MAX_SIZE
        );

        for(Rect rect: labels.toArray()){
            double x = (rect.x)/scale_factor;
            double y = (rect.y)/scale_factor;
            double width = rect.width / scale_factor;
            double height = rect.height / scale_factor;
            Rect window = new Rect((int)x, (int)y, (int)width, (int)height);
            rectangle(
                    display_frame,
                    window.tl(),
                    window.br(),
                    LABEL_LINE_COLOR,
                    LABEL_LINE_THIN,
                    8,
                    0
            );
        }

        return display_frame;
    }
}
