package com.congxiaoyao.pullrefresh.overscroll.test;

import android.graphics.Rect;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageView;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.example.admin.myapplication.R;
import com.example.admin.myapplication.overscroll.PullRefreshLayout;

import java.util.ArrayList;
import java.util.List;

public class RefreshTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_refresh_test);

        final PullRefreshLayout refreshLayout = (PullRefreshLayout) findViewById(R.id.pull_refresh_layout);
        refreshLayout.setLoadingViewAdapter(new PullRefreshLayout.LoadingViewAdapter() {

            private Interpolator interpolator = new DecelerateInterpolator();

            @Override
            public void onRequestLoadingViewBounds(Rect bounds, int topOffset, boolean isRefreshing) {
                bounds.set(getParent().getWidth() / 2 - topOffset / 2, 0, getParent().getWidth() / 2 + topOffset / 2, topOffset);
            }

            @Override
            public int getSteadyStateDistance() {
                return 250;
            }

            @Override
            public int getTopOffsetByTotalOffset(int totalTouchY) {
                if (totalTouchY > 1500) {
                    totalTouchY = 1500;
                }
                return (int) (interpolator.getInterpolation(totalTouchY / 1500.0f) * 500.0f);
            }

            @Override
            public int getTotalTouchYByTopOffset(int topOffset) {
                return (int) (1500 * (1 - Math.sqrt(1 - topOffset / 500.0f)));
            }
        });

        ImageView imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                refreshLayout.setRefreshing(false);
            }
        });

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setAdapter(new BaseQuickAdapter<Integer,BaseViewHolder>(R.layout.item_board) {
            @Override
            protected void convert(BaseViewHolder helper, Integer item) {
                helper.setText(R.id.tv_board, item + "");
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < 25; i++) list.add(100 + i);

        ((BaseQuickAdapter<Integer, BaseViewHolder>) recyclerView.getAdapter()).addData(list);
    }
}
