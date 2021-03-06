package gatech.cs6456.speednote;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.Build;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.RelativeLayout;

import androidx.annotation.RequiresApi;

import java.util.ArrayList;

public class DrawView extends View {
    private static final String LOG_TAG = "SPEEDNOTE_DEBUG";  // FIXME: debug logcat tag
    private static final float TOUCH_TOLERANCE = 4;
    private static final double TAP_WINDOW = 300;    // in milliseconds
    private static final int TAP_MOVE_DISTANCE_TOLERANCE = 30;
    private static final int TAP_COORDINATE_CALIBRATION = 15;
    private static final int WRAP_PADDING = 15;
    private static final int DELETE_BOUNDARY = 300;

    private final ArrayList<NoteObjectWrap> noteObjects;
    private final ArrayList<PointerDescriptor> gesturePointers;
    private float lastX, lastY;
    private Path path;
    private final Paint paint;
    private int currPaintColor;
    private int currStrokeWidth;
    private int currBgColor;
    private final RectF roundRectWrap;
    private Bitmap bitmap;
    private Canvas _canvas;
    private Paint mBitmapPaint;     // FIXME: what is this?
    private final RelativeLayout drawViewRelativeLayout;
    private final Matrix mMatrix;
    private final InputMethodManager imm;
    private boolean keyboard_status;

    public DrawView(Context context) {
        this(context, null);
    }
    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);
        drawViewRelativeLayout = new RelativeLayout(this.getContext());
        mMatrix = new Matrix();
        noteObjects = new ArrayList<>();
        gesturePointers = new ArrayList<>();
        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setDither(true);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAlpha(0xff);
        roundRectWrap = new RectF();
        imm = (InputMethodManager) this.getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        keyboard_status = false;

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
        uiCanvas.save();
        uiCanvas.concat(mMatrix);

        uiCanvas.drawColor(currBgColor);
        if (isDragToDelete) {
            paint.setColor(Color.RED);
            paint.setStyle(Paint.Style.FILL);
            paint.setAlpha(150);
            uiCanvas.drawRect(0, this.getHeight()-DELETE_BOUNDARY,
                    this.getWidth(), this.getHeight(), paint);
        } else {
            paint.setAlpha(0xff);
        }

        // These 2 lines 'make' all EditTexts viewable
        drawViewRelativeLayout.measure(uiCanvas.getWidth(), uiCanvas.getHeight());
        drawViewRelativeLayout.layout(50, 50, uiCanvas.getWidth(), uiCanvas.getHeight());

        paint.setStyle(Paint.Style.STROKE);
        for (NoteObjectWrap objWrap: noteObjects) {
            if (objWrap.getNoteObj() instanceof Stroke) {
                Stroke stk = (Stroke) objWrap.getNoteObj();
                paint.setColor(stk.color);
                paint.setStrokeWidth(stk.strokeWidth);
                if (objWrap.isSelected())
                    paint.setPathEffect(new DashPathEffect(new float[] {20f, stk.strokeWidth*1.3f}, 0f));
                else
                    paint.setPathEffect(new PathEffect());
                uiCanvas.drawPath(stk.path, paint);
            } else if (objWrap.getNoteObj() instanceof EditText) {
                EditText ed = (EditText) objWrap.getNoteObj();
                roundRectWrap.set(ed.getX() - WRAP_PADDING, ed.getY(),
                                  ed.getX() + ((float) ed.getWidth()) + WRAP_PADDING,
                                  ed.getY() + ((float) ed.getHeight()));
                if (objWrap.isSelected())
                    paint.setPathEffect(new DashPathEffect( new float[] {20f, 20f}, 0f ));
                else
                    paint.setPathEffect(new PathEffect());

                paint.setStrokeWidth(2);
                uiCanvas.drawRoundRect(roundRectWrap, 20f, 20f, paint);
            }
        }

        drawViewRelativeLayout.draw(uiCanvas);
        uiCanvas.restore();
    }

    // Flags for ACTION_MOVE, ACTION_UP to decide how to act
    private boolean isDragging = false;

    private boolean isCopying = false;

    private boolean isDragToDelete = false;

    // Index of the note object in noteObjects that contains the pointer
    private int containingObjIndex = -1;

    // Time of last 'tap event'
    private long lastTapUpTime = 0;

    private int editingBoxIndex = -1;

    // touch event dispatcher
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int ptrCount = event.getPointerCount();
        final int ptrIndex = event.getActionIndex();
        final int ptrId = event.getPointerId(ptrIndex);
        final int ptrType = event.getToolType(ptrIndex);
        final float x = event.getX(ptrIndex);
        final float y = event.getY(ptrIndex);
        TouchDownType touchDownType;

        // FIXME: DEBUG: finger vs. stylus events
//        Log.d(LOG_TAG, (ptrType == MotionEvent.TOOL_TYPE_STYLUS ? "STYLUS: " : "FINGER: ") +
//                MotionEvent.actionToString(event.getActionMasked()));

        updateGesturePointers(event);
        if (gesturePointers.size() == 1 && ptrType == MotionEvent.TOOL_TYPE_STYLUS) {
            deselectAll();
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
        containingObjIndex = -1;
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touchDownType = checkTouchDownType(x, y);
                gesturePointers.get(0).setTouchDownType(touchDownType);
                gesturePointers.get(0).setContainingObjIndex(containingObjIndex);

                if (touchDownType != TouchDownType.WHITESPACE &&
                    touchDownType != TouchDownType.EDITTEXT){
                    dragStart(ptrId, touchDownType==TouchDownType.UNSELECTED);
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                dragEnd();
                touchDownType = checkTouchDownType(x, y);
                gesturePointers.get(event.getActionIndex()).setTouchDownType(touchDownType);
                gesturePointers.get(event.getActionIndex()).setContainingObjIndex(containingObjIndex);
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging)
                    dragMove();

                if (gesturePointers.size() == 2) {
                    if (isCopyPaste() || isCopying) {
                        Log.d(LOG_TAG, ">>>>>>>>>>>>>> is copy paste");
                        if (!isCopying)
                            copyStart();
                        else
                            copyMove();
                    } else if (!isCopying){
                        handleScaleByPinching();
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                PointerDescriptor upPtr = gesturePointers.get(ptrIndex);
                PointerDescriptor primaryPtr = gesturePointers.get(0);

                // Primary hold + up finger tap
                if (isTap(upPtr) &&
                    primaryPtr.getTouchDownType() != TouchDownType.WHITESPACE)
                {
                    // Switch to select the obj under primaryPtr
                    if (primaryPtr.getTouchDownType() == TouchDownType.UNSELECTED) {
                        primaryPtr.setTouchDownType(TouchDownType.SELECTED);
                        deselectAll();
                        select(primaryPtr.getContainingObjIndex());
                    }
                    // Then select whichever is tapped
                    touchDownType = upPtr.getTouchDownType();
                    if (touchDownType == TouchDownType.UNSELECTED)
                        select(upPtr.getContainingObjIndex());
                    else if (touchDownType == TouchDownType.SELECTED)
                        deselect(upPtr.getContainingObjIndex());
                }

                // End copying
                if (gesturePointers.size() == 2 && isCopying)
                    copyEnd();

                // Update gesturePointers: remove upPtr
                gesturePointers.remove(upPtr);
                break;

            // ACTION_UP falls through to teardown in CANCEL
            case MotionEvent.ACTION_UP:
                if (event.getToolType(ptrIndex) == MotionEvent.TOOL_TYPE_FINGER &&
                    isTap(gesturePointers.get(0)))
                    handleTap(event);
            case MotionEvent.ACTION_CANCEL:
                teardown();
                break;
        }

        // FIXME: DEBUG: print all ptrs
        if (event.getActionMasked() != MotionEvent.ACTION_MOVE)
            printAllPtrs(event);
        return true;
    }

    private void updateGesturePointers(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (!gesturePointers.isEmpty())         // Sanity check
                    throw new IllegalStateException("DrawView: " +
                            "OnTouchEvent(): on ACTION_DOWN gesturePointers " +
                            "should be empty");
                gesturePointers.add(new PointerDescriptor(event));
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if (gesturePointers.isEmpty())
                    throw new IllegalStateException("DrawView: " +
                            "updateGesturePointers(): on ACTION_POINTER_DOWN" +
                            " gesturePointers should NOT be empty");
                gesturePointers.add(new PointerDescriptor(event));
                break;
            case MotionEvent.ACTION_MOVE:
                for (PointerDescriptor p: gesturePointers) {
                    p.update(event);
                }
            case MotionEvent.ACTION_POINTER_UP:
                break;
            case MotionEvent.ACTION_UP:
                if (gesturePointers.size() != 1)
                    throw new IllegalStateException("DrawView: updateGesturePointers: " +
                            "size of gesturePointers should be 1 upon ACTION_UP");
                if (gesturePointers.get(0).getToolType() == MotionEvent.TOOL_TYPE_STYLUS)
                    gesturePointers.remove(0);
                else
                    gesturePointers.get(0).update(event);
                break;
        }
    }

    private void teardown() {
        gesturePointers.clear();
        containingObjIndex = -1;
        if (isDragging) dragEnd();
    }

    private void copyStart() {
        ArrayList<NoteObjectWrap> copyBuffer = new ArrayList<>();
        for (NoteObjectWrap objectWrap: noteObjects) {
            if (objectWrap.isSelected()) {
                copyBuffer.add(new NoteObjectWrap(objectWrap));
                objectWrap.setSelected(false);
            }
        }
        for (NoteObjectWrap objectWrap: copyBuffer)
            if (objectWrap.getNoteObj() instanceof EditText)
                drawViewRelativeLayout.addView((EditText) objectWrap.getNoteObj());
        noteObjects.addAll(copyBuffer);
        isCopying = true;
    }

    private void copyMove() {
        PointerDescriptor secondaryPtr = gesturePointers.get(1);
        for (NoteObjectWrap objWrap: noteObjects)
            if (objWrap.isSelected())
                objWrap.moveBy(secondaryPtr.getDeltaLastX(),
                        secondaryPtr.getDeltaLastY());
        invalidate();
    }

    private void copyEnd() {
        isCopying = false;
    }

    private void handleScaleByPinching() {
        double currXDistance, currYDistance, lastXDistance, lastYDistance;
        float[] p0LastPos, p1LastPos;
        PointerDescriptor p0 = gesturePointers.get(0);
        PointerDescriptor p1 = gesturePointers.get(1);

        if (p0.getTouchDownType() == TouchDownType.SELECTED ||
                p1.getTouchDownType() == TouchDownType.SELECTED)
            return;

        ArrayList<float[]> p0h = p0.getHistory();
        ArrayList<float[]> p1h = p1.getHistory();
        if (!p0.isUpdateConsumed()) {
            p0LastPos = p0h.get(p0h.size()-2);
            p0.setUpdateConsumed(true);
        } else {
            p0LastPos = p0h.get(p0h.size()-1);
        }
        if (!p1.isUpdateConsumed()) {
            p1LastPos = p1h.get(p1h.size()-2);
            p1.setUpdateConsumed(true);
        } else {
            p1LastPos = p1h.get(p1h.size()-1);
        }
        lastXDistance = Math.abs(p0LastPos[0] - p1LastPos[0]);
        lastYDistance = Math.abs(p0LastPos[1] - p1LastPos[1]);
        currXDistance = Math.abs(p0h.get(p0h.size()-1)[0] - p1h.get(p1h.size()-1)[0]);
        currYDistance = Math.abs(p0h.get(p0h.size()-1)[1] - p1h.get(p1h.size()-1)[1]);

        for (NoteObjectWrap objWrap: noteObjects) {
            if (objWrap.isSelected())
                objWrap.scale(currXDistance, lastXDistance, currYDistance, lastYDistance);
        }
        invalidate();
    }

    private void handleTap(MotionEvent event) {
        PointerDescriptor primaryPtr = gesturePointers.get(0);
        // Double tap:
        //      new EditText at contact pt
        //      switch to select new EditText
        if (event.getDownTime() - lastTapUpTime <= TAP_WINDOW) {
            switch (primaryPtr.getTouchDownType()) {
                case UNSELECTED:
                case WHITESPACE:
                    addEditText(event.getX(), event.getY(), 20, "");
                    deselectAll();
                    enterEditMode(noteObjects.size()-1);
                    break;
                case UNDEFINED:
                    throw new IllegalArgumentException("UNDEFINED touchDownType in handleTap()");
            }
            lastTapUpTime = 0;                  // reset for next double tap
        }

        // Single tap: select/deselect
        else {
            boolean potentialDoubleTap = false;
            switch (primaryPtr.getTouchDownType()) {
                case EDITTEXT:
                    deselectAll();
                    enterEditMode(primaryPtr.getContainingObjIndex());
                    break;
                case UNSELECTED:                // select the unselected
                    deselectAll();
                    exitEditMode();
                    select(primaryPtr.getContainingObjIndex());
                    break;
                case SELECTED:                  // deselected the selected
                    deselect(primaryPtr.getContainingObjIndex());
                    break;
                case WHITESPACE:
                    Log.d(LOG_TAG, "whitespace handler");
                    potentialDoubleTap = true;
                    deselectAll();
                    exitEditMode();
                    break;
                case UNDEFINED:
                    throw new IllegalArgumentException("UNDEFINED touchDownType in handleTap");
            }
            lastTapUpTime = potentialDoubleTap ? event.getEventTime() : 0;
        }
    }

    private void dragStart(final int ptrId, boolean toDragAnUnselected) {
        isDragging = true;
        exitEditMode();
        if (toDragAnUnselected) {
            deselectAll();
            select(containingObjIndex);
        }
    }

    private void dragMove() {
        PointerDescriptor primaryPtr = gesturePointers.get(0);
        for (NoteObjectWrap objWrap: noteObjects) {
            if (objWrap.isSelected())
                objWrap.moveBy(primaryPtr.getDeltaLastX(),
                        primaryPtr.getDeltaLastY());
        }
        if (primaryPtr.getLastY() > this.getHeight() - DELETE_BOUNDARY)
            isDragToDelete = true;
        else
            isDragToDelete = false;
        invalidate();
    }

    private void dragEnd() {
        isDragging = false;
        if (isDragToDelete) {
            ArrayList<Integer> indexToRemove = new ArrayList<>();
            for (int i = 0; i < noteObjects.size(); i++) {
                if (noteObjects.get(i).isSelected())
                    indexToRemove.add(i);
            }
            for (int i = indexToRemove.size()-1; i >= 0; i--)
                noteObjects.remove((int)indexToRemove.get(i));
            isDragToDelete = false;
            invalidate();
        }
    }

    private void select(final int index) {
        noteObjects.get(index).setSelected(true);
        invalidate();
    }

    private void deselect(final int index) {
        noteObjects.get(index).setSelected(false);
        invalidate();
    }

    // Private helper to deselect all by unsetting 'isSelected' flags
    private void deselectAll() {
        for (NoteObjectWrap objWrap: noteObjects)
            if (objWrap.isSelected())
                objWrap.setSelected(false);
        invalidate();
    }


    // Single stylus drawing helper methods
    private void draw(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
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

    private void enterEditMode(final int index) {
        NoteObjectWrap objWrap = noteObjects.get(index);
        if (!(objWrap.getNoteObj() instanceof EditText))
            throw new IllegalStateException("DrawView: enterEditMode(): " +
                    "only calls with an index of EditText wrapper in " +
                    "noteObjects");
        if (editingBoxIndex != -1)
            clearEditingFocus();
        ((EditText) objWrap.getNoteObj()).requestFocus();
        editingBoxIndex = index;
        showKeyboard();
    }

    private void exitEditMode() {
        if (editingBoxIndex == -1) return;
        clearEditingFocus();
        closeKeyboard();
    }

    private void clearEditingFocus() {
        for (NoteObjectWrap objWrap: noteObjects) {
            if (objWrap.getNoteObj() instanceof EditText &&
                ((EditText) objWrap.getNoteObj()).hasFocus())
                ((EditText) objWrap.getNoteObj()).clearFocus();
        }
        editingBoxIndex = -1;
        invalidate();
    }

    // Private helpers for manually toggling soft keyboard
    public void showKeyboard(){
        if (!keyboard_status) {
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
            keyboard_status = true;
        }
    }
    public void closeKeyboard(){
        if (keyboard_status) {
            imm.hideSoftInputFromWindow(getWindowToken(), 0);
            keyboard_status = false;
        }
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
//        ed.setLayoutParams(new RelativeLayout.LayoutParams(
//                RelativeLayout.LayoutParams.MATCH_PARENT,
//                RelativeLayout.LayoutParams.MATCH_PARENT
//        ));
        ed.setVisibility(View.VISIBLE);
        ed.setFocusable(true);
        ed.setTextIsSelectable(true);
        ed.setInputType(InputType.TYPE_CLASS_TEXT);
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

    // Check position of contact to determine touchDownType of that ptr
    @RequiresApi(api = Build.VERSION_CODES.O)
    private TouchDownType checkTouchDownType(final float x, final float y) {
        TouchDownType touchDownType = TouchDownType.WHITESPACE;
        for (int i = noteObjects.size()-1; i >= 0; i--) {
            NoteObjectWrap wrap = noteObjects.get(i);
            if (wrap.getNoteObj() instanceof EditText &&
                    wrap.containsInEditText(x, y)) {
                containingObjIndex = i;
                return TouchDownType.EDITTEXT;
            }
            if (wrap.contains(x, y)) {
                containingObjIndex = i;
                return wrap.isSelected() ?
                        TouchDownType.SELECTED :
                        TouchDownType.UNSELECTED;
            }
        }
        return touchDownType;
    }

    // FIXME: conditions might have to be more strict to avoid conflict with hold+tap->multiselect
    private boolean isCopyPaste() {
        // Sanity check
        if (gesturePointers.size() != 2)
            throw new IllegalStateException("DrawView: isCopyPaste(): " +
                    "copy/paste should be invoke with 2 fingers");
        PointerDescriptor p0 = gesturePointers.get(0);
        PointerDescriptor p1 = gesturePointers.get(0);
        if (p0.getTouchDownType() == TouchDownType.SELECTED &&
            p1.getTouchDownType() == TouchDownType.SELECTED &&
            p0.getContainingObjIndex() == p1.getContainingObjIndex() &&
//            p0.getDeltaDown() <= TAP_MOVE_DISTANCE_TOLERANCE &&
            p0.getDownDuration() > TAP_WINDOW &&
            p1.getDownDuration() > TAP_WINDOW) {
            p0.setTouchDownType(TouchDownType.UNSELECTED);
            return true;
        }
        return false;
    }

    private boolean isTap(PointerDescriptor p) {
        return p.getDownDuration() <= TAP_WINDOW &&
                p.getDeltaDown() <= TAP_MOVE_DISTANCE_TOLERANCE;
   }

    private double distanceBtw(final float x1, final float y1,
                               final float x2, final float y2) {
        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
    }



    // Below are getters and setters
    public ArrayList<NoteObjectWrap> getNoteObjects() {
        return this.noteObjects;
    }

    public int getBoxID() {
        return this.editingBoxIndex;
    }

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

    private void printAllPtrs(MotionEvent event) {
        String divider = "===========================================================\n";
        String actionLogStr = "Event: " + MotionEvent.actionToString(event.getActionMasked()) + "\n";
        for (PointerDescriptor p: gesturePointers)
            actionLogStr += p.toString();
        Log.d(LOG_TAG, "\n" + divider + actionLogStr + divider);
    }
}