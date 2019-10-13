package androidx.demon.widget.helper;

import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.NestedScrollingParent;

/**
 * Author create by ok on 2019-07-19
 * Email : ok@163.com.
 */
public interface NestedScrollingHelper extends NestedScrollingParent, NestedScrollingChild2 {

	/**
	 * Indicates that the pager is in an idle, settled state. The current page
	 * is fully in view and no animation is in progress.
	 */
	int SCROLL_STATE_IDLE = 0;

	/**
	 * Indicates that the pager is currently being dragged by the user.
	 */
	int SCROLL_STATE_DRAGGING = 1;

	/**
	 * Indicates that the pager is in the process of settling to a final position.
	 */
	int SCROLL_STATE_SETTLING = 2;

	boolean onInterceptTouchEvent(@NonNull MotionEvent event);

	boolean onTouchEvent(@NonNull MotionEvent event);

	boolean isNestedScrollInProgress();

	void addOnScrollListener(@NonNull OnScrollListener listener);

	void removeOnScrollListener(@NonNull OnScrollListener listener);

	void setScrollingDuration(int duration);

	void stopScroll();

	void scrollTo(int x, int y);

	void scrollTo(int x, int y, long delayMillis);

	void smoothScrollTo(int x, int y);

	void smoothScrollTo(int x, int y, long delayMillis);

	int getPreScrollDirection(int delta);

	int getScrollDirection();

	int getScrollState();

	int getScrollOffset();

	int getScrollOffsetX();

	int getScrollOffsetY();

	interface Callback {

		boolean canScrollVertically();

		boolean canScrollHorizontally();

		boolean shouldStartNestedScroll();

		boolean canChildScroll(int direction);

		void onScrollBy(@NonNull NestedScrollingHelper helper, int dx, int dy, @NonNull int[] consumed);

		void onScrollStateChanged(@NonNull NestedScrollingHelper helper, int scrollState);
	}

	interface OnScrollListener {

		void onScrollStateChanged(@NonNull NestedScrollingHelper helper, int scrollState);

		void onScrolled(@NonNull NestedScrollingHelper helper, int dx, int dy);
	}
}
