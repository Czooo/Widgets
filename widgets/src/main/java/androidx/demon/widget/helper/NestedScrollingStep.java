package androidx.demon.widget.helper;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @Author create by Zoran on 2019-11-03
 * @Email : 171905184@qq.com
 * @Description :
 */
public abstract class NestedScrollingStep {

	private View mAnchorView;
	private NestedScrollingHelper mNestedScrollingHelper;

	public void onNestedPreScroll(int dx, int dy, @NonNull int[] consumed, @Nullable int[] offsetInWindow) {
		// no - op
	}

	public void onNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow) {
		// no - op
	}

	public void onStopNestedScroll() {
		// no - op
	}

	public final boolean canScrollVertically() {
		return this.getNestedScrollingHelper().getCallback().canScrollVertically();
	}

	public final boolean canScrollHorizontally() {
		return this.getNestedScrollingHelper().getCallback().canScrollHorizontally();
	}

	public final boolean canChildScroll(int direction) {
		return this.getNestedScrollingHelper().getCallback().canChildScroll(direction);
	}

	@NonNull
	public View getAnchorView() {
		return this.mAnchorView;
	}

	@NonNull
	public final NestedScrollingHelper getNestedScrollingHelper() {
		return this.mNestedScrollingHelper;
	}

	final boolean dispatchNestedPreScroll(int dx, int dy, @NonNull int[] consumed, @Nullable int[] offsetInWindow) {
		if (offsetInWindow != null) {
			offsetInWindow[0] = 0;
			offsetInWindow[1] = 0;
		}
		consumed[0] = 0;
		consumed[1] = 0;
		this.onNestedPreScroll(dx, dy, consumed, offsetInWindow);
		return consumed[0] != 0 || consumed[1] != 0;
	}

	final boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, @Nullable int[] offsetInWindow) {
		if (offsetInWindow != null) {
			offsetInWindow[0] = 0;
			offsetInWindow[1] = 0;
		}
		this.onNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
		if (offsetInWindow == null) {
			return false;
		}
		return offsetInWindow[0] != 0 || offsetInWindow[1] != 0;
	}

	final void stopNestedScroll() {
		this.onStopNestedScroll();
	}

	final void setNestedScrollingHelper(@Nullable NestedScrollingHelper helper, @Nullable View anchorView) {
		this.mNestedScrollingHelper = helper;
		this.mAnchorView = anchorView;
	}
}
