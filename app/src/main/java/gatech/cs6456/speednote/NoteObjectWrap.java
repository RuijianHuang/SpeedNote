package gatech.cs6456.speednote;

import android.os.Build;
import android.widget.EditText;

import androidx.annotation.RequiresApi;

public class NoteObjectWrap {
    private Object noteObj;
    private boolean isSelected;
    private final int CONTAIN_MARGIN = 30;

    public NoteObjectWrap(Object noteObj) {
        this.noteObj = noteObj;
        this.isSelected = false;

        if (!(noteObj instanceof Stroke) && !(noteObj instanceof EditText))
            throw new IllegalArgumentException(
                    "NoteObjectWrap should only contain Stroke or EditText object"
            );
    }

    public void moveBy(final float dx, final float dy) {
        if (noteObj instanceof Stroke) {
            ((Stroke) noteObj).path.offset(dx, dy);
        } else {
            ((EditText) noteObj).setX(((EditText) noteObj).getX()+dx);
            ((EditText) noteObj).setY(((EditText) noteObj).getY()+dy);
        }
    }

    // Check whether a point is in an Object based on type
    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean contains(final float x, final float y) {
        if (noteObj instanceof Stroke)
            return ((Stroke) noteObj).isOnStroke(x, y, CONTAIN_MARGIN);
        else
            return isOnEditTextContour(x, y);
    }

    // Private helper to check if a pt is in an EditText
    private boolean isOnEditTextContour(final float x, final float y) {
        // Sanity check
        if (!(noteObj instanceof EditText))
            throw new IllegalArgumentException("NoteObjectWrap: " +
                    "isWithinEditText(): only call this with EditText objs");

        boolean isOnBounds = false;
        EditText ed = (EditText) noteObj;
        final float left = ed.getX();
        final float right = left + (float) ed.getWidth();
        final float top = ed.getY();
        final float bottom = top + (float) ed.getHeight();

        // if inside EditText+CONTAIN_MARGIN
        if (left - CONTAIN_MARGIN <= x && x <= right + CONTAIN_MARGIN &&
            top - CONTAIN_MARGIN <= y && y <= bottom + CONTAIN_MARGIN)
            isOnBounds = true;

        // if inside EditText-CONTAIN_MARGIN
        if (left + CONTAIN_MARGIN < x && x < right - CONTAIN_MARGIN &&
                top + CONTAIN_MARGIN < y && y < bottom - CONTAIN_MARGIN)
            isOnBounds = false;
        return isOnBounds;
    }

    public boolean containsInEditText(final float x, final float y) {
        // Sanity check
        if (!(noteObj instanceof EditText))
            throw new IllegalArgumentException("NoteObjectWrap: " +
                    "isWithinEditText(): only call this with EditText objs");

        EditText ed = (EditText) noteObj;
        final float left = ed.getX();
        final float right = left + (float) ed.getWidth();
        final float top = ed.getY();
        final float bottom = top + (float) ed.getHeight();
        if (left + CONTAIN_MARGIN < x && x < right - CONTAIN_MARGIN &&
            top + CONTAIN_MARGIN < y && y < bottom - CONTAIN_MARGIN)
            return true;
        return false;
    }

    // Getters and setters
    public Object getNoteObj() {
        return noteObj;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
