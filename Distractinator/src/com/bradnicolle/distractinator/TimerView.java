package com.bradnicolle.distractinator;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;

import java.lang.Math;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TimerView extends View {
	private Paint paint;
	private Paint paintBack;
	private Paint paintText;
	
	private Bitmap thumb;
	private Bitmap play;
	
	private RectF oval;
	
	private float theta;
	private float thetaPrev = 0;
	private boolean lowLimit = false;
	private int hours = 0;
	private int mins = 0;
	
	private float radius;
	private float xpad;
	private float ypad;
	
	private float yAdjust = 0;
	private Rect startBounds;
	
	private String onClick;

	public TimerView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		onClick = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "onClick");

		paint = new Paint();
		paint.setAntiAlias(true);
		paint.setStrokeWidth(20f);
		paint.setColor(Color.rgb(51, 181, 229));
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeJoin(Paint.Join.ROUND);
		
		paintBack = new Paint();
		paintBack.set(paint);
		paintBack.setColor(Color.LTGRAY);
		
		paintText = new Paint();
		paintText.setAntiAlias(true);
		paintText.setColor(Color.GRAY);
		paintText.setTypeface(Typeface.SANS_SERIF);
		paintText.setTextAlign(Paint.Align.CENTER);
		
		thumb = BitmapFactory.decodeResource(getResources(), R.drawable.scrubber_control_normal_holo);
	}
	
	public long getTime() {
		return (hours * 60 * 60 + mins *60) * 1000; // Time in milliseconds
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// Account for padding
		xpad = (float) (getPaddingLeft() + getPaddingRight());
		ypad = (float) (getPaddingTop() + getPaddingBottom());

		float ww = (float) w - xpad;
		float hh = (float) h - ypad;
		radius = Math.min(ww,  hh)/2;
		oval = new RectF(-radius, -radius, radius, radius);
		
		// Recompute required text size for digits
		startBounds = adjustTextSize("0 00", 0.7f, paintText);
		yAdjust = (startBounds.bottom - startBounds.top) / 2;
		
		play = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.play), (int)(0.9f*radius), (int)(0.9f*radius), true);
	}
	
	private Rect adjustTextSize(String text, float proportion, Paint paint) {
		paint.setTextSize(100f);
		Rect bounds = new Rect();
		
		paint.getTextBounds(text, 0, text.length(), bounds);
		// Get the width produced
		int w = bounds.right - bounds.left;
		// We want 70% of this width
		float target = (float)radius*2*proportion;
		float size = ((target/w)*100f);
		
		paint.setTextSize(size);
		// Get new bounds, and return
		paint.getTextBounds(text, 0, text.length(), bounds);
		return bounds;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		canvas.translate(radius+xpad/2, radius+ypad/2);
		canvas.drawArc(oval, 0, 360, false, paintBack);

		if (hours > 0 || mins > 0) {
			canvas.save();
			canvas.translate(-play.getWidth()/2, -play.getHeight()/2);
			canvas.drawBitmap(play, 0, 0, paint);
			canvas.restore();
		}
		canvas.drawText(hours + " " + (mins < 10 ? "0":"") + mins, 0, yAdjust, paintText);
		
		if (!lowLimit) {
			canvas.drawArc(oval, -90, theta, false, paint);
			canvas.translate(0, -radius);
			canvas.rotate(theta, 0, radius);
			canvas.translate(-thumb.getWidth()/2, -thumb.getHeight()/2);
			canvas.drawBitmap(thumb, 0, 0, paint);
			mins = (int)(theta / 6);
		}
		else {
			canvas.translate(-thumb.getWidth()/2, -thumb.getHeight()/2 - radius);
			canvas.drawBitmap(thumb, 0, 0, paint);
			mins = 0; // In case the finger moved too quickly between events
		}
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		ViewParent parent = getParent();
		// This magic prevents the parent view hierarchy from allowing tab swipes, etc.
		parent.requestDisallowInterceptTouchEvent(true);
		
		float eventX = event.getX() - radius - xpad/2;
		float eventY = -event.getY() + radius + ypad/2;

		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (onRadius(eventX, eventY)) {
				theta = evalAngle(eventX, eventY);
				lowLimit = false;
				thetaPrev = theta;
			}
			else if (insideRadius(eventX, eventY)) {
				start();
			}
			else {
				return super.onTouchEvent(event);
			}
			break;
			
		case MotionEvent.ACTION_MOVE:
			theta = evalAngle(eventX, eventY);
			
			if (inBounds(thetaPrev, 270, 360) && inBounds(theta, 0, 90) && !lowLimit) {
				hours++;
				lowLimit = false;
			}
			else if (inBounds(thetaPrev, 0, 90) && inBounds(theta, 270, 360) && !lowLimit) {
				if (hours > 0) {
					hours--;
				}
				else {
					lowLimit = true;
				}
			}
			
			thetaPrev = theta;
			
			break;
		case MotionEvent.ACTION_UP:
			// nothing to do
			break;
		default:
			return false;
		}
		
		// Schedules a repaint.
		invalidate();
		return true;
	}
	
	private void start() {
		if (onClick != null) {
			Method clickHandler = null;
			try {
				clickHandler = getContext().getClass().getMethod(onClick, View.class);
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
	
	private boolean onRadius(float x, float y) {
		float len = (float)(Math.pow(x, 2) + Math.pow(y, 2));
		float radlen = (float)Math.pow(radius, 2);

		return (len < (radlen + 20000)) && (len > (radlen - 20000));
	}
	
	private boolean insideRadius(float x, float y) {
		float len = (float)(Math.pow(x, 2) + Math.pow(y, 2));
		float radlen = (float)Math.pow(radius, 2);

		return (len < (radlen - 30000));
	}
	
	private boolean inBounds(float num, float a, float b) {
		return (num >= a && num <= b);
	}
	
	private float evalAngle(float x, float y) {
		float angle = (float) Math.toDegrees(Math.atan(x / y));
		if (y < 0) {
			angle += 180;
		} else if (x < 0) {
			angle += 360;
		}
		return angle;
	}

}
