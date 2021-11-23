package gatech.cs6456.speednote;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageButton;

import com.google.android.material.slider.RangeSlider;

import petrov.kristiyan.colorpicker.ColorPicker;

public class MainActivity extends AppCompatActivity {
    private DrawView drawView;
    private ImageButton btnSave, btnColor, btnStroke, btnUndo;
    private RangeSlider rangeSlider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawView = (DrawView) findViewById(R.id.draw_view);
        rangeSlider = (RangeSlider) findViewById(R.id.rangebar);
        btnUndo = (ImageButton) findViewById(R.id.btn_undo);
        btnSave = (ImageButton) findViewById(R.id.btn_save);
        btnColor = (ImageButton) findViewById(R.id.btn_color);
        btnStroke = (ImageButton) findViewById(R.id.btn_stroke);

        btnUndo.setOnClickListener(view -> drawView.undo());

        btnSave.setOnClickListener(view -> System.out.println("trying to save"));

        // allow user to select color of the brush
        btnColor.setOnClickListener(view -> {
            final ColorPicker colorPicker = new ColorPicker(MainActivity.this);
            colorPicker.setOnFastChooseColorListener(new ColorPicker.OnFastChooseColorListener() {
                @Override
                public void setOnFastChooseColorListener(int position, int color) {
                    // get int value of color selected from the dialog and set as stroke color
                    drawView.setCurrPaintColor(color);
                }

                @Override
                public void onCancel() {
                    colorPicker.dismissDialog();
                }
            })
                .setColumns(5)                          // #color columns to show in dialog
                .setDefaultColorButton(Color.BLACK)     // default color selected in dialog
                .show();
        });

        // toggle visibility of the rangeSlider
        btnStroke.setOnClickListener(view ->
            rangeSlider.setVisibility(rangeSlider.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE)
        );

        // default values of rangeSlider
        rangeSlider.setValueFrom(0.0f);
        rangeSlider.setValueTo(100.0f);
        rangeSlider.addOnChangeListener((@NonNull RangeSlider slider, float value, boolean fromUser) ->
                drawView.setCurrStrokeWidth((int) value)
        );

        // Pass the height and width of the custom view to the innit method of the DrawView obj
        ViewTreeObserver viewTreeObserver = drawView.getViewTreeObserver();
        viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                drawView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int height = drawView.getMeasuredHeight();
                int width = drawView.getMeasuredWidth();
                drawView.init(height, width);
            }
        });
    }

    //text edit
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DEL:
                if (drawView.getBoxID()!=-1) {
                    if (drawView.getNoteObjects().get(drawView.getBoxID()).getNoteObj() instanceof EditText) {
                        EditText ed = (EditText) drawView.getNoteObjects().get(drawView.getBoxID()).getNoteObj();
                        String text = ed.getText().toString();
                        if (text.length()>0){
                            ed.setText(text.substring(0, text.length()-1));
                            drawView.invalidate();
                        }
                    }
                }
                break;
            default:
                if (drawView.getBoxID()!=-1) {
                    if (drawView.getNoteObjects().get(drawView.getBoxID()).getNoteObj() instanceof EditText) {
                        EditText ed = (EditText) drawView.getNoteObjects().get(drawView.getBoxID()).getNoteObj();
                        ed.setText(ed.getText() + String.valueOf((char)event.getUnicodeChar()));
                        drawView.invalidate();
                    }
                }
        }
        return super.onKeyUp(keyCode, event);
    }
}