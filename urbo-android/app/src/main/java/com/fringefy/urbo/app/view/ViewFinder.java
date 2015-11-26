package com.fringefy.urbo.app.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;


public class ViewFinder extends View {

    //Constants
    public static final int iCOLOR = 0x4cffffff;
    private static final int iAIM_THICKNESS = 5;
    private static final int iAIM_SIZE = 40;

    //members
    private Paint aimPaint;
    private Path aimPath;

    //Construction
    public ViewFinder(Context context, AttributeSet attrs) {
        super(context, attrs);

        // TODO: move this to XML (as values..)
        aimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        aimPaint.setColor(iCOLOR);
        aimPaint.setStrokeWidth(iAIM_THICKNESS);
        aimPaint.setStyle(Paint.Style.STROKE);

        aimPath = new Path();
    }

    //Private methods
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

        super.onSizeChanged(w, h, oldw, oldh);
        aimPath = new Path();

        aimPath.moveTo(w/2- iAIM_SIZE, h/2);
        aimPath.lineTo(w/2 + iAIM_SIZE, h/2);

        aimPath.moveTo(w/2, h/2- iAIM_SIZE);
        aimPath.lineTo(w/2, h/2+ iAIM_SIZE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        canvas.drawPath(aimPath, aimPaint);
    }
}