package cz.hrdinajan.perfecthour;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.TreeSet;

/**
 * TODO: document your custom view class.
 */
public class HourDial extends View {

    public interface MinPointsChangeListener {
        void onChange(TreeSet<Integer> minPoints);
    }

    private static final int[] colors = new int[] {
            Color.parseColor("#00bcd4"),
            Color.parseColor("#ffc107"),
            Color.parseColor("#f44336")
    };

    private final static float relSize = 100;
    private final static float hw = 7, hh = 7, ht = 2;
    private final static float hts = 4;
    private final static float tw = 7, th = 6;
    private final static float bt = 8;
    private final static float mmw = 1, mmh = 7, mw = 0.5f, mh = 6;

    private TreeSet<Integer> mMinPoints;
    private int movedStop = -1;
    private int movedNewStop = -1;
    private MinPointsChangeListener minPointsChangeListener;

    public TreeSet<Integer> getStops() {
        return mMinPoints;
    }

    public void setStops(TreeSet<Integer> mMinPoints) {
        this.mMinPoints = mMinPoints;
        invalidate();
    }

    public HourDial(Context context) {
        super(context);
        init(null, 0);
    }

    public HourDial(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public HourDial(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.HourDial, defStyle, 0);

        mMinPoints = new TreeSet<>();
        mMinPoints.add(0);
        mMinPoints.add(50);

        a.recycle();
    }

    public void setMinPointsChangeListener(MinPointsChangeListener minPointsChangeListener) {
        this.minPointsChangeListener = minPointsChangeListener;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final int action = event.getActionMasked();

        switch(action) {
            case MotionEvent.ACTION_DOWN:
                for (int minuteStop : mMinPoints) {
                    float origPoint[] = new float[2];
                    getTriangleMatrix(minuteStop, true).mapPoints(origPoint, new float[]{event.getX(), event.getY()});
                    if ((new RectF(-tw/1.5f, -2*th, tw/1.5f, th)).contains(origPoint[0], origPoint[1])) {
                        // This minuteStop move has started!
                        movedStop = minuteStop;
                        movedNewStop = minuteStop;
                        invalidate();
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (movedStop != -1) {
                    float origPoint[] = new float[2];
                    getBaseMatrix(true).mapPoints(origPoint, new float[]{event.getX(), event.getY()});
                    movedNewStop = Math.round((computeAngle(origPoint[0], origPoint[1]) + 90) * 60 / 360) % 60;
                    invalidate();
                }

                break;
            case MotionEvent.ACTION_UP:
                mMinPoints.remove(movedStop);
                // TODO: Check for existing stops
                mMinPoints.add(movedNewStop);
                movedStop = -1;
                movedNewStop = -1;
                if (minPointsChangeListener != null) {
                    minPointsChangeListener.onChange(mMinPoints);
                }
                invalidate();
                break;
        }

        return super.onTouchEvent(event);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float br = relSize/2 - hh - hts - th - bt / 2;

        canvas.concat(getBaseMatrix(false));

        Paint mPaint = new Paint();
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setAntiAlias(true);

        for (int min = 0; min < 60; min += 5) {
            canvas.save();
            canvas.rotate(360/60 * min);
            if (min % 15 == 0) {
                mPaint.setStrokeWidth(mmw);
                canvas.drawLine(0, -br, 0, -br + mmh, mPaint);
            } else {
                mPaint.setStrokeWidth(mw);
                canvas.drawLine(0, -br, 0, -br + mh, mPaint);
            }
            canvas.restore();
        }

        // Triangles/bars

        Paint trianglePaint = new Paint();
        trianglePaint.setColor(Color.WHITE);
        trianglePaint.setStyle(Paint.Style.FILL);
        trianglePaint.setAntiAlias(true);
        trianglePaint.setDither(true);

        Path trianglePath = new Path();
        trianglePath.moveTo(0, 0);
        trianglePath.lineTo(-tw/2, -th);
        trianglePath.lineTo(tw/2, -th);
        trianglePath.lineTo(0, 0);
        trianglePath.close();

        Paint p = new Paint();
        int colorIndex = 0;
        p.setColor(colors[colorIndex]);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(bt);
        p.setAntiAlias(true);

        Matrix pathMatrix = new Matrix();

        TreeSet<Integer> drawnMinPoints;
        if (movedStop != -1) {
            drawnMinPoints =  new TreeSet<>(mMinPoints);
            drawnMinPoints.remove(movedStop);
            drawnMinPoints.add(movedNewStop);
        } else {
            drawnMinPoints = mMinPoints;
        }

        int prevMin = -1;
        for (int minuteStop : drawnMinPoints) {
            if (movedStop == minuteStop) {
                minuteStop = movedNewStop;
            }

            // Triangle
            canvas.save();

            canvas.rotate(360 / 60 * minuteStop);
            canvas.translate(0, -(br + bt/2));
            canvas.drawPath(trianglePath, trianglePaint);

            canvas.restore();

            // Arc
            if (prevMin != -1) {
                canvas.drawArc(new RectF(-br, -br, br, br), -90 + 360/60 * prevMin, 360 / 60 * (minuteStop - prevMin), false, p);

                colorIndex = (colorIndex + 1) % colors.length;
                p.setColor(colors[colorIndex]);
            }

            prevMin = minuteStop;
        }

        if (prevMin != -1 && drawnMinPoints.size() != 0) {
            if (colorIndex == 0) {
                colorIndex++;
                p.setColor(colors[colorIndex]);
            }

            canvas.drawArc(new RectF(-br, -br, br, br), -90 + 360/60 * prevMin, 360 / 60 * (60 - prevMin + drawnMinPoints.first()), false, p);
        }
    }

    private Matrix getBaseMatrix(boolean inverse) {
        float size = Math.min(getWidth(), getHeight());


        Matrix m = new Matrix();
        m.preTranslate(getWidth() / 2, getHeight() / 2);
        m.preScale(size / relSize, size / relSize);

        if (inverse) {
            //Matrix inv = new Matrix();
            m.invert(m);
            //return m;
        }
        return m;
    }

    private Matrix getTriangleMatrix(int minuteStop, boolean inverse) {
        float br = relSize/2 - hh - hts - th - bt / 2;
        Matrix m = getBaseMatrix(false);
        m.preRotate(360 / 60 * minuteStop);
        m.preTranslate(0, -(br + bt/2));
        if (inverse) {
            m.invert(m);
        }
        return m;
    }

    private static float computeAngle(float x, float y) {
        if      (x > 0) return (float)Math.toDegrees(Math.atan(y/x));
        else if (x < 0) return (float)Math.toDegrees(Math.atan(y/x)) + 180;
        else { // x == 0
            if (y > 0) return 90;
            else return 0;
        }
    }
}
