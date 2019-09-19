package androidx.demon.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.customview.view.AbsSavedState;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * Author create by ok on 2019-07-14
 * Email : ok@163.com.
 */
public class BannerLayout extends RelativeLayout implements LifecycleObserver {

	private static final String TAG = "BannerView";
	private static final boolean DEBUG = false;

	public static final int SCROLL_DIRECTION_START = 0;

	public static final int SCROLL_DIRECTION_END = 1;

	@IntDef({SCROLL_DIRECTION_START, SCROLL_DIRECTION_END})
	@Retention(RetentionPolicy.SOURCE)
	@interface DirectionMode {
	}

	private static final int DEFAULT_AUTO_PLAY_DELAY = 2000;
	private static final int DEFAULT_SCROLLING_DURATION = 600;

	private ViewPagerCompat mViewPagerCompat;

	public BannerLayout(Context context) {
		this(context, null);
	}

	public BannerLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public BannerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.performInit(context, attrs);
	}

	private void performInit(@NonNull Context context, AttributeSet attrs) {
		final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.BannerLayout);
		final int mOrientation = mTypedArray.getInt(R.styleable.BannerLayout_android_orientation, ViewPagerCompat.HORIZONTAL);
		final int mScrollDirection = mTypedArray.getInt(R.styleable.BannerLayout_bannerScrollDirection, this.mScrollDirection);
		final int mScrollingDuration = mTypedArray.getInteger(R.styleable.BannerLayout_bannerScrollingDuration, DEFAULT_SCROLLING_DURATION);
		final int mAutoPlayDelayMillis = mTypedArray.getInteger(R.styleable.BannerLayout_bannerAutoPlayDelayMillis, DEFAULT_AUTO_PLAY_DELAY);
		final int mOffscreenPageLimit = mTypedArray.getInteger(R.styleable.BannerLayout_bannerOffscreenPageLimit, 1);
		final int mPageMargin = mTypedArray.getDimensionPixelOffset(R.styleable.BannerLayout_bannerPageMargin, 0);
		final boolean mShouldAutoPlaying = mTypedArray.getBoolean(R.styleable.BannerLayout_bannerShouldAutoPlaying, true);
		final boolean mAllowUserScrollable = mTypedArray.getBoolean(R.styleable.BannerLayout_bannerAllowUserScrollable, true);
		mTypedArray.recycle();

		if (this.mViewPagerCompat == null) {
			final LayoutParams preLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			preLayoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			this.mViewPagerCompat = new ViewPagerCompat(context);
			this.mViewPagerCompat.setLayoutParams(preLayoutParams);
			this.mViewPagerCompat.setId(R.id.app_banner_view_id);
			this.addView(this.mViewPagerCompat);
		}
		this.mViewPagerCompat.setAllowUserScrollable(mAllowUserScrollable);
		this.mViewPagerCompat.setOffscreenPageLimit(mOffscreenPageLimit);
		this.mViewPagerCompat.setScrollingDuration(mScrollingDuration);
		this.mViewPagerCompat.setPageMargin(mPageMargin);
		this.mViewPagerCompat.setOrientation(mOrientation);
		this.mViewPagerCompat.setScrollingLoop(true);
		this.setAutoPlayDelayMillis(mAutoPlayDelayMillis);
		this.setAutoPlaying(mShouldAutoPlaying);
		this.setScrollDirection(mScrollDirection);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState savedState = new SavedState(superState);
		savedState.mCurPosition = this.getCurrentItem();
		if (DEBUG) {
			Log.i(TAG, "[Banner, onSaveInstanceState] : " + savedState.mCurPosition);
		}
		return savedState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof SavedState) {
			SavedState savedState = (SavedState) state;
			super.onRestoreInstanceState(savedState.getSuperState());
			this.setCurrentItem(savedState.mCurPosition, false);
			if (DEBUG) {
				Log.i(TAG, "[Banner, onRestoreInstanceState] : " + savedState.mCurPosition);
			}
			return;
		}
		super.onRestoreInstanceState(state);
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		this.dispatchPlayTouchEvent(event);
		return super.dispatchTouchEvent(event);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		this.dispatchPlayTouchEvent(event);
		return super.onInterceptTouchEvent(event);
	}

	@Override
	@SuppressLint("ClickableViewAccessibility")
	public boolean onTouchEvent(MotionEvent event) {
		this.dispatchPlayTouchEvent(event);
		return super.onTouchEvent(event);
	}

	private void dispatchPlayTouchEvent(MotionEvent event) {
		if (this.mViewPagerCompat.isAllowUserScrollable()) {
			if (this.mIsAutoPlaying) {
				switch (event.getActionMasked()) {
					case MotionEvent.ACTION_DOWN:
						this.mIsShouldStartPlaying = false;
						this.stopPlay();
						break;
					case MotionEvent.ACTION_OUTSIDE:
					case MotionEvent.ACTION_CANCEL:
					case MotionEvent.ACTION_UP:
						this.mIsShouldStartPlaying = true;
						this.startPlay();
						break;
				}
			} else {
				this.stopPlay();
			}
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		this.startPlay();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		this.stopPlay();
	}

	@Override
	protected void onFocusChanged(boolean gainFocus, int direction, @Nullable Rect previouslyFocusedRect) {
		super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
		if (gainFocus) {
			this.startPlay();
		} else {
			this.stopPlay();
		}
	}

	@Override
	protected void onVisibilityChanged(@NonNull View changedView, int visibility) {
		super.onVisibilityChanged(changedView, visibility);
		if (View.VISIBLE == visibility) {
			this.startPlay();
		} else {
			this.stopPlay();
		}
	}

	public void setLifecycleOwner(@NonNull LifecycleOwner owner) {
		this.setLifecycle(owner.getLifecycle());
	}

	public void setLifecycle(@NonNull Lifecycle lifecycle) {
		lifecycle.addObserver(this);
	}

	@DirectionMode
	private int mScrollDirection = SCROLL_DIRECTION_START;
	private int mAutoPlayDelayMillis = DEFAULT_AUTO_PLAY_DELAY;
	private boolean mIsShouldStartPlaying = true;
	private boolean mIsIndicatorInited = false;
	private boolean mIsAdapterInited = false;
	private boolean mIsAutoPlaying = true;
	private boolean mIsPlaying = false;
	private PlayTask mPlayTask;
	private Indicator mIndicator;

	public void setAdapter(@NonNull ViewPagerCompat.Adapter adapter) {
		final ViewPagerCompat.Adapter mAdapter = this.getAdapter();
		if (mAdapter == adapter) {
			return;
		}
		if (this.mIsIndicatorInited && this.mIndicator != null) {
			this.mIndicator.onDetachedFromBannerLayout(this);
			this.mIsIndicatorInited = false;
		}
		this.mViewPagerCompat.setAdapter(adapter);
		this.mIsAdapterInited = true;
		if (!this.mIsIndicatorInited && this.mIndicator != null) {
			this.mIndicator.onAttachedToBannerLayout(this);
			this.mIsIndicatorInited = true;
		}
	}

	public void setOrientation(int orientation) {
		this.mViewPagerCompat.setOrientation(orientation);
	}

	public void setScrollingDuration(int duration) {
		this.mViewPagerCompat.setScrollingDuration(duration);
	}

	public void setCurrentItem(int position) {
		this.mViewPagerCompat.setCurrentItem(position);
	}

	public void setCurrentItem(int position, boolean smoothScroll) {
		this.mViewPagerCompat.setCurrentItem(position, smoothScroll);
	}

	public void setPageMargin(int marginPixels) {
		this.mViewPagerCompat.setPageMargin(marginPixels);
	}

	public void setOffscreenPageLimit(int pageLimit) {
		this.mViewPagerCompat.setOffscreenPageLimit(pageLimit);
	}

	public void setAllowUserScrollable(boolean allowUserScrollable) {
		this.mViewPagerCompat.setAllowUserScrollable(allowUserScrollable);
	}

	public void setPageTransformer(@Nullable ViewPagerCompat.PageTransformer transformer) {
		this.setPageTransformer(true, transformer);
	}

	public void setPageTransformer(boolean reverseDrawingOrder, @Nullable ViewPagerCompat.PageTransformer transformer) {
		this.mViewPagerCompat.setPageTransformer(reverseDrawingOrder, transformer);
	}

	public void setPageTransformer(boolean reverseDrawingOrder, @Nullable ViewPagerCompat.PageTransformer transformer, int pageLayerType) {
		this.mViewPagerCompat.setPageTransformer(reverseDrawingOrder, transformer, pageLayerType);
	}

	public void addOnPageChangeListener(@NonNull ViewPagerCompat.OnPageChangeListener listener) {
		this.mViewPagerCompat.addOnPageChangeListener(listener);
	}

	public void removeOnPageChangeListener(@NonNull ViewPagerCompat.OnPageChangeListener listener) {
		this.mViewPagerCompat.removeOnPageChangeListener(listener);
	}

	public void setScrollDirection(@DirectionMode int direction) {
		if (this.mScrollDirection != direction) {
			this.mScrollDirection = direction;
		}
	}

	public void setIndicator(@NonNull Indicator indicator) {
		final ViewPagerCompat.Adapter mAdapter = this.getAdapter();
		if (this.mIsIndicatorInited && this.mIndicator != null) {
			this.mIndicator.onDetachedFromBannerLayout(this);
			this.mIsIndicatorInited = false;
		}
		this.mIndicator = indicator;
		if (!this.mIsIndicatorInited && mAdapter != null) {
			this.mIndicator.onAttachedToBannerLayout(this);
			this.mIsIndicatorInited = true;
		}
	}

	public void setAutoPlaying(boolean autoPlaying) {
		if (this.mIsAutoPlaying != autoPlaying) {
			this.mIsAutoPlaying = autoPlaying;
			if (this.mIsAutoPlaying) {
				this.startPlay();
			}
		}
	}

	public void setAutoPlayDelayMillis(int delayMillis) {
		this.mAutoPlayDelayMillis = delayMillis;
	}

	public boolean isPlaying() {
		return this.mIsPlaying;
	}

	public boolean isAutoPlaying() {
		return this.mIsAutoPlaying;
	}

	public long getAutoPlayDelayMillis() {
		return this.mAutoPlayDelayMillis;
	}

	public int getCurrentItem() {
		return this.mViewPagerCompat.getCurrentItem();
	}

	public ViewPagerCompat.Adapter getAdapter() {
		return this.mViewPagerCompat.getAdapter();
	}

	public ViewPagerCompat getViewPagerCompat() {
		return this.mViewPagerCompat;
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
	public synchronized void startPlay() {
		if (this.mIsPlaying) {
			return;
		}
		if (this.isEnabled()
				&& this.isShown()
				&& this.mIsAdapterInited
				&& this.mIsShouldStartPlaying) {
			this.mIsPlaying = true;

			if (this.mPlayTask == null) {
				this.mPlayTask = new PlayTask();
			}
			this.postDelayed(this.mPlayTask, this.mAutoPlayDelayMillis);
		}
		if (DEBUG) {
			Log.i(TAG, "[Banner, StartPlay] : " + this.mIsPlaying);
		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
	public synchronized void stopPlay() {
		if (this.mIsPlaying) {
			this.mIsPlaying = false;

			if (this.mPlayTask != null) {
				this.removeCallbacks(this.mPlayTask);
			}
		}
		if (DEBUG) {
			Log.i(TAG, "[Banner, StopPlay] : " + this.mIsPlaying);
		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
	public synchronized void recycled() {
		this.stopPlay();
		this.mPlayTask = null;
		this.mIsPlaying = false;
		this.mIsAutoPlaying = false;
	}

	private synchronized boolean nextPlay() {
		if (this.mIsPlaying) {
			if ((SCROLL_DIRECTION_START == this.mScrollDirection && this.mViewPagerCompat.pageEnd())
					|| (SCROLL_DIRECTION_END == this.mScrollDirection && this.mViewPagerCompat.pageStart())) {
				return this.mIsAutoPlaying;
			}
		}
		this.stopPlay();
		return false;
	}

	final class PlayTask implements Runnable {

		/**
		 * When an object implementing interface <code>Runnable</code> is used
		 * to create a thread, starting the thread causes the object's
		 * <code>run</code> method to be called in that separately executing
		 * thread.
		 * <p>
		 * The general contract of the method <code>run</code> is that it may
		 * take any action whatsoever.
		 *
		 * @see Thread#run()
		 */
		@Override
		public void run() {
			if (BannerLayout.this.nextPlay()) {
				if (DEBUG) {
					Log.i(TAG, "[Banner, Playing] : " + BannerLayout.this.getCurrentItem());
				}
				BannerLayout.this.postDelayed(this, BannerLayout.this.mAutoPlayDelayMillis);
			}
		}
	}

	public interface Indicator {

		void onAttachedToBannerLayout(@NonNull BannerLayout bannerLayout);

		void onDetachedFromBannerLayout(@NonNull BannerLayout bannerLayout);
	}

	public static class SavedState extends AbsSavedState {

		private int mCurPosition;
		private Parcelable mSuperState;

		SavedState(Parcel source) {
			super(source);
		}

		SavedState(Parcel source, ClassLoader loader) {
			super(source, loader);
			if (loader == null) {
				loader = getClass().getClassLoader();
			}
			this.mCurPosition = source.readInt();
			this.mSuperState = source.readParcelable(loader);
		}

		SavedState(Parcelable superState) {
			super(superState);
			this.mSuperState = superState;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(this.mCurPosition);
			dest.writeParcelable(this.mSuperState, flags);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.ClassLoaderCreator<SavedState>() {

			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in, null);
			}

			@Override
			public SavedState createFromParcel(Parcel source, ClassLoader loader) {
				return new SavedState(source, loader);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}
