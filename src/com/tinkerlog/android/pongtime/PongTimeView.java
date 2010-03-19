package com.tinkerlog.android.pongtime;

import java.util.Date;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Typeface;
import android.graphics.Paint.Cap;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * View and thread for updating the pong playfield.
 * Based on the LunarLander example. 
 * 
 * This code is not as beauty as could be. It is optimized to avoid garbage collection
 * which would make the display updates stutter.
 */
public class PongTimeView extends SurfaceView implements SurfaceHolder.Callback {

	private PongThread thread;
	private static final boolean DEBUG = false;
	
	class Panel {
		public int x;
		public int y;
		public Panel(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}
	
	class Ball {
		public float x;
		public float y;
		public float direction;
		public float cosDir;
		public float sinDir;
		public Ball(float x, float y) {
			this.x = x;
			this.y = y;
		}
		public void computeDir() {
			cosDir = (float)Math.cos(direction);
			sinDir = (float)Math.sin(direction);
		}
	}
    
	class PongThread extends Thread {

        public static final int STATE_PAUSE = 1;
        public static final int STATE_RUNNING = 2;
                
        public static final int GSTATE_NONE = 0;
        public static final int GSTATE_HOURSWIN = 1;
        public static final int GSTATE_MINUTESWIN = 2;
        public static final int GSTATE_PLAY = 3;
        public static final int GSTATE_STOPPED = 4;
        
        private static final int NUMBER_Y = 50;
        
        private static final float BALL_SPEED = 300.0F;  // pixels per second
        private static final float PANEL_SPEED = 250.0F; // pixels per second
        private static final int PANEL_LENGTH = 20;     
        private static final int PANEL_XPOS = 25;
        private static final float TWOPI = (float)(Math.PI * 2);
        private static final float MIN_RANGLE = (float)(3 * (Math.PI/4));
        private static final float MAX_RANGLE = (float)(5 * (Math.PI/4));
        
        private static final int LINE_WIDTH = 7;         // line width to draw the playfield
        private static final int PANEL_LINE_WIDTH = 9;   // line width to draw the panels
        private static final int LW = 15;                // letter width
        private static final int LH = 26;                // letter height
        private static final int LH2 = LH / 2;            
        private static final int SP = LW + 15;           // letter width + space

        private Ball ball;
        private Panel leftPanel;
        private Panel rightPanel;
        
        private int number1X;                            // xpos of number 1 (hours)
        private int number2X;                            // xpos of number 2 (hours)
        private int number3X;                            // xpos of number 3 (minutes)
        private int number4X;                            // xpos of number 4 (minutes)
        
        private int currentHours;
        private int currentMinutes;
        private int currentFPS;
        private int waitCount;
        
        private int playFieldY1;                         // playfield frame
        private int playFieldY2;
        private int playFieldX1;
        private int playFieldX2;

		private boolean running;
		private long lastTimeMillis;
		private long nextTimeUpdate;
		private Date lastTime;
		private int mode;
		private int gMode;
		private boolean showFPS;
		
		private int canvasHeight;
		private int canvasHeight2;	// height / 2
		private int canvasWidth;
		private int canvasWidth2;	// width / 2;
		
        private SurfaceHolder surfaceHolder;
        private Paint dashedLinePaint;
        private Paint linePaint;
        private Paint panelPaint;
        private Paint textPaint;

        private Date currentDate = new Date();
        
        private float[][] numbers = {					// lines to draw numbers
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
        
        public PongThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;

            linePaint = new Paint();
            linePaint.setColor(Color.WHITE);
            linePaint.setStyle(Paint.Style.STROKE);            
            linePaint.setStrokeWidth(LINE_WIDTH);
            linePaint.setStrokeCap(Cap.SQUARE);

            panelPaint = new Paint();
            panelPaint.setColor(Color.WHITE);
            panelPaint.setStyle(Paint.Style.STROKE);            
            panelPaint.setStrokeWidth(PANEL_LINE_WIDTH);
            panelPaint.setStrokeCap(Cap.SQUARE);
            
            dashedLinePaint = new Paint();
            dashedLinePaint.setColor(Color.WHITE);
            dashedLinePaint.setStyle(Paint.Style.STROKE);
            dashedLinePaint.setStrokeWidth(LINE_WIDTH);
            PathEffect pe = new DashPathEffect(new float[] {20, 16}, 0.0F);
            dashedLinePaint.setPathEffect(pe);
            
            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setTextSize(15);
            textPaint.setColor(Color.GRAY);
            textPaint.setTypeface(Typeface.MONOSPACE);
            
            lastTime = new Date(); 
            currentHours = lastTime.getHours();
            currentMinutes = lastTime.getMinutes();
            
            mode = STATE_PAUSE;
            if (DEBUG) {
            	Log.i(this.getClass().getName(), "PongThread");
            }
        }		        
        
        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (surfaceHolder) {
                canvasWidth = width;
                canvasHeight = height;
                canvasWidth2 = canvasWidth / 2;
                canvasHeight2 = canvasHeight / 2;                
                playFieldY1 = 8;
                playFieldY2 = canvasHeight - 9;
                playFieldX1 = 5;
                playFieldX2 = canvasWidth - 5;
                
                leftPanel = new Panel(PANEL_XPOS, canvasHeight2);
                rightPanel = new Panel(canvasWidth - PANEL_XPOS, canvasHeight2);
                ball = new Ball(100, 100);
          
                number1X = canvasWidth2 - 100;
                number2X = canvasWidth2 - (100 - SP);
                number3X = canvasWidth2 + (100 - SP - LW);
                number4X = canvasWidth2 + (100 - LW);
                
                gMode = GSTATE_NONE;
                lastTimeMillis = System.currentTimeMillis();
                nextTimeUpdate = (lastTimeMillis / 1000) * 1000;
                
                newGame(true);
                setRunning(true);
                mode = STATE_RUNNING;
                if (DEBUG) {
                	Log.i(this.getClass().getName(), "setSurfaceSize");
                }
            }
        }   
        
        public void toggleFPS() {
        	showFPS = !showFPS; 
        }

        public void setRunning(boolean running) {
        	this.running = running;
        }
        
        private void newGame(boolean left) {
        	if (DEBUG) {
        		Log.i(this.getClass().getName(), "new game, left wins: " + left);
        	}
        	ball.y = canvasHeight2;
        	ball.x = (left) ? canvasWidth2 - 40 : canvasWidth2 + 40;
        	float d = (float)(Math.random() * 0.8 - 0.4);
        	ball.direction = (left) ? 0.0F + d : (float)Math.PI + d;
        	ball.computeDir();
        	
        	leftPanel.y = canvasHeight2;
        	rightPanel.y = canvasHeight2;
        	if (DEBUG) {
        		Log.i(this.getClass().getName(), "--> " + (left ? "left" : "right") + " wins, new game: " + ball.x + " " + ball.y);
        	}
        }
                
        private void doDraw(Canvas canvas) {
        	canvas.drawColor(Color.BLACK);
        	if (showFPS) {
        		canvas.drawText("FPS:" + currentFPS, 10, 25, textPaint);
        	}
        	drawBall(canvas);
        	drawFieldAndPanels(canvas);
           	drawTime(canvas, currentHours, currentMinutes);
        }
        
        private void drawBall(Canvas canvas) {
           	canvas.drawPoint(ball.x, ball.y, panelPaint);        	
        }
        
        private void drawTime(Canvas canvas, int hours, int minutes ) {        	
        	drawNumber(canvas, number1X, NUMBER_Y, hours / 10);
        	drawNumber(canvas, number2X, NUMBER_Y, hours % 10);
        	drawNumber(canvas, number3X, NUMBER_Y, minutes / 10);
        	drawNumber(canvas, number4X, NUMBER_Y, minutes % 10);
        }
        
        private void drawNumber(Canvas canvas, int x, int y, int n) {
        	canvas.save();
        	canvas.translate(x, y);
        	canvas.drawLines(numbers[n], linePaint);
        	canvas.restore();
        }
        
        private void drawFieldAndPanels(Canvas c) {
            c.drawLine(0, 3, canvasWidth, 3, linePaint);
            c.drawLine(0, canvasHeight-4, canvasWidth, canvasHeight-4, linePaint);
            c.drawLine(canvasWidth / 2, 0, canvasWidth / 2, canvasHeight, dashedLinePaint);
            c.drawLine(leftPanel.x, leftPanel.y - PANEL_LENGTH, leftPanel.x, leftPanel.y + PANEL_LENGTH, panelPaint);
            c.drawLine(rightPanel.x, rightPanel.y - PANEL_LENGTH, rightPanel.x, rightPanel.y + PANEL_LENGTH, panelPaint);            
        }
        
        private void updatePhysics(long now, long deltaMillis) {

        	// update time
        	updateTime(now);        
        	
        	if (gMode == GSTATE_STOPPED || gMode == GSTATE_NONE) {
        		return;
        	}

        	float distance = BALL_SPEED / (1000/deltaMillis);
        	float dX = (float)(distance * ball.cosDir);
        	float dY = (float)(distance * ball.sinDir);
            
        	// move panels only if neither is about to win
        	if (gMode != GSTATE_HOURSWIN) {
        		if ((dX > 0) && (ball.x > canvasWidth2)) {        		
        			movePanel(rightPanel, (int)ball.y, deltaMillis);
        		}
        		else {
        			movePanel(rightPanel, canvasHeight2, deltaMillis);        		
        		}        	
        	}
    		if (gMode != GSTATE_MINUTESWIN) {
    			if ((dX < 0) && (ball.x < canvasWidth2)) {
    				movePanel(leftPanel, (int)ball.y, deltaMillis);
    			}
    			else {
    				movePanel(leftPanel, canvasHeight2, deltaMillis);
    			}
    		}        	
        	
        	// check if ball bounces at playfield (top and bottom)
        	ball.y += dY;
        	if (ball.y < playFieldY1) {
        		ball.y = playFieldY1 + (playFieldY1 - ball.y);
        		ball.direction = -ball.direction;
        		ball.computeDir();
        	}
        	else if (ball.y > playFieldY2) {
        		ball.y = playFieldY2 - (ball.y - playFieldY2);
            	ball.direction = -ball.direction;
            	ball.computeDir();
        	}
        	
        	// check if ball bounces at panels
        	ball.x += dX;
        	if ((ball.x > rightPanel.x) &&
    			(ball.y > rightPanel.y - PANEL_LENGTH) && 
    			(ball.y < rightPanel.y + PANEL_LENGTH)) {
        		ball.x = rightPanel.x + (rightPanel.x - ball.x);
        		// try to keep the ball direction in between +/- 45 degrees
        		ball.direction = (float)(-ball.direction + Math.PI + Math.random() * 0.6 - 0.3);
        		ball.direction = (ball.direction > TWOPI) ? ball.direction - TWOPI : (ball.direction < 0) ? ball.direction + TWOPI : ball.direction;
        		if (ball.direction < MIN_RANGLE) {
        			ball.direction = MIN_RANGLE;
        		}
        		else if (ball.direction > MAX_RANGLE) {
        			ball.direction = MAX_RANGLE;
        		}
        		ball.computeDir();
    		}
        	else if ((ball.x < leftPanel.x) &&
        			(ball.y > leftPanel.y - PANEL_LENGTH) &&
        			(ball.y < leftPanel.y + PANEL_LENGTH)) {
        		ball.x = leftPanel.x + (leftPanel.x - (ball.x));
        		ball.direction = -(float)(ball.direction - Math.PI + Math.random() * 0.6 - 0.3);
        		ball.computeDir();
        	}
        	        	
        	// check if ball leaves the playfield.
        	if ((ball.x < playFieldX1) || (ball.x > playFieldX2)) {
        		newGame(gMode != GSTATE_MINUTESWIN);
        		if ((gMode == GSTATE_HOURSWIN) || (gMode == GSTATE_MINUTESWIN)) {
            		if (DEBUG) {
            			Log.i(this.getClass().getName(), "stopped");
            		}
        			gMode = GSTATE_STOPPED;
        		}
        		else if (DEBUG) {
            		Log.i(this.getClass().getName(), "oops, we lose! " + ball.x);        			
        		}
        	}
        }
        
        private void movePanel(Panel p, int target, long deltaMillis) {
        	int dPanel = target - p.y;
        	if (Math.abs(dPanel) > 6) {
        		int distance = (int)(PANEL_SPEED / (1000/deltaMillis));
        		p.y += (dPanel > 0) ? distance : -distance;
        	}
        }
        
        private void updateTime(long now) {
        	now = System.currentTimeMillis();
        	if (now > nextTimeUpdate) {
        		nextTimeUpdate += 1000;
        		currentDate.setTime(now);
        		switch (gMode) {
        		case GSTATE_MINUTESWIN:
        			break;
        		case GSTATE_HOURSWIN:
        			break;
        		case GSTATE_PLAY:        			
            		if (currentHours != currentDate.getHours()) {
            			gMode = GSTATE_HOURSWIN;
            			if (DEBUG) {
            				Log.i(this.getClass().getName(), "hours!");
            			}
            		}
            		else if (currentMinutes != currentDate.getMinutes()) {
            			gMode = GSTATE_MINUTESWIN;
            			if (DEBUG) {
            				Log.i(this.getClass().getName(), "minutes!");
            			}
            		}
            		else {
            			currentHours = currentDate.getHours();
            			currentMinutes = currentDate.getMinutes();
            		}
        			break;
        		case GSTATE_STOPPED:
        			waitCount++;
        			if (waitCount == 2) {
        				gMode = GSTATE_PLAY;
        				waitCount = 0;
        			}
        			currentHours = currentDate.getHours();
        			currentMinutes = currentDate.getMinutes();
        			if (DEBUG) {
        				Log.i(this.getClass().getName(), "play on");
        			}
        			break;
        		case GSTATE_NONE:
        			if (DEBUG) {
        				Log.i(this.getClass().getName(), "waiting ...");
        			}
        			waitCount++;
        			if (waitCount >= 3) {
        				gMode = GSTATE_PLAY;
        				waitCount = 0;
        			}
        			break;
        		default :
        			if (DEBUG) {
        				Log.i(this.getClass().getName(), "ooops! " + gMode);
        			}
        			gMode = GSTATE_PLAY;
        		}
        	}
        }
        
        @Override
        public void run() {
        	int count = 0;
        	long t1 = System.currentTimeMillis();
        	if (DEBUG) {
        		Log.i(this.getClass().getName(), "entering run");
        	}
            while (running) {
                Canvas c = null;
                try {
                    c = surfaceHolder.lockCanvas(null);
                    if (mode == STATE_RUNNING) {
                    	count++;
                        long now = System.currentTimeMillis(); 
                        long delta = now - lastTimeMillis;
                        updatePhysics(now, delta);
                        lastTimeMillis = now;

                        if (now - t1 > 5000) {
                        	currentFPS = count/5;
                        	t1 = now;
                        	count = 0;
                        }
                        synchronized (surfaceHolder) {
                        	doDraw(c);
                        }
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
		
    public PongTimeView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // register our interest in hearing about changes to our surface
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        // create thread only; it's started in surfaceCreated()
        thread = new PongThread(holder);
        setFocusable(true); // make sure we get key events
    }
    
    public PongThread getThread() {
    	return thread;
    }
    
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        thread.setSurfaceSize(width, height);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        // start the thread here so that we don't busy-wait in run()
        // waiting for the surface to be created
    	if (DEBUG) {
    		Log.i(this.getClass().getName(), "surfaceCreated");
    	}
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