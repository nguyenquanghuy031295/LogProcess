package main.logprocess;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import objectdetection.ObjectDetection;

import static main.logprocess.R.id.imageView;
import static main.logprocess.ViewGalleryActivity.scaleDown;

public class ViewGalleryActivity extends AppCompatActivity {
    private Bitmap[] mImageBm = new Bitmap[4];
    ImageView imageView;
    Bitmap imageSrc;
    Gallery gallery;
    EditText resolutionText;
    private GalleryAdapter galleryAdapter;
    private float[] ratios = {
            0.9f,
            0.7f,
            0.2f
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        GetImagePassed();
        setContentView(R.layout.activity_view_gallery);
        gallery = (Gallery) findViewById(R.id.gallery);
        imageView = (ImageView) findViewById(R.id.imageViewGallery);
        resolutionText = (EditText) findViewById(R.id.resolution);
        OnDetect();
        gallery.setSpacing(1);

        addListenerForComponent();
    }

    private void GetImagePassed() {
        Uri imageUri = Uri.parse(getIntent().getExtras().getString("imageUri"));
        final InputStream imageStream;
        try {
            imageStream = getContentResolver().openInputStream(imageUri);
            final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            imageSrc = selectedImage;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void OnDetect() {
        final InputStream islog;
        final InputStream islabel;
        File logCascadeFile = null;
        File labelCascadeFile = null;
        try {
            islog = getResources().getAssets().open("log_cascade_5.xml");
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            logCascadeFile = new File(cascadeDir, "log_cascade_5.xml");
            FileOutputStream os;
            os = new FileOutputStream(logCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = islog.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            islog.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            islabel = getResources().getAssets().open("label_cascade_12x12_2_1.xml");
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            labelCascadeFile = new File(cascadeDir, "label_cascade_12x12_2_1.xml");
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

        LogDetectTask logDetectTask = new LogDetectTask(this, ratios);
        logDetectTask.execute(logCascadeFile.getAbsolutePath(),labelCascadeFile.getAbsolutePath());
    }

    private void addListenerForComponent(){
        gallery.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Bitmap bm = galleryAdapter.mImageBm[i];
                imageView.setImageBitmap(bm);
                String resoText = bm.getWidth() + "x" + bm.getHeight();
                resolutionText.setText(resoText);
            }
        });
    }

    public static Bitmap scaleDown(Bitmap realImage, float ratio, boolean filter) {
        int width = Math.round((float) ratio * realImage.getWidth());
        int height = Math.round((float) ratio * realImage.getHeight());

        Bitmap newBitmap = Bitmap.createScaledBitmap(realImage, width,
                height, filter);
        return newBitmap;
    }

    class LogDetectTask extends AsyncTask<String, Void, Bitmap[]> {
        private float[] ratios;
        private Context context;
        public LogDetectTask(Context context, float[] ratios) {
            this.ratios = ratios;
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getBaseContext(),"Detecting Log!!!",Toast.LENGTH_LONG).show();
        }

        @Override
        protected Bitmap[] doInBackground(String... params) {
            Bitmap[] result = new Bitmap[4];
            String logCascadePath = params[0];
            String labelCascadePath = params[1];
            ObjectDetection logDetetion;
            logDetetion = new ObjectDetection(imageSrc, logCascadePath, labelCascadePath);
            result[0] = logDetetion.detectObject();
            for (int i = 0; i < ratios.length; i++) {
                float ratio = ratios[i];
                Bitmap newImageBm = scaleDown(imageSrc, ratio, true);
                logDetetion = new ObjectDetection(newImageBm, logCascadePath, labelCascadePath);
                result[i + 1] = logDetetion.detectObject();
            }
            return result;
        }

        @Override
        protected void onPostExecute(Bitmap[] bitmaps) {
            super.onPostExecute(bitmaps);
            Toast.makeText(getBaseContext(),"Done Detection", Toast.LENGTH_SHORT).show();
            mImageBm = bitmaps;

            galleryAdapter = new GalleryAdapter(context, mImageBm);
            gallery.setAdapter(galleryAdapter);
        }
    }
}

class GalleryAdapter extends BaseAdapter {
    private Context mContext;
    public Bitmap[] mImageBm;
    public GalleryAdapter(Context context, Bitmap[] imageBm){
        this.mContext = context;
        this.mImageBm = imageBm;
    }
    @Override
    public int getCount() {
        return mImageBm.length;
    }

    @Override
    public Object getItem(int i) {
        return mImageBm[i];
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ImageView imageView = new ImageView(mContext);

        imageView.setImageBitmap(mImageBm[i]);
        imageView.setLayoutParams(new Gallery.LayoutParams(200, 200));

        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        return imageView;
    }
}
