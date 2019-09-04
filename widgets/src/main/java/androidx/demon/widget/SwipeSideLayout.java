package androidx.demon.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.customview.view.AbsSavedState;

/**
 * Author create by ok on 2019-09-04
 * Email : ok@163.com.
 */
public class SwipeSideLayout extends DragRelativeLayout {

	public SwipeSideLayout(Context context) {
		this(context, null);
	}

	public SwipeSideLayout(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public SwipeSideLayout(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeSideLayout);
		final float mMinScrollScale = mTypedArray.getFloat(R.styleable.SwipeSideLayout_minScrollScale, 0.2F);
		final boolean mIsDrawerEnabled = mTypedArray.getBoolean(R.styleable.SwipeSideLayout_drawerEnabled, true);
		final int mDampSpringBackSize = mTypedArray.getDimensionPixelSize(R.styleable.SwipeSideLayout_dampSpringBackSize, 0);
		mTypedArray.recycle();

		// So that we can catch the back button
		this.setFocusableInTouchMode(true);
		this.setMotionEventSplittingEnabled(false);
		this.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);

		ViewCompat.setImportantForAccessibility(this,
				ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);

		this.setDragManager(new SwipeSideDragManager());
		this.setDampSpringBackSize(mDampSpringBackSize);
		this.setDrawerEnabled(mIsDrawerEnabled);
		this.setMinScrollScale(mMinScrollScale);
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		this.getDrawerView();
		this.getDragView();
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
	protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
		return layoutParams instanceof LayoutParams;
	}

	@Nullable
	@Override
	protected Parcelable onSaveInstanceState() {
		Parcelable mSuperState = super.onSaveInstanceState();
		SavedState mSavedState = new SavedState(mSuperState);
		mSavedState.mIsOpenState = this.isOpenState();
		return mSavedState;
	}

	@Override
	protected void onRestoreInstanceState(Parcelable state) {
		if (state instanceof SavedState) {
			SavedState mSavedState = (SavedState) state;
			super.onRestoreInstanceState(mSavedState.mSuperState);
			this.setOpenState(mSavedState.mIsOpenState);
		} else {
			super.onRestoreInstanceState(state);
		}
	}

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		if (this.isOpenState()) {
			this.setOpenState(true, false);
		}
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (this.isOpenState()) {
			this.setOpenState(false, false);
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode && this.isOpenState()) {
			event.startTracking();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode && this.isOpenState()) {
			this.setOpenState(false);
			return true;
		}
		return super.onKeyUp(keyCode, event);
	}

	@Override
	public boolean canScrollVertically() {
		if (this.shouldStartNestedScroll()) {
			return this.getDragManager().checkViewVertically(this.getDrawerView());
		}
		return super.canScrollVertically();
	}

	@Override
	public boolean canScrollHorizontally() {
		if (this.shouldStartNestedScroll()) {
			return this.getDragManager().checkViewHorizontally(this.getDrawerView());
		}
		return super.canScrollHorizontally();
	}

	@NonNull
	@Override
	public SwipeSideDragManager getDragManager() {
		final DragManager mDragManager = super.getDragManager();
		if (mDragManager == null) {
			throw new IllegalStateException("not has a DragManager created");
		}
		return (SwipeSideDragManager) mDragManager;
	}

	public boolean switchDrawer() {
		return this.switchDrawer(true);
	}

	public boolean switchDrawer(boolean smooth) {
		final boolean openState = !this.isOpenState();
		this.setOpenState(openState, smooth);
		return openState;
	}

	public void setOpenState(boolean openState) {
		this.setOpenState(openState, true);
	}

	public void setOpenState(boolean openState, boolean smooth) {
		this.getDragManager().setOpenState(openState, smooth);
	}

	public void setDrawerEnabled(boolean enabled) {
		this.getDragManager().setDrawerEnabled(enabled);
	}

	public void setMinScrollScale(@FloatRange(from = 0.01F, to = 1.F) float minScrollScale) {
		this.getDragManager().setMinScrollScale(minScrollScale);
	}

	public void setDampSpringBackSize(int dampSpringBackSize) {
		this.getDragManager().setDampSpringBackSize(dampSpringBackSize);
	}

	public void setOnOpenStateListener(@NonNull OnOpenStateListener listener) {
		this.getDragManager().setOnOpenStateListener(listener);
	}

	public boolean isOpenState() {
		return this.getDragManager().isOpenState();
	}

	public View getDragView() {
		return this.getDragManager().getDragView();
	}

	public View getDrawerView() {
		return this.getDragManager().getDrawerView();
	}

	public interface OnOpenStateListener {

		void onOpenStateChanged(@NonNull SwipeSideLayout swipeSideLayout);
	}

	public static class LayoutParams extends DragRelativeLayout.LayoutParams {

		public static final int GRAVITY_SCROLL_NONE = 0;
		public static final int GRAVITY_SCROLL_LEFT = 1;
		public static final int GRAVITY_SCROLL_TOP = 2;
		public static final int GRAVITY_SCROLL_RIGHT = 3;
		public static final int GRAVITY_SCROLL_BOTTOM = 4;

		public int scrollGravity = GRAVITY_SCROLL_NONE;

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(@NonNull LayoutParams source) {
			super(source);
			this.scrollGravity = source.scrollGravity;
		}

		public LayoutParams(@NonNull MarginLayoutParams source) {
			super(source);
		}

		public LayoutParams(@NonNull ViewGroup.LayoutParams source) {
			super(source);
		}

		public LayoutParams(@NonNull Context context, @Nullable AttributeSet attrs) {
			super(context, attrs);
			final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.SwipeSideLayout_Layout);
			this.scrollGravity = mTypedArray.getInteger(R.styleable.SwipeSideLayout_Layout_scroll_gravity, GRAVITY_SCROLL_NONE);
			mTypedArray.recycle();

			switch (this.scrollGravity) {
				case LayoutParams.GRAVITY_SCROLL_LEFT:
					this.addRule(ALIGN_PARENT_START);
					this.addRule(CENTER_VERTICAL);
					break;
				case LayoutParams.GRAVITY_SCROLL_TOP:
					this.addRule(ALIGN_PARENT_TOP);
					this.addRule(CENTER_HORIZONTAL);
					break;
				case LayoutParams.GRAVITY_SCROLL_RIGHT:
					this.addRule(ALIGN_PARENT_END);
					this.addRule(CENTER_VERTICAL);
					break;
				case LayoutParams.GRAVITY_SCROLL_BOTTOM:
					this.addRule(ALIGN_PARENT_BOTTOM);
					this.addRule(CENTER_HORIZONTAL);
					break;
			}
		}
	}

	public static class SavedState extends AbsSavedState {

		private boolean mIsOpenState;
		private Parcelable mSuperState;

		SavedState(Parcelable superState) {
			super(superState);
			this.mSuperState = superState;
		}

		SavedState(Parcel source, ClassLoader loader) {
			super(source, loader);
			if (loader == null) {
				loader = getClass().getClassLoader();
			}
			this.mIsOpenState = source.readInt() == 1;
			this.mSuperState = source.readParcelable(loader);
		}

		@Override
		public void writeToParcel(Parcel dest, int flags) {
			super.writeToParcel(dest, flags);
			dest.writeInt(this.mIsOpenState ? 1 : 0);
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
