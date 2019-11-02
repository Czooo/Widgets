package androidx.demon.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;

import androidx.annotation.CallSuper;
import androidx.annotation.FloatRange;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingParent;
import androidx.customview.view.AbsSavedState;
import androidx.demon.widget.helper.NestedScrollingHelper;
import androidx.demon.widget.helper.NestedScrollingHelperImpl;

/**
 * Author create by ok on 2019-09-04
 * Email : ok@163.com.
 */
public class DragRelativeLayout extends RelativeLayout implements NestedScrollingParent, NestedScrollingChild, NestedScrollingHelper.Callback {

	public static final int HORIZONTAL = LinearLayout.HORIZONTAL;

	public static final int VERTICAL = LinearLayout.VERTICAL;

	/**
	 * @hide
	 */
	@IntDef({HORIZONTAL, VERTICAL})
	@Retention(RetentionPolicy.SOURCE)
	@interface OrientationMode {
	}

	private static final float FRICTION_RATIO = .22F;

	private final NestedScrollingHelper mNestedScrollingHelper;

	@OrientationMode
	private int mOrientation = VERTICAL;
	private float mFrictionRatio = FRICTION_RATIO;
	private boolean mIsShouldStartNestedScroll = false;
	private boolean mIsDraggingToStart = true;
	private boolean mIsDraggingToEnd = true;

	private DragManager mDragManager;
	private OnChildScrollCallback mOnChildScrollCallback;
	private ArrayList<OnScrollListener> mOnScrollListeners;

	public DragRelativeLayout(Context context) {
		this(context, null);
	}

	public DragRelativeLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public DragRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);

		this.setFocusable(true);
		this.setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
		this.setWillNotDraw(false);

		final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.DragRelativeLayout);
		final int mOrientation = mTypedArray.getInt(R.styleable.DragRelativeLayout_android_orientation, this.mOrientation);
		final float mFrictionRatio = mTypedArray.getFloat(R.styleable.DragRelativeLayout_frictionRatio, this.mFrictionRatio);
		final boolean mIsShouldStartNestedScroll = mTypedArray.getBoolean(R.styleable.DragRelativeLayout_shouldStartNestedScroll, this.mIsShouldStartNestedScroll);
		mTypedArray.recycle();

		this.setOrientation(mOrientation);
		this.setFrictionRatio(mFrictionRatio);
		this.setShouldStartNestedScroll(mIsShouldStartNestedScroll);
		this.mNestedScrollingHelper = new NestedScrollingHelperImpl(this, this);
	}

	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable superState = super.onSaveInstanceState();
		SavedState savedState = new SavedState(superState);
		savedState.mOrientation = this.getOrientation();
		return savedState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof SavedState) {
			SavedState savedState = (SavedState) state;
			super.onRestoreInstanceState(savedState.getSuperState());
			this.setOrientation(savedState.mOrientation);
			return;
		}
		super.onRestoreInstanceState(state);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
		return layoutParams instanceof LayoutParams;
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
	}

	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
		return new LayoutParams(layoutParams);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(this.getContext(), attrs);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (this.mNestedScrollingHelper.onInterceptTouchEvent(event)) {
			return true;
		}
		return super.onInterceptTouchEvent(event);
	}

	@Override
	@SuppressLint("ClickableViewAccessibility")
	public boolean onTouchEvent(MotionEvent event) {
		if (this.mNestedScrollingHelper.onTouchEvent(event)) {
			return true;
		}
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int nestedScrollAxes) {
		return this.mNestedScrollingHelper.onStartNestedScroll(child, target, nestedScrollAxes);
	}

	@Override
	public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {
		this.mNestedScrollingHelper.onNestedScrollAccepted(child, target, axes);
	}

	@Override
	public int getNestedScrollAxes() {
		return this.mNestedScrollingHelper.getNestedScrollAxes();
	}

	@Override
	public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
		this.mNestedScrollingHelper.onNestedPreScroll(target, dx, dy, consumed);
	}

	@Override
	public void onNestedScroll(@NonNull final View target, final int dxConsumed, final int dyConsumed, final int dxUnconsumed, final int dyUnconsumed) {
		this.mNestedScrollingHelper.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
	}

	@Override
	public void onStopNestedScroll(@NonNull View target) {
		this.mNestedScrollingHelper.onStopNestedScroll(target);
	}

	// NestedScrollingChild

	@Override
	public void setNestedScrollingEnabled(boolean enabled) {
		this.mNestedScrollingHelper.setNestedScrollingEnabled(enabled);
	}

	@Override
	public boolean isNestedScrollingEnabled() {
		return this.mNestedScrollingHelper.isNestedScrollingEnabled();
	}

	@Override
	public boolean startNestedScroll(int axes) {
		return this.mNestedScrollingHelper.startNestedScroll(axes);
	}

	@Override
	public void stopNestedScroll() {
		this.mNestedScrollingHelper.stopNestedScroll();
	}

	@Override
	public boolean hasNestedScrollingParent() {
		return this.mNestedScrollingHelper.hasNestedScrollingParent();
	}

	@Override
	public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
		return this.mNestedScrollingHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
	}

	@Override
	public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
		return this.mNestedScrollingHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
	}

	@Override
	public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
		return this.dispatchNestedPreFling(velocityX, velocityY);
	}

	@Override
	public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
		return this.dispatchNestedFling(velocityX, velocityY, consumed);
	}

	@Override
	public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
		return this.mNestedScrollingHelper.dispatchNestedFling(velocityX, velocityY, consumed);
	}

	@Override
	public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
		return this.mNestedScrollingHelper.dispatchNestedPreFling(velocityX, velocityY);
	}

	// NestedScrollingHelper

	@CallSuper
	@Override
	public boolean canScrollVertically() {
		return VERTICAL == this.mOrientation;
	}

	@CallSuper
	@Override
	public boolean canScrollHorizontally() {
		return HORIZONTAL == this.mOrientation;
	}

	@CallSuper
	@Override
	public boolean shouldStartNestedScroll() {
		if (this.getChildCount() <= 0) {
			return false;
		}
		if (this.mDragManager == null) {
			return false;
		}
		return this.mIsShouldStartNestedScroll
				&& this.mDragManager.shouldStartNestedScroll();
	}

	@CallSuper
	@Override
	public boolean canChildScroll(int direction) {
		if ((!this.isDraggingToStart() && direction < 0)
				|| (!this.isDraggingToEnd() && direction > 0)) {
			return true;
		}
		if (this.mOnChildScrollCallback != null) {
			return this.mOnChildScrollCallback.canChildScroll(this, direction);
		}
		if (this.mDragManager == null) {
			return true;
		}
		return this.mDragManager.canChildScroll(direction);
	}

	@CallSuper
	@Override
	public void onScrollBy(@NonNull NestedScrollingHelper helper, int dx, int dy, @NonNull int[] consumed) {
		this.mDragManager.onScrollBy(helper, dx, dy, consumed);

		if (this.mOnScrollListeners != null) {
			for (OnScrollListener listener : this.mOnScrollListeners) {
				listener.onScrolled(this, consumed[0], consumed[1]);
			}
		}
	}

	@CallSuper
	@Override
	public void onScrollStateChanged(@NonNull NestedScrollingHelper helper, int scrollState) {
		this.mDragManager.onScrollStateChanged(helper, scrollState);

		if (this.mOnScrollListeners != null) {
			for (OnScrollListener listener : this.mOnScrollListeners) {
				listener.onScrollStateChanged(this, scrollState);
			}
		}
	}

	public void setShouldStartNestedScroll(boolean shouldStartNestedScroll) {
		this.mIsShouldStartNestedScroll = shouldStartNestedScroll;
	}

	public void setDraggingToStart(boolean start) {
		this.mIsDraggingToStart = start;
	}

	public void setDraggingToEnd(boolean end) {
		this.mIsDraggingToEnd = end;
	}

	@CallSuper
	public void setDragManager(@Nullable DragManager manager) {
		if (this.mDragManager != null) {
			this.mDragManager.onDetachedFromWindow(this);
			this.mDragManager.setDragRelativeLayout(null);
		}
		this.mDragManager = manager;
		if (this.mDragManager != null) {
			this.mDragManager.setDragRelativeLayout(this);
			this.mDragManager.onAttachedToWindow(this);
		}
	}

	@CallSuper
	public void setOrientation(@OrientationMode int orientation) {
		if (this.mOrientation != orientation) {
			this.mOrientation = orientation;
			this.requestLayout();
		}
	}

	public void setFrictionRatio(@FloatRange(from = 0.1f, to = 1.f) float frictionRatio) {
		this.mFrictionRatio = frictionRatio;
	}

	public void setOnChildScrollCallback(@NonNull OnChildScrollCallback callback) {
		this.mOnChildScrollCallback = callback;
	}

	public void addOnScrollListener(@NonNull OnScrollListener listener) {
		if (this.mOnScrollListeners == null) {
			this.mOnScrollListeners = new ArrayList<>();
		}
		this.mOnScrollListeners.add(listener);
	}

	public void removeOnScrollListener(@NonNull OnScrollListener listener) {
		if (this.mOnScrollListeners != null) {
			this.mOnScrollListeners.remove(listener);
		}
	}

	public boolean isDraggingToStart() {
		return this.mIsDraggingToStart;
	}

	public boolean isDraggingToEnd() {
		return this.mIsDraggingToEnd;
	}

	public int getOrientation() {
		return this.mOrientation;
	}

	public float getFrictionRatio() {
		return this.mFrictionRatio;
	}

	@Nullable
	public DragManager getDragManager() {
		return this.mDragManager;
	}

	@NonNull
	public NestedScrollingHelper getNestedScrollingHelper() {
		return this.mNestedScrollingHelper;
	}

	public static abstract class DragManager {

		private DragRelativeLayout mDragRelativeLayout;

		@CallSuper
		public void onAttachedToWindow(@NonNull ViewGroup container) {

		}

		@CallSuper
		public void onDetachedFromWindow(@NonNull ViewGroup container) {

		}

		public abstract boolean shouldStartNestedScroll();

		public abstract boolean canChildScroll(int direction);

		public abstract void onScrollBy(@NonNull NestedScrollingHelper helper, int dx, int dy, @NonNull int[] consumed);

		public abstract void onScrollStateChanged(@NonNull NestedScrollingHelper helper, int scrollState);

		public void addView(View child) {
			this.addView(child, -1);
		}

		public void addView(View child, int index) {
			this.addView(child, index, null);
		}

		public void addView(View child, int index, @Nullable ViewGroup.LayoutParams layoutParams) {
			this.addViewInternal(child, index, layoutParams);
		}

		public void scrollTo(int x, int y) {
			this.getNestedScrollingHelper().scrollTo(x, y);
		}

		public void scrollTo(int x, int y, long delayMillis) {
			this.getNestedScrollingHelper().scrollTo(x, y, delayMillis);
		}

		public void smoothScrollTo(int x, int y) {
			this.getNestedScrollingHelper().smoothScrollTo(x, y);
		}

		public void smoothScrollTo(int x, int y, long delayMillis) {
			this.getNestedScrollingHelper().smoothScrollTo(x, y, delayMillis);
		}

		public final boolean canScrollVertically() {
			return this.getDragRelativeLayout().canScrollVertically();
		}

		public final boolean canScrollHorizontally() {
			return this.getDragRelativeLayout().canScrollHorizontally();
		}

		public final int getScrollState() {
			return this.getNestedScrollingHelper().getScrollState();
		}

		public final int getOrientation() {
			return this.getDragRelativeLayout().getOrientation();
		}

		public final float getFrictionRatio() {
			return this.getDragRelativeLayout().getFrictionRatio();
		}

		public final NestedScrollingHelper getNestedScrollingHelper() {
			return this.getDragRelativeLayout().getNestedScrollingHelper();
		}

		public final DragRelativeLayout getDragRelativeLayout() {
			return this.mDragRelativeLayout;
		}

		final void setDragRelativeLayout(@Nullable DragRelativeLayout dragRelativeLayout) {
			synchronized (this) {
				this.mDragRelativeLayout = dragRelativeLayout;
			}
		}

		final void addViewInternal(View child, int index, @Nullable ViewGroup.LayoutParams layoutParams) {
			if (layoutParams == null) {
				layoutParams = this.getDragRelativeLayout().generateDefaultLayoutParams();
			} else if (!(layoutParams instanceof LayoutParams)) {
				layoutParams = this.getDragRelativeLayout().generateLayoutParams(layoutParams);
			}
			this.getDragRelativeLayout().addView(child, index, layoutParams);
		}
	}

	public interface OnChildScrollCallback {

		boolean canChildScroll(@NonNull ViewGroup container, int direction);
	}

	public interface OnScrollListener {

		void onScrolled(@NonNull ViewGroup container, int dx, int dy);

		void onScrollStateChanged(@NonNull ViewGroup container, int scrollState);
	}

	public static class LayoutParams extends RelativeLayout.LayoutParams {

		public static final int SCROLL_FLAG_NONE = 0;

		public static final int SCROLL_FLAG_START = -1;

		public static final int SCROLL_FLAG_END = 1;

		public static final int SCROLL_FLAG_ALL = 2;

		public int mScrollFlag = SCROLL_FLAG_NONE;

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(LayoutParams source) {
			super(source);
			this.mScrollFlag = source.mScrollFlag;
		}

		public LayoutParams(MarginLayoutParams source) {
			super(source);
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.DragRelativeLayout_Layout);
			this.mScrollFlag = mTypedArray.getInt(R.styleable.DragRelativeLayout_Layout_scrollFlag, SCROLL_FLAG_NONE);
			mTypedArray.recycle();
		}
	}

	public static class SavedState extends AbsSavedState {

		private int mOrientation;
		private Parcelable mSuperState;

		SavedState(Parcel source) {
			super(source);
		}

		SavedState(Parcel source, ClassLoader loader) {
			super(source, loader);
			if (loader == null) {
				loader = getClass().getClassLoader();
			}
			this.mOrientation = source.readInt();
			this.mSuperState = source.readParcelable(loader);
		}

		SavedState(Parcelable superState) {
			super(superState);
			this.mSuperState = superState;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(this.mOrientation);
			dest.writeParcelable(this.mSuperState, flags);
		}

		public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.ClassLoaderCreator<SavedState>() {

			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in, null);
			}

			@Override
			public SavedState createFromParcel(Parcel source, ClassLoader loader) {
				return new SavedState(source, loader);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}
}
