package androidx.demon.widget.adapter;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.OrientationHelper;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @Author create by Zoran on 2019-10-26
 * @Email : 171905184@qq.com
 * @Description :
 */
public class FixedGridLayoutManager extends RecyclerView.LayoutManager {

	private final LayoutState mLayoutState =
			new LayoutState();

	private SpanSizeLookup mSpanSizeLookup =
			new DefaultSpanSizeLookup();
	private OrientationHelper mOrientationHelper =
			OrientationHelper.createVerticalHelper(this);
	private int mSpanCount = 1;

	public FixedGridLayoutManager() {
		this(1);
	}

	public FixedGridLayoutManager(int spanCount) {
		this.setSpanCount(spanCount);
	}

	/**
	 * Create a default <code>LayoutParams</code> object for a child of the RecyclerView.
	 *
	 * <p>LayoutManagers will often want to use a custom <code>LayoutParams</code> type
	 * to store extra information specific to the layout. Client code should subclass
	 * {@link RecyclerView.LayoutParams} for this purpose.</p>
	 *
	 * <p><em>Important:</em> if you use your own custom <code>LayoutParams</code> type
	 * you must also override
	 * {@link #checkLayoutParams(RecyclerView.LayoutParams)},
	 * {@link #generateLayoutParams(ViewGroup.LayoutParams)} and
	 * {@link #generateLayoutParams(Context, AttributeSet)}.</p>
	 *
	 * @return A new LayoutParams for a child view
	 */
	@Override
	public RecyclerView.LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
	}

	/**
	 * Create a LayoutParams object suitable for this LayoutManager, copying relevant
	 * values from the supplied LayoutParams object if possible.
	 *
	 * <p><em>Important:</em> if you use your own custom <code>LayoutParams</code> type
	 * you must also override
	 * {@link #checkLayoutParams(RecyclerView.LayoutParams)},
	 * {@link #generateLayoutParams(ViewGroup.LayoutParams)} and
	 * {@link #generateLayoutParams(Context, AttributeSet)}.</p>
	 *
	 * @param layoutParams Source LayoutParams object to copy values from
	 * @return a new LayoutParams object
	 */
	@Override
	public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
		if (layoutParams instanceof ViewGroup.MarginLayoutParams) {
			return new LayoutParams((ViewGroup.MarginLayoutParams) layoutParams);
		} else {
			return new LayoutParams(layoutParams);
		}
	}

	/**
	 * Create a LayoutParams object suitable for this LayoutManager from
	 * an inflated layout resource.
	 *
	 * <p><em>Important:</em> if you use your own custom <code>LayoutParams</code> type
	 * you must also override
	 * {@link #checkLayoutParams(RecyclerView.LayoutParams)},
	 * {@link #generateLayoutParams(ViewGroup.LayoutParams)} and
	 * {@link #generateLayoutParams(Context, AttributeSet)}.</p>
	 *
	 * @param context Context for obtaining styled attributes
	 * @param attrs   AttributeSet describing the supplied arguments
	 * @return a new LayoutParams object
	 */
	@Override
	public RecyclerView.LayoutParams generateLayoutParams(Context context, AttributeSet attrs) {
		return new LayoutParams(context, attrs);
	}

	/**
	 * Determines the validity of the supplied LayoutParams object.
	 *
	 * <p>This should check to make sure that the object is of the correct type
	 * and all values are within acceptable ranges. The default implementation
	 * returns <code>true</code> for non-null params.</p>
	 *
	 * @param layoutParams LayoutParams object to check
	 * @return true if this LayoutParams object is valid, false otherwise
	 */
	@Override
	public boolean checkLayoutParams(RecyclerView.LayoutParams layoutParams) {
		return layoutParams instanceof LayoutParams;
	}

	/**
	 * @return <code>True</code> if the measuring pass of layout should use the AutoMeasure
	 * mechanism of {@link RecyclerView} or <code>False</code> if it should be done by the
	 * LayoutManager's implementation of
	 * {@link RecyclerView.LayoutManager#onMeasure(RecyclerView.Recycler, RecyclerView.State, int, int)}.
	 * @see #setMeasuredDimension(Rect, int, int)
	 * @see #onMeasure(RecyclerView.Recycler, RecyclerView.State, int, int)
	 */
	@Override
	public boolean isAutoMeasureEnabled() {
		return true;
	}

	/**
	 * Sets the measured dimensions from the given bounding box of the children and the
	 * measurement specs that were passed into onMeasure(int, int). It is
	 * only called if a LayoutManager returns <code>true</code> from
	 * {@link #isAutoMeasureEnabled()} and it is called after the RecyclerView calls
	 * {@link #onLayoutChildren(RecyclerView.Recycler, RecyclerView.State)} in the execution of
	 * <p>
	 * This method must call {@link #setMeasuredDimension(int, int)}.
	 * <p>
	 * The default implementation adds the RecyclerView's padding to the given bounding box
	 * then caps the value to be within the given measurement specs.
	 *
	 * @param childrenBounds The bounding box of all children
	 * @param wSpec          The widthMeasureSpec that was passed into the RecyclerView.
	 * @param hSpec          The heightMeasureSpec that was passed into the RecyclerView.
	 * @see #isAutoMeasureEnabled()
	 * @see #setMeasuredDimension(int, int)
	 */
	@Override
	public void setMeasuredDimension(Rect childrenBounds, int wSpec, int hSpec) {
		if (this.mCachedBorders == null) {
			super.setMeasuredDimension(childrenBounds, wSpec, hSpec);
		}
		this.setMeasuredDimension(childrenBounds.width(), this.mLayoutState.mLayoutOffset);
	}

	/**
	 * Lay out all relevant child views from the given adapter.
	 * <p>
	 * The LayoutManager is in charge of the behavior of item animations. By default,
	 * RecyclerView has a non-null {@link RecyclerView#getItemAnimator() ItemAnimator}, and simple
	 * item animations are enabled. This means that add/remove operations on the
	 * adapter will result in animations to add new or appearing items, removed or
	 * disappearing items, and moved items. If a LayoutManager returns false from
	 * {@link #supportsPredictiveItemAnimations()}, which is the default, and runs a
	 * normal layout operation during {@link #onLayoutChildren(RecyclerView.Recycler, RecyclerView.State)}, the
	 * RecyclerView will have enough information to run those animations in a simple
	 * way. For example, the default ItemAnimator, {@link DefaultItemAnimator}, will
	 * simply fade views in and out, whether they are actually added/removed or whether
	 * they are moved on or off the screen due to other add/remove operations.
	 *
	 * <p>A LayoutManager wanting a better item animation experience, where items can be
	 * animated onto and off of the screen according to where the items exist when they
	 * are not on screen, then the LayoutManager should return true from
	 * {@link #supportsPredictiveItemAnimations()} and add additional logic to
	 * {@link #onLayoutChildren(RecyclerView.Recycler, RecyclerView.State)}. Supporting predictive animations
	 * means that {@link #onLayoutChildren(RecyclerView.Recycler, RecyclerView.State)} will be called twice;
	 * once as a "pre" layout step to determine where items would have been prior to
	 * a real layout, and again to do the "real" layout. In the pre-layout phase,
	 * items will remember their pre-layout positions to allow them to be laid out
	 * appropriately. Also, {@link RecyclerView.LayoutParams#isItemRemoved() removed} items will
	 * be returned from the scrap to help determine correct placement of other items.
	 * These removed items should not be added to the child list, but should be used
	 * to help calculate correct positioning of other views, including views that
	 * were not previously onscreen (referred to as APPEARING views), but whose
	 * pre-layout offscreen position can be determined given the extra
	 * information about the pre-layout removed views.</p>
	 *
	 * <p>The second layout pass is the real layout in which only non-removed views
	 * will be used. The only additional requirement during this pass is, if
	 * {@link #supportsPredictiveItemAnimations()} returns true, to note which
	 * views exist in the child list prior to layout and which are not there after
	 * layout (referred to as DISAPPEARING views), and to position/layout those views
	 * appropriately, without regard to the actual bounds of the RecyclerView. This allows
	 * the animation system to know the location to which to animate these disappearing
	 * views.</p>
	 *
	 * <p>The default LayoutManager implementations for RecyclerView handle all of these
	 * requirements for animations already. Clients of RecyclerView can either use one
	 * of these layout managers directly or look at their implementations of
	 * onLayoutChildren() to see how they account for the APPEARING and
	 * DISAPPEARING views.</p>
	 *
	 * @param recycler Recycler to use for fetching potentially cached views for a
	 *                 position
	 * @param state    Transient state of RecyclerView
	 */
	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
		if (state.getItemCount() <= 0) {
			this.removeAndRecycleAllViews(recycler);
			return;
		}
		this.detachAndScrapAttachedViews(recycler);
		this.onAnchorReady(recycler, state);
		this.layoutChunk(recycler, state);
	}

	private View[] mSet;
	private int[] mCachedBorders;

	private void onAnchorReady(RecyclerView.Recycler recycler, RecyclerView.State state) {
		if (this.mSet == null || this.mSet.length != this.mSpanCount) {
			this.mSet = new View[this.mSpanCount];
		}
		this.updateMeasurements();
	}

	private void layoutChunk(RecyclerView.Recycler recycler, RecyclerView.State state) {
		this.detachAndScrapAttachedViews(recycler);

		final LayoutState layoutState = this.mLayoutState;
		layoutState.resetInternal();
		// Row count is one more than the last item's row index.
		int rowItemCount = this.getSpanGroupIndex(recycler, state, state.getItemCount() - 1) + 1;
		for (int rowIndex = 0; rowIndex < rowItemCount; rowIndex++) {
			this.layoutRowChunk(recycler, state, layoutState);
		}
	}

	private void layoutRowChunk(RecyclerView.Recycler recycler, RecyclerView.State state, LayoutState layoutState) {
		final int currentOtherDirSize = this.getChildCount() > 0 ? this.mCachedBorders[this.mSpanCount] : 0;

		int colItemCount = 0;
		int spanIndexCount = 0;
		int remainingSpan = this.mSpanCount;
		while (colItemCount < this.mSpanCount && layoutState.hasMore(state) && remainingSpan > 0) {
			final int tempPosition = layoutState.mCurrentPosition;
			final int tempSpanSize = this.getSpanSize(recycler, state, tempPosition);
			if (tempSpanSize > this.mSpanCount) {
				throw new IllegalArgumentException("Item at position " + tempPosition + " requires "
						+ tempSpanSize + " spans but FixedGridLayoutManager has only " + this.mSpanCount
						+ " spans.");
			}
			remainingSpan -= tempSpanSize;
			if (remainingSpan < 0) {
				break;
			}
			final View child = layoutState.next(recycler);
			if (child == null) {
				break;
			}
			final int position = this.getPosition(child);
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();
			lp.mSpanSize = this.getSpanSize(recycler, state, position);
			lp.mSpanGroupIndex = this.getSpanGroupIndex(recycler, state, position);
			lp.mSpanIndex = spanIndexCount;
			spanIndexCount += lp.mSpanSize;
			this.mSet[colItemCount++] = child;
		}
		if (colItemCount == 0) {
			return;
		}

		int maxSize = 0;
		float maxSizeInOther = 0;
		for (int position = 0; position < colItemCount; position++) {
			final View child = this.mSet[position];
			// add
			this.addView(child);
			// measure
			this.measureChild(child, this.mOrientationHelper.getModeInOther(), false);

			maxSize = Math.max(this.mOrientationHelper.getDecoratedMeasurement(child), maxSize);
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();
			final float otherSize = 1.F * this.mOrientationHelper.getDecoratedMeasurementInOther(child) / lp.mSpanSize;
			maxSizeInOther = Math.max(otherSize, maxSizeInOther);
		}

		if (this.mOrientationHelper.getModeInOther() != View.MeasureSpec.EXACTLY) {
			// re-distribute columns
			this.guessMeasurement(maxSizeInOther, currentOtherDirSize);
			// now we should re-measure any item that was match parent.
			maxSize = 0;
			for (int position = 0; position < colItemCount; position++) {
				View child = this.mSet[position];
				this.measureChild(child, View.MeasureSpec.EXACTLY, true);
				maxSize = Math.max(this.mOrientationHelper.getDecoratedMeasurement(child), maxSize);
			}
		}

		// Views that did not measure the maxSize has to be re-measured
		// We will stop doing this once we introduce Gravity in the GLM layout params
		for (int position = 0; position < colItemCount; position++) {
			final View child = this.mSet[position];
			if (this.mOrientationHelper.getDecoratedMeasurement(child) != maxSize) {
				final Rect mDecorInsets = new Rect();
				this.calculateItemDecorationsForChild(child, mDecorInsets);
				final LayoutParams lp = (LayoutParams) child.getLayoutParams();
				final int verticalInsets = mDecorInsets.top + mDecorInsets.bottom
						+ lp.topMargin + lp.bottomMargin;
				final int horizontalInsets = mDecorInsets.left + mDecorInsets.right
						+ lp.leftMargin + lp.rightMargin;
				final int totalSpaceInOther = this.getSpaceForSpanRange(lp.mSpanIndex, lp.mSpanSize);
				final int wSpec = getChildMeasureSpec(totalSpaceInOther, View.MeasureSpec.EXACTLY, horizontalInsets, lp.width, false);
				final int hSpec = View.MeasureSpec.makeMeasureSpec(maxSize - verticalInsets, View.MeasureSpec.EXACTLY);
				this.measureChildWithDecorationsAndMargin(child, wSpec, hSpec, true);
			}
		}

		int left = 0, right = 0, top = 0, bottom = 0;
		top = layoutState.mLayoutOffset;
		bottom = top + maxSize;
		for (int position = 0; position < colItemCount; position++) {
			final View child = this.mSet[position];
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();
			left = this.getPaddingLeft() + this.mCachedBorders[lp.mSpanIndex];
			right = left + this.mOrientationHelper.getDecoratedMeasurementInOther(child);
			this.layoutDecoratedWithMargins(child, left, top, right, bottom);
		}
		layoutState.mLayoutOffset += maxSize;
	}

	private int getSpanSize(RecyclerView.Recycler recycler, RecyclerView.State state, int position) {
		if (!state.isPreLayout()) {
			return this.mSpanSizeLookup.getSpanSize(position);
		}
		final int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(position);
		if (adapterPosition == -1) {
			return 1;
		}
		return this.mSpanSizeLookup.getSpanSize(adapterPosition);
	}

	private int getSpanIndex(RecyclerView.Recycler recycler, RecyclerView.State state, int position) {
		if (!state.isPreLayout()) {
			return this.mSpanSizeLookup.getSpanIndex(position, this.mSpanCount);
		}
		final int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(position);
		if (adapterPosition == -1) {
			return 0;
		}
		return this.mSpanSizeLookup.getSpanIndex(adapterPosition, this.mSpanCount);
	}

	private int getSpanGroupIndex(RecyclerView.Recycler recycler, RecyclerView.State state, int position) {
		if (!state.isPreLayout()) {
			return this.mSpanSizeLookup.getSpanGroupIndex(position, this.mSpanCount);
		}
		final int adapterPosition = recycler.convertPreLayoutPositionToPostLayout(position);
		if (adapterPosition == -1) {
			return 0;
		}
		return this.mSpanSizeLookup.getSpanGroupIndex(adapterPosition, this.mSpanCount);
	}

	private int getSpaceForSpanRange(int spanIndex, int spanSize) {
		return this.mCachedBorders[spanIndex + spanSize] - this.mCachedBorders[spanIndex];
	}

	private void guessMeasurement(float maxSizeInOther, int currentOtherDirSize) {
		final int contentSize = Math.round(maxSizeInOther * mSpanCount);
		// always re-calculate because borders were stretched during the fill
		this.calculateItemBorders(Math.max(contentSize, currentOtherDirSize));
	}

	private void updateMeasurements() {
		final int totalSpace = this.getWidth() - this.getPaddingRight() - this.getPaddingLeft();
		this.calculateItemBorders(totalSpace);
	}

	private void calculateItemBorders(int totalSpace) {
		this.mCachedBorders = this.calculateItemBorders(this.mCachedBorders, this.mSpanCount, totalSpace);
	}

	private int[] calculateItemBorders(int[] cachedBorders, int spanCount, int totalSpace) {
		if (cachedBorders == null || cachedBorders.length != spanCount + 1
				|| cachedBorders[cachedBorders.length - 1] != totalSpace) {
			cachedBorders = new int[spanCount + 1];
		}
		cachedBorders[0] = 0;
		int sizePerSpan = totalSpace / spanCount;
		int sizePerSpanRemainder = totalSpace % spanCount;
		int consumedPixels = 0;
		int additionalSize = 0;
		for (int i = 1; i <= spanCount; i++) {
			int itemSize = sizePerSpan;
			additionalSize += sizePerSpanRemainder;
			if (additionalSize > 0 && (spanCount - additionalSize) < sizePerSpanRemainder) {
				itemSize += 1;
				additionalSize -= spanCount;
			}
			consumedPixels += itemSize;
			cachedBorders[i] = consumedPixels;
		}
		return cachedBorders;
	}

	private void measureChild(View child, int otherDirParentSpecMode, boolean alreadyMeasured) {
		final Rect mDecorInsets = new Rect();
		this.calculateItemDecorationsForChild(child, mDecorInsets);
		final LayoutParams lp = (LayoutParams) child.getLayoutParams();
		final int verticalInsets = mDecorInsets.top + mDecorInsets.bottom
				+ lp.topMargin + lp.bottomMargin;
		final int horizontalInsets = mDecorInsets.left + mDecorInsets.right
				+ lp.leftMargin + lp.rightMargin;
		final int availableSpaceInOther = this.getSpaceForSpanRange(lp.mSpanIndex, lp.mSpanSize);
		final int wSpec;
		final int hSpec;
		wSpec = getChildMeasureSpec(availableSpaceInOther, otherDirParentSpecMode, horizontalInsets, lp.width, false);
		hSpec = getChildMeasureSpec(this.mOrientationHelper.getTotalSpace(), this.getHeightMode(), verticalInsets, lp.height, false);
		this.measureChildWithDecorationsAndMargin(child, wSpec, hSpec, alreadyMeasured);
	}

	private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec, boolean alreadyMeasured) {
		final LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
		final boolean measure;
		if (alreadyMeasured) {
			measure = this.shouldReMeasureChild(child, widthSpec, heightSpec, layoutParams);
		} else {
			measure = this.shouldMeasureChild(child, widthSpec, heightSpec, layoutParams);
		}
		if (measure) {
			child.measure(widthSpec, heightSpec);
		}
	}

	private boolean shouldReMeasureChild(View child, int widthSpec, int heightSpec, LayoutParams layoutParams) {
		return !this.isMeasurementCacheEnabled()
				|| !this.isMeasurementUpToDate(child.getMeasuredWidth(), widthSpec, layoutParams.width)
				|| !this.isMeasurementUpToDate(child.getMeasuredHeight(), heightSpec, layoutParams.height);
	}

	private boolean shouldMeasureChild(View child, int widthSpec, int heightSpec, LayoutParams layoutParams) {
		return child.isLayoutRequested()
				|| !this.isMeasurementCacheEnabled()
				|| !this.isMeasurementUpToDate(child.getWidth(), widthSpec, layoutParams.width)
				|| !this.isMeasurementUpToDate(child.getHeight(), heightSpec, layoutParams.height);
	}

	private boolean isMeasurementUpToDate(int childSize, int spec, int dimension) {
		final int specMode = View.MeasureSpec.getMode(spec);
		final int specSize = View.MeasureSpec.getSize(spec);
		if (dimension > 0 && childSize != dimension) {
			return false;
		}
		switch (specMode) {
			case View.MeasureSpec.UNSPECIFIED:
				return true;
			case View.MeasureSpec.AT_MOST:
				return specSize >= childSize;
			case View.MeasureSpec.EXACTLY:
				return specSize == childSize;
		}
		return false;
	}

	public int getSpanIndex(int position) {
		return this.mSpanSizeLookup.getSpanIndex(position, this.mSpanCount);
	}

	public int getLastSpanIndex() {
		return this.mSpanCount - 1;
	}

	public int getSpanGroupIndex(int position) {
		return this.mSpanSizeLookup.getSpanGroupIndex(position, this.mSpanCount);
	}

	public int getLastSpanGroupIndex() {
		return this.mSpanSizeLookup.getSpanGroupIndex(this.getItemCount() - 1, this.mSpanCount);
	}

	public int getLastSpanGroupIndex(RecyclerView.State state) {
		return this.mSpanSizeLookup.getSpanGroupIndex(state.getItemCount() - 1, this.mSpanCount);
	}

	public void setSpanCount(int spanCount) {
		if (this.mSpanCount != spanCount) {
			this.mSpanCount = spanCount;
			this.requestLayout();
		}
	}

	public int getSpanCount() {
		return this.mSpanCount;
	}

	public void setSpanSizeLookup(SpanSizeLookup spanSizeLookup) {
		this.mSpanSizeLookup = spanSizeLookup;
	}

	@NonNull
	public SpanSizeLookup getSpanSizeLookup() {
		return this.mSpanSizeLookup;
	}

	private static class LayoutState {

		private int mLayoutOffset = 0;

		private int mCurrentPosition = 0;

		public boolean hasMore(RecyclerView.State state) {
			return this.mCurrentPosition >= 0 && this.mCurrentPosition < state.getItemCount();
		}

		public View next(RecyclerView.Recycler recycler) {
			return recycler.getViewForPosition(this.mCurrentPosition++);
		}

		public void resetInternal() {
			this.mLayoutOffset = 0;
			this.mCurrentPosition = 0;
		}
	}

	public static abstract class SpanSizeLookup {

		public abstract int getSpanSize(int position);

		public int getSpanIndex(int position, int spanCount) {
			return position % spanCount;
		}

		public int getSpanGroupIndex(int position, int spanCount) {
			int positionGroupSize = 0;
			int positionSpanCount = 0;
			int positionSpanSize = this.getSpanSize(position);
			for (int startPosition = 0; startPosition < position; startPosition++) {
				final int spanSize = this.getSpanSize(startPosition);
				positionSpanCount += spanSize;
				if (positionSpanCount >= spanCount) {
					if (positionSpanCount == spanCount) {
						positionSpanCount = 0;
					} else {
						positionSpanCount = spanSize;
					}
					positionGroupSize++;
				}
			}
			if (positionSpanCount + positionSpanSize > spanCount) {
				positionGroupSize++;
			}
			return positionGroupSize;
		}
	}

	public static class DefaultSpanSizeLookup extends SpanSizeLookup {

		@Override
		public int getSpanSize(int position) {
			return 1;
		}
	}

	public static class LayoutParams extends RecyclerView.LayoutParams {

		/**
		 * Span Id for Views that are not laid out yet.
		 */
		public static final int INVALID_SPAN_ID = -1;

		private int mSpanSize = 0;

		private int mSpanIndex = INVALID_SPAN_ID;

		private int mSpanGroupIndex = INVALID_SPAN_ID;

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}

		public LayoutParams(RecyclerView.LayoutParams source) {
			super(source);
		}

		public LayoutParams(ViewGroup.MarginLayoutParams source) {
			super(source);
		}

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
		}

		/**
		 * Returns the current span index of this View. If the View is not laid out yet, the return
		 * value is <code>undefined</code>.
		 * <p>
		 * Starting with RecyclerView <b>24.2.0</b>, span indices are always indexed from position 0
		 * even if the layout is RTL. In a vertical GridLayoutManager, <b>leftmost</b> span is span
		 * 0 if the layout is <b>LTR</b> and <b>rightmost</b> span is span 0 if the layout is
		 * <b>RTL</b>. Prior to 24.2.0, it was the opposite which was conflicting with
		 * {@link SpanSizeLookup#getSpanIndex(int, int)}.
		 * <p>
		 * If the View occupies multiple spans, span with the minimum index is returned.
		 *
		 * @return The span index of the View.
		 */
		public int getSpanIndex() {
			return mSpanIndex;
		}

		public int getSpanGroupIndex() {
			return mSpanGroupIndex;
		}

		/**
		 * Returns the number of spans occupied by this View. If the View not laid out yet, the
		 * return value is <code>undefined</code>.
		 *
		 * @return The number of spans occupied by this View.
		 */
		public int getSpanSize() {
			return mSpanSize;
		}
	}
}
