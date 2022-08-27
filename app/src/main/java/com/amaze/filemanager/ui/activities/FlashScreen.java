package com.amaze.filemanager.ui.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.amaze.filemanager.R;

public class FlashScreen extends AppCompatActivity {
TextView text1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_flash_screen);

        text1 = findViewById(R.id.flash_text);
        text1.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left));

        Intent intent =new Intent(this,MainActivity.class);
        Thread thread = new Thread() {
            public void run() {
                try {
                    sleep(1000);
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                finally {
                    text1.clearAnimation();
                    startActivity(intent);
                    finish();
                }
            }
        };thread.start();
    }
}
