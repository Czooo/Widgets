package androidx.demon.widget.managers;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Author create by ok on 2019-07-19
 * Email : ok@163.com.
 */
public class StickLinearLayoutManager extends RecyclerView.LayoutManager {

	private static final boolean DEBUG = true;
	private static final String TAG = "StickLayoutManager";

	/**
	 * Create a default <code>LayoutParams</code> object for a child of the RecyclerView.
	 *
	 * <p>LayoutManagers will often want to use a custom <code>LayoutParams</code> type
	 * to store extra information specific to the layout. Client code should subclass
	 * {@link RecyclerView.LayoutParams} for this purpose.</p>
	 *
	 * <p><em>Important:</em> if you use your own custom <code>LayoutParams</code> type
	 * you must also override
	 * {@link #generateLayoutParams(ViewGroup.LayoutParams)} and
	 * {@link #generateLayoutParams(Context, AttributeSet)}.</p>
	 *
	 * @return A new LayoutParams for a child view
	 */
	@Override
	public RecyclerView.LayoutParams generateDefaultLayoutParams() {
		return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
		if (state.getItemCount() == 0) {
			this.removeAndRecycleAllViews(recycler);
			return;
		}
		this.detachAndScrapAttachedViews(recycler);
		this.ensureLayoutState();

		final LayoutState layoutState = this.mLayoutState;
		layoutState.mRecycle = false;

		layoutState.mInfinite = this.resolveIsInfinite();
		layoutState.mIsPreLayout = state.isPreLayout();

		layoutState.mCurrentPosition = 0;
		layoutState.mScrollingOffset = 0;
		layoutState.mOffset = 0;
		layoutState.mItemDirection = LayoutState.ITEM_DIRECTION_TAIL;
		layoutState.mLayoutDirection = LayoutState.LAYOUT_END;
		this.populate(recycler, state, layoutState, false);
	}

	@Override
	public void onLayoutCompleted(RecyclerView.State state) {
		super.onLayoutCompleted(state);
	}

	@Override
	public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
		return this.scrollBy(dy, recycler, state);
	}

	@Override
	public boolean canScrollVertically() {
		return true;
	}

	int scrollBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
		if (getChildCount() == 0 || dy == 0) {
			return 0;
		}
		mLayoutState.mRecycle = true;
		this.ensureLayoutState();
		final LayoutState layoutState = this.mLayoutState;
		final int layoutDirection = dy > 0 ? LayoutState.LAYOUT_END : LayoutState.LAYOUT_START;
		final int absDy = Math.abs(dy);
		updateLayoutState(layoutDirection, absDy, true, state);
		final int consumed = layoutState.mScrollingOffset
				+ populate(recycler, state, layoutState,false);
		if (consumed < 0) {
			if (DEBUG) {
				Log.d(TAG, "Don't have any more elements to scroll");
			}
			return 0;
		}
		final int scrolled = absDy > consumed ? layoutDirection * consumed : dy;
		this.mOrientationHelper.offsetChildren(-scrolled);
		if (DEBUG) {
			Log.d(TAG, "scroll req: " + dy + " scrolled: " + scrolled);
		}
		layoutState.mLastScrollDelta = scrolled;
		return scrolled;
	}

	private void updateLayoutState(int layoutDirection, int requiredSpace,  boolean canUseExistingSpace, RecyclerView.State state) {
		// If parent provides a hint, don't measure unlimited.
		mLayoutState.mInfinite = resolveIsInfinite();
		mLayoutState.mExtra = getExtraLayoutSpace(state);
		mLayoutState.mLayoutDirection = layoutDirection;
		int scrollingOffset;
		if (layoutDirection == LayoutState.LAYOUT_END) {
			mLayoutState.mExtra += mOrientationHelper.getEndPadding();
			// get the first child in the direction we are going
			final View child = getChildClosestToEnd();
			// the direction in which we are traversing children
			mLayoutState.mItemDirection = LayoutState.ITEM_DIRECTION_HEAD;
			mLayoutState.mCurrentPosition = getPosition(child) + mLayoutState.mItemDirection;
			mLayoutState.mOffset = mOrientationHelper.getDecoratedEnd(child);
			// calculate how much we can scroll without adding new children (independent of layout)
			scrollingOffset = mOrientationHelper.getDecoratedEnd(child)
					- mOrientationHelper.getEndAfterPadding();

		} else {
			final View child = getChildClosestToStart();
			mLayoutState.mExtra += mOrientationHelper.getStartAfterPadding();
			mLayoutState.mItemDirection = LayoutState.ITEM_DIRECTION_TAIL;
			mLayoutState.mCurrentPosition = getPosition(child) + mLayoutState.mItemDirection;
			mLayoutState.mOffset = mOrientationHelper.getDecoratedStart(child);
			scrollingOffset = -mOrientationHelper.getDecoratedStart(child)
					+ mOrientationHelper.getStartAfterPadding();
		}
		mLayoutState.mAvailable = requiredSpace;
		if (canUseExistingSpace) {
			mLayoutState.mAvailable -= scrollingOffset;
		}
		mLayoutState.mScrollingOffset = scrollingOffset;
	}

	private int populate(RecyclerView.Recycler recycler, RecyclerView.State state,
						 LayoutState layoutState, boolean stopOnFocusable) {
		if (DEBUG) {
			Log.e(TAG, "populate start : hasMore(" + layoutState.hasMore(state) + "), infinite(" + layoutState.mInfinite + ")");
		}
		final int start = layoutState.mAvailable;

		if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
			if (layoutState.mAvailable < 0) {
				layoutState.mScrollingOffset += layoutState.mAvailable;
			}
			this.recycleByLayoutState(recycler, layoutState);
		}

		final LayoutChunkState mLayoutChunkState = this.mLayoutChunkState;
		while (/*layoutState.mInfinite &&*/ layoutState.hasMore(state)) {
			mLayoutChunkState.resetInternal();
			this.layoutChunk(recycler, state, layoutState, mLayoutChunkState);

			if (mLayoutChunkState.mFinished) {
				break;
			}
			layoutState.mOffset += mLayoutChunkState.mConsumed * layoutState.mLayoutDirection;

			if (layoutState.mScrollingOffset != LayoutState.SCROLLING_OFFSET_NaN) {
				layoutState.mScrollingOffset += mLayoutChunkState.mConsumed;
				if (layoutState.mAvailable < 0) {
					layoutState.mScrollingOffset += layoutState.mAvailable;
				}
				this.recycleByLayoutState(recycler, layoutState);
			}

			if (stopOnFocusable && mLayoutChunkState.mFocusable) {
				break;
			}
		}
		return start - layoutState.mAvailable;
	}

	private void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state,
							 LayoutState layoutState, LayoutChunkState layoutChunkState) {
		final View preItemView = layoutState.next(recycler);
		if (preItemView == null) {
			layoutChunkState.mFinished = true;
			return;
		}
		this.addView(preItemView);
		this.measureChildWithMargins(preItemView, 0, 0);
		layoutChunkState.mConsumed = this.mOrientationHelper.getDecoratedMeasurement(preItemView);

		int left, top, right, bottom;

		if (this.isLayoutRTL()) {
			right = this.getWidth() - this.getPaddingRight();
			left = right - this.mOrientationHelper.getDecoratedMeasurementInOther(preItemView);
		} else {
			left = this.getPaddingLeft();
			right = left + this.mOrientationHelper.getDecoratedMeasurementInOther(preItemView);
		}
		if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
			bottom = layoutState.mOffset;
			top = layoutState.mOffset - layoutChunkState.mConsumed;
		} else {
			top = layoutState.mOffset;
			bottom = layoutState.mOffset + layoutChunkState.mConsumed;
		}
		this.layoutDecoratedWithMargins(preItemView, left, top, right, bottom);

		final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) preItemView.getLayoutParams();
		if (params.isItemRemoved() || params.isItemChanged()) {
			layoutChunkState.mIgnoreConsumed = true;
		}
		layoutChunkState.mFocusable = preItemView.hasFocusable();
	}

	private void recycleByLayoutState(RecyclerView.Recycler recycler, LayoutState layoutState) {
		if(DEBUG) {
			Log.e(TAG, "recycleByLayoutState " + layoutState.mRecycle + " , " + layoutState.mInfinite + " , " + layoutState.mScrollingOffset);
		}
		if (!layoutState.mRecycle || layoutState.mInfinite) {
			return;
		}
		if (layoutState.mLayoutDirection == LayoutState.LAYOUT_START) {
			this.recycleViewsFromEnd(recycler, layoutState.mScrollingOffset);
		} else {
			this.recycleViewsFromStart(recycler, layoutState.mScrollingOffset);
		}
	}

	private void recycleViewsFromStart(RecyclerView.Recycler recycler, int dt) {
		if (dt < 0) {
			if (DEBUG) {
				Log.d(TAG, "Called recycle from start with a negative value. This might happen"
						+ " during layout changes but may be sign of a bug");
			}
			return;
		}

		for (int index = this.getChildCount() - 1; index >= 0; index--) {
			final View child = this.getChildAt(index);
			if (this.mOrientationHelper.getDecoratedEnd(child) > dt
					|| this.mOrientationHelper.getTransformedEndWithDecoration(child) > dt) {
				this.recycleChildren(recycler, this.getChildCount() - 1, index);
				return;
			}
		}

//		for (int index = 0; index < this.getChildCount(); index++) {
//			final View child = this.getChildAt(index);
//			if (this.mOrientationHelper.getDecoratedEnd(child) > dt
//					|| this.mOrientationHelper.getTransformedEndWithDecoration(child) > dt) {
//				this.recycleChildren(recycler, 0, index);
//				return;
//			}
//		}
	}

	private void recycleViewsFromEnd(RecyclerView.Recycler recycler, int dt) {
		if (dt < 0) {
			if (DEBUG) {
				Log.d(TAG, "Called recycle from start with a negative value. This might happen"
						+ " during layout changes but may be sign of a bug");
			}
			return;
		}

		for (int index = 0; index < this.getChildCount(); index++) {
			final View child = this.getChildAt(index);
			if (this.mOrientationHelper.getDecoratedStart(child) < dt
					|| this.mOrientationHelper.getTransformedStartWithDecoration(child) < dt) {
				this.recycleChildren(recycler, 0, index);
				return;
			}
		}

//		for (int index = this.getChildCount() - 1; index >= 0; index--) {
//			final View child = this.getChildAt(index);
//			if (this.mOrientationHelper.getDecoratedStart(child) < dt
//					|| this.mOrientationHelper.getTransformedStartWithDecoration(child) < dt) {
//				this.recycleChildren(recycler, this.getChildCount() - 1, index);
//				return;
//			}
//		}
	}

	private void recycleChildren(RecyclerView.Recycler recycler, int startIndex, int endIndex) {
		if (startIndex == endIndex) {
			return;
		}
		if (endIndex > startIndex) {
			for (int index = endIndex - 1; index >= startIndex; index--) {
				this.removeAndRecycleViewAt(index, recycler);
			}
		} else {
			for (int index = startIndex; index > endIndex; index--) {
				this.removeAndRecycleViewAt(index, recycler);
			}
		}
	}

	private final LayoutChunkState mLayoutChunkState = new LayoutChunkState();

	private LayoutState mLayoutState;

	private OrientationHelper mOrientationHelper = OrientationHelper.createVerticalHelper(this);

	void ensureLayoutState() {
		if (this.mLayoutState == null) {
			this.mLayoutState = new LayoutState();
		}
	}

	private View getChildClosestToStart() {
		return getChildAt(getChildCount() - 1);
	}

	private View getChildClosestToEnd() {
		return getChildAt(0);
	}

	private int getExtraLayoutSpace(RecyclerView.State state) {
		if (state.hasTargetScrollPosition()) {
			return this.mOrientationHelper.getTotalSpace();
		} else {
			return 0;
		}
	}

	private boolean isLayoutRTL() {
		return this.getLayoutDirection() == ViewCompat.LAYOUT_DIRECTION_RTL;
	}

	private boolean resolveIsInfinite() {
		return  this.mOrientationHelper.getMode() == View.MeasureSpec.UNSPECIFIED
				&&  this.mOrientationHelper.getEnd() == 0;
	}

	static class LayoutState {

		static final int LAYOUT_START = -1;

		static final int LAYOUT_END = 1;

		static final int INVALID_LAYOUT = Integer.MIN_VALUE;

		static final int ITEM_DIRECTION_HEAD = -1;

		static final int ITEM_DIRECTION_TAIL = 1;

		static final int SCROLLING_OFFSET_NaN = Integer.MIN_VALUE;

		boolean mRecycle = true;

		boolean mInfinite = false;

		boolean mIsPreLayout = false;

		int mLayoutDirection;

		int mItemDirection;

		int mCurrentPosition;

		int mScrollingOffset;

		int mLastScrollDelta;

		int mAvailable;

		int mOffset;

		int mExtra;

		boolean hasMore(RecyclerView.State state) {
			return mCurrentPosition >= 0 && mCurrentPosition < state.getItemCount();
		}

		View next(RecyclerView.Recycler recycler) {
			final View view = recycler.getViewForPosition(mCurrentPosition);
			mCurrentPosition += mItemDirection;
			return view;
		}
	}

	static class LayoutChunkState {

		int mConsumed;
		boolean mFinished;
		boolean mFocusable;
		boolean mIgnoreConsumed;

		void resetInternal() {
			mConsumed = 0;
			mFinished = false;
			mFocusable = false;
			mIgnoreConsumed = false;
		}
	}
}
