package gatech.cs6456.speednote;
import android.graphics.Path;

public class Stroke {

    public int color;       // color of stroke
    public int strokeWidth;       // width of stroke
    public Path path;       // path of stroke drawn

    public Stroke(int color, int strokeWidth, Path path) {
        this.color = color;
        this.strokeWidth = strokeWidth;
        this.path = path;
    }
}
