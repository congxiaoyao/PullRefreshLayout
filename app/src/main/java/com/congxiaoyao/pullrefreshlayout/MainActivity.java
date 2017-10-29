package com.congxiaoyao.pullrefreshlayout;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationItemView;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.view.menu.MenuItemImpl;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final BottomNavigationView navigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation_view);
        navigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                return true;
            }
        });
        try {
            addRedDot(MainActivity.this, navigationView, 2);
            setShiftingMode(navigationView, false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addRedDot(Context context, BottomNavigationView navigationView, int index) throws NoSuchFieldException, IllegalAccessException {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) navigationView.getChildAt(0);
        if (index >= menuView.getChildCount()) return;
        View view = new View(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(32, 32);
        params.gravity = Gravity.TOP | Gravity.RIGHT;
        params.topMargin = 8;
        params.rightMargin = 8;
        ((ViewGroup) menuView.getChildAt(index)).addView(view, params);
        view.setBackgroundColor(Color.BLACK);
    }

    public void setShiftingMode(BottomNavigationView navigationView, boolean shift) throws NoSuchFieldException, IllegalAccessException {
        BottomNavigationMenuView menuView = (BottomNavigationMenuView) navigationView.getChildAt(0);
        Field field = menuView.getClass().getDeclaredField("mShiftingMode");
        field.setAccessible(true);
        field.set(menuView, false);
        int count = menuView.getChildCount();
        for (int i = 0; i < count; i++) {
            BottomNavigationItemView itemView = (BottomNavigationItemView) menuView.getChildAt(i);
            itemView.setShiftingMode(shift);
            itemView.setChecked(itemView.getItemData().isChecked());
        }
    }

}
