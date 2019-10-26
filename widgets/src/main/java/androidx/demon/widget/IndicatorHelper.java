package androidx.demon.widget;

import android.database.Cursor;
import android.database.DataSetObserver;

import java.lang.ref.WeakReference;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

/**
 * @Author create by Zoran on 2019-10-26
 * @Email : 171905184@qq.com
 * @Description :
 */
public final class IndicatorHelper {

	private final WeakReference<IndicatorView> mWeakReference;

	private ViewPager mViewPager;
	private PagerAdapter mPagerAdapter;
	private IndicatorDataSetObserver mIndicatorDataSetObserver;
	private IndicatorOnPageChangeListener mOnPageChangeListener;
	private IndicatorOnAdapterChangeListener mOnAdapterChangeListener;

	public static IndicatorHelper obtain(@NonNull IndicatorView indicatorView) {
		return new IndicatorHelper(indicatorView);
	}

	private IndicatorHelper(@NonNull IndicatorView indicatorView) {
		this.mWeakReference = new WeakReference<>(indicatorView);
	}

	public void setupViewPager(@Nullable ViewPager viewPager) {
		if (this.mViewPager != null) {
			this.mViewPager.removeOnAdapterChangeListener(this.mOnAdapterChangeListener);
			this.mViewPager.removeOnPageChangeListener(this.mOnPageChangeListener);
			this.mViewPager = null;
		}
		if (viewPager == null) {
			this.setPagerAdapter(null);
		} else {
			if (this.mOnPageChangeListener == null) {
				this.mOnPageChangeListener = new IndicatorOnPageChangeListener();
			}
			if (this.mOnAdapterChangeListener == null) {
				this.mOnAdapterChangeListener = new IndicatorOnAdapterChangeListener();
			}
			this.mViewPager = viewPager;
			this.mViewPager.addOnPageChangeListener(this.mOnPageChangeListener);
			this.mViewPager.addOnAdapterChangeListener(this.mOnAdapterChangeListener);
			this.setPagerAdapter(this.mViewPager.getAdapter());
		}
	}

	private void setPagerAdapter(@Nullable PagerAdapter adapter) {
		if (this.mPagerAdapter != null) {
			this.mPagerAdapter.unregisterDataSetObserver(this.mIndicatorDataSetObserver);
		}
		this.mPagerAdapter = adapter;
		if (this.mPagerAdapter != null) {
			if (this.mIndicatorDataSetObserver == null) {
				this.mIndicatorDataSetObserver = new IndicatorDataSetObserver();
			}
			this.mPagerAdapter.registerDataSetObserver(this.mIndicatorDataSetObserver);
		}
		this.populateFromAdapter();
	}

	private void populateFromAdapter() {

	}

	private final class IndicatorOnAdapterChangeListener implements ViewPager.OnAdapterChangeListener {

		/**
		 * Called when the adapter for the given view pager has changed.
		 *
		 * @param viewPager  ViewPager where the adapter change has happened
		 * @param oldAdapter the previously set adapter
		 * @param newAdapter the newly set adapter
		 */
		@Override
		public void onAdapterChanged(@NonNull ViewPager viewPager, @Nullable PagerAdapter oldAdapter, @Nullable PagerAdapter newAdapter) {
			IndicatorHelper.this.setPagerAdapter(newAdapter);
		}
	}

	private final class IndicatorOnPageChangeListener implements ViewPager.OnPageChangeListener {

		/**
		 * This method will be invoked when the current page is scrolled, either as part
		 * of a programmatically initiated smooth scroll or a user initiated touch scroll.
		 *
		 * @param position             Position index of the first page currently being displayed.
		 *                             Page position+1 will be visible if positionOffset is nonzero.
		 * @param positionOffset       Value from [0, 1) indicating the offset from the page at position.
		 * @param positionOffsetPixels Value in pixels indicating the offset from position.
		 */
		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
			IndicatorHelper.this.mWeakReference.get().setScrollPosition(position, positionOffset, positionOffsetPixels);
		}

		/**
		 * This method will be invoked when a new page becomes selected. Animation is not
		 * necessarily complete.
		 *
		 * @param position Position index of the new selected page.
		 */
		@Override
		public void onPageSelected(int position) {
			IndicatorHelper.this.mWeakReference.get().setCurrentItem(position);
		}

		/**
		 * Called when the scroll state changes. Useful for discovering when the user
		 * begins dragging, when the pager is automatically settling to the current page,
		 * or when it is fully stopped/idle.
		 *
		 * @param state The new scroll state.
		 * @see ViewPagerCompat#SCROLL_STATE_IDLE
		 * @see ViewPagerCompat#SCROLL_STATE_DRAGGING
		 * @see ViewPagerCompat#SCROLL_STATE_SETTLING
		 */
		@Override
		public void onPageScrollStateChanged(int state) {

		}
	}

	private final class IndicatorDataSetObserver extends DataSetObserver {

		/**
		 * This method is called when the entire data set has changed,
		 * most likely through a call to {@link Cursor#requery()} on a {@link Cursor}.
		 */
		@Override
		public void onChanged() {
			IndicatorHelper.this.populateFromAdapter();
		}

		/**
		 * This method is called when the entire data becomes invalid,
		 * most likely through a call to {@link Cursor#deactivate()} or {@link Cursor#close()} on a
		 * {@link Cursor}.
		 */
		@Override
		public void onInvalidated() {
			IndicatorHelper.this.populateFromAdapter();
		}
	}
}
