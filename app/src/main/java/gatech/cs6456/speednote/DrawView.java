package gatech.cs6456.speednote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

public class DrawView extends View {
    private static final float TOUCH_TOLERANCE = 4;
    private float lastX, lastY;
    private Path path;
    private Paint paint;  // contains color/style info about how to draw geometries, text, bitmaps
    private ArrayList<Stroke> strokes;
    private int currPaintColor;
    private int currStrokeWidth;
    private int currBgColor;
    private Bitmap bitmap;
    private Canvas canvas;
    private Paint mBitmapPaint;    // FIXME: what is this?

    public DrawView(Context context) {
        this(context, null);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        strokes = new ArrayList<>();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAlpha(0xff);                   // FIXME: what is this?
    }

    // init bitmap, canvas, and attributes
    public void init(int height, int width) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        currPaintColor = Color.BLACK;
        currStrokeWidth = 20;
        currBgColor = Color.WHITE;
    }

    public void undo() {
        if (strokes.size() != 0) {         // TODO: handle "nothing-to-undo" case, dim the undo button?
            strokes.remove(strokes.size() - 1);
            invalidate();
        }
    }

    // actual drawing
    // FIXME: canvas vs. inCanvas?
    @Override
    protected void onDraw(Canvas inCanvas) {
        // save the curr state of canvas to draw background of canvas
        inCanvas.save();

        // now redraw
        canvas.drawColor(currBgColor);
        for (Stroke stroke: strokes) {
            paint.setColor(stroke.color);
            paint.setStrokeWidth(stroke.strokeWidth);
            canvas.drawPath(stroke.path, paint);
        }
        inCanvas.drawBitmap(bitmap, 0, 0, mBitmapPaint);        // FIXME: what does this do?
        inCanvas.restore();
    }

    // methods that manage the touch response
    // first, create a new Stroke and add it to strokes list
    private void touchStart(float x, float y) {
        path = new Path();
        strokes.add(new Stroke(currPaintColor, currStrokeWidth, path));

        // remove any curve or line from the path   FIXME: why?
        path.reset();
        // sets the start point og the line being drawn
        path.moveTo(x, y);

        // record curr coordinates
        lastX = x;
        lastY = y;
    }

    // move the stroke head (smoothly via beizer - quadTo())
    // if movement less then tolerance value, ignore
    private void touchMove(float x, float y) {
        float dx = Math.abs(x - lastX);
        float dy = Math.abs(y - lastY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            path.quadTo(lastX, lastY, (x+lastX)/2, (y+lastY)/2);
            lastX = x;
            lastY = y;
        }
    }

    // lastly, lineTo() the end position
    private void touchUp() {
        path.lineTo(lastX, lastY);
    }

    // touch event dispatcher
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                touchStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                touchMove(x, y);
                invalidate();
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
        }
        return true;                        // FIXME: well?
    }

    // getters and setters
    public Bitmap getBitmap() {     // FIXME: save?
        return this.bitmap;
    }

    public void setCurrBgColor(int color) {
        this.currBgColor = color;
    }

    public void setCurrPaintColor(int color) {
        this.currPaintColor = color;
    }

    public void setCurrStrokeWidth(int strokeWidth) {
        this.currStrokeWidth = strokeWidth;
    }
}
