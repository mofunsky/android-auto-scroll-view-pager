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
import java.util.Comparator;

/**
 * override ViewPager's 'populate' medthod to prevent main thread from blocking
 *  Created by lsjwzh on 14-12-25.
 */
public class LoopCompatibleViewPager extends ViewPager {
    private static final int DRAW_ORDER_DEFAULT = 0;
    private static final ViewPositionComparator sPositionComparator = new ViewPositionComparator();
    InfiniteLoopPagerAdapter mWrapperAdapter;
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
        return mWrapperAdapter;
    }

    /**
     * Set a PagerAdapter that will supply views for this pager as needed.
     *
     * @param adapter Adapter to use
     */
    public void setAdapter(PagerAdapter adapter) {
        mWrapperAdapter = new InfiniteLoopPagerAdapter(adapter);
        super.setAdapter(mWrapperAdapter);
    }

    /**
     * Set the currently selected page. If the ViewPager has already been through its first
     * layout with its current adapter there will be a smooth animated transition between
     * the current item and the specified item.
     *
     * @param item Item index to select
     */
    public void setCurrentItem(int item) {
        if (item < mWrapperAdapter.getRealCount()) {
            item = mWrapperAdapter.getCount() / 2 - (mWrapperAdapter.getCount() / 2) % mWrapperAdapter.getRealCount() + item;
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
        if (item < mWrapperAdapter.getRealCount()) {
            item = mWrapperAdapter.getCount() / 2 - (mWrapperAdapter.getCount() / 2) % mWrapperAdapter.getRealCount() + item;
        }
        super.setCurrentItem(item, smoothScroll);
    }

    @Override
    void populate(int newCurrentItem) {
        ItemInfo oldCurInfo = null;
        int focusDirection = View.FOCUS_FORWARD;
        if (getCurrentItem() != newCurrentItem) {
            focusDirection = getCurrentItem() < newCurrentItem ? View.FOCUS_RIGHT : View.FOCUS_LEFT;
            oldCurInfo = infoForPosition(getCurrentItem());
            setmCurrentItem(newCurrentItem);
        }

        if (mWrapperAdapter == null) {
            sortChildDrawingOrder();
            return;
        }

        // Bail now if we are waiting to populate.  This is to hold off
        // on creating views from the time the user releases their finger to
        // fling to a new position until we have finished the scroll to
        // that position, avoiding glitches from happening at that point.
        if (getmPopulatePending()) {
            sortChildDrawingOrder();
            return;
        }

        // Also, don't populate until we are attached to a window.  This is to
        // avoid trying to populate before we have restored our view hierarchy
        // state and conflicting with what is restored.
        if (getWindowToken() == null) {
            return;
        }

        mWrapperAdapter.startUpdate(this);

        final int pageLimit = getOffscreenPageLimit();
        final int startPos = Math.max(0, getCurrentItem() - pageLimit);
        final int N = mWrapperAdapter.getCount();
        final int endPos = Math.min(N - 1, getCurrentItem() + pageLimit);

        if (N != getmExpectedAdapterCount()) {
            String resName;
            try {
                resName = getResources().getResourceName(getId());
            } catch (Resources.NotFoundException e) {
                resName = Integer.toHexString(getId());
            }
            throw new IllegalStateException("The application's PagerAdapter changed the adapter's" +
                    " contents without calling PagerAdapter#notifyDataSetChanged!" +
                    " Expected adapter item count: " + getmExpectedAdapterCount() + ", found: " + N +
                    " Pager id: " + resName +
                    " Pager class: " + getClass() +
                    " Problematic adapter: " + mWrapperAdapter.getClass());
        }

        // Locate the currently focused item or add it if needed.
        int curIndex = -1;
        ItemInfo curItem = null;
        for (curIndex = 0; curIndex < getmItems().size(); curIndex++) {
            final ItemInfo ii = getmItems().get(curIndex);
            if (ii.position >= getCurrentItem()) {
                if (ii.position == getCurrentItem()) curItem = ii;
                break;
            }
        }

        if (curItem == null && N > 0) {
            curItem = addNewItem(getCurrentItem(), curIndex);
        }

        // Fill 3x the available width or up to the number of offscreen
        // pages requested to either side, whichever is larger.
        // If we have no current item we have no work to do.
        if (curItem != null) {
            float extraWidthLeft = 0.f;
            int itemIndex = curIndex - 1;
            ItemInfo ii = itemIndex >= 0 ? getmItems().get(itemIndex) : null;
            final int clientWidth = getClientWidth();
            final float leftWidthNeeded = clientWidth <= 0 ? 0 :
                    2.f - curItem.widthFactor + (float) getPaddingLeft() / (float) clientWidth;
            int checkStart = Math.max(0,getCurrentItem()-getOffscreenPageLimit());//reduce execute times,when loop viewpager is available
            for (int pos = getCurrentItem() - 1; pos >= checkStart; pos--) {
                if (extraWidthLeft >= leftWidthNeeded && pos < startPos) {
                    if (ii == null) {
                        break;
                    }
                    if (pos == ii.position && !ii.scrolling) {
                        getmItems().remove(itemIndex);
                        mWrapperAdapter.destroyItem(this, pos, ii.object);

                        itemIndex--;
                        curIndex--;
                        ii = itemIndex >= 0 ? getmItems().get(itemIndex) : null;
                    }
                } else if (ii != null && pos == ii.position) {
                    extraWidthLeft += ii.widthFactor;
                    itemIndex--;
                    ii = itemIndex >= 0 ? getmItems().get(itemIndex) : null;
                } else {
                    ii = addNewItem(pos, itemIndex + 1);
                    extraWidthLeft += ii.widthFactor;
                    curIndex++;
                    ii = itemIndex >= 0 ? getmItems().get(itemIndex) : null;
                }
            }

            float extraWidthRight = curItem.widthFactor;
            itemIndex = curIndex + 1;
            if (extraWidthRight < 2.f) {
                ii = itemIndex < getmItems().size() ? getmItems().get(itemIndex) : null;
                final float rightWidthNeeded = clientWidth <= 0 ? 0 :
                        (float) getPaddingRight() / (float) clientWidth + 2.f;
                int checkEnd = Math.min(N,getCurrentItem()+getOffscreenPageLimit());//reduce execute times,when loop viewpager is available
                for (int pos = getCurrentItem() + 1; pos < checkEnd; pos++) {
                    if (extraWidthRight >= rightWidthNeeded && pos > endPos) {
                        if (ii == null) {
                            break;
                        }
                        if (pos == ii.position && !ii.scrolling) {
                            getmItems().remove(itemIndex);
                            mWrapperAdapter.destroyItem(this, pos, ii.object);
                            ii = itemIndex < getmItems().size() ? getmItems().get(itemIndex) : null;
                        }
                    } else if (ii != null && pos == ii.position) {
                        extraWidthRight += ii.widthFactor;
                        itemIndex++;
                        ii = itemIndex < getmItems().size() ? getmItems().get(itemIndex) : null;
                    } else {
                        ii = addNewItem(pos, itemIndex);
                        itemIndex++;
                        extraWidthRight += ii.widthFactor;
                        ii = itemIndex < getmItems().size() ? getmItems().get(itemIndex) : null;
                    }
                }
            }

            calculatePageOffsets(curItem, curIndex, oldCurInfo);
        }


        mWrapperAdapter.setPrimaryItem(this, getCurrentItem(), curItem != null ? curItem.object : null);

        mWrapperAdapter.finishUpdate(this);

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
            if (ii == null || ii.position != getCurrentItem()) {
                for (int i=0; i<getChildCount(); i++) {
                    View child = getChildAt(i);
                    ii = infoForChild(child);
                    if (ii != null && ii.position == getCurrentItem()) {
                        if (child.requestFocus(focusDirection)) {
                            break;
                        }
                    }
                }
            }
        }
    }

    private void setmCurrentItem(int newCurrentItem) {
        try {
            mCurItem_Field.set(this, newCurrentItem);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getmExpectedAdapterCount() {
        try {
            return mExpectedAdapterCount_Field.getInt(this);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private boolean getmPopulatePending() {
        try {
            return mPopulatePending_Field.getBoolean(this);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void sortChildDrawingOrder() {
        if (getmDrawingOrder() != DRAW_ORDER_DEFAULT) {
            if (getmDrawingOrderedChildren() == null) {
                setmDrawingOrderedChildren();
            } else {
                getmDrawingOrderedChildren().clear();
            }
            final int childCount = getChildCount();
            for (int i = 0; i < childCount; i++) {
                final View child = getChildAt(i);
                getmDrawingOrderedChildren().add(child);
            }
            Collections.sort(getmDrawingOrderedChildren(), sPositionComparator);
        }
    }

    private void setmDrawingOrderedChildren() {
        try {
            mDrawingOrderedChildren_Field.set(this, new ArrayList<View>());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private ArrayList<View> getmDrawingOrderedChildren() {
        try {
            return (ArrayList<View>) mDrawingOrderedChildren_Field.get(this);
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<View>();
        }
    }

    private int getmDrawingOrder() {
        try {
            return mDrawingOrder_Field.getInt(this);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void calculatePageOffsets(ItemInfo curItem, int curIndex, ItemInfo oldCurInfo) {
        final int N = mWrapperAdapter.getCount();
        final int width = getClientWidth();
        final float marginOffset = width > 0 ? (float) getmPageMargin() / width : 0;
        // Fix up offsets for later layout.
        if (oldCurInfo != null) {
            final int oldCurPosition = oldCurInfo.position;
            // Base offsets off of oldCurInfo.
            if (oldCurPosition < curItem.position) {
                int itemIndex = 0;
                ItemInfo ii = null;
                float offset = oldCurInfo.offset + oldCurInfo.widthFactor + marginOffset;
                for (int pos = oldCurPosition + 1;
                     pos <= curItem.position && itemIndex < getmItems().size(); pos++) {
                    ii = getmItems().get(itemIndex);
                    while (pos > ii.position && itemIndex < getmItems().size() - 1) {
                        itemIndex++;
                        ii = getmItems().get(itemIndex);
                    }
                    while (pos < ii.position) {
                        // We don't have an item populated for this,
                        // ask the adapter for an offset.
                        offset += mWrapperAdapter.getPageWidth(pos) + marginOffset;
                        pos++;
                    }
                    ii.offset = offset;
                    offset += ii.widthFactor + marginOffset;
                }
            } else if (oldCurPosition > curItem.position) {
                int itemIndex = getmItems().size() - 1;
                ItemInfo ii = null;
                float offset = oldCurInfo.offset;
                for (int pos = oldCurPosition - 1;
                     pos >= curItem.position && itemIndex >= 0; pos--) {
                    ii = getmItems().get(itemIndex);
                    while (pos < ii.position && itemIndex > 0) {
                        itemIndex--;
                        ii = getmItems().get(itemIndex);
                    }
                    while (pos > ii.position) {
                        // We don't have an item populated for this,
                        // ask the adapter for an offset.
                        offset -= mWrapperAdapter.getPageWidth(pos) + marginOffset;
                        pos--;
                    }
                    offset -= ii.widthFactor + marginOffset;
                    ii.offset = offset;
                }
            }
        }

        // Base all offsets off of curItem.
        final int itemCount = getmItems().size();
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
            final ItemInfo ii = getmItems().get(i);
            while (pos > ii.position) {
                offset -= mWrapperAdapter.getPageWidth(pos--) + marginOffset;
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
            final ItemInfo ii = getmItems().get(i);
            while (pos < ii.position) {
                offset += mWrapperAdapter.getPageWidth(pos++) + marginOffset;
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


    private float getmLastOffset() {
        try {
            return mLastOffset_Field.getFloat(this);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private float getmFirstOffset() {
        try {
            return mFirstOffset_Field.getFloat(this);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private int getmPageMargin() {
        try {
            return mPageMargin_Field.getInt(this);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private ArrayList<ItemInfo> getmItems() {
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
