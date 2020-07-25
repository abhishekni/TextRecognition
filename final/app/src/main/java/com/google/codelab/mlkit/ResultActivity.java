package com.google.codelab.mlkit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResultActivity extends AppCompatActivity {

    EditText mResultEt;
    ImageView mPreviewIv;
    private GraphicOverlay mGraphicOverlay;
    String[] cameraPermission;
    Uri imageUri;
    //    private int [] inputRect = {5, 445, 350, 640};
//    private int [] inputRect = {5, 400, 350, 640};
//    private int [] inputRect = {627, 959, 59, 164};
    private int [] inputRect = {1704, 2608, 184, 367};

    private static final int CAMERA_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_CAMERA_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        mPreviewIv = findViewById(R.id.imageIv);
        mResultEt = findViewById(R.id.resultEt);
        mGraphicOverlay = findViewById(R.id.graphic_overlay);
        cameraPermission = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.addImage) {
            if (!checkCameraPermission()) {
                requestCameraPermission();
            } else {
                pickCamera();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case CAMERA_REQUEST_CODE:
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (cameraAccepted)
                        pickCamera();
                    else
                        showToast("Permission Denied for Camera");

                }
                break;
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_CAMERA_CODE) {
                CropImage.activity(imageUri).setGuidelines(CropImageView.Guidelines.ON).start(this);
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                mPreviewIv.setImageURI(resultUri);

                BitmapDrawable bitmapDrawable = (BitmapDrawable) mPreviewIv.getDrawable();
                Bitmap bitmap = bitmapDrawable.getBitmap();
                runTextRecognition(bitmap);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception exception = result.getError();
                showToast(exception + "");
            }
        }
    }

    private void runTextRecognition(Bitmap mSelectedImage) {
        InputImage image = InputImage.fromBitmap(mSelectedImage, 0);
        com.google.mlkit.vision.text.TextRecognizer recognizer = TextRecognition.getClient();
        recognizer.process(image)
                .addOnSuccessListener(
                        new OnSuccessListener<Text>() {
                            @Override
                            public void onSuccess(Text texts) {
                                String text = processTextRecognitionResult(texts);
                                mResultEt.setText(text);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                            }
                        });
    }

    private String processTextRecognitionResult(Text texts) {
        List<Text.TextBlock> blocks = texts.getTextBlocks();
        StringBuilder sb = new StringBuilder();
        if (blocks.size() == 0) {
            showToast("No text found");
            return "";
        }
        mGraphicOverlay.clear();
        GraphicOverlay.Graphic textGraphic = new TextGraphic(mGraphicOverlay, inputRect);
        mGraphicOverlay.add(textGraphic);
        for (int index = 0; index < blocks.size(); index++) {
            CalculateText calculateText = new CalculateText(blocks.get(index), inputRect[0], inputRect[1], inputRect[2], inputRect[3]);
            if (calculateText.doRectOverlap()) {
                // These variable will store the horizontal & vertical intersected segment along with the starting point
                float horOverlap = calculateText.calHorizontalOverlap();
                float verOverlap = calculateText.calVerticalOverlap();
                float startX = calculateText.getStartX();
                float startY = calculateText.getStartY();

                List<Text.Line> lines = blocks.get(index).getLines();

                int yCount = lines.size();
                int start = (int)Math.round((yCount*startY)/100);
                int end = (int)Math.round((yCount*(startY + verOverlap))/100);
                int length = 0 ;

                // This condition will calculate the index of longest string, which lies inside the desired Rectangular area
                for (int noOfRows = start; noOfRows < end; noOfRows++) {
                    int len = lines.get(noOfRows).getText().length();
                    if (len >  length) length  = len;
                }
                // This loop will process all the rows one by one which lies inside the desired Rectangular area
                for (int noOfRows = start; noOfRows < end; noOfRows++) {
                    String text     = lines.get(noOfRows).getText();
                    int len         = text.length();
                    int beginIndex  = (int) Math.round((length * startX) / 100);
                    int endIndex    = (int) Math.round((length * (startX + horOverlap)) / 100);

                    // if any string is less than the bigger string, then it will consider only length of that string
                    if (endIndex > len)
                        endIndex = len;
                    String str      = text.substring(beginIndex, endIndex);
                    sb.append(str + "\n");
                    System.out.println("Output = " + str.trim());
                }
            }
        }
        return sb.toString();
    }

    private File createFilePath() {
        String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File file = getExternalFilesDir(Environment.DIRECTORY_NOTIFICATIONS);
        try {
            file = File.createTempFile(fileName, ".jpg", file).getAbsoluteFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return file;
    }

    private void pickCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "New Pic");
        /// imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        File imagePath = createFilePath();
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        imageUri = new FileProvider().getUriForFile(this, "com.google.codelab.mlkit", imagePath);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);
    }

    private boolean checkCameraPermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
        return result;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}

