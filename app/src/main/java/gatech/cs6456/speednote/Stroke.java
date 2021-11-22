package gatech.cs6456.speednote;
import android.graphics.Path;
import android.os.Build;

import androidx.annotation.RequiresApi;

public class Stroke {

    public int color;       // color of stroke
    public int strokeWidth;       // width of stroke
    public Path path;       // path of stroke drawn

    public Stroke(int color, int strokeWidth, Path path) {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.path = path;
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
