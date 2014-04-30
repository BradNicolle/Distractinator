package com.bradnicolle.distractinator;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

public class OverlayService extends Service {
	private LayoutInflater li;
	private WindowManager wm;
	private View view;
	private final int TIMEOUT = 5000;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
    @Override
    public void onCreate() {
		super.onCreate();
	 
		wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
	 
		WindowManager.LayoutParams params = new WindowManager.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT);
		params.gravity = Gravity.CENTER | Gravity.TOP;
		params.x = 0;
		params.y = 0;
		
		li = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
		view = li.inflate(R.layout.locked_layout, null);
		
		wm.addView(view, params);
    }
    
    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
    	// Defaults to -1 for indefinite duration
    	long duration = i.getLongExtra(MainActivity.EXTRA_TIME, -1);

    	if (duration >= 0) {
    		// Set to quit in 'duration' milliseconds
    		(new Timer()).schedule(new TimerTask() {
    			public void run() {
    				stopSelf();
    			}
    		}, duration);
    	}
    	return START_STICKY;
    }
    
	private Handler uiCallback = new Handler () {
	    public void handleMessage (Message msg) {
	        if (view.getVisibility() == View.INVISIBLE) {
	        	view.setVisibility(View.VISIBLE);
	        }
	    }
	};
    
    @Override
    public void onDestroy() {
		super.onDestroy();
		if (view != null) {
		    wm.removeView(view);
		    view = null;
		}
    }
    
    public void hide(View v) {
    	(new Timer()).schedule(new TimerTask() {
    		public void run() {
    			uiCallback.sendEmptyMessage(0);
    		}
    	}, TIMEOUT);
    	view.setVisibility(View.INVISIBLE);
    }
    
	public void quitService(View v) {
		stopSelf();
	}

}
