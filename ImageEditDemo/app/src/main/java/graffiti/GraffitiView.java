package graffiti;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.Shader;
import android.os.Build;
import android.view.MotionEvent;
import android.view.View;

import com.bumptech.glide.util.Util;

import java.util.concurrent.CopyOnWriteArrayList;

import utils.DensityUtil;
import utils.ImageUtils;


/**
 * Created by Administrator on 2016/9/3.
 */
public class GraffitiView extends View {

    public static final int ERROR_INIT = -1;
    public static final int ERROR_SAVE = -2;

    private static final float VALUE = 1f;
    private final int TIME_SPAN = 80;

    private GraffitiListener mGraffitiListener;
    private Bitmap mBitmap; // ԭͼ
    private Bitmap mBitmapEraser; // ��Ƥ����ͼ
    private Bitmap mGraffitiBitmap; // �û���Ϳѻ��ͼƬ
    private Canvas mBitmapCanvas;

    private float mPrivateScale; // ͼƬ��Ӧ��Ļ��mScale=1��ʱ�����ű���
    private int mPrivateHeight, mPrivateWidth;// ͼƬ������mPrivateScale����������£���Ӧ��Ļ��mScale=1��ʱ�Ĵ�С�����ۿ���������Ļ�ϵĴ�С��
    private float mCentreTranX, mCentreTranY;// ͼƬ������mPrivateScale����������£����У�mScale=1��ʱ��ƫ�ƣ����ۿ���������Ļ�ϵ�ƫ�ƣ�

    private BitmapShader mBitmapShader; // ����Ϳѻ��ͼƬ��!
    private BitmapShader mBitmapShaderEraser; // ��Ƥ����ͼ
    private Path mCurrPath; // ��ǰ��д��·��
    private Path mTempPath;
    private CopyLocation mCopyLocation; // ���ƵĶ�λ��

    private Paint mPaint;
    private int mTouchMode; // ����ģʽ�������жϵ�����㴥��
    private float mPaintSize;
    private GraffitiColor mColor; // ���ʵ�ɫ
    private float mScale; // ͼƬ������ھ���ʱ�����ű��� �� ͼƬ��ʵ�����ű���Ϊ mPrivateScale*mScale ��

    private float mTransX = 0, mTransY = 0; // ͼƬ������ھ���ʱ��������mScale����������µ�ƫ���� �� ͼƬ��ʵƫ����Ϊ��(mCentreTranX + mTransX)/mPrivateScale*mScale ��

/*
      ��������һ�㣬�������Ϳѻ����ϵ����Ҫ��
      ���費�����κ����ţ�ͼƬ�������ۿ�������ô�󣬴�ʱͼƬ�Ĵ�Сwidth =  mPrivateWidth * mScale ,
      ƫ����x = mCentreTranX + mTransX����view�Ĵ�СΪwidth = getWidth()��height��ƫ����y�Դ����ơ�
*/

    private boolean mIsPainting = false; // �Ƿ����ڻ���
    private boolean isJustDrawOriginal; // �Ƿ�ֻ����ԭͼ

    private boolean mIsDrawableOutside = false; // ����ʱ��ͼƬ�������Ƿ����Ϳѻ�켣
    private boolean mEraserImageIsResizeable;
    private boolean mReady = false;


    // ����Ϳѻ���������ڳ���
    private CopyOnWriteArrayList<GraffitiPath> mPathStack = new CopyOnWriteArrayList<GraffitiPath>();//������¼�б�
    private CopyOnWriteArrayList<GraffitiPath> undoStack = new CopyOnWriteArrayList<GraffitiPath>();//undo list
    /**
     * ����
     */
    public enum Pen {
        HAND, // �ֻ�
        COPY, // ����
        ERASER // ��Ƥ��
    }

    /**
     * ͼ��
     */
    public enum Shape {
        HAND_WRITE, //
        ARROW, // ��ͷ
        LINE, // ֱ��
        FILL_CIRCLE, // ʵ��Բ
        HOLLOW_CIRCLE, // ����Բ
        FILL_RECT, // ʵ�ľ���
        HOLLOW_RECT, // ���ľ���
    }

    private Pen mPen;
    private Shape mShape;

    private float mTouchDownX, mTouchDownY, mLastTouchX, mLastTouchY, mTouchX, mTouchY;
    private Matrix mShaderMatrix, mMatrixTemp;

    private float mAmplifierRadius;
    private Path mAmplifierPath;
    private float mAmplifierScale = 0; // �Ŵ󾵵ı���
    private Paint mAmplifierPaint;
    private int mAmplifierHorizonX; // �Ŵ�����λ�õ�x���꣬ʹ��ˮƽ����

    public GraffitiView(Context context, Bitmap bitmap, GraffitiListener listener) {
        this(context, bitmap, null, true, listener);
    }

    /**
     * @param context
     * @param bitmap
     * @param eraser                  ��Ƥ���ĵ�ͼ�����Ϳѻ������ٴ�Ϳѻ������Ϳѻǰ�ĵ�ͼ�������ʵ�ֲ���Ϳѻ��Ч����
     * @param eraserImageIsResizeable ��Ƥ����ͼ�Ƿ������С��������������������ǰͿѻͼƬһ���Ĵ�С��
     * @param listener
     * @
     */
    public GraffitiView(Context context, Bitmap bitmap, String eraser, boolean eraserImageIsResizeable, GraffitiListener listener) {
        super(context);

        if (Build.VERSION.SDK_INT >= 11) {
            setLayerType(LAYER_TYPE_SOFTWARE, null);
        }

        mBitmap = bitmap;
        mGraffitiListener = listener;
        if (mGraffitiListener == null) {
            throw new RuntimeException("GraffitiListener is null!!!");
        }
        if (mBitmap == null) {
            throw new RuntimeException("Bitmap is null!!!");
        }

        if (eraser != null) {
            mBitmapEraser = ImageUtils.createBitmapFromPath(eraser, getContext());
        }
        mEraserImageIsResizeable = eraserImageIsResizeable;
        init();
    }

    public void init() {
        mScale = 1f;
        mPaintSize = 3;
        mColor = new GraffitiColor(Color.RED);
        mPaint = new Paint();
        mPaint.setStrokeWidth(mPaintSize);
        mPaint.setColor(mColor.mColor);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);// Բ��

        mPen = Pen.HAND;
        mShape = Shape.HAND_WRITE;

        this.mBitmapShader = new BitmapShader(this.mBitmap, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);

        if (mBitmapEraser != null) {
            this.mBitmapShaderEraser = new BitmapShader(this.mBitmapEraser, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
        } else {
            this.mBitmapShaderEraser = mBitmapShader;
        }

        mShaderMatrix = new Matrix();
        mMatrixTemp = new Matrix();
        mTempPath = new Path();
        mCopyLocation = new CopyLocation(150, 150);

        mAmplifierPaint = new Paint();
        mAmplifierPaint.setColor(0xaaffffff);
        mAmplifierPaint.setStyle(Paint.Style.STROKE);
        mAmplifierPaint.setAntiAlias(true);
        mAmplifierPaint.setStrokeJoin(Paint.Join.ROUND);
        mAmplifierPaint.setStrokeCap(Paint.Cap.ROUND);// Բ��
        mAmplifierPaint.setStrokeWidth(DensityUtil.dip2px(getContext(), 10));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setBG();
        mCopyLocation.updateLocation(toX(w / 2), toY(h / 2));
        if (!mReady) {
            mGraffitiListener.onReady();
            mReady = true;
        }
    }
    int moveX = 0;
    int moveY = 0;
    float downX = 0;
    float downY = 0;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mTouchMode = 1;
                mTouchDownX = mTouchX = mLastTouchX = event.getX();
                mTouchDownY = mTouchY = mLastTouchY = event.getY();
                downX = event.getX();
                downY = event.getY();
                moveX = 0;
                moveY = 0;
                if (mPen == Pen.COPY && mCopyLocation.isInIt(toX(mTouchX), toY(mTouchY))) { // ���copy
                    mCopyLocation.isRelocating = true;
                    mCopyLocation.isCopying = false;
                } else {
                    if (mPen == Pen.COPY) {
                        if (!mCopyLocation.isCopying) {
                            mCopyLocation.setStartPosition(toX(mTouchX), toY(mTouchY));
                            resetMatrix();
                        }
                        mCopyLocation.isCopying = true;
                    }
                    mCopyLocation.isRelocating = false;
                    mCurrPath = new Path();
                    mCurrPath.moveTo(toX(mTouchDownX), toY(mTouchDownY));
                    mIsPainting = true;
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchMode = 0;
                mLastTouchX = mTouchX;
                mLastTouchY = mTouchY;
                mTouchX = event.getX();
                mTouchY = event.getY();

                // Ϊ�˽����ʱҲ�ܳ��ֻ�ͼ�������ƶ�path
               /* if (mTouchDownX == mTouchX && mTouchDownY == mTouchY & mTouchDownX == mLastTouchX && mTouchDownY == mLastTouchY) {
                    mTouchX += VALUE;
                    mTouchY += VALUE;
                }*/

                if (mCopyLocation.isRelocating) { // ���ڶ�λlocation
                    mCopyLocation.updateLocation(toX(mTouchX), toY(mTouchY));
                    mCopyLocation.isRelocating = false;
                } else {
                    if (mIsPainting) {

                        if (mPen == Pen.COPY) {
                            mCopyLocation.updateLocation(mCopyLocation.mCopyStartX + toX(mTouchX) - mCopyLocation.mTouchStartX,
                                    mCopyLocation.mCopyStartY + toY(mTouchY) - mCopyLocation.mTouchStartY);
                        }

                        GraffitiPath path = null;

                        // �Ѳ�����¼������Ķ�ջ��
                        if (mShape == Shape.HAND_WRITE) { // ��д
                            mCurrPath.quadTo(
                                    toX(mLastTouchX),
                                    toY(mLastTouchY),
                                    toX((mTouchX + mLastTouchX) / 2),
                                    toY((mTouchY + mLastTouchY) / 2));
                            path = GraffitiPath.toPath(mPen, mShape, mPaintSize, mColor.copy(), mCurrPath, mPen == Pen.COPY ? new Matrix(mShaderMatrix) : null);
                        } else {  // ��ͼ��
                            path = GraffitiPath.toShape(mPen, mShape, mPaintSize, mColor.copy(),
                                    toX(mTouchDownX), toY(mTouchDownY), toX(mTouchX), toY(mTouchY),
                                    mPen == Pen.COPY ? new Matrix(mShaderMatrix) : null);
                        }
                        mPathStack.add(path);
                        draw(mBitmapCanvas, path); // ���浽ͼƬ��
                        mIsPainting = false;
                    }
                }
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                moveX = (int) Math.abs(event.getX() - downX);//X�����
                moveY = (int) Math.abs(event.getY() - downY);//y�����
                if( moveX > 100 || moveY > 100){
                    if (mTouchMode < 2) { // ���㻬��
                        mLastTouchX = mTouchX;
                        mLastTouchY = mTouchY;
                        mTouchX = event.getX();
                        mTouchY = event.getY();

                        if (mCopyLocation.isRelocating) { // ���ڶ�λlocation
                            mCopyLocation.updateLocation(toX(mTouchX), toY(mTouchY));
                        } else {
                            if (mPen == Pen.COPY) {
                                mCopyLocation.updateLocation(mCopyLocation.mCopyStartX + toX(mTouchX) - mCopyLocation.mTouchStartX,
                                        mCopyLocation.mCopyStartY + toY(mTouchY) - mCopyLocation.mTouchStartY);
                            }
                            if (mShape == Shape.HAND_WRITE) { // ��д
                                mCurrPath.quadTo(
                                        toX(mLastTouchX),
                                        toY(mLastTouchY),
                                        toX((mTouchX + mLastTouchX) / 2),
                                        toY((mTouchY + mLastTouchY) / 2));
                            } else { // ��ͼ��

                            }
                        }
                    } else { // ���
                    }
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_POINTER_UP:
                mTouchMode -= 1;
                invalidate();
                return true;
            case MotionEvent.ACTION_POINTER_DOWN:
                mTouchMode += 1;
                invalidate();
                return true;
        }
        return super.onTouchEvent(event);
    }


    private void setBG() {// ����resize preview
        int w = mBitmap.getWidth();
        int h = mBitmap.getHeight();
        float nw = w * 1f / getWidth();
        float nh = h * 1f / getHeight();
        if (nw > nh) {
            mPrivateScale = 1 / nw;
            mPrivateWidth = getWidth();
            mPrivateHeight = (int) (h * mPrivateScale);
        } else {
            mPrivateScale = 1 / nh;
            mPrivateWidth = (int) (w * mPrivateScale);
            mPrivateHeight = getHeight();
        }
        // ʹͼƬ����
        mCentreTranX = (getWidth() - mPrivateWidth) / 2f;
        mCentreTranY = (getHeight() - mPrivateHeight) / 2f;

        initCanvas();
        resetMatrix();

        mAmplifierRadius = Math.min(getWidth(), getHeight()) / 4;
        mAmplifierPath = new Path();
        mAmplifierPath.addCircle(mAmplifierRadius, mAmplifierRadius, mAmplifierRadius, Path.Direction.CCW);
        mAmplifierHorizonX = (int) (Math.min(getWidth(), getHeight()) / 2 - mAmplifierRadius);

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap.isRecycled() || mGraffitiBitmap.isRecycled()) {
            return;
        }

        canvas.save();
        doDraw(canvas);
        canvas.restore();

        if (mAmplifierScale > 0) { //���÷Ŵ�
            canvas.save();

            if (mTouchY <= mAmplifierRadius * 2) { // �ڷŴ󾵵ķ�Χ�ڣ� �ѷŴ󾵷��Ƶײ�
                canvas.translate(mAmplifierHorizonX, getHeight() - mAmplifierRadius * 2);
            } else {
                canvas.translate(mAmplifierHorizonX, 0);
            }
            canvas.clipPath(mAmplifierPath);
            canvas.drawColor(0xff000000);
            canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            canvas.save();
            float scale = mAmplifierScale / mScale; // ����mScale�����۵�ǰͼƬ���Ŷ��٣�������ͼƬ�ھ���״̬������mAmplifierScale����Ч��
            canvas.scale(scale, scale);
            canvas.translate(-mTouchX + mAmplifierRadius / scale, -mTouchY + mAmplifierRadius / scale);
            doDraw(canvas);
            canvas.restore();

            // ���Ŵ����ı߿�
            DrawUtil.drawCircle(canvas, mAmplifierRadius, mAmplifierRadius, mAmplifierRadius, mAmplifierPaint);
            canvas.restore();
        }

    }

    private void doDraw(Canvas canvas) {
        float left = (mCentreTranX + mTransX) / (mPrivateScale * mScale);
        float top = (mCentreTranY + mTransY) / (mPrivateScale * mScale);
        // ������ͼƬ����һ������ϵ��ֻ��Ҫ������Ļ����ϵ��ͼƬ������������ϵ��ӳ���ϵ
        canvas.scale(mPrivateScale * mScale, mPrivateScale * mScale); // ���Ż���
        canvas.translate(left, top); // ƫ�ƻ���

        if (!mIsDrawableOutside) { // �ü���������ΪͼƬ����
            canvas.clipRect(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
        }

        if (isJustDrawOriginal) { // ֻ����ԭͼ
            canvas.drawBitmap(mBitmap, 0, 0, null);
            return;
        }

        // ����Ϳѻ
        canvas.drawBitmap(mGraffitiBitmap, 0, 0, null);

        if (mIsPainting) {  //����view�Ļ�����
            Path path;
            float span = 0;
            // Ϊ�˽����ʱҲ�ܳ��ֻ�ͼ�������ƶ�path
            if (mTouchDownX == mTouchX && mTouchDownY == mTouchY && mTouchDownX == mLastTouchX && mTouchDownY == mLastTouchY) {
                mTempPath.reset();
                mTempPath.addPath(mCurrPath);
                mTempPath.quadTo(
                        toX(mLastTouchX),
                        toY(mLastTouchY),
                        toX((mTouchX + mLastTouchX + VALUE) / 2),
                        toY((mTouchY + mLastTouchY + VALUE) / 2));
                path = mTempPath;
                span = VALUE;
            } else {
                path = mCurrPath;
                span = 0;
            }
            // ��������·��
            mPaint.setStrokeWidth(mPaintSize);
            if (mShape == Shape.HAND_WRITE) { // ��д
                draw(canvas, mPen, mPaint, path, mShaderMatrix, mColor);
            } else {  // ��ͼ��
                draw(canvas, mPen, mShape, mPaint,
                        toX(mTouchDownX), toY(mTouchDownY), toX(mTouchX + span), toY(mTouchY + span), mShaderMatrix, mColor);
            }
        }

        if (mPen == Pen.COPY) {
            mCopyLocation.drawItSelf(canvas);
        }
    }

    private void draw(Canvas canvas, Pen pen, Paint paint, Path path, Matrix matrix, GraffitiColor color) {
        resetPaint(pen, paint, matrix, color);

        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(path, paint);

    }

    private void draw(Canvas canvas, Pen pen, Shape shape, Paint paint, float sx, float sy, float dx, float dy, Matrix matrix, GraffitiColor color) {
        resetPaint(pen, paint, matrix, color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        switch (shape) { // ����ͼ��
            case ARROW:
                paint.setStyle(Paint.Style.FILL);
                paint.setStrokeWidth(45);
                DrawUtil.drawArrow(canvas, sx, sy, dx, dy, paint);
                break;
            case LINE:
                DrawUtil.drawLine(canvas, sx, sy, dx, dy, paint);
                break;
            case FILL_CIRCLE:
                paint.setStyle(Paint.Style.FILL);
            case HOLLOW_CIRCLE:
                DrawUtil.drawOval(canvas, sx, sy, dx, dy, paint);
                break;
            case FILL_RECT:
                paint.setStyle(Paint.Style.FILL);
            case HOLLOW_RECT:
                DrawUtil.drawRect(canvas, sx, sy, dx, dy, paint);
                break;
            default:
                throw new RuntimeException("unknown shape:" + shape);
        }
    }


    private void draw(Canvas canvas, CopyOnWriteArrayList<GraffitiPath> pathStack) {
        // ��ԭ��ջ�еļ�¼�Ĳ���
        for (GraffitiPath path : pathStack) {
            draw(canvas, path);
        }
    }

    private void draw(Canvas canvas, GraffitiPath path) {
        mPaint.setStrokeWidth(path.mStrokeWidth);
        if (path.mShape == Shape.HAND_WRITE) { // ��д
            draw(canvas, path.mPen, mPaint, path.mPath, path.mMatrix, path.mColor);
        } else { // ��ͼ��
            draw(canvas, path.mPen, path.mShape, mPaint,
                    path.mSx, path.mSy, path.mDx, path.mDy, path.mMatrix, path.mColor);
        }
    }

    private void resetPaint(Pen pen, Paint paint, Matrix matrix, GraffitiColor color) {
        switch (pen) { // ���û���
            case HAND:
                paint.setShader(null);

                color.initColor(paint, null);
                break;
            case COPY:
                // ����copyͼƬλ��
                mBitmapShader.setLocalMatrix(matrix);
                paint.setShader(this.mBitmapShader);
                break;
            case ERASER:
                if (mBitmapShader == mBitmapShaderEraser) { // ͼƬ�ľ�����Ҫ�κ�ƫ��
                    mBitmapShaderEraser.setLocalMatrix(null);
                }
                paint.setShader(this.mBitmapShaderEraser);
                break;
        }
    }


    /**
     * ����Ļ��������xת������ͼƬ�е�����
     */
    public final float toX(float touchX) {
        return (touchX - mCentreTranX - mTransX) / (mPrivateScale * mScale);
    }

    /**
     * ����Ļ��������yת������ͼƬ�е�����
     */
    public final float toY(float touchY) {
        return (touchY - mCentreTranY - mTransY) / (mPrivateScale * mScale);
    }

    /**
     * ���껻��
     * ����ʽ��toX()�еĹ�ʽ�������
     *
     * @param touchX    ��������
     * @param graffitiX ��ͿѻͼƬ�е�����
     * @return ƫ����
     */
    public final float toTransX(float touchX, float graffitiX) {
        return -graffitiX * (mPrivateScale * mScale) + touchX - mCentreTranX;
    }

    public final float toTransY(float touchY, float graffitiY) {
        return -graffitiY * (mPrivateScale * mScale) + touchY - mCentreTranY;
    }

    private static class GraffitiPath {
        Pen mPen; // ��������
        Shape mShape; // ������״
        float mStrokeWidth; // ��С
        GraffitiColor mColor; // ��ɫ
        Path mPath; // ���ʵ�·��
        float mSx, mSy; // ӳ������ʼ���꣬����ָ�����
        float mDx, mDy; // ӳ������ֹ���꣬����ָ̧��
        Matrix mMatrix; //������ͼƬ��ƫ�ƾ���

        static GraffitiPath toShape(Pen pen, Shape shape, float width, GraffitiColor color,
                                    float sx, float sy, float dx, float dy, Matrix matrix) {
            GraffitiPath path = new GraffitiPath();
            path.mPen = pen;
            path.mShape = shape;
            path.mStrokeWidth = width;
            path.mColor = color;
            path.mSx = sx;
            path.mSy = sy;
            path.mDx = dx;
            path.mDy = dy;
            path.mMatrix = matrix;
            return path;
        }

        static GraffitiPath toPath(Pen pen, Shape shape, float width, GraffitiColor color, Path p, Matrix matrix) {
            GraffitiPath path = new GraffitiPath();
            path.mPen = pen;
            path.mShape = shape;
            path.mStrokeWidth = width;
            path.mColor = color;
            path.mPath = p;
            path.mMatrix = matrix;
            return path;
        }
    }

    private void initCanvas() {
        if (mGraffitiBitmap != null) {
            mGraffitiBitmap.recycle();
        }
        mGraffitiBitmap = mBitmap.copy(Bitmap.Config.RGB_565, true);
        mBitmapCanvas = new Canvas(mGraffitiBitmap);
    }

    private void resetMatrix() {
        if (mPen == Pen.COPY) { // ���ƣ�����mCopyLocation��¼��ƫ��
            this.mShaderMatrix.set(null);
            this.mShaderMatrix.postTranslate(mCopyLocation.mTouchStartX - mCopyLocation.mCopyStartX, mCopyLocation.mTouchStartY - mCopyLocation.mCopyStartY);
            this.mBitmapShader.setLocalMatrix(this.mShaderMatrix);
        } else {
            this.mShaderMatrix.set(null);
            this.mBitmapShader.setLocalMatrix(this.mShaderMatrix);
        }

        // ���ʹ�����Զ������Ƥ����ͼ������Ҫ��������
        if (mPen == Pen.ERASER && mBitmapShader != mBitmapShaderEraser) {
            mMatrixTemp.reset();
            mBitmapShaderEraser.getLocalMatrix(mMatrixTemp);
            mBitmapShader.getLocalMatrix(mMatrixTemp);
            // ������Ƥ����ͼ��ʹ֮��ͿѻͼƬ��Сһ��
            if (mEraserImageIsResizeable) {
                mMatrixTemp.preScale(mBitmap.getWidth() * 1f / mBitmapEraser.getWidth(), mBitmap.getHeight() * 1f / mBitmapEraser.getHeight());
            }
            mBitmapShaderEraser.setLocalMatrix(mMatrixTemp);
        }
    }

    /**
     * ����ͼƬλ��
     *
     * ��������һ�����Ҫ��
     * ���費�����κ����ţ�ͼƬ�������ۿ�������ô�󣬴�ʱͼƬ�Ĵ�Сwidth =  mPrivateWidth * mScale ,
     * ƫ����x = mCentreTranX + mTransX����view�Ĵ�СΪwidth = getWidth()��height��ƫ����y�Դ����ơ�
     */
    private void judgePosition() {
        boolean changed = false;
        if (mPrivateWidth * mScale < getWidth()) { // ������view��Χ��
            if (mTransX + mCentreTranX < 0) {
                mTransX = -mCentreTranX;
                changed = true;
            } else if (mTransX + mCentreTranX + mPrivateWidth * mScale > getWidth()) {
                mTransX = getWidth() - mCentreTranX - mPrivateWidth * mScale;
                changed = true;
            }
        } else { // ������view��Χ��
            if (mTransX + mCentreTranX > 0) {
                mTransX = -mCentreTranX;
                changed = true;
            } else if (mTransX + mCentreTranX + mPrivateWidth * mScale < getWidth()) {
                mTransX = getWidth() - mCentreTranX - mPrivateWidth * mScale;
                changed = true;
            }
        }
        if (mPrivateHeight * mScale < getHeight()) { // ������view��Χ��
            if (mTransY + mCentreTranY < 0) {
                mTransY = -mCentreTranY;
                changed = true;
            } else if (mTransY + mCentreTranY + mPrivateHeight * mScale > getHeight()) {
                mTransY = getHeight() - mCentreTranY - mPrivateHeight * mScale;
                changed = true;
            }
        } else { // ������view��Χ��
            if (mTransY + mCentreTranY > 0) {
                mTransY = -mCentreTranY;
                changed = true;
            } else if (mTransY + mCentreTranY + mPrivateHeight * mScale < getHeight()) {
                mTransY = getHeight() - mCentreTranY - mPrivateHeight * mScale;
                changed = true;
            }
        }
        if (changed) {
            resetMatrix();
        }
    }

    /**
     * ���ƵĶ�λ��
     */
    private class CopyLocation {

        private float mCopyStartX, mCopyStartY; // ���Ƶ�����
        private float mTouchStartX, mTouchStartY; // ��ʼ����������
        private float mX, mY; // ��ǰλ��

        private Paint mPaint;

        private boolean isRelocating = true; // ���ڶ�λ��
        private boolean isCopying = false; // ���ڷ��ƻ�ͼ��

        public CopyLocation(float x, float y) {
            mX = x;
            mY = y;
            mTouchStartX = x;
            mTouchStartY = y;
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setStrokeWidth(mPaintSize);
            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
        }


        public void updateLocation(float x, float y) {
            mX = x;
            mY = y;
        }

        public void setStartPosition(float x, float y) {
            mCopyStartX = mX;
            mCopyStartY = mY;
            mTouchStartX = x;
            mTouchStartY = y;
        }

        public void drawItSelf(Canvas canvas) {
            mPaint.setStrokeWidth(mPaintSize / 4);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0xaa666666); // ��ɫ
            DrawUtil.drawCircle(canvas, mX, mY, mPaintSize / 2 + mPaintSize / 8, mPaint);

            mPaint.setStrokeWidth(mPaintSize / 16);
            mPaint.setStyle(Paint.Style.STROKE);
            mPaint.setColor(0xaaffffff); // ��ɫ
            DrawUtil.drawCircle(canvas, mX, mY, mPaintSize / 2 + mPaintSize / 32, mPaint);

            mPaint.setStyle(Paint.Style.FILL);
            if (!isCopying) {
                mPaint.setColor(0x44ff0000); // ��ɫ
                DrawUtil.drawCircle(canvas, mX, mY, mPaintSize / 2, mPaint);
            } else {
                mPaint.setColor(0x44000088); // ��ɫ
                DrawUtil.drawCircle(canvas, mX, mY, mPaintSize / 2, mPaint);
            }
        }

        /**
         * �ж��Ƿ����
         */
        public boolean isInIt(float x, float y) {
            if ((mX - x) * (mX - x) + (mY - y) * (mY - y) <= mPaintSize * mPaintSize) {
                return true;
            }
            return false;
        }

    }

    /**
     * Ϳѻ��ɫ
     */
    public static class GraffitiColor {
        public enum Type {
            COLOR, // ��ɫֵ
            BITMAP // ͼƬ
        }

        private int mColor;
        private Bitmap mBitmap;
        private Type mType;
        private Shader.TileMode mTileX = Shader.TileMode.MIRROR;
        private Shader.TileMode mTileY = Shader.TileMode.MIRROR;  // ����

        public GraffitiColor(int color) {
            mType = Type.COLOR;
            mColor = color;
        }

        public GraffitiColor(Bitmap bitmap) {
            mType = Type.BITMAP;
            mBitmap = bitmap;
        }

        public GraffitiColor(Bitmap bitmap, Shader.TileMode tileX, Shader.TileMode tileY) {
            mType = Type.BITMAP;
            mBitmap = bitmap;
            mTileX = tileX;
            mTileY = tileY;
        }

        void initColor(Paint paint, Matrix matrix) {
            if (mType == Type.COLOR) {
                paint.setColor(mColor);
            } else if (mType == Type.BITMAP) {
                BitmapShader shader = new BitmapShader(mBitmap, mTileX, mTileY);
                shader.setLocalMatrix(matrix);
                paint.setShader(shader);
            }
        }

        private void setColor(int color) {
            mType = Type.COLOR;
            mColor = color;
        }

        private void setColor(Bitmap bitmap) {
            mType = Type.BITMAP;
            mBitmap = bitmap;
        }

        private void setColor(Bitmap bitmap, Shader.TileMode tileX, Shader.TileMode tileY) {
            mType = Type.BITMAP;
            mBitmap = bitmap;
            mTileX = tileX;
            mTileY = tileY;
        }

        public int getColor() {
            return mColor;
        }

        public Bitmap getBitmap() {
            return mBitmap;
        }

        public Type getType() {
            return mType;
        }

        public GraffitiColor copy() {
            GraffitiColor color = null;
            if (mType == Type.COLOR) {
                color = new GraffitiColor(mColor);
            } else {
                color = new GraffitiColor(mBitmap);
            }
            color.mTileX = mTileX;
            color.mTileY = mTileY;
            return color;
        }
    }


    // ===================== api ==============

    /**
     * ����
     */
    public void save() {
        mGraffitiListener.onSaved(mGraffitiBitmap, mBitmapEraser);
    }

    /**
     * ����
     */
    public void clear() {
        mPathStack.clear();
        initCanvas();
        invalidate();
    }

    /**
     * ����
     */
    public void undo() {//�ӵ����б���ȥ������ӵ�undo list��
        if (mPathStack.size() > 0) {
            undoStack.add(mPathStack.get(mPathStack.size() - 1));//��ӵ�undo�б�
            mPathStack.remove(mPathStack.size() - 1);//�Ƴ��б�
            initCanvas();
            draw(mBitmapCanvas, mPathStack);
            invalidate();
        }
    }
    public void restore(){//��undo listȥ������ӵ� mPathStack��
        if (undoStack.size() > 0) {
            mPathStack.add(undoStack.get(undoStack.size() - 1));//��ӵ��б���
            undoStack.remove(undoStack.size() - 1);//�����б����Ƴ�
            initCanvas();
            draw(mBitmapCanvas, mPathStack);
            invalidate();
        }
    }
    /**
     * �Ƿ����޸�
     */
    public boolean isModified() {
        return mPathStack.size() != 0;
    }

    /**
     * ����ͼƬ
     */
    public void centrePic() {
        mScale = 1;
        // ����ͼƬ
        mTransX = 0;
        mTransY = 0;
        judgePosition();
        invalidate();
    }

    /**
     * ֻ����ԭͼ
     *
     * @param justDrawOriginal
     */
    public void setJustDrawOriginal(boolean justDrawOriginal) {
        isJustDrawOriginal = justDrawOriginal;
        invalidate();
    }

    public boolean isJustDrawOriginal() {
        return isJustDrawOriginal;
    }

    /**
     * ���û��ʵ�ɫ
     *
     * @param color
     */
    public void setColor(int color) {
        mColor.setColor(color);
        invalidate();
    }

    public void setColor(Bitmap bitmap) {
        if (mBitmap == null) {
            return;
        }
        mColor.setColor(bitmap);
        invalidate();
    }

    public void setColor(Bitmap bitmap, Shader.TileMode tileX, Shader.TileMode tileY) {
        if (mBitmap == null) {
            return;
        }
        mColor.setColor(bitmap, tileX, tileY);
        invalidate();
    }

    public GraffitiColor getGraffitiColor() {
        return mColor;
    }

    /**
     * ���ű�����ͼƬ��ʵ�����ű���Ϊ mPrivateScale*mScale
     *
     * @param scale
     */
    public void setScale(float scale) {
        this.mScale = scale;
        judgePosition();
        resetMatrix();
        invalidate();
    }

    public float getScale() {
        return mScale;
    }

    /**
     * ���û���
     *
     * @param pen
     */
    public void setPen(Pen pen) {
        if (pen == null) {
            throw new RuntimeException("Pen can't be null");
        }
        mPen = pen;
        resetMatrix();
        invalidate();
    }

    public Pen getPen() {
        return mPen;
    }

    /**
     * ���û�����״
     *
     * @param shape
     */
    public void setShape(Shape shape) {
        if (shape == null) {
            throw new RuntimeException("Shape can't be null");
        }
        mShape = shape;
        invalidate();
    }

    public Shape getShape() {
        return mShape;
    }

    public void setTrans(float transX, float transY) {
        mTransX = transX;
        mTransY = transY;
        judgePosition();
        resetMatrix();
        invalidate();
    }

    /**
     * ����ͼƬƫ��
     *
     * @param transX
     */
    public void setTransX(float transX) {
        this.mTransX = transX;
        judgePosition();
        invalidate();
    }

    public float getTransX() {
        return mTransX;
    }

    public void setTransY(float transY) {
        this.mTransY = transY;
        judgePosition();
        invalidate();
    }

    public float getTransY() {
        return mTransY;
    }


    public void setPaintSize(float paintSize) {
        mPaintSize = paintSize;
        invalidate();
    }

    public float getPaintSize() {
        return mPaintSize;
    }

    /**
     * ����ʱ��ͼƬ�������Ƿ����Ϳѻ�켣
     *
     * @param isDrawableOutside
     */
    public void setIsDrawableOutside(boolean isDrawableOutside) {
        mIsDrawableOutside = isDrawableOutside;
    }

    /**
     * ����ʱ��ͼƬ�������Ƿ����Ϳѻ�켣
     */
    public boolean getIsDrawableOutside() {
        return mIsDrawableOutside;
    }

    /**
     * ���÷Ŵ󾵵ı�������С�ڵ���0ʱ��ʾ��ʹ�÷Ŵ�������
     *
     * @param amplifierScale
     */
    public void setAmplifierScale(float amplifierScale) {
        mAmplifierScale  = amplifierScale;
        mAmplifierScale = 0;//ǿ����Ϊ0
        invalidate();
    }

    public float getAmplifierScale() {
        return mAmplifierScale;
    }

    public interface GraffitiListener {

        /**
         * ����ͼƬ
         *
         * @param bitmap       Ϳѻ���ͼƬ
         * @param bitmapEraser ��Ƥ����ͼ
         */
        void onSaved(Bitmap bitmap, Bitmap bitmapEraser);

        /**
         * ����
         *
         * @param i
         * @param msg
         */
        void onError(int i, String msg);

        /**
         * ׼�������Ѿ����
         */
        void onReady();
    }
}
