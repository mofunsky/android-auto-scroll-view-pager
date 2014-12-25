package android.support.v4.view;

import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by lsjwzh on 13-8-12.
 */
public class InfiniteLoopPagerAdapter extends PagerAdapter {
    private static final String TAG = "InfiniteLoopPagerAdapter";
    private static final boolean DEBUG = false;

    private PagerAdapter mAdapter;

    public InfiniteLoopPagerAdapter(PagerAdapter mAdapter) {
        this.mAdapter = mAdapter;
    }

    @Override
    public int getCount() {
        // warning: scrolling to very high values (1,000,000+) results in
        // strange drawing behaviour
        return Integer.MAX_VALUE;
    }

    /**
     * @return the {@link #getCount()} result of the wrapped mAdapter
     */
    public int getRealCount() {
        return mAdapter.getCount();
    }

    public PagerAdapter getOriginalPageAdapter(){
        return mAdapter;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        int virtualPosition = position % getRealCount();
        debug("instantiateItem: real position: " + position);
        debug("instantiateItem: virtual position: " + virtualPosition);

        // only expose virtual position to the inner mAdapter
        return mAdapter.instantiateItem(container, virtualPosition);
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        int virtualPosition = position % getRealCount();
        debug("destroyItem: real position: " + position);
        debug("destroyItem: virtual position: " + virtualPosition);

        // only expose virtual position to the inner mAdapter
        mAdapter.destroyItem(container, virtualPosition, object);
    }

    /*
     * Delegate rest of methods directly to the inner mAdapter.
     */

    @Override
    public void finishUpdate(ViewGroup container) {
        mAdapter.finishUpdate(container);
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return mAdapter.isViewFromObject(view, object);
    }

    @Override
    public void restoreState(Parcelable bundle, ClassLoader classLoader) {
        mAdapter.restoreState(bundle, classLoader);
    }

    @Override
    public Parcelable saveState() {
        return mAdapter.saveState();
    }

    @Override
    public void startUpdate(ViewGroup container) {
        mAdapter.startUpdate(container);
    }

    /*
     * End delegation
     */

    private void debug(String message) {
        if (DEBUG) {
            Log.d(TAG, message);
        }
    }
}
