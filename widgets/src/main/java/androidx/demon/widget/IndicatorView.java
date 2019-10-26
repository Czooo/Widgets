package androidx.demon.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;

/**
 * @Author create by Zoran on 2019-10-18
 * @Email : 171905184@qq.com
 * @Description :
 */
public class IndicatorView extends View {

	private Drawable mSelectedIndicatorDrawable;
	private Drawable mUnSelectedIndicatorDrawable;

	@ColorInt
	private int mSelectedIndicatorColor = -1;
	@ColorInt
	private int mUnSelectedIndicatorColor = -1;

	@FloatRange(from = 0.1F, to = 1.F)
	private float mSelectedIndicatorAlpha = 1.F;
	@FloatRange(from = 0.1F, to = 1.F)
	private float mUnSelectedIndicatorAlpha = 1.F;

	// 选中下标
	private int mCurrentItem = -1;
	// 游标数量
	private int mIndicatorCount = 0;
	// 游标宽度
	private int mIndicatorWidth = 0;
	// 游标高度
	private int mIndicatorHeight = 0;
	// 游标间隙
	private int mIndicatorInterval = 10;

	public IndicatorView(Context context) {
		this(context, null);
	}

	public IndicatorView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public IndicatorView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.IndicatorView);
		final int mIndicatorWidth = mTypedArray.getDimensionPixelOffset(R.styleable.IndicatorView_indicatorWidth, this.mIndicatorWidth);
		final int mIndicatorHeight = mTypedArray.getDimensionPixelOffset(R.styleable.IndicatorView_indicatorHeight, this.mIndicatorHeight);
		final int mIndicatorInterval = mTypedArray.getDimensionPixelOffset(R.styleable.IndicatorView_indicatorInterval, this.mIndicatorInterval);
		final int mSelectedIndicatorResId = mTypedArray.getResourceId(R.styleable.IndicatorView_indicatorSelectedDrawable, R.drawable.sha_indicator_theme);
		final int mUnSelectedIndicatorResId = mTypedArray.getResourceId(R.styleable.IndicatorView_indicatorUnSelectedDrawable, R.drawable.sha_indicator_white);
		final int mSelectedIndicatorColor = mTypedArray.getColor(R.styleable.IndicatorView_indicatorSelectedColor, this.mSelectedIndicatorColor);
		final int mUnSelectedIndicatorColor = mTypedArray.getColor(R.styleable.IndicatorView_indicatorUnSelectedColor, this.mUnSelectedIndicatorColor);
		final float mSelectedIndicatorAlpha = mTypedArray.getFloat(R.styleable.IndicatorView_indicatorSelectedAlpha, this.mSelectedIndicatorAlpha);
		final float mUnSelectedIndicatorAlpha = mTypedArray.getFloat(R.styleable.IndicatorView_indicatorUnSelectedAlpha, this.mUnSelectedIndicatorAlpha);
		mTypedArray.recycle();

		this.setWillNotDraw(false);
		this.setIndicatorWidth(mIndicatorWidth);
		this.setIndicatorHeight(mIndicatorHeight);
		this.setIndicatorInterval(mIndicatorInterval);
		if (mSelectedIndicatorResId != -1) {
			this.setSelectedIndicatorResources(mSelectedIndicatorResId);
		}
		if (mUnSelectedIndicatorResId != -1) {
			this.setUnSelectedIndicatorResources(mUnSelectedIndicatorResId);
		}
		if (mSelectedIndicatorColor != -1) {
			this.setSelectedIndicatorColor(mSelectedIndicatorColor);
		}
		if (mUnSelectedIndicatorColor != -1) {
			this.setUnSelectedIndicatorColor(mUnSelectedIndicatorColor);
		}
		this.setSelectedIndicatorAlpha(mSelectedIndicatorAlpha);
		this.setUnSelectedIndicatorAlpha(mUnSelectedIndicatorAlpha);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int indicatorWidth = 0;
		int indicatorHeight = 0;

		if (this.mIndicatorCount > 0) {
			final Drawable selectedIndicatorDrawable = this.mSelectedIndicatorDrawable;
			final Drawable unselectedIndicatorDrawable = this.mUnSelectedIndicatorDrawable;

			final int indicatorInterval = this.mIndicatorInterval;
			int selectedIntrinsicWidth = selectedIndicatorDrawable.getIntrinsicWidth();
			int selectedIntrinsicHeight = selectedIndicatorDrawable.getIntrinsicHeight();
			int unselectedIntrinsicWidth = unselectedIndicatorDrawable.getIntrinsicWidth();
			int unselectedIntrinsicHeight = unselectedIndicatorDrawable.getIntrinsicHeight();

			if (this.mIndicatorWidth > 0) {
				selectedIntrinsicWidth = this.mIndicatorWidth;
				unselectedIntrinsicWidth = this.mIndicatorWidth;
			}

			if (this.mIndicatorHeight > 0) {
				selectedIntrinsicHeight = this.mIndicatorHeight;
				unselectedIntrinsicHeight = this.mIndicatorHeight;
			}

			indicatorWidth = selectedIntrinsicWidth;
			indicatorHeight = selectedIntrinsicHeight;

			indicatorWidth += unselectedIntrinsicWidth * (this.mIndicatorCount - 1);
			indicatorHeight = Math.max(unselectedIntrinsicHeight, indicatorHeight);
			// append indicator interval
			indicatorWidth += indicatorInterval * (this.mIndicatorCount - 1);
		}
		this.setMeasuredDimension(MeasureSpec.makeMeasureSpec(indicatorWidth, MeasureSpec.EXACTLY),
				MeasureSpec.makeMeasureSpec(indicatorHeight, MeasureSpec.EXACTLY));
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);

		final Drawable selectedIndicatorDrawable = this.mSelectedIndicatorDrawable;
		final Drawable unselectedIndicatorDrawable = this.mUnSelectedIndicatorDrawable;

		final int indicatorInterval = this.mIndicatorInterval;
		int selectedIntrinsicWidth = selectedIndicatorDrawable.getIntrinsicWidth();
		int selectedIntrinsicHeight = selectedIndicatorDrawable.getIntrinsicHeight();
		int unselectedIntrinsicWidth = unselectedIndicatorDrawable.getIntrinsicWidth();
		int unselectedIntrinsicHeight = unselectedIndicatorDrawable.getIntrinsicHeight();

		int selectedIndicatorColor = this.mSelectedIndicatorColor;
		int unselectedIndicatorColor = this.mUnSelectedIndicatorColor;

		if (this.mIndicatorWidth > 0) {
			selectedIntrinsicWidth = this.mIndicatorWidth;
			unselectedIntrinsicWidth = this.mIndicatorWidth;
		}
		if (this.mIndicatorHeight > 0) {
			selectedIntrinsicHeight = this.mIndicatorHeight;
			unselectedIntrinsicHeight = this.mIndicatorHeight;
		}

		int layoutLeft = 0;
		int layoutTop = 0;
		for (int position = 0; position < this.mIndicatorCount; position++) {
			final @ColorInt int indicatorColor;

			final Drawable drawable;
			if (position == this.mCurrentItem) {
				drawable = DrawableCompat.wrap(selectedIndicatorDrawable);
				drawable.setAlpha((int) (this.mSelectedIndicatorAlpha * 255));
				drawable.setBounds(layoutLeft, layoutTop,
						layoutLeft + selectedIntrinsicWidth,
						layoutTop + selectedIntrinsicHeight);
				layoutLeft += (selectedIntrinsicWidth + indicatorInterval);
				indicatorColor = selectedIndicatorColor;
			} else {
				drawable = DrawableCompat.wrap(unselectedIndicatorDrawable);
				drawable.setAlpha((int) (this.mUnSelectedIndicatorAlpha * 255));
				drawable.setBounds(layoutLeft, layoutTop,
						layoutLeft + unselectedIntrinsicWidth,
						layoutTop + unselectedIntrinsicHeight);
				layoutLeft += (unselectedIntrinsicWidth + indicatorInterval);
				indicatorColor = unselectedIndicatorColor;
			}
			if (indicatorColor != -1) {
				if (Build.VERSION.SDK_INT == 21) {
					drawable.setColorFilter(indicatorColor, PorterDuff.Mode.SRC_IN);
				} else {
					DrawableCompat.setTint(drawable, indicatorColor);
				}
			}
			drawable.draw(canvas);
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		final ViewParent parent = this.getParent();
		if (parent instanceof BannerLayout) {
			this.setupBannerLayout((BannerLayout) parent);
		}
	}

	public void setScrollPosition(int position, float positionOffset, int positionOffsetPixels) {
		ViewCompat.postInvalidateOnAnimation(this);
	}

	public void setIndicatorWidth(int indicatorWidth) {
		this.mIndicatorWidth = indicatorWidth;
		ViewCompat.postInvalidateOnAnimation(this);
	}

	public void setIndicatorHeight(int indicatorHeight) {
		this.mIndicatorHeight = indicatorHeight;
		ViewCompat.postInvalidateOnAnimation(this);
	}

	public void setIndicatorCount(int indicatorCount) {
		this.mIndicatorCount = indicatorCount;
		ViewCompat.postInvalidateOnAnimation(this);
	}

	public void setIndicatorInterval(int indicatorInterval) {
		this.mIndicatorInterval = indicatorInterval;
		ViewCompat.postInvalidateOnAnimation(this);
	}

	public void setSelectedIndicatorColor(@ColorInt int color) {
		this.mSelectedIndicatorColor = color;
		ViewCompat.postInvalidateOnAnimation(this);
	}

	public void setUnSelectedIndicatorColor(@ColorInt int color) {
		this.mUnSelectedIndicatorColor = color;
		ViewCompat.postInvalidateOnAnimation(this);
	}

	public void setSelectedIndicatorResources(@DrawableRes int resId) {
		this.setSelectedIndicatorDrawable(ContextCompat.getDrawable(this.getContext(), resId));
	}

	public void setSelectedIndicatorDrawable(@Nullable Drawable drawable) {

		this.mSelectedIndicatorDrawable = drawable;
		ViewCompat.postInvalidateOnAnimation(this);
	}

	public void setUnSelectedIndicatorResources(@DrawableRes int resId) {
		this.setUnSelectedIndicatorDrawable(ContextCompat.getDrawable(this.getContext(), resId));
	}

	public void setUnSelectedIndicatorDrawable(@Nullable Drawable drawable) {
		this.mUnSelectedIndicatorDrawable = drawable;
		ViewCompat.postInvalidateOnAnimation(this);
	}

	public void setSelectedIndicatorAlpha(@FloatRange(from = 0.1F, to = 1.F) float alpha) {
		this.mSelectedIndicatorAlpha = alpha;
		ViewCompat.postInvalidateOnAnimation(this);
	}

	public void setUnSelectedIndicatorAlpha(@FloatRange(from = 0.1F, to = 1.F) float alpha) {
		this.mUnSelectedIndicatorAlpha = alpha;
		ViewCompat.postInvalidateOnAnimation(this);
	}

	public void setCurrentItem(int position) {
		if (this.mCurrentItem != position && position < this.mIndicatorCount) {
			this.mCurrentItem = position;
			ViewCompat.postInvalidateOnAnimation(this);
		}
	}

	public int getIndicatorCount() {
		return this.mIndicatorCount;
	}

	public int getCurrentItem() {
		return this.mCurrentItem;
	}

	private ViewPagerCompat mViewPagerCompat;
	private ViewPagerCompat.Adapter mIndicatorAdapter;
	private IndicatorDataSetObserver mIndicatorDataSetObserver;
	private IndicatorOnPageChangeListener mOnPageChangeListener;
	private IndicatorOnAdapterChangeListener mOnAdapterChangeListener;

	public void setupBannerLayout(@Nullable BannerLayout bannerLayout) {
		if (bannerLayout == null) {
			this.setupViewPagerCompat(null);
		} else {
			this.setupViewPagerCompat(bannerLayout.getViewPagerCompat());
		}
	}

	public void setupViewPagerCompat(@Nullable ViewPagerCompat viewPager) {
		if (this.mViewPagerCompat != null) {
			this.mViewPagerCompat.removeOnAdapterChangeListener(this.mOnAdapterChangeListener);
			this.mViewPagerCompat.removeOnPageChangeListener(this.mOnPageChangeListener);
			this.mViewPagerCompat = null;
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
			this.mViewPagerCompat = viewPager;
			this.mViewPagerCompat.addOnPageChangeListener(this.mOnPageChangeListener);
			this.mViewPagerCompat.addOnAdapterChangeListener(this.mOnAdapterChangeListener);
			this.setPagerAdapter(this.mViewPagerCompat.getAdapter());
		}
	}

	private void setPagerAdapter(@Nullable ViewPagerCompat.Adapter adapter) {
		if (this.mIndicatorAdapter != null) {
			this.mIndicatorAdapter.unregisterDataSetObserver(this.mIndicatorDataSetObserver);
		}
		this.mIndicatorAdapter = adapter;
		if (this.mIndicatorAdapter != null) {
			if (this.mIndicatorDataSetObserver == null) {
				this.mIndicatorDataSetObserver = new IndicatorDataSetObserver();
			}
			this.mIndicatorAdapter.registerDataSetObserver(this.mIndicatorDataSetObserver);
		}
		this.populateFromAdapter();
	}

	private void populateFromAdapter() {
		this.mIndicatorCount = 0;
		if (this.mIndicatorAdapter != null) {
			this.mIndicatorCount = this.mIndicatorAdapter.getItemCount();
		}
		if (this.mViewPagerCompat != null) {
			this.setCurrentItem(this.mViewPagerCompat.getCurrentItem());
		}
		ViewCompat.postInvalidateOnAnimation(this);
	}

	private final class IndicatorOnAdapterChangeListener implements ViewPagerCompat.OnAdapterChangeListener {

		/**
		 * Called when the adapter for the given view pager has changed.
		 *
		 * @param container  ViewPagerCompat where the adapter change has happened
		 * @param oldAdapter the previously set adapter
		 * @param newAdapter the newly set adapter
		 */
		@Override
		public void onAdapterChanged(@NonNull ViewPagerCompat container, @Nullable ViewPagerCompat.Adapter oldAdapter, @Nullable ViewPagerCompat.Adapter newAdapter) {
			IndicatorView.this.setPagerAdapter(newAdapter);
		}
	}

	private final class IndicatorOnPageChangeListener implements ViewPagerCompat.OnPageChangeListener {

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
			IndicatorView.this.setScrollPosition(position, positionOffset, positionOffsetPixels);
		}

		/**
		 * This method will be invoked when a new page becomes selected. Animation is not
		 * necessarily complete.
		 *
		 * @param position Position index of the new selected page.
		 */
		@Override
		public void onPageSelected(int position) {
			IndicatorView.this.setCurrentItem(position);
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
			IndicatorView.this.populateFromAdapter();
		}

		/**
		 * This method is called when the entire data becomes invalid,
		 * most likely through a call to {@link Cursor#deactivate()} or {@link Cursor#close()} on a
		 * {@link Cursor}.
		 */
		@Override
		public void onInvalidated() {
			IndicatorView.this.populateFromAdapter();
		}
	}
}
