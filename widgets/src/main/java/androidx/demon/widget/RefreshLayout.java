package androidx.demon.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingParent;
import androidx.customview.view.AbsSavedState;
import androidx.demon.widget.helper.NestedRefreshHelper;
import androidx.demon.widget.helper.NestedScrollingHelper;

/**
 * Author create by ok on 2019-07-22
 * Email : ok@163.com.
 */
public class RefreshLayout extends RelativeLayout implements NestedScrollingParent, NestedScrollingChild {

	public static final int HORIZONTAL = NestedRefreshHelper.HORIZONTAL;

	public static final int VERTICAL = NestedRefreshHelper.VERTICAL;

	public static final int SCROLL_STYLE_FOLLOWED = NestedRefreshHelper.SCROLL_STYLE_FOLLOWED;

	public static final int SCROLL_STYLE_AFTER_FOLLOWED = NestedRefreshHelper.SCROLL_STYLE_AFTER_FOLLOWED;

	public static final int SCROLL_FLAG_NONE = 0;

	public static final int SCROLL_FLAG_START = -1;

	public static final int SCROLL_FLAG_END = 1;

	public static final int SCROLL_FLAG_ALL = 2;

	private final NestedRefreshHelper mNestedRefreshHelper;

	public RefreshLayout(@NonNull Context context) {
		this(context, null);
	}

	public RefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public RefreshLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.setFocusable(true);
		this.setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
		this.setWillNotDraw(false);

		final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout);
		final int mOrientation = mTypedArray.getInt(R.styleable.RefreshLayout_android_orientation, VERTICAL);
		final int mRefreshMode = mTypedArray.getInt(R.styleable.RefreshLayout_refreshMode, RefreshMode.REFRESH_MODE_NONE.getKey());
		mTypedArray.recycle();

		this.mNestedRefreshHelper = new NestedRefreshHelper(this);
		this.mNestedRefreshHelper.setHeaderScrollStyleMode(SCROLL_STYLE_AFTER_FOLLOWED);
		this.mNestedRefreshHelper.setFooterScrollStyleMode(SCROLL_STYLE_AFTER_FOLLOWED);
		this.mNestedRefreshHelper.setRefreshMode(RefreshMode.parse(mRefreshMode));
		this.mNestedRefreshHelper.setOrientation(mOrientation);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		// ensure dragging view
		this.mNestedRefreshHelper.getDragView();
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
		if (this.mNestedRefreshHelper.getNestedScrollingHelper().onInterceptTouchEvent(event)) {
			return true;
		}
		return super.onInterceptTouchEvent(event);
	}

	@Override
	@SuppressLint("ClickableViewAccessibility")
	public boolean onTouchEvent(MotionEvent event) {
		if (this.mNestedRefreshHelper.getNestedScrollingHelper().onTouchEvent(event)) {
			return true;
		}
		return super.onTouchEvent(event);
	}

	// NestedScrollingParent

	@Override
	public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int nestedScrollAxes) {
		return this.mNestedRefreshHelper.getNestedScrollingHelper().onStartNestedScroll(child, target, nestedScrollAxes);
	}

	@Override
	public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes) {
		this.mNestedRefreshHelper.getNestedScrollingHelper().onNestedScrollAccepted(child, target, axes);
	}

	@Override
	public int getNestedScrollAxes() {
		return this.mNestedRefreshHelper.getNestedScrollingHelper().getNestedScrollAxes();
	}

	@Override
	public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed) {
		this.mNestedRefreshHelper.getNestedScrollingHelper().onNestedPreScroll(target, dx, dy, consumed);
	}

	@Override
	public void onNestedScroll(@NonNull final View target, final int dxConsumed, final int dyConsumed, final int dxUnconsumed, final int dyUnconsumed) {
		this.mNestedRefreshHelper.getNestedScrollingHelper().onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
	}

	@Override
	public void onStopNestedScroll(@NonNull View target) {
		this.mNestedRefreshHelper.getNestedScrollingHelper().onStopNestedScroll(target);
	}

	// NestedScrollingChild

	@Override
	public void setNestedScrollingEnabled(boolean enabled) {
		this.mNestedRefreshHelper.getNestedScrollingHelper().setNestedScrollingEnabled(enabled);
	}

	@Override
	public boolean isNestedScrollingEnabled() {
		return this.mNestedRefreshHelper.getNestedScrollingHelper().isNestedScrollingEnabled();
	}

	@Override
	public boolean startNestedScroll(int axes) {
		return this.mNestedRefreshHelper.getNestedScrollingHelper().startNestedScroll(axes);
	}

	@Override
	public void stopNestedScroll() {
		this.mNestedRefreshHelper.getNestedScrollingHelper().stopNestedScroll();
	}

	@Override
	public boolean hasNestedScrollingParent() {
		return this.mNestedRefreshHelper.getNestedScrollingHelper().hasNestedScrollingParent();
	}

	@Override
	public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
		return this.mNestedRefreshHelper.getNestedScrollingHelper().dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
	}

	@Override
	public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
		return this.mNestedRefreshHelper.getNestedScrollingHelper().dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow);
	}

	@Override
	public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
		return this.mNestedRefreshHelper.getNestedScrollingHelper().onNestedPreFling(target, velocityX, velocityY);
	}

	@Override
	public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
		return this.mNestedRefreshHelper.getNestedScrollingHelper().onNestedFling(target, velocityX, velocityY, consumed);
	}

	@Override
	public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
		return this.mNestedRefreshHelper.getNestedScrollingHelper().dispatchNestedFling(velocityX, velocityY, consumed);
	}

	@Override
	public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
		return this.mNestedRefreshHelper.getNestedScrollingHelper().dispatchNestedPreFling(velocityX, velocityY);
	}

	public void setDraggingToStart(boolean start) {
		this.mNestedRefreshHelper.setDraggingToStart(start);
	}

	public void setDraggingToEnd(boolean end) {
		this.mNestedRefreshHelper.setDraggingToEnd(end);
	}

	public void setRefreshMode(@NonNull RefreshMode mode) {
		this.mNestedRefreshHelper.setRefreshMode(mode);
	}

	public void setOrientation(int orientation) {
		this.mNestedRefreshHelper.setOrientation(orientation);
	}

	public void setFrictionRatio(@FloatRange(from = 0.1f, to = 1.f) float frictionRatio) {
		this.mNestedRefreshHelper.setFrictionRatio(frictionRatio);
	}

	public void setRefreshing(boolean refreshing) {
		this.mNestedRefreshHelper.setRefreshing(refreshing);
	}

	public void setRefreshing(boolean refreshing, long delayMillis) {
		this.mNestedRefreshHelper.setRefreshing(refreshing, delayMillis);
	}

	public <V extends LoadView> void setHeaderLoadView(@NonNull V loadView) {
		this.mNestedRefreshHelper.setHeaderLoadView(loadView);
	}

	public <V extends LoadView> void setFooterLoadView(@NonNull V loadView) {
		this.mNestedRefreshHelper.setFooterLoadView(loadView);
	}

	public void setHeaderScrollStyleMode(int scrollStyleMode) {
		this.mNestedRefreshHelper.setHeaderScrollStyleMode(scrollStyleMode);
	}

	public void setFooterScrollStyleMode(int scrollStyleMode) {
		this.mNestedRefreshHelper.setFooterScrollStyleMode(scrollStyleMode);
	}

	public void setOnChildScrollCallback(@NonNull OnChildScrollCallback callback) {
		this.mNestedRefreshHelper.setOnChildScrollCallback(callback);
	}

	public void setOnDragViewOwner(@NonNull OnDragViewOwner owner) {
		this.mNestedRefreshHelper.setOnDragViewOwner(owner);
	}

	public void setOnRefreshListener(@NonNull OnRefreshListener listener) {
		this.mNestedRefreshHelper.setOnRefreshListener(listener);
	}

	public void addOnScrollListener(@NonNull OnScrollListener listener) {
		this.mNestedRefreshHelper.addOnScrollListener(listener);
	}

	public void removeOnScrollListener(@NonNull OnScrollListener listener) {
		this.mNestedRefreshHelper.removeOnScrollListener(listener);
	}

	public boolean isRefreshing() {
		return this.mNestedRefreshHelper.isRefreshing();
	}

	public int getOrientation() {
		return this.mNestedRefreshHelper.getOrientation();
	}

	public float getFrictionRatio() {
		return this.mNestedRefreshHelper.getFrictionRatio();
	}

	@NonNull
	public RefreshMode getRefreshMode() {
		return this.mNestedRefreshHelper.getRefreshMode();
	}

	@NonNull
	public LoadView getHeaderLoadView() {
		return this.mNestedRefreshHelper.getHeaderLoadView();
	}

	@NonNull
	public LoadView getFooterLoadView() {
		return this.mNestedRefreshHelper.getFooterLoadView();
	}

	@NonNull
	public NestedScrollingHelper getNestedScrollingHelper() {
		return this.mNestedRefreshHelper.getNestedScrollingHelper();
	}

	public interface OnDragViewOwner {

		View getDragView(@NonNull RefreshLayout refreshLayout);
	}

	public interface OnChildScrollCallback {

		boolean canChildScroll(@NonNull RefreshLayout refreshLayout, int direction);
	}

	public interface OnRefreshListener {

		void onRefreshing(@NonNull RefreshLayout refreshLayout, @NonNull RefreshMode mode);
	}

	public interface OnScrollListener {

		void onScrolled(@NonNull RefreshLayout refreshLayout, int dx, int dy);
	}

	public interface LoadView {

		@NonNull
		View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container);

		void onViewCreated(@NonNull RefreshLayout refreshLayout, @NonNull View container);

		int onGetScrollDistance(@NonNull RefreshLayout refreshLayout, @NonNull View container);

		void onRefreshPull(@NonNull RefreshLayout refreshLayout, int scrollOffset, float offset);

		void onRefreshing(@NonNull RefreshLayout refreshLayout);

		void onRefreshed(@NonNull RefreshLayout refreshLayout);
	}

	public static abstract class SimpleLoadView implements LoadView {

		private View mContentView;

		@CallSuper
		@Override
		public void onViewCreated(@NonNull RefreshLayout refreshLayout, @NonNull View container) {
			this.mContentView = container;
		}

		@Override
		public int onGetScrollDistance(@NonNull RefreshLayout refreshLayout, @NonNull View container) {
			if (RefreshLayout.VERTICAL == refreshLayout.getOrientation()) {
				return container.getMeasuredHeight();
			}
			return container.getMeasuredWidth();
		}

		@NonNull
		public final View getContentView() {
			return this.mContentView;
		}
	}

	public static class LayoutParams extends RelativeLayout.LayoutParams {

		public int mScrollFlag = SCROLL_FLAG_NONE;

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(LayoutParams source) {
			super(source);
		}

		public LayoutParams(MarginLayoutParams source) {
			super(source);
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.RefreshLayout_Layout);
			this.mScrollFlag = mTypedArray.getInt(R.styleable.RefreshLayout_Layout_scrollFlag, SCROLL_FLAG_NONE);
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
