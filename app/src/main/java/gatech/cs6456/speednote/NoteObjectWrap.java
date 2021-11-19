package gatech.cs6456.speednote;

import android.os.Build;
import android.widget.EditText;

import androidx.annotation.RequiresApi;

public class NoteObjectWrap {
    private Object noteObj;
    private boolean isSelected;

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
    public boolean isWithin(final float x, final float y) {
        if (noteObj instanceof Stroke)
            return ((Stroke) noteObj).isOnStroke(x, y);
        else
            return isWithinEditText(x, y);
    }

    // Private helper to check if a pt is in an EditText
    private boolean isWithinEditText(final float x, final float y) {
        EditText ed = (EditText) noteObj;
        if (x > ed.getX() && y > ed.getY() &&
            x < ed.getX() + ed.getWidth() &&
            y < ed.getY() + ed.getHeight())
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
