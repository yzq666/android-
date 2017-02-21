package graffiti;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.yzq.imageeditdemo.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import utils.ImageUtils;
import utils.LogUtil;
import utils.ThreadUtil;

/**
 * Ϳѻ���棬����GraffitiView�Ľӿڣ��ṩҳ�潻��
 * ����ߴ����ui�Ƚϴֲڣ���ҪĿ���Ǹ��ߴ��GraffitiView�Ľӿھ�����ʵ��ʲô���ܣ�ʵ�������е�ui�ͽ�����������ۣ�
 */
public class GraffitiActivity extends Activity {

    public static final String TAG = "Graffiti";

    public static final int RESULT_ERROR = -111; // ���ִ���

    /**
     * ����Ϳѻ����
     *
     * @param activity
     * @param params      Ϳѻ����
     * @param requestCode startActivityForResult��������
     * @see GraffitiParams
     */
    public static void startActivityForResult(Activity activity, GraffitiParams params, int requestCode) {
        Intent intent = new Intent(activity, GraffitiActivity.class);
        intent.putExtra(GraffitiActivity.KEY_PARAMS, params);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * ����Ϳѻ����
     *
     * @param activity
     * @param imagePath   ��ͼƬ·��
     * @param savePath    ������·��
     * @param isDir       ������·���Ƿ�ΪĿ¼
     * @param requestCode ��startActivityForResult��������
     */
    @Deprecated
    public static void startActivityForResult(Activity activity, String imagePath, String savePath, boolean isDir, int requestCode) {
        GraffitiParams params = new GraffitiParams();
        params.mImagePath = imagePath;
        params.mSavePath = savePath;
        params.mSavePathIsDir = isDir;
        startActivityForResult(activity, params, requestCode);
    }

    /**
     * {@link GraffitiActivity#startActivityForResult(Activity, String, String, boolean, int)}
     */
    @Deprecated
    public static void startActivityForResult(Activity activity, String imagePath, int requestCode) {
        GraffitiParams params = new GraffitiParams();
        params.mImagePath = imagePath;
        startActivityForResult(activity, params, requestCode);
    }

    public static final String KEY_PARAMS = "key_graffiti_params";
    public static final String KEY_IMAGE_PATH = "key_image_path";

    private String mImagePath;
    private Bitmap mBitmap;

    private FrameLayout mFrameLayout;
    private GraffitiView mGraffitiView;

    private View.OnClickListener mOnClickListener;

    private SeekBar mPaintSizeBar;
    private TextView mPaintSizeView;

    private View mBtnColor;
    private Runnable mUpdateScale;

    private int mTouchMode;
    private boolean mIsMovingPic = false;

    // ���Ʋ������
    private float mOldScale, mOldDist, mNewDist, mToucheCentreXOnGraffiti,
            mToucheCentreYOnGraffiti, mTouchCentreX, mTouchCentreY;// ˫ָ����

    private float mTouchLastX, mTouchLastY;

    private boolean mIsScaling = false;
    private float mScale = 1;
    private final float mMaxScale = 3.5f; // ������ű���
    private final float mMinScale = 0.25f; // ��С���ű���
    private final int TIME_SPAN = 40;
    private View mBtnMovePic, mBtnHidePanel, mSettingsPanel;

    private int mTouchSlop;

    private AlphaAnimation mViewShowAnimation, mViewHideAnimation; // view���غ���ʾʱ�õ��Ľ��䶯��

    // ��ǰ��Ļ���ĵ��Ӧ��GraffitiView�еĵ������
    float mCenterXOnGraffiti;
    float mCenterYOnGraffiti;

    private GraffitiParams mGraffitiParams;

    // ������Ļ����һ��ʱ����ж�Ϊ��Ҫ�����������
    private Runnable mHideDelayRunnable;
    //������Ļ����һ��ʱ����ж�Ϊ��Ҫ�����������
    private Runnable mShowDelayRunnable;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(KEY_PARAMS, mGraffitiParams);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
        mGraffitiParams = savedInstanceState.getParcelable(KEY_PARAMS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mGraffitiParams == null) {
            mGraffitiParams = getIntent().getExtras().getParcelable(KEY_PARAMS);
        }
        if (mGraffitiParams == null) {
            LogUtil.d("TAG", "mGraffitiParams is null!");
            this.finish();
            return;
        }

        mImagePath = mGraffitiParams.mImagePath;
        if (mImagePath == null) {
            LogUtil.d("TAG", "mImagePath is null!");
            this.finish();
            return;
        }
        LogUtil.d("TAG", mImagePath);
        if (mGraffitiParams.mIsFullScreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mBitmap = ImageUtils.createBitmapFromPath(mImagePath, this);
        if (mBitmap == null) {
            this.finish();
            return;
        }
        setContentView(R.layout.layout_graffiti);
        mFrameLayout = (FrameLayout) findViewById(R.id.graffiti_container);
        mGraffitiView = new GraffitiView(this, mBitmap, mGraffitiParams.mEraserPath, mGraffitiParams.mEraserImageIsResizeable,
                new GraffitiView.GraffitiListener() {
                    @Override
                    public void onSaved(Bitmap bitmap, Bitmap bitmapEraser) { // ����ͼƬ
                        if (bitmapEraser != null) {
                            bitmapEraser.recycle(); // ����ͼƬ������Ϳѻ�������ڴ����
                        }
                        File graffitiFile = null;
                        File file = null;
                        String savePath = mGraffitiParams.mSavePath;
                        boolean isDir = mGraffitiParams.mSavePathIsDir;
                        if (TextUtils.isEmpty(savePath)) {
                            File dcimFile = new File(Environment.getExternalStorageDirectory(), "DCIM");
                            graffitiFile = new File(dcimFile, "Graffiti");
                            //�������·��
                            file = new File(graffitiFile, System.currentTimeMillis() + ".jpg");
                        } else {
                            if (isDir) {
                                graffitiFile = new File(savePath);
                                //�������·��
                                file = new File(graffitiFile, System.currentTimeMillis() + ".jpg");
                            } else {
                                file = new File(savePath);
                                graffitiFile = file.getParentFile();
                            }
                        }
                        graffitiFile.mkdirs();

                        FileOutputStream outputStream = null;
                        try {
                            outputStream = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream);
                            ImageUtils.addImage(getContentResolver(), file.getAbsolutePath());
                            Intent intent = new Intent();
                            intent.putExtra(KEY_IMAGE_PATH, file.getAbsolutePath());
                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        } catch (Exception e) {
                            e.printStackTrace();
                            onError(GraffitiView.ERROR_SAVE, e.getMessage());
                        } finally {
                            if (outputStream != null) {
                                try {
                                    outputStream.close();
                                } catch (IOException e) {
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(int i, String msg) {
                        setResult(RESULT_ERROR);
                        finish();
                    }

                    @Override
                    public void onReady() {
                        findViewById(R.id.btn_pen_hand).performClick();
                        findViewById(R.id.btn_hand_write).performClick();
                    }
                });
        mGraffitiView.setIsDrawableOutside(mGraffitiParams.mIsDrawableOutside);
        mFrameLayout.addView(mGraffitiView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mOnClickListener = new GraffitiOnClickListener();
        mTouchSlop = ViewConfiguration.get(getApplicationContext()).getScaledTouchSlop();
        initView();
    }

    private void initView() {
        findViewById(R.id.btn_pen_hand).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_pen_copy).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_pen_eraser).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_hand_write).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_arrow).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_line).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_holl_circle).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_fill_circle).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_holl_rect).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_fill_rect).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_clear).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_undo).setOnClickListener(mOnClickListener);
        mBtnHidePanel = findViewById(R.id.graffiti_btn_hide_panel);
        mBtnHidePanel.setOnClickListener(mOnClickListener);
        findViewById(R.id.graffiti_btn_finish).setOnClickListener(mOnClickListener);
        findViewById(R.id.graffiti_btn_back).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_centre_pic).setOnClickListener(mOnClickListener);
        findViewById(R.id.btn_restore).setOnClickListener(mOnClickListener);
        mBtnMovePic = findViewById(R.id.btn_move_pic);
        mBtnMovePic.setOnClickListener(mOnClickListener);
        mBtnColor = findViewById(R.id.btn_set_color);
        mBtnColor.setOnClickListener(mOnClickListener);
        mSettingsPanel = findViewById(R.id.graffiti_panel);
        if (mGraffitiView.getGraffitiColor().getType() == GraffitiView.GraffitiColor.Type.COLOR) {
            mBtnColor.setBackgroundColor(mGraffitiView.getGraffitiColor().getColor());
        } else if (mGraffitiView.getGraffitiColor().getType() == GraffitiView.GraffitiColor.Type.BITMAP) {
            mBtnColor.setBackgroundDrawable(new BitmapDrawable(mGraffitiView.getGraffitiColor().getBitmap()));
        }

        mPaintSizeBar = (SeekBar) findViewById(R.id.paint_size);
        mPaintSizeView = (TextView) findViewById(R.id.paint_size_text);

        mPaintSizeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mPaintSizeView.setText("" + progress);
                mGraffitiView.setPaintSize(progress);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        mPaintSizeBar.setProgress((int) mGraffitiView.getPaintSize());

        ScaleOnTouchListener onTouchListener = new ScaleOnTouchListener();
        findViewById(R.id.btn_amplifier).setOnTouchListener(onTouchListener);
        findViewById(R.id.btn_reduce).setOnTouchListener(onTouchListener);

        // ���Ϳѻ�Ĵ������������ƶ�ͼƬλ��
        mGraffitiView.setOnTouchListener(new View.OnTouchListener() {

            boolean mIsBusy = false; // ����˫ָ��������ָ̧��ʱ����ָ�¼���

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // �����������
                if (!mBtnHidePanel.isSelected()  // �������û�б�����
                        && mGraffitiParams.mChangePanelVisibilityDelay > 0) {
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_DOWN:
                            mSettingsPanel.removeCallbacks(mHideDelayRunnable);
                            mSettingsPanel.removeCallbacks(mShowDelayRunnable);
                            mSettingsPanel.postDelayed(mHideDelayRunnable, mGraffitiParams.mChangePanelVisibilityDelay); //������Ļ����һ��ʱ����ж�Ϊ��Ҫ�����������
                            break;
                        case MotionEvent.ACTION_CANCEL:
                        case MotionEvent.ACTION_UP:
                            mSettingsPanel.removeCallbacks(mHideDelayRunnable);
                            mSettingsPanel.removeCallbacks(mShowDelayRunnable);
                            mSettingsPanel.postDelayed(mShowDelayRunnable, mGraffitiParams.mChangePanelVisibilityDelay); //�뿪��Ļ����һ��ʱ����ж�Ϊ��Ҫ��ʾ�������
                            break;
                    }
                } else if (mBtnHidePanel.isSelected() && mGraffitiView.getAmplifierScale() > 0) {
                    mGraffitiView.setAmplifierScale(-1);
                }

                if (!mIsMovingPic) {
                    return false;  // ������һ���Ϳѻ����
                }
                mScale = mGraffitiView.getScale();
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        mTouchMode = 1;
                        mTouchLastX = event.getX();
                        mTouchLastY = event.getY();
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mTouchMode = 0;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (mTouchMode < 2) { // ���㻬��
                            if (mIsBusy) { // �Ӷ�㴥����Ϊ���㴥�������Ըô��¼��������˫ָ���ű�Ϊ��ָ�ƶ�ʱͼƬ˲���ƶ�
                                mIsBusy = false;
                                mTouchLastX = event.getX();
                                mTouchLastY = event.getY();
                                return true;
                            }
                            float tranX = event.getX() - mTouchLastX;
                            float tranY = event.getY() - mTouchLastY;
                            mGraffitiView.setTrans(mGraffitiView.getTransX() + tranX, mGraffitiView.getTransY() + tranY);
                            mTouchLastX = event.getX();
                            mTouchLastY = event.getY();
                        } else { // ���
                            mNewDist = spacing(event);// ���㻬��ʱ�ľ���
                            if (Math.abs(mNewDist - mOldDist) >= mTouchSlop) {
                                float scale = mNewDist / mOldDist;
                                mScale = mOldScale * scale;

                                if (mScale > mMaxScale) {
                                    mScale = mMaxScale;
                                }
                                if (mScale < mMinScale) { // ��С����
                                    mScale = mMinScale;
                                }
                                // Χ������(0,0)����ͼƬ
                                mGraffitiView.setScale(mScale);
                                // ���ź�ƫ��ͼƬ���Բ���Χ��ĳ�������ŵ�Ч��
                                float transX = mGraffitiView.toTransX(mTouchCentreX, mToucheCentreXOnGraffiti);
                                float transY = mGraffitiView.toTransY(mTouchCentreY, mToucheCentreYOnGraffiti);
                                mGraffitiView.setTrans(transX, transY);
                            }
                        }
                        return true;
                    case MotionEvent.ACTION_POINTER_UP:
                        mTouchMode -= 1;
                        return true;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        mTouchMode += 1;
                        mOldScale = mGraffitiView.getScale();
                        mOldDist = spacing(event);// ���㰴��ʱ�ľ���
                        mTouchCentreX = (event.getX(0) + event.getX(1)) / 2;// ���ü�trans
                        mTouchCentreY = (event.getY(0) + event.getY(1)) / 2;
                        mToucheCentreXOnGraffiti = mGraffitiView.toX(mTouchCentreX);
                        mToucheCentreYOnGraffiti = mGraffitiView.toY(mTouchCentreY);
                        mIsBusy = true; // ��־λ��㴥��
                        return true;
                }
                return true;
            }
        });

        findViewById(R.id.graffiti_txt_title).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) { // ������������ʾԭͼ
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        mGraffitiView.setJustDrawOriginal(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        mGraffitiView.setJustDrawOriginal(false);
                        break;
                }
                return true;
            }
        });

        mViewShowAnimation = new AlphaAnimation(0, 1);
        mViewShowAnimation.setDuration(500);
        mViewHideAnimation = new AlphaAnimation(1, 0);
        mViewHideAnimation.setDuration(500);
        mHideDelayRunnable = new Runnable() {
            public void run() {
                hideView(mSettingsPanel);
            }

        };
        mShowDelayRunnable = new Runnable() {
            public void run() {
                showView(mSettingsPanel);
            }
        };
    }

    /**
     * ������ָ��ľ���
     *
     * @param event
     * @return
     */

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private class GraffitiOnClickListener implements View.OnClickListener {

        private View mLastPenView, mLastShapeView;
        private boolean mDone = false;

        @Override
        public void onClick(View v) {
            mDone = false;
            if (v.getId() == R.id.btn_pen_hand) {
                mGraffitiView.setPen(GraffitiView.Pen.HAND);
                mDone = true;
            } else if (v.getId() == R.id.btn_pen_copy) {
                mGraffitiView.setPen(GraffitiView.Pen.COPY);
                mDone = true;
            } else if (v.getId() == R.id.btn_pen_eraser) {
                mGraffitiView.setPen(GraffitiView.Pen.ERASER);
                mDone = true;
            }
            if (mDone) {
                if (mLastPenView != null) {
                    mLastPenView.setSelected(false);
                }
                v.setSelected(true);
                mLastPenView = v;
                return;
            }

            if (v.getId() == R.id.btn_clear) {
                new AlertDialog.Builder(GraffitiActivity.this)
                        .setTitle(R.string.graffiti_clear_screen)
                        .setMessage(R.string.graffiti_cant_undo_after_clearing)
                        .setPositiveButton(R.string.graffiti_enter, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                mGraffitiView.clear();
                            }
                        })
                        .setNegativeButton(R.string.graffiti_cancel, null)
                        .show();
                mDone = true;
            } else if (v.getId() == R.id.btn_undo) {
                mGraffitiView.undo();
                mDone = true;
            } else if (v.getId() == R.id.btn_set_color) {
                new ColorPickerDialog(GraffitiActivity.this, mGraffitiView.getGraffitiColor().getColor(), "������ɫ",
                        new ColorPickerDialog.OnColorChangedListener() {
                            public void colorChanged(int color) {
                                mBtnColor.setBackgroundColor(color);
                                mGraffitiView.setColor(color);
                            }

                            @Override
                            public void colorChanged(Drawable color) {
                                mBtnColor.setBackgroundDrawable(color);
                                mGraffitiView.setColor(ImageUtils.getBitmapFromDrawable(color));
                            }
                        }).show();
                mDone = true;
            }else if(v.getId() == R.id.btn_restore){
                mGraffitiView.restore();
                mDone = true;
            }
            if (mDone) {
                return;
            }

            if (v.getId() == R.id.graffiti_btn_hide_panel) {
                mSettingsPanel.removeCallbacks(mHideDelayRunnable);
                mSettingsPanel.removeCallbacks(mShowDelayRunnable);
                v.setSelected(!v.isSelected());
                if (!mBtnHidePanel.isSelected()) {
                    showView(mSettingsPanel);
                } else {
                    hideView(mSettingsPanel);
                }
                mDone = true;
            } else if (v.getId() == R.id.graffiti_btn_finish) {
                mGraffitiView.save();
                mDone = true;
            } else if (v.getId() == R.id.graffiti_btn_back) {
                if (!mGraffitiView.isModified()) {
                    finish();
                    return;
                }
                new AlertDialog.Builder(GraffitiActivity.this).setTitle(R.string.graffiti_saving_picture)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setPositiveButton(R.string.graffiti_enter, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                mGraffitiView.save();
                            }
                        })
                        .setNegativeButton(R.string.graffiti_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                finish();
                            }
                        }).show();
                mDone = true;
            } else if (v.getId() == R.id.btn_centre_pic) {
                mGraffitiView.centrePic();
                mDone = true;
            } else if (v.getId() == R.id.btn_move_pic) {
                v.setSelected(!v.isSelected());
                mIsMovingPic = v.isSelected();
                if (mIsMovingPic) {
                    Toast.makeText(getApplicationContext(), R.string.graffiti_moving_pic, Toast.LENGTH_SHORT).show();
                }
                mDone = true;
            }
            if (mDone) {
                return;
            }

            if (v.getId() == R.id.btn_hand_write) {
                mGraffitiView.setShape(GraffitiView.Shape.HAND_WRITE);
            } else if (v.getId() == R.id.btn_arrow) {
                mGraffitiView.setShape(GraffitiView.Shape.ARROW);
            } else if (v.getId() == R.id.btn_line) {
                mGraffitiView.setShape(GraffitiView.Shape.LINE);
            } else if (v.getId() == R.id.btn_holl_circle) {
                mGraffitiView.setShape(GraffitiView.Shape.HOLLOW_CIRCLE);
            } else if (v.getId() == R.id.btn_fill_circle) {
                mGraffitiView.setShape(GraffitiView.Shape.FILL_CIRCLE);
            } else if (v.getId() == R.id.btn_holl_rect) {
                mGraffitiView.setShape(GraffitiView.Shape.HOLLOW_RECT);
            } else if (v.getId() == R.id.btn_fill_rect) {
                mGraffitiView.setShape(GraffitiView.Shape.FILL_RECT);
            }

            if (mLastShapeView != null) {
                mLastShapeView.setSelected(false);
            }
            v.setSelected(true);
            mLastShapeView = v;
        }
    }

    @Override
    public void onBackPressed() {

        if (mBtnMovePic.isSelected()) {
            mBtnMovePic.setSelected(false);
            return;
        } else {
            findViewById(R.id.graffiti_btn_back).performClick();
        }

    }

    /**
     * �Ŵ���С
     */
    private class ScaleOnTouchListener implements View.OnTouchListener {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    scalePic(v);
                    v.setSelected(true);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    mIsScaling = false;
                    v.setSelected(false);
                    break;
            }
            return true;
        }
    }

    /**
     * ����
     *
     * @param v
     */
    public void scalePic(View v) {
        if (mIsScaling)
            return;
        mIsScaling = true;
        mScale = mGraffitiView.getScale();

        // ȷ����ǰ��Ļ���ĵ��Ӧ��GraffitiView�еĵ�����֮꣬��Χ�����������
        mCenterXOnGraffiti = mGraffitiView.toX(mGraffitiView.getWidth() / 2);
        mCenterYOnGraffiti = mGraffitiView.toY(mGraffitiView.getHeight() / 2);

        if (v.getId() == R.id.btn_amplifier) { // �Ŵ�
            ThreadUtil.getInstance().runOnAsyncThread(new Runnable() {
                public void run() {
                    do {
                        mScale += 0.05f;
                        if (mScale > mMaxScale) {
                            mScale = mMaxScale;
                            mIsScaling = false;
                        }
                        updateScale();
                        try {
                            Thread.sleep(TIME_SPAN);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } while (mIsScaling);

                }
            });
        } else if (v.getId() == R.id.btn_reduce) { // ��С
            ThreadUtil.getInstance().runOnAsyncThread(new Runnable() {
                public void run() {
                    do {
                        mScale -= 0.05f;
                        if (mScale < mMinScale) {
                            mScale = mMinScale;
                            mIsScaling = false;
                        }
                        updateScale();
                        try {
                            Thread.sleep(TIME_SPAN);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } while (mIsScaling);
                }
            });
        }
    }

    private void updateScale() {
        if (mUpdateScale == null) {

            mUpdateScale = new Runnable() {
                public void run() {
                    // Χ������(0,0)����ͼƬ
                    mGraffitiView.setScale(mScale);
                    // ���ź�ƫ��ͼƬ���Բ���Χ��ĳ�������ŵ�Ч��
                    float transX = mGraffitiView.toTransX(mGraffitiView.getWidth() / 2, mCenterXOnGraffiti);
                    float transY = mGraffitiView.toTransY(mGraffitiView.getHeight() / 2, mCenterYOnGraffiti);
                    mGraffitiView.setTrans(transX, transY);
                }
            };
        }
        ThreadUtil.getInstance().runOnMainThread(mUpdateScale);
    }

    private void showView(View view) {
        if (view.getVisibility() == View.VISIBLE) {
            return;
        }

        view.clearAnimation();
        view.startAnimation(mViewShowAnimation);
        view.setVisibility(View.VISIBLE);
        if (view == mSettingsPanel || mBtnHidePanel.isSelected()) {
            mGraffitiView.setAmplifierScale(-1);
        }
    }

    private void hideView(View view) {
        if (view.getVisibility() != View.VISIBLE) {
            if (view == mSettingsPanel && mGraffitiView.getAmplifierScale() > 0) {
                mGraffitiView.setAmplifierScale(-1);
            }
            return;
        }
        view.clearAnimation();
        view.startAnimation(mViewHideAnimation);
        view.setVisibility(View.GONE);
        if (view == mSettingsPanel && !mBtnHidePanel.isSelected() && !mBtnMovePic.isSelected()) {
            // �������������ʱ����ʾ�Ŵ���
            mGraffitiView.setAmplifierScale(mGraffitiParams.mAmplifierScale);
        } else if ((view == mSettingsPanel && mGraffitiView.getAmplifierScale() > 0)) {
            mGraffitiView.setAmplifierScale(-1);
        }
    }

    /**
     * Ϳѻ����
     */
    public static class GraffitiParams implements Parcelable {

        /**
         * ͼƬ·��
         */
        public String mImagePath;
        /**
         * ������·�������Ϊnull����ͼƬ�����ڸ�Ŀ¼��/DCIM/Graffiti/
         */
        public String mSavePath;
        /**
         * ������·���Ƿ�ΪĿ¼�����ΪĿ¼�����ڸ�Ŀ¼������ʱ�����ɵ�ͼƬ����
         */
        public boolean mSavePathIsDir;
        /**
         * ����Ƥ����ͼ�����Ϊnull�����ͼΪ��ǰͼƬ·��
         * {@link GraffitiView#GraffitiView(Context, Bitmap, String, boolean, GraffitiView.GraffitiListener)}
         */
        public String mEraserPath;

        /**
         * ��Ƥ����ͼ�Ƿ������С�����Ϊtrue�����������ǰͿѻͼƬһ���Ĵ�С��
         * Ĭ��Ϊtrue
         */
        public boolean mEraserImageIsResizeable = true;

        /**
         * ����ʱ��ͼƬ�������Ƿ����Ϳѻ�켣
         */
        public boolean mIsDrawableOutside;

        /**
         * Ϳѻʱ����ָ���£��������������ӳ�ʱ��(ms)����С�ڵ���0ʱ��Ϊ������������壨��������嵱ǰ״̬���䣩;������0ʱ��ʾ��Ҫ������Ļ����һ��ʱ��������
         * ������ָ̧��ʱչʾ�����ӳ�ʱ��(ms)�����߱�ʾ��Ҫ�뿪��Ļ����һ��ʱ����չʾ
         * Ĭ��Ϊ800ms
         */
        public long mChangePanelVisibilityDelay = 800; //ms

        /**
         * ���÷Ŵ󾵵ı�������С�ڵ���0ʱ��ʾ��ʹ�÷Ŵ�������
         * �Ŵ���ֻ����������屻���ص�ʱ��Ż����
         * Ĭ��Ϊ2.5��
         */
        public float mAmplifierScale = 2.5f;

        /**
         * �Ƿ�ȫ����ʾ�����Ƿ�����״̬��
         * Ĭ��Ϊfalse����ʾ״̬���̳�Ӧ����ʽ
         */
        public boolean mIsFullScreen = false;

        public static final Creator<GraffitiParams> CREATOR = new Creator<GraffitiParams>() {
            @Override
            public GraffitiParams createFromParcel(Parcel in) {
                GraffitiParams params = new GraffitiParams();
                params.mImagePath = in.readString();
                params.mSavePath = in.readString();
                params.mSavePathIsDir = in.readInt() == 1;
                params.mEraserPath = in.readString();
                params.mEraserImageIsResizeable = in.readInt() == 1;
                params.mIsDrawableOutside = in.readInt() == 1;

                return params;
            }

            @Override
            public GraffitiParams[] newArray(int size) {
                return new GraffitiParams[size];
            }
        };

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mImagePath);
            dest.writeString(mSavePath);
            dest.writeInt(mSavePathIsDir ? 1 : 0);
            dest.writeString(mEraserPath);
            dest.writeInt(mEraserImageIsResizeable ? 1 : 0);
            dest.writeInt(mIsDrawableOutside ? 1 : 0);
        }

        @Override
        public int describeContents() {
            return 0;
        }
    }
}
