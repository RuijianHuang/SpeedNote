package gatech.cs6456.speednote;
import android.graphics.Path;
import android.graphics.PathMeasure;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

public class Stroke {

    private final int MIN_STROKE_WIDTH = 1;
    private final int MAX_STROKE_WIDTH = 300;
    public int color;       // color of stroke
    public float strokeWidth;       // width of stroke
    public Path path;       // path of stroke drawn
    private PathMeasure pathMeasure;


    public Stroke(int color, int strokeWidth, Path path) {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.path = path;
        this.pathMeasure = new PathMeasure();
    }

    public void scaleBy(final double scaleFactor) {
        this.strokeWidth += scaleFactor/10;
        this.strokeWidth = Math.max(this.strokeWidth, MIN_STROKE_WIDTH);
        this.strokeWidth = Math.min(this.strokeWidth, MAX_STROKE_WIDTH);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean isOnStroke(final float x, final float y, final int containMargin) {
        float[] pathPoints = path.approximate(0.5F);
        for (int i = 0; i < pathPoints.length; i += 3) {
            final float thisX = pathPoints[i+1];
            final float thisY = pathPoints[i+2];
            final double distance = Math.sqrt(
                    (x-thisX)*(x-thisX) + (y-thisY)*(y-thisY)
            );
            if (distance <= (float)strokeWidth/2 + containMargin)
                return true;
        }
        return false;
    }
}
