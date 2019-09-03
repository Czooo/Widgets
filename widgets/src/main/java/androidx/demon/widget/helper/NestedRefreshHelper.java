package androidx.demon.widget.helper;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.demon.widget.DefaultFooterLoadView;
import androidx.demon.widget.DefaultHeaderLoadView;
import androidx.demon.widget.DefaultHorFooterLoadView;
import androidx.demon.widget.DefaultHorHeaderLoadView;
import androidx.demon.widget.R;
import androidx.demon.widget.RefreshLayout;
import androidx.demon.widget.RefreshMode;

/**
 * Author create by ok on 2019-07-22
 * Email : ok@163.com.
 */
public class NestedRefreshHelper implements NestedScrollingHelper.Callback {

	public static final int HORIZONTAL = 0;

	public static final int VERTICAL = 1;

	@IntDef({HORIZONTAL, VERTICAL})
	@Retention(RetentionPolicy.SOURCE)
	@interface OrientationMode {
	}

	private static final float FRICTION_RATIO = .22F;

	private final NestedScrollingHelper mNestedScrollingHelper;
	private final RefreshLayout mAnchorView;

	private View mDragView;
	private RefreshLayout.OnRefreshListener mOnRefreshListener;
	private RefreshLayout.OnDragViewOwner mOnDragViewOwner;
	private RefreshLayout.OnChildScrollCallback mOnChildScrollCallback;
	private ArrayList<RefreshLayout.OnScrollListener> mOnScrollListeners;

	private boolean mIsDragStart = true;
	private boolean mIsDragEnd = true;
	private RefreshMode mRefreshMode = RefreshMode.REFRESH_MODE_NONE;
	@OrientationMode
	private int mOrientation = VERTICAL;

	public NestedRefreshHelper(@NonNull RefreshLayout anchorView) {
		this.mNestedScrollingHelper = new NestedScrollingHelperImpl(anchorView, this);
		this.mNestedScrollingHelper.setScrollingDuration(800);
		this.mAnchorView = anchorView;

		this.setHeaderLoadView(new DefaultHeaderLoadView());
		this.setFooterLoadView(new DefaultFooterLoadView());
	}

	@Override
	public boolean canScrollVertically() {
		return VERTICAL == this.mOrientation;
	}

	@Override
	public boolean canScrollHorizontally() {
		return HORIZONTAL == this.mOrientation;
	}

	@Override
	public boolean shouldStartNestedScroll() {
		return !this.mIsNotifying && !this.mIsFinishRollbackInProgress;
	}

	@Override
	public boolean canChildScroll(int direction) {
		if ((!this.mIsDragStart && direction < 0)
				|| (!this.mIsDragEnd && direction > 0)) {
			return true;
		}
		if (this.mOnChildScrollCallback != null) {
			return this.mOnChildScrollCallback.canChildScroll(this.mAnchorView, direction);
		}
		if (VERTICAL == this.mOrientation) {
			return this.getDragView().canScrollVertically(direction);
		}
		return this.getDragView().canScrollHorizontally(direction);
	}

	private boolean mIsNotifying;
	private boolean mIsRefreshing;
	private boolean mIsFinishRollbackInProgress;
	private long mFinishRollbackDelayMillis;

	private float mFrictionRatio = FRICTION_RATIO;

	@Override
	public void onScrollBy(@NonNull NestedScrollingHelper helper, int dx, int dy, @NonNull int[] consumed) {
		final int mScrollOffsetX = helper.getScrollOffsetX();
		final int mScrollOffsetY = helper.getScrollOffsetY();
		int unconsumedX = dx;
		int unconsumedY = dy;

		if (this.mIsRefreshing) {
			final int mScrollDirection = helper.getScrollDirection();
			final int mPreScrollOffsetX = mScrollOffsetX + dx;
			final int mPreScrollOffsetY = mScrollOffsetY + dy;
			final int mPreScrollDistance = this.getPreScrollDistance(mScrollDirection);
			// If you need boundary detection
			if (this.canScrollHorizontally() && Math.abs(mPreScrollOffsetX) < Math.abs(mPreScrollDistance)) {
				unconsumedX = mPreScrollDistance - mScrollOffsetX;
			}
			if (this.canScrollVertically() && Math.abs(mPreScrollOffsetY) < Math.abs(mPreScrollDistance)) {
				unconsumedY = mPreScrollDistance - mScrollOffsetY;
			}
		}
		consumed[0] = unconsumedX;
		consumed[1] = unconsumedY;

		final float nowScrollOffsetX = mScrollOffsetX + consumed[0];
		final float nowScrollOffsetY = mScrollOffsetY + consumed[1];
		this.getDragView().setTranslationX(-(nowScrollOffsetX * this.mFrictionRatio));
		this.getDragView().setTranslationY(-(nowScrollOffsetY * this.mFrictionRatio));

		if (HORIZONTAL == this.mOrientation) {
			final int direction = helper.getPreScrollDirection(dx);
			this.dispatchOnRefreshPull(direction, (int) (nowScrollOffsetX + NestedHelper.getDirectionDifference(direction)));
		} else {
			final int direction = helper.getPreScrollDirection(dy);
			this.dispatchOnRefreshPull(direction, (int) (nowScrollOffsetY + NestedHelper.getDirectionDifference(direction)));
		}

		if (this.mOnScrollListeners != null) {
			for (RefreshLayout.OnScrollListener listener : this.mOnScrollListeners) {
				listener.onScrolled(this.mAnchorView, consumed[0], consumed[1]);
			}
		}
	}

	@Override
	public void onScrollStateChanged(@NonNull NestedScrollingHelper helper, int scrollState) {
		if (NestedScrollingHelper.SCROLL_STATE_IDLE == scrollState) {
			final float mScrollOffset = helper.getScrollOffset();
			final int mScrollDirection = helper.getScrollDirection();
			final int mPreScrollDistance = this.getPreScrollDistance(mScrollDirection);

			if (this.mIsRefreshing) {
				if (Math.abs(mPreScrollDistance) != Math.abs(mScrollOffset)) {
					this.setRefreshing(true, this.mIsNotifying);
				} else {
					if (this.mIsNotifying) {
						this.mIsNotifying = false;
						// your refreshing
						if (this.mOnRefreshListener != null) {
							this.mOnRefreshListener.onRefreshing(this.mAnchorView, RefreshMode.parse(mScrollDirection));
						}
					}
				}
			} else {
				if (!this.mIsFinishRollbackInProgress
						&& Math.abs(mPreScrollDistance) > 0
						&& Math.abs(mPreScrollDistance) <= Math.abs(mScrollOffset)) {
					this.setRefreshing(true, true);
				} else if (Math.abs(mScrollOffset) != 0) {
					final long finishRollbackDelayMillis = this.mFinishRollbackDelayMillis;
					this.mFinishRollbackDelayMillis = 0;
					this.setRefreshing(false, false, finishRollbackDelayMillis);
				} else {
					this.mFinishRollbackDelayMillis = 0;
					this.mIsFinishRollbackInProgress = false;
					this.mHeaderContainer.setVisibility(View.GONE);
					this.mFooterContainer.setVisibility(View.GONE);
				}
			}
		}
	}

	private void setRefreshing(boolean refreshing, boolean notifying) {
		this.setRefreshing(refreshing, notifying, 0);
	}

	private void setRefreshing(boolean refreshing, boolean notifying, long delayMillis) {
		final float mScrollOffset = this.mNestedScrollingHelper.getScrollOffset();
		final int mScrollDirection = this.mNestedScrollingHelper.getScrollDirection();
		final int mPreScrollDistance = this.getPreScrollDistance(mScrollDirection);

		int destinationX = 0;
		int destinationY = 0;
		boolean shouldRefreshing = refreshing;

		if (this.mIsRefreshing != refreshing) {
			this.mIsRefreshing = refreshing;
			this.mIsNotifying = notifying;

			if (refreshing) {
				this.dispatchOnRefreshing(mScrollDirection);
			} else {
				this.mIsFinishRollbackInProgress = true;
				this.dispatchOnRefreshed(mScrollDirection);
				// if your wait
				if (delayMillis > 0
						&& Math.abs(mPreScrollDistance) > 0
						&& Math.abs(mPreScrollDistance) < Math.abs(mScrollOffset)) {
					shouldRefreshing = true;
					// rollback delay millis
					this.mFinishRollbackDelayMillis = delayMillis;
				}
			}
		}
		if (shouldRefreshing) {
			if (this.canScrollVertically()) {
				destinationY = mPreScrollDistance;
			} else if (this.canScrollHorizontally()) {
				destinationX = mPreScrollDistance;
			}
		}
		if (this.mFinishRollbackDelayMillis > 0) {
			this.mNestedScrollingHelper.smoothScrollTo(destinationX, destinationY);
		} else {
			this.mNestedScrollingHelper.smoothScrollTo(destinationX, destinationY, delayMillis);
		}
	}

	public void setRefreshing(boolean refreshing) {
		this.setRefreshing(refreshing, 0);
	}

	public void setRefreshing(boolean refreshing, long delayMillis) {
		if (!this.mIsRefreshing && refreshing) {
			this.setRefreshing(true, true, delayMillis);
		} else {
			this.setRefreshing(refreshing, false, delayMillis);
		}
	}

	public void setOrientation(@OrientationMode int orientation) {
		if (this.mOrientation != orientation) {
			this.mOrientation = orientation;

			// use default LoadView
			final RefreshLayout.LoadView preHeaderLoadView = this.getHeaderLoadView();
			final RefreshLayout.LoadView preFooterLoadView = this.getFooterLoadView();
			if (VERTICAL == orientation) {
				if (preHeaderLoadView instanceof DefaultHorHeaderLoadView) {
					this.setHeaderLoadView(new DefaultHeaderLoadView());
				}
				if (preFooterLoadView instanceof DefaultHorFooterLoadView) {
					this.setFooterLoadView(new DefaultFooterLoadView());
				}
			} else {
				if (preHeaderLoadView instanceof DefaultHeaderLoadView) {
					this.setHeaderLoadView(new DefaultHorHeaderLoadView());
				}
				if (preFooterLoadView instanceof DefaultFooterLoadView) {
					this.setFooterLoadView(new DefaultHorFooterLoadView());
				}
			}
			this.requestLayoutParams();
		}
	}

	public void setRefreshMode(@NonNull RefreshMode refreshMode) {
		if (!this.mIsRefreshing && this.mRefreshMode != refreshMode) {
			this.mRefreshMode = refreshMode;

			if (this.mRefreshMode.hasStartMode()) {
				this.setDraggingToStart(true);
			}
			if (this.mRefreshMode.hasEndMode()) {
				this.setDraggingToEnd(true);
			}
		}
	}

	public void setDraggingToStart(boolean start) {
		this.mIsDragStart = this.mRefreshMode.hasStartMode() || start;
	}

	public void setDraggingToEnd(boolean end) {
		this.mIsDragEnd = this.mRefreshMode.hasEndMode() || end;
	}

	public void setFrictionRatio(@FloatRange(from = 0.1F, to = 1.F) float frictionRatio) {
		if (this.mFrictionRatio != frictionRatio) {
			this.mFrictionRatio = frictionRatio;
		}
	}

	public void setOnChildScrollCallback(@NonNull RefreshLayout.OnChildScrollCallback callback) {
		this.mOnChildScrollCallback = callback;
	}

	public void setOnDragViewOwner(@NonNull RefreshLayout.OnDragViewOwner owner) {
		this.mOnDragViewOwner = owner;
	}

	public void setOnRefreshListener(@NonNull RefreshLayout.OnRefreshListener listener) {
		this.mOnRefreshListener = listener;
	}

	public void addOnScrollListener(@NonNull RefreshLayout.OnScrollListener listener) {
		if (this.mOnScrollListeners == null) {
			this.mOnScrollListeners = new ArrayList<>();
		}
		this.mOnScrollListeners.add(listener);
	}

	public void removeOnScrollListener(@NonNull RefreshLayout.OnScrollListener listener) {
		if (this.mOnScrollListeners != null) {
			this.mOnScrollListeners.remove(listener);
		}
	}

	public static final int SCROLL_STYLE_FOLLOWED = 0;
	public static final int SCROLL_STYLE_AFTER_FOLLOWED = 1;

	private int mHeaderScrollStyleMode = SCROLL_STYLE_AFTER_FOLLOWED;
	private int mFooterScrollStyleMode = SCROLL_STYLE_AFTER_FOLLOWED;
	private RefreshLayout.LoadView mHeaderLoadView;
	private RefreshLayout.LoadView mFooterLoadView;
	private RelativeLayout mHeaderContainer;
	private RelativeLayout mFooterContainer;

	public <V extends RefreshLayout.LoadView> void setHeaderLoadView(@NonNull V loadView) {
		if (this.mIsRefreshing) {
			return;
		}
		if (this.mHeaderLoadView != null) {
			this.mHeaderContainer.removeAllViews();
		}
		if (this.mHeaderContainer == null) {
			this.mHeaderContainer = new RelativeLayout(this.mAnchorView.getContext());
			this.mHeaderContainer.setId(R.id.app_refresh_header_view_id);
			this.mAnchorView.addView(this.mHeaderContainer, 0);
		}
		this.mHeaderLoadView = loadView;
		final View preView = this.mHeaderLoadView.onCreateView(LayoutInflater.from(this.mAnchorView.getContext()), this.mHeaderContainer);
		if (preView.getParent() == null) {
			this.mHeaderContainer.addView(preView);
		}
		preView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		this.mHeaderContainer.setVisibility(View.GONE);
		this.mHeaderContainer.requestLayout();
		this.mHeaderLoadView.onViewCreated(this.mAnchorView, this.mHeaderContainer);
		this.requestLayoutParams();
	}

	public <V extends RefreshLayout.LoadView> void setFooterLoadView(@NonNull V loadView) {
		if (this.mIsRefreshing) {
			return;
		}
		if (this.mFooterLoadView != null) {
			this.mFooterContainer.removeAllViews();
		}
		if (this.mFooterContainer == null) {
			this.mFooterContainer = new RelativeLayout(this.mAnchorView.getContext());
			this.mFooterContainer.setId(R.id.app_refresh_footer_view_id);
			this.mAnchorView.addView(this.mFooterContainer, 0);
		}
		this.mFooterLoadView = loadView;
		final View preView = this.mFooterLoadView.onCreateView(LayoutInflater.from(this.mAnchorView.getContext()), this.mFooterContainer);
		if (preView.getParent() == null) {
			this.mFooterContainer.addView(preView);
		}
		preView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		this.mFooterContainer.setVisibility(View.GONE);
		this.mFooterContainer.requestLayout();
		this.mFooterLoadView.onViewCreated(this.mAnchorView, this.mFooterContainer);
		this.requestLayoutParams();
	}

	public void setHeaderScrollStyleMode(int scrollStyleMode) {
		this.mHeaderScrollStyleMode = scrollStyleMode;
	}

	public void setFooterScrollStyleMode(int scrollStyleMode) {
		this.mFooterScrollStyleMode = scrollStyleMode;
	}

	public boolean isRefreshing() {
		return this.mIsRefreshing;
	}

	@NonNull
	public RefreshLayout.LoadView getHeaderLoadView() {
		return this.mHeaderLoadView;
	}

	@NonNull
	public RefreshLayout.LoadView getFooterLoadView() {
		return this.mFooterLoadView;
	}

	@OrientationMode
	public int getOrientation() {
		return this.mOrientation;
	}

	public float getFrictionRatio() {
		return this.mFrictionRatio;
	}

	@NonNull
	public RefreshMode getRefreshMode() {
		return this.mRefreshMode;
	}

	@NonNull
	public NestedScrollingHelper getNestedScrollingHelper() {
		return this.mNestedScrollingHelper;
	}

	private void requestLayoutParams() {
		View preContainer = this.mAnchorView.findViewById(R.id.app_refresh_header_view_id);
		if (preContainer != null) {
			final RefreshLayout.LayoutParams preLayoutParams;
			if (VERTICAL == this.mOrientation) {
				preLayoutParams = new RefreshLayout.LayoutParams(RefreshLayout.LayoutParams.MATCH_PARENT, RefreshLayout.LayoutParams.WRAP_CONTENT);
				preLayoutParams.addRule(RefreshLayout.CENTER_HORIZONTAL);
				preLayoutParams.addRule(RefreshLayout.ALIGN_PARENT_TOP);
			} else {
				preLayoutParams = new RefreshLayout.LayoutParams(RefreshLayout.LayoutParams.WRAP_CONTENT, RefreshLayout.LayoutParams.MATCH_PARENT);
				preLayoutParams.addRule(RefreshLayout.CENTER_VERTICAL);
				preLayoutParams.addRule(RefreshLayout.ALIGN_PARENT_LEFT);
			}
			preContainer.setLayoutParams(preLayoutParams);
		}
		preContainer = this.mAnchorView.findViewById(R.id.app_refresh_footer_view_id);
		if (preContainer != null) {
			final RefreshLayout.LayoutParams preLayoutParams;
			if (VERTICAL == this.mOrientation) {
				preLayoutParams = new RefreshLayout.LayoutParams(RefreshLayout.LayoutParams.MATCH_PARENT, RefreshLayout.LayoutParams.WRAP_CONTENT);
				preLayoutParams.addRule(RefreshLayout.CENTER_HORIZONTAL);
				preLayoutParams.addRule(RefreshLayout.ALIGN_PARENT_BOTTOM);
			} else {
				preLayoutParams = new RefreshLayout.LayoutParams(RefreshLayout.LayoutParams.WRAP_CONTENT, RefreshLayout.LayoutParams.MATCH_PARENT);
				preLayoutParams.addRule(RefreshLayout.CENTER_VERTICAL);
				preLayoutParams.addRule(RefreshLayout.ALIGN_PARENT_RIGHT);
			}
			preContainer.setLayoutParams(preLayoutParams);
		}
		this.mAnchorView.requestLayout();
	}

	private void dispatchOnRefreshPull(int direction, int scrollOffset) {
		final int mPreScrollDistance = this.getPreScrollDistance(direction);
		// use scroll scale
		final float mScrollOffsetScale = Math.abs(scrollOffset) / Math.max(Math.abs(mPreScrollDistance) * 1.F, 1.F);

		if (direction < 0 && this.mRefreshMode.hasStartMode()) {
			this.mHeaderContainer.setVisibility(View.VISIBLE);
			// If you want to use custom scrollStyle to addOnScrollListener
			final int mHeaderLoadViewScrollOffset = this.getHeaderLoadViewScrollOffset(scrollOffset);
			if (VERTICAL == this.mOrientation) {
				this.mHeaderContainer.setTranslationY(mHeaderLoadViewScrollOffset);
			} else {
				this.mHeaderContainer.setTranslationX(mHeaderLoadViewScrollOffset);
			}
			if (!this.mIsRefreshing && !this.mIsFinishRollbackInProgress) {
				this.mHeaderLoadView.onRefreshPull(this.mAnchorView, scrollOffset, mScrollOffsetScale);
			}
		} else if (direction > 0 && this.mRefreshMode.hasEndMode()) {
			this.mFooterContainer.setVisibility(View.VISIBLE);
			final int mFooterLoadViewScrollOffset = this.getFooterLoadViewScrollOffset(scrollOffset);
			if (VERTICAL == this.mOrientation) {
				this.mFooterContainer.setTranslationY(mFooterLoadViewScrollOffset);
			} else {
				this.mFooterContainer.setTranslationX(mFooterLoadViewScrollOffset);
			}
			if (!this.mIsRefreshing && !this.mIsFinishRollbackInProgress) {
				this.mFooterLoadView.onRefreshPull(this.mAnchorView, scrollOffset, mScrollOffsetScale);
			}
		}

		// eg child scrolling
		for (int index = 0; index < this.mAnchorView.getChildCount(); index++) {
			final View preView = this.mAnchorView.getChildAt(index);
			final RefreshLayout.LayoutParams preLayoutParams = (RefreshLayout.LayoutParams) preView.getLayoutParams();
			if (RefreshLayout.SCROLL_FLAG_NONE == preLayoutParams.mScrollFlag) {
				continue;
			}
			int childScrollOffset = 0;
			if (preLayoutParams.mScrollFlag == RefreshLayout.SCROLL_FLAG_ALL
					|| (preLayoutParams.mScrollFlag == RefreshLayout.SCROLL_FLAG_START && scrollOffset <= 0)
					|| (preLayoutParams.mScrollFlag == RefreshLayout.SCROLL_FLAG_END && scrollOffset >= 0)) {
				childScrollOffset = -(int) (scrollOffset * this.mFrictionRatio + NestedHelper.getDirectionDifference(direction));
			}
			if (VERTICAL == this.mOrientation) {
				preView.setTranslationY(childScrollOffset);
			} else {
				preView.setTranslationX(childScrollOffset);
			}
		}
	}

	private void dispatchOnRefreshing(int direction) {
		if (direction < 0) {
			this.mHeaderLoadView.onRefreshing(this.mAnchorView);
		} else if (direction > 0) {
			this.mFooterLoadView.onRefreshing(this.mAnchorView);
		}
	}

	private void dispatchOnRefreshed(int direction) {
		if (direction < 0) {
			this.mHeaderLoadView.onRefreshed(this.mAnchorView);
		} else if (direction > 0) {
			this.mFooterLoadView.onRefreshed(this.mAnchorView);
		}
	}

	private int getPreScrollDistance(int direction) {
		final float distance;
		// 高度作为刷新条件之一
		if (direction < 0 && this.mRefreshMode.hasStartMode()) {
			distance = -this.mHeaderLoadView.onGetScrollDistance(this.mAnchorView, this.mHeaderContainer);
		} else if (direction > 0 && this.mRefreshMode.hasEndMode()) {
			distance = this.mFooterLoadView.onGetScrollDistance(this.mAnchorView, this.mFooterContainer);
		} else {
			distance = 0.F;
		}
		return (int) (distance / this.mFrictionRatio + NestedHelper.getDirectionDifference(direction));
	}

	private int getHeaderLoadViewScrollOffset(int scrollOffset) {
		final View loadView = this.mHeaderContainer;
		if (loadView != null && loadView.isShown()) {
			final int viewSize;
			if (VERTICAL == this.mOrientation) {
				viewSize = (int) (loadView.getMeasuredHeight() / this.mFrictionRatio + 0.5F);
			} else {
				viewSize = (int) (loadView.getMeasuredWidth() / this.mFrictionRatio + 0.5F);
			}
			if (this.mHeaderScrollStyleMode == SCROLL_STYLE_AFTER_FOLLOWED) {
				return (int) -(Math.min(0, viewSize + scrollOffset) * this.mFrictionRatio - 0.5F);
			}
			return (int) -(Math.min(viewSize, viewSize + scrollOffset) * this.mFrictionRatio - 0.5F);
		}
		return 0;
	}

	private int getFooterLoadViewScrollOffset(int scrollOffset) {
		final View loadView = this.mFooterContainer;
		if (loadView != null && loadView.isShown()) {
			final int viewSize;
			if (VERTICAL == this.mOrientation) {
				viewSize = (int) (loadView.getMeasuredHeight() / this.mFrictionRatio + 0.5F);
			} else {
				viewSize = (int) (loadView.getMeasuredWidth() / this.mFrictionRatio + 0.5F);
			}
			if (this.mFooterScrollStyleMode == SCROLL_STYLE_AFTER_FOLLOWED) {
				return (int) (Math.min(0, viewSize - scrollOffset) * this.mFrictionRatio + 0.5F);
			}
			return (int) (Math.min(viewSize, viewSize - scrollOffset) * this.mFrictionRatio + 0.5F);
		}
		return 0;
	}

	@NonNull
	public View getDragView() {
		if (this.mDragView == null) {
			if (this.mOnDragViewOwner != null) {
				return (this.mDragView = this.mOnDragViewOwner.getDragView(this.mAnchorView));
			}
			for (int index = 0; index < this.mAnchorView.getChildCount() && this.mDragView == null; index++) {
				final View preView = this.mAnchorView.getChildAt(index);
				if (R.id.app_refresh_header_view_id == preView.getId()
						|| R.id.app_refresh_footer_view_id == preView.getId()) {
					defameViewScrollFlag(preView);
					continue;
				}
				this.mDragView = preView;
			}
			if (this.mDragView == null) {
				throw new IllegalStateException("not has a child");
			}
			if (this.mDragView.getId() == View.NO_ID) {
				this.mDragView.setId(R.id.app_refresh_view_id);
			}
			if (this.mDragView.getBackground() == null) {
				this.mDragView.setBackgroundColor(Color.WHITE);
			}
			this.mDragView.setOverScrollMode(View.OVER_SCROLL_NEVER);
			defameViewScrollFlag(this.mDragView);
		}
		return this.mDragView;
	}

	private static void defameViewScrollFlag(@NonNull View view) {
		try {
			final RefreshLayout.LayoutParams preLayoutParams = (RefreshLayout.LayoutParams) view.getLayoutParams();
			preLayoutParams.mScrollFlag = RefreshLayout.SCROLL_FLAG_NONE;
			view.setLayoutParams(preLayoutParams);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
