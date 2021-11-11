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
import android.widget.EditText;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class DrawView extends View {
    private static final float TOUCH_TOLERANCE = 4;
    private float lastX, lastY;
    private Path path;
    private Paint paint;
    private ArrayList<Object> noteObjects;
    private int currPaintColor;
    private int currStrokeWidth;
    private int currBgColor;
    private Bitmap bitmap;
    private Canvas _canvas;
    private Paint mBitmapPaint;     // FIXME: what is this?
    private LinearLayout drawViewLayout;

    public DrawView(Context context) {
        this(context, null);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        drawViewLayout = new LinearLayout(this.getContext());
        noteObjects = new ArrayList<>();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAlpha(0xff);                   // FIXME: what is this?

        // FIXME: testing textBoxes
        for (int i = 1; i <= 3; i++) {
            drawViewLayout.addView(createEditText(i*50, i*50, 36, "test test"));
        }
    }

    // init bitmap, canvas, and attributes
    public void init(int height, int width) {
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        _canvas = new Canvas(bitmap);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        currPaintColor = Color.BLACK;
        currStrokeWidth = 20;
        currBgColor = Color.WHITE;
    }

    public void undo() {
        if (noteObjects.size() != 0) {      // TODO: nothing-to-do case: dim the undo btn?
            noteObjects.remove(noteObjects.size() - 1);
            invalidate();
        }
    }

    // Actual drawing
    @Override
    protected void onDraw(Canvas uiCanvas) {
        super.onDraw(uiCanvas);
        uiCanvas.drawColor(currBgColor);

        for (Object object: noteObjects) {
            if (object instanceof Stroke) {
                paint.setColor(((Stroke) object).color);
                paint.setStrokeWidth(((Stroke) object).strokeWidth);
                uiCanvas.drawPath(((Stroke) object).path, paint);
            }
        }

        // FIXME: examine code below
        drawViewLayout.measure(uiCanvas.getWidth(), uiCanvas.getHeight());
        drawViewLayout.layout(50, 50, uiCanvas.getWidth(), uiCanvas.getHeight());
        drawViewLayout.draw(uiCanvas);
    }



    // Below are methods that manage touch response:

    // first, create a new Stroke and add it to strokes list
    private void touchStart(float x, float y) {
        path = new Path();
        noteObjects.add(new Stroke(currPaintColor, currStrokeWidth, path));

        path.reset();                   // remove any curve or line from the path
        path.moveTo(x, y);              // sets the start point og the line being drawn

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
                break;
            case MotionEvent.ACTION_UP:
                touchUp();
                invalidate();
                break;
        }
        return true;                        // FIXME: well?
    }

    // Below are getters and setters
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

    // EditText initializers
    private EditText createEditText(float x, float y, float textSize, String text) {
        return createEditText(x, y, 200, 300, 0,
                textSize, Color.BLUE, text);
    }

    // TODO: add border
    private EditText createEditText(float x, float y, int height, int width, float rotation,
                                    float textSize, int textColor, String text) {
        EditText ed = new EditText(this.getContext());
        ed.setVisibility(View.VISIBLE);
        ed.setBackgroundColor(Color.TRANSPARENT);
        ed.setX(x);
        ed.setY(y);
        ed.setHeight(height);
        ed.setWidth(width);
        ed.setRotation(rotation);
        ed.setTextSize(textSize);
        ed.setTextColor(textColor);
        ed.setText(text);
        return ed;
    }
}
