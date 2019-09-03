package androidx.demon.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.util.ArrayList;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.core.view.ViewCompat;
import androidx.customview.view.AbsSavedState;
import androidx.demon.widget.helper.NestedHelper;
import androidx.demon.widget.helper.NestedScrollingHelper;
import androidx.demon.widget.helper.NestedScrollingHelperImpl;

/**
 * Author create by ok on 2019-07-31
 * Email : ok@163.com.
 */
public class SwipeSideLayout extends FrameLayout implements NestedScrollingHelper.Callback {

	private static final float FRICTION_RATIO = .75F;
	private static final float MIN_SCROLL_SCALE = .2F;

	private final NestedScrollingHelper mNestedScrollingHelper;

	private int mDampSpringBackSize;
	private float mFrictionRatio = FRICTION_RATIO;
	private float mMinScrollScale = MIN_SCROLL_SCALE;
	private boolean mIsOpenState = false;
	private boolean mIsDrawerEnabled = true;

	private OnOpenStateListener mOnOpenStateListener;
	private ArrayList<OnScrollListener> mOnScrollListeners;

	public SwipeSideLayout(@NonNull Context context) {
		this(context, null);
	}

	public SwipeSideLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SwipeSideLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeSideLayout);
		this.mDampSpringBackSize = mTypedArray.getDimensionPixelSize(R.styleable.SwipeSideLayout_dampSpringBackSize, 0);
		mTypedArray.recycle();

		this.mNestedScrollingHelper = new NestedScrollingHelperImpl(this, this);
		// So that we can catch the back button
		this.setFocusableInTouchMode(true);
		this.setMotionEventSplittingEnabled(false);
		this.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

		ViewCompat.setImportantForAccessibility(this,
				ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		this.ensureDrawerView();
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}

	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
		return new LayoutParams(layoutParams);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(this.getContext(), attrs);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
		return layoutParams instanceof LayoutParams;
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (this.mIsOpenState) {
			this.setOpenState(true, false);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (this.mIsOpenState) {
			this.setOpenState(false, false);
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		this.ensureDrawerView();
		if (this.mNestedScrollingHelper.onInterceptTouchEvent(event)) {
			return true;
		}
		return super.onInterceptTouchEvent(event);
	}

	@Override
	@SuppressLint("ClickableViewAccessibility")
	public boolean onTouchEvent(MotionEvent event) {
		this.ensureDrawerView();
		if (this.mNestedScrollingHelper.onTouchEvent(event)) {
			return true;
		}
		return super.onTouchEvent(event);
	}

	@Nullable
	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable mSuperState = super.onSaveInstanceState();
		SavedState mSavedState = new SavedState(mSuperState);
		mSavedState.mIsOpenState = this.mIsOpenState;
		return mSavedState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof SavedState) {
			SavedState mSavedState = (SavedState) state;
			super.onRestoreInstanceState(mSavedState.mSuperState);
			this.setOpenState(mSavedState.mIsOpenState);
		} else {
			super.onRestoreInstanceState(state);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode && this.mIsOpenState) {
			event.startTracking();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode && this.mIsOpenState) {
			this.setOpenState(false);
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	// NestedScrollingParent

	@Override
	public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int nestedScrollAxes) {
		return this.mNestedScrollingHelper.onStartNestedScroll(child, target, nestedScrollAxes);
	}

	@Override
	public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {
		this.mNestedScrollingHelper.onNestedScrollAccepted(child, target, axes);
	}

	@Override
	public int getNestedScrollAxes() {
		return this.mNestedScrollingHelper.getNestedScrollAxes();
	}

	@Override
	public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
		this.mNestedScrollingHelper.onNestedPreScroll(target, dx, dy, consumed);
	}

	@Override
	public void onNestedScroll(@NonNull final View target, final int dxConsumed, final int dyConsumed, final int dxUnconsumed, final int dyUnconsumed) {
		this.mNestedScrollingHelper.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
	}

	@Override
	public void onStopNestedScroll(@NonNull View target) {
		this.mNestedScrollingHelper.onStopNestedScroll(target);
	}

	// NestedScrollingChild

	@Override
	public void setNestedScrollingEnabled(boolean enabled) {
		this.mNestedScrollingHelper.setNestedScrollingEnabled(enabled);
	}

	@Override
	public boolean isNestedScrollingEnabled() {
		return this.mNestedScrollingHelper.isNestedScrollingEnabled();
	}

	@Override
	public boolean startNestedScroll(int axes) {
		return this.mNestedScrollingHelper.startNestedScroll(axes);
	}

	@Override
	public void stopNestedScroll() {
		this.mNestedScrollingHelper.stopNestedScroll();
	}

	@Override
	public boolean hasNestedScrollingParent() {
		return this.mNestedScrollingHelper.hasNestedScrollingParent();
	}

	@Override
	public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
		return this.mNestedScrollingHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
	}

	@Override
	public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
		return this.mNestedScrollingHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
	}

	@Override
	public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
		return this.mNestedScrollingHelper.onNestedPreFling(target, velocityX, velocityY);
	}

	@Override
	public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
		return this.mNestedScrollingHelper.onNestedFling(target, velocityX, velocityY, consumed);
	}

	@Override
	public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
		return this.mNestedScrollingHelper.dispatchNestedFling(velocityX, velocityY, consumed);
	}

	@Override
	public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
		return this.mNestedScrollingHelper.dispatchNestedPreFling(velocityX, velocityY);
	}

	@Override
	public boolean canScrollVertically() {
		if (this.shouldStartNestedScroll()) {
			return this.checkViewScrollLayout(this.getDrawerView(), LayoutParams.SCROLL_LAYOUT_TOP)
					|| this.checkViewScrollLayout(this.getDrawerView(), LayoutParams.SCROLL_LAYOUT_BOTTOM);
		}
		return false;
	}

	@Override
	public boolean canScrollHorizontally() {
		if (this.shouldStartNestedScroll()) {
			return this.checkViewScrollLayout(this.getDrawerView(), LayoutParams.SCROLL_LAYOUT_LEFT)
					|| this.checkViewScrollLayout(this.getDrawerView(), LayoutParams.SCROLL_LAYOUT_RIGHT);
		}
		return false;
	}

	@Override
	public boolean shouldStartNestedScroll() {
		return this.mIsDrawerEnabled && this.mIsEnsureDrawer;
	}

	@Override
	public boolean canChildScroll(int direction) {
		if (this.shouldStartNestedScroll()) {
			if (this.mIsOpenState) {
				if (this.canScrollHorizontally()) {
					return this.getDrawerView().canScrollHorizontally(direction);
				}
				if (this.canScrollVertically()) {
					return this.getDrawerView().canScrollVertically(direction);
				}
			}
			if ((this.getDrawerDirection() < 0 && direction < 0)
					|| (this.getDrawerDirection() > 0 && direction > 0)) {
				if (this.canScrollHorizontally()) {
					return this.getDragView().canScrollHorizontally(direction);
				}
				if (this.canScrollVertically()) {
					return this.getDragView().canScrollVertically(direction);
				}
			}
			return true;
		} else {
			if (this.canScrollHorizontally()) {
				return this.canScrollHorizontally(direction);
			}
			return this.canScrollVertically(direction);
		}
	}

	@Override
	public void onScrollBy(@NonNull NestedScrollingHelper helper, int dx, int dy, @NonNull int[] consumed) {
		final int mScrollOffsetX = helper.getScrollOffsetX();
		final int mScrollOffsetY = helper.getScrollOffsetY();
		int unconsumedX = dx;
		int unconsumedY = dy;
		int direction = 0;

		if (this.canScrollHorizontally()) {
			direction = Integer.compare(helper.getPreScrollDirection(dx), 0);
		} else if (this.canScrollVertically()) {
			direction = Integer.compare(helper.getPreScrollDirection(dy), 0);
		}
		if (NestedScrollingHelper.SCROLL_STATE_DRAGGING == helper.getScrollState()) {
			final int mScrollDistancePixelSize = this.getScrollDistancePixelSize();
			final int mPreScrollOffsetX = mScrollOffsetX + dx;
			final int mPreScrollOffsetY = mScrollOffsetY + dy;
			// If you need boundary detection
			if (this.canScrollHorizontally() && Math.abs(mPreScrollOffsetX) > Math.abs(mScrollDistancePixelSize)) {
				unconsumedX = mScrollDistancePixelSize - mScrollOffsetX;
			}
			if (this.canScrollVertically() && Math.abs(mPreScrollOffsetY) > Math.abs(mScrollDistancePixelSize)) {
				unconsumedY = mScrollDistancePixelSize - mScrollOffsetY;
			}
		}
		consumed[0] = unconsumedX;
		consumed[1] = unconsumedY;

		final int nowScrollOffsetX = (int) ((mScrollOffsetX + consumed[0]) * this.mFrictionRatio + NestedHelper.getDirectionDifference(direction));
		final int nowScrollOffsetY = (int) ((mScrollOffsetY + consumed[1]) * this.mFrictionRatio + NestedHelper.getDirectionDifference(direction));
		this.getDragView().setTranslationX(-nowScrollOffsetX);
		this.getDragView().setTranslationY(-nowScrollOffsetY);

		final View mDrawerView = this.getDrawerView();
		final int mDrawerDirection = this.getDrawerDirection();

		// direction same
		if (direction == mDrawerDirection) {
			if (mDrawerView.getVisibility() == View.INVISIBLE) {
				mDrawerView.setVisibility(View.VISIBLE);
			}
			this.onDrawerAnimation(mDrawerView, nowScrollOffsetX, nowScrollOffsetY, direction);
		} else {
			if (mDrawerView.getVisibility() == View.VISIBLE) {
				mDrawerView.setVisibility(View.INVISIBLE);
			}
		}
		if (this.mOnScrollListeners != null) {
			for (OnScrollListener listener : this.mOnScrollListeners) {
				listener.onScrolled(this, consumed[0], consumed[1]);
			}
		}
	}

	@Override
	public void onScrollStateChanged(@NonNull NestedScrollingHelper helper, int scrollState) {
		if (NestedScrollingHelper.SCROLL_STATE_IDLE == scrollState) {
			final int scrollDistance = this.getScrollDistancePixelSize();

			final int scrollOffset = helper.getScrollOffset();
			final int absScrollOffset = Math.abs(scrollOffset);
			final int absScrollDistance = Math.abs(scrollDistance);
			final boolean directionFlags = (scrollOffset < 0 && scrollDistance < 0) || (scrollOffset > 0 && scrollDistance > 0);

			if (this.mIsOpenState) {
				if (directionFlags && absScrollOffset <= absScrollDistance * (1 - this.mMinScrollScale)) {
					this.setOpenState(false);
					return;
				} else if (absScrollOffset != absScrollDistance) {
					this.setOpenState(true);
					return;
				}
			} else {
				if (directionFlags && absScrollOffset >= absScrollDistance * this.mMinScrollScale) {
					this.setOpenState(true);
					return;
				} else if (absScrollOffset != 0) {
					this.setOpenState(false);
					return;
				} else {
					this.getDrawerView().setVisibility(View.INVISIBLE);
				}
			}
		}
		// dispatch listener
		if (this.mOnScrollListeners != null) {
			for (OnScrollListener listener : this.mOnScrollListeners) {
				listener.onScrollStateChanged(this, scrollState);
			}
		}
	}

	protected void onDrawerAnimation(@NonNull View drawerView, int scrollOffsetX, int scrollOffsetY, int scrollDriection) {
		final float measuredWidth = drawerView.getMeasuredWidth();
		final float measuredHeight = drawerView.getMeasuredHeight();
		float transformPos = 0;

		if (this.canScrollHorizontally()) {
			transformPos = (float) (scrollOffsetX) / measuredWidth;
		} else if (this.canScrollVertically()) {
			transformPos = (float) (scrollOffsetY) / measuredHeight;
		}
		final float absTransformPos = Math.abs(transformPos);

		if (absTransformPos >= 1.F) {
			if (this.canScrollHorizontally()) {
				drawerView.setScaleX(absTransformPos);
				drawerView.setTranslationX((1.F - absTransformPos) * measuredWidth / 2.F * scrollDriection);
			} else if (this.canScrollVertically()) {
				drawerView.setScaleY(absTransformPos);
				drawerView.setTranslationY((1.F - absTransformPos) * measuredHeight / 2.F * scrollDriection);
			}
		} else {
			drawerView.setScaleX(1.F);
			drawerView.setScaleY(1.F);
			if (drawerView instanceof ViewGroup) {
				drawerView.setTranslationX(0.F);
				drawerView.setTranslationY(0.F);
			} else {
				if (this.canScrollHorizontally()) {
					drawerView.setTranslationX(measuredWidth * scrollDriection - measuredWidth * transformPos);
				} else if (this.canScrollVertically()) {
					drawerView.setTranslationY(measuredHeight * scrollDriection - measuredHeight * transformPos);
				}
			}
		}
		if (drawerView instanceof ViewGroup) {
			for (int index = 0; index < ((ViewGroup) drawerView).getChildCount(); index++) {
				final View child = ((ViewGroup) drawerView).getChildAt(index);
				if (this.canScrollHorizontally()) {
					child.setTranslationX(absTransformPos >= 1.F ? 0 : child.getWidth() * scrollDriection - child.getWidth() * transformPos);
				} else if (this.canScrollVertically()) {
					child.setTranslationY(absTransformPos >= 1.F ? 0 : child.getHeight() * scrollDriection - child.getHeight() * transformPos);
				}
			}
		}
	}

	public void setOnOpenStateListener(@NonNull OnOpenStateListener listener) {
		this.mOnOpenStateListener = listener;
	}

	public void addOnScrollListener(@NonNull OnScrollListener listener) {
		if (this.mOnScrollListeners == null) {
			this.mOnScrollListeners = new ArrayList<>();
		}
		this.mOnScrollListeners.add(listener);
	}

	public void removeOnScrollListener(@NonNull OnScrollListener listener) {
		if (this.mOnScrollListeners != null) {
			this.mOnScrollListeners.remove(listener);
		}
	}

	public void setDampSpringBackSize(@Px int dampSpringBackSize) {
		this.mDampSpringBackSize = dampSpringBackSize;
	}

	public void setMinScrollScale(@FloatRange(from = 0.01F, to = 1.F) float minScrollScale) {
		this.mMinScrollScale = minScrollScale;
	}

	public void setFrictionRatio(@FloatRange(from = 0.1F, to = 1.F) float frictionRatio) {
		this.mFrictionRatio = frictionRatio;
	}

	public void setDrawerEnabled(boolean enabled) {
		this.mIsDrawerEnabled = enabled;
	}

	public boolean isOpenState() {
		return this.mIsOpenState;
	}

	public boolean switchDrawer() {
		return this.switchDrawer(true);
	}

	public boolean switchDrawer(boolean smooth) {
		final boolean openState = !this.mIsOpenState;
		this.setOpenState(openState, smooth);
		return openState;
	}

	public void setOpenState(boolean openState) {
		this.setOpenState(openState, true);
	}

	public void setOpenState(boolean openState, boolean smooth) {
		boolean dampSpringBack = false;
		if (this.mIsOpenState != openState) {
			this.mIsOpenState = openState;
			// dispatch listener
			if (this.mOnOpenStateListener != null) {
				this.mOnOpenStateListener.onOpenStateChanged(this);
			}
			// 回弹效果
			dampSpringBack = openState;
		}
		if (openState) {
			this.openDrawer(smooth, dampSpringBack);
		} else {
			this.closeDrawer(smooth);
		}
	}

	private void openDrawer(boolean smooth, boolean dampSpringBack) {
		int deltaX = 0;
		int deltaY = 0;

		if (this.canScrollHorizontally()) {
			deltaX = this.getScrollDistancePixelSize();
		} else if (this.canScrollVertically()) {
			deltaY = this.getScrollDistancePixelSize();
		}
		if (smooth) {
			final int pixelSizeX = dampSpringBack ? (deltaX < 0 ? -this.mDampSpringBackSize : (deltaX > 0 ? this.mDampSpringBackSize : 0)) : 0;
			final int pixelSizeY = dampSpringBack ? (deltaY < 0 ? -this.mDampSpringBackSize : (deltaY > 0 ? this.mDampSpringBackSize : 0)) : 0;
			this.mNestedScrollingHelper.smoothScrollTo(deltaX + pixelSizeX, deltaY + pixelSizeY);
		} else {
			this.mNestedScrollingHelper.scrollTo(deltaX, deltaY);
		}
	}

	private void closeDrawer(boolean smooth) {
		if (smooth) {
			this.mNestedScrollingHelper.smoothScrollTo(0, 0);
		} else {
			this.mNestedScrollingHelper.scrollTo(0, 0);
		}
	}

	@NonNull
	public NestedScrollingHelper getNestedScrollingHelper() {
		return this.mNestedScrollingHelper;
	}

	private int getScrollDistancePixelSize() {
		final float distance;
		if (this.canScrollHorizontally()) {
			distance = this.getDrawerView().getMeasuredWidth();
		} else if (this.canScrollVertically()) {
			distance = this.getDrawerView().getMeasuredHeight();
		} else {
			distance = 0;
		}
		final int direction = this.getDrawerDirection();
		final float pixelSize = NestedHelper.getDirectionDifference(direction);
		return (int) (distance * direction / this.mFrictionRatio + pixelSize);
	}

	private View mDragView;
	private View mDrawerView;
	private boolean mIsEnsureDrawer;

	private void ensureDrawerView() {
		if (this.getChildCount() == 0) {
			return;
		}
		if (this.mDragView == null) {
			this.mDragView = this.findView(false);
		}
		if (this.mDrawerView == null) {
			this.mDrawerView = this.findView(true);
		}
		if (this.mDragView != null && this.mDrawerView != null) {
			if (!this.mIsOpenState && !this.mIsEnsureDrawer) {
				this.mDragView.setVisibility(View.VISIBLE);
				this.mDrawerView.setVisibility(View.INVISIBLE);
				this.mIsEnsureDrawer = true;
			}
		}
	}

	@NonNull
	private View getDragView() {
		if (this.mDragView == null) {
			throw new IllegalStateException("View " + this + " not dragView");
		}
		return this.mDragView;
	}

	@NonNull
	private View getDrawerView() {
		if (this.mDrawerView == null) {
			throw new IllegalStateException("View " + this + " not drawerView");
		}
		return this.mDrawerView;
	}

	@Nullable
	private View findView(boolean findDrawerView) {
		for (int index = 0; index < this.getChildCount(); index++) {
			final View child = this.getChildAt(index);
			if (View.GONE == child.getVisibility()) {
				continue;
			}
			final LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
			if (layoutParams.mScrollLayout == LayoutParams.SCROLL_LAYOUT_NONE) {
				if (findDrawerView) {
					continue;
				}
				return child;
			} else {
				if (findDrawerView) {
					return child;
				}
			}
		}
		return null;
	}

	@SuppressLint("RtlHardcoded")
	private int getDrawerDirection() {
		final View drawerView = this.getDrawerView();
		if (this.checkViewScrollLayout(drawerView, LayoutParams.SCROLL_LAYOUT_LEFT)
				|| this.checkViewScrollLayout(drawerView, LayoutParams.SCROLL_LAYOUT_TOP)) {
			return -1;
		}
		if (this.checkViewScrollLayout(drawerView, LayoutParams.SCROLL_LAYOUT_RIGHT)
				|| this.checkViewScrollLayout(drawerView, LayoutParams.SCROLL_LAYOUT_BOTTOM)) {
			return 1;
		}
		return 0;
	}

	private boolean checkViewScrollLayout(@NonNull View view, int scrollLayout) {
		return ((LayoutParams) view.getLayoutParams()).mScrollLayout == scrollLayout;
	}

	private static int getAbsoluteGravityByScrollLayout(int scrollLayout, int defGravity) {
		switch (scrollLayout) {
			case LayoutParams.SCROLL_LAYOUT_LEFT:
				return Gravity.START;
			case LayoutParams.SCROLL_LAYOUT_TOP:
				return Gravity.TOP;
			case LayoutParams.SCROLL_LAYOUT_RIGHT:
				return Gravity.END;
			case LayoutParams.SCROLL_LAYOUT_BOTTOM:
				return Gravity.BOTTOM;
			default:
				return defGravity;
		}
	}

	public interface OnOpenStateListener {

		void onOpenStateChanged(@NonNull SwipeSideLayout swipeSideLayout);
	}

	public interface OnScrollListener {

		void onScrolled(@NonNull SwipeSideLayout swipeSideLayout, int dx, int dy);

		/**
		 * @param scrollState The new scroll state.
		 * @see NestedScrollingHelper#SCROLL_STATE_IDLE
		 * @see NestedScrollingHelper#SCROLL_STATE_DRAGGING
		 * @see NestedScrollingHelper#SCROLL_STATE_SETTLING
		 */
		void onScrollStateChanged(@NonNull SwipeSideLayout swipeSideLayout, int scrollState);
	}

	public static class LayoutParams extends FrameLayout.LayoutParams {

		public static final int SCROLL_LAYOUT_NONE = 0;
		public static final int SCROLL_LAYOUT_LEFT = 1;
		public static final int SCROLL_LAYOUT_TOP = 2;
		public static final int SCROLL_LAYOUT_RIGHT = 3;
		public static final int SCROLL_LAYOUT_BOTTOM = 4;

		public int mScrollLayout = SCROLL_LAYOUT_NONE;

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(@NonNull LayoutParams source) {
			super(source);
			this.mScrollLayout = source.mScrollLayout;
		}

		public LayoutParams(@NonNull MarginLayoutParams source) {
			super(source);
		}

		public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
			super(source);
		}

		public LayoutParams(@NonNull Context context, @Nullable AttributeSet attrs) {
			super(context, attrs);
			final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeSideLayout_Layout);
			this.mScrollLayout = mTypedArray.getInteger(R.styleable.SwipeSideLayout_Layout_scrollLayout, SCROLL_LAYOUT_NONE);
			mTypedArray.recycle();
			// reset view layout_gravity
			this.gravity = getAbsoluteGravityByScrollLayout(this.mScrollLayout, this.gravity);
		}
	}

	public static class SavedState extends AbsSavedState {

		private boolean mIsOpenState;
		private Parcelable mSuperState;

		SavedState(Parcelable superState) {
			super(superState);
			this.mSuperState = superState;
		}

		SavedState(Parcel source, ClassLoader loader) {
			super(source, loader);
			if (loader == null) {
				loader = getClass().getClassLoader();
			}
			this.mIsOpenState = source.readInt() == 1;
			this.mSuperState = source.readParcelable(loader);
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(this.mIsOpenState ? 1 : 0);
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
