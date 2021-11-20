package gatech.cs6456.speednote;

import android.view.MotionEvent;

public class PointerDescriptor {
    private final int ID;
    private final float downX, downY;
    private float lastX, lastY;
    private float deltaDownX, deltaDownY, deltaLastX, deltaLastY;
    private final long downTime;
    private long downDuration;
    private TouchDownType touchDownType;


    public PointerDescriptor(MotionEvent event) {
        this.ID = event.getPointerId(event.getActionIndex());
        this.downTime = event.getDownTime();
        this.downDuration = 0;
        this.downX = this.lastX = event.getX();
        this.downY = this.lastY = event.getY();
        this.deltaDownX = this.deltaDownY = this.deltaLastX = this.deltaLastY = 0;
        this.touchDownType = TouchDownType.UNDEFINED;
    }

    public void update(MotionEvent event) throws IllegalStateException {
        final float currX = event.getX();
        final float currY = event.getY();
        this.deltaLastX = currX - this.lastX;
        this.deltaLastY = currY - this.lastY;
        this.deltaDownX = currX - this.downX;
        this.deltaDownY = currY - this.downY;
        this.lastX = currX;
        this.lastY = currY;
        this.downDuration = event.getEventTime() - this.downTime;
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
}

enum TouchDownType {
    SELECTED,
    UNSELECTED,
    WHITESPACE,
    UNDEFINED
}
