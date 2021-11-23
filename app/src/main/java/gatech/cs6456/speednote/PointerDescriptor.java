package gatech.cs6456.speednote;

import android.view.MotionEvent;

import java.util.ArrayList;

public class PointerDescriptor {
    private final int ID;
    private final float downX, downY;
    private float lastX, lastY;
    private float deltaDownX, deltaDownY,
                  deltaLastX, deltaLastY;
    private double deltaLast, deltaDown;
    private final long downTime;
    private long downDuration;
    private TouchDownType touchDownType;
    private int containingObjIndex;
    private final int toolType;
    private final ArrayList<float[]> history;
    private boolean updateConsumed;


    public PointerDescriptor(MotionEvent event) {
        final int ptrIndex = event.getActionIndex();
        this.ID = event.getPointerId(ptrIndex);
        this.downTime = event.getEventTime();
        this.downDuration = 0;
        this.downX = this.lastX = event.getX(ptrIndex);
        this.downY = this.lastY = event.getY(ptrIndex);
        this.deltaDownX = this.deltaDownY = this.deltaLastX = this.deltaLastY = 0;
        this.deltaLast = this.deltaDown = 0;
        this.touchDownType = TouchDownType.UNDEFINED;
        this.containingObjIndex = -1;
        this.toolType = event.getToolType(ptrIndex);
        this.history = new ArrayList<>();
        this.history.add(new float[] {this.downX, this.downY});
        this.updateConsumed = true;
    }

    public void update(MotionEvent event) throws IllegalStateException {
        final int index = event.findPointerIndex(getID());
        final float currX = event.getX(index);
        final float currY = event.getY(index);
        if (currX == lastX && currY == lastY)
            return;
        this.deltaLastX = currX - this.lastX;
        this.deltaLastY = currY - this.lastY;
        this.deltaDownX = currX - this.downX;
        this.deltaDownY = currY - this.downY;
        this.deltaLast = Math.sqrt(Math.pow(deltaLastX, 2) + Math.pow(deltaLastY, 2));
        this.deltaDown = Math.sqrt(Math.pow(deltaDownX, 2) + Math.pow(deltaDownY, 2));
        this.lastX = currX;
        this.lastY = currY;
        this.downDuration = event.getEventTime() - this.downTime;
        this.history.add(new float[] {currX, currY});
        this.updateConsumed = false;
        if (history.size() > 10)
            this.history.remove(0);
    }

    public int getID() {
        return this.ID;
    }

    public float getDownX() {
        return downX;
    }

    public float getDownY() {
        return downY;
    }

    public float getLastX() {
        return lastX;
    }

    public float getLastY() {
        return lastY;
    }

    public float getDeltaDownX() {
        return deltaDownX;
    }

    public float getDeltaDownY() {
        return deltaDownY;
    }

    public float getDeltaLastX() {
        return deltaLastX;
    }

    public float getDeltaLastY() {
        return deltaLastY;
    }

    public TouchDownType getTouchDownType() {
        return touchDownType;
    }

    public void setTouchDownType(TouchDownType touchDownType) {
        this.touchDownType = touchDownType;
    }

    public long getDownTime() {
        return this.downTime;
    }

    public long getDownDuration() {
        return this.downDuration;
    }

    public double getDeltaLast() {
        return deltaLast;
    }

    public double getDeltaDown() {
        return deltaDown;
    }

    public int getContainingObjIndex() {
        return containingObjIndex;
    }

    public void setContainingObjIndex(int containingObjIndex) {
        this.containingObjIndex = containingObjIndex;
    }

    public int getToolType() {
        return toolType;
    }

    public ArrayList<float[]> getHistory() {
        return history;
    }

    public boolean isUpdateConsumed() {
        return updateConsumed;
    }

    public void setUpdateConsumed(boolean updateConsumed) {
        this.updateConsumed = updateConsumed;
    }

    public String toString() {
        return "PointerDescriptor: \n" +
                "\tID: " + this.ID + "\n" +
                "\tdownTime: " + this.downTime + ", downDuration: " + this.downDuration + "\n" +
                "\tdown coordinate: (" + this.downX + ", " + this.downY + ")\n" +
                "\tlast coordinate: (" + this.lastX + ", " + this.lastY + ")\n" +
                "\ttouchDownType: " + touchDownType.toString() + "\n";
    }
}

enum TouchDownType {
    SELECTED,
    UNSELECTED,
    EDITTEXT,
    WHITESPACE,
    UNDEFINED
}
