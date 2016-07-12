package cc.liyongzhi.ecgview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.util.Queue;

/**
 * Created by lee on 7/8/16.
 */
public class ECGSubView {

    private int offsetStartPointX = 0;
    private int offsetStartPointY = 0;
    private float subHeight = 0;
    private float subWidth = 0;
    private int parentWidth = 0;
    private int parentHeight = 0;
    private Queue dataChannel;




    public ECGSubView(Queue dataChannel) {
        this.dataChannel = dataChannel;
    }

    public void draw(Canvas canvas) {
        //draw border
        Paint borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(offsetStartPointX, offsetStartPointY, offsetStartPointX + subWidth, offsetStartPointY + subHeight, borderPaint);
        //draw wave
        Paint wavePaint = new Paint();

    }

    public void setOffsetStartPoint(int offsetStartPointX, int offsetStartPointY) {
        this.offsetStartPointX = offsetStartPointX;
        this.offsetStartPointY = offsetStartPointY;
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                }

    public void setSubHeight(float subHeight) {
        this.subHeight = subHeight;
    }

    public void setSubWidth(float subWidth) {
        this.subWidth = subWidth;
    }

    public void setParentWidth(int parentWidth) {
        this.parentWidth = parentWidth;
    }

    public void setParentHeight(int parentHeight) {
        this.parentHeight = parentHeight;
    }
}
