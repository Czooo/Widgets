package androidx.demon.widget;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.demon.widget.helper.NestedHelper;
import androidx.demon.widget.helper.NestedScrollingHelper;

/**
 * Author create by ok on 2019-09-04
 * Email : ok@163.com.
 */
public class RefreshDragManager extends RefreshLayout.DragManager {

	private RefreshMode mRefreshMode = RefreshMode.REFRESH_MODE_NONE;

	private View mDragView;
	private boolean mIsNotifying;
	private boolean mIsRefreshing;
	private boolean mIsFinishRollbackInProgress;
	private boolean mIsFinishRollbackInDragging;
	private long mFinishRollbackDelayMillis = 0L;

	private RefreshLayout.OnRefreshListener mOnRefreshListener;
	private RefreshLayout.OnDragViewOwner mOnDragViewOwner;
	private int[] mRollbackConsumed = new int[2];

	@Override
	public void onAttachedToWindow(@NonNull ViewGroup container) {
		super.onAttachedToWindow(container);
		this.getNestedScrollingHelper().setNestedScrollingStep(
				new RefreshLayout.SimpleNestedScrollingStep());
	}

	@Override
	public boolean shouldStartNestedScroll() {
		this.ensureDragView();
		if (this.mDragView == null) {
			// Drag view not confirmed, nested scroll prohibited.
			return false;
		}
		if (this.mIsNotifying) {
			// In preparation for refresh notify, no scrolling.
			return false;
		}
		if (this.mIsFinishRollbackInProgress) {
			// When refresh completes rollback ???
			if (NestedScrollingHelper.SCROLL_STATE_DRAGGING == this.getScrollState()) {
				return true;
			}
			return false;
		}
		return true;
	}

	@Override
	public boolean canChildScroll(int direction) {
		this.ensureDragView();
		if (RefreshLayout.HORIZONTAL == this.getOrientation()) {
			return this.mDragView.canScrollHorizontally(direction);
		} else if (RefreshLayout.VERTICAL == this.getOrientation()) {
			return this.mDragView.canScrollVertically(direction);
		}
		return true;
	}

	@Override
	public void onScrollBy(@NonNull NestedScrollingHelper helper, int dx, int dy, @NonNull int[] consumed) {
		// scrollState : SCROLL_STATE_DRAGGING / SCROLL_STATE_SETTLING
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

		boolean locatRefreshed = this.mIsRefreshing;
		if (!locatRefreshed && this.mIsFinishRollbackInDragging) {
			locatRefreshed = this.mFinishRollbackDelayMillis > System.currentTimeMillis();
		}
		if (locatRefreshed && NestedScrollingHelper.SCROLL_STATE_DRAGGING == helper.getScrollState()) {
			final int mPreScrollDistance = this.getPreScrollDistance2(direction);
			final int mPreScrollOffsetX = mScrollOffsetX + dx;
			final int mPreScrollOffsetY = mScrollOffsetY + dy;
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

		final int preScrollOffsetX = (int) ((mScrollOffsetX + consumed[0]) * this.getFrictionRatio() + NestedHelper.getDirectionDifference(direction));
		final int preScrollOffsetY = (int) ((mScrollOffsetY + consumed[1]) * this.getFrictionRatio() + NestedHelper.getDirectionDifference(direction));
		this.mDragView.setTranslationX(-preScrollOffsetX);
		this.mDragView.setTranslationY(-preScrollOffsetY);

		if (RefreshLayout.HORIZONTAL == this.getOrientation()) {
			this.dispatchOnRefreshPull(direction, preScrollOffsetX);
		} else {
			this.dispatchOnRefreshPull(direction, preScrollOffsetY);
		}
		if (!locatRefreshed
				&& this.mIsFinishRollbackInProgress
				&& helper.isNestedScrollInProgress()
				&& helper.getScrollState() == NestedScrollingHelper.SCROLL_STATE_SETTLING) {
			this.mRollbackConsumed[0] += consumed[0];
			this.mRollbackConsumed[1] += consumed[1];
		} else {
			Arrays.fill(this.mRollbackConsumed, 0);
		}
	}

	@Override
	public void onScrollStateChanged(@NonNull NestedScrollingHelper helper, int scrollState) {
		if (NestedScrollingHelper.SCROLL_STATE_IDLE == scrollState) {
			final float mScrollOffset = helper.getScrollOffset();
			final int mScrollDirection = helper.getScrollDirection();
			final int mPreScrollDistance = this.getPreScrollDistance2(mScrollDirection);

			if (this.mIsRefreshing) {
				if (Math.abs(mPreScrollDistance) == Math.abs(mScrollOffset)) {
					if (this.mIsNotifying) {
						this.mIsNotifying = false;
						// your refreshing
						if (this.mOnRefreshListener != null) {
							this.mOnRefreshListener.onRefreshing((RefreshLayout) this.getDragRelativeLayout(), RefreshMode.parse(mScrollDirection));
						}
					}
				} else {
					this.performRefreshing(true, this.mIsNotifying);
				}
			} else {
				if (!this.mIsFinishRollbackInProgress
						&& Math.abs(mPreScrollDistance) > 0
						&& Math.abs(mPreScrollDistance) <= Math.abs(mScrollOffset)) {
					this.performRefreshing(true, true);
				} else if (Math.abs(mScrollOffset) != 0) {
					this.performRefreshing(false, false);
				} else {
					this.mFinishRollbackDelayMillis = 0L;
					this.mIsFinishRollbackInProgress = false;
					this.mIsFinishRollbackInDragging = false;
					this.mHeaderContainer.setVisibility(View.GONE);
					this.mFooterContainer.setVisibility(View.GONE);
				}
			}
		}
	}

	public void dispatchTouchEvent(MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
			Arrays.fill(this.mRollbackConsumed, 0);
		}
	}

	public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
		// notify child view on rollback consumed
		consumed[0] -= this.mRollbackConsumed[0] * this.getFrictionRatio();
		consumed[1] -= this.mRollbackConsumed[1] * this.getFrictionRatio();
		Arrays.fill(this.mRollbackConsumed, 0);
	}

	public void setRefreshMode(@NonNull RefreshMode mode) {
		if (!this.mIsRefreshing && this.mRefreshMode != mode) {
			this.mRefreshMode = mode;

			if (this.mRefreshMode.hasStartMode()) {
				this.getDragRelativeLayout().setDraggingToStart(true);
			}
			if (this.mRefreshMode.hasEndMode()) {
				this.getDragRelativeLayout().setDraggingToEnd(true);
			}
		}
	}

	public void setRefreshing(boolean refreshing) {
		this.setRefreshing(refreshing, 0);
	}

	public void setRefreshing(boolean refreshing, long delayMillis) {
		if (!this.mIsRefreshing && refreshing) {
			this.performRefreshing(true, true, delayMillis);
		} else {
			this.performRefreshing(refreshing, false, delayMillis);
		}
	}

	public void setOnRefreshListener(@NonNull RefreshLayout.OnRefreshListener listener) {
		this.mOnRefreshListener = listener;
	}

	public void setOnDragViewOwner(@NonNull RefreshLayout.OnDragViewOwner owner) {
		this.mOnDragViewOwner = owner;
	}

	public RefreshMode getRefreshMode() {
		return this.mRefreshMode;
	}

	public boolean isNotifying() {
		return this.mIsNotifying;
	}

	public boolean isRefreshing() {
		return this.mIsRefreshing;
	}

	public boolean isFinishRollbackInProgress() {
		return this.mIsFinishRollbackInProgress;
	}

	public boolean isFinishRollbackInDragging() {
		return this.mIsFinishRollbackInDragging;
	}

	private void performRefreshing(boolean refreshing, boolean notifying) {
		this.performRefreshing(refreshing, notifying, 0);
	}

	private void performRefreshing(boolean refreshing, boolean notifying, long delayMillis) {
		this.ensureDragView();
		int direction = this.getNestedScrollingHelper().getScrollDirection();

		if (this.mIsRefreshing != refreshing) {
			this.mIsRefreshing = refreshing;
			this.mIsNotifying = notifying;

			if (refreshing) {
				if (direction == 0) {
					direction = -1;
				}
				this.dispatchOnRefreshing(direction);
			} else {
				this.mIsFinishRollbackInProgress = true;
				this.dispatchOnRefreshed(direction);

				if (NestedScrollingHelper.SCROLL_STATE_DRAGGING == this.getNestedScrollingHelper().getScrollState()) {
					this.mIsFinishRollbackInDragging = true;
					this.mFinishRollbackDelayMillis = System.currentTimeMillis() + delayMillis;
					this.getDragRelativeLayout().postDelayed(new Runnable() {
						@Override
						public void run() {
							mHeaderContainer.setVisibility(View.GONE);
							mFooterContainer.setVisibility(View.GONE);
						}
					}, delayMillis);
					return;
				}
			}
		}
		if (refreshing) {
			if (this.canScrollHorizontally()) {
				this.smoothScrollTo(this.getPreScrollDistance2(direction), 0, delayMillis);
			} else if (this.canScrollVertically()) {
				this.smoothScrollTo(0, this.getPreScrollDistance2(direction), delayMillis);
			} else {
				this.smoothScrollTo(0, 0);
			}
		} else {
			this.smoothScrollTo(0, 0, delayMillis);
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
			this.mHeaderContainer = new RelativeLayout(this.getDragRelativeLayout().getContext());
			this.mHeaderContainer.setId(R.id.app_refresh_header_view_id);
			this.addView(this.mHeaderContainer, 0);
		}
		this.mHeaderLoadView = loadView;
		final View preView = this.mHeaderLoadView.onCreateView(LayoutInflater.from(this.getDragRelativeLayout().getContext()), this.mHeaderContainer);
		if (preView.getParent() == null) {
			this.mHeaderContainer.addView(preView);
		}
		preView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		this.mHeaderContainer.setVisibility(View.GONE);
		this.mHeaderContainer.requestLayout();
		this.mHeaderLoadView.onViewCreated((RefreshLayout) this.getDragRelativeLayout(), preView);
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
			this.mFooterContainer = new RelativeLayout(this.getDragRelativeLayout().getContext());
			this.mFooterContainer.setId(R.id.app_refresh_footer_view_id);
			this.addView(this.mFooterContainer, 0);
		}
		this.mFooterLoadView = loadView;
		final View preView = this.mFooterLoadView.onCreateView(LayoutInflater.from(this.getDragRelativeLayout().getContext()), this.mFooterContainer);
		if (preView.getParent() == null) {
			this.mFooterContainer.addView(preView);
		}
		preView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		this.mFooterContainer.setVisibility(View.GONE);
		this.mFooterContainer.requestLayout();
		this.mFooterLoadView.onViewCreated((RefreshLayout) this.getDragRelativeLayout(), preView);
		this.requestLayoutParams();
	}

	public RefreshLayout.LoadView getHeaderLoadView() {
		return this.mHeaderLoadView;
	}

	public RefreshLayout.LoadView getFooterLoadView() {
		return this.mFooterLoadView;
	}

	public RelativeLayout getHeaderParent() {
		return this.mHeaderContainer;
	}

	public RelativeLayout getFooterParent() {
		return this.mFooterContainer;
	}

	public void setHeaderScrollStyleMode(int scrollStyleMode) {
		this.mHeaderScrollStyleMode = scrollStyleMode;
	}

	public void setFooterScrollStyleMode(int scrollStyleMode) {
		this.mFooterScrollStyleMode = scrollStyleMode;
	}

	private void ensureDragView() {
		if (this.mDragView == null) {
			this.mDragView = this.getDragView();
		}
	}

	private void requestLayoutParams() {
		View preContainer = this.getDragRelativeLayout().findViewById(R.id.app_refresh_header_view_id);
		if (preContainer != null) {
			final RefreshLayout.LayoutParams preLayoutParams;
			if (RefreshLayout.VERTICAL == this.getOrientation()) {
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
		preContainer = this.getDragRelativeLayout().findViewById(R.id.app_refresh_footer_view_id);
		if (preContainer != null) {
			final RefreshLayout.LayoutParams preLayoutParams;
			if (RefreshLayout.VERTICAL == this.getOrientation()) {
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
		this.getDragRelativeLayout().requestLayout();
	}

	private void dispatchOnRefreshPull(int direction, int scrollOffset) {
		// use scrollScale
		final float mPreScrollDistance = this.getPreScrollDistance(direction);
		final float mScrollOffsetScale = Math.abs(scrollOffset) / Math.max(Math.abs(mPreScrollDistance) * 1.F, 1.F);

		if (direction < 0 && this.mRefreshMode.hasStartMode()) {
			if (!this.mIsFinishRollbackInDragging) {
				this.mHeaderContainer.setVisibility(View.VISIBLE);
			}
			// If you want to use custom scrollStyle to addOnScrollListener
			final int mHeaderLoadViewScrollOffset = this.getHeaderLoadViewScrollOffset(scrollOffset);
			if (RefreshLayout.VERTICAL == this.getOrientation()) {
				this.mHeaderContainer.setTranslationY(mHeaderLoadViewScrollOffset);
			} else {
				this.mHeaderContainer.setTranslationX(mHeaderLoadViewScrollOffset);
			}
			if (!this.mIsRefreshing && !this.mIsFinishRollbackInProgress) {
				this.mHeaderLoadView.onRefreshPull(scrollOffset, mScrollOffsetScale);
			}
		} else if (direction > 0 && this.mRefreshMode.hasEndMode()) {
			if (!this.mIsFinishRollbackInDragging) {
				this.mFooterContainer.setVisibility(View.VISIBLE);
			}
			final int mFooterLoadViewScrollOffset = this.getFooterLoadViewScrollOffset(scrollOffset);
			if (RefreshLayout.VERTICAL == this.getOrientation()) {
				this.mFooterContainer.setTranslationY(mFooterLoadViewScrollOffset);
			} else {
				this.mFooterContainer.setTranslationX(mFooterLoadViewScrollOffset);
			}
			if (!this.mIsRefreshing && !this.mIsFinishRollbackInProgress) {
				this.mFooterLoadView.onRefreshPull(scrollOffset, mScrollOffsetScale);
			}
		}

		// other child scrolling
		for (int index = 0; index < this.getDragRelativeLayout().getChildCount(); index++) {
			final View preView = this.getDragRelativeLayout().getChildAt(index);
			final RefreshLayout.LayoutParams preLayoutParams = (RefreshLayout.LayoutParams) preView.getLayoutParams();
			if (RefreshLayout.LayoutParams.SCROLL_FLAG_NONE == preLayoutParams.mScrollFlag) {
				continue;
			}
			int childScrollOffset = 0;
			if (preLayoutParams.mScrollFlag == RefreshLayout.LayoutParams.SCROLL_FLAG_ALL
					|| (preLayoutParams.mScrollFlag == RefreshLayout.LayoutParams.SCROLL_FLAG_START && scrollOffset <= 0)
					|| (preLayoutParams.mScrollFlag == RefreshLayout.LayoutParams.SCROLL_FLAG_END && scrollOffset >= 0)) {
				childScrollOffset = -scrollOffset;
			}
			if (RefreshLayout.VERTICAL == this.getOrientation()) {
				preView.setTranslationY(childScrollOffset);
			} else {
				preView.setTranslationX(childScrollOffset);
			}
		}
	}

	private void dispatchOnRefreshing(int direction) {
		if (direction < 0) {
			this.mHeaderLoadView.onRefreshing();
		} else if (direction > 0) {
			this.mFooterLoadView.onRefreshing();
		}
	}

	private void dispatchOnRefreshed(int direction) {
		if (direction < 0) {
			this.mHeaderLoadView.onRefreshed();
		} else if (direction > 0) {
			this.mFooterLoadView.onRefreshed();
		}
	}

	private int getPreScrollDistance(int direction) {
		int distance = 0;
		// 高度作为刷新条件之一
		if (direction < 0 && this.mRefreshMode.hasStartMode()) {
			distance = -this.mHeaderLoadView.onGetScrollDistance();
		} else if (direction > 0 && this.mRefreshMode.hasEndMode()) {
			distance = this.mFooterLoadView.onGetScrollDistance();
		}
		return distance;
	}

	private int getPreScrollDistance2(int direction) {
		return (int) (this.getPreScrollDistance(direction) / this.getFrictionRatio() + NestedHelper.getDirectionDifference(direction));
	}

	private int getHeaderLoadViewScrollOffset(int scrollOffset) {
		final View loadView = this.mHeaderContainer;
		if (loadView != null && loadView.isShown()) {
			final int viewSize;
			if (RefreshLayout.VERTICAL == this.getOrientation()) {
				viewSize = loadView.getMeasuredHeight();
			} else {
				viewSize = loadView.getMeasuredWidth();
			}
			if (this.mHeaderScrollStyleMode == SCROLL_STYLE_AFTER_FOLLOWED) {
				return -(Math.min(0, viewSize + scrollOffset));
			}
			return -(Math.min(viewSize, viewSize + scrollOffset));
		}
		return 0;
	}

	private int getFooterLoadViewScrollOffset(int scrollOffset) {
		final View loadView = this.mFooterContainer;
		if (loadView != null && loadView.isShown()) {
			final int viewSize;
			if (RefreshLayout.VERTICAL == this.getOrientation()) {
				viewSize = loadView.getMeasuredHeight();
			} else {
				viewSize = loadView.getMeasuredWidth();
			}
			if (this.mFooterScrollStyleMode == SCROLL_STYLE_AFTER_FOLLOWED) {
				return (Math.min(0, viewSize - scrollOffset));
			}
			return (Math.min(viewSize, viewSize - scrollOffset));
		}
		return 0;
	}

	@Nullable
	public View getDragView() {
		final DragRelativeLayout dragRelativeLayout = this.getDragRelativeLayout();
		if (this.mOnDragViewOwner != null) {
			return this.mOnDragViewOwner.getDragView((RefreshLayout) dragRelativeLayout);
		}
		View tempDragView = dragRelativeLayout.findViewById(R.id.app_refresh_view_id);
		// search first dragView
		for (int index = 0; index < dragRelativeLayout.getChildCount() && tempDragView == null; index++) {
			final View preView = dragRelativeLayout.getChildAt(index);
			// filter header/footer
			if (R.id.app_refresh_header_view_id == preView.getId()
					|| R.id.app_refresh_footer_view_id == preView.getId()) {
				defameViewScrollFlag(preView);
				continue;
			}
			tempDragView = preView;
		}
		if (tempDragView == null) {
			return null;
		}
		if (tempDragView.getId() == View.NO_ID) {
			tempDragView.setId(R.id.app_refresh_view_id);
		}
		if (tempDragView.getBackground() == null) {
			tempDragView.setBackgroundColor(Color.WHITE);
		}
		tempDragView.setOverScrollMode(View.OVER_SCROLL_NEVER);
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
