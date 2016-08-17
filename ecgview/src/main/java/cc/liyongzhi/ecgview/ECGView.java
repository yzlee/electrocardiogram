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
    private ArrayList<String> text = new ArrayList<>();
    private int secsSyncTimeInterval = 5000;


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
    private int originColumnSubViewNum = 2;
    private int thumbnailOrDetail = -1; // -1 is thumbnail
    private Context context;
    private long lastTimeMillis = 0;
    private long lastTimeSecs = 0;
    private int pointNumUntilSecSync = 0;
    private ArrayList<int[]> dataToSubViewList = new ArrayList<>();


    Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            invalidate();

            postDelayed(refreshRunnable, 1000 / fps);

        }
    };

    Runnable drawDataRunnable = new Runnable() {
        @Override
        public void run() {

            long currentTime = System.currentTimeMillis();

            long millisTimeInterval = currentTime - lastTimeMillis;
            long secsTimeTimeInterval = currentTime - lastTimeSecs;


            if (secsTimeTimeInterval > secsSyncTimeInterval * 10) {
                lastTimeMillis = currentTime;
                for (Queue queue : channel) {
                    queue.clear();
                }
                pointNumUntilSecSync = 0;
                lastTimeSecs = currentTime;
            } else if (secsTimeTimeInterval > secsSyncTimeInterval) {
                lastTimeSecs = currentTime;

                int drawPointNumThisInterval = (int) (secsSyncTimeInterval / 1000.0 * 250) - pointNumUntilSecSync;


                pointNumUntilSecSync = 0;
            }

            lastTimeMillis = currentTime;

            if (millisTimeInterval < 1000) {
                int drawPointNumThisInterval = (int) (millisTimeInterval / 1000.0 * 250);
                queueToArrayByNum(drawPointNumThisInterval);
                pointNumUntilSecSync += drawPointNumThisInterval;
            }

            postDelayed(drawDataRunnable, 1000 / fps);
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

        this.context = context;
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
                    if (isColumnSubViewNumSet && subViewNum > minSubViewNum && columnSubViewNum >= 1) {
                        if (subViewNum / columnSubViewNum < columnSubViewNum && columnSubViewNum > 1) {
                            subViewNum -= subViewNum / columnSubViewNum;
                            columnSubViewNum --;
                        } else {
                            subViewNum -= columnSubViewNum;
                        }
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
                    if (isColumnSubViewNumSet && subViewNum < maxSubViewNum && columnSubViewNum <= originColumnSubViewNum) {
                        if (subViewNum / columnSubViewNum >= columnSubViewNum && columnSubViewNum < originColumnSubViewNum) {
                            subViewNum += subViewNum / columnSubViewNum;
                            columnSubViewNum ++;
                        } else {
                            subViewNum += columnSubViewNum;
                        }

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
        if (thumbnailOrDetail == -1) {
            changeSubViewLayout();
        } else {
            changeCertainSubViewLayout(thumbnailOrDetail);
        }

    }


    private void createSubView() {
        for (int i = 0; i < inputChannelNum; i++) {
            ECGSubView subView = new ECGSubView(channel.get(i));
            if (text.get(i) != null) {
                subView.setText(text.get(i));
            }
            subViewList.add(subView);
        }
    }

    private void changeCertainSubViewLayout(int id) {
        ECGSubView subView = subViewList.get(id);
        subView.setSubHeight(mainViewHeight);
        subView.setSubWidth(mainViewWidth);
        subView.setOffsetStartPoint(0,0);
        int[] data = new int[(int) (subViewList.get(id).getSubWidth() / (pixelPerMillimeter * 25 / drawPointSpeed))];
        subView.setData(data, 0);

    }

    private void changeSubViewLayout() {
        ECGSubView subview = null;
        int tmp = currentPageLeftSubViewNumber % columnSubViewNum;
        for (int i = currentPageStartIndex; i < currentPageLeftSubViewNumber + currentPageStartIndex; i++) {
            subview = subViewList.get(i);

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
/*            //get the precise pixel address of sub-view's height.
            if (i - currentPageStartIndex >= currentPageLeftSubViewNumber - tmp) {
                subview.setSubHeight(mainViewHeight - offsetStartPointY);
            }

            if (i - currentPageStartIndex >= columnSubViewNum && (i - currentPageStartIndex) % columnSubViewNum == 0) {
                for (int j = i - columnSubViewNum; j < i; j++) {
                    ECGSubView preHSubview = subViewList.get(j);
                    preHSubview.setSubHeight(offsetStartPointY - preHSubview.getOffsetStartPointY());
                }
            }*/
        }
        int totalLineNum = tmp == 0 ? currentPageLeftSubViewNumber / columnSubViewNum : currentPageLeftSubViewNumber / columnSubViewNum + 1;
        for (int i = currentPageStartIndex; i < currentPageLeftSubViewNumber + currentPageStartIndex; i++) {
            int lineNum = i / columnSubViewNum + 1;
            subview = subViewList.get(i);
            int thisLineOffsetStartPointY = subview.getOffsetStartPointY();
            if (lineNum < totalLineNum) {
                int nextLineOffsetStartPointY = subViewList.get(lineNum * columnSubViewNum).getOffsetStartPointY();
                subview.setSubHeight(nextLineOffsetStartPointY - thisLineOffsetStartPointY);
            } else {
                subview.setSubHeight(mainViewHeight - thisLineOffsetStartPointY);
            }
            int[] data = new int[(int) (subview.getSubWidth() / (pixelPerMillimeter * 25 / drawPointSpeed))];
            subview.setData(data, 0);
        }
    }

    private void drawCurrentPage(Canvas canvas) {
        for (int i = currentPageStartIndex; i < currentPageLeftSubViewNumber + currentPageStartIndex; i++) {
            ECGSubView subView = subViewList.get(i);
            subView.draw(canvas);
        }
    }

    private void drawDetail(Canvas canvas) {
        ECGSubView subView = subViewList.get(thumbnailOrDetail);
        subView.draw(canvas);
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

        if (thumbnailOrDetail == -1) {
            drawCurrentPage(canvas);
        } else {
            drawDetail(canvas);
        }
    }

    private void showDetail(int x, int y) {
        for (int i = 0; i < subViewList.size(); i ++) {
            ECGSubView subView = subViewList.get(i);
            if (subView.isBelongToMe(x,y)) {
                thumbnailOrDetail = i;
                invalidate();
            }
        }
        subViewNumChanged();
    }

    private void showThumbnail() {

        thumbnailOrDetail = -1;
        subViewNumChanged();
    }

    private void queueToArrayByNum(int num) {
        int pointPerFresh = drawPointSpeed / fps;

        if (thumbnailOrDetail == -1) {
            for (int i = 0; i < inputChannelNum; i++) {
                Queue queue = channel.get(i);
                ECGSubView subView = subViewList.get(i);
                int[] data = subView.getData();
                int endPoint = subView.getEndPoint();
                endPoint = endPoint + num >= data.length ? (endPoint + num) % data.length : endPoint + num;
                subView.setData(data, endPoint);
            }
        } else {
            Queue queue = channel.get(thumbnailOrDetail);
            ECGSubView subView = subViewList.get(thumbnailOrDetail);
            int[] data = subView.getData();
            int endPoint = subView.getEndPoint();
            endPoint = endPoint + num >= data.length ? (endPoint + num) % data.length : endPoint + num;
            subView.setData(data, endPoint);
        }
    }

    public void start() {


        if (subViewList != null && subViewList.size() == 0) {
            createSubView();
        }
        post(drawDataRunnable);
        post(refreshRunnable);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        LogShower.custom("liyongzhi", "ECGView", "onTouchEvent", "event.getPointerCount" + event.getPointerCount());
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (thumbnailOrDetail == -1) {
                    showDetail((int) event.getX(), (int) event.getY());
                } else {
                    showThumbnail();
                }
                return true;

        }
//        scaleGestureDetector.onTouchEvent(event);


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

    }

    public void setText(ArrayList<String> text) {
        this.text = text;
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
        this.originColumnSubViewNum = columnSubViewNum;
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

