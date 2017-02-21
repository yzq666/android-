package com.yzq.imageeditdemo;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import graffiti.GraffitiActivity;


public class AcCreateEvent extends Activity{
    Button imgEdit;
    private Uri imageUri;
    public static final int REQ_CODE_GRAFFITI = 101;
    private ImageView img;
    private Button edit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ac_create_event);
        img = (ImageView) findViewById(R.id.event_message);
        findViewById(R.id.back_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.img_edit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                GraffitiActivity.GraffitiParams params = new GraffitiActivity.GraffitiParams();
                params.mImagePath = getRealFilePath(AcCreateEvent.this, imageUri);
                GraffitiActivity.startActivityForResult(AcCreateEvent.this, params, REQ_CODE_GRAFFITI);
            }
        });
        imageUri = getIntent().getData();
        Glide.with(this).load(imageUri).into(img);
    }





    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("backCode_fg", requestCode + "/" + resultCode);
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case  REQ_CODE_GRAFFITI:
                if (data == null) {
                    return;
                }
                if (resultCode == GraffitiActivity.RESULT_OK) {
                    String path = data.getStringExtra(GraffitiActivity.KEY_IMAGE_PATH);
                    if (TextUtils.isEmpty(path)) {
                        return;
                    }else{
                        Glide.with(this).load(path).into(img);
                    }
                } else if (resultCode == GraffitiActivity.RESULT_ERROR) {
                    Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }
    public static String getRealFilePath( final Context context, final Uri uri ) {
        if ( null == uri ) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if ( scheme == null )
            data = uri.getPath();
        else if ( ContentResolver.SCHEME_FILE.equals( scheme ) ) {
            data = uri.getPath();
        } else if ( ContentResolver.SCHEME_CONTENT.equals( scheme ) ) {
            Cursor cursor = context.getContentResolver().query( uri, new String[] { MediaStore.Images.ImageColumns.DATA }, null, null, null );
            if ( null != cursor ) {
                if ( cursor.moveToFirst() ) {
                    int index = cursor.getColumnIndex( MediaStore.Images.ImageColumns.DATA );
                    if ( index > -1 ) {
                        data = cursor.getString( index );
                    }
                }
                cursor.close();
            }
        }
        return data;
    }
}
