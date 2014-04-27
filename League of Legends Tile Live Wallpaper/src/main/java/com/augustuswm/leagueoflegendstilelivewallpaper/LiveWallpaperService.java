package com.augustuswm.leagueoflegendstilelivewallpaper;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;

import java.io.IOException;
import java.io.InputStream;

public class LiveWallpaperService extends WallpaperService
{
    int x,y;

    public void onCreate()
    {
        super.onCreate();
    }

    public void onDestroy()
    {
        super.onDestroy();
    }

    public Engine onCreateEngine()
    {
        return new LCSWallpaperEngine();
    }

    class LCSWallpaperEngine extends Engine {

        // Gesture detector to detect single taps.
        private GestureDetector.SimpleOnGestureListener gestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap (MotionEvent e) {
                freeze(e, false);
                return true;
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                freeze(e, true);
                return true;
            }
        };

        GestureDetector mGestureDetector = new GestureDetector(getApplicationContext(), gestureListener);

        private final Handler handler = new Handler();
        private final Runnable drawRunner = new Runnable() {
            @Override
            public void run() {
                draw();
            }
        };
        private boolean visible = true;
        public int width, height, time_to_animate = 0;

        public LCSColumn[] display_columns = new LCSColumn[5];

        public BitmapRegionDecoder borderDecoder;
        public Bitmap border;
        public long time = 0;

        LCSWallpaperEngine() {

            display_columns[0] = new LCSColumn(
                    getApplicationContext(),
                    new int[]{ R.drawable.akali, R.drawable.corki, R.drawable.sejuani },
                    new int[]{ 1400, 1230, 1080 },
                    3,
                    0
            );

            display_columns[1] = new LCSColumn(
                    getApplicationContext(),
                    new int[]{ R.drawable.alistar, R.drawable.nunu },
                    new int[]{ 1260, 1190 },
                    0,
                    1
            );

            display_columns[2] = new LCSColumn(
                    getApplicationContext(),
                    new int[]{ R.drawable.elise, R.drawable.elise, R.drawable.elise },
                    new int[]{ 1383, 1383, 1383 },
                    1,
                    2
            );

            display_columns[3] = new LCSColumn(
                    getApplicationContext(),
                    new int[]{ R.drawable.nunu,R.drawable.alistar },
                    new int[]{ 1190, 1260 },
                    2,
                    3
            );

            display_columns[4] = new LCSColumn(
                    getApplicationContext(),
                    new int[]{ R.drawable.sejuani, R.drawable.akali, R.drawable.corki },
                    new int[]{ 1080, 1400, 1230 },
                    3,
                    4
            );

            try {
                borderDecoder = BitmapRegionDecoder.newInstance(getResources().openRawResource(R.drawable.border), false);
                border = borderDecoder.decodeRegion(new Rect(0, 0, 12, 16), null);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        public void onCreate(SurfaceHolder surfaceHolder) {
            super.onCreate(surfaceHolder);
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            this.visible = visible;
            // if screen wallpaper is visible then draw the image otherwise do not draw
            if (visible) {
                for ( int i = 0; i < display_columns.length; i++ )
                    display_columns[i].decode();

                handler.post(drawRunner);
            } else {
                handler.removeCallbacks(drawRunner);
            }
        }

        @Override
        public void onSurfaceDestroyed(SurfaceHolder holder) {
            super.onSurfaceDestroyed(holder);
            this.visible = false;
            handler.removeCallbacks(drawRunner);
        }

        public void onOffsetsChanged(float xOffset, float yOffset, float xStep, float yStep, int xPixels, int yPixels) {
            draw();
        }

        /*
         * Store the position of the touch event so we can use it for drawing later
         */
        @Override
        public void onTouchEvent(MotionEvent event) {
            mGestureDetector.onTouchEvent(event);
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int s_width, int s_height) {
            Log.d("lolwall", "Surface Changed");
            width = s_width;
            height = s_height;

            for ( int i = 0; i < display_columns.length; i++ ) {
                display_columns[i].setDimensions( width, height );
                display_columns[i].decode();
            }
            //Log.d("lolwall", "Done setting defaults");
        }

        public void freeze(MotionEvent event, boolean state) {
            float x = event.getX();

            int touchAreas = width / 5;

            if ( x < touchAreas )
                display_columns[0].freeze(state);
            else if ( x < touchAreas * 2 )
                display_columns[1].freeze(state);
            else if ( x < touchAreas * 3 )
                display_columns[2].freeze(state);
            else if ( x < touchAreas * 4 )
                display_columns[3].freeze(state);
            else if ( x < touchAreas * 5 )
                display_columns[4].freeze(state);
        }

        void draw() {

            final SurfaceHolder holder = getSurfaceHolder();

            Canvas c = null;
            try {
                c = holder.lockCanvas();
                if (c != null) {
                    try {

                        display_columns[1].draw(c);
                        display_columns[3].draw(c);
                        display_columns[0].draw(c);
                        display_columns[2].draw(c);
                        display_columns[4].draw(c);

                        if ( border != null ) {
                            c.drawBitmap(border, null, new Rect(0 * width / 5 - 7, 0, 0 * width / 5 + 5, height), null);
                            c.drawBitmap(border, null, new Rect(1 * width / 5 - 6, 0, 1 * width / 5 + 6, height), null);
                            c.drawBitmap(border, null, new Rect(2 * width / 5 - 6, 0, 2 * width / 5 + 6, height), null);
                            c.drawBitmap(border, null, new Rect(3 * width / 5 - 6, 0, 3 * width / 5 + 6, height), null);
                            c.drawBitmap(border, null, new Rect(4 * width / 5 - 6, 0, 4 * width / 5 + 6, height), null);
                            c.drawBitmap(border, null, new Rect(5 * width / 5 - 5, 0, 5 * width / 5 + 7, height), null);
                        }

//                        if ( !display_columns[0].isAnimating() &&
//                             !display_columns[1].isAnimating() &&
//                             !display_columns[2].isAnimating() &&
//                             !display_columns[3].isAnimating() &&
//                             !display_columns[4].isAnimating() ) {
//
//                            if ( time_to_animate > 500 ) {
////                                display_columns[0].animate(80);
////                                display_columns[1].animate(0);
////                                display_columns[2].animate(160);
////                                display_columns[3].animate(40);
////                                display_columns[4].animate(120);
//                                display_columns[0].animate(0);
//                                display_columns[1].animate(0);
//                                display_columns[2].animate(0);
//                                display_columns[3].animate(0);
//                                display_columns[4].animate(0);
//                                time_to_animate = 0;
//                            } else {
//                                time_to_animate += 20;
//                            }
//
//                        }
                        //Log.d("lolwall", Integer.toString(x));
                    } catch (Exception e) {
                        // Do Nothing
                    }

                    long time_cur = System.currentTimeMillis() - time;
                    time = System.currentTimeMillis();
//                    Log.d("lolstream", Long.toString(time_cur));
                }
            } finally {
                if (c != null)
                    holder.unlockCanvasAndPost(c);
            }

            handler.removeCallbacks(drawRunner);

            if (visible) {
                handler.post(drawRunner); // delay 10 mileseconds
            }

        }
    }
}