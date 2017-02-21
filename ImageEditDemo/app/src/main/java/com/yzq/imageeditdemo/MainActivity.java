package com.yzq.imageeditdemo;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    public static final int TAKE_PHOTO = 102;
    public static final int MAINREQUESTCODE = 103;
    public static final int OPEN_ALBUM = 103;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
       findViewById(R.id.open_Album).setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View v) {
               openAlbum();
           }
       });
        findViewById(R.id.open_camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });
    }

    public void openCamera(){
        File outputImage = new File(getExternalCacheDir(), "output_image.jpg");
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= 24) {
            imageUri = FileProvider.getUriForFile(this, "com.gyq.cameraalbumtest.fileprovider", outputImage);
        } else {
            imageUri = Uri.fromFile(outputImage);
        }
        //启动相机程序
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }
    public void openAlbum(){
        Intent intent = new Intent(Intent.ACTION_PICK);intent.setType("image/*");
        startActivityForResult(intent, OPEN_ALBUM);
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("backCode_fg", requestCode + "/" + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case TAKE_PHOTO:
                Log.i("data",resultCode+"");
                Intent intent = new Intent(this,AcCreateEvent.class);
                intent.setData(imageUri);
                startActivityForResult(intent,MAINREQUESTCODE);
                break;
            case  OPEN_ALBUM:
                if(data != null){
                    Uri uri = data.getData();
                    Intent intent1 = new Intent(this,AcCreateEvent.class);
                    intent1.setData(uri);
                    startActivityForResult(intent1,MAINREQUESTCODE);
                }
                break;
            default:
                break;
        }
    }
}
