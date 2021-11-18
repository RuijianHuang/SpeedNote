package gatech.cs6456.speednote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;

public class DrawView extends View {
    private static final float TOUCH_TOLERANCE = 4;
    private static final double TAP_WINDOW = 200;
    private static final String LOG_TAG = "RICHIE";  // FIXME: debug logcat tag

    private float lastX, lastY;
    private Path path;
    private final Paint paint;
    private final ArrayList<NoteObjectWrap> noteObjects;
    private int currPaintColor;
    private int currStrokeWidth;
    private int currBgColor;
    private Bitmap bitmap;
    private Canvas _canvas;
    private Paint mBitmapPaint;     // FIXME: what is this?
    private final LinearLayout drawViewLayout;

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

    // FIXME: adapt undo for more operations?
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

        for (NoteObjectWrap objWrap: noteObjects) {
            if (objWrap.getNoteObj() instanceof Stroke) {
                Stroke stk = (Stroke) objWrap.getNoteObj();
                paint.setColor(stk.color);
                paint.setStrokeWidth(stk.strokeWidth);
                uiCanvas.drawPath(stk.path, paint);
            }
        }

        // FIXME: examine code below
        drawViewLayout.measure(uiCanvas.getWidth(), uiCanvas.getHeight());
        drawViewLayout.layout(50, 50, uiCanvas.getWidth(), uiCanvas.getHeight());
        drawViewLayout.draw(uiCanvas);
    }


    // The 'active pointer' is the one that drags the selected
    private int activePtrID = MotionEvent.INVALID_POINTER_ID;

    // Where the position of 'active pointer' is in the last event
    private float lastActivePtrX, lastActivePtrY;

    // Flags for ACTION_MOVE, ACTION_UP to decide how to act
    private boolean isDragging = false;
    private TOUCH_DOWN_TYPE touchDownType = TOUCH_DOWN_TYPE.UNDEFINED;

    // Flag to specify whether the object to drag is one of the selected
    private boolean toDragAnUnselected = false;

    // Index of the note object in noteObjects that contains the pointer
    private int containingObjIndex = -1;

    // touch event dispatcher
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int ptrCount = event.getPointerCount();
        final int ptrIndex = event.getActionIndex();
        final int ptrId = event.getPointerId(ptrIndex);
        final int ptrType = event.getToolType(ptrIndex);
        final float x = event.getX();
        final float y = event.getY();

        if (ptrCount == 1 && ptrType == MotionEvent.TOOL_TYPE_STYLUS) {
            draw(event);
            return true;
        }

        // Drag:
        //      if 1st contact pt is in one of the selected
        //          drag all the selected
        //      if 1st contact pt is in one of the not-selected
        //          deselect all, select & drag that one
        //      if 1st contact pt is in whitespace
        //          do not drag
        // Select:
        //      if tap one of the not-selected, select that one
        //      if tap one of the selected, deselect that one
        //      if tap on white space, deselect all
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (isWithin(x, y, true)) {
                    touchDownType = TOUCH_DOWN_TYPE.SELECTED;
                    dragStart(x, y, ptrId);
                } else if (isWithin(x, y, false)) {
                    touchDownType = TOUCH_DOWN_TYPE.UNSELECTED;
                    dragStart(x, y, ptrId);
                    toDragAnUnselected = true;
                } else {
                    touchDownType = TOUCH_DOWN_TYPE.WHITESPACE;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (isDragging)
                    dragMove(event);
                break;

            // TODO: Multitouch scenarios
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            case MotionEvent.ACTION_UP:             // ACTION_UP falls through to teardown in CANCEL
                final long duration = event.getEventTime() - event.getDownTime();
                if (duration <= TAP_WINDOW)
                    handleTap();
            case MotionEvent.ACTION_CANCEL:
                teardown();
                break;
        }
        return true;
    }

    private void teardown() {
        touchDownType = TOUCH_DOWN_TYPE.UNDEFINED;
        containingObjIndex = -1;
        if (isDragging)
            dragEnd();
    }
    private void handleTap() {
        switch (touchDownType) {
            case UNSELECTED:                // select the unselected
                deselectAll();
                select(containingObjIndex);
                break;
            case SELECTED:                  // deselected the selected
                deselect(containingObjIndex);
                break;
            case WHITESPACE:
                deselectAll();
                break;
            case UNDEFINED:
                throw new IllegalArgumentException("UNDEFINED touchDownType in handleTap");
        }

        // FIXME: debugging
        int numSelected = 0;
        for (NoteObjectWrap n: noteObjects)
            if (n.isSelected())
                numSelected++;
        Log.d(LOG_TAG, "number of objects selected = " + numSelected);
    }

    //  1st touch down within one of the objects:
    //  Record where the ptr started
    //  Record as "active pointer" (primary ptr)
    private void dragStart(final float x, final float y, final int ptrId) {
        isDragging = true;
        lastActivePtrX = x;
        lastActivePtrY = y;
        activePtrID = ptrId;
    }

    private void dragMove(MotionEvent event) {
        final int activePtrIndex = event.findPointerIndex(activePtrID);
        final float currActivePtrX = event.getX(activePtrIndex);
        final float currActivePtrY = event.getY(activePtrIndex);
        final float dx = currActivePtrX - lastActivePtrX;
        final float dy = currActivePtrY - lastActivePtrY;

        if (toDragAnUnselected) {
            deselectAll();
            select(containingObjIndex);
        }

        for (NoteObjectWrap objWrap: noteObjects)
            if (objWrap.isSelected())
                objWrap.moveBy(dx, dy);

        invalidate();
        lastActivePtrX = currActivePtrX;
        lastActivePtrY = currActivePtrY;
    }

    private void dragEnd() {
        activePtrID = MotionEvent.INVALID_POINTER_ID;
        isDragging = false;
    }

    // Pointer position checker
    // If the point is within an note object of the specified type (selected or not)
    // containingObjIndex will be set to the index of the object in noteObjects
    @RequiresApi(api = Build.VERSION_CODES.O)
    private boolean isWithin(final float x, final float y, boolean isSelectedList) {
        for (NoteObjectWrap objWrap: noteObjects) {
            if (isSelectedList != objWrap.isSelected())
                continue;
            if (objWrap.isWithin(x, y)) {
                containingObjIndex = noteObjects.indexOf(objWrap);
                return true;
            }
        }
        return false;
    }

    private void select(final int index) {
        noteObjects.get(containingObjIndex).setSelected(true);
    }

    private void deselect(final int index) {
        noteObjects.get(containingObjIndex).setSelected(false);
    }

    // Private helper to deselect all by unsetting 'isSelected' flags
    private void deselectAll() {
        for (NoteObjectWrap objWrap: noteObjects)
            objWrap.setSelected(false);
    }



    // Single stylus drawing helper methods
    private void draw(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // TODO: deselect all selected
                drawStart(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                drawMove(x, y);
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                drawEnd();
                invalidate();
                break;
        }
    }
    // first, create a new Stroke and add it to strokes list
    private void drawStart(float x, float y) {
        path = new Path();
        noteObjects.add(new NoteObjectWrap(
                new Stroke(currPaintColor, currStrokeWidth, path)
        ));

        path.reset();                   // remove any curve or line from the path
        path.moveTo(x, y);              // sets the start point og the line being drawn

        // record curr coordinates
        lastX = x;
        lastY = y;
    }

    // move the stroke head (smoothly via beizer - quadTo())
    // if movement less then tolerance value, ignore
    private void drawMove(float x, float y) {
        float dx = Math.abs(x - lastX);
        float dy = Math.abs(y - lastY);

        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            path.quadTo(lastX, lastY, (x+lastX)/2, (y+lastY)/2);
            lastX = x;
            lastY = y;
        }
    }

    // lastly, lineTo() the end position
    private void drawEnd() {
        path.lineTo(lastX, lastY);
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
}

enum TOUCH_DOWN_TYPE {
    SELECTED,
    UNSELECTED,
    WHITESPACE,
    UNDEFINED
}
