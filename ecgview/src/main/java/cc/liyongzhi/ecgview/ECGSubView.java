package cc.liyongzhi.ecgview;

import android.graphics.Canvas;

/**
 * Created by lee on 7/8/16.
 */
public class ECGSubView {

    private int offsetStartPoint = 0;
    private int subHeight = 0;
    private int subWidth = 0;
    private int parentWidth = 0;
    private int parentHeight = 0;

    public ECGSubView() {

    }

    public void draw(Canvas canvas) {

    }

    public void setOffsetStartPoint(int offsetStartPoint) {
        this.offsetStartPoint = offsetStartPoint;
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
}
