package main.logprocess;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.SizeF;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Date;

import objectdetection.ObjectDetection;


public class MainActivity extends AppCompatActivity {
    static{
        System.loadLibrary("opencv_java3");
    }
    private static final String TAG = "MainActivity";
    private static final int CAMERA_REQUEST = 1888;
    private ObjectDetection logDetection;
    private boolean isLoadImage = false;
    Button AttachImageBtn;
    Button ProcessBtn;
    Button GalleryBtn;
    ImageView imageView;
    Uri imagePassed;
    //

    private static final int REQUEST_CODE_WRITE_EXTERNAL_SOURCE = 1;
    private static final int REQUEST_CODE_READ_EXTERNAL_SOURCE = 2;
    private static final int REQUEST_CODE_INTERNET = 3;
    private static final int REQUEST_CODE_ACCESS_NETWORK_STATE = 4;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AttachImageBtn = (Button) findViewById(R.id.button);
        ProcessBtn = (Button) findViewById(R.id.procBtn);
        GalleryBtn = (Button) findViewById(R.id.galleryBtn);
        imageView = (ImageView)findViewById(R.id.imageView);
        CheckPermission();
        addListenerForAll();
    }

    private void CheckPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_WRITE_EXTERNAL_SOURCE);
        }

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_CODE_READ_EXTERNAL_SOURCE);
        }

//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.INTERNET)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.INTERNET},
//                    REQUEST_CODE_INTERNET);
//        }
//
//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.ACCESS_NETWORK_STATE)
//                != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this,
//                    new String[]{Manifest.permission.ACCESS_NETWORK_STATE},
//                    REQUEST_CODE_ACCESS_NETWORK_STATE);
//        }
    }

    private void SaveImageCaptured(Bitmap imgBitmap) {
        String fileName = "IMG_" + DateFormat.format( "yyyyMMdd_HHmmss" ,new java.util.Date()).toString() + ".png";
        File sdCardDirectory = Environment.getExternalStorageDirectory();
        File image = new File(sdCardDirectory, fileName);
        // Encode the file as a PNG image.
        FileOutputStream outStream;
        try {

            outStream = new FileOutputStream(image);
            imgBitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.flush();
            outStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK)  {
            final Uri imageUri = data.getData();
            final InputStream imageStream;
            try {
                imageStream = getContentResolver().openInputStream(imageUri);
                final Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
                imageView.setImageBitmap(selectedImage);
                isLoadImage = true;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        else if(requestCode == CAMERA_REQUEST && resultCode == RESULT_OK) {
            Uri imageUri = data.getData();
            Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
            //imageView.setImageBitmap(imageBitmap);
            //save image to SD card
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                SaveImageCaptured(imageBitmap);
            }
            //
            imagePassed = imageUri;
            Glide.with(getApplicationContext())
                    .load(imageUri)
                    .into(imageView);
            isLoadImage = false;
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.PNG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    private void addListenerForAll() {
        AttachImageBtn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //load static image
                        //loadImage();

                        //load Image after capturing by Camera
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(cameraIntent, CAMERA_REQUEST);
                    }
                }
        );

        ProcessBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //test();
                onDetectClicked();
            }
        });

        GalleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //load Image from Gallery
                Intent getIntent = new Intent(Intent.ACTION_GET_CONTENT);
                getIntent.setType("image/*");

                Intent pickIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                pickIntent.setType("image/*");

                Intent chooserIntent = Intent.createChooser(getIntent, "Select Image");
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{pickIntent});
                startActivityForResult(chooserIntent, 1);
            }
        });

        if (ContextCompat.checkSelfPermission(this,Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            GalleryBtn.setEnabled(true);
        }
    }


    private void onDetectClicked() {
        if(!isLoadImage) {
            Intent intent = new Intent(this, ViewGalleryActivity.class);
            intent.putExtra("imageUri", imagePassed.toString());
            startActivity(intent);
        } else {
            OnDetectImageLoaded();
        }
    }

    private void OnDetectImageLoaded() {
        Bitmap imgBitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
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

        LogDetectTask logDetectTask = new LogDetectTask(imgBitmap);
        logDetectTask.execute(logCascadeFile.getAbsolutePath(),labelCascadeFile.getAbsolutePath());
    }

    private void loadImage() {
        try {
            // get input stream
            InputStream ims = getAssets().open("6.jpg");
            // load image as Drawable
            Drawable d = Drawable.createFromStream(ims, null);
            // set image to ImageView
            imageView.setImageDrawable(d);
        }
        catch(IOException ex) {
            return;
        }
    }

    private double distanceFromRefObjToCameraBasedHardware(double refObjWidthPixels, double realObjWidth){
        double focalLength = 0;
        double sensorWidth = 0;
        double imageWidth = 0;

        try {
            CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String[] cameraIdList = new String[0];

            cameraIdList = manager.getCameraIdList();
            CameraCharacteristics cameraCharacteristics = manager.getCameraCharacteristics(cameraIdList[1]);

            SizeF sizeF = cameraCharacteristics.get(
                    CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);

            float[] focals = cameraCharacteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);

            focalLength = focals[0];
            sensorWidth = sizeF.getWidth();

            Bitmap imageBitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
            imageWidth = imageBitmap.getWidth();
//            Log.d(TAG, "onCreate: height: " + sizeF.getHeight() + " width: " + sizeF.getWidth() + "focals : " + focals[0]);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        double result =(focalLength * realObjWidth * imageWidth) / (refObjWidthPixels * sensorWidth);
        return result;
    }

    private double perceivedFocalLength(double pixelsWidth, double distance, double realWidth){

        return (pixelsWidth * distance) / realWidth;
    }

    private double apparentWidthPixels(double perceivedFocalLength, double realWidth, double distanceToCamera){
        return (perceivedFocalLength * realWidth) / distanceToCamera;
    }

    private void test() {

        //All in millimeter

        //temporary value

        double refObjWidthPixels = 62; //Use opencv to detect
        double realReferenceObjWidth = 57; //Get from user input
        double distanceFromTargetObjectToCamera = 310; //Get from user input

        double targetObjectXInPixels = 290; //from image => target object pixels in x axis
        double targetObjectYInPixels = 290; //from image => target object pixels in y axis

        //end temporary value

        double referenceObjectToCamera = distanceFromRefObjToCameraBasedHardware(refObjWidthPixels, realReferenceObjWidth);
        double pFocalLength = perceivedFocalLength(refObjWidthPixels, referenceObjectToCamera, realReferenceObjWidth);
        double refObjPixelsSameDepthWithRealObj =  apparentWidthPixels(pFocalLength, realReferenceObjWidth, distanceFromTargetObjectToCamera);
        double pixelsPerMillimeter = refObjPixelsSameDepthWithRealObj / realReferenceObjWidth;

        double targetObjectX = targetObjectXInPixels / pixelsPerMillimeter;
        double targetObjectY = targetObjectYInPixels / pixelsPerMillimeter;

        double targetObjectArea = Math.PI * targetObjectX * targetObjectY;
        double targetObjectPerimeter = Math.PI *
                (3 * (targetObjectX + targetObjectY)
                        - Math.sqrt(
                                (3 * targetObjectX + targetObjectY) * (targetObjectX + 3 * targetObjectY)
                        )
                );
    }

    private class LogDetectTask extends AsyncTask<String, Void, Bitmap>{
        private Bitmap imgSrc;
        public LogDetectTask(Bitmap imgSrc){
            this.imgSrc = imgSrc;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProcessBtn.setEnabled(false);
            Toast.makeText(getBaseContext(),"Detecting Log!!!",Toast.LENGTH_LONG).show();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String logCascadePath = params[0];
            String labelCascadePath = params[1];

            logDetection = new ObjectDetection(this.imgSrc,logCascadePath, labelCascadePath);

            return logDetection.detectObject();
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            imageView.setImageBitmap(bitmap);
            ProcessBtn.setEnabled(true);
            Toast.makeText(getBaseContext(),"Done Detection", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_CODE_READ_EXTERNAL_SOURCE:
            case REQUEST_CODE_WRITE_EXTERNAL_SOURCE: {
                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED){
                    GalleryBtn.setEnabled(false);
                }
                else {
                    GalleryBtn.setEnabled(true);
                }
            }
        }
    }
}
