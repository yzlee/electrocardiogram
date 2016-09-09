package cc.liyongzhi.ecgview;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

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
    private short[] data;
    private int nextStartPoint = 0; // end point is the last point's next point;
    private float scaling = 0.5f;
    private int emptyDataAmount = 5;
    private boolean drawBackground;
    private float pixelPerMillimeter;
    private float gridInterval;
    private short[] dataForDraw;
    private int drawPaperSpeed = 25; // mm/s
    private int drawPointSpeed = 250; // point/s
    private float pointPerPixel;
    private final float POINT_PER_PIXEL;
    private Paint borderPaint = new Paint();
    private Paint backgroundPaint = new Paint();
    private Paint wavePaint = new Paint();
    private float step = 1;
    private int strokeWidth = 1;
    private boolean thumbnailMode = true;
    private float hScale = 2.0f; //horizontal scaling
    private float everyNPointDraw = 1;

    private static final String TAG = "ECGSubView";

    public ECGSubView(int drawPaperSpeed, int drawPointSpeed, float pixelPerMillimeter) {
        this.drawPointSpeed = drawPointSpeed;
        this.drawPaperSpeed = drawPaperSpeed;
        this.pixelPerMillimeter = pixelPerMillimeter;


        int pointPerMillimeter = drawPointSpeed / drawPaperSpeed;
        pointPerPixel = pointPerMillimeter / pixelPerMillimeter;
        POINT_PER_PIXEL = pointPerPixel;

        //border painter
        borderPaint.setAntiAlias(true);
        borderPaint.setColor(Color.BLACK);
        borderPaint.setStrokeWidth(1);
        borderPaint.setStyle(Paint.Style.STROKE);
        //background painter
        backgroundPaint.setColor(Color.rgb(255, 182, 189));
        //wave painter
        wavePaint.setStrokeWidth(strokeWidth);
        wavePaint.setColor(Color.rgb(0, 128, 0));
    }

    public void draw(Canvas canvas) {

        //draw background
        if (drawBackground) {
            float pixelPerGridInterval = pixelPerMillimeter * gridInterval;
            int i = 1;
            while (pixelPerGridInterval * i < subHeight ) {
                canvas.drawLine(0, pixelPerGridInterval * i, subWidth, pixelPerGridInterval * i, backgroundPaint);
                i++;
            }
            int j = 1;
            while (pixelPerGridInterval * j < subWidth) {
                canvas.drawLine(pixelPerGridInterval * j, 0, pixelPerGridInterval * j, subHeight, backgroundPaint);
                j++;
            }
        }

        //draw border
        canvas.drawRect(offsetStartPointX, offsetStartPointY, offsetStartPointX + subWidth, offsetStartPointY + subHeight, borderPaint);


        //draw wave
        if (dataForDraw != null) {
            //for performance
            if (thumbnailMode) {
                everyNPointDraw = subWidth / 240 > 1 ? subWidth / 240 : 1;
            } else {
                everyNPointDraw = subWidth / 360 > 1 ? subWidth / 360 : 1;
            }

            int offsetY = subHeight / 2;
            float maxValue = 0;
            float maxValueReal = 0;
            for (int i = 0; i < dataForDraw.length - pointPerPixel * everyNPointDraw; i = i + (int)(pointPerPixel * everyNPointDraw)) {

                int emptyLength = i - nextStartPoint;

                if (emptyLength < 6 * pointPerPixel * everyNPointDraw  && emptyLength > 0) {
                    continue;
                }

                //thumbnail adjust screen
                maxValueReal = maxValueReal > Math.abs(dataForDraw[i]) ? maxValue : Math.abs(dataForDraw[i]);
                float heightFirst = dataForDraw[i] * scaling;
                float heightNext = dataForDraw[i + (int)(pointPerPixel * everyNPointDraw)] * scaling;
                maxValue = maxValue > Math.abs(heightNext) ? maxValue : Math.abs(heightNext);

                if (thumbnailMode && Math.abs(heightNext) > subHeight / 2) {
                    scaling *= 0.9;
                    heightNext *= scaling;
                }
                if (Math.abs(heightNext) > subHeight / 2) {
                    heightNext = heightNext > 0 ? subHeight / 2 : -subHeight / 2;
                }

                canvas.drawLine(step * i + offsetStartPointX, - heightFirst + offsetStartPointY + offsetY, step * (i + pointPerPixel * everyNPointDraw) + offsetStartPointX, - heightNext + offsetStartPointY + offsetY, wavePaint);
            }

            if (maxValue < subHeight / 8 && maxValueReal > subHeight / 10 && thumbnailMode) {
                scaling *= 1.05;
            }

            if (thumbnailMode && maxValue < subHeight / 12 && maxValueReal > subHeight / 12) {
                scaling = 0.2f;
            }

        }

/*        if (data != null) {
            Paint wavePaint = new Paint();
            wavePaint.setColor(Color.rgb(0, 128, 0));
            int dataLength = data.length;
            float step = (float) subWidth / dataLength;
            int offsetY = subHeight / 2;
            for (int i = 1; i < dataLength; i++) {
                if (i < nextStartPoint || i > nextStartPoint + emptyDataAmount) {
                    canvas.drawLine(step * (i - 1) + offsetStartPointX, data[i - 1] * scaling + offsetY + offsetStartPointY, step * i + offsetStartPointX, data[i] * scaling + offsetY + + offsetStartPointY, wavePaint);
                }
            }
        }*/

        //draw number
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        //text size
        float size = 12;
        //text position y
        float positionY = offsetStartPointY + subHeight / 5;
        size = subWidth / 18;
        if (size < 8) {
            size = 8;
            positionY = offsetStartPointY + subHeight / 2;
        } else if(size < 16) {
            positionY = offsetStartPointY + subHeight / 3;
        }
        else if (size > 60) {
            size = 60;
        }
        textPaint.setTextSize(size);

        canvas.drawText(text, offsetStartPointX + subWidth / 8, positionY, textPaint);
    }

    /**
     * add child array to parent array.
     * @param child child array
     * @param parent parent array
     * @param startPoint add from startPoint
     * @return  which is the new parent and next start Point, which can be as startPoint next time.
     */
    private int addArrayToArray(short[] parent, short[] child, int startPoint) {
        int childLength = child.length;
        int parentLength = parent.length;

        int num = (childLength + startPoint) / parentLength;
        int left = (childLength + startPoint) % parentLength;

        for (int i = 0; i < parentLength; i++) {
            if (i < left && childLength - left + i >= 0) {
                parent[i] = child[childLength - left + i];
            }
            if (i < parentLength - left && childLength - parentLength + i >= 0){
                parent[i + left] = child[childLength - parentLength + i];
            }
        }
        return left;
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

    public void setThumbnailMode(boolean thumbnailMode) {
        if (thumbnailMode) {
            pointPerPixel = POINT_PER_PIXEL * hScale;
        } else {
            pointPerPixel = POINT_PER_PIXEL;
        }
        this.thumbnailMode = thumbnailMode;
        setSubWidth(subWidth);
    }

    public void setSubWidth(int subWidth) {

        this.subWidth = subWidth; //pixel number of one page
        int totalPoint = (int)(subWidth * pointPerPixel);
        short[] newDataForDraw = new short[totalPoint];
        if (dataForDraw != null) {
            addArrayToArray(newDataForDraw, dataForDraw, 0);
        }
        dataForDraw = newDataForDraw;
        step = (float) subWidth / totalPoint;
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

/*    public void setData(short[] data, int nextStartPoint) {
        this.data = data;
        this.nextStartPoint = nextStartPoint;
    }*/

    public void addData(short[] data) {
        if (dataForDraw != null) {
            nextStartPoint = addArrayToArray(dataForDraw, data, nextStartPoint);
        }

    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
        wavePaint.setStrokeWidth(strokeWidth);
    }

    public short[] getData() {
        return data;
    }

    public void setScaling(float scaling) {
        this.scaling = scaling;
    }

    public int getNextStartPoint() {
        return nextStartPoint;
    }

    public void setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
    }


    public void setGridInterval(float gridInterval) {
        this.gridInterval = gridInterval;
    }

}
