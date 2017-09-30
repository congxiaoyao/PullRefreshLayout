package com.congxiaoyao.pullrefresh.overscroll;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.example.admin.myapplication.R;

import java.util.ArrayList;
import java.util.List;

public class FlingTestActivity extends AppCompatActivity {

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fling_test);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        Button button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                long millis = SystemClock.uptimeMillis();
                MotionEvent down = MotionEvent.obtain(millis, millis,
                        MotionEvent.ACTION_DOWN, 0, 50, 0);
                MotionEvent move = MotionEvent.obtain(millis + 10, millis + 10,
                        MotionEvent.ACTION_MOVE, 0, 0, 0);
                MotionEvent up = MotionEvent.obtain(millis + 10, millis + 10,
                        MotionEvent.ACTION_UP, 0, 0, 0);
                recyclerView.dispatchTouchEvent(down);
                recyclerView.dispatchTouchEvent(move);
                recyclerView.dispatchTouchEvent(up);
                int touchSlop = ViewConfiguration.get(FlingTestActivity.this).getScaledTouchSlop();

            }
        });
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
