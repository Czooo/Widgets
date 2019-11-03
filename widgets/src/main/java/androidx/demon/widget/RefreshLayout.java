package androidx.demon.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.demon.widget.helper.NestedScrollingHelper;
import androidx.demon.widget.helper.NestedScrollingStep;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Author create by ok on 2019-09-04
 * Email : ok@163.com.
 */
public class RefreshLayout extends DragRelativeLayout {

	public static final int SCROLL_STYLE_FOLLOWED = RefreshDragManager.SCROLL_STYLE_FOLLOWED;

	public static final int SCROLL_STYLE_AFTER_FOLLOWED = RefreshDragManager.SCROLL_STYLE_AFTER_FOLLOWED;

	public RefreshLayout(Context context) {
		this(context, null);
	}

	public RefreshLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public RefreshLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.setFocusable(true);
		this.setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
		this.setWillNotDraw(false);

		final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout);
		final int mRefreshMode = mTypedArray.getInt(R.styleable.RefreshLayout_refreshMode, RefreshMode.REFRESH_MODE_NONE.getKey());
		mTypedArray.recycle();

		this.setDragManager(new RefreshDragManager());
		this.setHeaderScrollStyleMode(SCROLL_STYLE_AFTER_FOLLOWED);
		this.setFooterScrollStyleMode(SCROLL_STYLE_FOLLOWED);
		this.setRefreshMode(RefreshMode.parse(mRefreshMode));

		if (VERTICAL == this.getOrientation()) {
			this.setHeaderLoadView(new DefaultHeaderLoadView());
			this.setFooterLoadView(new DefaultFooterLoadView());
		} else {
			this.setHeaderLoadView(new DefaultHorHeaderLoadView());
			this.setFooterLoadView(new DefaultHorFooterLoadView());
		}
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		this.getRefreshView();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		this.getDragManager().dispatchTouchEvent(event);
		return super.dispatchTouchEvent(event);
	}

	@Override
	public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
		super.onNestedPreScroll(target, dx, dy, consumed);
		this.getDragManager().onNestedPreScroll(target, dx, dy, consumed);
	}

	@Override
	public void setDragManager(@Nullable DragManager dragManager) {
		if (dragManager instanceof RefreshDragManager) {
			super.setDragManager(dragManager);
		}
	}

	@Override
	public boolean isDraggingToStart() {
		return super.isDraggingToStart() || this.getRefreshMode().hasStartMode();
	}

	@Override
	public boolean isDraggingToEnd() {
		return super.isDraggingToEnd() || this.getRefreshMode().hasEndMode();
	}

	@NonNull
	@Override
	public RefreshDragManager getDragManager() {
		final DragManager mDragManager = super.getDragManager();
		if (mDragManager == null) {
			throw new IllegalStateException("not has a DragManager created");
		}
		return (RefreshDragManager) mDragManager;
	}

	public void setRefreshMode(RefreshMode mode) {
		this.getDragManager().setRefreshMode(mode);
	}

	public void setRefreshing(boolean refreshing) {
		this.getDragManager().setRefreshing(refreshing);
	}

	public void setRefreshing(boolean refreshing, long delayMillis) {
		this.getDragManager().setRefreshing(refreshing, delayMillis);
	}

	public void setOnRefreshListener(@NonNull OnRefreshListener listener) {
		this.getDragManager().setOnRefreshListener(listener);
	}

	public void setOnDragViewOwner(@NonNull OnDragViewOwner owner) {
		this.getDragManager().setOnDragViewOwner(owner);
	}

	public <V extends LoadView> void setHeaderLoadView(@NonNull V loadView) {
		this.getDragManager().setHeaderLoadView(loadView);
	}

	public <V extends LoadView> void setFooterLoadView(@NonNull V loadView) {
		this.getDragManager().setFooterLoadView(loadView);
	}

	public void setHeaderScrollStyleMode(int scrollStyleMode) {
		this.getDragManager().setHeaderScrollStyleMode(scrollStyleMode);
	}

	public void setFooterScrollStyleMode(int scrollStyleMode) {
		this.getDragManager().setFooterScrollStyleMode(scrollStyleMode);
	}

	public RefreshLayout.LoadView getHeaderLoadView() {
		return this.getDragManager().getHeaderLoadView();
	}

	public RefreshLayout.LoadView getFooterLoadView() {
		return this.getDragManager().getFooterLoadView();
	}

	public ViewGroup getHeaderParent() {
		return this.getDragManager().getHeaderParent();
	}

	public ViewGroup getFooterParent() {
		return this.getDragManager().getFooterParent();
	}

	public RefreshMode getRefreshMode() {
		return this.getDragManager().getRefreshMode();
	}

	public boolean isNotifying() {
		return this.getDragManager().isNotifying();
	}

	public boolean isRefreshing() {
		return this.getDragManager().isRefreshing();
	}

	public boolean isFinishRollbackInProgress() {
		return this.getDragManager().isFinishRollbackInProgress();
	}

	public boolean isFinishRollbackInDragging() {
		return this.getDragManager().isFinishRollbackInDragging();
	}

	public View getRefreshView() {
		return this.getDragManager().getDragView();
	}

	public interface OnDragViewOwner {

		View getDragView(@NonNull RefreshLayout refreshLayout);
	}

	public interface OnRefreshListener {

		void onRefreshing(@NonNull RefreshLayout refreshLayout, @NonNull RefreshMode mode);
	}

	public interface LoadView {

		@NonNull
		View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container);

		void onViewCreated(@NonNull RefreshLayout refreshLayout, @NonNull View view);

		void onRefreshPull(int scrollOffset, float offset);

		void onRefreshing();

		void onRefreshed();

		int onGetScrollDistance();
	}

	public static abstract class SimpleLoadView implements LoadView {

		private View mContentView;
		private RefreshLayout mRefreshLayout;

		@CallSuper
		@Override
		public void onViewCreated(@NonNull RefreshLayout refreshLayout, @NonNull View view) {
			this.mRefreshLayout = refreshLayout;
			this.mContentView = view;
		}

		@Override
		public int onGetScrollDistance() {
			if (RefreshLayout.VERTICAL == this.mRefreshLayout.getOrientation()) {
				return this.getContentView().getMeasuredHeight();
			}
			return this.getContentView().getMeasuredWidth();
		}

		@NonNull
		public final RefreshLayout getRefreshLayout() {
			return this.mRefreshLayout;
		}

		@NonNull
		public final View getContentView() {
			return this.mContentView;
		}
	}

	public static class SimpleNestedScrollingStep extends NestedScrollingStep {

		protected final int[] mConsumed = new int[2];
		// parent scroll consumed
		protected final int[] mParentScrollConsumed = new int[2];

		@Override
		public void onNestedPreScroll(int dx, int dy, @NonNull int[] consumed, @Nullable int[] offsetInWindow) {
			this.dispatchParentScrollStep(dx, dy, consumed, offsetInWindow);
		}

		@Override
		public void onNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow) {
			this.dispatchChildScrollStep(dxUnconsumed, dyUnconsumed, this.mConsumed, offsetInWindow);
		}

		@NonNull
		@Override
		public final RefreshLayout getAnchorView() {
			return (RefreshLayout) super.getAnchorView();
		}

		protected void dispatchParentScrollStep(int dx, int dy, @NonNull int[] consumed, @Nullable int[] offsetInWindow) {
			int unconsumedX = dx;
			int unconsumedY = dy;
			final NestedScrollingHelper nestedScrollingHelper = this.getNestedScrollingHelper();
			nestedScrollingHelper.startNestedScroll();
			nestedScrollingHelper.dispatchNestedPreScroll(unconsumedX, unconsumedY,
					consumed, this.mParentScrollConsumed);
			unconsumedX -= consumed[0];
			unconsumedY -= consumed[1];
			if (offsetInWindow != null) {
				offsetInWindow[0] += this.mParentScrollConsumed[0];
				offsetInWindow[1] += this.mParentScrollConsumed[1];
			}
			nestedScrollingHelper.dispatchNestedScroll(consumed[0], consumed[1],
					unconsumedX, unconsumedY, this.mParentScrollConsumed);
			if (offsetInWindow != null) {
				offsetInWindow[0] += this.mParentScrollConsumed[0];
				offsetInWindow[1] += this.mParentScrollConsumed[1];
			}
		}

		protected void dispatchChildScrollStep(int dx, int dy, @NonNull int[] consumed, @Nullable int[] offsetInWindow) {
			if (offsetInWindow != null) {
				offsetInWindow[0] = 0;
				offsetInWindow[1] = 0;
			}
			consumed[0] = 0;
			consumed[1] = 0;
			final RefreshLayout refreshLayout = this.getAnchorView();
			final View refreshView = refreshLayout.getRefreshView();
			// default NestedScrollView / RecyclerView
			final boolean hasScrollStep = this.getNestedScrollingHelper().getScrollOffset() == 0
					&& ((refreshView instanceof NestedScrollView)
					|| (refreshView instanceof RecyclerView));
			if (hasScrollStep) {
				if (this.canScrollHorizontally()) {
					refreshView.scrollBy(dx, 0);
				} else if (this.canScrollVertically()) {
					refreshView.scrollBy(0, dy);
				}
			}
		}
	}
}
