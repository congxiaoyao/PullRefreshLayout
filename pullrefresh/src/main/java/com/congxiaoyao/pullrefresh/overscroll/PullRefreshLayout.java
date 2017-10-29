package com.congxiaoyao.pullrefresh.overscroll;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.v4.view.NestedScrollingChild;
import android.support.v4.view.NestedScrollingChildHelper;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ScrollerCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by admin on 2017/9/7.
 */

public class PullRefreshLayout extends ViewGroup implements NestedScrollingParent, NestedScrollingChild {

    private static final String TAG = PullRefreshLayout.class.getSimpleName();
    public static final int SCROLLING_DURATION = 350;
    private NestedScrollingChildHelper mNestedScrollingChildHelper;
    private NestedScrollingParentHelper mNestedScrollingParentHelper;
    private final int[] mParentScrollConsumed = new int[2];
    private final int[] mParentOffsetInWindow = new int[2];

    private View mScrollingView;
    //NestedChild(mScrollingView)的偏移量
    private int mTopOffset = 0;
    //手指在屏幕上的移动距离
    private int mTotalTouchY = 0;
    //fling时的移动速度 以备滑动处理之需
    private int mFlingVelocityY = 0;

    private View mLoadingView;
    private int mLoadingViewLayer = Gravity.BOTTOM;
    private int mLoadingViewIndex = -1;
    private Rect mLoadingViewBounds;
    private LoadingViewAdapter mLoadingViewAdapter;

    private ScrollerCompat mScroller;

    private boolean mRefreshing = false;
    private boolean mForceAnimating = false;
    private boolean mNestedScrollInProgress = false;

    public PullRefreshLayout(Context context) {
        this(context, null);
    }

    public PullRefreshLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        mNestedScrollingParentHelper = new NestedScrollingParentHelper(this);

        mNestedScrollingChildHelper = new NestedScrollingChildHelper(this);
        setNestedScrollingEnabled(true);

        mLoadingViewBounds = new Rect();

        mScroller = ScrollerCompat.create(context);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (!ensureChildren()) return;
        //测量LoadingView
        if (mLoadingViewAdapter != null) {
            mLoadingViewAdapter.onRequestLoadingViewBounds(mLoadingViewBounds, mTopOffset, false);
            mLoadingView.measure(MeasureSpec.makeMeasureSpec(mLoadingViewBounds.width(),
                    MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mLoadingViewBounds.height(),
                    MeasureSpec.EXACTLY));
        }
        //测量NestedChild
        mScrollingView.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth()
                        - getPaddingLeft() - getPaddingRight(), MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(getMeasuredHeight()
                        - getPaddingTop() - getPaddingBottom(), MeasureSpec.EXACTLY));

        //确定LoadingView索引
        for (int i = 0; i < getChildCount(); i++) {
            if (getChildAt(i).equals(mLoadingView)) {
                mLoadingViewIndex = i;
                break;
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (!ensureChildren()) {
            return;
        }
        //layout LoadingView,可直接根据mLoadingViewBounds进行layout
        mLoadingView.layout(mLoadingViewBounds.left, mLoadingViewBounds.top,
                mLoadingViewBounds.right, mLoadingViewBounds.bottom);

        //layout mScrollingView 将其铺满全屏
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int nestedChildLeft = getPaddingLeft();
        int nestedChildRight = getPaddingRight();
        int nestedChildWidth = width - nestedChildLeft - nestedChildRight;
        int nestedChildHeight = height - getPaddingBottom() - getPaddingTop();
        mScrollingView.layout(nestedChildLeft, mTopOffset,
                nestedChildWidth + nestedChildLeft, mTopOffset + nestedChildHeight);
        Log.d("cxy", "onLayout: ");
    }

    /**
     * 确定PullRefreshLayout中的滑动View及加载View
     *
     * @return 成功找到两个View返回true 否则false
     */
    public boolean ensureChildren() {
        if (mScrollingView == null || mLoadingView == null) {
            View view0 = getChildAt(0);
            View view1 = getChildAt(1);
            if (view1 instanceof NestedScrollingChild) {
                mScrollingView = view1;
                mLoadingView = view0;
            } else if (view0 instanceof NestedScrollingChild) {
                mScrollingView = view0;
                mLoadingView = view0;
            }
        }
        if (mScrollingView != null && mLoadingView != null) {
            return true;
        }
        return false;
    }

    /**
     * 根据LoadingView的绘制层级返回子View的绘制顺序
     * 简单的说,当我希望LoadingView显示在最上层时,子View的绘制顺序将调整为LoadingView最后绘制,
     * 其他View绘制的相对顺序保持不变
     *
     * @param childCount
     * @param i
     * @return
     */
    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        if (mLoadingViewLayer == Gravity.BOTTOM) {
            if (i == 0) {
                return mLoadingViewIndex;
            }
            if (i <= mLoadingViewIndex) {
                return i - 1;
            }
        } else if (mLoadingViewLayer == Gravity.TOP) {
            if (i == childCount - 1) {
                return mLoadingViewIndex;
            }
            if (i >= mLoadingViewIndex) {
                return i + 1;
            }
        }
        return i;
    }

    /**
     * 设置LoadingView层级,传入{@link Gravity#TOP}可使LoadingView至于顶层
     * 传入 {@link Gravity#BOTTOM} 可使LoadingView至于底层
     *
     * @param gravity
     */
    public void setLoadingViewLayer(int gravity) {
        this.mLoadingViewLayer = gravity;
    }

    public void setRefreshing(boolean refreshing) {
        if (this.mRefreshing == refreshing) return;

        this.mRefreshing = refreshing;
        mForceAnimating = true;
        //切换为刷新中状态
        if (mRefreshing) {
            mScroller.startScroll(0, mTopOffset, 0, mLoadingViewAdapter.getSteadyStateDistance() - mTopOffset, SCROLLING_DURATION);
        }
        //切换为刷新结束状态
        else {
            mScroller.startScroll(0, mTopOffset, 0, -mTopOffset, SCROLLING_DURATION);
        }
        requestLayout();
        invalidate();
    }

    public boolean isRefreshing() {
        return mRefreshing;
    }

    //    public void enableTouchWhenRefreshing(boolean enableTouchWhenRefreshing) {
//
//    }

    public void setLoadingViewAdapter(LoadingViewAdapter adapter) {
        mLoadingViewAdapter = adapter;
        mLoadingViewAdapter.parent = this;
        ensureChildren();
        mLoadingViewAdapter.loadingView = mLoadingView;
        requestLayout();
    }

    // NestedScrollingParent
    @Override
    public int getNestedScrollAxes() {
        return mNestedScrollingParentHelper.getNestedScrollAxes();
    }

    @Override
    public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
        return (nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0;
//                && !isAutoReturning;
    }

    @Override
    public void onNestedScrollAccepted(View child, View target, int axes) {
        mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, axes);
        mNestedScrollInProgress = true;
        mFlingVelocityY = 0;
        startNestedScroll(axes);
    }

    @Override
    public void onNestedScroll(View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
        //让父布局先滑动
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, mParentOffsetInWindow);
        //dy中去掉父布局用掉的部分
        final int dy = dyUnconsumed + mParentOffsetInWindow[1];

        //向下拉
        if (dy < 0 && !ViewCompat.canScrollVertically(target, -1)) {
            if (mRefreshing) {
                mTopOffset -= dy;
                int stateDistance = mLoadingViewAdapter.getSteadyStateDistance();
                if (mTopOffset > stateDistance) {
                    mTopOffset = stateDistance;
                }
                mTotalTouchY = mLoadingViewAdapter.getTotalTouchYByTopOffset(mTopOffset);
            } else {
                mTotalTouchY -= dy;
                mTopOffset = mLoadingViewAdapter.getTopOffsetByTotalOffset(mTotalTouchY);
            }
            Log.d(TAG, "pull down: mTotalTouchY = " + mTotalTouchY + " dy = " + dy);
            ViewCompat.offsetTopAndBottom(mScrollingView, mTopOffset - mScrollingView.getTop());
            requestLayout();
        }
    }

    @Override
    public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
        //向上推
        if (dy > 0 && mTotalTouchY > 0) {
            //需要保证顶到头就不再偏移子View了
            if (dy > mTotalTouchY) {
                consumed[1] = mTotalTouchY;
                mTotalTouchY = 0;
            } else {
                mTotalTouchY -= dy;
                consumed[1] = dy;
            }
            Log.d(TAG, "pull up: mTotalTouchY = " + mTotalTouchY + " dy = " + dy + " consumed = " + consumed[1]);
            mTopOffset = mLoadingViewAdapter.getTopOffsetByTotalOffset(mTotalTouchY);
            Log.d("cxy", "offsetTopAndBottom");
            ViewCompat.offsetTopAndBottom(mScrollingView, mTopOffset - mScrollingView.getTop());
            requestLayout();
        }
        final int[] parentConsumed = mParentScrollConsumed;
        if (dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
            consumed[0] += parentConsumed[0];
            consumed[1] += parentConsumed[1];
        }
    }

    @Override
    public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
        if (dispatchNestedPreFling(velocityX, velocityY)) return true;

        //向下拉为负,向上推为正
        Log.d(TAG, "onNestedPreFling: mFlingVelocityY = " + velocityY);

        if (mTopOffset == 0) return false;

        if (!mRefreshing && velocityY >= 0) {
            mFlingVelocityY = (int) velocityY;
            return true;
        }
        return false;
    }

    @Override
    public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
        if (mRefreshing) {
            mFlingVelocityY = (int) velocityY;
            return true;
        }
        return dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public void onStopNestedScroll(View child) {
        Log.d(TAG, "onStopNestedScroll: ");
        mNestedScrollingParentHelper.onStopNestedScroll(child);
        mNestedScrollInProgress = false;

        if (mTopOffset == 0) {
            stopNestedScroll();
            return;
        }

        //刷新中 无阻尼滑动
        if (mRefreshing) {
            if (mFlingVelocityY != 0) {
                mScroller.fling(0, mTopOffset, 0, -mFlingVelocityY, 0, 0, 0, mLoadingViewAdapter.getSteadyStateDistance());
            }
        } else {
            //用户向上快速滑动动取消下拉
            if (mFlingVelocityY >= 1200) {
                int time = (int) (mTopOffset / (float) mFlingVelocityY * 3200);
                mScroller.startScroll(0, mTopOffset, 0, -mTopOffset, time);
            }
            //用户松手,回弹至合适位置
            else {
                int steadyDistance = mLoadingViewAdapter.getSteadyStateDistance();
                if (mTopOffset >= steadyDistance) {
                    mScroller.startScroll(0, mTopOffset, 0, steadyDistance - mTopOffset, SCROLLING_DURATION);
                } else {
                    mScroller.startScroll(0, mTopOffset, 0, -mTopOffset, SCROLLING_DURATION);
                }
            }
        }
        requestLayout();
        invalidate();
        stopNestedScroll();
    }

    // NestedScrollingChild 摘自SwipeRefreshLayout
    @Override
    public void setNestedScrollingEnabled(boolean enabled) {
        mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
    }

    @Override
    public boolean isNestedScrollingEnabled() {
        return mNestedScrollingChildHelper.isNestedScrollingEnabled();
    }

    @Override
    public boolean startNestedScroll(int axes) {
        return mNestedScrollingChildHelper.startNestedScroll(axes);
    }

    @Override
    public void stopNestedScroll() {
        mNestedScrollingChildHelper.stopNestedScroll();
    }

    @Override
    public boolean hasNestedScrollingParent() {
        return mNestedScrollingChildHelper.hasNestedScrollingParent();
    }

    @Override
    public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                                        int dyUnconsumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed,
                dxUnconsumed, dyUnconsumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
        return mNestedScrollingChildHelper.dispatchNestedPreScroll(
                dx, dy, consumed, offsetInWindow);
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        return mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        return mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
    }

    @Override
    public void computeScroll() {
        if (mNestedScrollInProgress) {
//            isAutoReturning = false;
            Log.d(TAG, "computeScroll: abort...");
            mScroller.abortAnimation();
            return;
        }
        if (mScroller.computeScrollOffset()) {
            mTopOffset = mScroller.getCurrY();
            mTotalTouchY = mLoadingViewAdapter.getTotalTouchYByTopOffset(mTopOffset);
            requestLayout();
            invalidate();
            return;
        }
        Log.d(TAG, "computeScroll: mTopOffset = " + mTopOffset + " velocity = " + mScroller.getCurrVelocity());
//        isAutoReturning = false;
        if (mTopOffset == mLoadingViewAdapter.getSteadyStateDistance()) {
            mRefreshing = true;
            mLoadingView.setBackgroundColor(Color.GRAY);
        }
    }

    /**
     * LoadingView适配器
     * PullRefreshLayout负责下拉刷新的逻辑处理与流程,顶部LoadingView的显示效果(布局及绘制)交由适配器来完成
     * 有几个方法要求必须被重写以便控制LoadingView的显示效果
     * 当NestedChild与PullRefreshLayout顶部距离发生变化时,需要重写{@link LoadingViewAdapter#onRequestLoadingViewBounds(Rect, int, boolean)}
     * 方法来根据当前的移动距离计算LoadingView布局边界,松开手指后,NestedChild开始回弹
     * 此时需要覆写{@link LoadingViewAdapter#getSteadyStateDistance()} 以规定回弹至什么位置
     * 既稳定状态下NestedChild与PullRefreshLayout顶部距离
     * 两个方法具体的覆写要求参见方法注释
     * 每当LoadingView的布局参数发生改变,{@link LoadingViewAdapter#onLoadingViewLayoutChanged(Rect, int, int)}方法将被调用
     * 为了控制下拉手感,可以覆写{@link LoadingViewAdapter#getTopOffsetByTotalOffset(int)}来根据手指移动距离返回下拉高度
     */
    public static abstract class LoadingViewAdapter {

        private PullRefreshLayout parent;
        private View loadingView;

        /**
         * 当NestedChild与PullRefreshLayout顶部距离发生变化时,此方法将被回调
         * 需要根据{@param topOffset},{@param mRefreshing}计算LoadingView的布局位置并放入{@param bounds}中
         * 需要说明的是,{@param bounds}代表了LoadingView的整个布局信息,包括宽高与位置,既LoadingView的
         * 左上角坐标与右下角坐标
         *
         * @param topOffset NestedChild与PullRefreshLayout顶部的实际距离
         * @param isRefreshing 是否处在下拉过程中
         *                     下拉过程指从初始状态手指触摸屏幕开始,到手指抬起的整个过程,与手指的移动方向无关
         *                     其他的情况状态导致的{@link LoadingViewAdapter#onRequestLoadingViewBounds(Rect, int, boolean)}回调的产生
         *                     都会使{@param mRefreshing}参数为false
         */
        public abstract void onRequestLoadingViewBounds(Rect bounds, int topOffset, boolean isRefreshing);

        /**
         * 当NestedChild与PullRefreshLayout顶部距离发生变化时,此方法将被回调,需要注意的是
         * 此方法的调用时机位于{@link LoadingViewAdapter#onRequestLoadingViewBounds(Rect, int, boolean)}之后
         * 也就是当LoadingView的布局参数被重新确定下来的时候
         *
         * @param bounds      LoadingView的布局信息
         * @param topOffset   NestedChild的顶部与PullRefreshLayout顶部之间的距离
         * @param totalTouchY 手指在屏幕上移动的总长度
         *                    注意,{@param topOffset}并不总是等于{@param totalTouchY}
         *                    两者的关系取决于 {@link LoadingViewAdapter#getTopOffsetByTotalOffset(int)} 的行为
         */
        public void onLoadingViewLayoutChanged(Rect bounds, int topOffset, int totalTouchY) {

        }

        /**
         * @return 当控件处于刷新状态, NestedChild与PullRefreshLayout顶部距离暂时稳定不发生变化时, 两者之间的距离
         * 为了说明情况,可以简单理解为刷新状态时LoadingView的高度
         */
        public abstract int getSteadyStateDistance();

        /**
         * 屏幕上手指的滑动距离与NestedChild真正的滑动距离之间的映射关系
         * 如果希望实现阻尼效果,可以覆写此方法控制滑动手感
         *
         * @param totalTouchY 手指在屏幕移动的总距离
         * @return NestedChild的滑动距离
         */
        public int getTopOffsetByTotalOffset(int totalTouchY) {
            return totalTouchY;
        }

        public int getTotalTouchYByTopOffset(int topOffset) {
            return topOffset;
        }

        /**
         * @return PullRefreshLayout
         */
        public PullRefreshLayout getParent() {
            return parent;
        }

        public View getLoadingView() {
            return loadingView;
        }
    }

    /**
     * PullRefreshLayout的操作状态保存在一个byte类型的变量之中,可以通过{@link State}类提供的静态方法进行解析
     * 变量包含了当前时刻的位置状态 手指按压状态 以及刷新状态 可通过相应的三个方法获取
     * 方法返回的为具体的状态类型,可通过类内的静态字段了解其具体含义
     *
     * 如 byte state = 0b01100;
     * 调用{@link State#getRefreshState(byte)} 可得到返回值0x01 既刷新中状态
     * 调用{@link State#getLocationState(byte)}可得到返回值0x10 既当前位置位于稳定线上
     * 调用{@link State#getTouchState(byte)}   可得到返回值0x00 既手指已经离开
     *
     */
    public static class State {

        //刷新状态
        public static final byte PRE_REFRESH = 0x00;
        public static final byte REFRESHING = 0x01;
        public static final byte POST_REFRESH = 0x10;

        //位置状态
        public static final byte INITIAL = 0x00;
        public static final byte BEFORE_STABLE_DISTANCE = 0x01;
        public static final byte STABLE_DISTANCE = 0x10;
        public static final byte AFTER_STABLE_DISTANCE = 0x11;

        //手指状态
        public static final byte PRESS = 0x01;
        public static final byte RELEASE = 0x00;

        /**
         * 获取刷新状态
         * <p>
         * 正在刷新{@link State#PRE_REFRESH}
         * 还未刷新{@link State#REFRESHING}
         * 刷新完成{@link State#POST_REFRESH}
         *
         * @param state
         * @return
         */
        public static byte getRefreshState(byte state) {
            return (byte) ((state & 0xff) >> 3);
        }

        /**
         * 获取位置状态
         * <p>
         * 初始位置 {@link State#INITIAL}
         * 稳定线之前位置 {@link State#BEFORE_STABLE_DISTANCE}
         * 稳定线上 {@link State#STABLE_DISTANCE}
         * 越过稳定线 {@link State#AFTER_STABLE_DISTANCE}
         *
         * @param state
         * @return
         */
        public static byte getLocationState(byte state) {
            return (byte) (state >> 1 & 0x3);
        }

        /**
         * 获取手指按压状态
         * <p>
         * 按下状态 {@link State#PRESS}
         * 松开状态 {@link State#RELEASE}
         *
         * @param state
         * @return
         */
        public static byte getTouchState(byte state) {
            return (byte) (state & 0x01);
        }

        /**
         * 将参数 {@param refreshState} 设置到 {@param src} 中
         * @param refreshState
         * @param src
         * @return
         */
        public static byte setRefreshState(byte refreshState, byte src) {
            if (refreshState < PRE_REFRESH || refreshState > POST_REFRESH)
                throw new RuntimeException();
            return  (byte) (src & 0x07 | (refreshState << 3));
        }

        /**
         * 将参数 {@param locationState} 设置到 {@param src} 中
         * @param locationState
         * @param src
         * @return
         */
        public static byte setLocationState(byte locationState, byte src) {
            if (locationState < INITIAL || locationState > AFTER_STABLE_DISTANCE)
                throw new RuntimeException();
            return (byte) (src & 0x19 | (locationState << 1));
        }

        /**
         * 将参数 {@param touchState} 设置到 {@param src} 中
         * @param touchState
         * @param src
         * @return
         */
        public static byte setTouchState(byte touchState, byte src) {
            if (touchState < PRESS || touchState > RELEASE)
                throw new RuntimeException();
            return (byte) (src & 0x01 | touchState);
        }

        /**
         * 通过三个参数直接生成state
         *
         * @param refreshState
         * @param locationState
         * @param touchState
         * @return
         */
        public static byte makeState(byte refreshState, byte locationState, byte touchState) {
            int state = 0x00;
            state = state | refreshState;
            state = state << 2;
            state = state | locationState;
            state = state << 1;
            state = state | touchState;
            return (byte) state;
        }
    }
}
