package com.android.demo;

import android.support.v4.view.PagerAdapter;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Random;

import cn.trinea.android.view.autoscrollviewpager.AutoScrollViewPager;


public class MainActivity extends ActionBarActivity {
    AutoScrollViewPager mViewPager;
    BaseViewPagerAdapter mViewPagerAdapter;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mViewPager = (AutoScrollViewPager) findViewById(R.id.home_banner_viewpager);
        initViewPager();
    }

    private void initViewPager() {
        mViewPagerAdapter = new BaseViewPagerAdapter();
        //显示默认占位图片
        mViewPagerAdapter.add(makeView(R.drawable.ic_launcher));
        mViewPagerAdapter.add(makeView(R.drawable.ic_launcher));
        mViewPagerAdapter.add(makeView(R.drawable.ic_launcher));
        mViewPagerAdapter.add(makeView(R.drawable.ic_launcher));
        mViewPagerAdapter.add(makeView(R.drawable.ic_launcher));

        mViewPager.setAdapter(mViewPagerAdapter);
        mViewPager.setInterval(1000);
        mViewPager.startAutoScroll();
    }
    public View makeView(int rid) {
        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(rid);
        imageView.setId(new Random().nextInt(Integer.MAX_VALUE));
        return imageView;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public class BaseViewPagerAdapter extends PagerAdapter {
        ArrayList<View> views = new ArrayList<View>();

        public BaseViewPagerAdapter() {

        }

        public void add(View view) {
            views.add(view);
        }

        public View getItem(int position) {
            if (position < views.size() && position >= 0) {
                return views.get(position);
            }
            return null;
        }

        public void clear() {
            views.clear();
            notifyDataSetChanged();
        }


        @Override
        public int getCount() {
            return views.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            if (position >= 0 && position < views.size()) {
                View view = views.get(position);

                if (view != null) {
                    if(view.getId()>0){
                        if(container.findViewById(view.getId())==null){
                            container.addView(view);
                        }
                    }else {
                        boolean hasContained = false;
                        for (int i = 0; i < container.getChildCount(); i++) {
                            if(container.getChildAt(i)==view){
                                hasContained = true;
                                break;
                            }
                        }
                        if(!hasContained){
                            container.addView(view);
                        }
                    }
                    return view;
                }
            }
            return null;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            if (position >= 0 && position < views.size()) {
                View view = views.get(position);
                container.removeView(view);
            }
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }
    }

}
