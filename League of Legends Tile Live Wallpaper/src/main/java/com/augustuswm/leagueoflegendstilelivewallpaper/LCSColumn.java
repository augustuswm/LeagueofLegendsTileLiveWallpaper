package com.augustuswm.leagueoflegendstilelivewallpaper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.EmbossMaskFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;

class LCSColumn {

    private static final int SOURCE_IMAGE_WIDTH = 1920;
    private static final int SOURCE_IMAGE_HEIGHT = 1080;
    private static final int ZOOM_OFF = 0;
    private static final int ZOOM_IN = 1;
    private static final int ZOOM_OUT = 2;

    private int[] offsets;
    private int static_champion_index, current_champion_index, width, height, maxColumnWidth, adjustedWidth;
    private BitmapRegionDecoder[] decoderList;
    private Bitmap[] bitmapList;
    private Bitmap selectedChampion = null;
    private boolean is_animating = true, is_frozen = false;

    // 0 : Left -> Right, 1 : Top -> Bottom, 2 : Right to Left, 3 : Bottom to Top
    private int direction = 0;
    private int position_offset = 0, zoom_state = 0, column_index = 0;
    private double zoom_coefficient = 0.9;

    private Rect coords, sourceArea, animcoords, zoomSourceArea;

    private final Handler handler = new Handler(), calculateHandler = new Handler();
    private final Runnable animateDelay = new Runnable() {
        @Override
        public void run() {
            animate(0);
        }
    };
    private final Runnable calculateRunner = new Runnable() {
        @Override
        public void run() {
            calculate();
        }
    };

    LCSColumn(Context ctx, int[] champion_resources, int[] champion_offsets, int column_direction, int col ) {

        this.coords = coords;

        offsets = champion_offsets;

        decoderList = new BitmapRegionDecoder[champion_resources.length];
        bitmapList = new Bitmap[champion_resources.length];
        direction = column_direction;

        for ( int i = 0; i < champion_resources.length; i++ ) {
            try {
                decoderList[i] = BitmapRegionDecoder.newInstance(ctx.getResources().openRawResource(champion_resources[i]), false);
//                Log.d("lolwall", "Built decoder #" + Integer.toString(i));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        current_champion_index = 0;
        static_champion_index = 0;

        column_index = col;

    }

    public boolean freeze( boolean state ) {
        is_frozen = state;

        if ( is_frozen ) {
            selectedChampion = decoderList[current_champion_index].decodeRegion(new Rect(offsets[current_champion_index] - maxColumnWidth/2, 0, offsets[current_champion_index] + maxColumnWidth/2, SOURCE_IMAGE_HEIGHT), null);
//            animate_zoom = true;
            zoom_state = ZOOM_IN;
        } else {
            selectedChampion = null;
//            animate_zoom = false;
            zoom_state = ZOOM_OFF;
            zoom_coefficient = 0.9;
        }

        return state;
    }

    public void setDimensions( int c_width, int c_height ) {
        width = c_width;
        height = c_height;

        double maxEdge = (double)Math.max( width, height);
        double minEdge = (double)Math.min( width, height);

        maxColumnWidth = (int)Math.ceil( maxEdge / 5 * ( SOURCE_IMAGE_HEIGHT / minEdge ) );

        coords = new Rect(column_index * width / 5, 0, (column_index + 1) * width / 5, height);
//        Log.d("lolstream", coords.left + " " + coords.top + " " + coords.right + " " + coords.bottom);

        int left, top = 0, right;
        float scalingCoefficient = (float)SOURCE_IMAGE_HEIGHT / (float)height ;
        adjustedWidth = (int)Math.ceil((float)coords.width() * scalingCoefficient);

        left = (maxColumnWidth - adjustedWidth) / 2;
        right = (maxColumnWidth + adjustedWidth) / 2;

        // Modify draw to select the proper region of the decoded region
        sourceArea = new Rect( left, top, right, SOURCE_IMAGE_HEIGHT );

        calculateHandler.postDelayed(calculateRunner, 10); // delay 10 mileseconds

//        Log.d("lolwall", Integer.toString(width) + ", " + Integer.toString(height) + ", " + Integer.toString(maxColumnWidth));
    }

    public void decode() {
        decode( false );
    }

    public void decode( boolean invalidate ) {

        if ( direction % 2 == 0 )
            position_offset = -1 * width / 5;
        else
            position_offset = -1 * height;

        for ( int i = 0; i < decoderList.length; i++ ) {
            if ( invalidate || bitmapList[i] == null || bitmapList[i].isRecycled() )
                //bitmapList[i] = grayscaleBitmap(decoderList[i].decodeRegion(new Rect(offsets[i] - maxColumnWidth/2, 0, offsets[i] + maxColumnWidth/2, SOURCE_IMAGE_HEIGHT), null));
                bitmapList[i] = BitmapUtil.changeBitmapContrastBrightness(
                        BitmapUtil.grayscaleBitmap(
                                decoderList[i].decodeRegion(
                                        new Rect(offsets[i] - maxColumnWidth / 2, 0, offsets[i] + maxColumnWidth / 2, SOURCE_IMAGE_HEIGHT), null)
                        ), 1.5f, -40);
        }

    }

    public boolean isAnimating() {
        return is_animating;
    }

    public void animate() {
        animate(0);
    }

    public void animate( int offset ) {

        if ( offset == 0 && !is_frozen ) {
            handler.removeCallbacks(animateDelay);

            current_champion_index++;

            if ( current_champion_index == bitmapList.length )
                current_champion_index = 0;

            is_animating = true;
        } else {
            handler.postDelayed( animateDelay, offset );
        }

    }

    public void calculate() {
        animcoords = coords;
        zoomSourceArea = sourceArea;

        if ( zoom_coefficient >= 0.93 ) {
//            animate_zoom = false;
            zoom_state = ZOOM_OFF;
        } else if ( zoom_coefficient < 0.9 && zoom_state == ZOOM_IN ) {
            zoom_state = ZOOM_OUT;
        }

        //if ( zoom_state == ZOOM_IN || zoom_state == ZOOM_OUT ) {
        int zoomAdjustedHeight = (int)((float)SOURCE_IMAGE_HEIGHT * zoom_coefficient);
        int zoomAdjustedWidth = (int)((float)adjustedWidth * zoom_coefficient);

        int zoomTop = (SOURCE_IMAGE_HEIGHT - zoomAdjustedHeight) / 2;
        int zoomBottom = (SOURCE_IMAGE_HEIGHT + zoomAdjustedHeight) / 2;
        int zoomLeft = (maxColumnWidth - zoomAdjustedWidth) / 2;
        int zoomRight = (maxColumnWidth + zoomAdjustedWidth) / 2;

        if ( zoom_state == ZOOM_IN ) {
            zoom_coefficient = zoom_coefficient - 0.05;
        } else if ( zoom_state == ZOOM_OUT ) {
            zoom_coefficient = zoom_coefficient + 0.0125;
        }

        zoomSourceArea = new Rect( zoomLeft, zoomTop, zoomRight, zoomBottom );
        //}

//        if ( is_animating ) {
//
//            if ( direction % 2 == 0 ) {
//                if ( position_offset + ( width / 5 ) / 6 < 0 ) {
//                    position_offset = position_offset + ( width / 5 ) / 6;
//
//                    if ( direction == 0 )
//                        animcoords.set( coords.left + position_offset, coords.top, coords.right + position_offset, coords.bottom );
//                    else if ( direction == 2 )
//                        animcoords.set( coords.left - position_offset, coords.top, coords.right - position_offset, coords.bottom );
//
//                } else {
//                    position_offset = -1 * width / 5;
//                    is_animating = false;
//                    static_champion_index = current_champion_index;
//                }
//            } else if ( direction % 2 == 1 ) {
//                if ( position_offset + height / 10 < 0 ) {
//                    position_offset = (int)(position_offset + height / 10);
//
//                    if ( direction == 1 )
//                        animcoords.set( coords.left, coords.top + position_offset, coords.right, coords.bottom + position_offset );
//                    else if ( direction == 3 )
//                        animcoords.set( coords.left, coords.top - position_offset, coords.right, coords.bottom - position_offset );
//
//                } else {
//                    position_offset = -1 * height;
//                    is_animating = false;
//                    static_champion_index = current_champion_index;
//                }
//            }
//        }

        calculateHandler.removeCallbacks(calculateRunner);
        calculateHandler.postDelayed(calculateRunner, 10); // delay 10 mileseconds
//        Log.d("lolstream", "Run Calculate");
    }

    public void draw( Canvas c ) {
//        Log.d("lolstream", sourceArea.toString() + " " + coords.toString());

        // Render the base first
        if ( is_frozen )
            c.drawBitmap(selectedChampion, zoomSourceArea, coords, null);
        else
            c.drawBitmap(bitmapList[static_champion_index], sourceArea, coords, null);

        //Render the adjusted bitmap if we are not frozen
        if ( !is_frozen )
            c.drawBitmap(bitmapList[current_champion_index], sourceArea, animcoords, null);

    }

}