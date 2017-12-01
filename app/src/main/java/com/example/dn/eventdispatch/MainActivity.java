package com.example.dn.eventdispatch;

import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.text);
        textView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                Log.d("out","onTouchListener");
                return false;
            }
        });
        textView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Log.d("out","onLongClick");
                return true;
            }
        });




    }

    public void myclick(View view){
        Toast.makeText(this,"hello",Toast.LENGTH_SHORT).show();
        Log.d("out","onClick");
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d("out","onTouchEvent");
        return super.onTouchEvent(event);
    }


}
