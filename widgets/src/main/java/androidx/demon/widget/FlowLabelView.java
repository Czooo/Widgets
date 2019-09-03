package androidx.demon.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

/**
 * Author create by ok on 2019-08-08
 * Email : ok@163.com.
 */
public class FlowLabelView extends ViewGroup {

	private static final boolean DEBUG = true;
	private static final String TAG = "FlowLabelView";

	private static final int DEFAULT_GRID_GAP = 100;

	private int mWidthMargin = DEFAULT_GRID_GAP;
	private int mHeightMargin = DEFAULT_GRID_GAP;

	private boolean mInLayout;

	public FlowLabelView(Context context) {
		super(context);
	}

	public FlowLabelView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public FlowLabelView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
		final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
		final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

		final int paddingLeft = this.getPaddingLeft();
		final int paddingTop = this.getPaddingTop();
		final int paddingRight = this.getPaddingRight();
		final int paddingBottom = this.getPaddingBottom();

		this.preMeasure(widthMeasureSpec, heightMeasureSpec);

		int widthCum = paddingLeft;
		int heightCum = paddingTop;
		int maxRowHeight = 0;

		int rowIndex = 0;
		int colIndex = 0;
		boolean feedRow;

		int maxWidth = widthSize;
		int maxHeight = paddingTop + paddingBottom;

		for (int index = 0; index < this.getChildCount(); index++) {
			final View preChildView = this.getChildAt(index);
			if (View.GONE == preChildView.getVisibility()) {
				continue;
			}
			final LayoutParams preLayoutParams = (LayoutParams) preChildView.getLayoutParams();
			preLayoutParams.rowIndex = rowIndex;
			preLayoutParams.colIndex = colIndex;

			colIndex++;
			widthCum += preLayoutParams.preWidth + this.mWidthMargin;
			maxRowHeight = Math.max(preLayoutParams.preHeight, maxRowHeight);

			if ((feedRow = widthCum + paddingRight > widthSize)) {
				widthCum = paddingLeft;
				rowIndex++;
				colIndex = 0;
				index--;
				// 保存行最大高度
				preLayoutParams.upRowMaxHeight = maxRowHeight;
			}
			if (feedRow || index == this.getChildCount() - 1) {
				maxHeight += maxRowHeight;

				if (feedRow) {
					maxRowHeight = 0;
					maxHeight += this.mHeightMargin;
				}
			}
		}
		this.setMeasuredDimension(
				widthMode == MeasureSpec.EXACTLY ? widthSize : maxWidth,
				heightMode == MeasureSpec.EXACTLY ? heightSize : maxHeight
		);
	}

	private void preMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		final int paddingLeft = this.getPaddingLeft();
		final int paddingTop = this.getPaddingTop();
		final int paddingRight = this.getPaddingRight();
		final int paddingBottom = this.getPaddingBottom();

		for (int index = 0; index < this.getChildCount(); index++) {
			final View preChildView = this.getChildAt(index);
			if (View.GONE == preChildView.getVisibility()) {
				continue;
			}
			final LayoutParams preLayoutParams = (LayoutParams) preChildView.getLayoutParams();
			final int childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(widthMeasureSpec, paddingLeft + paddingRight + preLayoutParams.leftMargin + preLayoutParams.rightMargin,
					preLayoutParams.width);
			final int childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(heightMeasureSpec, paddingTop + paddingBottom + preLayoutParams.topMargin + preLayoutParams.bottomMargin,
					preLayoutParams.height);
			preChildView.measure(childWidthMeasureSpec, childHeightMeasureSpec);
			preLayoutParams.preWidth = preChildView.getMeasuredWidth() + preLayoutParams.leftMargin + preLayoutParams.rightMargin;
			preLayoutParams.preHeight = preChildView.getMeasuredHeight() + preLayoutParams.topMargin + preLayoutParams.bottomMargin;
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		final int widthSize = right - left;
		final int heightSize = bottom - top;

		final int paddingLeft = this.getPaddingLeft();
		final int paddingTop = this.getPaddingTop();
		final int paddingRight = this.getPaddingRight();
		final int paddingBottom = this.getPaddingBottom();

		int widthCum = paddingLeft;
		int heightCum = paddingTop;

		for (int index = 0; index < this.getChildCount(); index++) {
			final View preChildView = this.getChildAt(index);
			if (View.GONE == preChildView.getVisibility()) {
				continue;
			}
			final LayoutParams preLayoutParams = (LayoutParams) preChildView.getLayoutParams();
			if (preLayoutParams.rowIndex > 0 && preLayoutParams.colIndex == 0) {
				widthCum = paddingLeft;
				heightCum += preLayoutParams.upRowMaxHeight + this.mHeightMargin;
			}

			int preChildLeft = widthCum + preLayoutParams.leftMargin;
			int preChildTop = heightCum + preLayoutParams.topMargin;
			widthCum += preLayoutParams.preWidth + this.mWidthMargin;

			preChildView.layout(preChildLeft, preChildTop, preChildLeft + preChildView.getMeasuredWidth(), preChildTop + preChildView.getMeasuredHeight());
		}
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(this.getContext(), attrs);
	}

	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
		return new LayoutParams(layoutParams);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
		return layoutParams instanceof LayoutParams;
	}

	@Override
	public void requestLayout() {
		if (!this.mInLayout) {
			super.requestLayout();
		}
	}

	public static class LayoutParams extends MarginLayoutParams {

		private Rect boundRect;

		private int rowIndex = -1;
		private int colIndex = -1;

		private int preWidth;
		private int preHeight;

		private int upRowMaxHeight;

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(LayoutParams source) {
			super(source);
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
	}
}
