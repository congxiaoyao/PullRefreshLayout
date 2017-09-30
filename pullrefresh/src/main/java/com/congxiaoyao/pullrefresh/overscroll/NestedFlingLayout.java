package com.congxiaoyao.pullrefresh.overscroll;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;

/**
 * Created by admin on 2017/9/14.
 */

public class NestedFlingLayout extends LinearLayout implements NestedScrollingParent {

    private static final String TAG = "cxy";
    private ScrollerCompat mScroller;
    private int mTotalScrollY = 0;
    private boolean mNestedScrollInProgress = false;

    public NestedFlingLayout(Context context) {
        this(context, null);
    }

    public NestedFlingLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mScroller = ScrollerCompat.create(context);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        super.onNestedScrollAccepted(child, target, axes);
        mNestedScrollInProgress = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        Log.d(TAG, "onNestedPreScroll: ");
        super.onNestedPreScroll(target, dx, dy, consumed);
        consumed[1] = dy;
        scrollBy(0, dy);
        mTotalScrollY += dy;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        Log.d(TAG, "onNestedPreFling: ");
        mScroller.fling(0, mTotalScrollY, 0, (int) velocityY, 0, 0, 0, 1000);
        invalidate();
        return true;
    }


    @Override
    public void onStopNestedScroll(View child) {
        Log.d(TAG, "onStopNestedScroll: ");
        mNestedScrollInProgress = false;
        super.onStopNestedScroll(child);
    }

    @Override
    public void computeScroll() {
        if (mNestedScrollInProgress) {
            return;
        }
        if (mScroller.computeScrollOffset()) {
            mTotalScrollY = mScroller.getCurrY();
            scrollTo(0, mTotalScrollY);
            invalidate();
        } else {
            Log.d(TAG, "computeScroll: finished");
        }
    }
}
