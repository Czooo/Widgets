package androidx.demon.widget;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @Author create by Zoran on 2019-10-18
 * @Email : 171905184@qq.com
 * @Description :
 */
@ViewPagerCompat.DecorView
public class TabLayoutCompat extends TabLayout {

	private TabViewProvider mTabViewProvider;
	private ViewPagerCompat mViewPagerCompat;
	private ViewPagerCompat.Adapter mViewPagerAdapter;
	private ViewPagerAdapterObserver mAdapterDataObserver;
	private ViewPagerOnTabSelectedListener mOnTabSelectedListener;
	private TabLayoutOnPageChangeListener mOnPageChangeListener;
	private TabLayoutOnAdapterChangeListener mOnAdapterChangeListener;

	public TabLayoutCompat(Context context) {
		this(context, null);
	}

	public TabLayoutCompat(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TabLayoutCompat(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	public void setupWithViewPagerCompat(@Nullable ViewPagerCompat viewPager) {
		this.setupWithViewPagerCompat(viewPager, true);
	}

	public void setupWithViewPagerCompat(@Nullable ViewPagerCompat viewPager, boolean autoRefresh) {
		if (this.mViewPagerCompat != null) {
			this.mViewPagerCompat.removeOnAdapterChangeListener(this.mOnAdapterChangeListener);
			this.mViewPagerCompat.removeOnPageChangeListener(this.mOnPageChangeListener);
		}
		if (viewPager != null) {
			this.mViewPagerCompat = viewPager;
			if (this.mOnPageChangeListener == null) {
				this.mOnPageChangeListener = new TabLayoutOnPageChangeListener();
			}
			this.mOnPageChangeListener.reset();
			this.mViewPagerCompat.addOnPageChangeListener(this.mOnPageChangeListener);

			if (this.mOnAdapterChangeListener == null) {
				this.mOnAdapterChangeListener = new TabLayoutOnAdapterChangeListener();
			}
			this.mOnAdapterChangeListener.setAutoRefresh(autoRefresh);
			this.mViewPagerCompat.addOnAdapterChangeListener(this.mOnAdapterChangeListener);

			if (this.mOnTabSelectedListener == null) {
				this.mOnTabSelectedListener = new ViewPagerOnTabSelectedListener();
			}
			this.addOnTabSelectedListener(this.mOnTabSelectedListener);
			this.setPagerAdapter(this.mViewPagerCompat.getAdapter(), autoRefresh);
		} else {
			this.setPagerAdapter(null, false);
		}
	}

	public void setTabViewProvider(@Nullable TabViewProvider tabViewProvider) {
		this.mTabViewProvider = tabViewProvider;
		this.populateFromPagerAdapter();
	}

	private void setPagerAdapter(@Nullable ViewPagerCompat.Adapter adapter, boolean autoRefresh) {
		if (this.mViewPagerAdapter != null && this.mAdapterDataObserver != null) {
			this.mViewPagerAdapter.unregisterDataSetObserver(this.mAdapterDataObserver);
		}
		this.mViewPagerAdapter = adapter;

		if (this.mViewPagerAdapter != null && autoRefresh) {
			if (this.mAdapterDataObserver == null) {
				this.mAdapterDataObserver = new ViewPagerAdapterObserver();
			}
			this.mViewPagerAdapter.registerDataSetObserver(this.mAdapterDataObserver);
		}
		this.populateFromPagerAdapter();
	}

	private void populateFromPagerAdapter() {
		this.removeAllTabs();
		if (this.mViewPagerAdapter != null) {
			final int itemCount = this.mViewPagerAdapter.getItemCount();

			int currentItem = -1;
			if (this.mViewPagerCompat != null && itemCount > 0) {
				currentItem = this.mViewPagerCompat.getCurrentItem();
			}

			if (this.mTabViewProvider == null) {
				for (int index = 0; index < itemCount; ++index) {
					this.addTab(this.newTab().setText(this.mViewPagerAdapter.getPageTitle(index)), false);
				}
			} else {
				for (int index = 0; index < itemCount; ++index) {
					this.addTab(this.newTab().setCustomView(this.mTabViewProvider.getTabView(LayoutInflater.from(this.getContext()),
							this, index)), false);
				}
			}
			if (currentItem != -1 && currentItem < this.getTabCount()) {
				final Tab layoutTab = this.getTabAt(currentItem);
				if (layoutTab != null) {
					layoutTab.select();
				}
			}
		}
	}

	private final class TabLayoutOnAdapterChangeListener implements ViewPagerCompat.OnAdapterChangeListener {

		private boolean autoRefresh;

		/**
		 * Called when the adapter for the given view pager has changed.
		 *
		 * @param container  ViewPagerCompat where the adapter change has happened
		 * @param oldAdapter the previously set adapter
		 * @param newAdapter the newly set adapter
		 */
		@Override
		public void onAdapterChanged(@NonNull ViewPagerCompat container, @Nullable ViewPagerCompat.Adapter oldAdapter, @Nullable ViewPagerCompat.Adapter newAdapter) {
			TabLayoutCompat.this.setPagerAdapter(newAdapter, this.autoRefresh);
		}

		void setAutoRefresh(boolean autoRefresh) {
			this.autoRefresh = autoRefresh;
		}
	}

	private final class TabLayoutOnPageChangeListener implements ViewPagerCompat.OnPageChangeListener {

		private int previousScrollState;
		private int scrollState;

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
			final boolean updateSelectedText = this.scrollState != 2 || this.previousScrollState == 1;
			TabLayoutCompat.this.setScrollPosition(position, positionOffset, updateSelectedText);
		}

		/**
		 * This method will be invoked when a new page becomes selected. Animation is not
		 * necessarily complete.
		 *
		 * @param position Position index of the new selected page.
		 */
		@Override
		public void onPageSelected(int position) {
			TabLayoutCompat indicatorView = TabLayoutCompat.this;
			if (indicatorView.getSelectedTabPosition() != position && position < indicatorView.getTabCount()) {
				final Tab layoutTab = indicatorView.getTabAt(position);
				if (layoutTab != null) {
					layoutTab.select();
				}
			}
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
			this.previousScrollState = this.scrollState;
			this.scrollState = state;
		}

		void reset() {
			this.previousScrollState = this.scrollState = 0;
		}
	}

	private final class ViewPagerAdapterObserver extends DataSetObserver {

		/**
		 * This method is called when the entire data set has changed,
		 * most likely through a call to {@link Cursor#requery()} on a {@link Cursor}.
		 */
		@Override
		public void onChanged() {
			TabLayoutCompat.this.populateFromPagerAdapter();
		}

		/**
		 * This method is called when the entire data becomes invalid,
		 * most likely through a call to {@link Cursor#deactivate()} or {@link Cursor#close()} on a
		 * {@link Cursor}.
		 */
		@Override
		public void onInvalidated() {
			TabLayoutCompat.this.populateFromPagerAdapter();
		}
	}

	private final class ViewPagerOnTabSelectedListener implements OnTabSelectedListener {

		@Override
		public void onTabSelected(Tab tab) {
			if (TabLayoutCompat.this.mViewPagerCompat != null) {
				TabLayoutCompat.this.mViewPagerCompat.setCurrentItem(tab.getPosition());
			}
		}

		@Override
		public void onTabUnselected(Tab tab) {

		}

		@Override
		public void onTabReselected(Tab tab) {

		}
	}

	public interface TabViewProvider {

		@NonNull
		View getTabView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, int position);
	}
}
