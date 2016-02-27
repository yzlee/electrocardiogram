package cc.liyongzhi.electrocardiogram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Jesse write this view for draw line,use it easy.
 */
public class ElectrocardiogramView extends View {
	private final static String X_KEY = "Xpos";
	private final static String Y_KEY = "Ypos";

    //最大点的数量
	private int mPointMaxAmount;
    private int mXUnitLength;
    private int mCurX = 0;
    private int mCurY = 0;
    //当前加入点
    private int mCurP = 0;
    private Context mContext;
    private DisplayMetrics dm;


    private List<Map<String, Integer>> mListPoint = new ArrayList<Map<String, Integer>>();

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

        mXUnitLength = getWidth()/mPointMaxAmount;
        Log.i("getWidth :", "" + getWidth());

		for (int index = 0; index < mListPoint.size(); index++) {
            if (index == mCurP) {
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
	 * @param curX
	 *            which x position you want to draw.
	 * @param curY
	 *            which y position you want to draw.
	 */
	public void setLinePoint(int curX, int curY) {
		Map<String, Integer> temp = new HashMap<String, Integer>();
		temp.put(X_KEY, mCurX);
        mCurX += mXUnitLength;
        mCurY = curY;
        temp.put(Y_KEY, curY);
        //判断当前点是否大于最大点数
        if (mCurP < mPointMaxAmount) {
            try {
                if (mListPoint.size() == mPointMaxAmount && mListPoint.get(mCurP) != null) {
                    mListPoint.remove(mCurP);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            mListPoint.add(mCurP,temp);
            mCurP++;
        } else {
            mCurP = 0;
            mCurX = 0;
        }

        Log.d("mListPoint" , mListPoint.toString());
		invalidate();
	}

    public int getCurrentPointX() {
        return mCurX;
    }

    public int getCurrentPointY() {
        return mCurY;
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    public void setMaxPointAmount(int i) {
		mPointMaxAmount = i;

	}

}
