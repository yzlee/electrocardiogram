package cc.liyongzhi.electrocardiogram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author liyongzhi
 *
 *
 *
 */
public class ElectrocardiogramView extends View {
    private final static String X_KEY = "Xpos";
    private final static String Y_KEY = "Ypos";

    //最大点的数量
    private int mPointMaxAmount;
    //需要修正的次数
    private int mCorrectionPointNumber;
    //由于计算每段尺寸时除法小数部分舍弃造成的误差的累计值
    private int mErrorWidth;
    private float mXUnitLength;
    private float mCurX = 0;
    private float mCurY = 0;
    //当前加入点
    private int mCurP = 0;
    private int mRemovedPointNum = 0;

    private int mStartYOffset;

    private int mEveryNPoint = 1;
    private int mEveryNPointBold = 1;
    //设置为false后不会重新生成背景
    private Boolean mBFlag = true;
    //设置为false后不会再次计算起始点Y轴位置
    private Boolean mIsStartPosNotSet = true;


    //设置每mEveryNPointRefresh个点刷新一次
    private int mEveryNPointRefresh = 1;
    //当前在第mCurrentPoint个点。
    private int mCurrentPoint = 1;


    private Context mContext;
    private DisplayMetrics dm;


    private List<Map<String, Float>> mListPoint = new ArrayList<Map<String, Float>>();
    private List<Float> mListVLine = new ArrayList();
    private List<Float> mListHLine = new ArrayList();

    Paint mPaint = new Paint();

    public ElectrocardiogramView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public ElectrocardiogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.mContext = context;
        initView();
    }

    public ElectrocardiogramView(Context context) {
        this(context, null);
    }

    private void initView() {
/*        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);*/
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        mPaint.setColor(Color.RED);
        mPaint.setAntiAlias(true);

        mXUnitLength = getWidth() / (mPointMaxAmount - 1);
        int relWidth = (int) (mXUnitLength * (mPointMaxAmount - 1));
        mErrorWidth = getWidth() - relWidth;

        if (mErrorWidth != 0) {
            mCorrectionPointNumber = mPointMaxAmount / mErrorWidth;
        }

        if (mIsStartPosNotSet) {
            mStartYOffset = getHeight() / 2;
            mIsStartPosNotSet = false;
        }

//        Log.i("getWidth :", "" + getWidth());
//        Log.i("getRealWidth :", "" + relWidth);

        drawBackground(canvas);
        drawWave(canvas);


    }

    public void drawBackground(Canvas canvas) {

        int num = mPointMaxAmount/mEveryNPoint;

        if (mBFlag) {
            float curX = 0;
            for (int i = 0; i < mPointMaxAmount; i++) {
                mListVLine.add(curX);
                if (mCorrectionPointNumber!=0 && i%mCorrectionPointNumber == 0) {
                    curX++;
                }
                curX += mXUnitLength;
            }

            float curY = 0;
            while (curY < getHeight()) {
                mListHLine.add(curY);
                curY += mXUnitLength;
            }
            mBFlag = false;
        }

        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setAntiAlias(true);

        paint.setStrokeWidth(10);
        canvas.drawLine(getWidth(), 0, getWidth(), getHeight(), paint);
        paint.setStrokeWidth(1);
        for (int i = 0; i < mListVLine.size(); i++) {
            if (i == 0) {
                paint.setStrokeWidth(10);
                canvas.drawLine(mListVLine.get(i), 0, mListVLine.get(i), getHeight(), paint);
                paint.setStrokeWidth(1);
            } else if (i%mEveryNPoint == 0){
                if (i%mEveryNPointBold == 0) {
                    paint.setStrokeWidth(3);
                    canvas.drawLine(mListVLine.get(i), 0, mListVLine.get(i), getHeight(), paint);
                    paint.setStrokeWidth(1);
                } else {
                    canvas.drawLine(mListVLine.get(i), 0, mListVLine.get(i), getHeight(), paint);
                }
            }
        }
        paint.setStrokeWidth(10);
        canvas.drawLine(0, getHeight(), getWidth(), getHeight(), paint);
        paint.setStrokeWidth(1);
        for (int i = 0; i < mListHLine.size(); i++) {
            if (i == 0) {
                paint.setStrokeWidth(10);
                canvas.drawLine(0,  mListHLine.get(i), getWidth(), mListHLine.get(i), paint);
                paint.setStrokeWidth(1);
            } else if (i%mEveryNPoint == 0){
                if (i%mEveryNPointBold == 0) {
                    paint.setStrokeWidth(3);
                    canvas.drawLine(0,  mListHLine.get(i), getWidth(), mListHLine.get(i), paint);
                    paint.setStrokeWidth(1);
                } else {
                    canvas.drawLine(0, mListHLine.get(i), getWidth(), mListHLine.get(i), paint);
                }
            }
        }
    }

    public void drawWave(Canvas canvas) {
        for (int index = 0; index < mListPoint.size(); index++) {
            if (mListPoint.size() == mPointMaxAmount && (index >= mCurP && index <mCurP + mRemovedPointNum)) {
                continue;
            }
            if (index > 0) {

                canvas.drawLine(mListPoint.get(index - 1).get(X_KEY),
                        mListPoint.get(index - 1).get(Y_KEY),
                        mListPoint.get(index).get(X_KEY), mListPoint.get(index)
                                .get(Y_KEY), mPaint);
                canvas.setDrawFilter(new PaintFlagsDrawFilter(0,
                        Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
            }
        }
    }

    /**
     * @param curY which y position you want to draw.
     */
    public void setLinePoint(float curY) {

        Map<String, Float> temp = new HashMap<String, Float>();
        temp.put(X_KEY, mCurX);
        if (mCorrectionPointNumber!=0 && mCurP%mCorrectionPointNumber == 0) {
            mCurX++;
        }
        mCurX += mXUnitLength;


        //计算y真实所在点
        float y = curY;
        mCurY = mStartYOffset - y;


        temp.put(Y_KEY, mCurY);
        //判断当前点是否大于最大点数
        if (mCurP < mPointMaxAmount) {
            try {
                if (mListPoint.size() == mPointMaxAmount && mListPoint.get(mCurP) != null) {
                    mListPoint.remove(mCurP);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mListPoint.add(mCurP, temp);
            mCurP++;
        } else {
            mCurP = 0;
            mCurX = 0;
        }

//        Log.d("mListPoint", mListPoint.toString());
        if (mCurP % mEveryNPointRefresh == 0) {
            invalidate();
        }

    }

    public void setRemovedPointNum(int removedPointNum) {
        mRemovedPointNum = removedPointNum;
    }

    /**
     * @param pos 以左上角为起始点 向下的点的数量
     */
    public void setYPosOffset(int pos) {
        mStartYOffset =  pos;
        mIsStartPosNotSet = false;
    }


    /**
     * 设置每N个点刷新一次
     * @param num 每num个点。
     */
    public void setEveryNPointRefresh(int num) {
        mEveryNPointRefresh = num;
    }

    public float getCurrentPointX() {
        return mCurX;
    }

    public float getCurrentPointY() {
        return mCurY;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setMaxPointAmount(int i) {
        mPointMaxAmount = i;
    }

    /**
     *@param everyNPoint 每everyNPoint个点画一条细线
     *@param everyNPointBold  每everyNPoint个点画一条粗线
     */
    public void setEveryNPoint(int everyNPoint,int everyNPointBold) {
        if (everyNPoint == 0 || everyNPointBold < everyNPoint) {
            return;
        }
        mEveryNPoint = everyNPoint;
        mEveryNPointBold = everyNPointBold;
    }
}
