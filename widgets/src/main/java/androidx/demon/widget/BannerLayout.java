package androidx.demon.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * Author create by ok on 2019-07-14
 * Email : ok@163.com.
 */
public class BannerLayout extends RelativeLayout implements LifecycleObserver, ViewTreeObserver.OnScrollChangedListener {

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

	private boolean mIsDispatchTouchEvent;

	private void dispatchPlayTouchEvent(MotionEvent event) {
		if (this.mViewPagerCompat.isAllowUserScrollable()) {
			if (this.mIsAutoPlaying) {
				switch (event.getActionMasked()) {
					case MotionEvent.ACTION_DOWN:
						this.mIsDispatchTouchEvent = true;
						this.stopPlay();
						break;
					case MotionEvent.ACTION_OUTSIDE:
					case MotionEvent.ACTION_CANCEL:
					case MotionEvent.ACTION_UP:
						this.mIsDispatchTouchEvent = false;
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
		this.getViewTreeObserver()
				.addOnScrollChangedListener(this);
		this.startPlay();
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		this.getViewTreeObserver()
				.removeOnScrollChangedListener(this);
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

	@Override
	public void onScrollChanged() {
		if (isLocalVisibleBound(this)) {
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
			this.mIndicator.onIndicatorDetachedFromWindow(this, mAdapter);
			this.mIsIndicatorInited = false;
		}
		this.mViewPagerCompat.setAdapter(adapter);
		this.mIsAdapterInited = true;
		if (!this.mIsIndicatorInited && this.mIndicator != null) {
			this.mIndicator.onIndicatorAttachedToWindow(this, adapter);
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
			this.mIndicator.onIndicatorDetachedFromWindow(this, mAdapter);
			this.mIsIndicatorInited = false;
		}
		this.mIndicator = indicator;
		if (!this.mIsIndicatorInited && mAdapter != null) {
			this.mIndicator.onIndicatorAttachedToWindow(this, mAdapter);
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
		if (this.mIsAdapterInited
				&& !this.mIsDispatchTouchEvent
				&& isLocalVisibleBound(this)) {
			this.mIsPlaying = true;
			if (this.mPlayTask == null) {
				this.mPlayTask = new PlayTask();
			}
			this.removeCallbacks(this.mPlayTask);
			this.postDelayed(this.mPlayTask, this.mAutoPlayDelayMillis);
		}
		if (DEBUG) {
			Log.i(TAG, "[Banner, StartPlay] : " + this.mIsPlaying);
		}
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
	public synchronized void stopPlay() {
		if (this.mIsPlaying) {
			if (this.mPlayTask != null) {
				this.removeCallbacks(this.mPlayTask);
				this.mPlayTask = null;
			}
			this.mIsPlaying = false;
		}
		if (DEBUG) {
			Log.i(TAG, "[Banner, StopPlay] : " + this.mIsPlaying);
		}
	}

	public boolean isPlaying() {
		return this.mIsPlaying;
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
	public synchronized void recycled() {
		this.stopPlay();
		this.mIsPlaying = false;
		this.mIsAutoPlaying = false;
	}

	private synchronized boolean performNextPlay() {
		if (this.mIsPlaying && this.mIsAutoPlaying) {
			if ((SCROLL_DIRECTION_START == this.mScrollDirection && this.mViewPagerCompat.pageEnd())
					|| (SCROLL_DIRECTION_END == this.mScrollDirection && this.mViewPagerCompat.pageStart())) {
				return true;
			}
		}
		this.stopPlay();
		return false;
	}

	private static boolean isLocalVisibleBound(@NonNull View view) {
		final Rect mRect = new Rect();
		view.getLocalVisibleRect(mRect);
		return view.isEnabled()
				&& view.isShown()
				&& (view.getParent() != null)
				&& (!(mRect.top < 0 || mRect.bottom > view.getBottom()));
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
			if (BannerLayout.this.performNextPlay()) {
				if (DEBUG) {
					Log.i(TAG, "[Banner, Playing] : " + BannerLayout.this.getCurrentItem());
				}
				BannerLayout.this.postDelayed(this, mAutoPlayDelayMillis);
			}
		}
	}

	public interface Indicator {

		void onIndicatorAttachedToWindow(@NonNull BannerLayout bannerLayout, @NonNull ViewPagerCompat.Adapter adapter);

		void onIndicatorDetachedFromWindow(@NonNull BannerLayout bannerLayout, @Nullable ViewPagerCompat.Adapter adapter);
	}
}
