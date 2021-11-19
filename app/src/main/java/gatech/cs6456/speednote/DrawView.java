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
import android.widget.RelativeLayout;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;

public class DrawView extends View {
    private static final String LOG_TAG = "RICHIE";  // FIXME: debug logcat tag
    private static final float TOUCH_TOLERANCE = 4;
    private static final double TAP_WINDOW = 200;
    private static final int TAP_MOVE_DISTANCE_TOLERANCE = 10;
    private static final int TAP_COORDINATE_CALIBRATION = 15;

    private final ArrayList<NoteObjectWrap> noteObjects;
    private float lastX, lastY;
    private Path path;
    private final Paint paint;
    private int currPaintColor;
    private int currStrokeWidth;
    private int currBgColor;
    private Bitmap bitmap;
    private Canvas _canvas;
    private Paint mBitmapPaint;     // FIXME: what is this?
    private final RelativeLayout drawViewRelativeLayout;

    public DrawView(Context context) {
        this(context, null);
    }

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        drawViewRelativeLayout = new RelativeLayout(this.getContext());
        noteObjects = new ArrayList<>();
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
        _canvas = new Canvas(bitmap);
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        currPaintColor = Color.BLACK;
        currStrokeWidth = 20;
        currBgColor = Color.WHITE;
    }

    // FIXME: adapt undo for more operations?
    public void undo() {
        NoteObjectWrap removed;
        if (noteObjects.size() != 0) {      // TODO: nothing-to-do case: dim the undo btn?
            removed = noteObjects.remove(noteObjects.size() - 1);
            if (removed.getNoteObj() instanceof EditText)
                drawViewRelativeLayout.removeView((EditText) removed.getNoteObj());
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

        // FIXME: code needed for updating changes of EditText
        drawViewRelativeLayout.measure(uiCanvas.getWidth(), uiCanvas.getHeight());
        drawViewRelativeLayout.layout(50, 50, uiCanvas.getWidth(), uiCanvas.getHeight());
        drawViewRelativeLayout.draw(uiCanvas);
    }

    // The 'active pointer' is the one that drags the selected
    private int activePtrID = MotionEvent.INVALID_POINTER_ID;

    // Where the position of 'active pointer' is in the last event
    private float lastActivePtrX, lastActivePtrY, downActivePtrX, downActivePtrY;

    // Flags for ACTION_MOVE, ACTION_UP to decide how to act
    private boolean isDragging = false;
    private TOUCH_DOWN_TYPE touchDownType = TOUCH_DOWN_TYPE.UNDEFINED;

    // Flag to specify whether the object to drag is one of the selected
    private boolean toDragAnUnselected = false;

    // Index of the note object in noteObjects that contains the pointer
    private int containingObjIndex = -1;

    // Time of last 'tap event'
    private long lastTapUpTime = 0;

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
                downActivePtrX = x;
                downActivePtrY = y;
                if (isWithin(x, y, true)) {
                    touchDownType = TOUCH_DOWN_TYPE.SELECTED;
                    dragStart(ptrId);
                } else if (isWithin(x, y, false)) {
                    touchDownType = TOUCH_DOWN_TYPE.UNSELECTED;
                    dragStart(ptrId);
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
            case MotionEvent.ACTION_UP:  // ACTION_UP falls through to teardown in CANCEL
                final long duration = event.getEventTime() - event.getDownTime();
                final double distanceMoved = distanceBtw(x, y, downActivePtrX, downActivePtrY);
                if (duration <= TAP_WINDOW &&
                    distanceMoved <= TAP_MOVE_DISTANCE_TOLERANCE)
                    handleTap(event);
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

    private void handleTap(MotionEvent event) {
        // Double tap:
        //      new EditText at contact pt
        //      switch to select new EditText TODO: switch to input mode?
        if (event.getDownTime() - lastTapUpTime <= TAP_WINDOW) {
            addEditText(event.getX(), event.getY(), 36, "NEW");
            deselectAll();
            select(noteObjects.size()-1);
            lastTapUpTime = 0;                  // reset for next double tap
        }

        // Single tap: select/deselect
        else {
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
            lastTapUpTime = event.getEventTime();
        }

        // FIXME: debugging
        Log.d(LOG_TAG, "handleTap(): number of objects selected = " +
                getSizeOfNoteObjects() + " after tap");
    }

    //  1st touch down within one of the objects:
    //  Record where the ptr started
    //  Record as "active pointer" (primary ptr)
    private void dragStart(final int ptrId) {
        isDragging = true;
        lastActivePtrX = downActivePtrX;
        lastActivePtrY = downActivePtrY;
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

    private void select(final int index) {
        noteObjects.get(index).setSelected(true);
    }

    private void deselect(final int index) {
        noteObjects.get(index).setSelected(false);
    }

    // Private helper to deselect all by unsetting 'isSelected' flags
    private void deselectAll() {
        for (NoteObjectWrap objWrap: noteObjects)
            if (objWrap.isSelected())
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
        addStroke(new Stroke(currPaintColor, currStrokeWidth, path));

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


    // Private helper to add Stroke to list
    private void addStroke(Stroke stroke) {
        noteObjects.add(new NoteObjectWrap(stroke));
    }

    // Private helper to add EditText to list
    private void addEditText(final float x,
                             final float y,
                             final float textSize,
                             final String text) {
        addEditText(x, y, 200, 300, 0, textSize, Color.BLACK, text);
    }

    private void addEditText(final float x,
                             final float y,
                             final int height,
                             final int width,
                             final float rotation,
                             final float textSize,
                             final int textColor,
                             final String text) {
        EditText ed = new EditText(this.getContext());
        ed.setLayoutParams(new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        ));
        ed.setVisibility(View.VISIBLE);
        ed.setBackgroundColor(Color.TRANSPARENT);
        ed.setX(x - TAP_COORDINATE_CALIBRATION);
        ed.setY(y - TAP_COORDINATE_CALIBRATION);
        ed.setHeight(height);
        ed.setWidth(width);
        ed.setRotation(rotation);
        ed.setTextSize(textSize);
        ed.setTextColor(textColor);
        ed.setText(text);

        noteObjects.add(new NoteObjectWrap(ed));
        drawViewRelativeLayout.addView(
                (EditText) noteObjects.get(noteObjects.size()-1).getNoteObj()
        );
        invalidate();
    }

    // Pointer position checker
    // If the point is within an note object of the specified type (selected or not)
    // containingObjIndex will be set to the index of the object in noteObjects
    // FIXME:
    //  if isWithin() is only used in ACTION_DOWN, then x y args can be removed,
    //  use downActivePtrX/downActivePtrY instead
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

    private double distanceBtw(final float x1, final float y1,
                               final float x2, final float y2) {
        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
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

    // Private debug helpers
    private int getSizeOfNoteObjects() {
        int numSelected = 0;
        for (NoteObjectWrap n: noteObjects)
            if (n.isSelected())
                numSelected++;
        return numSelected;
    }
}

enum TOUCH_DOWN_TYPE {
    SELECTED,
    UNSELECTED,
    WHITESPACE,
    UNDEFINED
}
