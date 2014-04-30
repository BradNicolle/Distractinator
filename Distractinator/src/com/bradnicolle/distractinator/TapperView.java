package com.bradnicolle.distractinator;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class TapperView extends View {
	private Paint paint;
	private Paint glowPaint;
	private Paint textPaint;
	
	private float xpad, ypad;
	private float ww, hh, radius;
	private float buttonRad;
	
	private boolean held = false;
	private boolean timeUp = false;
	private final int TIMEOUT = 5000;
	private Timer timer;
	private TimerTask task;
	
	private float glowRadius = 0f;
	private ObjectAnimator anim;
	
	private String onUnlocked;

	public TapperView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		onUnlocked = attrs.getAttributeValue("http://schemas.android.com/apk/res/com.bradnicolle.distractinator", "onUnlocked");
		
		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setColor(Color.rgb(51, 181, 229));
		paint.setStyle(Paint.Style.FILL);
		
		glowPaint = new Paint();
		glowPaint.set(paint);
		glowPaint.setARGB(120, 138, 213, 240);
		
		textPaint = new Paint();
		textPaint.setAntiAlias(true);
		textPaint.setColor(Color.WHITE);
		textPaint.setTypeface(Typeface.SANS_SERIF);
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTextSize(48f);
		
		timer = new Timer();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// Account for padding
		xpad = (float) (getPaddingLeft() + getPaddingRight());
		ypad = (float) (getPaddingTop() + getPaddingBottom());

		ww = (float) w - xpad;
		hh = (float) h - ypad;
		radius = Math.min(ww, hh);
		buttonRad = radius/10;
		
		anim = ObjectAnimator.ofFloat(this, "glowRadius", buttonRad, radius/3);
		anim.setDuration(TIMEOUT);
	}
	
	@Override
	protected void onDraw(Canvas canvas) {
		canvas.translate(ww/2 + xpad/2, hh/2 + ypad/2);
		canvas.drawCircle(0, 0, glowRadius, glowPaint);
		canvas.drawCircle(0, 0, buttonRad, paint);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		float eventX = event.getX() - (ww + xpad)/2;
		float eventY = event.getY() - (hh + ypad)/2;
		
		switch(event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (inRadius(eventX, eventY, buttonRad)) {
				held();
			}
			break;
		case MotionEvent.ACTION_UP:
			resetGlowRadius(); // Reset the glow if the finger was released anywhere, not just inside the circle
			if (inRadius(eventX, eventY, buttonRad) && held == true) {
				released();
			}
		default:
			return false;
		}
		invalidate();
		return true;
	}
	
	private boolean inRadius(float x, float y, float radius) {
		float len = (float)(Math.pow(x, 2) + Math.pow(y, 2));
		radius = (float)Math.pow(radius, 2);
		return len < radius;
	}
	
	private void held() {
		held = true;
		timeUp = false;
		if (task != null) {
			task.cancel();
		}
		task = null;
		task = new TimerTask() {
			public void run() {
				timeUp = true;
				glowPaint.setARGB(120, 255, 148, 148);
			}
		};
		anim.start();
		try {
			timer.schedule(task, TIMEOUT);
		} catch (IllegalStateException ise) {
			// Do nothing, in case we cancelled before starting anything (this will happen on the first press etc)
		}
	}
	
	private void released() {
		if (held && timeUp) {
			task.cancel();
			timeUp = false;
			held = false;
			invalidate();
			unlock();
		}
	}
	
	public void setGlowRadius(float radius) {
		glowRadius = radius;
		invalidate();
	}
	
	public void resetGlowRadius() {
		anim.end();
		glowPaint.setARGB(120, 138, 213, 240);
		glowRadius = buttonRad;
		invalidate();
	}
	
	private void unlock() {
		if (onUnlocked != null) {
			Method clickHandler = null;
			try {
				clickHandler = getContext().getClass().getMethod(onUnlocked, View.class);
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
			if (clickHandler != null) {
				try {
	                clickHandler.invoke(getContext(), this);
	            } catch (IllegalAccessException e) {
	                throw new IllegalStateException("Could not execute non "
	                        + "public method of the activity", e);
	            } catch (InvocationTargetException e) {
	                throw new IllegalStateException("Could not execute "
	                        + "method of the activity", e);
	            }
			}
		}
	}

}
