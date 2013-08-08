package test.paintclient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;

import test.paintclient.util.SerialPaint;
import test.paintclient.util.SerialPath;
import test.paintclient.util.SerializablePair;


/**
 * Created by elvislee on 7/29/13.
 */
public class CanvasView extends View implements ColorPickerDialog.OnColorChangedListener {

    private final String LOG_TAG = this.getClass().getSimpleName();

    private float mSignatureWidth = 8f;
    private int mSignatureColor = Color.BLACK;
    private boolean mCapturing = true;
    private Bitmap mSignature = null;

    private static final boolean GESTURE_RENDERING_ANTIALIAS = true;
    private static final boolean DITHER_FLAG = true;

    private SerialPaint mPaint = new SerialPaint();
    private SerialPath mPath = new SerialPath();
    private SerialPath mOldPath = new SerialPath();

    private final Rect mInvalidRect = new Rect();

    private float mX;
    private float mY;

    private float mCurveEndX;
    private float mCurveEndY;

    private int mInvalidateExtraBorder = 10;

    public static final int TYPE_LINE = 0;
    public static final int TYPE_CIRCLE = 1;
    public static final int TYPE_RECT = 2;
    public static final int TYPE_CLEAR = 3;
    private int mDrawType = TYPE_LINE;

    private Context mContext;

    private Paint.Style mOldStyle;

    private Bitmap  mBitmap;
    private Canvas  mCanvas;
    private SerialPaint   mBitmapPaint;

    private ArrayList<SerializablePair> mPairsFromServer = new ArrayList<SerializablePair>();

    private PrintWriter out;

    private PathTrace mPathTrace;

    Path serverPath = new Path();
    private float mSX;
    private float mSY;

    public CanvasView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        init();
    }

    public CanvasView(Context context) {
        super(context);
        mContext = context;
        init();
    }

    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    private void init() {
        setWillNotDraw(false);

        mPaint.setAntiAlias(GESTURE_RENDERING_ANTIALIAS);
        mPaint.setColor(mSignatureColor);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(mSignatureWidth);
        mPaint.setDither(DITHER_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPath.reset();
        mBitmapPaint = new SerialPaint(Paint.DITHER_FLAG);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mSignature != null) {
            canvas.drawBitmap(mSignature, null, new Rect(0, 0, getWidth(),
                    getHeight()), null);
        } else {
            canvas.drawBitmap(mBitmap, 0, 0, mBitmapPaint);

            canvas.drawPath(mPath, mPaint);
        }

    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (mCapturing) {
            processEvent(event);
            Log.d(VIEW_LOG_TAG, "dispatchTouchEvent");
            return true;
        } else {
            return false;
        }
    }

    private boolean processEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchDown(event);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                Rect rect = null;
                if (mDrawType == TYPE_LINE)
                    rect = touchMoveLine(event);
                else if (mDrawType == TYPE_CIRCLE)
                    rect = touchMoveCircle(event);
                else if (mDrawType == TYPE_RECT)
                    rect = touchMoveRect(event);

                if (rect != null) {
                    invalidate(rect); // draw partially
                }
                return true;

            case MotionEvent.ACTION_UP:

                touchUp(event, false);
                invalidate();
                return true;

            case MotionEvent.ACTION_CANCEL:

                touchUp(event, true);
                invalidate();
                return true;

        }

        return false;

    }

    private void touchUp(MotionEvent event, boolean b) {
        // commit the path to our offscreen
        mCanvas.drawPath(mPath, mPaint);
        Gson gson = new Gson();

        if (out != null) {
            String jsonRepresentation = gson.toJson(mPathTrace);

            out.println(jsonRepresentation);
            out.flush();


        } else {
            Log.i(LOG_TAG, "writer null");
        }
        mPath.reset();
    }

    private Rect touchMoveCircle(MotionEvent event) {
        Rect areaToRefresh = mInvalidRect;
        mPath.set(mOldPath);

        final float x = event.getX();
        final float y = event.getY();

        mPathTrace.xS.add(x);
        mPathTrace.yS.add(y);

        final int border = mInvalidateExtraBorder;

        double dX = x - mX;
        double dY = y - mY;

        float rX = (mX + x) / 2;

        float rY = (int) (mY + y) / 2;
        int radius = (int) Math.sqrt(Math.pow(dX, 2) + Math.pow(dY, 2)) / 2;

        areaToRefresh.union((int) rX - radius - border, (int) rY + radius + border);
        areaToRefresh.union((int) rX + radius + border, (int) rY - radius - border);

        float circleX = (mX + x) / 2;
        float circleY = (mY + y) / 2;

        mPathTrace.circleX = circleX;
        mPathTrace.circleY = circleY;
        mPathTrace.radius = radius;


        mPath.addCircle(circleX, circleY, radius, Path.Direction.CW);
        return areaToRefresh;
    }

    private Rect touchMoveRect(MotionEvent event) {
        Rect areaToRefresh = mInvalidRect;
        mPath.set(mOldPath);

        final float x = event.getX();
        final float y = event.getY();

        mPathTrace.xS.add(x);
        mPathTrace.yS.add(y);

        final int border = mInvalidateExtraBorder;

        areaToRefresh.union((int) x - border, (int) y - border, (int) x
                + border, (int) y + border);


        float left = Math.min(mX, x);
        float top = Math.min(mY, y);
        float right = Math.max(mX, x);
        float bottom = Math.max(mY, y);

        mPathTrace.left = left;
        mPathTrace.top = top;
        mPathTrace.right = right;
        mPathTrace.bottom = bottom;

        mPath.addRect(left, top, right, bottom, Path.Direction.CW);
        return areaToRefresh;
    }

    private Rect touchMoveLine(MotionEvent event) {
        Rect areaToRefresh = null;

        final float x = event.getX();
        final float y = event.getY();

        mPathTrace.xS.add(x);
        mPathTrace.yS.add(y);

        final float previousX = mX;
        final float previousY = mY;

        areaToRefresh = mInvalidRect;

        // start with the curve end
        final int border = mInvalidateExtraBorder;
        areaToRefresh.set((int) mCurveEndX - border, (int) mCurveEndY - border,
                (int) mCurveEndX + border, (int) mCurveEndY + border);

        float cX = mCurveEndX = (x + previousX) / 2;
        float cY = mCurveEndY = (y + previousY) / 2;

        mPath.quadTo(previousX, previousY, cX, cY);

        // union with the control point of the new curve
        areaToRefresh.union((int) previousX - border, (int) previousY - border,
                (int) previousX + border, (int) previousY + border);

        // union with the end point of the new curve
        areaToRefresh.union((int) cX - border, (int) cY - border, (int) cX
                + border, (int) cY + border);

        mX = x;
        mY = y;

        return areaToRefresh;

    }

    private void touchDown(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        mPathTrace = new PathTrace();
        mPathTrace.cmd = mDrawType;
        mPathTrace.color = mPaint.getColor();

        mPathTrace.xS.add(x);
        mPathTrace.yS.add(y);

        mOldPath.set(mPath);

        mX = x;
        mY = y;
        mPath.moveTo(x, y);

        final int border = mInvalidateExtraBorder;
        mInvalidRect.set((int) x - border, (int) y - border, (int) x + border,
                (int) y + border);

        mCurveEndX = x;
        mCurveEndY = y;
    }


    /**
     * Erases the signature.
     */
    public void clear() {
        mSignature = null;
        mPath.rewind();
        // Repaints the entire view.
        invalidate();
    }

    public boolean isCapturing() {
        return mCapturing;
    }

    public void setIsCapturing(boolean mCapturing) {
        this.mCapturing = mCapturing;
    }

    public void setSignatureBitmap(Bitmap signature) {
        mSignature = signature;
        invalidate();
    }

    public Bitmap getSignatureBitmap() {
        if (mSignature != null) {
            return mSignature;
        } else if (mPath.isEmpty()) {
            return null;
        } else {
            Bitmap bmp = Bitmap.createBitmap(getWidth(), getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(bmp);
            c.drawPath(mPath, mPaint);
            return bmp;
        }
    }

    public void setSignatureWidth(float width) {
        mSignatureWidth = width;
        mPaint.setStrokeWidth(mSignatureWidth);
        invalidate();
    }

    public float getSignatureWidth(){
        return mPaint.getStrokeWidth();
    }

    public void setSignatureColor(int color) {
        mSignatureColor = color;
    }

    /**
     * @return the byte array representing the signature as a PNG file format
     */
    public byte[] getSignaturePNG() {
        return getSignatureBytes(CompressFormat.PNG, 0);
    }

    /**
     * @param quality Hint to the compressor, 0-100. 0 meaning compress for small
     *            size, 100 meaning compress for max quality.
     * @return the byte array representing the signature as a JPEG file format
     */
    public byte[] getSignatureJPEG(int quality) {
        return getSignatureBytes(CompressFormat.JPEG, quality);
    }

    private byte[] getSignatureBytes(CompressFormat format, int quality) {
        Log.d(LOG_TAG, "getSignatureBytes() path is empty: " + mPath.isEmpty());
        Bitmap bmp = getSignatureBitmap();
        if (bmp == null) {
            return null;
        } else {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();

            getSignatureBitmap().compress(format, quality, stream);

            return stream.toByteArray();
        }
    }

    public void setDrawType(int type) {
        mDrawType = type;
    }

    public void showColorPicker() {
        new ColorPickerDialog(mContext, this, mPaint.getColor()).show();
    }

    @Override
    public void colorChanged(int color) {
        mPaint.setColor(color);
    }

    public void setPaintStyle(Paint.Style style) {
        mPaint.setStyle(style);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        mCanvas = new Canvas(mBitmap);
    }

    public void addServerPath(PathTrace trace) {
        Log.i("Elvis", "addPathPaintPair");
        if (trace == null)
            return;

        int userColor = mPaint.getColor();
        int userType = mDrawType;
        Paint.Style userStyle = mPaint.getStyle();

        mPaint.setColor(trace.color);
        mDrawType = trace.cmd;

        mSX = trace.xS.get(0);
        mSY = trace.yS.get(0);

        serverPath.moveTo(mSX, mSY);


        if (trace.cmd == TYPE_LINE) {
            mPaint.setStyle(Paint.Style.STROKE);
            drawServerLine(trace);
        } else if (trace.cmd == TYPE_CIRCLE) {
            mPaint.setStyle(Paint.Style.FILL);
            serverPath.addCircle(trace.circleX, trace.circleY, trace.radius, Path.Direction.CW);
        } else if (trace.cmd == TYPE_RECT) {
            mPaint.setStyle(Paint.Style.FILL);
            serverPath.addRect(trace.left, trace.top, trace.right, trace.bottom, Path.Direction.CW);
        }

        mCanvas.drawPath(serverPath, mPaint);

        mPaint.setColor(userColor);
        mDrawType = userType;
        mPaint.setStyle(userStyle);
        serverPath.reset();
        invalidate();

    }

    private void drawServerLine(PathTrace trace) {

        for (int i = 1; i < trace.xS.size(); ++i) {
            final float x = trace.xS.get(i);
            final float y = trace.yS.get(i);

            final float previousX = mSX;
            final float previousY = mSY;

            float cX = (x + previousX) / 2;
            float cY = (y + previousY) / 2;

            serverPath.quadTo(previousX, previousY, cX, cY);


            mSX = x;
            mSY = y;
        }


    }



    public void setWriter(PrintWriter out) {
        this.out = out;
    }

    public void eraseCanvas() {
        mCanvas.drawColor(Color.WHITE);
        invalidate();
    }

}
