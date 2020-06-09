package com.example.snake_app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Random;

public class GameActivity extends Activity implements SensorEventListener {
    SnakeView snakeView;
    int screenWidth, screenHeight, fps, blockSize, numBlocksHorizontal, numBlocksVertical, topGap, highest;
    long lastFrameTime;

    private SensorManager sensorManager;
    private Sensor accel, gyro;
    private float y,z;

    static FirebaseAuth mAuth;
    static String user;
    private static boolean sensorsDetected;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        snakeView = new SnakeView(this);
        setContentView(snakeView);

        run();
    }

    public void run() {
        createPlayField();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if(currentUser != null)
            user = currentUser.getEmail(); else user = "guest";

        if(testSensors()) {
            startMotionSensors();
            sensorsDetected = true;
        } else {
            Toast.makeText(GameActivity.this, "no sensors detected\nmanual control only", Toast.LENGTH_SHORT).show();
            sensorsDetected = false;
        }
    }

    public boolean testSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        return (sensorManager != null && sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null && sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null);
    }

    public void startMotionSensors() {
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accel, 20000);
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorManager.registerListener(this, gyro, 20000);
    }

    public void stopMotionSensor() {
        if(sensorsDetected) {
            sensorManager.unregisterListener(this, accel);
            sensorManager.unregisterListener(this, gyro);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(sensorsDetected) {
            Sensor sensor = event.sensor;
            if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                z = event.values[2];
            } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
                y = event.values[1];
            }

            if (z > 2.01) {
                if(snakeView.getSnake().getHeadDirection() != 2)
                    snakeView.getSnake().setHeadDirection(0);
                //up
            } else if (z < -2.01) {
                if(snakeView.getSnake().getHeadDirection() != 0)
                    snakeView.getSnake().setHeadDirection(2);
                //down
            } else if(y > 2.01) {
                if(snakeView.getSnake().getHeadDirection() != 3)
                    snakeView.getSnake().setHeadDirection(1);
                //right
            } else if (y < -2.01) {
                if(snakeView.getSnake().getHeadDirection() != 1)
                    snakeView.getSnake().setHeadDirection(3);
                //left
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    class SnakeView extends SurfaceView implements Runnable {

        private Goal goal;
        private Snake snake;
        private Paint paint;

        private Thread thread;
        private boolean isPlaying;
        private SurfaceHolder surfaceHolder;

        private boolean dialogOpen = false;

        private float x1,x2, y1, y2;
        private static final int MIN_SWIPE_DISTANCE = 120;

        public SnakeView(Context context) {
            super(context);

            snake = new Snake();
            snake.setPosX(new int[200]);
            snake.setPosY(new int[200]);

            goal = new Goal();
            goal.setPosX(new int[1]);
            goal.setPosY(new int[1]);

            paint = new Paint();
            surfaceHolder = getHolder();
        }

        public void setSnake() {
            snake.setSnakeLength(1);
            snake.setPosX(0, numBlocksHorizontal/2);
            snake.setPosY(0, numBlocksVertical/2);
        }

        public void setGoal(){
            Random random = new Random();
            goal.setPosX(0, random.nextInt(numBlocksHorizontal-1)+1);
            goal.setPosY(0, random.nextInt(numBlocksVertical-1)+1);
        }

        @Override
        public void run() {
            updateHighestScore();
            while (isPlaying){
                update();

                draw();

                fps();
            }
        }

        private void update() {
            if(snake.getPosX()[0] == goal.getPosX()[0] && snake.getPosY()[0] == goal.getPosY()[0]) {
                snake.setSnakeLength(snake.getSnakeLength()+1);
                setGoal();
            }

            for(int i=snake.getSnakeLength(); i > 0 ; i--){
                snake.setPosX(i, snake.getPosX()[i-1]);
                snake.setPosY(i, snake.getPosY()[i-1]);
            }

            switch (snake.getHeadDirection()){
                case 0://up
                    snake.setPosY(0, snake.getPosY()[0]-1);
                    break;

                case 1://right
                    snake.setPosX(0, snake.getPosX()[0]+1);
                    break;

                case 2://down
                    snake.setPosY(0, snake.getPosY()[0]+1);
                    break;

                case 3://left
                    snake.setPosX(0, snake.getPosX()[0]-1);
                    break;
            }

            boolean dead = false;
            if(snake.getPosX()[0] == -1 || snake.getPosX()[0] >= numBlocksHorizontal || snake.getPosY()[0] == -1 || snake.getPosY()[0] == numBlocksVertical)
                dead = true;

            for (int i = snake.getSnakeLength(); i > 0; i--) {
                if ((i > 2) && (snake.getPosX()[0] == snake.getPosX()[i]) && (snake.getPosY()[0] == snake.getPosY()[i])) {
                    dead = true;
                }
            }

            if(dead){
                updateHighestScore();
                setSnake();
                setGoal();
            }
        }

        private void fps() {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 200 - timeThisFrame;
            if (timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if (timeToSleep > 0) {
                try {
                    Thread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            lastFrameTime = System.currentTimeMillis();
        }

        private void draw() {
            if(surfaceHolder.getSurface().isValid()) {
                Canvas canvas = surfaceHolder.lockCanvas();
                canvas.drawColor(Color.BLACK);

                paint.setColor(Color.WHITE);
                paint.setTextSize(36);
                canvas.drawText("fps: "+fps + " score: "+snake.getSnakeLength()+ " highest: "+highest,50, 50, paint);
                canvas.drawText(user,900,50, paint);

                paint.setStrokeWidth(3);
                canvas.drawLine(1,topGap,screenWidth-1,topGap, paint);
                canvas.drawLine(screenWidth-1,topGap,screenWidth-1,topGap+(numBlocksVertical*blockSize), paint);
                canvas.drawLine(screenWidth-1,topGap+(numBlocksVertical*blockSize),1,topGap+(numBlocksVertical*blockSize),paint);
                canvas.drawLine(1,topGap, 1,topGap+(numBlocksVertical*blockSize), paint);


                canvas.drawBitmap(snake.getBitmap(), snake.getPosX()[0]*blockSize, (snake.getPosY()[0]*blockSize)+topGap, paint);
                for(int i = 1; i < snake.getSnakeLength();i++){
                    canvas.drawBitmap(snake.getBitmap(), snake.getPosX()[i]*blockSize, (snake.getPosY()[i]*blockSize)+topGap, paint);
                }
                canvas.drawBitmap(goal.getBitmap(), goal.getPosX()[0]*blockSize, (goal.getPosY()[0]*blockSize)+topGap, paint);

                surfaceHolder.unlockCanvasAndPost(canvas);
            }
        }

        public void updateHighestScore() {
            if(highest < snake.getSnakeLength())
                highest = snake.getSnakeLength();
        }

        public void createDialog() {
            if(!dialogOpen) {
                dialogOpen = true;
                AlertDialog.Builder alert = new AlertDialog.Builder(GameActivity.this)
                        .setCancelable(true)
                        .setTitle("What u want?")
                        .setNegativeButton("Resume", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                resume();
                                dialogOpen = false;
                            }
                        })
                        .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                saveAndExit();
                                dialogOpen = false;
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                resume();
                                dialogOpen = false;
                            }
                        });

                AlertDialog dialog = alert.create();
                dialog.show();
            }
        }

        public Snake getSnake() {
            return snake;
        }

        public Goal getGoal() {
            return goal;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x1 = event.getX();
                    y1 = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                    x2 = event.getX();
                    y2 = event.getY();
                    float deltaX = x2 - x1;
                    float deltaY = y2 - y1;

                    if (Math.abs(deltaX) > MIN_SWIPE_DISTANCE)
                    {
                        if(deltaX > 0 && snakeView.getSnake().getHeadDirection() != 3)
                            snakeView.getSnake().setHeadDirection(1); // right
                        else if(deltaX < 0 && snakeView.getSnake().getHeadDirection() != 1)
                            snakeView.getSnake().setHeadDirection(3); // left
                    } else if (Math.abs(deltaY) > MIN_SWIPE_DISTANCE)
                    {
                        if(deltaY > 0 && snakeView.getSnake().getHeadDirection() != 0)
                            snakeView.getSnake().setHeadDirection(2); // up
                        else if(deltaY < 0 && snakeView.getSnake().getHeadDirection() != 2)
                            snakeView.getSnake().setHeadDirection(0); // down
                    } else if (Math.abs(deltaX) < MIN_SWIPE_DISTANCE && Math.abs(deltaY) < MIN_SWIPE_DISTANCE) {
                        performClick();
                        if(isPlaying)
                            pause();
                            createDialog();
                    }
                    break;
            }
            return true;
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        public void saveAndExit() {
            pause();
            Intent returnResult = new Intent();
            returnResult.putExtra("score", highest);
            highest = 0;
            setResult(Activity.RESULT_OK, returnResult);
            finish();
        }

        public void resume() {
            isPlaying = true;
            thread = new Thread(this);
            thread.start();
            if(testSensors())
                startMotionSensors();
        }

        public void pause() {
            stopMotionSensor();
            isPlaying = false;
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void createPlayField() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        blockSize = screenWidth/20;
        topGap = screenHeight/14;
        numBlocksHorizontal = 20;
        numBlocksVertical = ((screenHeight-topGap))/blockSize;

        snakeView.setSnake();
        Bitmap snakeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.snake);
        snakeView.getSnake().setBitmap(Bitmap.createScaledBitmap(snakeBitmap, blockSize, blockSize, false));
        snakeBitmap.recycle();

        snakeView.setGoal();
        Bitmap goalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.goal);
        snakeView.getGoal().setBitmap(Bitmap.createScaledBitmap(goalBitmap, blockSize, blockSize, false));
        goalBitmap.recycle();
    }

    @Override
    protected void onStop() {
        super.onStop();
        snakeView.saveAndExit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        snakeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        snakeView.pause();
    }
}
