package com.driuki.breakout;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;

public class BreakoutEngine extends SurfaceView implements Runnable{

    // This is our thread
    private Thread gameThread = null;

    // This is new. We need a SurfaceHolder
    // When we use Paint and Canvas in a thread
    // We will see it in action in the draw method soon.
    private SurfaceHolder ourHolder;

    // A boolean which we will set and unset
    // When the game is running - or not.
    // Volatile shows that this variable is accessed within and outside Thread
    private volatile boolean playing;

    // Game is paused at the start
    private boolean paused = true;

    // A canvas and a Paint object
    private Canvas canvas;
    private Paint paint;

    // How wide and high is the screen
    private int screenX;
    private int screenY;

    // This variable tracks the game frame rate
    private long fps;

    // This isused to help calculate the fps
    private long timeThisFrame;

    // The player's bat, ball
    Bat bat;
    Ball ball;

    // Up to 200 bricks
    Brick[] bricks = new Brick[200];
    int numBricks = 0;

    // For sound FX
    SoundPool soundPool;
    int beep1ID = -1;
    int beep2ID = -1;
    int beep3ID = -1;
    int loseLifeID = -1;
    int explodeID = -1;

    // The score
    int score = 0;

    // Lives
    int lives = 3;

    // The constructor is called when the object is first created
    public BreakoutEngine(Context context, int x, int y) {
        // This calls the default constructor to setup the rest of the object
        super(context);

        // Initialize ourHolder an paint objects
        ourHolder = getHolder();
        paint = new Paint();

        // Initialize screenX and screenY because x and y are local
        screenX = x;
        screenY = y;

        // Initialize the player's bat

        bat = new Bat(screenX, screenY);
        ball = new Ball();

        // Load the sounds
        // This SoundPool is deprecated but don't worry
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

        try {
            // Create objects of the 2 required classes
            AssetManager assetManager = context.getAssets();
            AssetFileDescriptor descriptor;

            // Load our fx in memory ready for use
            descriptor = assetManager.openFd("beep1.ogg");
            beep1ID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("beep2.ogg");
            beep2ID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("beep3.ogg");
            beep3ID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("loseLife.ogg");
            loseLifeID = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("explode.ogg");
            explodeID = soundPool.load(descriptor, 0);

        } catch (IOException e) {
            // Print an error message to the console
            Log.e("error", "Failed to load sound files");
        }

        restart();

    }

    // Runs when the OS calls onPause on BreakoutActivity method
    public void pause() {

        playing = false;
        try {
            gameThread.join();
        }catch (InterruptedException e) {
            Log.e("ERROR: ", "Joining thread");
        }

    }

    // Runs when the OS calls onResume on BreakoutActivity method
    public void resume() {

        playing = true;
        gameThread = new Thread(this);
        gameThread.start();

    }

    @Override
    public void run() {
        while (playing) {

            // Capture the current time in milliseconds in startFrameTime
            long startFrameTime = System.currentTimeMillis();

            // Update the frame
            if (!paused) {
                update();
            }

            // Draw the frame
            draw();

            // Calculate the fps this frame
            // We can then use the result to
            // time animations and more
            timeThisFrame = System.currentTimeMillis() - startFrameTime;
            if (timeThisFrame >= 1) {
                fps = 1000 / timeThisFrame;
            }
        }
    }

    private void update() {
        // Update the ball and bat
        bat.update(fps);
        ball.update(fps);

        // Check for ball colliding with the brick
        for (int i = 0; i < numBricks; i++) {
            if (bricks[i].getVisibility()) {
                if (RectF.intersects(bricks[i].getRect(), ball.getRect())) {
                    bricks[i].setInvisible();
                    ball.reverseYVelocity();
                    score = score + 10;
                    soundPool.play(explodeID, 1, 1, 0, 0, 1);
                }
            }
        }

        if (RectF.intersects(bat.getRect(), ball.getRect())) {
            ball.setRandomXVelocity();
            ball.reverseYVelocity();
            // So it won't get caught in the loop
            ball.clearObstacleY(bat.getRect().top - 2);
            soundPool.play(beep1ID, 1, 1, 0, 0, 1);
        }

        // Bounce the ball back when it hits the bottom of the screen
        // And deduct a life
        if (ball.getRect().bottom > screenY) {
            ball.reverseYVelocity();
            // That bumps the ball by 2px up to avoid ball getting stuck
            ball.clearObstacleY(screenY - 2);

            // Lose a life
            lives --;
            soundPool.play(loseLifeID, 1, 1, 0, 0, 1);

            if (lives == 0) {
                paused = true;
                restart();
            }
        }

        // Bounce the ball back when it hits the top of screen
        if (ball.getRect().top < 0) {
            ball.reverseYVelocity();
            // Bump ball down 12px down so it does not stuck
            ball.clearObstacleY(12);
            soundPool.play(beep2ID, 1, 1, 0, 0, 1);
        }

        // If the ball hits left wall bounce
        if (ball.getRect().left < 0) {
            ball.reverseXVelocity();
            ball.clearObstackleX(2);
            soundPool.play(beep3ID, 1, 1, 0, 0, 1);
        }

        // If the ball hits right wall bounce
        // screenX - 10 - because ball is measured from the left hand side so it would
        // bounce as soon right ide hits the side
        if (ball.getRect().right > screenX - 10) {
            ball.reverseXVelocity();
            ball.clearObstackleX(screenX - 22);
            soundPool.play(beep3ID, 1, 1, 0, 0, 1);
        }

        // Pause if cleared screen
        if (score == numBricks * 10) {
            paused = true;
            restart();
        }

    }

    void restart() {
        // Put the ball back to the start
        ball.reset(screenX,screenY);

        // Brick's dimensions
        int brickWidth = screenX / 8;
        int brickHeight = screenY / 10;

        // Build a wall of bricks
        numBricks = 0;

        for (int column = 0; column < 8; column++) {
            for (int row = 0; row < 3; row++) {
                // Initialize new brick in array at number of array numBricks
                bricks[numBricks] = new Brick(row, column, brickWidth, brickHeight);
                numBricks ++;
            }
        }

        score = 0;
        lives = 3;

    }

    private void draw() {

        // Mke sure our drawing surface is valid or game will crash
        if (ourHolder.getSurface().isValid()) {
            // Lock the canvas ready to draw
            canvas = ourHolder.lockCanvas();

            // Draw the background color
            canvas.drawColor(Color.argb(255, 26, 128, 182));

            // Draw everything to the screen

            // Choose the brush color for drawing
            paint.setColor(Color.argb(255, 255, 255, 255));

            // Draw the bat, ball
            canvas.drawRect(bat.getRect(), paint);
            canvas.drawRect(ball.getRect(), paint);

            // Draw bricks
            // Change the brush color for drawing
            paint.setColor(Color.argb(255, 249, 129, 0));
            // Draw the bricks if visible
            for (int i = 0; i < numBricks; i++) {
                // If get's true makes the brick
                if (bricks[i].getVisibility()) {
                    canvas.drawRect(bricks[i].getRect(), paint);
                }
            }

            // Draw the HUD
            // Choose the brush color for drawing
            paint.setColor(Color.argb(255, 255, 255, 255));

            // Draw the score
            paint.setTextSize(50);
            canvas.drawText("Score: " + score + "  Lives: " + lives, 10, 80, paint);

            // Show everything we have drawn
            ourHolder.unlockCanvasAndPost(canvas);
        }

    }

    // TheSurfaceView class implements onTouchListener
    // So we can override this method and detect screen touches
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        // Our code
        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

            // Player has touched the screen
            case MotionEvent.ACTION_DOWN:
                paused = false;

                if (motionEvent.getX() > screenX / 2) {
                    bat.setMovementState(bat.RIGHT);
                } else {
                    bat.setMovementState(bat.LEFT);
                }

                break;

            // Player has removed finger from screen
            case MotionEvent.ACTION_UP:
                bat.setMovementState(bat.STOPPED);
                break;

        }

        return true;

    }

}
