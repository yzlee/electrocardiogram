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
    private int subHeight = 0;
    private int subWidth = 0;
    private int parentWidth = 0;
    private int parentHeight = 0;
    private String text = "";
    private Queue dataChannel;
    private int[] data;
    private int endPoint = 0; // end point is the last point's next point;
    private float scaling = 1.0f;
    private int emptyDataAmount = 5;





    public ECGSubView(Queue dataChannel) {
        this.dataChannel = dataChannel;
    }

    public void draw(Canvas canvas) {
        //draw border
        Paint borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStrokeWidth(1);

        borderPaint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(offsetStartPointX, offsetStartPointY, offsetStartPointX + subWidth, offsetStartPointY + subHeight, borderPaint);
        //draw wave
        if (data != null) {
            Paint wavePaint = new Paint();
            wavePaint.setColor(Color.RED);
            int dataLength = data.length;
            float step = (float) subWidth / dataLength;
            int offsetY = subHeight / 2;
            for (int i = 1; i < dataLength; i++) {
                if (i < endPoint || i > endPoint + emptyDataAmount) {
                    canvas.drawLine(step * (i - 1) + offsetStartPointX, data[i - 1] * scaling + offsetY + offsetStartPointY, step * i + offsetStartPointX, data[i] * scaling + offsetY + + offsetStartPointY, wavePaint);
                }
            }
        }

        //draw number
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        canvas.drawText(text, offsetStartPointX + subWidth / 8, offsetStartPointY + subHeight / 5, textPaint);

    }

    public boolean isBelongToMe(int x, int y) {
        return x < offsetStartPointX + subWidth && x >= offsetStartPointX && y < offsetStartPointY + subHeight && y >= offsetStartPointY;
    }


    public void setOffsetStartPoint(int offsetStartPointX, int offsetStartPointY) {
        this.offsetStartPointX = offsetStartPointX;
        this.offsetStartPointY = offsetStartPointY;
                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                }

    public void setSubHeight(int subHeight) {
        this.subHeight = subHeight;
    }

    public void setSubWidth(int subWidth) {
        this.subWidth = subWidth;
    }

    public void setParentWidth(int parentWidth) {
        this.parentWidth = parentWidth;
    }

    public void setParentHeight(int parentHeight) {
        this.parentHeight = parentHeight;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getOffsetStartPointX() {
        return offsetStartPointX;
    }

    public int getSubWidth() {
        return subWidth;
    }

    public int getOffsetStartPointY() {
        return offsetStartPointY;
    }

    public void setData(int[] data, int endPoint) {
        this.data = data;
        this.endPoint = endPoint;
    }

    public int[] getData() {
        return data;
    }

    public int getEndPoint() {
        return endPoint;
    }
}
