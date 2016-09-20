package cc.liyongzhi.ecgview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.text.LoginFilter;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by lee on 7/7/16.
 */
public class ECGView extends View {

    public static final int MODE_DRAW_PER_POINT = 0;
    public static final int MODE_DRAW_FROM_QUEUE = 1;

    public enum Order {
        VerticalThenHorizontal, HorizontalThenVertical
    }

    //Configuration by user
    private int subViewNum = 6; //Default sub-view number in one page.
    private boolean isZoomAllowed = true; // if allow changing sub-view number by two finger zooming.
    private int maxSubViewNum = 12;
    private int minSubViewNum = 1;
    private int drawPointSpeed = 250; //point per second, it determined the speed of drawing
    private int drawPaperSpeed = 25; //millimeter per second
    private int fps = 25; // frames per second, it determined if our eyes feeling comfortable.
    private int pointPerView = 500; //(the index is begin form 0) usefulPoint = (pointPerView - 1) / (pixelPerView - 1) * currentPixel
    private int YScale = 1;
    private boolean adaptiveDrawSpeed = true; // if cached point is less than threshold, then slow the drawing speed, vice versa.
    private int dataInputMode = 1;
    private int defaultLead = 1;
    private int columnSubViewNum = 2;
    private double aspectRatio = 4/3; //width / height
    private ArrayList<String> text = new ArrayList<>();
    private int secsSyncTimeInterval = 5000;
    private float gridInterval = 5; // 5mm
    private LinkedBlockingQueue<short[]> queue;
    private int arraySize;
    private float scaleThumbnail = 0.2f;
    private float scaleDetail = 0.5f;
    private int strokeWidthThumbnail = 1;
    private int strokeWidthDetail = 2;
    private Order order = Order.VerticalThenHorizontal;

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
    private boolean startFlag = false;
    private boolean stopFlag = false;
    private final int SLEEP_TIME = 1000;
    private int sleepTime = 1000;
    private static final String TAG = "ECGView";


    Handler handler=new Handler();

    Runnable refreshRunnable = new Runnable() {
        @Override
        public void run() {
            invalidate();

//            postDelayed(refreshRunnable, 1000 / fps);

        }
    };

    Runnable drawDataRunnable = new Runnable() {
        @Override
        public void run() {

            while (!stopFlag) {
                long currentTime = System.currentTimeMillis();

                long millisTimeInterval = currentTime - lastTimeMillis;

                //last time syc
                long secsTimeTimeInterval = currentTime - lastTimeSecs;

                //if never draw, set last drawing time is current time. clear the channel.

                if (secsTimeTimeInterval >= 1000) {
                    lastTimeMillis = currentTime;
                }

                int size = queue.size();

                if (size > 25000) {
                    sleepTime = SLEEP_TIME / 100;
                }

                if (size > 1000) {
                    sleepTime = SLEEP_TIME / 2;
                }

                if (size < 500) {
                    sleepTime = SLEEP_TIME;
                }

                if (size < 250) {
                    sleepTime = SLEEP_TIME * 2;
                }

                if (size < 100) {
                    sleepTime = SLEEP_TIME * 5;
                }

/*                if (secsTimeTimeInterval > secsSyncTimeInterval * 10) {
                    lastTimeMillis = currentTime;

                    queue.clear();

                    pointNumUntilSecSync = 0;
                    lastTimeSecs = currentTime;
                } else if (secsTimeTimeInterval > secsSyncTimeInterval) {
                    lastTimeSecs = currentTime;

                    int drawPointNumThisInterval = (int) (secsSyncTimeInterval / 1000.0 * 250) - pointNumUntilSecSync;
                    //todo throw drawPointNumThisInterval point.

                    pointNumUntilSecSync = 0;
                }*/



                if (millisTimeInterval < 1000 && millisTimeInterval > ((float)sleepTime) / drawPointSpeed) {
                    lastTimeMillis = currentTime;
                    int drawPointNumThisInterval = (int) (millisTimeInterval / ((float)sleepTime) * drawPointSpeed);
                    drawLine(drawPointNumThisInterval);
                    pointNumUntilSecSync += drawPointNumThisInterval;
                }

                try {
                    Thread.sleep(sleepTime / fps);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    handler.post(refreshRunnable);
                }
            }

           // handler.postDelayed(drawDataRunnable, 1000 / fps);
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

        if (isInEditMode()) {
            return;
        }

        this.context = context;
        //get pixelPerMillimeter
        displayMetrics = new DisplayMetrics();
        ((Activity)context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        pixelPerMillimeter = (float)(displayMetrics.xdpi / 2.54 /10);


        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scaleFactor *= detector.getScaleFactor();
//                LogShower.custom("liyongzhi", "ECGView", "onScale", "detector.getScaleFactor() = " + detector.getScaleFactor());
                if (scaleFactor > 1.2) {

                    if (isColumnSubViewNumSet && subViewNum > minSubViewNum && columnSubViewNum >= 1) {
                        if (subViewNum / columnSubViewNum < columnSubViewNum && columnSubViewNum > 1) {
                            subViewNum -= subViewNum / columnSubViewNum;
                            columnSubViewNum --;
                        } else {
                            subViewNum -= columnSubViewNum;
                        }

                        subViewNumChanged();
                        invalidate();
                        scaleFactor = 1.0f;
                    }
                    if (!isColumnSubViewNumSet && subViewNum > minSubViewNum) {
                        subViewNum--;

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
            ECGSubView subView = new ECGSubView(drawPaperSpeed, drawPointSpeed, pixelPerMillimeter);
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
        subView.setGridInterval(gridInterval);
        subView.setDrawBackground(true);
        subView.setOffsetStartPoint(0,0);
        subView.setScaling(scaleDetail);
        subView.setStrokeWidth(strokeWidthDetail);
        subView.setThumbnailMode(false);
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
            subview.setDrawBackground(false);
            subview.setScaling(scaleThumbnail);
            subview.setStrokeWidth(strokeWidthThumbnail);
            subview.setThumbnailMode(true);
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

    Paint bgPaint = new Paint();
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isInEditMode()) {



            bgPaint.setColor(Color.BLACK);
            int column = 2;
            int raw = 12;
            float subViewWidth = getWidth()  / column ;
            float subViewHeight = getHeight() / raw ;
            for (int i = 0; i < column; i++) {
                canvas.drawLine(i * subViewWidth, 0, i * subViewWidth, getHeight(), bgPaint);
            }
            canvas.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight(), bgPaint);

            for (int i = 0; i < raw; i++) {
                canvas.drawLine(0, i * subViewHeight, getWidth(), i * subViewHeight, bgPaint);
            }
            canvas.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1, bgPaint);

        } else {

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

    public  short[][] transposeMatrix(short[][] matrix)
    {
        int m = matrix.length;
        int n = matrix[0].length;

        short[][] transposedMatrix = new short[n][m];

        for(int x = 0; x < n; x++)
        {
            for(int y = 0; y < m; y++)
            {
                transposedMatrix[x][y] = matrix[y][x];
            }
        }

        return transposedMatrix;
    }

    private short[] changeCoordinate(short[] input) {

        short[] data = new short[input.length];

        if (order == Order.VerticalThenHorizontal) {

            for (int i = 0; i < data.length; i++) {
                data[i] = input[i / columnSubViewNum + i % columnSubViewNum * (((data.length - 1) / columnSubViewNum) + 1)];
            }

        } else {
            return input;
        }

        return data;
    }

    private String[] changeCoordinate(String[] input) {

        String[] data = new String[input.length];

        if (order == Order.VerticalThenHorizontal) {

            for (int i = 0; i < data.length; i++) {
                data[i] = input[i / columnSubViewNum + i % columnSubViewNum * (((data.length - 1) / columnSubViewNum) + 1)];
            }

        } else {
            return input;
        }

        return data;
    }

    private void drawLine(int num) {
        if (num <= 0) return;
        int pointPerFresh = drawPointSpeed / fps;
        short[][] dataN = new short[num][inputChannelNum]; // n line data in the same time; short[data][line]
        short[][] dataT;

        try {


            for (int i = 0; i < num; i++) {
                dataN[i] = queue.take();
                dataN[i] = changeCoordinate(dataN[i]);
            }

            if (dataN.length == 0) return;

            dataT = transposeMatrix(dataN);

            for (int i = 0; i < inputChannelNum; i++) {
                ECGSubView subView = subViewList.get(i);
                short[] data = dataT[i];
                subView.addData(data);
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

/*        if (thumbnailOrDetail == -1) {
            for (int i = 0; i < inputChannelNum; i++) {
                Queue queue = channel.get(i);
                ECGSubView subView = subViewList.get(i);
                short[] data = subView.getData();
                int endPoint = subView.getNextStartPoint();
                endPoint = endPoint + num >= data.length ? (endPoint + num) % data.length : endPoint + num;
                subView.setData(data, endPoint);
            }
        } else {
            Queue queue = channel.get(thumbnailOrDetail);
            ECGSubView subView = subViewList.get(thumbnailOrDetail);
            short[] data = subView.getData();
            int endPoint = subView.getNextStartPoint();
            endPoint = endPoint + num >= data.length ? (endPoint + num) % data.length : endPoint + num;
            subView.setData(data, endPoint);
        }*/
    }

    public void start() {

        if (!startFlag) {
            startFlag = true;
            stopFlag = false;
            if (subViewList == null || subViewList.size() == 0) {
                createSubView();
            }
            new Thread(drawDataRunnable).start();
//        post(refreshRunnable);
        }
    }

    public void stop() {
        stopFlag = true;
        startFlag = false;
    }

    private double countDistance(float oldX, float oldY, float x, float y) {
        return Math.sqrt(Math.pow(oldX - x, 2) + Math.pow(oldY - y, 2));
    }

    float downX;
    float downY;
    float upX;
    float upY;

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                downX = event.getX();
                downY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                upX = event.getX();
                upY = event.getY();

                if (countDistance(downX, downY, upX, upY) > 5) {
                    break;
                }

                if (thumbnailOrDetail == -1) {
                    showDetail((int) upX, (int) upY);
                } else {
                    showThumbnail();
                }
                break;
        }
//        scaleGestureDetector.onTouchEvent(event);


//        LogShower.custom("liyongzhi", "ECGView", "onTouchEvent", "get into onTouchEvent");
        invalidate();
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

    public void setChannel(LinkedBlockingQueue<short[]> queue) {
        this.queue = queue;
        queue.clear();
    }

    public void setText(ArrayList<String> text) {
        String[] t = (String[])text.toArray(new String[text.size()]);
        text.clear();
        Collections.addAll(text, changeCoordinate(t));
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

    public void setInputChannelNum(int inputChannelNum) {
        this.inputChannelNum = inputChannelNum;
    }


    public void setDrawPaperSpeed(int drawPaperSpeed) {
        this.drawPaperSpeed = drawPaperSpeed;
    }

    public void setOrder(Order order) {
        this.order = order;
    }
    
    public void setScaleDetail(float scaleDetail) {
        this.scaleDetail = scaleDetail;
    }
}

