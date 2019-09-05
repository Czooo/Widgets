package androidx.demon.widget;

import android.annotation.SuppressLint;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.demon.widget.helper.NestedHelper;
import androidx.demon.widget.helper.NestedScrollingHelper;

/**
 * Author create by ok on 2019-09-04
 * Email : ok@163.com.
 */
public class SwipeSideDragManager extends DragRelativeLayout.DragManager {

	private static final float MIN_SCROLL_SCALE = .2F;

	private int mDampSpringBackSize;
	private float mMinScrollScale = MIN_SCROLL_SCALE;
	private boolean mIsOpenState = false;
	private boolean mIsDrawerEnabled = true;

	private SwipeSideLayout.OnOpenStateListener mOnOpenStateListener;

	private View mDragView;
	private View mDrawerView;

	@Override
	public boolean shouldStartNestedScroll() {
		if (this.mDragView == null) {
			this.mDragView = this.getDragView();
		}
		if (this.mDrawerView == null) {
			this.mDrawerView = this.getDrawerView();
		}
		return this.mDragView != null
				&& this.mDrawerView != null
				&& this.mIsDrawerEnabled;
	}

	@Override
	public boolean canChildScroll(int direction) {
		if (this.shouldStartNestedScroll()) {
			if (this.mIsOpenState) {
				if (this.canScrollHorizontally()) {
					return this.mDrawerView.canScrollHorizontally(direction);
				}
				if (this.canScrollVertically()) {
					return this.mDrawerView.canScrollVertically(direction);
				}
			}
			if ((this.getDrawerDirection() < 0 && direction < 0)
					|| (this.getDrawerDirection() > 0 && direction > 0)) {
				if (this.canScrollHorizontally()) {
					return this.mDragView.canScrollHorizontally(direction);
				}
				if (this.canScrollVertically()) {
					return this.mDragView.canScrollVertically(direction);
				}
			}
			return true;
		} else {
			if (this.canScrollHorizontally()) {
				return this.getDragRelativeLayout().canScrollHorizontally(direction);
			}
			return this.getDragRelativeLayout().canScrollVertically(direction);
		}
	}

	@Override
	public void onScrollBy(@NonNull NestedScrollingHelper helper, int dx, int dy, @NonNull int[] consumed) {
		final int drawerDirection = this.getDrawerDirection();
		final int mScrollOffsetX = helper.getScrollOffsetX();
		final int mScrollOffsetY = helper.getScrollOffsetY();
		int unconsumedX = dx;
		int unconsumedY = dy;

		int direction = helper.getScrollDirection();
		if (this.canScrollHorizontally()) {
			direction = helper.getPreScrollDirection(dx);
		} else if (this.canScrollVertically()) {
			direction = helper.getPreScrollDirection(dy);
		}

		if (NestedScrollingHelper.SCROLL_STATE_DRAGGING == helper.getScrollState()) {
			if (direction == drawerDirection) {
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
		}
		consumed[0] = unconsumedX;
		consumed[1] = unconsumedY;

		final int preScrollOffsetX = (int) ((mScrollOffsetX + consumed[0]) * this.getFrictionRatio() + NestedHelper.getDirectionDifference(direction));
		final int preScrollOffsetY = (int) ((mScrollOffsetY + consumed[1]) * this.getFrictionRatio() + NestedHelper.getDirectionDifference(direction));
		this.mDragView.setTranslationX(-preScrollOffsetX);
		this.mDragView.setTranslationY(-preScrollOffsetY);

		// direction same
		if (direction == drawerDirection) {
			if (this.mDrawerView.getVisibility() == View.INVISIBLE) {
				this.mDrawerView.setVisibility(View.VISIBLE);
			}
			this.onDrawerAnimation(this.mDrawerView, preScrollOffsetX, preScrollOffsetY, direction);
		} else {
			if (this.mDrawerView.getVisibility() == View.VISIBLE) {
				this.mDrawerView.setVisibility(View.INVISIBLE);
			}
		}
	}

	@Override
	public void onScrollStateChanged(@NonNull NestedScrollingHelper helper, int scrollState) {
		if (NestedScrollingHelper.SCROLL_STATE_IDLE == scrollState) {
			final int scrollDirection = helper.getScrollDirection();
			final int drawerDirection = this.getDrawerDirection();
			final int scrollDistance = this.getScrollDistancePixelSize();

			final int scrollOffset = helper.getScrollOffset();
			final int absScrollOffset = Math.abs(scrollOffset);
			final int absScrollDistance = Math.abs(scrollDistance);

			if (scrollDirection != drawerDirection
					&& absScrollOffset > 0
					&& absScrollDistance > 0) {
				this.setOpenState(false);
				return;
			}

			if (this.mIsOpenState) {
				if (absScrollOffset <= absScrollDistance * (1 - this.mMinScrollScale)) {
					this.setOpenState(false);
				} else if (absScrollOffset != absScrollDistance) {
					this.setOpenState(true);
				}
			} else {
				if (absScrollOffset >= absScrollDistance * this.mMinScrollScale) {
					this.setOpenState(true);
				} else if (absScrollOffset != 0) {
					this.setOpenState(false);
				} else {
					this.mDrawerView.setVisibility(View.INVISIBLE);
				}
			}
		}
	}

	private void onDrawerAnimation(@NonNull View drawerView, int scrollOffsetX, int scrollOffsetY, int scrollDriection) {
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

	public boolean isOpenState() {
		return this.mIsOpenState;
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
				this.mOnOpenStateListener.onOpenStateChanged((SwipeSideLayout) this.getDragRelativeLayout());
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

	public void setDrawerEnabled(boolean enabled) {
		this.mIsDrawerEnabled = enabled;
	}

	public void setMinScrollScale(float minScrollScale) {
		this.mMinScrollScale = minScrollScale;
	}

	public void setDampSpringBackSize(int dampSpringBackSize) {
		this.mDampSpringBackSize = dampSpringBackSize;
	}

	public void setOnOpenStateListener(@NonNull SwipeSideLayout.OnOpenStateListener listener) {
		this.mOnOpenStateListener = listener;
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
			this.smoothScrollTo(deltaX + pixelSizeX, deltaY + pixelSizeY);
		} else {
			this.scrollTo(deltaX, deltaY);
		}
	}

	private void closeDrawer(boolean smooth) {
		if (smooth) {
			this.smoothScrollTo(0, 0);
		} else {
			this.scrollTo(0, 0);
		}
	}

	private int getScrollDistancePixelSize() {
		float distance = 0;
		if (this.shouldStartNestedScroll()) {
			if (this.canScrollHorizontally()) {
				distance = this.mDrawerView.getMeasuredWidth();
			} else if (this.canScrollVertically()) {
				distance = this.mDrawerView.getMeasuredHeight();
			}
		}
		final int direction = this.getDrawerDirection();
		final float pixelSize = NestedHelper.getDirectionDifference(direction);
		return (int) (distance * direction / this.getFrictionRatio() + pixelSize);
	}

	@SuppressLint("RtlHardcoded")
	private int getDrawerDirection() {
		if (this.shouldStartNestedScroll()) {
			if (this.checkViewScrollLayout(this.mDrawerView, SwipeSideLayout.LayoutParams.GRAVITY_SCROLL_LEFT)
					|| this.checkViewScrollLayout(this.mDrawerView, SwipeSideLayout.LayoutParams.GRAVITY_SCROLL_TOP)) {
				return -1;
			}
			if (this.checkViewScrollLayout(this.mDrawerView, SwipeSideLayout.LayoutParams.GRAVITY_SCROLL_RIGHT)
					|| this.checkViewScrollLayout(this.mDrawerView, SwipeSideLayout.LayoutParams.GRAVITY_SCROLL_BOTTOM)) {
				return 1;
			}
		}
		return 0;
	}

	public boolean checkViewScrollLayout(@NonNull View view, int scrollGravity) {
		return ((SwipeSideLayout.LayoutParams) view.getLayoutParams()).scrollGravity == scrollGravity;
	}

	public boolean checkViewVertically(@NonNull View view) {
		return this.checkViewScrollLayout(view, SwipeSideLayout.LayoutParams.GRAVITY_SCROLL_TOP)
				|| this.checkViewScrollLayout(view, SwipeSideLayout.LayoutParams.GRAVITY_SCROLL_BOTTOM);
	}

	public boolean checkViewHorizontally(@NonNull View view) {
		return this.checkViewScrollLayout(view, SwipeSideLayout.LayoutParams.GRAVITY_SCROLL_LEFT)
				|| this.checkViewScrollLayout(view, SwipeSideLayout.LayoutParams.GRAVITY_SCROLL_RIGHT);
	}

	@Nullable
	public View getDragView() {
		final DragRelativeLayout dragRelativeLayout = this.getDragRelativeLayout();
		View tempDragView = null;
		// search first dragView
		for (int index = 0; index < dragRelativeLayout.getChildCount() && tempDragView == null; index++) {
			final View child = dragRelativeLayout.getChildAt(index);
			if (View.GONE == child.getVisibility()) {
				continue;
			}
			final SwipeSideLayout.LayoutParams layoutParams = (SwipeSideLayout.LayoutParams) child.getLayoutParams();
			if (layoutParams.scrollGravity == SwipeSideLayout.LayoutParams.GRAVITY_SCROLL_NONE) {
				tempDragView = child;
			}
		}
		if (tempDragView == null) {
			return null;
		}
		defameViewScrollFlag(tempDragView);
		return tempDragView;
	}

	@Nullable
	public View getDrawerView() {
		final DragRelativeLayout dragRelativeLayout = this.getDragRelativeLayout();
		View tempDragView = null;
		// search first dragView
		for (int index = 0; index < dragRelativeLayout.getChildCount() && tempDragView == null; index++) {
			final View child = dragRelativeLayout.getChildAt(index);
			if (View.GONE == child.getVisibility()) {
				continue;
			}
			final SwipeSideLayout.LayoutParams layoutParams = (SwipeSideLayout.LayoutParams) child.getLayoutParams();
			if (layoutParams.scrollGravity != SwipeSideLayout.LayoutParams.GRAVITY_SCROLL_NONE) {
				tempDragView = child;
			}
		}
		if (tempDragView == null) {
			return null;
		}
		defameViewScrollFlag(tempDragView);
		return tempDragView;
	}

	private static void defameViewScrollFlag(@NonNull View view) {
		try {
			final RefreshLayout.LayoutParams preLayoutParams = (RefreshLayout.LayoutParams) view.getLayoutParams();
			preLayoutParams.mScrollFlag = RefreshLayout.LayoutParams.SCROLL_FLAG_NONE;
			view.setLayoutParams(preLayoutParams);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
