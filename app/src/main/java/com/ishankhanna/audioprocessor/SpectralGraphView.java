package com.ishankhanna.audioprocessor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by ishankhanna on 15/11/14.
 */
public class SpectralGraphView extends View {

    static final String TAG = "SpectralGrpahView";

    float[] heightOfSpectralLines;
    float baseLineYCoordinate;
    float screenWidth;
    double increment; // This decides the gap between two consecutive spectral lines
    Paint paint;

    static final int CLR_BACKGROUND = Color.CYAN;
    static final int CLR_X_AXIS = Color.BLACK;
    static final int CLR_SPEC_LINE_ODD = Color.RED;
    static final int CLR_SPEC_LINE_EVEN = Color.YELLOW;
    static final float STROKE_WIDTH = 1.1f;

    public SpectralGraphView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
    }

    public float[] getHeightOfSpectralLines() {
        return heightOfSpectralLines;
    }

    public void setHeightOfSpectralLines(float[] heightOfSpectralLines) {
        this.heightOfSpectralLines = heightOfSpectralLines;
        invalidate();
        requestLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // Set the Background
        paint.setColor(CLR_BACKGROUND);
        canvas.drawRGB(Color.red(CLR_BACKGROUND), Color.green(CLR_BACKGROUND), Color.blue(CLR_BACKGROUND));

        // Calculate Axis Postion
        baseLineYCoordinate = getHeight()/2;

        //calculate increments
        increment = (double)getWidth()/(double)heightOfSpectralLines.length;
        Log.v(TAG, "Increments = "+increment);
        float x = 0;
        // Plot lines
        paint.setColor(CLR_SPEC_LINE_ODD);
        for(int i=0;i<heightOfSpectralLines.length;i++){
            //Log.v(TAG, "Height of Line "+(i+1)+" is "+heightOfSpectralLines[i]);
            double pointAbove = (double)baseLineYCoordinate - ((double)heightOfSpectralLines[i]/(double)2);
            double pointBelow = (double)baseLineYCoordinate + ((double)heightOfSpectralLines[i]/(double)2);
            Log.v(TAG, "A = "+pointAbove+", B = "+pointBelow);
            canvas.drawLine(x, (float)pointAbove, x, (float)pointBelow, paint);
            x+=increment;
        }
        Log.v(TAG, "No of Lines = "+heightOfSpectralLines.length);
        // Draw the X Axis
        paint.setColor(CLR_X_AXIS);
        canvas.drawLine(0, baseLineYCoordinate, getWidth(), baseLineYCoordinate, paint);



    }
}
