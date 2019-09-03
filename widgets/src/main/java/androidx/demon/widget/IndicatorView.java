package androidx.demon.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.demon.widget.cacher.RecycledPool;

/**
 * Author create by ok on 2019-07-14
 * Email : ok@163.com.
 */
public class IndicatorView extends LinearLayout implements BannerLayout.Indicator {

	private final RecycledPool<View> mRecycledPool = new RecycledPool<>();
	private OnIndicatorChangeListener mOnIndicatorChangeListener;

	private IndicatorDataSetObserver mIndicatorDataSetObserver;
	private Adapter mIndicatorAdapter;

	private AdapterDataSetObserver mAdapterDataSetObserver;
	private ViewPagerCompat.Adapter mBannerAdapter;

	private float mIndicatorWeight = 0.f;
	private int mIndicatorMargin = 8;

	public IndicatorView(Context context) {
		this(context, null);
	}

	public IndicatorView(Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public IndicatorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.performInit(context, attrs);
	}

	private void performInit(@NonNull Context context, AttributeSet attrs) {
		final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.IndicatorView);
		this.mIndicatorWeight = mTypedArray.getFloat(R.styleable.IndicatorView_indicatorWeight, this.mIndicatorWeight);
		this.mIndicatorMargin = mTypedArray.getDimensionPixelOffset(R.styleable.IndicatorView_indicatorMargin, this.mIndicatorMargin);
		mTypedArray.recycle();
		this.setAdapter(new SimpleAdapter(context, attrs));
		this.setOrientation(HORIZONTAL);
	}

	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		throw new IllegalStateException("prohibit registration of new Views");
	}

	@Override
	public void onIndicatorAttachedToWindow(@NonNull BannerLayout bannerLayout, @NonNull ViewPagerCompat.Adapter adapter) {
		if (this.mOnIndicatorChangeListener == null) {
			this.mOnIndicatorChangeListener = new OnIndicatorChangeListener();
		}
		bannerLayout.addOnPageChangeListener(this.mOnIndicatorChangeListener);

		if (this.mAdapterDataSetObserver == null) {
			this.mAdapterDataSetObserver = new AdapterDataSetObserver();
		}
		adapter.registerDataSetObserver(this.mAdapterDataSetObserver);

		this.mBannerAdapter = adapter;
		// update indicator
		if (this.mIndicatorAdapter != null) {
			this.mIndicatorAdapter.notifyDataSetChanged();
		}
	}

	@Override
	public void onIndicatorDetachedFromWindow(@NonNull BannerLayout bannerLayout, @Nullable ViewPagerCompat.Adapter adapter) {
		if (this.mOnIndicatorChangeListener != null) {
			bannerLayout.removeOnPageChangeListener(this.mOnIndicatorChangeListener);
		}
		if (adapter != null && this.mAdapterDataSetObserver != null) {
			adapter.unregisterDataSetObserver(this.mAdapterDataSetObserver);
		}
		this.performIndicatorRecycled(false);
		this.mCurrentItemPosition = -1;
		this.mBannerAdapter = null;
	}

	public void setIndicatorWeight(float weight) {
		this.mIndicatorWeight = weight;
	}

	public void setIndicatorMargin(int marginSize) {
		this.mIndicatorMargin = marginSize;
	}

	public void setAdapter(@Nullable Adapter adapter) {
		if (this.mIndicatorAdapter == adapter) {
			return;
		}
		if (this.mIndicatorAdapter != null) {
			this.performIndicatorRecycled(false);
			this.mIndicatorAdapter.setViewPagerObserver(null);
			this.mCurrentItemPosition = -1;
		}
		this.mIndicatorAdapter = adapter;

		if (this.mIndicatorAdapter != null) {
			if (this.mIndicatorDataSetObserver == null) {
				this.mIndicatorDataSetObserver = new IndicatorDataSetObserver();
			}
			this.mIndicatorAdapter.setViewPagerObserver(this.mIndicatorDataSetObserver);
			this.mIndicatorAdapter.notifyDataSetChanged();
		}
	}

	public Adapter getAdapter() {
		return this.mIndicatorAdapter;
	}

	public RecycledPool<View> getRecycledPool() {
		return this.mRecycledPool;
	}

	private void indicatorDataSetChanged() {
		this.performIndicatorRecycled(true);
		if (this.mIndicatorAdapter == null || this.mBannerAdapter == null) {
			return;
		}
		final int mItemCount = this.mBannerAdapter.getItemCount();
		this.mCurrentItemPosition = Math.max(0, Math.min(this.mCurrentItemPosition, mItemCount - 1));

		for (int position = 0; position < mItemCount; position++) {
			View mIndicatorView = this.mRecycledPool.getRecycled(0);
			if (mIndicatorView == null) {
				mIndicatorView = this.mIndicatorAdapter.onCreateIndicatorView(LayoutInflater.from(this.getContext()), this, position);
			}
			if (mIndicatorView.getParent() != null) {
				throw new IllegalStateException("View " + mIndicatorView + " has parent exist");
			}
			final ViewGroup.LayoutParams mLayoutParams = mIndicatorView.getLayoutParams();
			LayoutParams preLayoutParams;
			if (mLayoutParams == null) {
				preLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			} else if (!this.checkLayoutParams(mLayoutParams)) {
				preLayoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
			} else {
				preLayoutParams = (LayoutParams) mLayoutParams;
			}
			preLayoutParams.gravity = Gravity.CENTER;
			preLayoutParams.weight = this.mIndicatorWeight;
			// direction : Vertical
			if (position == 0) {
				preLayoutParams.leftMargin = this.mIndicatorMargin;
				preLayoutParams.rightMargin = this.mIndicatorMargin / 2;
			} else if (position == mItemCount - 1) {
				preLayoutParams.leftMargin = this.mIndicatorMargin / 2;
				preLayoutParams.rightMargin = this.mIndicatorMargin;
			} else {
				preLayoutParams.leftMargin = this.mIndicatorMargin / 2;
				preLayoutParams.rightMargin = this.mIndicatorMargin / 2;
			}
			super.addView(mIndicatorView, -1, preLayoutParams);
			// bind
			this.mIndicatorAdapter.onBindIndicatorView(this, mIndicatorView, this.mCurrentItemPosition == position, position);
		}
	}

	private int mCurrentItemPosition = -1;

	private void performIndicatorChanged(int position) {
		if (this.mIndicatorAdapter == null) {
			return;
		}
		if (this.mCurrentItemPosition != position) {
			View mIndicatorView = this.getChildAt(position);
			// selected new position
			if (mIndicatorView != null) {
				this.mIndicatorAdapter.onBindIndicatorView(this, mIndicatorView, true, position);
			}
			// unselected old position
			if (this.mCurrentItemPosition >= 0) {
				mIndicatorView = this.getChildAt(this.mCurrentItemPosition);
				if (mIndicatorView != null) {
					this.mIndicatorAdapter.onBindIndicatorView(this, mIndicatorView, false, this.mCurrentItemPosition);
				}
			}
			this.mCurrentItemPosition = position;
		}
	}

	private void performIndicatorRecycled(boolean recycled) {
		for (int index = 0; index < this.getChildCount(); index++) {
			final View mIndicatorView = this.getChildAt(index);
			this.removeView(mIndicatorView);
			this.mRecycledPool.putRecycled(0, mIndicatorView);
			index--;
		}
		// recycled views
		if (!recycled) {
			this.mRecycledPool.clear();
			this.removeAllViews();
		}
	}

	final class AdapterDataSetObserver extends DataSetObserver {

		@Override
		public void onChanged() {
			indicatorDataSetChanged();
		}

		@Override
		public void onInvalidated() {
			indicatorDataSetChanged();
		}
	}

	final class IndicatorDataSetObserver extends DataSetObserver {

		@Override
		public void onChanged() {
			indicatorDataSetChanged();
		}

		@Override
		public void onInvalidated() {
			indicatorDataSetChanged();
		}
	}

	final class OnIndicatorChangeListener extends ViewPagerCompat.OnSimplePageChangeListener {

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
			performIndicatorChanged(position);
		}

		/**
		 * This method will be invoked when a new page becomes selected. Animation is not
		 * necessarily complete.
		 *
		 * @param position Position index of the new selected page.
		 */
		@Override
		public void onPageSelected(int position) {
			performIndicatorChanged(position);
		}
	}

	public static abstract class Adapter {

		private final DataSetObservable mObservable = new DataSetObservable();

		private DataSetObserver mViewPagerObserver;

		public final void registerDataSetObserver(@NonNull DataSetObserver observer) {
			this.mObservable.registerObserver(observer);
		}

		public final void unregisterDataSetObserver(@NonNull DataSetObserver observer) {
			this.mObservable.unregisterObserver(observer);
		}

		public final void notifyDataSetChanged() {
			synchronized (this) {
				if (this.mViewPagerObserver != null) {
					this.mViewPagerObserver.onChanged();
				}
			}
			this.mObservable.notifyChanged();
		}

		@NonNull
		public abstract View onCreateIndicatorView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, int position);

		public abstract void onBindIndicatorView(@NonNull ViewGroup container, @NonNull View view, boolean hasSelected, int position);

		final void setViewPagerObserver(@Nullable DataSetObserver observer) {
			synchronized (this) {
				this.mViewPagerObserver = observer;
			}
		}
	}

	public static class SimpleAdapter extends Adapter {

		private int mIndicatorWidth;
		private int mIndicatorHeight;
		@DrawableRes
		private int mIndicatorResId = -1;
		@DrawableRes
		private int mIndicatorSelectedResId = R.drawable.sha_sol_cir_gray;
		@DrawableRes
		private int mIndicatorUnSelectedResId = R.drawable.sha_sol_cir_white;

		private ImageView.ScaleType mScaleType = ImageView.ScaleType.CENTER_CROP;

		final ImageView.ScaleType[] mScaleTypes = new ImageView.ScaleType[]{
				ImageView.ScaleType.CENTER,
				ImageView.ScaleType.CENTER_CROP,
				ImageView.ScaleType.CENTER_INSIDE,
				ImageView.ScaleType.FIT_CENTER,
				ImageView.ScaleType.FIT_START,
				ImageView.ScaleType.FIT_END,
				ImageView.ScaleType.FIT_XY,
				ImageView.ScaleType.MATRIX
		};

		public SimpleAdapter(@NonNull Context context) {
			this(context, null);
		}

		public SimpleAdapter(@NonNull Context context, @Nullable AttributeSet attrs) {
			final DisplayMetrics display = context.getResources().getDisplayMetrics();
			this.mIndicatorWidth = display.widthPixels / 80;
			this.mIndicatorHeight = display.widthPixels / 80;

			if (attrs != null) {
				final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.IndicatorView);
				this.mIndicatorWidth = mTypedArray.getDimensionPixelOffset(R.styleable.IndicatorView_defIndicatorWidth, this.mIndicatorWidth);
				this.mIndicatorHeight = mTypedArray.getDimensionPixelOffset(R.styleable.IndicatorView_defIndicatorHeight, this.mIndicatorHeight);
				this.mIndicatorResId = mTypedArray.getResourceId(R.styleable.IndicatorView_defIndicatorDrawable, this.mIndicatorResId);
				this.mIndicatorSelectedResId = mTypedArray.getResourceId(R.styleable.IndicatorView_defIndicatorSelectedDrawable, this.mIndicatorSelectedResId);
				this.mIndicatorUnSelectedResId = mTypedArray.getResourceId(R.styleable.IndicatorView_defIndicatorUnSelectedDrawable, this.mIndicatorUnSelectedResId);
				final int index = mTypedArray.getInt(R.styleable.IndicatorView_defIndicatorScaleType, 1);
				this.mScaleType = this.mScaleTypes[index];
				mTypedArray.recycle();
			}
		}

		@NonNull
		@Override
		public View onCreateIndicatorView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, int position) {
			final RelativeLayout.LayoutParams preLayoutParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
			preLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			preLayoutParams.width = this.mIndicatorWidth;
			preLayoutParams.height = this.mIndicatorHeight;
			final AppCompatImageView mImageView = new AppCompatImageView(inflater.getContext());
			mImageView.setLayoutParams(preLayoutParams);
			mImageView.setScaleType(this.mScaleType);
			mImageView.setId(android.R.id.icon);

			final RelativeLayout preRelativeLayout = new RelativeLayout(inflater.getContext());
			preRelativeLayout.addView(mImageView);
			return preRelativeLayout;
		}

		@Override
		public void onBindIndicatorView(@NonNull ViewGroup container, @NonNull View view, boolean hasSelected, int position) {
			final AppCompatImageView mImageView = view.findViewById(android.R.id.icon);
			mImageView.setSelected(hasSelected);

			if (this.mIndicatorResId == -1) {
				if (hasSelected) {
					mImageView.setImageResource(this.mIndicatorSelectedResId);
				} else {
					mImageView.setImageResource(this.mIndicatorUnSelectedResId);
				}
			} else {
				mImageView.setImageResource(this.mIndicatorResId);
			}
		}

		public SimpleAdapter setIndicatorWidth(int indicatorWidth) {
			this.mIndicatorWidth = indicatorWidth;
			return this;
		}

		public SimpleAdapter setIndicatorHeight(int indicatorHeight) {
			this.mIndicatorHeight = indicatorHeight;
			return this;
		}

		public SimpleAdapter setIndicatorResId(@DrawableRes int resId) {
			this.mIndicatorResId = resId;
			return this;
		}

		public SimpleAdapter setIndicatorSelectedResId(@DrawableRes int selectedResId) {
			this.mIndicatorSelectedResId = selectedResId;
			return this;
		}

		public SimpleAdapter setIndicatorUnSelectedResId(@DrawableRes int unSelectedResId) {
			this.mIndicatorUnSelectedResId = unSelectedResId;
			return this;
		}

		public SimpleAdapter setScaleType(ImageView.ScaleType scaleType) {
			this.mScaleType = scaleType;
			return this;
		}
	}
}
