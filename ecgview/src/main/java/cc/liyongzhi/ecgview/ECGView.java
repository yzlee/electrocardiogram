package cc.liyongzhi.ecgview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.Queue;

import cc.liyongzhi.androidlogsaver.log.LogShower;

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
    private ArrayList<Queue> channel = new ArrayList<>();


    //Used by view;
    private int mainViewWidth = 0;
    private int mainViewHeight = 0;
    private boolean isSubViewNumInited = false;
    private boolean isInitDefParam = false; // Once the subView is Generated, this param is true. And it will never be false.
    private boolean isColumnSubViewNumSet = false;
    private boolean isAspectRatioSet = false;
    private ScaleGestureDetector scaleGestureDetector;
    private float scaleFactor = 1.0f;
    private ArrayList<ECGSubView> subViewList = new ArrayList<>();
    /*private ArrayList<ECGSubView> currentPageSubViewList = new ArrayList<>();*/
    private int currentPage = 0; // index is from 0; currentPage * subViewNum = theFirstSubViewIndexFromSubViewListInCurrentPage
    private int currentPageStartIndex = 0;
    private int currentPageLeftSubViewNumber = 0;
    private int inputChannelNum = 1; //
    private DisplayMetrics displayMetrics;
    private float pixelPerMillimeter;


    Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            invalidate();
            postDelayed(refreshRunnable, 1000/fps);
        }
    };



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

        //get pixelPerMillimeter
        displayMetrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        pixelPerMillimeter = (float)(displayMetrics.xdpi / 2.54 /10);
        LogShower.custom("liyongzhi", "ECGView", "init", "pixelPerMillimeter = " + pixelPerMillimeter);

        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
//                LogShower.custom("liyongzhi", "ECGView", "onScale", "detector.getScaleFactor() = " + detector.getScaleFactor());
                if (scaleFactor > 1.2) {
                    LogShower.custom("liyongzhi", "ECGView", "onScale", "scaleFactor > 1.2");
                    if (isColumnSubViewNumSet && subViewNum - columnSubViewNum >= minSubViewNum) {
                        subViewNum -= columnSubViewNum;
                        LogShower.custom("liyongzhi", "ECGView", "onScale", "subViewNum = " + subViewNum);
                        subViewNumChanged();
                        invalidate();
                        scaleFactor = 1.0f;
                    }
                    if (!isColumnSubViewNumSet && subViewNum > minSubViewNum) {
                        subViewNum--;
                        LogShower.custom("liyongzhi", "ECGView", "onScale", "subViewNum = " + subViewNum);
                        subViewNumChanged();
                        invalidate();
                        scaleFactor = 1.0f;
                    }
                }
                if (scaleFactor < 0.85) {
                    if (isColumnSubViewNumSet && subViewNum + columnSubViewNum <= maxSubViewNum) {
                        subViewNum += columnSubViewNum;
                        subViewNumChanged();
                        invalidate();
                        scaleFactor = 1.0f;
                    }
                    if (!isColumnSubViewNumSet && subViewNum < maxSubViewNum) {
                        subViewNum++;
                        subViewNumChanged();
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


    private void subViewNumChanged() {
        refreshCurrentPageSubViewIndex();
        recreateSubView();
    }

    private void currentPageChanged(int offset) {
        int tmpCurrentPage = currentPage + offset;
        if (tmpCurrentPage < 0) {
            currentPage = 0;
        } else if (tmpCurrentPage >= inputChannelNum / subViewNum) {
            currentPage = inputChannelNum / subViewNum - 1;
        } else {
            currentPage = tmpCurrentPage;
        }
        refreshCurrentPageSubViewIndex();
        recreateSubView();
    }


    private void refreshCurrentPageSubViewIndex() {
        currentPageStartIndex = currentPage * subViewNum;
        int leftSubViewNumber = inputChannelNum - currentPageStartIndex;
        currentPageLeftSubViewNumber = leftSubViewNumber <= subViewNum ? leftSubViewNumber : subViewNum;
/*        currentPageSubViewList = new ArrayList<>();
        for (int i = currentPageStartIndex; i < currentPageStartIndex + currentPageLeftSubViewNumber; i++) {
            currentPageSubViewList.add(subViewList.get(i));
        }*/
    }

    private void recreateSubView() {
        changeSubViewLayout();
    }


    private void createSubView() {
        for (int i = 0; i < inputChannelNum; i++) {
            subViewList.add(new ECGSubView(channel.get(i)));
        }
    }

    private void changeSubViewLayout() {
        ECGSubView subview = null;
        for (int i = currentPageStartIndex; i < currentPageLeftSubViewNumber + currentPageStartIndex; i++) {
            subview = subViewList.get(i);
            int tmp = currentPageLeftSubViewNumber % columnSubViewNum;
            float subWidth = 0;
            float subHeight = 0;
            if (i < currentPageLeftSubViewNumber + currentPageStartIndex - tmp) {
                subWidth = (float) mainViewWidth / columnSubViewNum;
            } else {
                subWidth = (float) mainViewWidth / tmp;
            }
            if (isAspectRatioSet) {
                subHeight = (int)(subWidth / aspectRatio);
            } else {
                subHeight = (float) mainViewHeight / ((currentPageLeftSubViewNumber + columnSubViewNum - 1)/ columnSubViewNum);
            }
            int offsetStartPointX = (int) (((i - currentPageStartIndex) % columnSubViewNum) * subWidth);
            int offsetStartPointY = (int) (((i - currentPageStartIndex) / columnSubViewNum) * subHeight);
            subview.setSubWidth((int)subWidth);
            subview.setParentHeight(mainViewHeight);
            subview.setParentWidth(mainViewWidth);
            subview.setOffsetStartPoint(offsetStartPointX, offsetStartPointY);
            //get the precise pixel address of sub-view's height.
            if (i - currentPageStartIndex >= currentPageLeftSubViewNumber - columnSubViewNum) {
                subview.setSubHeight(mainViewHeight - offsetStartPointY);
            }

            if (i - currentPageStartIndex >= columnSubViewNum) {
                ECGSubView preHSubview = subViewList.get(i - columnSubViewNum);
                preHSubview.setSubHeight(offsetStartPointY - preHSubview.getOffsetStartPointY());
            }
/*
            //get the precise pixel address of sub-view's width.
            if ((i - currentPageStartIndex) % columnSubViewNum == columnSubViewNum - 1) {
                subview.setSubWidth(mainViewWidth - offsetStartPointX);
            } else if ((i - currentPageStartIndex) % columnSubViewNum >= 1) {
                ECGSubView preWSubview = subViewList.get((i - currentPageStartIndex) % columnSubViewNum - 1);
                preWSubview.setSubWidth(offsetStartPointX - preWSubview.getOffsetStartPointX());
            }
*/

        }
    }

    private void drawCurrentPage(Canvas canvas) {
        for (int i = currentPageStartIndex; i < currentPageLeftSubViewNumber + currentPageStartIndex; i++) {
            subViewList.get(i).draw(canvas);
        }
    }

    private void autoAdjustDefParam() {
        //nothing has been set
        //aspectRatio has been set and others is default
        //columnSubViewNum has been set and others is default
        //Both columnSubViewNum and aspectRatio is set and others is default
        if (isColumnSubViewNumSet && isAspectRatioSet && isSubViewNumInited) {
            // I really want to do something but ……
        } else if (isColumnSubViewNumSet && isSubViewNumInited) {
            aspectRatio = mainViewWidth / columnSubViewNum / (mainViewHeight / (subViewNum / columnSubViewNum));
        } else if (isAspectRatioSet && isSubViewNumInited) {
            columnSubViewNum = 2;
        } else if (isColumnSubViewNumSet && isAspectRatioSet) {
            int rowSubViewNum = (int)(mainViewHeight / ((mainViewWidth / columnSubViewNum) / aspectRatio));
            subViewNum = rowSubViewNum * columnSubViewNum > inputChannelNum ? inputChannelNum : rowSubViewNum * columnSubViewNum;
        } else if (isColumnSubViewNumSet) {
            // if 1 < tmpAspectRatio < 2 then it is acceptable
            double tmpAspectRatio = mainViewWidth / (mainViewHeight / (inputChannelNum / columnSubViewNum));
            if (tmpAspectRatio <= 2 && tmpAspectRatio >= 1) {
                subViewNum = inputChannelNum;
            } else {
                int rowSubViewNum = (int)(mainViewHeight / ((mainViewWidth / columnSubViewNum) / aspectRatio));
                subViewNum = rowSubViewNum * columnSubViewNum > inputChannelNum ? inputChannelNum : rowSubViewNum * columnSubViewNum;
            }
        } else if (isAspectRatioSet) {
            if (aspectRatio >= 2) {
                columnSubViewNum = 1;
            } else {
                columnSubViewNum = 2;
            }
            int rowSubViewNum = (int)(mainViewHeight / ((mainViewWidth / columnSubViewNum) / aspectRatio));
            subViewNum = rowSubViewNum * columnSubViewNum > inputChannelNum ? inputChannelNum : rowSubViewNum * columnSubViewNum;
        } else if (isSubViewNumInited) {
            columnSubViewNum = 2;
            aspectRatio = 16/9;
        } else {
            columnSubViewNum = 2;
            double tmpAspectRatio = mainViewWidth / (mainViewHeight / (inputChannelNum / columnSubViewNum));
            if (tmpAspectRatio <= 2 && tmpAspectRatio >= 1) {
                subViewNum = inputChannelNum;
            } else if (tmpAspectRatio > 2) {
                aspectRatio = 16/9;
                int rowSubViewNum = (int)(mainViewHeight / ((mainViewWidth / columnSubViewNum) / aspectRatio));
                subViewNum = rowSubViewNum * columnSubViewNum > inputChannelNum ? inputChannelNum : rowSubViewNum * columnSubViewNum;
            } else {
                aspectRatio = 16/9;
                columnSubViewNum = 1;
                int rowSubViewNum = (int)(mainViewHeight / ((mainViewWidth / columnSubViewNum) / aspectRatio));
                subViewNum = rowSubViewNum * columnSubViewNum > inputChannelNum ? inputChannelNum : rowSubViewNum * columnSubViewNum;
            }
        }
        isInitDefParam = true;
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Generate default parameter in the first time.
        if (!isInitDefParam) {
            autoAdjustDefParam();
            subViewNumChanged();
        }

        drawCurrentPage(canvas);

    }


    private void queueToArray() {
        int pointPerFresh = drawPointSpeed / fps;
    }

    public void start() {
        queueToArray();
        post(refreshRunnable);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
//        LogShower.custom("liyongzhi", "ECGView", "onTouchEvent", "get into onTouchEvent");
        return true;
    }
    @Override
    public boolean isInEditMode() {
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
        this.isSubViewNumInited = true;
    }

    public void setChannel(ArrayList<Queue> channel) {
        this.channel = channel;
        this.inputChannelNum = channel.size();
        if (subViewList != null && subViewList.size() == 0) {
            createSubView();
        }
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
}

