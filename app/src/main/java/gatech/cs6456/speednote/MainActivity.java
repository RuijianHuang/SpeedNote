package gatech.cs6456.speednote;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.ImageButton;

import com.google.android.material.slider.RangeSlider;

public class MainActivity extends AppCompatActivity {
    private DrawView drawView;
    private ImageButton save,color,stroke,undo;
    private RangeSlider rangeSlider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}