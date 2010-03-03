package com.tinkerlog.android.pong;

import java.util.Date;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Paint.Cap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

public class PongView extends SurfaceView implements SurfaceHolder.Callback {

	private PongThread thread;
    private TextView statusText;

    private class Ball {
    	float x;
    	float y;
    	float direction;
    	float speed;
    	public Ball(float x, float y, float direction, float speed) {
    		this.x = x;
    		this.y = y;
    		this.direction = direction;
    		this.speed = speed;
    	}
    }
    
    private class Panel {
    	float x;
    	float y;
    	public Panel(float x, float y) {
    		this.x = x;
    		this.y = y;
    	}
    }
	
	class PongThread extends Thread {
		
        public static final int STATE_PAUSE = 2;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;
                
        public static final int STATE_NONE = 5;
        public static final int STATE_HOURSWIN = 6;
        public static final int STATE_MINUTESWIN = 7;
        
        private static final float BALL_SPEED = 100.0F;  // pixels per seconds
        private static final float PANEL_SPEED = 20.0F;
        private static final int PANEL_LENGTH = 20;
        private static final int PANEL_XPOS = 20;
        
        private static final int LINE_WIDTH = 7;
        private static final int LW = 15;
        private static final int LH = 26;
        private static final int LH2 = LH / 2;
        private static final int SP = LW + 15;

        private int playFieldY1;
        private int playFieldY2;
        private int playFieldX1;
        private int playFieldX2;
        
		private boolean running;
		private long lastTimeMillis;
		private Date lastTime;
		private int mode;
		private int gMode;
		
		private int canvasHeight;
		private int canvasWidth;
		
        private SurfaceHolder surfaceHolder;
        private Paint dashedLinePaint;
        private Paint linePaint;
        private Ball ball;
        private Panel leftPanel;
        private Panel rightPanel;
        
        private Handler handler;
        private Context context;
        
        private float[][] numbers = {
        		{ 0, 0, LW, 0,     // null 
        		LW, 0, LW, LH, 
        		LW, LH, 0, LH, 
        		0, LH, 0, 0 },
        		{ LW, 0, LW, LH }, // one 
        		{ 0, 0, LW, 0,     // two
        		LW, 0, LW, LH2,
        		LW, LH2, 0, LH2,
        		0, LH2, 0, LH,
        		0, LH, LW, LH },        		
        		{ 0, 0, LW, 0,     // three
        		LW, 0, LW, LH2,
        		LW, LH2, 0, LH2,
        		LW, LH2, LW, LH,
        		LW, LH, 0, LH },
        		{ 0, 0, 0, LH2,    // four
        		0, LH2, LW, LH2,
        		LW, 0, LW, LH },
        		{ LW, 0, 0, 0,     // five
        		0, 0, 0, LH2,
        		0, LH2, LW, LH2,
        		LW, LH2, LW, LH,
        		LW, LH, 0, LH },
        		{ LW, 0, 0, 0,     // six
        		0, 0, 0, LH, 
        		0, LH, LW, LH,
        		LW, LH, LW, LH2,
        		LW, LH2, 0, LH2 },
        		{ 0, 0, LW, 0,     // seven
        		LW, 0, LW, LH },
        		{ 0, 0, LW, 0,     // eight 
        		LW, 0, LW, LH, 
        		LW, LH, 0, LH, 
        		0, LH, 0, 0,
        		0, LH2, LW, LH2 },
        		{ LW, LH, LW, 0,   // nine
        		LW, 0, 0, 0,
        		0, 0, 0, LH2,
        		0, LH2, LW, LH2 }};
        
        public PongThread(SurfaceHolder surfaceHolder, Context context,
                Handler handler) {
            this.surfaceHolder = surfaceHolder;
            this.handler = handler;
            this.context = context;

            linePaint = new Paint();
            linePaint.setColor(Color.WHITE);
            linePaint.setStyle(Paint.Style.STROKE);            
            linePaint.setStrokeWidth(LINE_WIDTH);
            linePaint.setStrokeCap(Cap.SQUARE);

            dashedLinePaint = new Paint();
            dashedLinePaint.setColor(Color.WHITE);
            dashedLinePaint.setStyle(Paint.Style.STROKE);
            dashedLinePaint.setStrokeWidth(LINE_WIDTH);
            PathEffect pe = new DashPathEffect(new float[] {20, 15}, 0.0F);
            dashedLinePaint.setPathEffect(pe);
            
            lastTime = new Date();
            
            ball = new Ball(120, 150, 1.05F, BALL_SPEED);
            // ball = new Ball(120, 150, 0.01F, BALL_SPEED);
        }		        

        public void setRunning(boolean b) {
        	running = b;
        }
                                       
        public void doStart() {
            synchronized (surfaceHolder) {
            }
        }
        
        public void setState(int mode) {
            synchronized (surfaceHolder) {
                setState(mode, null);
            }
        }
        
        public void setState(int mode, CharSequence message) {
            synchronized (surfaceHolder) {
                this.mode = mode;

                if (mode == STATE_RUNNING) {
                    Message msg = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", "");
                    b.putInt("viz", View.INVISIBLE);
                    msg.setData(b);
                    handler.sendMessage(msg);
                } 
                else {
                    Resources res = context.getResources();
                    CharSequence str = "";
                    if (mode == STATE_READY) {
                        str = res.getText(R.string.mode_ready);
                    }
                    else if (mode == STATE_PAUSE) {
                        str = res.getText(R.string.mode_pause);
                    }

                    if (message != null) {
                        str = message + "\n" + str;
                    }

                    Message msg = handler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", str.toString());
                    b.putInt("viz", View.VISIBLE);
                    msg.setData(b);
                    handler.sendMessage(msg);
                }
            }
        }
        
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (surfaceHolder) {
                canvasWidth = width;
                canvasHeight = height;
                playFieldY1 = 8;
                playFieldY2 = canvasHeight - 9;
                playFieldX1 = 5;
                playFieldX2 = canvasWidth - 5;
                leftPanel = new Panel(PANEL_XPOS, canvasHeight/2);
                rightPanel = new Panel(canvasWidth - PANEL_XPOS, canvasHeight / 2);
                setState(STATE_RUNNING);
                gMode = STATE_NONE;
                lastTimeMillis = System.currentTimeMillis();
            }
        }        

        private void doDraw(Canvas canvas) {
        	canvas.drawColor(Color.BLUE);
        	
        	drawField(canvas);

        	Date now = new Date();
        	if (now.getHours() != lastTime.getHours()) {
        		gMode = STATE_HOURSWIN;
        	}
        	else if (now.getMinutes() != lastTime.getMinutes()) {
        		gMode = STATE_MINUTESWIN;
        	}
           	drawTime(canvas, lastTime.getHours(), lastTime.getMinutes());
           	
           	canvas.drawPoint(ball.x, ball.y, linePaint);
        }
        
        private void drawTime(Canvas canvas, int hours, int minutes ) {
        	drawNumber(canvas, canvasWidth/2 - 100,            50, hours / 10);
        	drawNumber(canvas, canvasWidth/2 - (100 - SP),     50, hours % 10);
        	drawNumber(canvas, canvasWidth/2 + (100 - SP * 2), 50, minutes / 10);
        	drawNumber(canvas, canvasWidth/2 + (100 - SP),     50, minutes % 10);
        }
        
        private void drawNumber(Canvas canvas, int x, int y, int n) {
        	canvas.save();
        	canvas.translate(x, y);
        	canvas.drawLines(numbers[n], linePaint);
        	canvas.restore();
        }
        
        private void drawField(Canvas c) {
            c.drawLine(0, 3, canvasWidth, 3, linePaint);
            c.drawLine(0, canvasHeight-4, canvasWidth, canvasHeight-4, linePaint);
            c.drawLine(canvasWidth / 2, 0, canvasWidth / 2, canvasHeight, dashedLinePaint);
            c.drawLine(leftPanel.x, leftPanel.y - PANEL_LENGTH, leftPanel.x, leftPanel.y + PANEL_LENGTH, linePaint);
            c.drawLine(rightPanel.x, rightPanel.y - PANEL_LENGTH, rightPanel.x, rightPanel.y + PANEL_LENGTH, linePaint);            
        }
        
        private void updatePhysics(long deltaMillis) {
        	float distance = ball.speed / (1000/deltaMillis);
        	float dX = (float)(distance * Math.cos(ball.direction));
        	float dY = (float)(distance * Math.sin(ball.direction));
        	        	        
        	// check if ball bounces at playfield (top and bottom)
        	if (ball.y + dY > playFieldY2 ) {
        		ball.y = playFieldY2 - ((ball.y + dY) - playFieldY2);
            	ball.direction = -ball.direction; 
        	}
        	else if (ball.y + dY < playFieldY1) {
        		ball.y = playFieldY1 + (playFieldY1 - (ball.y + dY));
        		ball.direction = -ball.direction;
        	}    
        	else {
        		ball.y += dY;
        	}
        	
        	// check if ball bounces at panels
        	if ((ball.x + dX > rightPanel.x) && 
        			(ball.y > rightPanel.y - PANEL_LENGTH) && 
        			(ball.y < rightPanel.y + PANEL_LENGTH)) {
        		ball.x = rightPanel.x - 5 -(rightPanel.x - (ball.x + dX));
        		ball.direction = -(float)(ball.direction + Math.PI);
        	}        	
        	else if ((ball.x + dX < leftPanel.x) &&
        			(ball.y > leftPanel.y - PANEL_LENGTH) &&
        			(ball.y < leftPanel.y + PANEL_LENGTH)) {
        		ball.x = leftPanel.x + 5 + (leftPanel.x - (ball.x + dX));
        		ball.direction = -(float)(ball.direction - Math.PI);
        	}
        	else {
        		ball.x += dX;
        	}
        	
        	if ((dX > 0) && (ball.x > canvasWidth / 2)) {
        		float dPanel = ball.y - rightPanel.y;
        		if (Math.abs(dPanel) > 10) {
                	float panelDistance = PANEL_SPEED / (1000/deltaMillis);
                	rightPanel.y += (dPanel > 0) ? panelDistance : -panelDistance;
        		}
        	}
        	
        	
        }
        
		
        @Override
        public void run() {
            while (running) {
                Canvas c = null;
                try {
                    c = surfaceHolder.lockCanvas(null);
                    if (mode == STATE_RUNNING) {
                        long now = System.currentTimeMillis();                        
                    	updatePhysics(now - lastTimeMillis);
                    	lastTimeMillis = now;
                    	synchronized (surfaceHolder) {
                        	doDraw(c);
                    	}
                    }
                    try {
                    	Thread.sleep(20);
                    }
                    catch (InterruptedException e) {
                    	// 
                    }
                    
                } 
                finally {
                    // do this in a finally so that if an exception is thrown
                    // during the above, we don't leave the Surface in an
                    // inconsistent state
                    if (c != null) {
                        surfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }
		
	} // PongThread

    public PongView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        thread = new PongThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
                statusText.setVisibility(m.getData().getInt("viz"));
                statusText.setText(m.getData().getString("text"));
            }
        });
        setFocusable(true); // make sure we get key events
    }
    
    public PongThread getThread() {
    	return thread;
    }
    
    public void setTextView(TextView textView) {
        statusText = textView;
    }
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
            int height) {
        thread.setSurfaceSize(width, height);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
        thread.setRunning(true);
        thread.start();
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // we have to tell thread to shut down & wait for it to finish, or else
        // it might touch the Surface after we return and explode
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } 
            catch (InterruptedException e) {
            }
        }
    }
	
}