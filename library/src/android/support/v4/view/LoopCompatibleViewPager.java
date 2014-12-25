/*
* Copyright (C) 2011 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package android.support.v4.view;

import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.view.View;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

/**
 * override ViewPager's 'populate' medthod to prevent main thread from blocking
 *  Created by lsjwzh on 14-12-25.
 */
public class LoopCompatibleViewPager extends ViewPager {
    private static final int DRAW_ORDER_DEFAULT = 0;
    private static final ViewPositionComparator sPositionComparator = new ViewPositionComparator();
    InfiniteLoopPagerAdapter mWrapperAdapter;
    PagerAdapter mActualAdapter;
    boolean mIsLoopScroll = true;
    Field mItems_Field;
    private Field mCurItem_Field;
    private Field mPopulatePending_Field;
    private Field mExpectedAdapterCount_Field;
    private Field mDrawingOrder_Field;
    private Field mDrawingOrderedChildren_Field;
    private Field mPageMargin_Field;
    private Field mFirstOffset_Field;
    private Field mLastOffset_Field;
    private Field mNeedCalculatePageOffsets_Field;


    public LoopCompatibleViewPager(Context context) {
        super(context);
        initFields();
    }

    public LoopCompatibleViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        initFields();
    }

    void initFields() {
        try {
            mCurItem_Field = ViewPager.class.getDeclaredField("mCurItem");
            if (mCurItem_Field != null) {
                mCurItem_Field.setAccessible(true);
            }
            mPopulatePending_Field = ViewPager.class.getDeclaredField("mPopulatePending");
            if (mPopulatePending_Field != null) {
                mPopulatePending_Field.setAccessible(true);
            }
            mExpectedAdapterCount_Field = ViewPager.class.getDeclaredField("mExpectedAdapterCount");
            if (mExpectedAdapterCount_Field != null) {
                mExpectedAdapterCount_Field.setAccessible(true);
            }
            mItems_Field = ViewPager.class.getDeclaredField("mItems");
            if (mItems_Field != null) {
                mItems_Field.setAccessible(true);
            }
            mDrawingOrder_Field = ViewPager.class.getDeclaredField("mDrawingOrder");
            if (mDrawingOrder_Field != null) {
                mDrawingOrder_Field.setAccessible(true);
            }
            mDrawingOrderedChildren_Field = ViewPager.class.getDeclaredField("mDrawingOrderedChildren");
            if (mDrawingOrderedChildren_Field != null) {
                mDrawingOrderedChildren_Field.setAccessible(true);
            }
            mPageMargin_Field = ViewPager.class.getDeclaredField("mPageMargin");
            if (mPageMargin_Field != null) {
                mPageMargin_Field.setAccessible(true);
            }
            mFirstOffset_Field = ViewPager.class.getDeclaredField("mFirstOffset");
            if (mFirstOffset_Field != null) {
                mFirstOffset_Field.setAccessible(true);
            }
            mLastOffset_Field = ViewPager.class.getDeclaredField("mLastOffset");

            if (mLastOffset_Field != null) {
                mLastOffset_Field.setAccessible(true);
            }
            mNeedCalculatePageOffsets_Field = ViewPager.class.getDeclaredField("mNeedCalculatePageOffsets");
            if (mNeedCalculatePageOffsets_Field != null) {
                mNeedCalculatePageOffsets_Field.setAccessible(true);
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieve the current adapter supplying pages.
     *
     * @return The currently registered PagerAdapter
     */
    public PagerAdapter getAdapter() {
        return mActualAdapter;
    }

    protected InfiniteLoopPagerAdapter getWrapperAdapter(){
        return mWrapperAdapter;
    }

    /**
     * Set a PagerAdapter that will supply views for this pager as needed.
     *
     * @param adapter Adapter to use
     */
    public void setAdapter(PagerAdapter adapter) {
        if(isLoopScroll()) {
            mActualAdapter = adapter;
            mWrapperAdapter = new InfiniteLoopPagerAdapter(adapter);
            super.setAdapter(mWrapperAdapter);
            setCurrentItem(0);
        }else {
            mActualAdapter = adapter;
            mWrapperAdapter = null;
            super.setAdapter(mActualAdapter);
        }
    }

    /**
     * Set the currently selected page. If the ViewPager has already been through its first
     * layout with its current adapter there will be a smooth animated transition between
     * the current item and the specified item.
     *
     * @param item Item index to select
     */
    public void setCurrentItem(int item) {
        if(getWrapperAdapter()!=null) {
            if (item < getWrapperAdapter().getRealCount()) {
                item = getWrapperAdapter().getCount() / 2 - (getWrapperAdapter().getCount() / 2) % getWrapperAdapter().getRealCount() + item;
            }
        }
        super.setCurrentItem(item);
    }

    /**
     * Set the currently selected page.
     *
     * @param item         Item index to select
     * @param smoothScroll True to smoothly scroll to the new item, false to transition immediately
     */
    public void setCurrentItem(int item, boolean smoothScroll) {
        if(getWrapperAdapter()!=null) {
            if (item < getWrapperAdapter().getRealCount()) {
                item = getWrapperAdapter().getCount() / 2 - (getWrapperAdapter().getCount() / 2) % getWrapperAdapter().getRealCount() + item;
            }
        }
        super.setCurrentItem(item, smoothScroll);
    }

    @Override
    public int getCurrentItem() {
        if(getWrapperAdapter()!=null) {
            return super.getCurrentItem() % getWrapperAdapter().getRealCount();
        }else {
            return super.getCurrentItem();
        }
    }

    public void setLoopScroll(boolean loopScroll){
        if(mIsLoopScroll==loopScroll){
            return;
        }
        mIsLoopScroll = loopScroll;
        if(getWrapperAdapter()!=null) {
            setAdapter(getWrapperAdapter().getOriginalPageAdapter());
        }
    }

    public boolean isLoopScroll(){
        return mIsLoopScroll;
    }

    @Override
    void populate(int newCurrentItem) {
        if(getWrapperAdapter()==null){
            super.populate(newCurrentItem);
            return;
        }
        ItemInfo oldCurInfo = null;
        int focusDirection = View.FOCUS_FORWARD;
        if (getInnerCurrentItem() != newCurrentItem) {
            focusDirection = getInnerCurrentItem() < newCurrentItem ? View.FOCUS_RIGHT : View.FOCUS_LEFT;
            oldCurInfo = infoForPosition(getInnerCurrentItem());
            setInnerCurrentItem(newCurrentItem);
        }

        if (getWrapperAdapter() == null) {
            sortChildDrawingOrder();
            return;
        }

        // Bail now if we are waiting to populate.  This is to hold off
        // on creating views from the time the user releases their finger to
        // fling to a new position until we have finished the scroll to
        // that position, avoiding glitches from happening at that point.
        if (getInnerPopulatePending()) {
            sortChildDrawingOrder();
            return;
        }

        // Also, don't populate until we are attached to a window.  This is to
        // avoid trying to populate before we have restored our view hierarchy
        // state and conflicting with what is restored.
        if (getWindowToken() == null) {
            return;
        }

        getWrapperAdapter().startUpdate(this);

        final int pageLimit = getOffscreenPageLimit();
        final int startPos = Math.max(0, getInnerCurrentItem() - pageLimit);
        final int N = getWrapperAdapter().getCount();
        final int endPos = Math.min(N - 1, getInnerCurrentItem() + pageLimit);

        if (N != getInnerExpectedAdapterCount()) {
            String resName;
            try {
                resName = getResources().getResourceName(getId());
            } catch (Resources.NotFoundException e) {
                resName = Integer.toHexString(getId());
            }
            throw new IllegalStateException("The application's PagerAdapter changed the adapter's" +
                    " contents without calling PagerAdapter#notifyDataSetChanged!" +
                    " Expected adapter item count: " + getInnerExpectedAdapterCount() + ", found: " + N +
                    " Pager id: " + resName +
                    " Pager class: " + getClass() +
                    " Problematic adapter: " + getWrapperAdapter().getClass());
        }

        // Locate the currently focused item or add it if needed.
        int curIndex = -1;
        ItemInfo curItem = null;
        for (curIndex = 0; curIndex < getInnerItems().size(); curIndex++) {
            final ItemInfo ii = getInnerItems().get(curIndex);
            if (ii.position >= getInnerCurrentItem()) {
                if (ii.position == getInnerCurrentItem()) curItem = ii;
                break;
            }
        }

        if (curItem == null && N > 0) {
            curItem = addNewItem(getInnerCurrentItem(), curIndex);
        }

        // Fill 3x the available width or up to the number of offscreen
        // pages requested to either side, whichever is larger.
        // If we have no current item we have no work to do.
        if (curItem != null) {
            float extraWidthLeft = 0.f;
            int itemIndex = curIndex - 1;
            ItemInfo ii = itemIndex >= 0 ? getInnerItems().get(itemIndex) : null;
            final int clientWidth = getClientWidth();
            final float leftWidthNeeded = clientWidth <= 0 ? 0 :
                    2.f - curItem.widthFactor + (float) getPaddingLeft() / (float) clientWidth;
            int checkStart = Math.max(0,getInnerCurrentItem()-getWrapperAdapter().getRealCount());//reduce execute times,when loop viewpager is available
            for (int pos = getInnerCurrentItem() - 1; pos >= checkStart; pos--) {
                if (extraWidthLeft >= leftWidthNeeded && pos < startPos) {
                    if (ii == null) {
                        break;
                    }
                    if (pos == ii.position && !ii.scrolling) {
                        getInnerItems().remove(itemIndex);
                        getWrapperAdapter().destroyItem(this, pos, ii.object);

                        itemIndex--;
                        curIndex--;
                        ii = itemIndex >= 0 ? getInnerItems().get(itemIndex) : null;
                    }
                } else if (ii != null && pos == ii.position) {
                    extraWidthLeft += ii.widthFactor;
                    itemIndex--;
                    ii = itemIndex >= 0 ? getInnerItems().get(itemIndex) : null;
                } else {
                    ii = addNewItem(pos, itemIndex + 1);
                    extraWidthLeft += ii.widthFactor;
                    curIndex++;
                    ii = itemIndex >= 0 ? getInnerItems().get(itemIndex) : null;
                }
            }

            float extraWidthRight = curItem.widthFactor;
            itemIndex = curIndex + 1;
            if (extraWidthRight < 2.f) {
                ii = itemIndex < getInnerItems().size() ? getInnerItems().get(itemIndex) : null;
                final float rightWidthNeeded = clientWidth <= 0 ? 0 :
                        (float) getPaddingRight() / (float) clientWidth + 2.f;
                int checkEnd = Math.min(N,getInnerCurrentItem()+getWrapperAdapter().getRealCount());//reduce execute times,when loop viewpager is available
                for (int pos = getInnerCurrentItem() + 1; pos < checkEnd; pos++) {
                    if (extraWidthRight >= rightWidthNeeded && pos > endPos) {
                        if (ii == null) {
                            break;
                        }
                        if (pos == ii.position && !ii.scrolling) {
                            getInnerItems().remove(itemIndex);
                            getWrapperAdapter().destroyItem(this, pos, ii.object);
                            ii = itemIndex < getInnerItems().size() ? getInnerItems().get(itemIndex) : null;
                        }
                    } else if (ii != null && pos == ii.position) {
                        extraWidthRight += ii.widthFactor;
                        itemIndex++;
                        ii = itemIndex < getInnerItems().size() ? getInnerItems().get(itemIndex) : null;
                    } else {
                        ii = addNewItem(pos, itemIndex);
                        itemIndex++;
                        extraWidthRight += ii.widthFactor;
                        ii = itemIndex < getInnerItems().size() ? getInnerItems().get(itemIndex) : null;
                    }
                }
            }

            calculatePageOffsets(curItem, curIndex, oldCurInfo);
        }


        getWrapperAdapter().setPrimaryItem(this, getInnerCurrentItem(), curItem != null ? curItem.object : null);

        getWrapperAdapter().finishUpdate(this);

        // Check width measurement of current pages and drawing sort order.
        // Update LayoutParams as needed.
        final int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = getChildAt(i);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            lp.childIndex = i;
            if (!lp.isDecor && lp.widthFactor == 0.f) {
                // 0 means requery the adapter for this, it doesn't have a valid width.
                final ItemInfo ii = infoForChild(child);
                if (ii != null) {
                    lp.widthFactor = ii.widthFactor;
                    lp.position = ii.position;
                }
            }
        }
        sortChildDrawingOrder();

        if (hasFocus()) {
            View currentFocused = findFocus();
            ItemInfo ii = currentFocused != null ? infoForAnyChild(currentFocused) : null;
            if (ii == null || ii.position != getInnerCurrentItem()) {
                for (int i=0; i<getChildCount(); i++) {
                    View child = getChildAt(i);
                    ii = infoForChild(child);
                    if (ii != null && ii.position == getInnerCurrentItem()) {
                        if (child.requestFocus(focusDirection)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void sortChildDrawingOrder() {
        if (getInnerDrawingOrder() != DRAW_ORDER_DEFAULT) {
            if (getInnerDrawingOrderedChildren() == null) {
                setInnerDrawingOrderedChildren();
            } else {
                getInnerDrawingOrderedChildren().clear();
            }
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                getInnerDrawingOrderedChildren().add(child);
            }
            Collections.sort(getInnerDrawingOrderedChildren(), sPositionComparator);
        }
    }

    private void calculatePageOffsets(ItemInfo curItem, int curIndex, ItemInfo oldCurInfo) {
        final int N = getWrapperAdapter().getCount();
        final int width = getClientWidth();
        final float marginOffset = width > 0 ? (float) getInnerPageMargin() / width : 0;
        // Fix up offsets for later layout.
        if (oldCurInfo != null) {
            final int oldCurPosition = oldCurInfo.position;
            // Base offsets off of oldCurInfo.
            if (oldCurPosition < curItem.position) {
                int itemIndex = 0;
                ItemInfo ii = null;
                float offset = oldCurInfo.offset + oldCurInfo.widthFactor + marginOffset;
                for (int pos = oldCurPosition + 1;
                     pos <= curItem.position && itemIndex < getInnerItems().size(); pos++) {
                    ii = getInnerItems().get(itemIndex);
                    while (pos > ii.position && itemIndex < getInnerItems().size() - 1) {
                        itemIndex++;
                        ii = getInnerItems().get(itemIndex);
                    }
                    while (pos < ii.position) {
                        // We don't have an item populated for this,
                        // ask the adapter for an offset.
                        offset += getWrapperAdapter().getPageWidth(pos) + marginOffset;
                        pos++;
                    }
                    ii.offset = offset;
                    offset += ii.widthFactor + marginOffset;
                }
            } else if (oldCurPosition > curItem.position) {
                int itemIndex = getInnerItems().size() - 1;
                ItemInfo ii = null;
                float offset = oldCurInfo.offset;
                for (int pos = oldCurPosition - 1;
                     pos >= curItem.position && itemIndex >= 0; pos--) {
                    ii = getInnerItems().get(itemIndex);
                    while (pos < ii.position && itemIndex > 0) {
                        itemIndex--;
                        ii = getInnerItems().get(itemIndex);
                    }
                    while (pos > ii.position) {
                        // We don't have an item populated for this,
                        // ask the adapter for an offset.
                        offset -= getWrapperAdapter().getPageWidth(pos) + marginOffset;
                        pos--;
                    }
                    offset -= ii.widthFactor + marginOffset;
                    ii.offset = offset;
                }
            }
        }

        // Base all offsets off of curItem.
        final int itemCount = getInnerItems().size();
        float offset = curItem.offset;
        int pos = curItem.position - 1;
        try {
            mFirstOffset_Field.set(this, curItem.position == 0 ? curItem.offset : -Float.MAX_VALUE);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        try {
            mLastOffset_Field.set(this, curItem.position == N - 1 ?
                    curItem.offset + curItem.widthFactor - 1 : Float.MAX_VALUE);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        // Previous pages
        for (int i = curIndex - 1; i >= 0; i--, pos--) {
            final ItemInfo ii = getInnerItems().get(i);
            while (pos > ii.position) {
                offset -= getWrapperAdapter().getPageWidth(pos--) + marginOffset;
            }
            offset -= ii.widthFactor + marginOffset;
            ii.offset = offset;
            if (ii.position == 0) {
                try {
                    mFirstOffset_Field.set(this, offset);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        offset = curItem.offset + curItem.widthFactor + marginOffset;
        pos = curItem.position + 1;
        // Next pages
        for (int i = curIndex + 1; i < itemCount; i++, pos++) {
            final ItemInfo ii = getInnerItems().get(i);
            while (pos < ii.position) {
                offset += getWrapperAdapter().getPageWidth(pos++) + marginOffset;
            }
            if (ii.position == N - 1) {
                try {
                    mLastOffset_Field.set(this, offset + ii.widthFactor - 1);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            ii.offset = offset;
            offset += ii.widthFactor + marginOffset;
        }

        try {
            mNeedCalculatePageOffsets_Field.set(this, false);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public int getInnerCurrentItem() {
        return super.getCurrentItem();
    }

    private void setInnerCurrentItem(int newCurrentItem) {
        try {
            mCurItem_Field.set(this, newCurrentItem);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getInnerExpectedAdapterCount() {
        try {
            return mExpectedAdapterCount_Field.getInt(this);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private boolean getInnerPopulatePending() {
        try {
            return mPopulatePending_Field.getBoolean(this);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    private void setInnerDrawingOrderedChildren() {
        try {
            mDrawingOrderedChildren_Field.set(this, new ArrayList<View>());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<View> getInnerDrawingOrderedChildren() {
        try {
            return (ArrayList<View>) mDrawingOrderedChildren_Field.get(this);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<View>();
        }
    }

    private int getInnerDrawingOrder() {
        try {
            return mDrawingOrder_Field.getInt(this);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    private float getInnerLastOffset() {
        try {
            return mLastOffset_Field.getFloat(this);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private float getInnerFirstOffset() {
        try {
            return mFirstOffset_Field.getFloat(this);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int getInnerPageMargin() {
        try {
            return mPageMargin_Field.getInt(this);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private ArrayList<ItemInfo> getInnerItems() {
        try {
            return (ArrayList<ItemInfo>) mItems_Field.get(this);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<ItemInfo>();
        }
    }


    private int getClientWidth() {
        return getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
    }
}
