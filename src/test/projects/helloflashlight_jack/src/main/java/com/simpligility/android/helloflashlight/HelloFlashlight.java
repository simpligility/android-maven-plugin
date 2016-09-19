package com.simpligility.android.helloflashlight;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;

/**
 * HelloFlashlight is a sample application for the usage of the Maven Android Plugin.
 * The code is trivial and not the focus of this example and therefore not really documented.
 *
 * @author Manfred Moser <manfred@simpligility.com>
 */
public class HelloFlashlight extends Activity {

	TableLayout table;
	Button redButton;
	Button greenButton;
	Button blueButton;
	Button blackButton;
	Button whiteButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // get all the view components
        table = (TableLayout) findViewById(R.id.Table);
        redButton = (Button) findViewById(R.id.ButtonRed);
        greenButton = (Button) findViewById(R.id.ButtonGreen);
        blueButton = (Button) findViewById(R.id.ButtonBlue);
        blackButton = (Button) findViewById(R.id.ButtonBlack);
        whiteButton = (Button) findViewById(R.id.ButtonWhite);

        // default the full screen to white
        table.setBackgroundColor(Color.WHITE);

        // hook up all the buttons with a table color change on click listener
        redButton.setOnClickListener(OnClickChangeColor(Color.RED));
        greenButton.setOnClickListener(OnClickChangeColor(Color.GREEN));
        blueButton.setOnClickListener(OnClickChangeColor(Color.BLUE));
        blackButton.setOnClickListener(OnClickChangeColor(Color.BLACK));
        whiteButton.setOnClickListener(OnClickChangeColor(Color.WHITE));
    }

    /**
     * An OnClickListener that changes the color of the table.
     * @param color
     * @return
     */
    View.OnClickListener OnClickChangeColor(final int color)
    {
         return (view) ->
        { table.setBackgroundColor(color); };

    }
}
