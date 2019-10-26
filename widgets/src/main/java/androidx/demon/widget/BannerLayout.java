package androidx.demon.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.customview.view.AbsSavedState;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

/**
 * @Author create by Zoran on 2019-09-21
 * @Email : 171905184@qq.com
 * @Description :
 */
public class BannerLayout extends RelativeLayout implements Handler.Callback, LifecycleObserver {

	private static final boolean DEBUG = false;
	private static final String TAG = "BannerLayout";

	public static final int FLAG_STATE_NONE = 0;
	public static final int FLAG_STATE_START = 1;
	public static final int FLAG_STATE_PLAYING = 2;
	public static final int FLAG_STATE_STOP = 3;

	public static final int PLAY_SCROLL_DIRECTION_START = 0;
	public static final int PLAY_SCROLL_DIRECTION_END = 1;

	private static final int DEFAULT_SETTLE_DURATION= 850;
	private static final int DEFAULT_AUTO_PLAY_DELAY = 2800;
	private ArrayList<OnPlayStateListener> mOnPlayStateListeners;
	private ViewPagerCompat mViewPagerCompat;
	private Handler mPlayStateHandler;

	private int mCurPlayState = FLAG_STATE_NONE;
	private int mPlayScrollDirection = PLAY_SCROLL_DIRECTION_START;
	private int mAutoPlayDelayMillis = DEFAULT_AUTO_PLAY_DELAY;

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
		this.mPlayStateHandler = new Handler(Looper.getMainLooper(), this);

		final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.BannerLayout);
		final int mOrientation = mTypedArray.getInt(R.styleable.BannerLayout_android_orientation, ViewPagerCompat.HORIZONTAL);
		final int mScrollDirection = mTypedArray.getInt(R.styleable.BannerLayout_bannerScrollDirection, this.mPlayScrollDirection);
		final int mScrollingDuration = mTypedArray.getInteger(R.styleable.BannerLayout_bannerScrollingDuration, DEFAULT_SETTLE_DURATION);
		final int mAutoPlayDelayMillis = mTypedArray.getInteger(R.styleable.BannerLayout_bannerAutoPlayDelayMillis, this.mAutoPlayDelayMillis);
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
		this.setAutoPlayFlags(mShouldAutoPlaying);
		this.setPlayScrollDirection(mScrollDirection);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState savedState = new SavedState(superState);
		savedState.mCurPosition = this.getCurrentItem();
		return savedState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof SavedState) {
			SavedState savedState = (SavedState) state;
			super.onRestoreInstanceState(savedState.getSuperState());
			this.setCurrentItem(savedState.mCurPosition, false);
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
			if (this.mIsShouldAutoPlayFlags) {
				switch (event.getActionMasked()) {
					case MotionEvent.ACTION_DOWN:
						this.stopPlay();
						break;
					case MotionEvent.ACTION_OUTSIDE:
					case MotionEvent.ACTION_CANCEL:
					case MotionEvent.ACTION_UP:
						this.startPlay();
						break;
				}
			}
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

	private boolean mIsShouldPlayFlags = true;
	private boolean mIsShouldAutoPlayFlags = true;
	private boolean mIsShouldPlayInProgress = false;
	private long mCurrentSystemTimeMillis = 0L;

	@Override
	public synchronized boolean handleMessage(Message msg) {
		final boolean handle = this.setPlayState(msg.what);
		// play state changed
		if (handle || this.mIsShouldPlayInProgress) {
			long autoPlayDelayMillis = this.mAutoPlayDelayMillis;

			switch (this.mCurPlayState) {
				case FLAG_STATE_START:
					this.mIsShouldPlayInProgress = true;
					break;
				case FLAG_STATE_STOP:
					this.mIsShouldPlayInProgress = false;
					break;
				case FLAG_STATE_PLAYING:
					if (this.isShown() && this.mIsShouldPlayInProgress) {
						final int positionByBefore = this.mViewPagerCompat.getCurrentLayoutItem();
						if (PLAY_SCROLL_DIRECTION_START == this.mPlayScrollDirection) {
							this.mIsShouldPlayInProgress = this.mViewPagerCompat.pageEnd();
						} else if (PLAY_SCROLL_DIRECTION_END == this.mPlayScrollDirection) {
							this.mIsShouldPlayInProgress = this.mViewPagerCompat.pageStart();
						}
						if (positionByBefore == this.mViewPagerCompat.getCurrentLayoutItem()) {
							autoPlayDelayMillis = 100L;
						}
						this.mIsShouldPlayInProgress &= this.mIsShouldAutoPlayFlags;
					} else {
						this.mIsShouldPlayInProgress = false;
					}
					break;
			}
			if (this.mIsShouldPlayInProgress &= this.mIsShouldPlayFlags) {
				if (DEBUG) {
					Log.i(TAG, "BannerLayout time delta : " +
							(this.mCurrentSystemTimeMillis - System.currentTimeMillis()));
				}
				this.mCurrentSystemTimeMillis = System.currentTimeMillis();
				this.nextPlay(autoPlayDelayMillis);
			} else {
				this.stopPlay();
			}
		}
		return handle;
	}

	public synchronized void startPlay() {
		if (this.isEnabled() && !this.mIsShouldPlayInProgress) {
			this.mPlayStateHandler.removeMessages(FLAG_STATE_STOP);
			this.mPlayStateHandler.removeMessages(FLAG_STATE_PLAYING);
			this.mPlayStateHandler.removeMessages(FLAG_STATE_START);
			this.mPlayStateHandler.sendEmptyMessage(FLAG_STATE_START);
		}
	}

	public synchronized void stopPlay() {
		this.mPlayStateHandler.removeMessages(FLAG_STATE_START);
		this.mPlayStateHandler.removeMessages(FLAG_STATE_PLAYING);
		this.mPlayStateHandler.removeMessages(FLAG_STATE_STOP);
		this.mPlayStateHandler.sendEmptyMessage(FLAG_STATE_STOP);
	}

	private synchronized void nextPlay(long delayMillis) {
		this.mPlayStateHandler.removeMessages(FLAG_STATE_PLAYING);
		this.mPlayStateHandler.sendEmptyMessageDelayed(FLAG_STATE_PLAYING, delayMillis);
	}

	public synchronized void recycled() {
		this.mPlayStateHandler.removeCallbacksAndMessages(null);
		this.mPlayStateHandler = null;
	}

	private boolean setPlayState(int playState) {
		if (this.mCurPlayState != playState) {
			this.mCurPlayState = playState;

			if (this.mOnPlayStateListeners != null) {
				for (OnPlayStateListener listener : this.mOnPlayStateListeners) {
					listener.onPlayStateChanged(this, playState);
				}
			}
			return true;
		}
		return false;
	}

	public void setLifecycleOwner(@NonNull LifecycleOwner owner) {
		this.setLifecycle(owner.getLifecycle());
	}

	public void setLifecycle(@NonNull Lifecycle lifecycle) {
		lifecycle.addObserver(this);
	}

	public void setAdapter(@NonNull ViewPagerCompat.Adapter adapter) {
		this.mViewPagerCompat.setAdapter(adapter);
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

	public void addOnAdapterChangeListener(@NonNull ViewPagerCompat.OnAdapterChangeListener listener) {
		this.mViewPagerCompat.addOnAdapterChangeListener(listener);
	}

	public void removeOnAdapterChangeListener(@NonNull ViewPagerCompat.OnAdapterChangeListener listener) {
		this.mViewPagerCompat.removeOnAdapterChangeListener(listener);
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

	public void setPlayScrollDirection(int direction) {
		if (this.mPlayScrollDirection != direction) {
			this.mPlayScrollDirection = direction;
		}
	}

	public void setShouldPlayFlags(boolean flags) {
		if (this.mIsShouldPlayFlags != flags) {
			this.mIsShouldPlayFlags = flags;
		}
	}

	public void setAutoPlayFlags(boolean flags) {
		if (this.mIsShouldAutoPlayFlags != flags) {
			this.mIsShouldAutoPlayFlags = flags;
		}
	}

	public void setAutoPlayDelayMillis(int delayMillis) {
		if (this.mAutoPlayDelayMillis != delayMillis) {
			this.mAutoPlayDelayMillis = delayMillis;
		}
	}

	public void addOnPlayStateListener(@NonNull OnPlayStateListener listener) {
		if (this.mOnPlayStateListeners == null) {
			this.mOnPlayStateListeners = new ArrayList<>();
		}
		this.mOnPlayStateListeners.add(listener);
	}

	public void removeOnPlayStateListener(@NonNull OnPlayStateListener listener) {
		if (this.mOnPlayStateListeners != null) {
			this.mOnPlayStateListeners.remove(listener);
		}
	}

	public int getCurPlayState() {
		return this.mCurPlayState;
	}

	public long getAutoPlayDelayMillis() {
		return this.mAutoPlayDelayMillis;
	}

	public boolean isShouldAutoPlayFlags() {
		return this.mIsShouldAutoPlayFlags;
	}

	public boolean isShouldPlayInProgress() {
		return this.mIsShouldPlayInProgress;
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
	protected void onResume() {
		this.startPlay();
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
	protected void onPause() {
		this.stopPlay();
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_STOP)
	protected void onStop() {
		this.stopPlay();
	}

	@OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
	protected void onDestroy() {
		this.recycled();
	}

	public interface OnPlayStateListener {

		void onPlayStateChanged(@NonNull BannerLayout container, int playState);
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
