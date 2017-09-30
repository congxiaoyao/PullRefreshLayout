package com.congxiaoyao.pullrefresh.overscroll;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Created by admin on 2017/8/30.
 */

public class OverScrollParent extends FrameLayout implements NestedScrollingParent {

    String TAG = "cxy";
    private static final float MIN_DRAG_RATE = 0.25f;
    private static final float MAX_DRAG_RATE = 0.55f;
    private View loadingView;
    private View targetView;

    private boolean isRefreshing = false;
    private OnRefreshListener onRefreshListener;
    private boolean handling = false;

    public OverScrollParent(@NonNull Context context) {
        super(context);
    }

    public OverScrollParent(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public OverScrollParent(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (!handling) {
            super.onLayout(changed, left, top, right, bottom);
        }
        loadingView = getChildAt(0);
        targetView = getChildAt(1);
    }


    @Override
    public boolean startNestedScroll(int axes) {
        return super.startNestedScroll(axes);
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        if (!(target instanceof RecyclerView)) {
            return false;
        }
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        super.onNestedScrollAccepted(child, target, axes);
        handling = true;
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        super.onNestedPreScroll(target, dx, dy, consumed);
        int top = target.getTop();

        //向上推
        if (dy > 0) {
            //需要保证顶到头就不再偏移子View了
            if (dy > top) {
                dy = top;
            }
        }
        //向下拉
        else {
//            float rate = map(0, loadingView.getHeight(), MAX_DRAG_RATE, MIN_DRAG_RATE, top);
//            dy = (int) (dy * rate);
            //需保证当前位置可下拉(子View滑动到顶部)
            if (ViewCompat.canScrollVertically(target, -1)) {
                dy = 0;
            }
        }
        ViewCompat.offsetTopAndBottom(target, -dy);
        consumed[1] = dy;
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        return target.getTop() != 0;
    }

    @Override
    public void onStopNestedScroll(View child) {
        super.onStopNestedScroll(child);
        int top = child.getTop();
        int height = loadingView.getHeight();
        if (top == 0 || top == height) return;
        //等待刷新
        if (top > height) {
            isRefreshing = true;
            animateTo(child, height, new Runnable() {
                @Override
                public void run() {
                    if (onRefreshListener != null) onRefreshListener.onRefresh();
                }
            });
        }
        //撤回
        else {
            animateTo(child, 0);
        }
    }

    private void animateTo(final View view, final int to, final Runnable onFinished) {
        final int height = view.getHeight();
        ValueAnimator animator = ValueAnimator.ofInt(view.getTop(), to);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int top = (int) animation.getAnimatedValue();
                view.setTop(top);
                view.setBottom(top + height);
            }
        });
        animator.setDuration(500);

        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (onFinished != null) {
                    onFinished.run();
                }
                if (to == 0) {
                    handling = false;
                    requestLayout();
                }
            }
        });
        animator.start();
    }

    private void animateTo(final View view, int to) {
        animateTo(view, to, null);
    }

    private static float map(float from1, float to1, float from2, float to2, float value) {
        if (value > to1) return to2;
        if (value < from1) return from2;
        float lenV = value - from1;
        float len1 = to1 - from1;
        float rate = len1 == 0 ? 1 : lenV / len1;
        float len2 = to2 - from2;
        lenV = len2 * rate;
        return from2 + lenV;
    }

    public void setRefresh(boolean refresh) {
        if (refresh == isRefreshing) return;
        if (refresh) {
            animateTo(targetView, loadingView.getHeight(), new Runnable() {
                @Override
                public void run() {
                    onRefreshListener.onRefresh();
                }
            });
        } else {
            animateTo(targetView, 0);
        }
    }

    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        this.onRefreshListener = onRefreshListener;
    }

    public interface OnRefreshListener {

        void onRefresh();

    }
}
