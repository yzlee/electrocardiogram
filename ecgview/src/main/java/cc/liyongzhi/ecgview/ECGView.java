package cc.liyongzhi.ecgview;

import android.content.Context;
import android.content.pm.ProviderInfo;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

/**
 * Created by lee on 7/7/16.
 */
public class ECGView extends View {

    public static final int MODE_DRAW_PER_POINT = 0;
    public static final int MODE_DRAW_FROM_QUEUE = 1;

    //Configuration by user
    private int subViewNum = 6; //Default sub-view number in one page.
    private boolean isZoomAllowed = true; // if allow changing sub-view number by two finger zooming.
    private int maxSubViewNum = 12;
    private int minSubViewNum = 1;
    private int drawPointSpeed = 250; //point per second, it determined the speed of drawing
    private int fps = 25; // frames per second, it determined if our eyes feeling comfortable.
    private int pointPerView = 500; //(the index is begin form 0) usefulPoint = (pointPerView - 1) / (pixelPerView - 1) * currentPixel
    private int YScale = 1;
    private boolean adaptiveDrawSpeed = true; // if cached point is less than threshold, then slow the drawing speed, vice versa.
    private int dataInputMode = 1;
    private int defaultLead = 1;
    private int columnSubViewNum = 2;
    private double aspectRatio = 4/3; //width / height
    private int inputChannelNum = 1; //


    //Used by view;
    private int mainViewWidth = 0;
    private int mainViewHeight = 0;
    private boolean isSubViewNumChanged = false;
    private boolean isSubViewGenerated = false; // Once the subView is Generated, this param is true. And it will never be false.
    private boolean isColumnSubViewNumSet = false;
    private boolean isAspectRatioSet = false;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;



    public ECGView(Context context) {
        super(context);
        init(context);
    }

    public ECGView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ECGView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();

//                LogShower.custom("liyongzhi", "ECGView", "onScale", "detector.getScaleFactor() = " + detector.getScaleFactor());

                if (scaleFactor > 1.2) {
                    if (isColumnSubViewNumSet && subViewNum + columnSubViewNum <= maxSubViewNum) {
                        subViewNum += columnSubViewNum;
                        isSubViewNumChanged = true;
                        invalidate();
                        scaleFactor = 1.0f;
                    }

                    if (!isColumnSubViewNumSet && subViewNum < maxSubViewNum) {
                        subViewNum++;
                        isSubViewNumChanged = true;
                        invalidate();
                        scaleFactor = 1.0f;
                    }
                }

                if (scaleFactor < 0.85) {
                    if (isColumnSubViewNumSet && subViewNum - columnSubViewNum >= minSubViewNum) {
                        subViewNum -= columnSubViewNum;
                        isSubViewNumChanged = true;
                        invalidate();
                        scaleFactor = 1.0f;
                    }

                    if (!isColumnSubViewNumSet && subViewNum > minSubViewNum) {
                        subViewNum--;
                        isSubViewNumChanged = true;
                        invalidate();
                        scaleFactor = 1.0f;
                    }
                }


                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector detector) {
                scaleFactor = 1.0f;
            }
        });

    }

    private void createSubView(int width, int height, int num) {
//        Log.d("ECGView", "[createSubView] subViewNum = " + num);


        if (num == 0) {

            //nothing has been set

            //aspectRatio has been set and others is default

            //columnSubViewNum has been set and others is default

            //Both columnSubViewNum and aspectRatio is set and others is default

            if (isColumnSubViewNumSet && isAspectRatioSet) {
                int rowSubViewNum = (int)(height / ((width / columnSubViewNum) / aspectRatio));
                columnSubViewNum = rowSubViewNum * columnSubViewNum > inputChannelNum ? inputChannelNum : rowSubViewNum * columnSubViewNum;
            } else if (isColumnSubViewNumSet) {
                // if 1 < tmpAspectRatio < 2 then it is acceptable

            } else if (isAspectRatioSet) {

            } else {

            }

        }

        isSubViewNumChanged = false;
        isSubViewGenerated = true;
    }


    private void changeSubViewLayout(int num) {


    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Generate sub-view in the first time.
        if (!isSubViewGenerated && !isSubViewNumChanged) {
            createSubView(mainViewWidth, mainViewHeight, 0);
        }

        // if sub-view number changed, recreate sub-view anyway.
        if (isSubViewNumChanged) {
            createSubView(mainViewWidth, mainViewHeight, subViewNum);
        }



    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
//        LogShower.custom("liyongzhi", "ECGView", "onTouchEvent", "get into onTouchEvent");
        return true;
    }

    @Override
    protected void onSizeChanged(int xNew, int yNew, int xOld, int yOld){
        super.onSizeChanged(xNew, yNew, xOld, yOld);

        mainViewWidth = xNew;
        mainViewHeight = yNew;
    }

    public void setSubViewNum(int subViewNum) {
        this.subViewNum = subViewNum;
        this.isSubViewNumChanged = true;
    }

    public void setZoomAllowed(boolean zoomAllowed) {
        isZoomAllowed = zoomAllowed;
    }

    public void setMaxSubViewNum(int maxSubViewNum) {
        this.maxSubViewNum = maxSubViewNum;
    }

    public void setMinSubViewNum(int minSubViewNum) {
        this.minSubViewNum = minSubViewNum;
    }

    public void setDrawPointSpeed(int drawPointSpeed) {
        this.drawPointSpeed = drawPointSpeed;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public void setPointPerView(int pointPerView) {
        this.pointPerView = pointPerView;
    }

    public void setYScale(int YScale) {
        this.YScale = YScale;
    }

    public void setAdaptiveDrawSpeed(boolean adaptiveDrawSpeed) {
        this.adaptiveDrawSpeed = adaptiveDrawSpeed;
    }

    public void setDataInputMode(int dataInputMode) {
        this.dataInputMode = dataInputMode;
    }

    public void setColumnSubViewNum(int columnSubViewNum) {
        this.columnSubViewNum = columnSubViewNum;
        this.isColumnSubViewNumSet = true;
    }

    public void setDefaultLead(int defaultLead) {
        this.defaultLead = defaultLead;
    }

    public void setAspectRatio(double aspectRatio) {
        this.aspectRatio = aspectRatio;
        this.isAspectRatioSet = true;
    }

    public void setInputChannelNum(int inputChannelNum) {
        this.inputChannelNum = inputChannelNum;
    }
}
