package com.tinkerlog.android.pongtime;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

import com.tinkerlog.android.pongtime.R;
import com.tinkerlog.android.pongtime.PongTimeView.PongThread;

/**
 * Entry point for PongTime.
 * Based on the LunarLander example. 
 */
public class PongTime extends Activity {
	
	private static final int MENU_TOGGLEFPS = 1;
	private static final int MENU_ABOUT = 2;
	private static final int DIALOG_ABOUT = 3;
	
    private PongThread pongThread;
    private PongTimeView pongView;
        
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_TOGGLEFPS, 0, R.string.menu_toggle_fps);
        menu.add(0, MENU_ABOUT, 0, R.string.menu_about);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_TOGGLEFPS:
            pongThread.toggleFPS();
            return true;
        case MENU_ABOUT:
        	showDialog(DIALOG_ABOUT);
            return true;
        }
        return false;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
    	if (id == DIALOG_ABOUT) {
    		return new AlertDialog.Builder(this)
    		.setIcon(R.drawable.pong_128)
    		.setTitle(R.string.about_title)
    		.setMessage(R.string.about_text)
    		.setPositiveButton("OK", new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int whichButton) {
    			}
    		})
    		.create();
    	}
    	return null;
    }    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.i(this.getClass().getName(), "onCreate");

        // turn off the window's title bar and switch to fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // tell system to use the layout defined in our XML file
        setContentView(R.layout.main);

        pongView = (PongTimeView)findViewById(R.id.pongview);
        pongThread = pongView.getThread();
    }
}

