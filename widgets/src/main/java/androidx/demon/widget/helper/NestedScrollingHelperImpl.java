package androidx.demon.widget.helper;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Interpolator;
import android.widget.OverScroller;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.NestedScrollingParent2;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;

/**
 * Author create by ok on 2019-07-19
 * Email : ok@163.com.
 */
public class NestedScrollingHelperImpl implements NestedScrollingHelper {

	private static final boolean DEBUG = false;
	private static final String TAG = "Nested";

	private static final int INVALID_POINTER = -1;
	private static final int DEFAULT_GUTTER_SIZE = 16; // dips
	private static final int DEFAULT_CLOSE_ENOUGH = 2; // dips

	private static final int MAX_SCROLL_DURATION = 1000;

	private final NestedScrollingParentHelper mNestedScrollingParentHelper;
	private final NestedScrollingChildHelper mNestedScrollingChildHelper;
	private final ViewScroller mViewScroller;
	private final Callback mCallback;
	private final View mAnchorView;

	private ArrayList<OnScrollListener> mOnScrollListeners;

	public NestedScrollingHelperImpl(@NonNull ViewGroup anchorView, @NonNull Callback callback) {
		this.mNestedScrollingParentHelper = new NestedScrollingParentHelper(anchorView);
		this.mNestedScrollingChildHelper = new NestedScrollingChildHelper(anchorView);
		this.mNestedScrollingChildHelper.setNestedScrollingEnabled(true);
		this.mViewScroller = new ViewScroller(anchorView.getContext());
		this.mAnchorView = anchorView;
		this.mCallback = callback;

		final float density = anchorView.getResources().getDisplayMetrics().density;
		this.mGutterSize = (int) (DEFAULT_GUTTER_SIZE * density);
		this.mCloseEnough = (int) (DEFAULT_CLOSE_ENOUGH * density);

		final ViewConfiguration mViewConfiguration = ViewConfiguration.get(anchorView.getContext());
		this.mMaximumVelocity = mViewConfiguration.getScaledMaximumFlingVelocity();
		this.mMinimumVelocity = mViewConfiguration.getScaledMinimumFlingVelocity();
		this.mTouchSlop = mViewConfiguration.getScaledTouchSlop();
	}

	private final float mMaximumVelocity;
	private final float mMinimumVelocity;
	private final float mCloseEnough;
	private final float mGutterSize;
	private final float mTouchSlop;

	private boolean mIsBeingDragged;
	private boolean mIsScrollStarted;
	private int mScrollingDuration;

	private VelocityTracker mVelocityTracker;
	private float mInitialMotionX, mInitialMotionY;
	private float mLastTouchMotionX, mLastTouchMotionY;
	private int mActivePointerId = INVALID_POINTER;
	private int mScrollState = SCROLL_STATE_IDLE;

	private NestedScrollingStep mNestedScrollingStep;
	private final int[] mScrollConsumed = new int[2];
	private final int[] mScrollOffsetCount = new int[2];
	private final int[] mScrollOffset = new int[2];

	@Override
	public boolean onInterceptTouchEvent(@NonNull MotionEvent event) {
		if (!this.dispatchOnStartNestedScroll() || this.mNestedScrollInProgress) {
			return false;
		}
		final int mActionMasked = event.getActionMasked();
		if (mActionMasked != MotionEvent.ACTION_DOWN) {
			if (this.mIsBeingDragged) {
				return true;
			}
		}
		if (this.mVelocityTracker == null) {
			this.mVelocityTracker = VelocityTracker.obtain();
		}
		this.mVelocityTracker.addMovement(event);

		final boolean canScrollVertically = this.mCallback.canScrollVertically();
		final boolean canScrollHorizontally = this.mCallback.canScrollHorizontally();
		final OverScroller mOverScroller = this.mViewScroller.mOverScroller;
		int mPointerIndex;

		switch (mActionMasked) {
			case MotionEvent.ACTION_DOWN:
				this.mActivePointerId = event.getPointerId(0);
				mPointerIndex = event.findPointerIndex(this.mActivePointerId);
				if (mPointerIndex == -1) {
					return false;
				}
				this.mIsScrollStarted = true;
				this.mIsBeingDragged = false;
				this.mLastTouchMotionX = this.mInitialMotionX = event.getX(mPointerIndex);
				this.mLastTouchMotionY = this.mInitialMotionY = event.getY(mPointerIndex);
				mOverScroller.computeScrollOffset();

				if ((canScrollVertically && this.mScrollState == SCROLL_STATE_SETTLING
						&& Math.abs(mOverScroller.getFinalY() - mOverScroller.getCurrY()) > this.mCloseEnough)
						|| (canScrollHorizontally && this.mScrollState == SCROLL_STATE_SETTLING
						&& Math.abs(mOverScroller.getFinalX() - mOverScroller.getCurrX()) > this.mCloseEnough)) {
					this.mIsBeingDragged = true;
					this.mViewScroller.stopScrollInternal();
					this.setScrollState(SCROLL_STATE_DRAGGING);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				mPointerIndex = event.findPointerIndex(this.mActivePointerId);
				if (mPointerIndex == -1) {
					return false;
				}
				final float x = event.getX(mPointerIndex);
				final float y = event.getY(mPointerIndex);
				final int dx = (int) (this.mLastTouchMotionX - x + 0.5F);
				final int dy = (int) (this.mLastTouchMotionY - y + 0.5F);

				if (!this.isGutterDrag(this.mLastTouchMotionX, this.mLastTouchMotionY, dx, dy)
						&& this.canScroll(this.mAnchorView, (int) x, (int) y, dx, dy, false)
						&& ((canScrollHorizontally && dx != 0) || (canScrollVertically && dy != 0))) {
					// Nested view has scrollable area under this point. Let it be handled there.
					this.mLastTouchMotionX = x;
					this.mLastTouchMotionY = y;
					return false;
				}

				// check start dragging
				if (!this.mIsBeingDragged) {
					final int dxDiff = Math.abs(dx);
					final int dyDiff = Math.abs(dy);
					if (this.mCallback.canScrollHorizontally()) {
						if (!this.mCallback.canChildScroll(dx) && dxDiff > this.mTouchSlop && dxDiff * 0.5F > dyDiff && dx != 0) {
							this.mLastTouchMotionX = x;
							this.mLastTouchMotionY = y;
							this.mIsBeingDragged = true;
						}
					}
					if (this.mCallback.canScrollVertically()) {
						if (!this.mCallback.canChildScroll(dy) && dyDiff > this.mTouchSlop && dyDiff * 0.5F > dxDiff && dy != 0) {
							this.mLastTouchMotionX = x;
							this.mLastTouchMotionY = y;
							this.mIsBeingDragged = true;
						}
					}
					if (this.mIsBeingDragged) {
						this.setScrollState(SCROLL_STATE_DRAGGING);
					}
				}
				break;
			case MotionEvent.ACTION_UP:
				this.resetTouchEvent();
				break;
			case MotionEvent.ACTION_CANCEL:
				this.cancelTouchEvent();
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				final int mActionIndex = event.getActionIndex();
				this.mLastTouchMotionX = event.getX(mActionIndex);
				this.mLastTouchMotionY = event.getY(mActionIndex);
				this.mActivePointerId = event.getPointerId(mActionIndex);
				break;
			case MotionEvent.ACTION_POINTER_UP:
				this.secondaryPointerUp(event);
				break;
		}
		return this.mIsBeingDragged;
	}

	@Override
	public boolean onTouchEvent(@NonNull MotionEvent event) {
		if (!this.dispatchOnStartNestedScroll() || this.mNestedScrollInProgress) {
			return false;
		}
		if (this.mVelocityTracker == null) {
			this.mVelocityTracker = VelocityTracker.obtain();
		}
		this.mVelocityTracker.addMovement(event);

		final int mActionMasked = event.getActionMasked();
		int mPointerIndex;

		switch (mActionMasked) {
			case MotionEvent.ACTION_DOWN:
				this.mActivePointerId = event.getPointerId(0);
				mPointerIndex = event.findPointerIndex(this.mActivePointerId);
				if (mPointerIndex == -1) {
					return false;
				}
				this.mViewScroller.stopScrollInternal();
				// Remember where the motion event started
				this.mLastTouchMotionX = this.mInitialMotionX = event.getX(mPointerIndex);
				this.mLastTouchMotionY = this.mInitialMotionY = event.getY(mPointerIndex);
				break;
			case MotionEvent.ACTION_MOVE:
				mPointerIndex = event.findPointerIndex(this.mActivePointerId);
				if (mPointerIndex == -1) {
					return false;
				}

				final float x = event.getX(mPointerIndex);
				final float y = event.getY(mPointerIndex);
				int dx = (int) (this.mLastTouchMotionX - x + 0.5F);
				int dy = (int) (this.mLastTouchMotionY - y + 0.5F);

				// Now check start dragging
				if (!this.mIsBeingDragged) {
					final int dxDiff = Math.abs(dx);
					final int dyDiff = Math.abs(dy);
					if (this.mCallback.canScrollHorizontally()) {
						if (!this.mCallback.canChildScroll(dx) && dxDiff > this.mTouchSlop && dxDiff * 0.5F > dyDiff && dx != 0) {
							dx = (int) (dx > 0 ? dx - this.mTouchSlop : dx + this.mTouchSlop);
							this.mIsBeingDragged = true;
						}
					}
					if (this.mCallback.canScrollVertically()) {
						if (!this.mCallback.canChildScroll(dy) && dyDiff > this.mTouchSlop && dyDiff * 0.5F > dxDiff && dy != 0) {
							dy = (int) (dy > 0 ? dy - this.mTouchSlop : dy + this.mTouchSlop);
							this.mIsBeingDragged = true;
						}
					}
					if (this.mIsBeingDragged) {
						this.setScrollState(SCROLL_STATE_DRAGGING);
					}
				}
				if (!this.mIsBeingDragged) {
					return false;
				}
				if (this.mNestedScrollingStep != null && this.mNestedScrollingStep
						.dispatchNestedPreScroll(dx, dy, this.mScrollConsumed, this.mScrollOffset)) {
					dx -= this.mScrollConsumed[0];
					dy -= this.mScrollConsumed[1];
				}
				// Not else! Note that mIsBeingDragged can be set above.
				if (SCROLL_STATE_DRAGGING == this.mScrollState) {
					this.mLastTouchMotionX = x - this.mScrollOffset[0];
					this.mLastTouchMotionY = y - this.mScrollOffset[1];
					// Scroll to follow the motion event
					this.scrollByInternal(dx, dy, this.consumed, this.unconsumed);
					// Update the last touch co-ords, taking any scroll offset into account
					if (this.mNestedScrollingStep != null && this.mNestedScrollingStep
							.dispatchNestedScroll(this.consumed[0], this.consumed[1], this.unconsumed[0], this.unconsumed[1], this.mScrollOffset)) {
						this.mLastTouchMotionX -= this.mScrollOffset[0];
						this.mLastTouchMotionY -= this.mScrollOffset[1];
					}
				}
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				final int mActionIndex = event.getActionIndex();
				this.mLastTouchMotionX = event.getX(mActionIndex);
				this.mLastTouchMotionY = event.getY(mActionIndex);
				this.mActivePointerId = event.getPointerId(mActionIndex);
				break;
			case MotionEvent.ACTION_POINTER_UP:
				this.secondaryPointerUp(event);
				break;
			case MotionEvent.ACTION_UP:
				this.mVelocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
				final int velocityX = (int) (this.mVelocityTracker.getXVelocity(this.mActivePointerId) + 0.5F);
				final int velocityY = (int) (this.mVelocityTracker.getYVelocity(this.mActivePointerId) + 0.5F);
				if (!((velocityX != 0 || velocityY != 0) && this.fling(-velocityX, -velocityY))) {
					this.cancelTouchEvent();
				} else {
					this.resetTouchEvent();
				}
				return false;
			case MotionEvent.ACTION_CANCEL:
				this.cancelTouchEvent();
				return false;
		}
		return true;
	}

	@Override
	public boolean isNestedScrollInProgress() {
		return this.mNestedScrollInProgress;
	}

	private void resetTouchEvent() {
		this.mActivePointerId = INVALID_POINTER;
		this.mIsBeingDragged = false;
		if (this.mVelocityTracker != null) {
			this.mVelocityTracker.clear();
		}
		this.stopNestedScroll();
	}

	private void cancelTouchEvent() {
		this.resetTouchEvent();
		this.setScrollState(SCROLL_STATE_IDLE);
	}

	private final int[] consumed = new int[2];
	private final int[] unconsumed = new int[2];

	public final void scrollByInternal(int deltaX, int deltaY) {
		this.scrollByInternal(deltaX, deltaY, this.consumed);
	}

	public final void scrollByInternal(int deltaX, int deltaY, @NonNull int[] consumed) {
		this.scrollByInternal(deltaX, deltaY, consumed, this.unconsumed);
	}

	public final void scrollByInternal(int deltaX, int deltaY, @NonNull int[] consumed, @NonNull int[] unconsumed) {
		this.scrollStep(deltaX, deltaY, consumed, unconsumed);
	}

	protected void scrollStep(int deltaX, int deltaY, @NonNull int[] consumed, @NonNull int[] unconsumed) {
		if (DEBUG) {
			Log.e(TAG, "scrollStep dy " + deltaY);
		}
		Arrays.fill(consumed, 0);
		Arrays.fill(unconsumed, 0);

		if (deltaX != 0 || deltaY != 0) {
			final int dxConsumed = this.getScrollOffsetX();
			final int dyConsumed = this.getScrollOffsetY();
			int unconsumedX = 0;
			int unconsumedY = 0;

			if (this.mCallback.canScrollHorizontally()) {
				if (deltaX >= 0 && (dxConsumed + deltaX) >= 0 && this.mCallback.canChildScroll(1)) {
					unconsumedX = Math.min(deltaX, -dxConsumed);
				} else if (deltaX <= 0 && (dxConsumed + deltaX) <= 0 && this.mCallback.canChildScroll(-1)) {
					unconsumedX = Math.max(deltaX, -dxConsumed);
				} else {
					unconsumedX = deltaX;
				}
			}
			if (this.mCallback.canScrollVertically()) {
				if (deltaY >= 0 && (dyConsumed + deltaY) >= 0 && this.mCallback.canChildScroll(1)) {
					unconsumedY = Math.min(deltaY, -dyConsumed);
				} else if (deltaY <= 0 && (dyConsumed + deltaY) <= 0 && this.mCallback.canChildScroll(-1)) {
					unconsumedY = Math.max(deltaY, -dyConsumed);
				} else {
					unconsumedY = deltaY;
				}
			}
			if (DEBUG) {
				Log.e(TAG, "scrollStep unconsumedY " + unconsumedY);
			}
			if (unconsumedX != 0 || unconsumedY != 0) {
				this.mCallback.onScrollBy(this, unconsumedX, unconsumedY, consumed);

				this.mScrollOffsetCount[0] += consumed[0];
				this.mScrollOffsetCount[1] += consumed[1];
				if (consumed[0] != 0 || consumed[1] != 0) {
					this.dispatchOnScrolled(consumed[0], consumed[1]);
				}
			}
			unconsumed[0] = (deltaX - consumed[0]);
			unconsumed[1] = (deltaY - consumed[1]);
		}
	}

	private void setScrollState(int scrollState) {
		if (this.mScrollState != scrollState) {
			if (DEBUG) {
				Log.e(TAG, "setScrollState " + scrollState + " oldScrollState " + this.mScrollState);
			}
			this.mScrollState = scrollState;

			if (scrollState == SCROLL_STATE_DRAGGING) {
				this.requestParentDisallowInterceptTouchEvent(true);
			}
			if (scrollState == SCROLL_STATE_IDLE) {
				this.requestParentDisallowInterceptTouchEvent(false);
			}
			if (scrollState != SCROLL_STATE_SETTLING) {
				this.mViewScroller.stopScrollInternal();
			}
			this.mCallback.onScrollStateChanged(this, scrollState);
			// dispatch listener
			this.dispatchOnScrollStateChanged();
		}
	}

	private boolean isGutterDrag(float x, float y, float dx, float dy) {
		if (this.mCallback.canScrollVertically()) {
			return (y < this.mGutterSize && dy < 0) || (y > this.mAnchorView.getHeight() - this.mGutterSize && dy > 0);
		}
		return (x < this.mGutterSize && dx < 0) || (x > this.mAnchorView.getWidth() - this.mGutterSize && dx > 0);
	}

	private boolean canScroll(View view, int x, int y, int dx, int dy, boolean checkView) {
		if (view instanceof ViewGroup) {
			final ViewGroup group = (ViewGroup) view;
			final int scrollX = view.getScrollX();
			final int scrollY = view.getScrollY();
			for (int index = group.getChildCount() - 1; index >= 0; index--) {
				final View child = group.getChildAt(index);
				if (x + scrollX >= child.getLeft()
						&& x + scrollX < child.getRight()
						&& y + scrollY >= child.getTop()
						&& y + scrollY < child.getBottom()
						&& this.canScroll(child, x + scrollX - child.getLeft(), y + scrollY - child.getTop(), dx, dy, true)) {
					return true;
				}
			}
		}
		return checkView && (view.canScrollHorizontally(dx) || view.canScrollVertically(dy));
	}

	private void secondaryPointerUp(@NonNull MotionEvent event) {
		final int pointerIndex = event.getActionIndex();
		final int pointerId = event.getPointerId(pointerIndex);
		if (pointerId == this.mActivePointerId) {
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			this.mLastTouchMotionX = event.getX(newPointerIndex);
			this.mLastTouchMotionY = event.getY(newPointerIndex);
			this.mActivePointerId = event.getPointerId(newPointerIndex);
			if (this.mVelocityTracker != null) {
				this.mVelocityTracker.clear();
			}
		}
	}

	private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
		if (this.mAnchorView.getParent() == null) {
			return;
		}
		this.mAnchorView.getParent().requestDisallowInterceptTouchEvent(disallowIntercept);
	}

	@Override
	public void addOnScrollListener(@NonNull OnScrollListener listener) {
		if (this.mOnScrollListeners == null) {
			this.mOnScrollListeners = new ArrayList<>();
		}
		this.mOnScrollListeners.add(listener);
	}

	@Override
	public void removeOnScrollListener(@NonNull OnScrollListener listener) {
		if (this.mOnScrollListeners != null) {
			this.mOnScrollListeners.remove(listener);
		}
	}

	@Override
	public void setNestedScrollingStep(@NonNull NestedScrollingStep step) {
		if (this.mNestedScrollingStep != null) {
			this.mNestedScrollingStep.setNestedScrollingHelper(null, null);
		}
		this.mNestedScrollingStep = step;
		this.mNestedScrollingStep.setNestedScrollingHelper(this, this.mAnchorView);
	}

	@Override
	public void setScrollingDuration(int duration) {
		this.mScrollingDuration = duration;
	}

	@Override
	public void stopScroll() {
		this.mViewScroller.stopScrollInternal();
		this.setScrollState(SCROLL_STATE_IDLE);
	}

	@Override
	public void scrollTo(int x, int y) {
		this.mViewScroller.scrollTo(x, y);
	}

	@Override
	public void scrollTo(final int x, final int y, long delayMillis) {
		this.mAnchorView.postDelayed(new Runnable() {
			@Override
			public void run() {
				NestedScrollingHelperImpl.this.mViewScroller.scrollTo(x, y);
			}
		}, delayMillis);
	}

	@Override
	public void smoothScrollTo(int x, int y) {
		this.mViewScroller.smoothScrollTo(x, y);
	}

	@Override
	public void smoothScrollTo(final int x, final int y, long delayMillis) {
		this.mAnchorView.postDelayed(new Runnable() {
			@Override
			public void run() {
				NestedScrollingHelperImpl.this.mViewScroller.smoothScrollTo(x, y);
			}
		}, delayMillis);
	}

	@Override
	public int getPreScrollDirection(int delta) {
		return Integer.compare(this.getScrollOffset() + delta, 0);
	}

	@Override
	public int getScrollDirection() {
		return Integer.compare(this.getScrollOffset(), 0);
	}

	@Override
	public int getScrollState() {
		return this.mScrollState;
	}

	@Override
	public int getScrollOffset() {
		if (this.mCallback.canScrollHorizontally()) {
			return this.getScrollOffsetX();
		} else if (this.mCallback.canScrollVertically()) {
			return this.getScrollOffsetY();
		}
		return 0;
	}

	@Override
	public int getScrollOffsetX() {
		return this.mScrollOffsetCount[0];
	}

	@Override
	public int getScrollOffsetY() {
		return this.mScrollOffsetCount[1];
	}

	@NonNull
	@Override
	public Callback getCallback() {
		return this.mCallback;
	}

	protected boolean fling(float velocityX, float velocityY) {
//		if (this.mIsBeingDragged) {
//			return this.mViewScroller.fling(velocityX, velocityY);
//		}
		return false;
	}

	private boolean dispatchOnStartNestedScroll() {
		if (this.mCallback == null
				|| this.mAnchorView == null) {
			return false;
		}
		return this.mAnchorView.isEnabled()
				&& this.mAnchorView.isShown()
				&& this.mCallback.shouldStartNestedScroll();
	}

	private void dispatchOnScrollStateChanged() {
		if (this.mOnScrollListeners != null) {
			for (OnScrollListener listener : this.mOnScrollListeners) {
				listener.onScrollStateChanged(this, this.mScrollState);
			}
		}
	}

	private void dispatchOnScrolled(int dx, int dy) {
		if (this.mOnScrollListeners != null) {
			for (OnScrollListener listener : this.mOnScrollListeners) {
				listener.onScrolled(this, dx, dy);
			}
		}
	}

	final class ViewScroller implements Runnable {

		private final Interpolator sQuinticInterpolator = new Interpolator() {

			@Override
			public float getInterpolation(float t) {
				t -= 1.0f;
				return t * t * t * t * t + 1.0f;
			}
		};

		private final OverScroller mOverScroller;
		private final float mMinimumVelocity;
		private final float mMaximumVelocity;

		private final int[] consumed = new int[2];
		private final int[] unconsumed = new int[2];

		private int mOldScrollX;
		private int mOldScrollY;

		ViewScroller(@NonNull Context context) {
			this.mOverScroller = new OverScroller(context, this.sQuinticInterpolator);

			final ViewConfiguration mViewConfiguration = ViewConfiguration.get(context);
			this.mMinimumVelocity = mViewConfiguration.getScaledMinimumFlingVelocity();
			this.mMaximumVelocity = mViewConfiguration.getScaledMaximumFlingVelocity();
		}

		@Override
		public void run() {
			NestedScrollingHelperImpl.this.mIsScrollStarted = true;
			if (this.mOverScroller.computeScrollOffset()) {
				final int currX = this.mOverScroller.getCurrX();
				final int currY = this.mOverScroller.getCurrY();

				int unconsumedX = (currX - this.mOldScrollX);
				int unconsumedY = (currY - this.mOldScrollY);
				this.mOldScrollX = currX;
				this.mOldScrollY = currY;

				// rollback scrolling
				NestedScrollingHelperImpl.this.scrollByInternal(unconsumedX, unconsumedY, this.consumed, this.unconsumed);

				if (!NestedScrollingHelperImpl.this.dispatchNestedScroll(this.consumed[0], this.consumed[1], this.unconsumed[0], this.unconsumed[1], null, ViewCompat.TYPE_NON_TOUCH)
						&& (this.unconsumed[0] != 0 || this.unconsumed[1] != 0)) {
					final int mCurrVelocity = (int) this.mOverScroller.getCurrVelocity();

					int velocityX = 0;
					if (this.unconsumed[0] != currX) {
						velocityX = this.unconsumed[0] < 0 ? -mCurrVelocity : this.unconsumed[0] > 0 ? mCurrVelocity : 0;
					}

					int velocityY = 0;
					if (this.unconsumed[1] != currY) {
						velocityY = this.unconsumed[1] < 0 ? -mCurrVelocity : this.unconsumed[1] > 0 ? mCurrVelocity : 0;
					}

					if ((velocityX != 0 || this.unconsumed[0] == currX || this.mOverScroller.getFinalX() == 0)
							&& (velocityY != 0 || this.unconsumed[1] == currY || this.mOverScroller.getFinalY() == 0)) {
						this.mOverScroller.abortAnimation();
					}
				}

				final boolean canScrollVertically = NestedScrollingHelperImpl.this.mCallback.canScrollVertically();
				final boolean canScrollHorizontally = NestedScrollingHelperImpl.this.mCallback.canScrollHorizontally();

				final boolean fullyConsumedVertical = unconsumedY != 0 && canScrollVertically
						&& this.consumed[1] == unconsumedY;
				final boolean fullyConsumedHorizontal = unconsumedX != 0 && canScrollHorizontally
						&& this.consumed[0] == unconsumedX;
				final boolean fullyConsumedAny = (unconsumedX == 0 && unconsumedY == 0) || fullyConsumedHorizontal
						|| fullyConsumedVertical;

				if (DEBUG) {
					Log.e(TAG, "smoothScrollTo " + this.consumed[1] + " => " + fullyConsumedAny);
				}

				if (this.mOverScroller.isFinished() || (!fullyConsumedAny && !NestedScrollingHelperImpl.this.hasNestedScrollingParent())) {
					// setting state to idle will stop this.
					NestedScrollingHelperImpl.this.stopNestedScroll();
					// reset scrollState
					NestedScrollingHelperImpl.this.setScrollState(SCROLL_STATE_IDLE);
				} else {
					this.postOnAnimation(this);
				}
			}
		}

		private boolean fling(float velocityX, float velocityY) {
			final boolean canScrollHorizontal = NestedScrollingHelperImpl.this.mCallback.canScrollHorizontally();
			final boolean canScrollVertical = NestedScrollingHelperImpl.this.mCallback.canScrollVertically();

			if (!canScrollHorizontal || Math.abs(velocityX) < this.mMinimumVelocity) {
				velocityX = 0;
			}
			if (!canScrollVertical || Math.abs(velocityY) < this.mMinimumVelocity) {
				velocityY = 0;
			}
			if (velocityX == 0 && velocityY == 0) {
				// If we don't have any velocity, return false
				return false;
			}
			if (!NestedScrollingHelperImpl.this.dispatchNestedPreFling(velocityX, velocityY)) {
				NestedScrollingHelperImpl.this.dispatchNestedFling(velocityX, velocityY, true);

				int nestedScrollAxis = ViewCompat.SCROLL_AXIS_NONE;
				if (canScrollHorizontal) {
					nestedScrollAxis |= ViewCompat.SCROLL_AXIS_HORIZONTAL;
				}
				if (canScrollVertical) {
					nestedScrollAxis |= ViewCompat.SCROLL_AXIS_VERTICAL;
				}
				NestedScrollingHelperImpl.this.startNestedScroll(nestedScrollAxis);
				velocityX = Math.max(-this.mMaximumVelocity, Math.min(velocityX, this.mMaximumVelocity));
				velocityY = Math.max(-this.mMaximumVelocity, Math.min(velocityY, this.mMaximumVelocity));

				int scrollX;
				int scrollY;

				if (this.mOverScroller.isFinished()) {
					scrollX = NestedScrollingHelperImpl.this.mScrollOffsetCount[0];
					scrollY = NestedScrollingHelperImpl.this.mScrollOffsetCount[1];
				} else {
					scrollX = NestedScrollingHelperImpl.this.mIsScrollStarted ? this.mOverScroller.getCurrX() : this.mOverScroller.getStartX();
					scrollY = NestedScrollingHelperImpl.this.mIsScrollStarted ? this.mOverScroller.getCurrY() : this.mOverScroller.getStartY();
					// And abort the current scrolling.
					this.mOverScroller.abortAnimation();
				}

				NestedScrollingHelperImpl.this.setScrollState(SCROLL_STATE_SETTLING);
				// Reset the "scroll started" flag. It will be flipped to true in all places
				// where we call computeScrollOffset().
				NestedScrollingHelperImpl.this.mIsScrollStarted = false;
				this.mOldScrollX = NestedScrollingHelperImpl.this.mScrollOffsetCount[0];
				this.mOldScrollY = NestedScrollingHelperImpl.this.mScrollOffsetCount[1];
				this.mOverScroller.fling(scrollX, scrollY, (int) velocityX, (int) velocityY,
						Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE);
				this.postOnAnimation(this);
				return true;
			}
			return false;
		}

		private boolean scrollTo(int x, int y) {
			return this.smoothScrollTo(x, y, 0);
		}

		private boolean smoothScrollTo(int x, int y) {
			if (NestedScrollingHelperImpl.this.mScrollingDuration > 0) {
				return this.smoothScrollTo(x, y, NestedScrollingHelperImpl.this.mScrollingDuration);
			} else {
				return this.smoothScrollTo(x, y, 0, 0);
			}
		}

		private boolean smoothScrollTo(int x, int y, int velocityX, int velocityY) {
			return this.smoothScrollTo(x, y, this.computeScrollDuration(x, y, velocityX, velocityY));
		}

		private boolean smoothScrollTo(int x, int y, int duration) {
			int scrollX;
			int scrollY;

			if (this.mOverScroller.isFinished()) {
				scrollX = NestedScrollingHelperImpl.this.mScrollOffsetCount[0];
				scrollY = NestedScrollingHelperImpl.this.mScrollOffsetCount[1];
			} else {
				scrollX = NestedScrollingHelperImpl.this.mIsScrollStarted ? this.mOverScroller.getCurrX() : this.mOverScroller.getStartX();
				scrollY = NestedScrollingHelperImpl.this.mIsScrollStarted ? this.mOverScroller.getCurrY() : this.mOverScroller.getStartY();
				// And abort the current scrolling.
				this.mOverScroller.abortAnimation();
			}
			final int dx = x - scrollX;
			final int dy = y - scrollY;

			if (dx == 0 && dy == 0) {
				// setting state to idle will stop this.
				NestedScrollingHelperImpl.this.stopNestedScroll();
				// reset scrollState
				NestedScrollingHelperImpl.this.setScrollState(SCROLL_STATE_IDLE);
			} else {
				NestedScrollingHelperImpl.this.setScrollState(SCROLL_STATE_SETTLING);

				// Reset the "scroll started" flag. It will be flipped to true in all places
				// where we call computeScrollOffset().
				NestedScrollingHelperImpl.this.mIsScrollStarted = false;
				this.mOldScrollX = NestedScrollingHelperImpl.this.mScrollOffsetCount[0];
				this.mOldScrollY = NestedScrollingHelperImpl.this.mScrollOffsetCount[1];
				this.mOverScroller.startScroll(scrollX, scrollY, dx, dy, duration);
				this.postOnAnimation(this);
				return true;
			}
			return false;
		}

		private void stopScrollInternal() {
			this.mOverScroller.abortAnimation();
			NestedScrollingHelperImpl.this.mAnchorView.removeCallbacks(this);
		}

		private void postOnAnimation(@NonNull Runnable runnable) {
			NestedScrollingHelperImpl.this.mAnchorView.removeCallbacks(runnable);
			ViewCompat.postOnAnimation(NestedScrollingHelperImpl.this.mAnchorView, runnable);
		}

		private float distanceInfluenceForSnapDuration(float f) {
			f -= 0.5f; // center the values about 0.
			f *= 0.3f * (float) Math.PI / 2.0f;
			return (float) Math.sin(f);
		}

		private int computeScrollDuration(int x, int y, int velocityX, int velocityY) {
			int scrollX;
			int scrollY;

			if (this.mOverScroller.isFinished()) {
				scrollX = NestedScrollingHelperImpl.this.mScrollOffsetCount[0];
				scrollY = NestedScrollingHelperImpl.this.mScrollOffsetCount[1];
			} else {
				scrollX = NestedScrollingHelperImpl.this.mIsScrollStarted ? this.mOverScroller.getCurrX() : this.mOverScroller.getStartX();
				scrollY = NestedScrollingHelperImpl.this.mIsScrollStarted ? this.mOverScroller.getCurrY() : this.mOverScroller.getStartY();
			}
			final int dx = x - scrollX;
			final int dy = y - scrollY;

			final int width = NestedScrollingHelperImpl.this.mAnchorView.getWidth();
			final int height = NestedScrollingHelperImpl.this.mAnchorView.getHeight();
			final int absDx = Math.abs(dx);
			final int absDy = Math.abs(dy);
			final boolean horizontal = absDx > absDy;
			final int delta = (int) Math.sqrt(dx * dx + dy * dy);
			final int velocity = (int) Math.sqrt(velocityX * velocityX + velocityY * velocityY);
			final int containerSize = horizontal ? width : height;
			final int halfContainerSize = containerSize / 2;
			final float distanceRatio = Math.min(1.f, 1.f * delta / containerSize);
			final float distance = halfContainerSize + halfContainerSize * this.distanceInfluenceForSnapDuration(distanceRatio);

			final int duration;
			if (velocity > 0) {
				duration = 4 * Math.round(1000 * Math.abs(distance / velocity));
			} else {
				float absDelta = (float) (horizontal ? absDx : absDy);
				duration = (int) (((absDelta / containerSize) + 1) * 300);
			}
			return Math.min(duration, MAX_SCROLL_DURATION);
		}
	}

	private final int[] mParentScrollConsumed = new int[2];
	private final int[] mParentOffsetInWindow = new int[2];
	private boolean mNestedScrollInProgress;

	// NestedScrollingParent

	/**
	 * React to a descendant view initiating a nestable scroll operation, claiming the
	 * nested scroll operation if appropriate.
	 *
	 * <p>This method will be called in response to a descendant view invoking
	 * {@link ViewCompat#startNestedScroll(View, int)}. Each parent up the view hierarchy will be
	 * given an opportunity to respond and claim the nested scrolling operation by returning
	 * <code>true</code>.</p>
	 *
	 * <p>This method may be overridden by ViewParent implementations to indicate when the view
	 * is willing to support a nested scrolling operation that is about to begin. If it returns
	 * true, this ViewParent will become the target view's nested scrolling parent for the duration
	 * of the scroll operation in progress. When the nested scroll is finished this ViewParent
	 * will receive a call to {@link #onStopNestedScroll(View)}.
	 * </p>
	 *
	 * @param child            Direct child of this ViewParent containing target
	 * @param target           View that initiated the nested scroll
	 * @param nestedScrollAxes Flags consisting of {@link ViewCompat#SCROLL_AXIS_HORIZONTAL},
	 *                         {@link ViewCompat#SCROLL_AXIS_VERTICAL} or both
	 * @return true if this ViewParent accepts the nested scroll operation
	 */
	@Override
	public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int nestedScrollAxes) {
		return ((nestedScrollAxes & ViewCompat.SCROLL_AXIS_VERTICAL) != 0 && this.mCallback.canScrollVertically())
				|| ((nestedScrollAxes & ViewCompat.SCROLL_AXIS_HORIZONTAL) != 0 && this.mCallback.canScrollHorizontally());
	}

	/**
	 * React to the successful claiming of a nested scroll operation.
	 *
	 * <p>This method will be called after
	 * {@link #onStartNestedScroll(View, View, int) onStartNestedScroll} returns true. It offers
	 * an opportunity for the view and its superclasses to perform initial configuration
	 * for the nested scroll. Implementations of this method should always call their superclass's
	 * implementation of this method if one is present.</p>
	 *
	 * @param child            Direct child of this ViewParent containing target
	 * @param target           View that initiated the nested scroll
	 * @param nestedScrollAxes Flags consisting of {@link ViewCompat#SCROLL_AXIS_HORIZONTAL},
	 *                         {@link ViewCompat#SCROLL_AXIS_VERTICAL} or both
	 * @see #onStartNestedScroll(View, View, int)
	 * @see #onStopNestedScroll(View)
	 */
	@Override
	public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int nestedScrollAxes) {
		this.mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
		// Start nested scroll touch
		this.mNestedScrollInProgress = true;
		// Dispatch up to the nested parent
		this.startNestedScroll(nestedScrollAxes);
	}

	/**
	 * React to a nested scroll operation ending.
	 *
	 * <p>Perform cleanup after a nested scrolling operation.
	 * This method will be called when a nested scroll stops, for example when a nested touch
	 * scroll ends with a {@link MotionEvent#ACTION_UP} or {@link MotionEvent#ACTION_CANCEL} event.
	 * Implementations of this method should always call their superclass's implementation of this
	 * method if one is present.</p>
	 *
	 * @param target View that initiated the nested scroll
	 */
	@Override
	public void onStopNestedScroll(@NonNull View target) {
		this.mNestedScrollingParentHelper.onStopNestedScroll(target);
		// Stop nested scroll touch
		this.mNestedScrollInProgress = false;
		// Dispatch up our nested parent
		this.stopNestedScroll();
		// Finish the spinner for nested scrolling if we ever consumed any
		// unconsumed nested scroll
		this.setScrollState(SCROLL_STATE_IDLE);
	}

	/**
	 * React to a nested scroll in progress.
	 *
	 * <p>This method will be called when the ViewParent's current nested scrolling child view
	 * dispatches a nested scroll event. To receive calls to this method the ViewParent must have
	 * previously returned <code>true</code> for a call to
	 * {@link #onStartNestedScroll(View, View, int)}.</p>
	 *
	 * <p>Both the consumed and unconsumed portions of the scroll distance are reported to the
	 * ViewParent. An implementation may choose to use the consumed portion to match or chase scroll
	 * position of multiple child elements, for example. The unconsumed portion may be used to
	 * allow continuous dragging of multiple scrolling or draggable elements, such as scrolling
	 * a list within a vertical drawer where the drawer begins dragging once the edge of inner
	 * scrolling content is reached.</p>
	 *
	 * @param target       The descendent view controlling the nested scroll
	 * @param dxConsumed   Horizontal scroll distance in pixels already consumed by target
	 * @param dyConsumed   Vertical scroll distance in pixels already consumed by target
	 * @param dxUnconsumed Horizontal scroll distance in pixels not consumed by target
	 * @param dyUnconsumed Vertical scroll distance in pixels not consumed by target
	 */
	@Override
	public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
		if (!this.hasNestedScrollingParent()) {
			this.startNestedScroll();
		}
		this.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, this.mParentOffsetInWindow);
		if (DEBUG) {
			Log.e(TAG, "onNestedScroll " + dyConsumed + " , " + dyUnconsumed + " , " + this.mParentOffsetInWindow[1]);
		}
		if (this.dispatchOnStartNestedScroll()) {
			final int dx = dxUnconsumed + this.mParentOffsetInWindow[0];
			final int dy = dyUnconsumed + this.mParentOffsetInWindow[1];

			int unconsumedX = 0;
			int unconsumedY = 0;
			if (!this.mCallback.canChildScroll(dx) && dx != 0) {
				unconsumedX = dx;
			}
			if (!this.mCallback.canChildScroll(dy) && dy != 0) {
				unconsumedY = dy;
			}
			if (unconsumedX != 0 || unconsumedY != 0) {
				this.setScrollState(SCROLL_STATE_DRAGGING);
				this.scrollByInternal(unconsumedX, unconsumedY);
			}
		}
	}

	/**
	 * React to a nested scroll in progress before the target view consumes a portion of the scroll.
	 *
	 * <p>When working with nested scrolling often the parent view may want an opportunity
	 * to consume the scroll before the nested scrolling child does. An example of this is a
	 * drawer that contains a scrollable list. The user will want to be able to scroll the list
	 * fully into view before the list itself begins scrolling.</p>
	 *
	 * <p><code>onNestedPreScroll</code> is called when a nested scrolling child invokes
	 * {@link View#dispatchNestedPreScroll(int, int, int[], int[])}. The implementation should
	 * report how any pixels of the scroll reported by dx, dy were consumed in the
	 * <code>consumed</code> array. Index 0 corresponds to dx and index 1 corresponds to dy.
	 * This parameter will never be null. Initial values for consumed[0] and consumed[1]
	 * will always be 0.</p>
	 *
	 * @param target   View that initiated the nested scroll
	 * @param dx       Horizontal scroll distance in pixels
	 * @param dy       Vertical scroll distance in pixels
	 * @param consumed Output. The horizontal and vertical scroll distance consumed by this parent
	 */
	@Override
	public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
		if (SCROLL_STATE_DRAGGING == this.mScrollState) {
			if (this.dispatchOnStartNestedScroll()) {
				final int dxConsumed = this.getScrollOffsetX();
				final int dyConsumed = this.getScrollOffsetY();

				int unconsumedX = 0;
				int unconsumedY = 0;
				if (dx > 0 && dxConsumed < 0) {
					if (dx + dxConsumed > 0) {
						consumed[0] = dx + dxConsumed;
						unconsumedX = -dxConsumed;
					} else {
						consumed[0] = dx;
						unconsumedX = dx;
					}
				} else if (dx < 0 && dxConsumed > 0) {
					if (dx + dxConsumed > 0) {
						consumed[0] = dx;
						unconsumedX = dx;
					} else {
						consumed[0] = dx + dxConsumed;
						unconsumedX = -dxConsumed;
					}
				}
				if (dy > 0 && dyConsumed < 0) {
					if (dy + dyConsumed > 0) {
						consumed[1] = dy + dyConsumed;
						unconsumedY = -dyConsumed;
					} else {
						consumed[1] = dy;
						unconsumedY = dy;
					}
				} else if (dy < 0 && dyConsumed > 0) {
					if (dy + dyConsumed > 0) {
						consumed[1] = dy;
						unconsumedY = dy;
					} else {
						consumed[1] = dy + dyConsumed;
						unconsumedY = -dyConsumed;
					}
				}
				if (unconsumedX != 0 || unconsumedY != 0) {
					this.scrollByInternal(unconsumedX, unconsumedY, this.consumed, this.unconsumed);
					consumed[0] -= this.unconsumed[0];
					consumed[1] -= this.unconsumed[1];
				}
			}
		}
		if (DEBUG) {
			Log.e(TAG, "onNestedPreScroll Before" + (dy - consumed[1] + " , " + this.mParentScrollConsumed[1]) + " , " + this.mNestedScrollInProgress);
		}
		if (!this.hasNestedScrollingParent()) {
			this.startNestedScroll();
		}
		final int[] parentConsumed = this.mParentScrollConsumed;
		if (this.dispatchNestedPreScroll(dx - consumed[0], dy - consumed[1], parentConsumed, null)) {
			consumed[0] += parentConsumed[0];
			consumed[1] += parentConsumed[1];
			if (DEBUG) {
				Log.e(TAG, "onNestedPreScrolling " + parentConsumed[1]);
			}
		}
		if (DEBUG) {
			Log.e(TAG, "onNestedPreScroll After " + (dy - consumed[1] + " , " + this.mParentScrollConsumed[1]));
		}
	}

	/**
	 * Request a fling from a nested scroll.
	 *
	 * <p>This method signifies that a nested scrolling child has detected suitable conditions
	 * for a fling. Generally this means that a touch scroll has ended with a
	 * {@link VelocityTracker velocity} in the direction of scrolling that meets or exceeds
	 * the {@link ViewConfiguration#getScaledMinimumFlingVelocity() minimum fling velocity}
	 * along a scrollable axis.</p>
	 *
	 * <p>If a nested scrolling child view would normally fling but it is at the edge of
	 * its own content, it can use this method to delegate the fling to its nested scrolling
	 * parent instead. The parent may optionally consume the fling or observe a child fling.</p>
	 *
	 * @param target    View that initiated the nested scroll
	 * @param velocityX Horizontal velocity in pixels per second
	 * @param velocityY Vertical velocity in pixels per second
	 * @param consumed  true if the child consumed the fling, false otherwise
	 * @return true if this parent consumed or otherwise reacted to the fling
	 */
	@Override
	public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
//		if (!consumed) {
//			this.dispatchNestedFling(velocityX, velocityY, false);
//			velocityX = Math.max(-80, Math.min((int) velocityX, 80));
//			velocityY = Math.max(-80, Math.min((int) velocityY, 80));
//			return this.mViewScroller.fling(velocityX, velocityY);
//		}
//		return this.dispatchNestedFling(velocityX, velocityY, true);
		return this.dispatchNestedFling(velocityX, velocityY, consumed);
	}

	/**
	 * React to a nested fling before the target view consumes it.
	 *
	 * <p>This method siginfies that a nested scrolling child has detected a fling with the given
	 * velocity along each axis. Generally this means that a touch scroll has ended with a
	 * {@link VelocityTracker velocity} in the direction of scrolling that meets or exceeds
	 * the {@link ViewConfiguration#getScaledMinimumFlingVelocity() minimum fling velocity}
	 * along a scrollable axis.</p>
	 *
	 * <p>If a nested scrolling parent is consuming motion as part of a
	 * {@link #onNestedPreScroll(View, int, int, int[]) pre-scroll}, it may be appropriate for
	 * it to also consume the pre-fling to complete that same motion. By returning
	 * <code>true</code> from this method, the parent indicates that the child should not
	 * fling its own internal content as well.</p>
	 *
	 * @param target    View that initiated the nested scroll
	 * @param velocityX Horizontal velocity in pixels per second
	 * @param velocityY Vertical velocity in pixels per second
	 * @return true if this parent consumed the fling ahead of the target view
	 */
	@Override
	public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
//		final int dxConsumed = this.getScrollOffsetX();
//		final int dyConsumed = this.getScrollOffsetY();

		int consumedX = 0;
		int consumedY = 0;

//		if ((this.mCallback.canScrollVertically() && dyConsumed != 0)
//				|| (this.mCallback.canScrollHorizontally() && dxConsumed != 0)) {
//			consumedX = -dxConsumed;
//			consumedY = -dyConsumed;
//			this.smoothScrollTo(consumedX, consumedY);
//			return true;
//		}
		return this.dispatchNestedPreFling(velocityX - consumedX, velocityY - consumedY);
	}

	/**
	 * Return the current axes of nested scrolling for this NestedScrollingParent.
	 *
	 * <p>A NestedScrollingParent returning something other than {@link ViewCompat#SCROLL_AXIS_NONE}
	 * is currently acting as a nested scrolling parent for one or more descendant views in
	 * the hierarchy.</p>
	 *
	 * @return Flags indicating the current axes of nested scrolling
	 * @see ViewCompat#SCROLL_AXIS_HORIZONTAL
	 * @see ViewCompat#SCROLL_AXIS_VERTICAL
	 * @see ViewCompat#SCROLL_AXIS_NONE
	 */
	@Override
	public int getNestedScrollAxes() {
		return this.mNestedScrollingParentHelper.getNestedScrollAxes();
	}

	// NestedScrollingChild

	/**
	 * Enable or disable nested scrolling for this view.
	 *
	 * <p>If this property is set to true the view will be permitted to initiate nested
	 * scrolling operations with a compatible parent view in the current hierarchy. If this
	 * view does not implement nested scrolling this will have no effect. Disabling nested scrolling
	 * while a nested scroll is in progress has the effect of {@link #stopNestedScroll() stopping}
	 * the nested scroll.</p>
	 *
	 * @param enabled true to enable nested scrolling, false to disable
	 * @see #isNestedScrollingEnabled()
	 */
	@Override
	public void setNestedScrollingEnabled(boolean enabled) {
		this.mNestedScrollingChildHelper.setNestedScrollingEnabled(enabled);
	}

	/**
	 * Returns true if nested scrolling is enabled for this view.
	 *
	 * <p>If nested scrolling is enabled and this View class implementation supports it,
	 * this view will act as a nested scrolling child view when applicable, forwarding data
	 * about the scroll operation in progress to a compatible and cooperating nested scrolling
	 * parent.</p>
	 *
	 * @return true if nested scrolling is enabled
	 * @see #setNestedScrollingEnabled(boolean)
	 */
	@Override
	public boolean isNestedScrollingEnabled() {
		return this.mNestedScrollingChildHelper.isNestedScrollingEnabled();
	}

	@Override
	public boolean startNestedScroll() {
		if (this.mCallback.canScrollHorizontally()) {
			return this.startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL);
		} else if (this.mCallback.canScrollVertically()) {
			return this.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
		}
		return this.startNestedScroll(ViewCompat.SCROLL_AXIS_NONE);
	}

	/**
	 * Begin a nestable scroll operation along the given axes.
	 *
	 * <p>A view starting a nested scroll promises to abide by the following contract:</p>
	 *
	 * <p>The view will call startNestedScroll upon initiating a scroll operation. In the case
	 * of a touch scroll this corresponds to the initial {@link MotionEvent#ACTION_DOWN}.
	 * In the case of touch scrolling the nested scroll will be terminated automatically in
	 * the same manner as {@link ViewParent#requestDisallowInterceptTouchEvent(boolean)}.
	 * In the event of programmatic scrolling the caller must explicitly call
	 * {@link #stopNestedScroll()} to indicate the end of the nested scroll.</p>
	 *
	 * <p>If <code>startNestedScroll</code> returns true, a cooperative parent was found.
	 * If it returns false the caller may ignore the rest of this contract until the next scroll.
	 * Calling startNestedScroll while a nested scroll is already in progress will return true.</p>
	 *
	 * <p>At each incremental step of the scroll the caller should invoke
	 * {@link #dispatchNestedPreScroll(int, int, int[], int[]) dispatchNestedPreScroll}
	 * once it has calculated the requested scrolling delta. If it returns true the nested scrolling
	 * parent at least partially consumed the scroll and the caller should adjust the amount it
	 * scrolls by.</p>
	 *
	 * <p>After applying the remainder of the scroll delta the caller should invoke
	 * {@link #dispatchNestedScroll(int, int, int, int, int[]) dispatchNestedScroll}, passing
	 * both the delta consumed and the delta unconsumed. A nested scrolling parent may treat
	 * these values differently. See
	 * {@link NestedScrollingParent#onNestedScroll(View, int, int, int, int)}.
	 * </p>
	 *
	 * @param axes Flags consisting of a combination of {@link ViewCompat#SCROLL_AXIS_HORIZONTAL}
	 *             and/or {@link ViewCompat#SCROLL_AXIS_VERTICAL}.
	 * @return true if a cooperative parent was found and nested scrolling has been enabled for
	 * the current gesture.
	 * @see #stopNestedScroll()
	 * @see #dispatchNestedPreScroll(int, int, int[], int[])
	 * @see #dispatchNestedScroll(int, int, int, int, int[])
	 */
	@Override
	public boolean startNestedScroll(int axes) {
		return this.startNestedScroll(axes, ViewCompat.TYPE_TOUCH);
	}

	/**
	 * Begin a nestable scroll operation along the given axes, for the given input type.
	 *
	 * <p>A view starting a nested scroll promises to abide by the following contract:</p>
	 *
	 * <p>The view will call startNestedScroll upon initiating a scroll operation. In the case
	 * of a touch scroll type this corresponds to the initial {@link MotionEvent#ACTION_DOWN}.
	 * In the case of touch scrolling the nested scroll will be terminated automatically in
	 * the same manner as {@link ViewParent#requestDisallowInterceptTouchEvent(boolean)}.
	 * In the event of programmatic scrolling the caller must explicitly call
	 * {@link #stopNestedScroll(int)} to indicate the end of the nested scroll.</p>
	 *
	 * <p>If <code>startNestedScroll</code> returns true, a cooperative parent was found.
	 * If it returns false the caller may ignore the rest of this contract until the next scroll.
	 * Calling startNestedScroll while a nested scroll is already in progress will return true.</p>
	 *
	 * <p>At each incremental step of the scroll the caller should invoke
	 * {@link #dispatchNestedPreScroll(int, int, int[], int[], int) dispatchNestedPreScroll}
	 * once it has calculated the requested scrolling delta. If it returns true the nested scrolling
	 * parent at least partially consumed the scroll and the caller should adjust the amount it
	 * scrolls by.</p>
	 *
	 * <p>After applying the remainder of the scroll delta the caller should invoke
	 * {@link #dispatchNestedScroll(int, int, int, int, int[], int) dispatchNestedScroll}, passing
	 * both the delta consumed and the delta unconsumed. A nested scrolling parent may treat
	 * these values differently. See
	 * {@link NestedScrollingParent2#onNestedScroll(View, int, int, int, int, int)}.
	 * </p>
	 *
	 * @param axes Flags consisting of a combination of {@link ViewCompat#SCROLL_AXIS_HORIZONTAL}
	 *             and/or {@link ViewCompat#SCROLL_AXIS_VERTICAL}.
	 * @param type the type of input which cause this scroll event
	 * @return true if a cooperative parent was found and nested scrolling has been enabled for
	 * the current gesture.
	 * @see #stopNestedScroll(int)
	 * @see #dispatchNestedPreScroll(int, int, int[], int[], int)
	 * @see #dispatchNestedScroll(int, int, int, int, int[], int)
	 */
	@Override
	public boolean startNestedScroll(int axes, int type) {
		return this.mNestedScrollingChildHelper.startNestedScroll(axes, type);
	}

	/**
	 * Stop a nested scroll in progress.
	 *
	 * <p>Calling this method when a nested scroll is not currently in progress is harmless.</p>
	 *
	 * @see #startNestedScroll(int)
	 */
	@Override
	public void stopNestedScroll() {
		this.stopNestedScroll(ViewCompat.TYPE_TOUCH);
	}

	/**
	 * Stop a nested scroll in progress for the given input type.
	 *
	 * <p>Calling this method when a nested scroll is not currently in progress is harmless.</p>
	 *
	 * @param type the type of input which cause this scroll event
	 * @see #startNestedScroll(int, int)
	 */
	@Override
	public void stopNestedScroll(int type) {
		if (this.mNestedScrollingStep != null) {
			this.mNestedScrollingStep.stopNestedScroll();
		}
		this.mNestedScrollingChildHelper.stopNestedScroll(type);
	}

	/**
	 * Returns true if this view has a nested scrolling parent.
	 *
	 * <p>The presence of a nested scrolling parent indicates that this view has initiated
	 * a nested scroll and it was accepted by an ancestor view further up the view hierarchy.</p>
	 *
	 * @return whether this view has a nested scrolling parent
	 */
	@Override
	public boolean hasNestedScrollingParent() {
		return this.hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);
	}

	/**
	 * Returns true if this view has a nested scrolling parent for the given input type.
	 *
	 * <p>The presence of a nested scrolling parent indicates that this view has initiated
	 * a nested scroll and it was accepted by an ancestor view further up the view hierarchy.</p>
	 *
	 * @param type the type of input which cause this scroll event
	 * @return whether this view has a nested scrolling parent
	 */
	@Override
	public boolean hasNestedScrollingParent(int type) {
		return this.mNestedScrollingChildHelper.hasNestedScrollingParent(type);
	}

	/**
	 * Dispatch one step of a nested scroll in progress.
	 *
	 * <p>Implementations of views that support nested scrolling should call this to report
	 * info about a scroll in progress to the current nested scrolling parent. If a nested scroll
	 * is not currently in progress or nested scrolling is not
	 * {@link #isNestedScrollingEnabled() enabled} for this view this method does nothing.</p>
	 *
	 * <p>Compatible View implementations should also call
	 * {@link #dispatchNestedPreScroll(int, int, int[], int[]) dispatchNestedPreScroll} before
	 * consuming a component of the scroll event themselves.</p>
	 *
	 * @param dxConsumed     Horizontal distance in pixels consumed by this view during this scroll step
	 * @param dyConsumed     Vertical distance in pixels consumed by this view during this scroll step
	 * @param dxUnconsumed   Horizontal scroll distance in pixels not consumed by this view
	 * @param dyUnconsumed   Horizontal scroll distance in pixels not consumed by this view
	 * @param offsetInWindow Optional. If not null, on return this will contain the offset
	 *                       in local view coordinates of this view from before this operation
	 *                       to after it completes. View implementations may use this to adjust
	 *                       expected input coordinate tracking.
	 * @return true if the event was dispatched, false if it could not be dispatched.
	 * @see #dispatchNestedPreScroll(int, int, int[], int[])
	 */
	@Override
	public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow) {
		return this.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, ViewCompat.TYPE_TOUCH);
	}

	/**
	 * Dispatch one step of a nested scroll in progress.
	 *
	 * <p>Implementations of views that support nested scrolling should call this to report
	 * info about a scroll in progress to the current nested scrolling parent. If a nested scroll
	 * is not currently in progress or nested scrolling is not
	 * {@link #isNestedScrollingEnabled() enabled} for this view this method does nothing.</p>
	 *
	 * <p>Compatible View implementations should also call
	 * {@link #dispatchNestedPreScroll(int, int, int[], int[], int) dispatchNestedPreScroll} before
	 * consuming a component of the scroll event themselves.</p>
	 *
	 * @param dxConsumed     Horizontal distance in pixels consumed by this view during this scroll step
	 * @param dyConsumed     Vertical distance in pixels consumed by this view during this scroll step
	 * @param dxUnconsumed   Horizontal scroll distance in pixels not consumed by this view
	 * @param dyUnconsumed   Horizontal scroll distance in pixels not consumed by this view
	 * @param offsetInWindow Optional. If not null, on return this will contain the offset
	 *                       in local view coordinates of this view from before this operation
	 *                       to after it completes. View implementations may use this to adjust
	 *                       expected input coordinate tracking.
	 * @param type           the type of input which cause this scroll event
	 * @return true if the event was dispatched, false if it could not be dispatched.
	 * @see #dispatchNestedPreScroll(int, int, int[], int[], int)
	 */
	@Override
	public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
		return this.mNestedScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow, type);
	}

	/**
	 * Dispatch one step of a nested scroll in progress before this view consumes any portion of it.
	 *
	 * <p>Nested pre-scroll events are to nested scroll events what touch intercept is to touch.
	 * <code>dispatchNestedPreScroll</code> offers an opportunity for the parent view in a nested
	 * scrolling operation to consume some or all of the scroll operation before the child view
	 * consumes it.</p>
	 *
	 * @param dx             Horizontal scroll distance in pixels
	 * @param dy             Vertical scroll distance in pixels
	 * @param consumed       Output. If not null, consumed[0] will contain the consumed component of dx
	 *                       and consumed[1] the consumed dy.
	 * @param offsetInWindow Optional. If not null, on return this will contain the offset
	 *                       in local view coordinates of this view from before this operation
	 *                       to after it completes. View implementations may use this to adjust
	 *                       expected input coordinate tracking.
	 * @return true if the parent consumed some or all of the scroll delta
	 * @see #dispatchNestedScroll(int, int, int, int, int[])
	 */
	@Override
	public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow) {
		return this.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH);
	}

	/**
	 * Dispatch one step of a nested scroll in progress before this view consumes any portion of it.
	 *
	 * <p>Nested pre-scroll events are to nested scroll events what touch intercept is to touch.
	 * <code>dispatchNestedPreScroll</code> offers an opportunity for the parent view in a nested
	 * scrolling operation to consume some or all of the scroll operation before the child view
	 * consumes it.</p>
	 *
	 * @param dx             Horizontal scroll distance in pixels
	 * @param dy             Vertical scroll distance in pixels
	 * @param consumed       Output. If not null, consumed[0] will contain the consumed component of dx
	 *                       and consumed[1] the consumed dy.
	 * @param offsetInWindow Optional. If not null, on return this will contain the offset
	 *                       in local view coordinates of this view from before this operation
	 *                       to after it completes. View implementations may use this to adjust
	 *                       expected input coordinate tracking.
	 * @param type           the type of input which cause this scroll event
	 * @return true if the parent consumed some or all of the scroll delta
	 * @see #dispatchNestedScroll(int, int, int, int, int[], int)
	 */
	@Override
	public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed, @Nullable int[] offsetInWindow, int type) {
		return this.mNestedScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type);
	}

	/**
	 * Dispatch a fling to a nested scrolling parent.
	 *
	 * <p>This method should be used to indicate that a nested scrolling child has detected
	 * suitable conditions for a fling. Generally this means that a touch scroll has ended with a
	 * {@link VelocityTracker velocity} in the direction of scrolling that meets or exceeds
	 * the {@link ViewConfiguration#getScaledMinimumFlingVelocity() minimum fling velocity}
	 * along a scrollable axis.</p>
	 *
	 * <p>If a nested scrolling child view would normally fling but it is at the edge of
	 * its own content, it can use this method to delegate the fling to its nested scrolling
	 * parent instead. The parent may optionally consume the fling or observe a child fling.</p>
	 *
	 * @param velocityX Horizontal fling velocity in pixels per second
	 * @param velocityY Vertical fling velocity in pixels per second
	 * @param consumed  true if the child consumed the fling, false otherwise
	 * @return true if the nested scrolling parent consumed or otherwise reacted to the fling
	 */
	@Override
	public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
		return this.mNestedScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
	}

	/**
	 * Dispatch a fling to a nested scrolling parent before it is processed by this view.
	 *
	 * <p>Nested pre-fling events are to nested fling events what touch intercept is to touch
	 * and what nested pre-scroll is to nested scroll. <code>dispatchNestedPreFling</code>
	 * offsets an opportunity for the parent view in a nested fling to fully consume the fling
	 * before the child view consumes it. If this method returns <code>true</code>, a nested
	 * parent view consumed the fling and this view should not scroll as a result.</p>
	 *
	 * <p>For a better user experience, only one view in a nested scrolling chain should consume
	 * the fling at a time. If a parent view consumed the fling this method will return false.
	 * Custom view implementations should account for this in two ways:</p>
	 *
	 * <ul>
	 * <li>If a custom view is paged and needs to settle to a fixed page-point, do not
	 * call <code>dispatchNestedPreFling</code>; consume the fling and settle to a valid
	 * position regardless.</li>
	 * <li>If a nested parent does consume the fling, this view should not scroll at all,
	 * even to settle back to a valid idle position.</li>
	 * </ul>
	 *
	 * <p>Views should also not offer fling velocities to nested parent views along an axis
	 * where scrolling is not currently supported; a {@link ScrollView ScrollView}
	 * should not offer a horizontal fling velocity to its parents since scrolling along that
	 * axis is not permitted and carrying velocity along that motion does not make sense.</p>
	 *
	 * @param velocityX Horizontal fling velocity in pixels per second
	 * @param velocityY Vertical fling velocity in pixels per second
	 * @return true if a nested scrolling parent consumed the fling
	 */
	@Override
	public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
		return this.mNestedScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
	}
}
