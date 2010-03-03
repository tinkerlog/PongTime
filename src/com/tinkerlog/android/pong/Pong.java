package com.tinkerlog.android.pong;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.tinkerlog.android.pong.PongView.PongThread;

public class Pong extends Activity {
	
	private static final int MENU_START = 1;
	private static final int MENU_STOP = 2;
	
    private PongThread pongThread;
    private PongView pongView;

    
    
    /**
     * Invoked during init to give the Activity a chance to set up its Menu.
     * 
     * @param menu the Menu to which entries may be added
     * @return true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_START, 0, R.string.menu_start);
        menu.add(0, MENU_STOP, 0, R.string.menu_stop);
        return true;
    }

    /**
     * Invoked when the user selects an item from the Menu.
     * 
     * @param item the Menu entry which was selected
     * @return true if the Menu item was legit (and we consumed it), false
     *         otherwise
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_START:
            pongThread.doStart();
            return true;
        case MENU_STOP:
            //pongThread.setState(PongThread.STATE_LOSE, getText(R.string.message_stopped));
            return true;
            /*
        case MENU_PAUSE:
            pongThread.pause();
            return true;
        case MENU_RESUME:
            pongThread.unpause();
            return true;
            */
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // turn off the window's title bar
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, 
                WindowManager.LayoutParams.FLAG_FULLSCREEN );

        // tell system to use the layout defined in our XML file
        setContentView(R.layout.main);

        // get handles to the LunarView from XML, and its LunarThread
        pongView = (PongView) findViewById(R.id.pong);
        pongThread = pongView.getThread();

        // give the LunarView a handle to the TextView used for messages
        pongView.setTextView((TextView) findViewById(R.id.text));

        if (savedInstanceState == null) {
            // we were just launched: set up a new game
            //pongThread.setState(PongThread.STATE_READY);
            pongThread.doStart();
            Log.w(this.getClass().getName(), "SIS is null");
        } 
    }

}

