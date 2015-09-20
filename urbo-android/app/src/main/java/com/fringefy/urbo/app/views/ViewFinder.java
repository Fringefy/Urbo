package com.fringefy.urbo.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Orr's laptop on 08/09/2015.
 */
public class ViewFinder extends View {


    private Paint aimPaint;
    private Path aimPath;

    private static final int AIM_THICKNESS = 5;
    private static final int AIM_SIZE = 40;

    public ViewFinder(Context context, AttributeSet attrs) {
        super(context, attrs);

        aimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        aimPaint.setColor(0x4cffffff);
        aimPaint.setStrokeWidth(AIM_THICKNESS);
        aimPaint.setStyle(Paint.Style.STROKE);

        aimPath = new Path();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        super.onSizeChanged(w, h, oldw, oldh);
        aimPath = new Path();

        aimPath.moveTo(w/2- AIM_SIZE, h/2);
        aimPath.lineTo(w/2 + AIM_SIZE, h/2);

        aimPath.moveTo(w/2, h/2- AIM_SIZE);
        aimPath.lineTo(w/2, h/2+ AIM_SIZE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawPath(aimPath, aimPaint);
    }
}