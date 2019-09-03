package androidx.demon.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.FocusFinder;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EdgeEffect;
import android.widget.Scroller;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import androidx.annotation.CallSuper;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Px;
import androidx.core.content.ContextCompat;
import androidx.core.os.TraceCompat;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingChildHelper;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.widget.EdgeEffectCompat;
import androidx.customview.view.AbsSavedState;
import androidx.viewpager.widget.PagerTitleStrip;

/**
 * Author create by ok on 2019-07-05
 * Email : ok@163.com.
 */
public class ViewPagerCompat extends ViewGroup implements NestedScrollingChild {

	private static final String TAG = "ViewPagerCompat";
	private static final boolean DEBUG = false;
	private static final boolean USE_CACHE = false;

	/**
	 * Indicates that the pager is in an idle, settled state. The current page
	 * is fully in view and no animation is in progress.
	 */
	public static final int SCROLL_STATE_IDLE = 0;

	/**
	 * Indicates that the pager is currently being dragged by the user.
	 */
	public static final int SCROLL_STATE_DRAGGING = 1;

	/**
	 * Indicates that the pager is in the process of settling to a final position.
	 */
	public static final int SCROLL_STATE_SETTLING = 2;

	/**
	 * Annotation which allows marking of views to be decoration views when added to a view
	 * pager.
	 *
	 * <p>Views marked with this annotation can be added to the view pager with a layout resource.
	 * An example being {@link PagerTitleStrip}.</p>
	 *
	 * <p>You can also control whether a view is a decor view but setting
	 * {@link ViewPagerCompat.LayoutParams#isDecor} on the child's layout params.</p>
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.TYPE)
	@Inherited
	@interface DecorView {
	}

	public static final int HORIZONTAL = 0;

	public static final int VERTICAL = 1;

	@IntDef({HORIZONTAL, VERTICAL})
	@Retention(RetentionPolicy.SOURCE)
	@interface OrientationMode {
	}

	private static final int INVALID_POINTER = -1;
	private static final int DEFAULT_OFFSCREEN_PAGES = 1;
	private static final int DEFAULT_GUTTER_SIZE = 16; // dips
	private static final int DEFAULT_CLOSE_ENOUGH = 2; // dips
	private static final int MAX_SETTLE_DURATION = 850; // ms
	private static final int MIN_DISTANCE_FOR_FLING = 25; // dips
	private static final int MIN_FLING_VELOCITY_VERTICAL = 400; // dips
	private static final int MIN_FLING_VELOCITY_HORIZONTAL = 600; // dips

	public ViewPagerCompat(Context context) {
		this(context, null);
	}

	public ViewPagerCompat(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public ViewPagerCompat(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		this.performInit(context, attrs);
	}

	private NestedScrollingChildHelper mScrollingChildHelper;

	private int mTouchSlop;
	private int mGutterSize;
	private int mCloseEnough;
	private int mFlingDistance;

	private int mMinimumVelocity;
	private int mMaximumVelocity;
	private int mScrollingDuration;
	private Scroller mScroller;
	private EdgeEffect mStartEdgeEffect;
	private EdgeEffect mEndEdgeEffect;

	void performInit(@NonNull Context context, AttributeSet attrs) {
		this.setWillNotDraw(false);
		this.setDescendantFocusability(FOCUS_AFTER_DESCENDANTS);
		this.setFocusable(true);

		final ViewConfiguration mViewConfiguration = ViewConfiguration.get(context);
		this.mTouchSlop = mViewConfiguration.getScaledTouchSlop();
		this.mMinimumVelocity = mViewConfiguration.getScaledMinimumFlingVelocity();
		this.mMaximumVelocity = mViewConfiguration.getScaledMaximumFlingVelocity();

		final float density = context.getResources().getDisplayMetrics().density;
		this.mGutterSize = (int) (DEFAULT_GUTTER_SIZE * density);
		this.mCloseEnough = (int) (DEFAULT_CLOSE_ENOUGH * density);
		this.mFlingDistance = (int) (MIN_DISTANCE_FOR_FLING * density);

		this.mScroller = new Scroller(context);
		this.mStartEdgeEffect = new EdgeEffect(context);
		this.mEndEdgeEffect = new EdgeEffect(context);

		this.mScrollingChildHelper = new NestedScrollingChildHelper(this);
		// ...because why else would you be using this widget?
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			this.setNestedScrollingEnabled(true);
		}

		final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, R.styleable.ViewPagerCompat);
		final int mOrientation = mTypedArray.getInt(R.styleable.ViewPagerCompat_android_orientation, this.mOrientation);
		final int mPageLimit = mTypedArray.getInt(R.styleable.ViewPagerCompat_pageLimit, this.mOffscreenPageLimit);
		final int mScrollingDuration = mTypedArray.getInt(R.styleable.ViewPagerCompat_scrollingDuration, this.mScrollingDuration);
		final boolean mScrollingLoop = mTypedArray.getBoolean(R.styleable.ViewPagerCompat_scrollingLoop, this.mIsScrollingLoop);
		final boolean mAllowUserScrollable = mTypedArray.getBoolean(R.styleable.ViewPagerCompat_scrollingInAllowUser, this.mIsAllowUserScrollable);
		mTypedArray.recycle();

		this.setOrientation(mOrientation);
		this.setScrollingLoop(mScrollingLoop);
		this.setOffscreenPageLimit(mPageLimit);
		this.setScrollingDuration(mScrollingDuration);
		this.setAllowUserScrollable(mAllowUserScrollable);

		ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegate());

		if (ViewCompat.getImportantForAccessibility(this)
				== ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_AUTO) {
			ViewCompat.setImportantForAccessibility(this,
					ViewCompat.IMPORTANT_FOR_ACCESSIBILITY_YES);
		}

		ViewCompat.setOnApplyWindowInsetsListener(this, new androidx.core.view.OnApplyWindowInsetsListener() {

			private final Rect mTempRect = new Rect();

			@Override
			public WindowInsetsCompat onApplyWindowInsets(final View view, final WindowInsetsCompat originalInsets) {
				// First let the BannerView itself try and consume them...
				final WindowInsetsCompat applied = ViewCompat.onApplyWindowInsets(view, originalInsets);
				if (applied.isConsumed()) {
					// If the BannerView consumed all insets, return now
					return applied;
				}
				final Rect res = this.mTempRect;
				res.left = applied.getSystemWindowInsetLeft();
				res.top = applied.getSystemWindowInsetTop();
				res.right = applied.getSystemWindowInsetRight();
				res.bottom = applied.getSystemWindowInsetBottom();

				for (int i = 0, count = getChildCount(); i < count; i++) {
					final WindowInsetsCompat childInsets = ViewCompat
							.dispatchApplyWindowInsets(getChildAt(i), applied);
					// Now keep track of any consumed by tracking each dimension's min
					// value
					res.left = Math.min(childInsets.getSystemWindowInsetLeft(),
							res.left);
					res.top = Math.min(childInsets.getSystemWindowInsetTop(),
							res.top);
					res.right = Math.min(childInsets.getSystemWindowInsetRight(),
							res.right);
					res.bottom = Math.min(childInsets.getSystemWindowInsetBottom(),
							res.bottom);
				}
				// Now return a new WindowInsets, using the consumed window insets
				return applied.replaceSystemWindowInsets(
						res.left, res.top, res.right, res.bottom);
			}
		});
	}

	private boolean mInLayout;
	private boolean mFirstLayout = true;
	private boolean mCalledSuper;
	private boolean mPopulatePending;
	private boolean mScrollingCacheEnabled;

	private static final int DRAW_ORDER_DEFAULT = 0;
	private static final int DRAW_ORDER_FORWARD = 1;
	private static final int DRAW_ORDER_REVERSE = 2;
	private PageTransformer mPageTransformer;
	private int mPageTransformerLayerType;
	private int mDrawingOrder;

	private int mPageMargin;
	private int mDecorChildCount;
	private Drawable mMarginDrawable;
	private Rect mPageBoundsRect = new Rect();

	private int mOrientation = HORIZONTAL;
	private boolean mIsScrollingLoop = false;
	private boolean mIsAllowUserScrollable = true;

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		this.mFirstLayout = true;
	}

	@Override
	protected void onDetachedFromWindow() {
		this.removeCallbacks(this.mCompleteScrollRunnable);
		// To be on the safe side, abort the scroller
		if ((this.mScroller != null) && !this.mScroller.isFinished()) {
			this.mScroller.abortAnimation();
		}
		super.onDetachedFromWindow();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		this.setMeasuredDimension(View.getDefaultSize(0, widthMeasureSpec),
				View.getDefaultSize(0, heightMeasureSpec));

		final int measuredWidth = this.getMeasuredWidth();
		final int measuredHeight = this.getMeasuredHeight();
		this.mGutterSize = Math.min(measuredWidth / 10, this.mGutterSize);

		int preChildWidthSize = measuredWidth - this.getPaddingLeft() - this.getPaddingRight();
		int preChildHeightSize = measuredHeight - this.getPaddingTop() - this.getPaddingBottom();

		// decorView
		for (int index = 0; index < this.getChildCount(); index++) {
			final View preChildView = this.getChildAt(index);
			if (preChildView.getVisibility() != View.GONE) {
				final LayoutParams preLayoutParams = (LayoutParams) preChildView.getLayoutParams();
				if (preLayoutParams != null && preLayoutParams.isDecor) {
					int preWidthMode = MeasureSpec.AT_MOST;
					int preHeightMode = MeasureSpec.AT_MOST;

					if (Gravity.isVertical(preLayoutParams.gravity)) {
						preWidthMode = MeasureSpec.EXACTLY;
					} else if (Gravity.isHorizontal(preLayoutParams.gravity)) {
						preHeightMode = MeasureSpec.EXACTLY;
					}

					int widthSize = preChildWidthSize;
					int heightSize = preChildHeightSize;
					if (preLayoutParams.width != LayoutParams.WRAP_CONTENT) {
						preWidthMode = MeasureSpec.EXACTLY;
						if (preLayoutParams.width != LayoutParams.MATCH_PARENT) {
							widthSize = preLayoutParams.width;
						}
					}
					if (preLayoutParams.height != LayoutParams.WRAP_CONTENT) {
						preHeightMode = MeasureSpec.EXACTLY;
						if (preLayoutParams.height != LayoutParams.MATCH_PARENT) {
							heightSize = preLayoutParams.height;
						}
					}
					final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, preWidthMode);
					final int heightSpec = MeasureSpec.makeMeasureSpec(heightSize, preHeightMode);
					preChildView.measure(widthSpec, heightSpec);

					if (Gravity.isVertical(preLayoutParams.gravity)) {
						preChildHeightSize -= preChildView.getMeasuredHeight();
					} else if (Gravity.isHorizontal(preLayoutParams.gravity)) {
						preChildWidthSize -= preChildView.getMeasuredWidth();
					}
				}
			}
		}

		final int mChildWidthMeasureSpec = MeasureSpec.makeMeasureSpec(preChildWidthSize, MeasureSpec.EXACTLY);
		final int mChildHeightMeasureSpec = MeasureSpec.makeMeasureSpec(preChildHeightSize, MeasureSpec.EXACTLY);

		this.mInLayout = true;
		this.populate();
		this.mInLayout = false;

		// Page views next.
		for (int index = 0; index < this.getChildCount(); ++index) {
			final View preChildView = this.getChildAt(index);
			if (preChildView.getVisibility() != View.GONE) {
				final LayoutParams preLayoutParams = (LayoutParams) preChildView.getLayoutParams();
				if (preLayoutParams != null && !preLayoutParams.isDecor) {
					if (VERTICAL == this.mOrientation) {
						preChildView.measure(mChildWidthMeasureSpec, MeasureSpec.makeMeasureSpec((int) (preChildHeightSize * preLayoutParams.weight), MeasureSpec.EXACTLY));
					} else {
						preChildView.measure(MeasureSpec.makeMeasureSpec((int) (preChildWidthSize * preLayoutParams.weight), MeasureSpec.EXACTLY), mChildHeightMeasureSpec);
					}
				}
			}
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		int prePaddingLeft = this.getPaddingLeft();
		int prePaddingTop = this.getPaddingTop();
		int prePaddingRight = this.getPaddingRight();
		int prePaddingBottom = this.getPaddingBottom();

		final int preChildWidthSize = right - left;
		final int preChildHeightSize = bottom - top;

		final int preWidth = preChildWidthSize - prePaddingLeft - prePaddingRight;
		final int preHeight = preChildHeightSize - prePaddingTop - prePaddingBottom;
		final int mChildCount = this.getChildCount();
		final int mScrollX = this.getScrollX();
		final int mScrollY = this.getScrollY();

		// decorView
		int decorCount = 0;
		for (int index = 0; index < mChildCount; index++) {
			final View preChildView = this.getChildAt(index);
			if (preChildView.getVisibility() != View.GONE) {
				final LayoutParams preLayoutParams = (LayoutParams) preChildView.getLayoutParams();
				if (preLayoutParams != null && preLayoutParams.isDecor) {
					final int horizontalGravity = preLayoutParams.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
					final int verticalGravity = preLayoutParams.gravity & Gravity.VERTICAL_GRAVITY_MASK;
					int preChildLeft;
					int preChildTop;
					switch (horizontalGravity) {
						default:
							preChildLeft = prePaddingLeft;
							break;
						case Gravity.LEFT:
							preChildLeft = prePaddingLeft;
							prePaddingLeft += preChildView.getMeasuredWidth();
							break;
						case Gravity.CENTER_HORIZONTAL:
							preChildLeft = Math.max((preChildWidthSize - preChildView.getMeasuredWidth()) / 2, prePaddingLeft);
							break;
						case Gravity.RIGHT:
							preChildLeft = preChildWidthSize - prePaddingRight - preChildView.getMeasuredWidth();
							prePaddingRight += preChildView.getMeasuredWidth();
							break;
					}
					switch (verticalGravity) {
						default:
							preChildTop = prePaddingTop;
							break;
						case Gravity.TOP:
							preChildTop = prePaddingTop;
							prePaddingTop += preChildView.getMeasuredHeight();
							break;
						case Gravity.CENTER_VERTICAL:
							preChildTop = Math.max((preChildHeightSize - preChildView.getMeasuredHeight()) / 2, prePaddingTop);
							break;
						case Gravity.BOTTOM:
							preChildTop = preChildHeightSize - prePaddingBottom - preChildView.getMeasuredHeight();
							prePaddingBottom += preChildView.getMeasuredHeight();
							break;
					}
					preChildTop += mScrollY;
					preChildLeft += mScrollX;
					preChildView.layout(preChildLeft, preChildTop,
							preChildLeft + preChildView.getMeasuredWidth(),
							preChildTop + preChildView.getMeasuredHeight());
					decorCount++;
				}
			}
		}

		// Page views. Do this once we have the right padding offsets from above.
		for (int index = 0; index < mChildCount; index++) {
			final View preChildView = this.getChildAt(index);
			if (preChildView.getVisibility() != View.GONE) {
				final LayoutParams preLayoutParams = (LayoutParams) preChildView.getLayoutParams();
				final Page page = this.getPagerForChild(preChildView);
				if (!preLayoutParams.isDecor && page != null) {
					int preChildLeft = prePaddingLeft;
					int preChildTop = prePaddingTop;

					if (VERTICAL == this.mOrientation) {
						preChildTop += (int) (preHeight * page.offset);
					} else {
						preChildLeft += (int) (preWidth * page.offset);
					}
					if (preLayoutParams.needsMeasure) {
						preLayoutParams.needsMeasure = false;
						final int widthSpec;
						final int heightSpec;

						if (VERTICAL == this.mOrientation) {
							widthSpec = MeasureSpec.makeMeasureSpec(preWidth, MeasureSpec.EXACTLY);
							heightSpec = MeasureSpec.makeMeasureSpec((int) (preHeight * preLayoutParams.weight), MeasureSpec.EXACTLY);
						} else {
							widthSpec = MeasureSpec.makeMeasureSpec((int) (preWidth * preLayoutParams.weight), MeasureSpec.EXACTLY);
							heightSpec = MeasureSpec.makeMeasureSpec(preHeight, MeasureSpec.EXACTLY);
						}
						preChildView.measure(widthSpec, heightSpec);
					}
					preChildView.layout(preChildLeft, preChildTop,
							preChildLeft + preChildView.getMeasuredWidth(),
							preChildTop + preChildView.getMeasuredHeight());
				}
			}
		}
		this.mPageBoundsRect.left = prePaddingLeft;
		this.mPageBoundsRect.top = prePaddingTop;
		this.mPageBoundsRect.right = preChildWidthSize - prePaddingRight;
		this.mPageBoundsRect.bottom = preChildHeightSize - prePaddingBottom;
		this.mDecorChildCount = decorCount;

		if (this.mFirstLayout) {
			this.scrollToItem(this.mCurrentPosition, false, 0, false);
		} else {
			this.pageScrolled(mScrollX, mScrollY);
		}
		this.mFirstLayout = false;
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		// Make sure scroll position is set correctly.
		if (w != oldw || h != oldh) {
			this.recomputeScrollPosition(w, h, oldw, oldh, this.mPageMargin, this.mPageMargin);
		}
	}

	@Override
	public void draw(Canvas canvas) {
		super.draw(canvas);
		final int overScrollMode = this.getOverScrollMode();
		if (overScrollMode == View.OVER_SCROLL_ALWAYS
				|| (overScrollMode == View.OVER_SCROLL_IF_CONTENT_SCROLLS
				&& this.mAdapter != null && this.mAdapter.getItemCount() > 1)) {

			final boolean clipToPadding = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && this.getClipToPadding();
			final int width = this.getWidth() + (clipToPadding ? -this.getPaddingLeft() - this.getPaddingRight() : 0);
			final int height = this.getHeight() + (clipToPadding ? -this.getPaddingTop() - this.getPaddingBottom() : 0);

			if (!this.mStartEdgeEffect.isFinished()) {
				final int restoreCount = canvas.save();
				if (VERTICAL == this.mOrientation) {
					final int translateX = (clipToPadding ? this.getPaddingLeft() : 0);
					final int translateY = this.getScrollY() + (clipToPadding ? this.getPaddingTop() : 0);
					canvas.rotate(0);
					canvas.translate(translateX, translateY);
					this.mStartEdgeEffect.setSize(width, height);
				} else {
					final int translateX = -height + (clipToPadding ? -this.getPaddingTop() : 0);
					final int translateY = this.getScrollX() + (clipToPadding ? this.getPaddingLeft() : 0);
					canvas.rotate(270);
					canvas.translate(translateX, translateY);
					this.mStartEdgeEffect.setSize(height, width);
				}
				if (this.mStartEdgeEffect.draw(canvas)) {
					// Keep animating
					ViewCompat.postInvalidateOnAnimation(this);
				}
				canvas.restoreToCount(restoreCount);
			}
			if (!this.mEndEdgeEffect.isFinished()) {
				final int restoreCount = canvas.save();
				if (VERTICAL == this.mOrientation) {
					final int translateX = -width + (clipToPadding ? this.getPaddingLeft() : 0);
					final int translateY = (this.getScrollY() + height) + (clipToPadding ? this.getPaddingTop() : -this.getPaddingTop());
					canvas.translate(translateX, translateY);
					canvas.rotate(180, width, 0);
					this.mEndEdgeEffect.setSize(width, height);
				} else {
					final int translateX = (clipToPadding ? this.getPaddingTop() : 0);
					final int translateY = -(this.getScrollX() + width) + (clipToPadding ? -this.getPaddingLeft() : this.getPaddingLeft());
					canvas.rotate(90);
					canvas.translate(translateX, translateY);
					this.mEndEdgeEffect.setSize(height, width);
				}
				if (this.mEndEdgeEffect.draw(canvas)) {
					// Keep animating
					ViewCompat.postInvalidateOnAnimation(this);
				}
				canvas.restoreToCount(restoreCount);
			}
		} else {
			this.mStartEdgeEffect.finish();
			this.mEndEdgeEffect.finish();
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Draw the margin drawable between pages if needed.
		if (this.mPageMargin > 0 && this.mMarginDrawable != null && this.mPagePools.size() > 0 && this.mAdapter != null) {
			final boolean clipToPadding = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && this.getClipToPadding();
			final int clientSize;
			final int scrollOffset;
			if (VERTICAL == this.mOrientation) {
				scrollOffset = this.getScrollY();
				clientSize = this.getClientHeight();
			} else {
				scrollOffset = this.getScrollX();
				clientSize = this.getClientWidth();
			}
			final float marginOffset = (float) this.mPageMargin / clientSize;
			final int itemCount = this.mPagePools.size();

			int nextIndex = 0;
			Page page = this.mPagePools.get(0);
			final int firstPosition = page.position;
			final int lastPosition = this.mPagePools.get(itemCount - 1).position;

			float offset = page.offset;
			for (int position = firstPosition; position < lastPosition; position++) {
				while (position > page.position && nextIndex < itemCount) {
					page = this.mPagePools.get(++nextIndex);
				}

				final float nextPos;
				if (position == page.position) {
					nextPos = (page.offset + page.weight) * clientSize;
					offset = page.offset + page.weight + marginOffset;
				} else {
					float weight = this.mAdapter.getPageWeight(this.adapterPositionForPosition(position));
					nextPos = (offset + weight) * clientSize;
					offset += weight + marginOffset;
				}

				final int roundPos = Math.round(nextPos);
				if ((roundPos + this.mPageMargin > scrollOffset
						&& roundPos < (scrollOffset + clientSize)) || !clipToPadding) {
					int left = this.mPageBoundsRect.left;
					int top = this.mPageBoundsRect.top;
					int right = this.mPageBoundsRect.right;
					int bottom = this.mPageBoundsRect.bottom;

					if (VERTICAL == this.mOrientation) {
						top += (int) (roundPos - 0.5F);
						bottom = (int) (top + this.mPageMargin + 0.5F);
					} else {
						left += (int) (roundPos - 0.5F);
						right = (int) (left + this.mPageMargin + 0.5F);
					}
					this.mMarginDrawable.setBounds(left, top, right, bottom);
					this.mMarginDrawable.draw(canvas);
				}
				if (roundPos > scrollOffset + clientSize) {
					break; // No more visible, no sense in continuing
				}
			}
		}
	}

	@Override
	protected boolean verifyDrawable(@NonNull Drawable drawable) {
		return super.verifyDrawable(drawable) || drawable == this.mMarginDrawable;
	}

	@Override
	protected void drawableStateChanged() {
		super.drawableStateChanged();
		final Drawable mDrawable = this.mMarginDrawable;
		if (mDrawable != null && mDrawable.isStateful()) {
			mDrawable.setState(this.getDrawableState());
		}
	}

	@Override
	protected LayoutParams generateDefaultLayoutParams() {
		return new LayoutParams();
	}

	@Override
	protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams layoutParams) {
		return new LayoutParams(layoutParams);
	}

	@Override
	protected boolean checkLayoutParams(ViewGroup.LayoutParams layoutParams) {
		return layoutParams instanceof LayoutParams && super.checkLayoutParams(layoutParams);
	}

	@Override
	public LayoutParams generateLayoutParams(AttributeSet attrs) {
		return new LayoutParams(getContext(), attrs);
	}

	@Override
	protected int getChildDrawingOrder(int childCount, int index) {
		final int newIndex = this.mDrawingOrder == DRAW_ORDER_REVERSE ? childCount - 1 - index : index;
		return ((LayoutParams) this.mDrawingOrderedChilds.get(newIndex).getLayoutParams()).childIndex;
	}

	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams layoutParams) {
		final LayoutParams preLayoutParams = this.generateLayoutParams(layoutParams);
		// Any views added via inflation should be classed as part of the decor
		preLayoutParams.isDecor = isDecorView(child);
		if (this.mInLayout) {
			if (preLayoutParams.isDecor) {
				throw new IllegalStateException("Cannot add pager decor view during layout");
			}
			preLayoutParams.needsMeasure = true;
			this.addViewInLayout(child, index, preLayoutParams);
		} else {
			super.addView(child, index, preLayoutParams);
		}
		if (USE_CACHE) {
			if (child.getVisibility() != GONE) {
				child.setDrawingCacheEnabled(this.mScrollingCacheEnabled);
			} else {
				child.setDrawingCacheEnabled(false);
			}
		}
	}

	@Override
	public void removeView(View view) {
		if (this.mInLayout) {
			this.removeViewInLayout(view);
		} else {
			super.removeView(view);
		}
	}

	private Bundle mRestoredAdapterState = null;
	private ClassLoader mRestoredClassLoader = null;

	@Override
	public Parcelable onSaveInstanceState() {
		final Parcelable mParcelable = super.onSaveInstanceState();
		final SavedState mSavedState = new SavedState(mParcelable);
		final Bundle mSaveInstanceState = new Bundle();
		if (mAdapter != null) {
			mAdapter.onSaveInstanceState(mSaveInstanceState);
		}
		mSavedState.position = this.mCurrentPosition;
		mSavedState.orientation = this.mOrientation;
		mSavedState.shouldScrollingLoop = this.mIsScrollingLoop;
		mSavedState.shouldScrollingInAllowUser = this.mIsAllowUserScrollable;
		mSavedState.adapterState = mSaveInstanceState;
		return mSavedState;
	}

	@Override
	public void onRestoreInstanceState(Parcelable state) {
		if (state instanceof SavedState) {
			final SavedState mSavedState = (SavedState) state;
			super.onRestoreInstanceState(mSavedState.getSuperState());
			if (this.mAdapter != null) {
				this.mAdapter.onRestoreInstanceState(mSavedState.adapterState, mSavedState.loader);
				this.setCurrentItemInternal(mSavedState.position, false, true);
			} else {
				this.mRestoredAdapterState = mSavedState.adapterState;
				this.mRestoredClassLoader = mSavedState.loader;
				this.setCurrentItem(mSavedState.position);
				this.setOrientation(mSavedState.orientation);
				this.setScrollingLoop(mSavedState.shouldScrollingLoop);
				this.setAllowUserScrollable(mSavedState.shouldScrollingInAllowUser);
			}
		} else {
			super.onRestoreInstanceState(state);
		}
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (this.performOnInterceptTouchEvent(event)) {
			return true;
		}
		return super.onInterceptTouchEvent(event);
	}

	@Override
	@SuppressLint("ClickableViewAccessibility")
	public boolean onTouchEvent(MotionEvent event) {
		if (this.performOnTouchEvent(event)) {
			return true;
		}
		return super.onTouchEvent(event);
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		return super.dispatchKeyEvent(event) || this.executeKeyEvent(event);
	}

	@Override
	public void addFocusables(ArrayList<View> views, int direction, int focusableMode) {
		final int focusableCount = views.size();
		final int descendantFocusability = this.getDescendantFocusability();
		if (descendantFocusability != FOCUS_BLOCK_DESCENDANTS) {
			for (int index = 0; index < this.getChildCount(); index++) {
				final View preChildView = this.getChildAt(index);
				if (preChildView.getVisibility() == VISIBLE) {
					final Page page = this.getPagerForChild(preChildView);
					if (page != null && page.position == this.mCurrentPosition) {
						preChildView.addFocusables(views, direction, focusableMode);
					}
				}
			}
		}
		if (descendantFocusability != FOCUS_AFTER_DESCENDANTS || (focusableCount == views.size())) {
			if (!this.isFocusable()) {
				return;
			}
			if ((focusableMode & FOCUSABLES_TOUCH_MODE) == FOCUSABLES_TOUCH_MODE && this.isInTouchMode() && !this.isFocusableInTouchMode()) {
				return;
			}
			views.add(this);
		}
	}

	@Override
	public void addTouchables(ArrayList<View> views) {
		for (int index = 0; index < this.getChildCount(); index++) {
			final View preChildView = this.getChildAt(index);
			if (preChildView.getVisibility() == VISIBLE) {
				final Page page = this.getPagerForChild(preChildView);
				if (page != null && page.position == this.mCurrentPosition) {
					preChildView.addTouchables(views);
				}
			}
		}
	}

	@Override
	protected boolean onRequestFocusInDescendants(int direction, Rect previouslyFocusedRect) {
		int index;
		int increment;
		int end;
		int count = this.getChildCount();
		if ((direction & FOCUS_FORWARD) != 0) {
			index = 0;
			increment = 1;
			end = count;
		} else {
			index = count - 1;
			increment = -1;
			end = -1;
		}
		for (int i = index; i != end; i += increment) {
			final View preChildView = this.getChildAt(i);
			if (preChildView.getVisibility() == VISIBLE) {
				final Page page = this.getPagerForChild(preChildView);
				if (page != null && page.position == this.mCurrentPosition) {
					if (preChildView.requestFocus(direction, previouslyFocusedRect)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean dispatchPopulateAccessibilityEvent(AccessibilityEvent event) {
		// Dispatch scroll events from this ViewPager.
		if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
			return super.dispatchPopulateAccessibilityEvent(event);
		}
		// Dispatch all other accessibility events from the current page.
		for (int index = 0; index < this.getChildCount(); index++) {
			final View preChildView = this.getChildAt(index);
			if (preChildView.getVisibility() == VISIBLE) {
				final Page page = this.getPagerForChild(preChildView);
				if (page != null && page.position == this.mCurrentPosition
						&& preChildView.dispatchPopulateAccessibilityEvent(event)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public boolean canScrollVertically(int direction) {
		if (VERTICAL == this.mOrientation) {
			if (this.mIsScrollingLoop) {
				return true;
			}
			final int offset = this.getScrollY();
			final int range = (int) (this.mLastOffset * this.getClientHeight()) - this.getClientHeight();
			if (direction < 0) {
				return offset > 0;
			} else {
				return offset < range - 1;
			}
		}
		return super.canScrollVertically(direction);
	}

	@Override
	public boolean canScrollHorizontally(int direction) {
		if (HORIZONTAL == this.mOrientation) {
			if (this.mIsScrollingLoop) {
				return true;
			}
			final int offset = this.getScrollX();
			final int range = (int) (this.mLastOffset * this.getClientWidth()) - this.getClientWidth();
			if (direction < 0) {
				return offset > 0;
			} else {
				return offset < range - 1;
			}
		}
		return super.canScrollHorizontally(direction);
	}

	@Override
	public void computeScroll() {
		this.mIsScrollStarted = true;
		if (!this.mScroller.isFinished() && this.mScroller.computeScrollOffset()) {
			final int oldScrollX = this.getScrollX();
			final int oldScrollY = this.getScrollY();
			final int x = this.mScroller.getCurrX();
			final int y = this.mScroller.getCurrY();

			int unconsumedX = (x - oldScrollX);
			int unconsumedY = (y - oldScrollY);

			// Nested Scrolling Pre Pass
//			this.mScrollConsumed[0] = 0;
//			this.mScrollConsumed[1] = 0;
//			if (this.dispatchNestedPreScroll(unconsumedX, unconsumedY, this.mScrollConsumed, null)) {
//				unconsumedX -= this.mScrollConsumed[0];
//				unconsumedY -= this.mScrollConsumed[1];
//			}

			if (unconsumedX != 0 || unconsumedY != 0) {
				final int curScrollX = oldScrollX + unconsumedX;
				final int curScrollY = oldScrollY + unconsumedY;
				this.scrollTo(curScrollX, curScrollY);

				// Nested Scrolling Pre Pass
//				this.mScrollConsumed[0] = 0;
//				this.mScrollConsumed[1] = 0;
//				final int scrolledDeltaX = (this.getScrollX() - oldScrollX);
//				final int scrolledDeltaY = (this.getScrollY() - oldScrollY);
//				unconsumedX -= scrolledDeltaX;
//				unconsumedY -= scrolledDeltaY;
//				this.dispatchNestedScroll(scrolledDeltaX, scrolledDeltaY, unconsumedX, unconsumedY, this.mScrollConsumed);
//				unconsumedX -= this.mScrollConsumed[0];
//				unconsumedY -= this.mScrollConsumed[1];
//
//				if (unconsumedX != 0 || unconsumedY != 0) {
//					if (unconsumedX < 0 || unconsumedY < 0) {
//						if (this.mStartEdgeEffect.isFinished()) {
//							this.mStartEdgeEffect.onAbsorb((int) this.mScroller.getCurrVelocity());
//						}
//					} else {
//						if (this.mEndEdgeEffect.isFinished()) {
//							this.mEndEdgeEffect.onAbsorb((int) this.mScroller.getCurrVelocity());
//						}
//					}
//				}

				if (VERTICAL == this.mOrientation) {
					if (!this.pageScrolled(0, curScrollY)) {
						this.mScroller.abortAnimation();
						this.scrollTo(curScrollX, 0);
					}
				} else {
					if (!this.pageScrolled(curScrollX, 0)) {
						this.mScroller.abortAnimation();
						this.scrollTo(0, curScrollY);
					}
				}
			}
			// Keep on drawing until the animation has finished.
			ViewCompat.postInvalidateOnAnimation(this);
			return;
		}
		// Done with scroll, clean up state.
		this.completeScroll(true);
	}

	private static boolean isDecorView(@NonNull View view) {
		Class<?> clazz = view.getClass();
		return clazz.getAnnotation(DecorView.class) != null;
	}

	private boolean executeKeyEvent(@NonNull KeyEvent event) {
		boolean handled = false;
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_DPAD_LEFT:
					if (event.hasModifiers(KeyEvent.META_ALT_ON)) {
						handled = this.pageStart();
					} else {
						handled = this.arrowScroll(FOCUS_LEFT);
					}
					break;
				case KeyEvent.KEYCODE_DPAD_RIGHT:
					if (event.hasModifiers(KeyEvent.META_ALT_ON)) {
						handled = this.pageEnd();
					} else {
						handled = this.arrowScroll(FOCUS_RIGHT);
					}
					break;
				case KeyEvent.KEYCODE_TAB:
					if (event.hasNoModifiers()) {
						handled = this.arrowScroll(FOCUS_FORWARD);
					} else if (event.hasModifiers(KeyEvent.META_SHIFT_ON)) {
						handled = this.arrowScroll(FOCUS_BACKWARD);
					}
					break;
			}
		}
		return handled;
	}

	private final Rect mTempRect = new Rect();

	/**
	 * Handle scrolling in response to a left or right arrow click.
	 *
	 * @param direction The direction corresponding to the arrow key that was pressed. It should be
	 *                  either {@link View#FOCUS_LEFT} or {@link View#FOCUS_RIGHT}.
	 * @return Whether the scrolling was handled successfully.
	 */
	public boolean arrowScroll(int direction) {
		View currentFocused = this.findFocus();
		if (currentFocused == this) {
			currentFocused = null;
		} else if (currentFocused != null) {
			boolean isChild = false;
			for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup;
				 parent = parent.getParent()) {
				if (parent == this) {
					isChild = true;
					break;
				}
			}
			if (!isChild) {
				// This would cause the focus search down below to fail in fun ways.
				final StringBuilder sb = new StringBuilder();
				sb.append(currentFocused.getClass().getSimpleName());
				for (ViewParent parent = currentFocused.getParent(); parent instanceof ViewGroup;
					 parent = parent.getParent()) {
					sb.append(" => ").append(parent.getClass().getSimpleName());
				}
				Log.e(TAG, "arrowScroll tried to find focus based on non-child "
						+ "current focused view " + sb.toString());
				currentFocused = null;
			}
		}

		boolean handled = false;
		View nextFocused = FocusFinder.getInstance().findNextFocus(this, currentFocused, direction);
		if (nextFocused != null && nextFocused != currentFocused) {
			if (direction == View.FOCUS_LEFT) {
				// If there is nothing to the left, or this is causing us to
				// jump to the right, then what we really want to do is page left.
				final int nextStart;
				final int currStart;
				if (VERTICAL == this.mOrientation) {
					nextStart = this.getChildRectInPagerCoordinates(mTempRect, nextFocused).top;
					currStart = this.getChildRectInPagerCoordinates(mTempRect, currentFocused).top;
				} else {
					nextStart = this.getChildRectInPagerCoordinates(mTempRect, nextFocused).left;
					currStart = this.getChildRectInPagerCoordinates(mTempRect, currentFocused).left;
				}
				if (currentFocused != null && nextStart >= currStart) {
					handled = this.pageStart();
				} else {
					handled = nextFocused.requestFocus();
				}
			} else if (direction == View.FOCUS_RIGHT) {
				// If there is nothing to the right, or this is causing us to
				// jump to the left, then what we really want to do is page right.
				final int nextEnd;
				final int currEnd;
				if (VERTICAL == this.mOrientation) {
					nextEnd = this.getChildRectInPagerCoordinates(mTempRect, nextFocused).top;
					currEnd = this.getChildRectInPagerCoordinates(mTempRect, currentFocused).top;
				} else {
					nextEnd = this.getChildRectInPagerCoordinates(mTempRect, nextFocused).left;
					currEnd = this.getChildRectInPagerCoordinates(mTempRect, currentFocused).left;
				}
				if (currentFocused != null && nextEnd <= currEnd) {
					handled = this.pageEnd();
				} else {
					handled = nextFocused.requestFocus();
				}
			}
		} else if (direction == FOCUS_LEFT || direction == FOCUS_BACKWARD) {
			// Trying to move left and nothing there; try to page.
			handled = this.pageStart();
		} else if (direction == FOCUS_RIGHT || direction == FOCUS_FORWARD) {
			// Trying to move right and nothing there; try to page.
			handled = this.pageEnd();
		}
		if (handled) {
			this.playSoundEffect(SoundEffectConstants.getContantForFocusDirection(direction));
		}
		return handled;
	}

	private Rect getChildRectInPagerCoordinates(Rect outRect, View child) {
		if (outRect == null) {
			outRect = new Rect();
		}
		if (child == null) {
			outRect.set(0, 0, 0, 0);
			return outRect;
		}
		outRect.left = child.getLeft();
		outRect.top = child.getTop();
		outRect.right = child.getRight();
		outRect.bottom = child.getBottom();

		ViewParent parent = child.getParent();
		while (parent instanceof ViewGroup && parent != this) {
			final ViewGroup group = (ViewGroup) parent;
			outRect.left += group.getLeft();
			outRect.top += group.getTop();
			outRect.right += group.getRight();
			outRect.bottom += group.getBottom();
			parent = group.getParent();
		}
		return outRect;
	}

	public boolean pageStart() {
		if (this.mAdapter == null || this.mAdapter.getItemCount() <= 0) {
			return false;
		}
		if (this.mIsScrollingLoop) {
			this.setCurrentItemInternal(this.mCurrentPosition - 1, true, false);
			return true;
		} else {
			if (this.mCurrentPosition > 0) {
				this.setCurrentItem(this.mCurrentPosition - 1, true);
				return true;
			}
		}
		return false;
	}

	public boolean pageEnd() {
		if (this.mAdapter == null || this.mAdapter.getItemCount() <= 0) {
			return false;
		}
		if (this.mIsScrollingLoop) {
			this.setCurrentItemInternal(this.mCurrentPosition + 1, true, false);
			return true;
		} else {
			if (this.mCurrentPosition < (this.mAdapter.getItemCount() - 1)) {
				this.setCurrentItem(this.mCurrentPosition + 1, true);
				return true;
			}
		}
		return false;
	}

	private VelocityTracker mVelocityTracker;
	private boolean mIsScrollStarted;
	private boolean mIsBeingDragged;
	private boolean mIsUnableToDrag;
	private int mActivePointerId = INVALID_POINTER;
	private int mScrollState = SCROLL_STATE_IDLE;

	private float mInitialMotionX;
	private float mInitialMotionY;
	private float mTouchMotionX;
	private float mTouchMotionY;

	boolean performOnInterceptTouchEvent(@NonNull MotionEvent event) {
		if (!this.mIsAllowUserScrollable) {
			return false;
		}
		final int mActionMasked = event.getActionMasked();
		// Always take care of the touch gesture being complete.
		if (mActionMasked == MotionEvent.ACTION_CANCEL || mActionMasked == MotionEvent.ACTION_UP) {
			// Release the drag.
			this.resetTouch();
			return false;
		}
		if (mActionMasked != MotionEvent.ACTION_DOWN) {
			if (this.mIsBeingDragged) {
				return true;
			}
			if (this.mIsUnableToDrag) {
				return false;
			}
		}

		switch (mActionMasked) {
			case MotionEvent.ACTION_DOWN:
				this.mTouchMotionX = this.mInitialMotionX = event.getX();
				this.mTouchMotionY = this.mInitialMotionY = event.getY();
				this.mActivePointerId = event.getPointerId(0);
				this.mIsUnableToDrag = false;
				this.mIsScrollStarted = true;
				this.mScroller.computeScrollOffset();

				if ((VERTICAL == this.mOrientation && this.mScrollState == SCROLL_STATE_SETTLING
						&& Math.abs(this.mScroller.getFinalY() - this.mScroller.getCurrY()) > this.mCloseEnough)
						|| (HORIZONTAL == this.mOrientation && this.mScrollState == SCROLL_STATE_SETTLING
						&& Math.abs(this.mScroller.getFinalX() - this.mScroller.getCurrX()) > this.mCloseEnough)) {
					this.mScroller.abortAnimation();
					this.mPopulatePending = false;
					this.populate();
					// dragging
					this.requestParentDisallowInterceptTouchEvent(true);
					this.mIsBeingDragged = true;
					this.setScrollState(SCROLL_STATE_DRAGGING);
				} else {
					this.completeScroll(false);
					this.mIsBeingDragged = false;
				}
				if (VERTICAL == this.mOrientation) {
					this.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
				} else {
					this.startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				final int mPointerIndex = event.findPointerIndex(this.mActivePointerId);
				if (mPointerIndex == -1) {
					break;
				}

				final float x = event.getX(mPointerIndex);
				final float y = event.getY(mPointerIndex);
				final float dx = x - this.mTouchMotionX;
				final float dy = y - this.mTouchMotionY;
				final float xDiff = Math.abs(dx);
				final float yDiff = Math.abs(dy);

				if (((VERTICAL == this.mOrientation && dy != 0)
						|| (HORIZONTAL == this.mOrientation && dx != 0))
						&& !this.isGutterDrag(this.mTouchMotionX, this.mTouchMotionY, dx, dy)
						&& this.canScroll(this, false, (int) dx, (int) dy, (int) x, (int) y)) {
					// Nested view has scrollable area under this point. Let it be handled there.
					this.mTouchMotionX = x;
					this.mTouchMotionY = y;
					this.mIsUnableToDrag = true;
					return false;
				}

				if ((VERTICAL == this.mOrientation && yDiff > this.mTouchSlop && yDiff * 0.5f > xDiff)
						|| (HORIZONTAL == this.mOrientation && xDiff > this.mTouchSlop && xDiff * 0.5f > yDiff)) {
					this.requestParentDisallowInterceptTouchEvent(true);
					this.mIsBeingDragged = true;
					this.mTouchMotionX = dx > 0 ? this.mTouchMotionX + this.mTouchSlop : this.mTouchMotionX - this.mTouchSlop;
					this.mTouchMotionY = dy > 0 ? this.mTouchMotionY + this.mTouchSlop : this.mTouchMotionY - this.mTouchSlop;
					this.setScrollState(SCROLL_STATE_DRAGGING);
					this.setScrollingCacheEnabled(true);
				} else if ((VERTICAL == this.mOrientation && xDiff > this.mTouchSlop)
						|| (HORIZONTAL == this.mOrientation && yDiff > this.mTouchSlop)) {
					this.mIsUnableToDrag = true;
				}

				if (this.mIsBeingDragged) {
					final float deltaX = this.mTouchMotionX - x;
					final float deltaY = this.mTouchMotionY - y;
					this.mTouchMotionX = x;
					this.mTouchMotionY = y;

					// Scroll to follow the motion event
					if (this.performDrag(x, y, deltaX, deltaY)) {
						ViewCompat.postInvalidateOnAnimation(this);
					}
				}
				break;
			case MotionEvent.ACTION_POINTER_UP:
				this.onSecondaryPointerUp(event);
				break;
		}
		if (this.mVelocityTracker == null) {
			this.mVelocityTracker = VelocityTracker.obtain();
		}
		this.mVelocityTracker.addMovement(event);
		return this.mIsBeingDragged;
	}

	boolean performOnTouchEvent(@NonNull MotionEvent event) {
		if (!this.mIsAllowUserScrollable) {
			return false;
		}
		if (this.mAdapter == null || this.mAdapter.getItemCount() == 0) {
			// Nothing to present or scroll; nothing to touch.
			return false;
		}
		final int mActionMasked = event.getActionMasked();
		boolean mNeedsInvalidate = false;

		if (this.mVelocityTracker == null) {
			this.mVelocityTracker = VelocityTracker.obtain();
		}
		this.mVelocityTracker.addMovement(event);

		switch (mActionMasked) {
			case MotionEvent.ACTION_DOWN:
				this.mScroller.abortAnimation();
				this.mPopulatePending = false;
				this.populate();
				// Remember where the motion event started
				this.mTouchMotionX = this.mInitialMotionX = event.getX();
				this.mTouchMotionY = this.mInitialMotionY = event.getY();
				this.mActivePointerId = event.getPointerId(0);
				if (VERTICAL == this.mOrientation) {
					this.startNestedScroll(ViewCompat.SCROLL_AXIS_VERTICAL);
				} else {
					this.startNestedScroll(ViewCompat.SCROLL_AXIS_HORIZONTAL);
				}
				break;
			case MotionEvent.ACTION_MOVE:
				if (!this.mIsBeingDragged) {
					final int mPointerIndex = event.findPointerIndex(this.mActivePointerId);
					if (mPointerIndex == -1) {
						mNeedsInvalidate = this.resetTouch();
						break;
					}

					final float x = event.getX(mPointerIndex);
					final float y = event.getY(mPointerIndex);
					final float dx = x - this.mTouchMotionX;
					final float dy = y - this.mTouchMotionY;
					final float xDiff = Math.abs(dx);
					final float yDiff = Math.abs(dy);

					if ((VERTICAL == this.mOrientation && yDiff > this.mTouchSlop && yDiff > xDiff)
							|| (HORIZONTAL == this.mOrientation && xDiff > this.mTouchSlop && xDiff > yDiff)) {
						this.requestParentDisallowInterceptTouchEvent(true);
						this.mIsBeingDragged = true;
						this.mTouchMotionX = dx > 0 ? this.mTouchMotionX + this.mTouchSlop : this.mTouchMotionX - this.mTouchSlop;
						this.mTouchMotionY = dy > 0 ? this.mTouchMotionY + this.mTouchSlop : this.mTouchMotionY - this.mTouchSlop;
						this.setScrollState(SCROLL_STATE_DRAGGING);
						this.setScrollingCacheEnabled(true);
					}
				}
				// Not else! Note that mIsBeingDragged can be set above.
				if (this.mIsBeingDragged) {
					final int mPointerIndex = event.findPointerIndex(this.mActivePointerId);
					final float x = event.getX(mPointerIndex);
					final float y = event.getY(mPointerIndex);
					final float deltaX = this.mTouchMotionX - x;
					final float deltaY = this.mTouchMotionY - y;
					this.mTouchMotionX = x;
					this.mTouchMotionY = y;

					// Scroll to follow the motion event
					mNeedsInvalidate = this.performDrag(x, y, deltaX, deltaY);
				}
				break;
			case MotionEvent.ACTION_POINTER_DOWN:
				final int index = event.getActionIndex();
				this.mTouchMotionX = event.getX(index);
				this.mTouchMotionY = event.getY(index);
				this.mActivePointerId = event.getPointerId(index);
				break;
			case MotionEvent.ACTION_POINTER_UP:
				this.onSecondaryPointerUp(event);
				this.mTouchMotionX = event.getX(event.findPointerIndex(this.mActivePointerId));
				this.mTouchMotionY = event.getY(event.findPointerIndex(this.mActivePointerId));
				break;
			case MotionEvent.ACTION_CANCEL:
				if (this.mIsBeingDragged) {
					this.scrollToItem(this.mCurrentPosition, true, 0, false);
					mNeedsInvalidate = this.resetTouch();
				}
				break;
			case MotionEvent.ACTION_UP:
				if (this.mIsBeingDragged) {
					final VelocityTracker mVelocityTracker = this.mVelocityTracker;
					mVelocityTracker.computeCurrentVelocity(1000, this.mMaximumVelocity);
					this.mPopulatePending = true;
					final int velocityX = (int) (mVelocityTracker.getXVelocity(this.mActivePointerId));
					final int velocityY = (int) (mVelocityTracker.getYVelocity(this.mActivePointerId));
					final int mActivePointerIndex = event.findPointerIndex(this.mActivePointerId);
					final float dx = event.getX(mActivePointerIndex);
					final float dy = event.getY(mActivePointerIndex);
					final int deltaX = (int) (dx - this.mInitialMotionX);
					final int deltaY = (int) (dy - this.mInitialMotionY);
					if (VERTICAL == this.mOrientation) {
						final int mNextPagePosition = this.determineTargetPage(deltaY, velocityY);
						this.setCurrentItemInternal(mNextPagePosition, true, true, velocityY);
					} else {
						final int mNextPagePosition = this.determineTargetPage(deltaX, velocityX);
						this.setCurrentItemInternal(mNextPagePosition, true, true, velocityX);
					}
					mNeedsInvalidate = this.resetTouch();
				}
				break;
		}
		if (mNeedsInvalidate) {
			ViewCompat.postInvalidateOnAnimation(this);
		}
		return true;
	}

	private float mFirstOffset = -Float.MAX_VALUE;
	private float mLastOffset = Float.MAX_VALUE;

	private final int[] mScrollOffset = new int[2];
	private final int[] mScrollConsumed = new int[2];

	private boolean performDrag(float x, float y, float deltaX, float deltaY) {
		// Nested Scrolling Pre Pass
		this.mScrollConsumed[0] = 0;
		this.mScrollConsumed[1] = 0;
		if (this.dispatchNestedPreScroll((int) deltaX, (int) deltaY, this.mScrollConsumed, this.mScrollOffset)) {
			deltaX -= this.mScrollConsumed[0];
			deltaY -= this.mScrollConsumed[1];
		}

		final int width = this.getClientWidth();
		final int height = this.getClientHeight();
		final int oldScrollX = this.getScrollX();
		final int oldScrollY = this.getScrollY();
		final float nowScrollX = oldScrollX + deltaX;
		final float nowScrollY = oldScrollY + deltaY;
		final int clientSize;
		float nowScroll;

		if (VERTICAL == this.mOrientation) {
			clientSize = height;
			nowScroll = oldScrollY + deltaY;
		} else {
			clientSize = width;
			nowScroll = oldScrollX + deltaX;
		}

		float startBound = this.mFirstOffset * clientSize;
		float endBound = this.mLastOffset * clientSize;
		boolean startNeedsInvalidate = false;
		boolean endNeedsInvalidate = false;

		if (!this.mIsScrollingLoop) {
			// Only let the user target pages we have items for
			final Page firstPage = this.mPagePools.get(0);
			final Page lastPage = this.mPagePools.get(this.mPagePools.size() - 1);

			if (firstPage.position != 0) {
				startBound = firstPage.offset * clientSize;
			}
			if (lastPage.position != this.mAdapter.getItemCount() - 1) {
				endBound = lastPage.offset * clientSize;
			}
		}
		if (DEBUG) {
			Log.e(TAG, "performDrag : OldScroll(" + oldScrollX + "," + oldScrollY + ")");
			Log.e(TAG, "performDrag : NewScroll(" + nowScrollX + "," + nowScrollY + ") => " + nowScroll);
			Log.e(TAG, "performDrag : Bound(" + startBound + "," + endBound + ") => Offset(" + mFirstOffset + "," + mLastOffset + ")");
		}

		if (nowScroll < startBound) {
			startNeedsInvalidate = !this.mIsScrollingLoop;
			nowScroll = startBound;
		} else if (nowScroll > endBound) {
			endNeedsInvalidate = !this.mIsScrollingLoop;
			nowScroll = endBound;
		}
		// Don't lose the rounded component
		this.mTouchMotionX += nowScrollX - (int) nowScrollX;
		this.mTouchMotionY += nowScrollY - (int) nowScrollY;

		// Scrolling
		if (VERTICAL == this.mOrientation) {
			this.scrollTo(oldScrollX, (int) nowScroll);
			this.pageScrolled(oldScrollX, (int) nowScroll);
		} else {
			this.scrollTo((int) nowScroll, oldScrollY);
			this.pageScrolled((int) nowScroll, oldScrollY);
		}

		// Nested Scrolling Pre Pass
		this.mScrollConsumed[0] = 0;
		this.mScrollConsumed[1] = 0;
		final int scrolledDeltaX = (this.getScrollX() - oldScrollX);
		final int scrolledDeltaY = (this.getScrollY() - oldScrollY);
		deltaX -= scrolledDeltaX;
		deltaY -= scrolledDeltaY;
		this.dispatchNestedScroll(scrolledDeltaX, scrolledDeltaY, (int) deltaX, (int) deltaY, this.mScrollConsumed);
		this.mTouchMotionX -= this.mScrollOffset[0];
		this.mTouchMotionY -= this.mScrollOffset[1];
		deltaX -= this.mScrollConsumed[0];
		deltaY -= this.mScrollConsumed[1];

		if (startNeedsInvalidate || endNeedsInvalidate) {
			if (VERTICAL == this.mOrientation) {
				if ((startNeedsInvalidate && this.canScrollVertically(-1))
						|| (endNeedsInvalidate && this.canScrollVertically(1))) {
					return false;
				}
			} else {
				if ((startNeedsInvalidate && this.canScrollHorizontally(-1))
						|| (endNeedsInvalidate && this.canScrollHorizontally(1))) {
					return false;
				}
			}
			final float deltaDistance;
			final float displacement;
			if (VERTICAL == this.mOrientation) {
				deltaDistance = deltaY / height;
				displacement = x / width;
			} else {
				deltaDistance = deltaX / width;
				displacement = y / height;
			}
			if (startNeedsInvalidate) {
				EdgeEffectCompat.onPull(this.mStartEdgeEffect, deltaDistance, displacement);
				if (!this.mEndEdgeEffect.isFinished()) {
					this.mEndEdgeEffect.onRelease();
				}
			} else {
				EdgeEffectCompat.onPull(this.mEndEdgeEffect, deltaDistance, 1.f - displacement);
				if (!this.mStartEdgeEffect.isFinished()) {
					this.mStartEdgeEffect.onRelease();
				}
			}
			return true;
		}
		return false;
	}

	private void performEndDrag() {
		this.mIsBeingDragged = false;
		this.mIsUnableToDrag = false;

		if (this.mVelocityTracker != null) {
			this.mVelocityTracker.recycle();
			this.mVelocityTracker = null;
		}
	}

	private void setScrollState(int newState) {
		if (this.mScrollState == newState) {
			return;
		}
		this.mScrollState = newState;
		if (this.mPageTransformer != null) {
			// PageTransformers can do complex things that benefit from hardware layers.
			final boolean shouldLayerTypeEnabled = newState != SCROLL_STATE_IDLE;
			for (int index = 0; index < this.getChildCount(); index++) {
				final int layerType = shouldLayerTypeEnabled
						? this.mPageTransformerLayerType : View.LAYER_TYPE_NONE;
				this.getChildAt(index).setLayerType(layerType, null);
			}
		}
		if (this.mScrollState == SCROLL_STATE_IDLE) {
			this.setCurrentItemInternal(this.getCurrentItem(), false, false);
		}
		this.dispatchOnScrollStateChanged(newState);
	}

	private int determineTargetPage(int delta, int velocity) {
		final Page page = this.getPagerForCurrentScrollPosition();
		final int mCurrentScrollPosition = page.position;
		final int mClientWidth = this.getClientWidth();
		final int mClientHeight = this.getClientHeight();
		final int mScrollX = this.getScrollX();
		final int mScrollY = this.getScrollY();

		int nextPagePosition;
		if (Math.abs(delta) > this.mFlingDistance && Math.abs(velocity) > this.mMinimumVelocity) {
			if (velocity > 0) {
				nextPagePosition = mCurrentScrollPosition;
			} else {
				nextPagePosition = delta <= 0 ? mCurrentScrollPosition + 1 : mCurrentScrollPosition - 1;
			}
		} else {
			final float widthMarginOffset = (float) this.mPageMargin / mClientWidth;
			final float heightMarginOffset = (float) this.mPageMargin / mClientHeight;
			final float widthPageOffset = (((float) mScrollX / mClientWidth) - page.offset) / (page.weight + widthMarginOffset);
			final float heightPageOffset = (((float) mScrollY / mClientHeight) - page.offset) / (page.weight + heightMarginOffset);
			final float truncator = mCurrentScrollPosition >= this.mCurrentPosition ? 0.4f : 0.6f;

			if (VERTICAL == this.mOrientation) {
				nextPagePosition = mCurrentScrollPosition + (int) (heightPageOffset + truncator);
			} else {
				nextPagePosition = mCurrentScrollPosition + (int) (widthPageOffset + truncator);
			}
		}
		if (this.mPagePools.size() > 0) {
			// Only let the user target pages we have items for
			if (this.mIsScrollingLoop) {
				nextPagePosition = Math.max(this.mCurrentPosition - 1, Math.min(nextPagePosition, this.mCurrentPosition + 1));
			} else {
				final Page firstPage = this.mPagePools.get(0);
				final Page lastPage = this.mPagePools.get(this.mPagePools.size() - 1);
				nextPagePosition = Math.max(firstPage.position, Math.min(nextPagePosition, lastPage.position));
			}
		}
		return nextPagePosition;
	}

	private boolean isGutterDrag(float x, float y, float dx, float dy) {
		if (VERTICAL == this.mOrientation) {
			return (y < this.mGutterSize && dy > 0) || (y > this.getHeight() - this.mGutterSize && dy < 0);
		}
		return (x < this.mGutterSize && dx > 0) || (x > this.getWidth() - this.mGutterSize && dx < 0);
	}

	private boolean canScroll(View view, boolean checkView, int dx, int dy, int x, int y) {
		if (view instanceof ViewGroup) {
			final ViewGroup group = (ViewGroup) view;
			final int scrollX = view.getScrollX();
			final int scrollY = view.getScrollY();
			final int count = group.getChildCount();
			// Count backwards - let topmost views consume scroll distance first.
			for (int index = count - 1; index >= 0; index--) {
				// This will not work for transformed views in Honeycomb+
				final View preChildView = group.getChildAt(index);
				if (x + scrollX >= preChildView.getLeft() && x + scrollX < preChildView.getRight()
						&& y + scrollY >= preChildView.getTop() && y + scrollY < preChildView.getBottom()
						&& canScroll(preChildView, true, dx, dy, x + scrollX - preChildView.getLeft(), y + scrollY - preChildView.getTop())) {
					return true;
				}
			}
		}
		if (VERTICAL == this.mOrientation) {
			return checkView && view.canScrollVertically(-dy);
		}
		return checkView && view.canScrollHorizontally(-dx);
	}

	private boolean resetTouch() {
		this.mActivePointerId = INVALID_POINTER;
		this.performEndDrag();
		this.stopNestedScroll();
		this.requestParentDisallowInterceptTouchEvent(false);
		this.mStartEdgeEffect.onRelease();
		this.mEndEdgeEffect.onRelease();
		return this.mStartEdgeEffect.isFinished() || this.mEndEdgeEffect.isFinished();
	}

	private void requestParentDisallowInterceptTouchEvent(boolean disallowIntercept) {
		final ViewParent parent = this.getParent();
		if (parent != null) {
			parent.requestDisallowInterceptTouchEvent(disallowIntercept);
		}
	}

	private void onSecondaryPointerUp(MotionEvent event) {
		final int pointerIndex = event.getActionIndex();
		final int pointerId = event.getPointerId(pointerIndex);
		if (pointerId == this.mActivePointerId) {
			// This was our active pointer going up. Choose a new
			// active pointer and adjust accordingly.
			final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
			this.mTouchMotionX = event.getX(newPointerIndex);
			this.mTouchMotionY = event.getY(newPointerIndex);
			this.mActivePointerId = event.getPointerId(newPointerIndex);
			if (this.mVelocityTracker != null) {
				this.mVelocityTracker.clear();
			}
		}
	}

	private int getClientWidth() {
		return this.getMeasuredWidth() - this.getPaddingLeft() - this.getPaddingRight();
	}

	private int getClientHeight() {
		return this.getMeasuredHeight() - this.getPaddingTop() - this.getPaddingBottom();
	}

	private Adapter mAdapter;
	private PagerObserver mObserver;
	private int mExpectedItemCount;

	public void setAdapter(@Nullable Adapter adapter) {
		if (this.mAdapter == adapter) {
			return;
		}
		if (this.mAdapter != null) {
			this.mAdapter.setViewPagerObserver(null);
			this.mAdapter.onDetachedFromWindow(this);
			this.mAdapter.onStartUpdate(this);
			for (int index = 0; index < this.mPagePools.size(); index++) {
				this.performOnRemovePage(index);
				index--;
			}
			this.mAdapter.onFinishUpdate(this);
			this.removeNonDecorViews();
			this.mCurrentPosition = 0;
			this.mPagePools.clear();
			this.scrollTo(0, 0);
		}

		final Adapter oldAdapter = this.mAdapter;
		this.mAdapter = adapter;
		this.mExpectedItemCount = 0;

		if (this.mAdapter != null) {
			if (this.mObserver == null) {
				this.mObserver = new PagerObserver();
			}
			this.mAdapter.onAttachedToWindow(this);
			this.mAdapter.setViewPagerObserver(this.mObserver);
			this.mExpectedItemCount = this.mAdapter.getItemCount();
			this.mPopulatePending = false;
			final boolean wasFirstLayout = this.mFirstLayout;
			this.mFirstLayout = true;
			if (this.mRestoredAdapterState != null && this.mRestoredClassLoader != null) {
				this.mAdapter.onRestoreInstanceState(this.mRestoredAdapterState, this.mRestoredClassLoader);
				this.setCurrentItemInternal(this.mCurrentPosition, false, true);
				this.mRestoredAdapterState = null;
				this.mRestoredClassLoader = null;
			} else if (wasFirstLayout) {
				this.requestLayout();
			} else {
				this.populate();
			}
			this.mAdapter.notifyDataSetChanged();
		}
		// Dispatch the change to any listeners
		if (this.mOnAdapterChangeListeners != null && !this.mOnAdapterChangeListeners.isEmpty()) {
			for (OnAdapterChangeListener listener : this.mOnAdapterChangeListeners) {
				listener.onAdapterChanged(this, oldAdapter, adapter);
			}
		}
	}

	@Nullable
	public Adapter getAdapter() {
		return this.mAdapter;
	}

	public void addOnPageChangeListener(@NonNull OnPageChangeListener listener) {
		if (this.mOnPageChangeListeners == null) {
			this.mOnPageChangeListeners = new ArrayList<>();
		}
		this.mOnPageChangeListeners.add(listener);
	}

	public void removeOnPageChangeListener(@NonNull OnPageChangeListener listener) {
		if (this.mOnPageChangeListeners != null) {
			this.mOnPageChangeListeners.remove(listener);
		}
	}

	public void clearOnPageChangeListeners() {
		if (this.mOnPageChangeListeners != null) {
			this.mOnPageChangeListeners.clear();
		}
	}

	public void addOnAdapterChangeListener(@NonNull OnAdapterChangeListener listener) {
		if (this.mOnAdapterChangeListeners == null) {
			this.mOnAdapterChangeListeners = new ArrayList<>();
		}
		this.mOnAdapterChangeListeners.add(listener);
	}

	public void removeOnAdapterChangeListener(@NonNull OnAdapterChangeListener listener) {
		if (this.mOnAdapterChangeListeners != null) {
			this.mOnAdapterChangeListeners.remove(listener);
		}
	}

	public void setAllowUserScrollable(boolean allowUserScrollable) {
		if(this.mIsAllowUserScrollable != allowUserScrollable) {
			this.mIsAllowUserScrollable = allowUserScrollable;
			if (!this.mIsAllowUserScrollable) {
				final boolean wasFirstLayout = this.mFirstLayout;
				this.mFirstLayout = true;
				if (wasFirstLayout) {
					this.requestLayout();
				} else {
					this.populate();
				}
			}
		}
	}

	public boolean isAllowUserScrollable() {
		return this.mIsAllowUserScrollable;
	}

	public void setOffscreenPageLimit(int limit) {
		if (limit < DEFAULT_OFFSCREEN_PAGES) {
			limit = DEFAULT_OFFSCREEN_PAGES;
		}
		if (limit != this.mOffscreenPageLimit) {
			this.mOffscreenPageLimit = limit;
			this.populate();
		}
	}

	public int getOffscreenPageLimit() {
		return this.mOffscreenPageLimit;
	}

	public void setPageTransformer(boolean reverseDrawingOrder, @Nullable PageTransformer transformer) {
		this.setPageTransformer(reverseDrawingOrder, transformer, View.LAYER_TYPE_HARDWARE);
	}

	/**
	 * Sets a {@link PageTransformer} that will be called for each attached page whenever
	 * the scroll position is changed. This allows the application to apply custom property
	 * transformations to each page, overriding the default sliding behavior.
	 *
	 * @param reverseDrawingOrder true if the supplied PageTransformer requires page views
	 *                            to be drawn from last to first instead of first to last.
	 * @param transformer         PageTransformer that will modify each page's animation properties
	 * @param pageLayerType       View layer type that should be used for this pages. It should be
	 *                            either {@link View#LAYER_TYPE_HARDWARE},
	 *                            {@link View#LAYER_TYPE_SOFTWARE}, or
	 *                            {@link View#LAYER_TYPE_NONE}.
	 */
	public void setPageTransformer(boolean reverseDrawingOrder, @Nullable PageTransformer transformer, int pageLayerType) {
		final boolean hasTransformer = transformer != null;
		final boolean hasLastTransformer = this.mPageTransformer != null;
		final boolean needsPopulate = (hasTransformer == !hasLastTransformer);
		this.mPageTransformer = transformer;
		this.setChildrenDrawingOrderEnabled(hasTransformer);
		if (hasTransformer) {
			this.mDrawingOrder = reverseDrawingOrder ? DRAW_ORDER_REVERSE : DRAW_ORDER_FORWARD;
			this.mPageTransformerLayerType = pageLayerType;
		} else {
			this.mDrawingOrder = DRAW_ORDER_DEFAULT;
		}
		if (needsPopulate) {
			this.populate();
		}
	}

	@Nullable
	public PageTransformer getPageTransformer() {
		return this.mPageTransformer;
	}

	public void setPageMarginDrawable(@DrawableRes int resId) {
		this.setPageMarginDrawable(ContextCompat.getDrawable(getContext(), resId));
	}

	/**
	 * Set a drawable that will be used to fill the margin between pages.
	 *
	 * @param drawable Drawable to display between pages
	 */
	public void setPageMarginDrawable(@Nullable Drawable drawable) {
		this.mMarginDrawable = drawable;
		if (drawable != null) {
			this.refreshDrawableState();
		}
		this.setWillNotDraw(drawable == null);
		this.invalidate();
	}

	/**
	 * Set the margin between pages.
	 *
	 * @param marginPixels Distance between adjacent pages in pixels
	 * @see #getPageMargin()
	 * @see #setPageMarginDrawable(Drawable)
	 * @see #setPageMarginDrawable(int)
	 */
	public void setPageMargin(int marginPixels) {
		final int oldMargin = this.mPageMargin;
		this.mPageMargin = marginPixels;
		final int width = this.getWidth();
		final int height = this.getHeight();
		this.recomputeScrollPosition(width, height, width, height, marginPixels, oldMargin);
		this.requestLayout();
	}

	public int getPageMargin() {
		return this.mPageMargin;
	}

	void recomputeScrollPosition(int width, int height, int oldWidth, int oldHeight, int margin, int oldMargin) {
		final int mPaddingLeft = this.getPaddingLeft();
		final int mPaddingTop = this.getPaddingTop();
		final int mPaddingRight = this.getPaddingRight();
		final int mPaddingBottom = this.getPaddingBottom();
		final int mClientWidth = this.getClientWidth();
		final int mClientHeight = this.getClientHeight();

		if (((VERTICAL == this.mOrientation && oldHeight > 0) || (HORIZONTAL == this.mOrientation && oldWidth > 0)) && !this.mPagePools.isEmpty()) {
			if (!this.mScroller.isFinished()) {
				if (VERTICAL == this.mOrientation) {
					this.mScroller.setFinalY(this.mCurrentPosition * mClientHeight);
				} else {
					this.mScroller.setFinalX(this.mCurrentPosition * mClientWidth);
				}
			} else {
				final int xpos = this.getScrollX();
				final int ypos = this.getScrollY();
				final int widthWithMargin = width - mPaddingLeft - mPaddingRight + margin;
				final int heightWithMargin = height - mPaddingTop - mPaddingBottom + margin;
				final int oldWidthWithMargin = oldWidth - mPaddingLeft - mPaddingRight + oldMargin;
				final int oldHeightWithMargin = oldHeight - mPaddingTop - mPaddingBottom + oldMargin;
				final float pageOffsetX = (float) xpos / oldWidthWithMargin;
				final float pageOffsetY = (float) ypos / oldHeightWithMargin;
				final int newOffsetPixelsX = (int) (pageOffsetX * widthWithMargin);
				final int newOffsetPixelsY = (int) (pageOffsetY * heightWithMargin);
				if (VERTICAL == this.mOrientation) {
					this.scrollTo(xpos, newOffsetPixelsY);
				} else {
					this.scrollTo(newOffsetPixelsX, ypos);
				}
			}
		} else {
			final Page page = this.getPagerForPosition(this.mCurrentPosition);
			final float scrollOffset = page != null ? Math.min(page.offset, this.mLastOffset) : 0;
			final int scrollPosX = (int) (scrollOffset * (width - mPaddingLeft - mPaddingRight));
			final int scrollPosY = (int) (scrollOffset * (height - mPaddingTop - mPaddingBottom));
			this.completeScroll(false);
			if (VERTICAL == this.mOrientation
					&& scrollPosY != this.getScrollY()) {
				this.scrollTo(this.getScrollX(), scrollPosY);
			} else if (HORIZONTAL == this.mOrientation
					&& scrollPosX != this.getScrollX()) {
				this.scrollTo(scrollPosX, this.getScrollY());
			}
		}
	}

	@OrientationMode
	public int getOrientation() {
		return this.mOrientation;
	}

	public void setOrientation(@OrientationMode int orientation) {
		if (this.mOrientation != orientation) {
			this.mOrientation = orientation;
			final float density = this.getResources().getDisplayMetrics().density;
			if (VERTICAL == this.mOrientation) {
				this.mMinimumVelocity = (int) (MIN_FLING_VELOCITY_VERTICAL * density);
			} else {
				this.mMinimumVelocity = (int) (MIN_FLING_VELOCITY_HORIZONTAL * density);
			}
			if (this.mFirstLayout) {
				this.populate();
			} else {
				this.mFirstLayout = true;
				this.requestLayout();
			}
		}
	}

	public void setScrollingDuration(int duration) {
		this.mScrollingDuration = duration;
	}

	public int getScrollingDuration() {
		return this.mScrollingDuration;
	}

	public void setScrollingLoop(boolean loop) {
		if (this.mIsScrollingLoop != loop) {
			this.mIsScrollingLoop = loop;
			if (this.mFirstLayout) {
				this.populate();
			} else {
				this.mFirstLayout = true;
				this.requestLayout();
			}
		}
	}

	public boolean isScrollingLoop() {
		return this.mIsScrollingLoop;
	}

	public int getCurrentItem() {
		return this.adapterPositionForPosition(this.mCurrentPosition);
	}

	public void setCurrentItem(int position) {
		this.setCurrentItem(position, !this.mFirstLayout);
	}

	public void setCurrentItem(int position, boolean smoothScroll) {
		if (this.mAdapter == null) {
			throw new IllegalStateException("Not adapter created set");
		}
		if (position < 0 || position >= this.mAdapter.getItemCount()) {
			throw new IndexOutOfBoundsException("Index: " + position + ", Size: " + this.mAdapter.getItemCount());
		}
		this.mPopulatePending = false;
		if (this.mIsScrollingLoop) {
			final int mAdapterPosition = this.adapterPositionForPosition(this.mCurrentPosition);
			final int mCurrentPosition = this.getCurrentPositionDelta(mAdapterPosition, position, this.mAdapter.getItemCount()) + this.mCurrentPosition;
			this.setCurrentItemInternal(mCurrentPosition, smoothScroll, false);
		} else {
			this.setCurrentItemInternal(position, smoothScroll, false);
		}
	}

	int getCurrentPositionDelta(int oldPosition, int nowPosition, int size) {
		if (oldPosition < nowPosition) {
			final int d1 = nowPosition - oldPosition;
			final int d2 = size + oldPosition - nowPosition;
			return d1 <= d2 ? d1 : -d2;
		} else if (oldPosition > nowPosition) {
			final int d1 = oldPosition - nowPosition;
			final int d2 = size - oldPosition + nowPosition;
			return d1 <= d2 ? -d1 : d2;
		}
		return 0;
	}

	void setCurrentItemInternal(int position, boolean smoothScroll, boolean always) {
		this.setCurrentItemInternal(position, smoothScroll, always, 0);
	}

	void setCurrentItemInternal(int position, boolean smoothScroll, boolean always, int velocity) {
		if (this.mAdapter == null || this.mAdapter.getItemCount() <= 0) {
			this.setScrollingCacheEnabled(false);
			return;
		}
		if (!always && this.mCurrentPosition == position && this.mPagePools.size() != 0) {
			this.setScrollingCacheEnabled(false);
			return;
		}

		final int pageLimit = this.mOffscreenPageLimit;
		if (position > (this.mCurrentPosition + pageLimit) || position < (this.mCurrentPosition - pageLimit)) {
			for (int index = 0; index < this.mPagePools.size(); index++) {
				this.mPagePools.get(index).scrolling = true;
			}
		}
		final boolean dispatchSelected = this.mCurrentPosition != position;
		if (this.mFirstLayout) {
			// We don't have any idea how big we are yet and shouldn't have any pages either.
			// Just set things up and let the pending layout handle things.
			this.mCurrentPosition = position;
			if (dispatchSelected) {
				this.dispatchOnPageSelected(this.adapterPositionForPosition(position));
			}
			this.requestLayout();
		} else {
			this.populate(position);
			this.scrollToItem(position, smoothScroll, velocity, dispatchSelected);
		}
	}

	void scrollToItem(int position, boolean smoothScroll, int velocity, boolean dispatchSelected) {
		final Page page = this.getPagerForPosition(position);
		int destX = 0;
		int destY = 0;
		if (page != null) {
			final int width = this.getClientWidth();
			final int height = this.getClientHeight();
			if (VERTICAL == this.mOrientation) {
				destY = (int) (height * Math.max(this.mFirstOffset, Math.min(page.offset, this.mLastOffset)));
			} else {
				destX = (int) (width * Math.max(this.mFirstOffset, Math.min(page.offset, this.mLastOffset)));
			}
		}
		if (smoothScroll) {
			this.smoothScrollTo(destX, destY, velocity);
		} else {
			this.completeScroll(false);
			this.scrollTo(destX, destY);
			this.pageScrolled(destX, destY);
		}
		if (dispatchSelected) {
			this.dispatchOnPageSelected(this.adapterPositionForPosition(position));
		}
	}

	void smoothScrollTo(int x, int y, int velocity) {
		if (this.getChildCount() == 0) {
			// Nothing to do.
			this.setScrollingCacheEnabled(false);
			return;
		}

		int scrollX;
		int scrollY;
		boolean wasScrolling = (this.mScroller != null) && !this.mScroller.isFinished();
		if (wasScrolling) {
			scrollX = this.mIsScrollStarted ? this.mScroller.getCurrX() : this.mScroller.getStartX();
			scrollY = this.mIsScrollStarted ? this.mScroller.getCurrY() : this.mScroller.getStartY();
			// And abort the current scrolling.
			this.mScroller.abortAnimation();
			this.setScrollingCacheEnabled(false);
		} else {
			scrollX = this.getScrollX();
			scrollY = this.getScrollY();
		}
		int dx = x - scrollX;
		int dy = y - scrollY;
		if (dx == 0 && dy == 0) {
			this.completeScroll(false);
			this.populate();
			this.setScrollState(SCROLL_STATE_IDLE);
			return;
		}
		this.setScrollingCacheEnabled(true);
		this.setScrollState(SCROLL_STATE_SETTLING);

		final int width = this.getClientWidth();
		final int height = this.getClientHeight();
		final int halfWidth = width / 2;
		final int halfHeight = height / 2;
		final float distance;
		if (VERTICAL == this.mOrientation) {
			distance = halfHeight + halfHeight * this.distanceInfluenceForSnapDuration(Math.min(1f, 1.0f * Math.abs(dy) / height));
		} else {
			distance = halfWidth + halfWidth * this.distanceInfluenceForSnapDuration(Math.min(1f, 1.0f * Math.abs(dx) / width));
		}

		final int duration;
		if (this.mScrollingDuration > 0) {
			duration = this.mScrollingDuration;
		} else {
			if (Math.abs(velocity) > 0) {
				duration = 4 * Math.round(1000 * Math.abs(distance / Math.abs(velocity)));
			} else {
				final int adapterPosition = this.adapterPositionForPosition(this.mCurrentPosition);
				final float pageWidth = width * this.mAdapter.getPageWeight(adapterPosition);
				final float pageHeight = height * this.mAdapter.getPageWeight(adapterPosition);
				final float pageDelta;
				if (VERTICAL == this.mOrientation) {
					pageDelta = (float) Math.abs(dy) / (pageHeight + this.mPageMargin);
				} else {
					pageDelta = (float) Math.abs(dx) / (pageWidth + this.mPageMargin);
				}
				duration = (int) ((pageDelta + 1) * 100);
			}
		}
		// Reset the "scroll started" flag. It will be flipped to true in all places
		// where we call computeScrollOffset().
		this.mIsScrollStarted = false;
		this.mScroller.startScroll(scrollX, scrollY, dx, dy, Math.min(duration, MAX_SETTLE_DURATION));
		ViewCompat.postInvalidateOnAnimation(this);
	}

	private float distanceInfluenceForSnapDuration(float distanceRatio) {
		distanceRatio -= 0.5f; // center the values about 0.
		distanceRatio *= 0.3f * (float) Math.PI / 2.0f;
		return (float) Math.sin(distanceRatio);
	}

	private final Runnable mCompleteScrollRunnable = new Runnable() {

		@Override
		public void run() {
			ViewPagerCompat.this.setScrollState(SCROLL_STATE_IDLE);
			ViewPagerCompat.this.populate();
		}
	};

	private void completeScroll(boolean postEvents) {
		boolean needPopulate = this.mScrollState == SCROLL_STATE_SETTLING;
		if (needPopulate) {
			// Done with scroll, no longer want to cache view drawing.
			this.setScrollingCacheEnabled(false);
			// Check scrolling
			if (!this.mScroller.isFinished()) {
				this.mScroller.abortAnimation();
				int oldScrollX = this.getScrollX();
				int oldScrollY = this.getScrollY();
				int x = this.mScroller.getCurrX();
				int y = this.mScroller.getCurrY();
				if (oldScrollX != x || oldScrollY != y) {
					this.scrollTo(x, y);

					if (VERTICAL == this.mOrientation) {
						if (oldScrollY != y) {
							this.pageScrolled(0, y);
						}
					} else {
						if (oldScrollX != x) {
							this.pageScrolled(x, 0);
						}
					}
				}
			}
		}
		this.mPopulatePending = false;
		for (Page page : this.mPagePools) {
			if (page.scrolling) {
				needPopulate = true;
				page.scrolling = false;
			}
		}
		if (needPopulate) {
			if (postEvents) {
				ViewCompat.postOnAnimation(this, this.mCompleteScrollRunnable);
			} else {
				this.mCompleteScrollRunnable.run();
			}
		}
	}

	private boolean pageScrolled(int x, int y) {
		if (this.mPagePools.size() == 0) {
			if (this.mFirstLayout) {
				return false;
			}
			this.mCalledSuper = false;
			this.onPageScrolled(0, 0, 0);
			if (!this.mCalledSuper) {
				throw new IllegalStateException("onPageScrolled did not call superclass implementation");
			}
			return false;
		}
		final Page page = this.getPagerForCurrentScrollPosition();
		final int width = this.getClientWidth();
		final int height = this.getClientHeight();
		final float pageMargin = this.mPageMargin;
		final float mPageOffset;
		final int mOffsetPixels;

		if (VERTICAL == this.mOrientation) {
			mPageOffset = (((float) y / height) - page.offset) / (page.weight + (pageMargin / height));
			mOffsetPixels = (int) (mPageOffset * (height + pageMargin));
		} else {
			mPageOffset = (((float) x / width) - page.offset) / (page.weight + (pageMargin / width));
			mOffsetPixels = (int) (mPageOffset * (width + pageMargin));
		}
		this.mCalledSuper = false;
		this.onPageScrolled(page.position, mPageOffset, mOffsetPixels);
		if (!this.mCalledSuper) {
			throw new IllegalStateException("onPageScrolled did not call superclass implementation");
		}
		return true;
	}

	private void onPageScrolled(int position, float offset, int offsetPixels) {
		final int mScrollX = this.getScrollX();
		final int mScrollY = this.getScrollY();

		if (this.mDecorChildCount > 0) {
			int prePaddingLeft = this.getPaddingLeft();
			int prePaddingTop = this.getPaddingTop();
			int prePaddingRight = this.getPaddingRight();
			int prePaddingBottom = this.getPaddingBottom();
			final int width = this.getWidth();
			final int height = this.getHeight();
			for (int index = 0; index < this.getChildCount(); index++) {
				final View preChildView = this.getChildAt(index);
				final LayoutParams preLayoutParams = (LayoutParams) preChildView.getLayoutParams();
				if (!preLayoutParams.isDecor) {
					continue;
				}
				final int horizontalGravity = preLayoutParams.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
				final int verticalGravity = preLayoutParams.gravity & Gravity.VERTICAL_GRAVITY_MASK;
				int preChildLeft;
				int preChildTop;
				switch (horizontalGravity) {
					default:
						preChildLeft = prePaddingLeft;
						break;
					case Gravity.LEFT:
						preChildLeft = prePaddingLeft;
						prePaddingLeft += preChildView.getWidth();
						break;
					case Gravity.CENTER_HORIZONTAL:
						preChildLeft = Math.max((width - preChildView.getMeasuredWidth()) / 2, prePaddingLeft);
						break;
					case Gravity.RIGHT:
						preChildLeft = width - prePaddingRight - preChildView.getMeasuredWidth();
						prePaddingRight += preChildView.getMeasuredWidth();
						break;
				}
				switch (verticalGravity) {
					default:
						preChildTop = prePaddingTop;
						break;
					case Gravity.TOP:
						preChildTop = prePaddingTop;
						prePaddingTop += preChildView.getHeight();
						break;
					case Gravity.CENTER_VERTICAL:
						preChildTop = Math.max((height - preChildView.getMeasuredHeight()) / 2, prePaddingTop);
						break;
					case Gravity.BOTTOM:
						preChildTop = height - prePaddingBottom - preChildView.getMeasuredHeight();
						prePaddingBottom += preChildView.getMeasuredHeight();
						break;
				}
				preChildLeft += mScrollX;
				preChildTop += mScrollY;
				final int childOffsetLeft = preChildLeft - preChildView.getLeft();
				final int childOffsetTop = preChildTop - preChildView.getTop();
				if (childOffsetLeft != 0) {
					preChildView.offsetLeftAndRight(childOffsetLeft);
				}
				if (childOffsetTop != 0) {
					preChildView.offsetTopAndBottom(childOffsetTop);
				}
			}
		}
		this.dispatchOnPageScrolled(this.adapterPositionForPosition(position), offset, offsetPixels);

		if (this.mPageTransformer != null) {
			for (int index = 0; index < this.getChildCount(); index++) {
				final View preChildView = this.getChildAt(index);
				final LayoutParams preLayoutParams = (LayoutParams) preChildView.getLayoutParams();
				if (preLayoutParams.isDecor) {
					continue;
				}
				final float transformPos;
				if (VERTICAL == this.mOrientation) {
					transformPos = (float) (preChildView.getTop() - mScrollY) / this.getClientHeight();
				} else {
					transformPos = (float) (preChildView.getLeft() - mScrollX) / this.getClientWidth();
				}
				/*
				 * transformPos
				 * left page : [~, 0)
				 * center page : (-1, 1)
				 * right page : (0, ~]
				 * */
				this.mPageTransformer.transformPage(preChildView, transformPos);
			}
		}
		this.mCalledSuper = true;
	}

	private void dispatchOnScrollStateChanged(int state) {
		if (this.mOnPageChangeListeners != null) {
			for (OnPageChangeListener listener : this.mOnPageChangeListeners) {
				listener.onPageScrollStateChanged(state);
			}
		}
	}

	private void dispatchOnPageScrolled(int position, float offset, int offsetPixels) {
		if (this.mOnPageChangeListeners != null) {
			for (OnPageChangeListener listener : this.mOnPageChangeListeners) {
				listener.onPageScrolled(position, offset, offsetPixels);
			}
		}
	}

	private void dispatchOnPageSelected(int position) {
		if (this.mOnPageChangeListeners != null) {
			for (OnPageChangeListener listener : this.mOnPageChangeListeners) {
				listener.onPageSelected(position);
			}
		}
	}

	private int mCurrentPosition = 0;
	private int mOffscreenPageLimit = DEFAULT_OFFSCREEN_PAGES;
	private ArrayList<OnPageChangeListener> mOnPageChangeListeners;
	private ArrayList<OnAdapterChangeListener> mOnAdapterChangeListeners;
	private final Page mTempPage = new Page();
	private final ArrayList<Page> mPagePools = new ArrayList<>();
	private final ArrayList<View> mDrawingOrderedChilds = new ArrayList<>();
	private final ViewPositionComparator sViewPositionComparator = new ViewPositionComparator();
	private final PagePositionComparator sPagePositionComparator = new PagePositionComparator();

	void dataSetChanged() {
		// This method only gets called if our observer is attached, so mAdapter is non-null.
		final int itemCount = this.mAdapter.getItemCount();
		boolean needPopulate = this.mPagePools.size() < this.mOffscreenPageLimit * 2 + 1 && this.mPagePools.size() < itemCount;

		if (this.mExpectedItemCount != itemCount) {
			this.mExpectedItemCount = itemCount;
			needPopulate = true;
		}

		final int oldCurrentPosition = this.mCurrentPosition;
		int nowCurrentPosition = this.adapterPositionForPosition(oldCurrentPosition);
		if (oldCurrentPosition != nowCurrentPosition) {
			needPopulate = true;
		}

		boolean isUpdating = false;
		for (int index = 0; index < this.mPagePools.size(); index++) {
			final Page page = this.mPagePools.get(index);
			final int newPosition = this.mAdapter.getItemPosition(this, page.object);

			if (newPosition == Adapter.POSITION_UNCHANGED) {
				continue;
			}

			if (newPosition == Adapter.POSITION_NONE) {
				this.mPagePools.remove(index);
				index--;

				if (!isUpdating) {
					this.mAdapter.onStartUpdate(this);
					isUpdating = true;
				}
				this.mAdapter.onDestroyItem(this, page.object, this.adapterPositionForPosition(page.position));
				needPopulate = true;

				if (page.position == this.mCurrentPosition) {
					// Keep the current item in the valid range
					nowCurrentPosition = Math.max(0, Math.min(this.mCurrentPosition, itemCount - 1));
					needPopulate = true;
				}
				continue;
			}
			// normal ？
			if (!this.mIsScrollingLoop && page.position != newPosition) {
				if (page.position == this.mCurrentPosition) {
					// Our current item changed position. Follow it.
					nowCurrentPosition = newPosition;
				}
				page.position = newPosition;
				needPopulate = true;
			}
		}
		if (isUpdating) {
			this.mAdapter.onFinishUpdate(this);
		}
		Collections.sort(this.mPagePools, this.sPagePositionComparator);

		if (needPopulate) {
			// Reset our known page widths; populate will recompute them.
			for (int index = 0; index < this.getChildCount(); index++) {
				final LayoutParams preLayoutParams = (LayoutParams) this.getChildAt(index).getLayoutParams();
				if (!preLayoutParams.isDecor) {
					preLayoutParams.weight = 0.f;
				}
			}
			this.setCurrentItemInternal(nowCurrentPosition, false, true);
			this.requestLayout();
		}
	}

	void populate() {
		this.populate(this.mCurrentPosition);
	}

	void populate(int currentPosition) {
		// set position
		Page oldCurrentPage = null;
		if (this.mCurrentPosition != currentPosition) {
			oldCurrentPage = this.getPagerForPosition(this.mCurrentPosition);
			this.mCurrentPosition = currentPosition;
		}

		if (this.mAdapter == null) {
			this.sortChildDrawingOrder();
			return;
		}

		if (this.mPopulatePending) {
			this.sortChildDrawingOrder();
			return;
		}

		if (this.getWindowToken() == null) {
			return;
		}
		this.mAdapter.onStartUpdate(this);

		final int mItemCount = this.mAdapter.getItemCount();
		if (mItemCount != this.mExpectedItemCount) {
			String resName;
			try {
				resName = getResources().getResourceName(getId());
			} catch (Resources.NotFoundException e) {
				resName = Integer.toHexString(getId());
			}
			throw new IllegalStateException("The application's Adapter changed the adapter's"
					+ " contents without calling Adapter#notifyDataSetChanged!"
					+ " Expected adapter item count: " + this.mExpectedItemCount + ", found: " + mItemCount
					+ " Page id: " + resName
					+ " Page class: " + getClass()
					+ " Problematic adapter: " + this.mAdapter.getClass());
		}

		int currentIndex;
		Page currentPage = null;
		for (currentIndex = 0; currentIndex < this.mPagePools.size(); currentIndex++) {
			final Page page = this.mPagePools.get(currentIndex);
			if (page.position >= currentPosition) {
				if (page.position == currentPosition) {
					currentPage = page;
				}
				break;
			}
		}

		if (currentPage == null && mItemCount > 0) {
			currentPage = this.performOnCreatePage(currentPosition, currentIndex);
		}

		if (currentPage != null) {
			TraceCompat.beginSection("populate");
			final int pageLimit = this.mOffscreenPageLimit;
			int limitStartPosition = currentPosition - pageLimit;
			int limitEndPosition = currentPosition + pageLimit;
			int nextPosition = currentPosition - 1;
			int nextIndex = currentIndex - 1;

			if (!this.mIsScrollingLoop) {
				limitStartPosition = Math.max(limitStartPosition, 0);
				limitEndPosition = Math.min(limitEndPosition, mItemCount - 1);
			}

			// LEFT PAGES
			while (nextPosition >= limitStartPosition || nextIndex >= 0) {
				final Page page = nextIndex >= 0 ? this.mPagePools.get(nextIndex) : null;
				// add page to left
				if (nextPosition >= limitStartPosition) {
					if (page != null && page.position == nextPosition) {
						nextIndex--;
					} else {
						this.performOnCreatePage(nextPosition, nextIndex + 1);
						// pointer move for right
						currentIndex++;
					}
				} else {
					if (page == null || page.scrolling) {
						nextIndex--;
					} else {
						// scrolling not remove
						if (page.position == nextPosition) {
							this.performOnRemovePage(nextIndex);
							// pointer move for left
							currentIndex--;
							// next page index move for left
							nextIndex--;
						}
					}
				}
				nextPosition--;
			}

			nextPosition = currentPosition + 1;
			nextIndex = currentIndex + 1;
			// RIGHT PAGES
			while (nextPosition <= limitEndPosition || nextIndex < this.mPagePools.size()) {
				final Page page = nextIndex < this.mPagePools.size() ? this.mPagePools.get(nextIndex) : null;
				// add page to right
				if (nextPosition <= limitEndPosition) {
					if (page == null || page.position != nextPosition) {
						this.performOnCreatePage(nextPosition, nextIndex);
					}
					nextIndex++;
				} else {
					if (page == null || page.scrolling) {
						nextIndex++;
					} else {
						// scrolling not remove
						if (page.position == nextPosition) {
							this.performOnRemovePage(nextIndex);
							// next page index move for right
							nextIndex++;
						}
					}
				}
				nextPosition++;
			}
			TraceCompat.endSection();
			// calculate offset
			this.calculatePageOffsets(currentPage, oldCurrentPage, currentIndex);
			// preview item
			this.mAdapter.onPrimaryItem(this, currentPage.object, this.adapterPositionForPosition(currentPosition));
		}
		this.mAdapter.onFinishUpdate(this);

		// Check width measurement of current pages and drawing sort order.
		// Update LayoutParams as needed.
		for (int index = 0; index < this.getChildCount(); index++) {
			final View preChildView = this.getChildAt(index);
			final LayoutParams preLayoutParams = (LayoutParams) preChildView.getLayoutParams();
			preLayoutParams.childIndex = index;
			if (!preLayoutParams.isDecor && preLayoutParams.weight == 0.f) {
				// 0 means requery the adapter for this, it doesn't have a valid width.
				final Page page = this.getPagerForChild(preChildView);
				if (page != null) {
					preLayoutParams.weight = page.weight;
					preLayoutParams.position = page.position;
				}
			}
		}
		this.sortChildDrawingOrder();

		if (this.hasFocus()) {
			final View mCurrentFocused = this.findFocus();
			Page page = mCurrentFocused != null ? this.getPagerForAnyChild(mCurrentFocused) : null;
			if (page == null || page.position != currentPosition) {
				for (int index = 0; index < this.getChildCount(); index++) {
					final View preChildView = this.getChildAt(index);
					page = this.getPagerForChild(preChildView);
					if (page != null && page.position == currentPosition) {
						if (preChildView.requestFocus(View.FOCUS_FORWARD)) {
							break;
						}
					}
				}
			}
		}
	}

	void sortChildDrawingOrder() {
		if (this.mDrawingOrder != DRAW_ORDER_DEFAULT) {
			this.mDrawingOrderedChilds.clear();
			for (int index = 0; index < this.getChildCount(); index++) {
				this.mDrawingOrderedChilds.add(this.getChildAt(index));
			}
			Collections.sort(this.mDrawingOrderedChilds, this.sViewPositionComparator);
		}
	}

	void calculatePageOffsets(@NonNull Page currentPage, @Nullable Page oldPage, int curIndex) {
		if (DEBUG) {
			for (Page page : this.mPagePools) {
				Log.e(TAG, "calculatePageOffsets start : " + page.position + " => " + page.offset);
			}
		}

		final int size = this.mPagePools.size();
		final int itemCount = this.mAdapter.getItemCount();
		final int clientWidth = this.getClientWidth();
		final int clientHeight = this.getClientHeight();
		final float widthMarginOffset = clientWidth > 0 ? (float) this.mPageMargin / clientWidth : 0;
		final float heightMarginOffset = clientHeight > 0 ? (float) this.mPageMargin / clientHeight : 0;
		final float marginOffset;

		if (VERTICAL == this.mOrientation) {
			marginOffset = heightMarginOffset;
		} else {
			marginOffset = widthMarginOffset;
		}

		if (oldPage != null) {
			final int oldPosition = oldPage.position;
			final int nowPosition = currentPage.position;
			float offset;

			if (oldPosition < nowPosition) {
				offset = oldPage.offset + oldPage.weight + marginOffset;
				for (int position = oldPosition + 1, nextIndex = 0; position <= nowPosition && nextIndex < size; position++) {
					Page page = this.mPagePools.get(nextIndex);
					while (position > page.position && nextIndex < size - 1) {
						page = this.mPagePools.get(++nextIndex);
					}
					while (position < page.position) {
						offset += this.mAdapter.getPageWeight(this.adapterPositionForPosition(position)) + marginOffset;
						position++;
					}
					page.offset = offset;
					offset += page.weight + marginOffset;
				}
			} else if (oldPosition > nowPosition) {
				offset = oldPage.offset;
				for (int position = oldPosition - 1, nextIndex = size - 1; position >= nowPosition && nextIndex >= 0; position--) {
					Page page = this.mPagePools.get(nextIndex);
					while (position < page.position && nextIndex > 0) {
						page = this.mPagePools.get(--nextIndex);
					}
					while (position > page.position) {
						offset -= this.mAdapter.getPageWeight(this.adapterPositionForPosition(position)) + marginOffset;
						position--;
					}
					offset -= page.weight + marginOffset;
					page.offset = offset;
				}
			}
		}

		final int nowPosition = currentPage.position;
		final float nowOffset = currentPage.offset;
		float offset;

		if (this.mIsScrollingLoop) {
			this.mFirstOffset = nowOffset - currentPage.weight - marginOffset;
			this.mLastOffset = nowOffset + currentPage.weight + marginOffset;
		} else {
			this.mFirstOffset = currentPage.position == 0 ? currentPage.offset : -Float.MAX_VALUE;
			this.mLastOffset = currentPage.position == itemCount - 1
					? currentPage.offset + currentPage.weight - 1 : Float.MAX_VALUE;
		}

		// LEFT PAGES OFFSET
		offset = nowOffset;
		for (int index = curIndex - 1, position = nowPosition - 1; index >= 0; index--, position--) {
			Page page = this.mPagePools.get(index);
			while (position > page.position) {
				offset -= this.mAdapter.getPageWeight(this.adapterPositionForPosition(position--)) + marginOffset;
				position--;
			}
			offset -= page.weight + marginOffset;
			page.offset = offset;
			if (this.mIsScrollingLoop) {
				if (page.position + 1 == nowPosition) {
					this.mFirstOffset = offset;
				}
			} else {
				if (page.position == 0) {
					this.mFirstOffset = offset;
				}
			}
		}
		// RIGHT PAGES OFFSET
		offset = nowOffset + currentPage.weight + marginOffset;
		for (int index = curIndex + 1, position = nowPosition + 1; index < size; index++, position++) {
			Page page = this.mPagePools.get(index);
			while (position < page.position) {
				offset += this.mAdapter.getPageWeight(this.adapterPositionForPosition(position++)) + marginOffset;
				position++;
			}
			if (this.mIsScrollingLoop) {
				if (page.position - 1 == nowPosition) {
					this.mLastOffset = offset;
				}
			} else {
				if (page.position == itemCount - 1) {
					this.mLastOffset = offset + page.weight - 1;
				}
			}
			page.offset = offset;
			offset += page.weight + marginOffset;
		}
		if (DEBUG) {
			for (Page page : this.mPagePools) {
				Log.e(TAG, "calculatePageOffsets end : " + page.position + " => " + page.offset);
			}
			Log.e(TAG, "calculatePageOffsets Bound(" + mFirstOffset + "," + mLastOffset + ")");
		}
	}

	int adapterPositionForPosition(int position) {
		final int itemCount = this.mAdapter.getItemCount();
		if (itemCount > 0) {
			int adapterPosition = position;
			if (position >= itemCount) {
				adapterPosition %= itemCount;
			} else if (position < 0) {
				int delta = (-position) % itemCount;
				if (delta == 0) {
					delta = itemCount;
				}
				adapterPosition = itemCount - delta;
			}
			return adapterPosition;
		}
		return 0;
	}

	@NonNull
	Page performOnCreatePage(int position, int index) {
		final int adapterPosition = this.adapterPositionForPosition(position);
		final Page page = new Page();
		page.position = position;
		page.weight = this.mAdapter.getPageWeight(adapterPosition);
		page.object = this.mAdapter.onCreateItem(this, adapterPosition, position);
		if (index < 0 || index >= this.mPagePools.size()) {
			this.mPagePools.add(page);
		} else {
			this.mPagePools.add(index, page);
		}
		return page;
	}

	@Nullable
	Page performOnRemovePage(int index) {
		if (index >= 0 && index < this.mPagePools.size()) {
			final Page page = this.mPagePools.remove(index);
			this.mAdapter.onDestroyItem(this, page.object, this.adapterPositionForPosition(page.position));
			return page;
		}
		return null;
	}

	@Nullable
	Page getPagerForChild(@NonNull View child) {
		for (int index = 0; index < this.mPagePools.size(); index++) {
			final Page page = this.mPagePools.get(index);
			if (this.mAdapter.isViewFromObject(child, page.object)) {
				return page;
			}
		}
		return null;
	}

	@Nullable
	Page getPagerForAnyChild(@NonNull View child) {
		ViewParent parent;
		while ((parent = child.getParent()) != this) {
			if (!(parent instanceof View)) {
				return null;
			}
			child = (View) parent;
		}
		return this.getPagerForChild(child);
	}

	@Nullable
	Page getPagerForPosition(int position) {
		for (int index = 0; index < this.mPagePools.size(); index++) {
			final Page page = this.mPagePools.get(index);
			if (page.position == position) {
				return page;
			}
		}
		return null;
	}

	Page getPagerForCurrentScrollPosition() {
		final float mScrollX = this.getScrollX();
		final float mScrollY = this.getScrollY();
		final int mClientWidth = this.getClientWidth();
		final int mClientHeight = this.getClientHeight();
		final float scrollOffset;
		final float marginOffset;

		if (VERTICAL == this.mOrientation) {
			marginOffset = mClientHeight > 0 ? (float) this.mPageMargin / mClientHeight : 0;
			scrollOffset = mClientHeight > 0 ? mScrollY / mClientHeight : 0;
		} else {
			marginOffset = mClientWidth > 0 ? (float) this.mPageMargin / mClientWidth : 0;
			scrollOffset = mClientWidth > 0 ? mScrollX / mClientWidth : 0;
		}
		if (DEBUG) {
			Log.e(TAG, "ScrollOffset : " + scrollOffset + " ==> Scroll(" + mScrollX + "," + mScrollY + ") ==> ChildCount : " + mPagePools.size());
		}
		int lastPosition = -1;
		float lastOffset = 0.f;
		float lastPageWeight = 0.f;
		boolean shouldFirst = true;
		Page lastPage = null;
		for (int index = 0; index < this.mPagePools.size(); index++) {
			Page page = this.mPagePools.get(index);

			if (!shouldFirst && page.position != lastPosition + 1) {
				// Create a synthetic item for a missing page.
				page = this.mTempPage;
				page.offset = lastOffset + lastPageWeight + marginOffset;
				page.position = lastPosition + 1;
				page.weight = this.mAdapter.getPageWeight(this.adapterPositionForPosition(page.position));
				index--;
			}
			final float offset = page.offset;
			final float leftBound = offset;
			final float rightBound = offset + page.weight + marginOffset;

			if (DEBUG) {
				Log.e(TAG, "Index : " + index + " ==> [" + leftBound + "," + rightBound + ") ==> position : " + page.position + " ==> curPosition : " + mCurrentPosition);
			}
			if (shouldFirst || scrollOffset >= leftBound) {
				if (scrollOffset < rightBound || index == this.mPagePools.size() - 1) {
					return page;
				}
			} else {
				return lastPage;
			}
			shouldFirst = false;
			lastOffset = offset;
			lastPosition = page.position;
			lastPageWeight = page.weight;
			lastPage = page;
		}
		return null;
	}

	private void removeNonDecorViews() {
		for (int index = 0; index < this.getChildCount(); index++) {
			final LayoutParams preLayoutParams = (LayoutParams) this.getChildAt(index).getLayoutParams();
			if (!preLayoutParams.isDecor) {
				this.removeViewAt(index);
				index--;
			}
		}
	}

	private void setScrollingCacheEnabled(boolean enabled) {
		if (this.mScrollingCacheEnabled != enabled) {
			this.mScrollingCacheEnabled = enabled;
			if (USE_CACHE) {
				for (int index = 0; index < this.getChildCount(); index++) {
					final View preChildView = this.getChildAt(index);
					if (preChildView.getVisibility() != GONE) {
						preChildView.setDrawingCacheEnabled(enabled);
					}
				}
			}
		}
	}

	static final int[] LAYOUT_ATTRS = new int[]{
			android.R.attr.layout_gravity
	};

	public static class LayoutParams extends ViewGroup.LayoutParams {

		private static final int NO_POSITION = -1;
		private static final int NO_INDEX = -1;
		private static final float INVALID_WEIGHT = 0.f;

		public boolean isDecor = false;

		public boolean needsMeasure = false;

		public int gravity = Gravity.CENTER;

		int position = NO_POSITION;

		int childIndex = NO_INDEX;

		float weight = INVALID_WEIGHT;

		public LayoutParams() {
			super(MATCH_PARENT, MATCH_PARENT);
		}

		public LayoutParams(int width, int height) {
			super(width, height);
		}

		public LayoutParams(LayoutParams source) {
			super(source);
			this.gravity = source.gravity;
			this.isDecor = source.isDecor;
			this.needsMeasure = source.needsMeasure;
		}

		public LayoutParams(ViewGroup.LayoutParams source) {
			super(source);
		}

		public LayoutParams(Context context, AttributeSet attrs) {
			super(context, attrs);
			final TypedArray mTypedArray = context.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
			this.gravity = mTypedArray.getInteger(0, Gravity.TOP);
			mTypedArray.recycle();
		}

		public final int getPosition() {
			return this.position;
		}

		public final int getChildIndex() {
			return this.childIndex;
		}

		public final float getWeight() {
			return this.weight;
		}
	}

	public static abstract class Adapter {

		public static final int POSITION_UNCHANGED = -1;

		public static final int POSITION_NONE = -2;

		private final DataSetObservable mObservable = new DataSetObservable();

		private DataSetObserver mViewPagerObserver;

		public final void registerDataSetObserver(@NonNull DataSetObserver observer) {
			this.mObservable.registerObserver(observer);
		}

		public final void unregisterDataSetObserver(@NonNull DataSetObserver observer) {
			this.mObservable.unregisterObserver(observer);
		}

		public final void notifyDataSetChanged() {
			synchronized (this) {
				if (this.mViewPagerObserver != null) {
					this.mViewPagerObserver.onChanged();
				}
			}
			this.mObservable.notifyChanged();
		}

		@CallSuper
		public void onAttachedToWindow(@NonNull ViewGroup container) {
			// NO-OP
		}

		@CallSuper
		public void onDetachedFromWindow(@NonNull ViewGroup container) {
			// NO-OP
		}

		@CallSuper
		public void onSaveInstanceState(@NonNull Bundle saveInstanceState) {
			// NO-OP
		}

		@CallSuper
		public void onRestoreInstanceState(@Nullable Bundle saveInstanceState, @Nullable ClassLoader classLoader) {
			// NO-OP
		}

		/**
		 * @param position     is a real position, bound : [0, {@link #getItemCount()})
		 * @param pagePosition is a page position, bound : (~, ~)
		 */
		@NonNull
		public abstract Object onCreateItem(@NonNull ViewGroup container, int position, int pagePosition);

		public abstract void onDestroyItem(@NonNull ViewGroup container, @NonNull Object object, int position);

		public abstract boolean isViewFromObject(@NonNull View view, @NonNull Object object);

		public abstract int getItemCount();

		@CallSuper
		public void onPrimaryItem(@NonNull ViewGroup container, @NonNull Object object, int position) {
			// NO-OP
		}

		@CallSuper
		public void onStartUpdate(@NonNull ViewGroup container) {
			// NO-OP
		}

		@CallSuper
		public void onFinishUpdate(@NonNull ViewGroup container) {
			// NO-OP
		}

		/**
		 * Called when the host view is attempting to determine if an item's position
		 * has changed. Returns {@link #POSITION_UNCHANGED} if the position of the given
		 * item has not changed or {@link #POSITION_NONE} if the item is no longer present
		 * in the adapter.
		 *
		 * <p>The default implementation assumes that items will never
		 * change position and always returns {@link #POSITION_UNCHANGED}.
		 *
		 * @param object Object representing an item, previously returned by a call to
		 *               {@link #onCreateItem(ViewGroup, int, int)}.
		 * @return object's new pagePosition index from (~, ~),
		 * {@link #POSITION_UNCHANGED} if the object's position has not changed,
		 * or {@link #POSITION_NONE} if the item is no longer present.
		 */
		public int getItemPosition(@NonNull ViewGroup container, @NonNull Object object) {
			return POSITION_UNCHANGED;
		}

		/**
		 * This method may be called by the ViewPagerCompat to obtain a title string
		 * to describe the specified page. This method may return null
		 * indicating no title for this page. The default implementation returns
		 * null.
		 *
		 * @param position The position of the title requested
		 * @return A title for the requested page
		 */
		@Nullable
		public CharSequence getPageTitle(int position) {
			return null;
		}

		/**
		 * Returns the proportional width of a given page as a percentage of the
		 * ViewPagerCompat's measured width from (0.f-1.f]
		 *
		 * @param position The position of the page requested
		 * @return Proportional width for the given page position
		 */
		public float getPageWeight(int position) {
			return 1.f;
		}

		final void setViewPagerObserver(@Nullable DataSetObserver observer) {
			synchronized (this) {
				this.mViewPagerObserver = observer;
			}
		}
	}

	final class Page {
		Object object;
		int position;
		float offset;
		float weight;
		boolean scrolling;
	}

	public interface PageTransformer {
		/**
		 * Apply a property transformation to the given page.
		 *
		 * @param page     Apply the transformation to this page
		 * @param position Position of page relative to the current front-and-center
		 *                 position of the pager. 0 is front and center. 1 is one full
		 *                 page position to the right, and -1 is one page position to the left.
		 */
		void transformPage(@NonNull View page, float position);
	}

	public interface OnPageChangeListener {
		/**
		 * This method will be invoked when the current page is scrolled, either as part
		 * of a programmatically initiated smooth scroll or a user initiated touch scroll.
		 *
		 * @param position             Position index of the first page currently being displayed.
		 *                             Page position+1 will be visible if positionOffset is nonzero.
		 * @param positionOffset       Value from [0, 1) indicating the offset from the page at position.
		 * @param positionOffsetPixels Value in pixels indicating the offset from position.
		 */
		void onPageScrolled(int position, float positionOffset, @Px int positionOffsetPixels);

		/**
		 * This method will be invoked when a new page becomes selected. Animation is not
		 * necessarily complete.
		 *
		 * @param position Position index of the new selected page.
		 */
		void onPageSelected(int position);

		/**
		 * Called when the scroll state changes. Useful for discovering when the user
		 * begins dragging, when the pager is automatically settling to the current page,
		 * or when it is fully stopped/idle.
		 *
		 * @param state The new scroll state.
		 * @see ViewPagerCompat#SCROLL_STATE_IDLE
		 * @see ViewPagerCompat#SCROLL_STATE_DRAGGING
		 * @see ViewPagerCompat#SCROLL_STATE_SETTLING
		 */
		void onPageScrollStateChanged(int state);
	}

	public interface OnAdapterChangeListener {
		/**
		 * Called when the adapter for the given view pager has changed.
		 *
		 * @param container  ViewPagerCompat where the adapter change has happened
		 * @param oldAdapter the previously set adapter
		 * @param newAdapter the newly set adapter
		 */
		void onAdapterChanged(@NonNull ViewPagerCompat container,
							  @Nullable Adapter oldAdapter, @Nullable Adapter newAdapter);
	}

	public static abstract class OnSimplePageChangeListener implements OnPageChangeListener {

		/**
		 * This method will be invoked when the current page is scrolled, either as part
		 * of a programmatically initiated smooth scroll or a user initiated touch scroll.
		 *
		 * @param position             Position index of the first page currently being displayed.
		 *                             Page position+1 will be visible if positionOffset is nonzero.
		 * @param positionOffset       Value from [0, 1) indicating the offset from the page at position.
		 * @param positionOffsetPixels Value in pixels indicating the offset from position.
		 */
		@Override
		public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

		}

		/**
		 * This method will be invoked when a new page becomes selected. Animation is not
		 * necessarily complete.
		 *
		 * @param position Position index of the new selected page.
		 */
		@Override
		public void onPageSelected(int position) {

		}

		/**
		 * Called when the scroll state changes. Useful for discovering when the user
		 * begins dragging, when the pager is automatically settling to the current page,
		 * or when it is fully stopped/idle.
		 *
		 * @param state The new scroll state.
		 * @see ViewPagerCompat#SCROLL_STATE_IDLE
		 * @see ViewPagerCompat#SCROLL_STATE_DRAGGING
		 * @see ViewPagerCompat#SCROLL_STATE_SETTLING
		 */
		@Override
		public void onPageScrollStateChanged(int state) {

		}
	}

	public static class SavedState extends AbsSavedState {
		private int position;
		private int orientation;
		private boolean shouldScrollingLoop;
		private boolean shouldScrollingInAllowUser;
		private Bundle adapterState;
		private ClassLoader loader;

		SavedState(@NonNull Parcelable superState) {
			super(superState);
		}

		SavedState(@NonNull Parcel in, @Nullable ClassLoader loader) {
			super(in, loader);
			if (loader == null) {
				loader = getClass().getClassLoader();
			}
			this.loader = loader;
			this.position = in.readInt();
			this.orientation = in.readInt();
			this.shouldScrollingLoop = in.readInt() == 1;
			this.shouldScrollingInAllowUser = in.readInt() == 1;
			this.adapterState = in.readBundle(loader);
		}

		@Override
		public void writeToParcel(@NonNull Parcel out, int flags) {
			super.writeToParcel(out, flags);
			out.writeInt(this.position);
			out.writeInt(this.orientation);
			out.writeInt(this.shouldScrollingLoop ? 1 : 0);
			out.writeInt(this.shouldScrollingInAllowUser ? 1 : 0);
			out.writeBundle(this.adapterState);
		}

		@Override
		@NonNull
		public String toString() {
			return "FragmentPager.SavedState{"
					+ Integer.toHexString(System.identityHashCode(this))
					+ " position=" + position + "}";
		}

		public static final Creator<SavedState> CREATOR = new ClassLoaderCreator<SavedState>() {

			@Override
			public SavedState createFromParcel(Parcel in) {
				return new SavedState(in, null);
			}

			@Override
			public SavedState createFromParcel(Parcel in, ClassLoader loader) {
				return new SavedState(in, loader);
			}

			@Override
			public SavedState[] newArray(int size) {
				return new SavedState[size];
			}
		};
	}

	static class ViewPositionComparator implements Comparator<View> {

		@Override
		public int compare(View lhs, View rhs) {
			final LayoutParams llp = (LayoutParams) lhs.getLayoutParams();
			final LayoutParams rlp = (LayoutParams) rhs.getLayoutParams();
			if (llp.isDecor != rlp.isDecor) {
				return llp.isDecor ? 1 : -1;
			}
			return llp.position - rlp.position;
		}
	}

	static class PagePositionComparator implements Comparator<Page> {

		@Override
		public int compare(Page lhs, Page rhs) {
			return lhs.position - rhs.position;
		}
	}

	final class PagerObserver extends DataSetObserver {

		PagerObserver() {

		}

		@Override
		public void onChanged() {
			dataSetChanged();
		}

		@Override
		public void onInvalidated() {
			dataSetChanged();
		}
	}

	final class AccessibilityDelegate extends AccessibilityDelegateCompat {

		@Override
		public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
			super.onInitializeAccessibilityEvent(host, event);
			event.setClassName(ViewPagerCompat.class.getName());
			event.setScrollable(this.canScroll());
			if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED && mAdapter != null) {
				event.setItemCount(mAdapter.getItemCount());
				event.setFromIndex(mCurrentPosition);
				event.setToIndex(mCurrentPosition);
			}
		}

		@Override
		public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfoCompat info) {
			super.onInitializeAccessibilityNodeInfo(host, info);
			info.setClassName(ViewPagerCompat.class.getName());
			info.setScrollable(this.canScroll());
			if (canScrollHorizontally(1)) {
				info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
			}
			if (canScrollHorizontally(-1)) {
				info.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
			}
		}

		@Override
		public boolean performAccessibilityAction(View host, int action, Bundle args) {
			if (super.performAccessibilityAction(host, action, args)) {
				return true;
			}
			switch (action) {
				case AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD: {
					if (canScrollHorizontally(1)) {
						setCurrentItem(mCurrentPosition + 1);
						return true;
					}
				}
				return false;
				case AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD: {
					if (canScrollHorizontally(-1)) {
						setCurrentItem(mCurrentPosition - 1);
						return true;
					}
				}
				return false;
			}
			return false;
		}

		private boolean canScroll() {
			return (mAdapter != null) && (mAdapter.getItemCount() > 1);
		}
	}

	// NestedScrollingChild

	@Override
	public void setNestedScrollingEnabled(boolean enabled) {
		this.mScrollingChildHelper.setNestedScrollingEnabled(enabled);
	}

	@Override
	public boolean isNestedScrollingEnabled() {
		return this.mScrollingChildHelper.isNestedScrollingEnabled();
	}

	@Override
	public boolean startNestedScroll(int axes) {
		return this.mScrollingChildHelper.startNestedScroll(axes, ViewCompat.TYPE_TOUCH);
	}

	@Override
	public void stopNestedScroll() {
		this.mScrollingChildHelper.stopNestedScroll(ViewCompat.TYPE_TOUCH);
	}

	@Override
	public boolean hasNestedScrollingParent() {
		return this.mScrollingChildHelper.hasNestedScrollingParent(ViewCompat.TYPE_TOUCH);
	}

	@Override
	public boolean dispatchNestedPreScroll(int dx, int dy, int[] consumed, int[] offsetInWindow) {
		return this.mScrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, ViewCompat.TYPE_TOUCH);
	}

	@Override
	public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int[] offsetInWindow) {
		return this.mScrollingChildHelper.dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow);
	}

	@Override
	public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
		return this.mScrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY);
	}

	@Override
	public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
		return this.mScrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed);
	}
}
