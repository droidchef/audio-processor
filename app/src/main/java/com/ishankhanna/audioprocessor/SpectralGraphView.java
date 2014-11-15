package com.ishankhanna.audioprocessor;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/**
 * Created by ishankhanna on 15/11/14.
 */
public class SpectralGraphView extends View {

    float[] heightOfSpectralLines;
    float baseLineYCoordinate;
    float screenWidth;
    float increment; // This decides the gap between two consecutive spectral lines
    Paint paint;

    static final int CLR_BACKGROUND = Color.CYAN;
    static final int CLR_X_AXIS = Color.BLACK;
    static final int CLR_SPEC_LINE_ODD = Color.RED;
    static final int CLR_SPEC_LINE_EVEN = Color.YELLOW;
    static final float STROKE_WIDTH = 1.1f;

    public SpectralGraphView(Context context, float[] _heighOfSpectralLines) {
        super(context);
        heightOfSpectralLines = _heighOfSpectralLines;
        paint = new Paint();

    }

    @Override
    protected void onDraw(Canvas canvas) {

        // Set the Background
        paint.setColor(CLR_BACKGROUND);
        canvas.drawRGB(Color.red(CLR_BACKGROUND), Color.green(CLR_BACKGROUND), Color.blue(CLR_BACKGROUND));

        // Calculate Axis Postion
        baseLineYCoordinate = getHeight()/2;

        // Draw the X Axis
        paint.setColor(CLR_X_AXIS);
        canvas.drawLine(0, baseLineYCoordinate, getWidth(), baseLineYCoordinate, paint);



    }
}
