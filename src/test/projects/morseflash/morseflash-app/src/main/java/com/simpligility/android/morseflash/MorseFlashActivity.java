package com.simpligility.android.morseflash;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.LinearLayout;
import com.simpligility.android.morse.MorseCodeConverter;

/**
 * MorseFlashActivity is the actual activity that flashes the screen in morse code.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class MorseFlashActivity extends Activity {

    private LinearLayout page;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.morseflash);
        page = (LinearLayout) findViewById(R.id.page);

        MorseFlashApplication application = (MorseFlashApplication) getApplication();
        String message = application.message;

        Handler myHandler = new Handler();
        long[] morseCode = MorseCodeConverter.pattern(message);
        long total = 0;
        for (int i = 0; i < morseCode.length; i++) {
            total = total + morseCode[i];
            long MULTIPLIER = 5;
            if (i % 2 == 0) {
                myHandler.postDelayed(new ColorSwitch(total, Color.WHITE), total * MULTIPLIER);
            } else {
                myHandler.postDelayed(new ColorSwitch(total, Color.BLACK), total * MULTIPLIER);
            }
        }
    }

    class ColorSwitch implements Runnable {

        final int color;
        long delay;

        ColorSwitch(long delay, int color) {
            this.delay = delay;
            this.color = color;
        }

        public void run() {
            page.setBackgroundColor(color);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

