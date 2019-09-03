package androidx.demon.widget;

import android.content.Context;
import android.database.DataSetObservable;
import android.database.DataSetObserver;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.demon.widget.cacher.RecycledPool;

/**
 * Author create by ok on 2019-07-18
 * Email : ok@163.com.
 */
public class NineGridView extends ViewGroup {

	private static final boolean DEBUG = false;
	private static final String TAG = "NineGridView";

	private static final int DEFAULT_GRID_GAP = 10;

	private int mGridGap = DEFAULT_GRID_GAP;

	public NineGridView(Context context) {
		super(context);
	}

	public NineGridView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public NineGridView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
		final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

		final int paddingLeft = this.getPaddingLeft();
		final int paddingTop = this.getPaddingTop();
		final int paddingRight = this.getPaddingRight();
		final int paddingBottom = this.getPaddingBottom();

		final int width = widthSize - paddingLeft - paddingRight;
		final int height = heightSize - paddingTop - paddingBottom;

		final int childCount = this.getChildCount();
		final int[] ranks = this.calculateRowAndCol();
		final int[] itemViewSize = this.getItemViewSize(width, height);

		int realClientHeight = paddingTop;
		for (int index = 0; index < childCount; index++) {
			final View preChildView = this.getChildAt(index);
			if (preChildView.getVisibility() == View.GONE) {
				continue;
			}
			preChildView.measure(MeasureSpec.makeMeasureSpec(itemViewSize[0], MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(itemViewSize[1], MeasureSpec.EXACTLY));

			if (index == 0 || (index % ranks[1] + 1 >= ranks[1] && index < childCount - 1)) {
				realClientHeight += preChildView.getMeasuredHeight() + this.mGridGap;
			}
		}
		realClientHeight += paddingBottom;
		this.setMeasuredDimension(widthSize, realClientHeight);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		final int paddingLeft = this.getPaddingLeft();
		final int paddingTop = this.getPaddingTop();
		final int[] ranks = this.calculateRowAndCol();

		int preTop = paddingTop;
		int preLeft = paddingLeft;

		for (int index = 0; index < this.getChildCount(); index++) {
			final View preChildView = this.getChildAt(index);
			if (preChildView.getVisibility() == View.GONE) {
				continue;
			}
			final int childWidth = preChildView.getMeasuredWidth();
			final int childHeight = preChildView.getMeasuredHeight();
			preChildView.layout(preLeft, preTop, preLeft + childWidth, preTop + childHeight);

			if (index % ranks[1] + 1 >= ranks[1]) {
				preTop += childHeight + this.mGridGap;
				preLeft = paddingLeft;
			} else {
				preLeft += childWidth + this.mGridGap;
			}
		}
	}

	@Override
	public void addView(View child, int index, ViewGroup.LayoutParams params) {
		throw new IllegalStateException("prohibit registration of new Views");
	}

	@NonNull
	private int[] getItemViewSize(int width, int height) {
		final int[] itemViewSize = new int[2];
		final int[] ranks = this.calculateRowAndCol();
		final int realWidth = width - (ranks[1] - 1) * this.mGridGap;
		final int size = realWidth / ranks[1];
		itemViewSize[0] = size;
		itemViewSize[1] = size;
		if (this.mAdapter != null && this.mAdapter.getItemCount() == 1) {
			this.mAdapter.getSingleItemViewSize(this, itemViewSize);
		}
		itemViewSize[0] = Math.max(size, Math.min(itemViewSize[0], width));
		itemViewSize[1] = Math.max(size, Math.min(itemViewSize[1], width));
		return itemViewSize;
	}

	@NonNull
	private int[] calculateRowAndCol() {
		final int[] ranks = new int[2];
		ranks[0] = 3;
		ranks[1] = 3;
		return ranks;
	}

	public void setGridGap(int gridGap) {
		this.mGridGap = gridGap;
	}

	private OnItemClickListener mOnItemClickListener;
	private OnItemLongClickListener mOnItemLongClickListener;

	public void setOnItemClickListener(@NonNull OnItemClickListener listener) {
		this.mOnItemClickListener = listener;
	}

	public void setOnItemLongClickListener(@NonNull OnItemLongClickListener listener) {
		this.mOnItemLongClickListener = listener;
	}

	private Adapter mAdapter;
	private ViewObserver mObserver;

	public <VH extends ViewHolder> void setAdapter(@Nullable Adapter<VH> adapter) {
		this.setAdapter(adapter, false);
	}

	/**
	 * 注意优化项:
	 * 场景(非列表中嵌套使用时)不同布局类型时，建议全部释放
	 * 场景(列表中嵌套使用时)不同布局类型时，可以考虑回收使用oldAdapter的ViewHolders
	 *
	 * @param recycledOldViewHolders true : 回收利用oldAdapter的ViewHolder / false : oldAdapter的ViewHolder全部释放.
	 */
	public <VH extends ViewHolder> void setAdapter(@Nullable Adapter<VH> adapter, boolean recycledOldViewHolders) {
		if (this.mAdapter == adapter) {
			if (this.mAdapter != null) {
				this.mAdapter.notifyDataSetChanged();
			}
			return;
		}
		if (this.mAdapter != null) {
			this.mAdapter.setViewPagerObserver(null);
			this.mAdapter.onDetachedFromWindow(this);
			this.performRecycledViewHolder(recycledOldViewHolders);
		}
		this.mAdapter = adapter;
		if (this.mAdapter != null) {
			if (this.mObserver == null) {
				this.mObserver = new ViewObserver();
			}
			this.mAdapter.setViewPagerObserver(this.mObserver);
			this.mAdapter.onAttachedToWindow(this);
			this.mAdapter.notifyDataSetChanged();
		}
	}

	@Nullable
	public Adapter getAdapter() {
		return this.mAdapter;
	}

	public RecycledPool<ViewHolder> getRecycledPool() {
		return this.mRecycledPool;
	}

	private final RecycledPool<ViewHolder> mRecycledPool = new RecycledPool<>(9);
	private final ArrayList<ViewHolder> mViewHolders = new ArrayList<>();

	private void dataSetChanged() {
		this.performRecycledViewHolder(true);
		if (this.mAdapter == null || this.mAdapter.getItemCount() == 0) {
			return;
		}
		for (int position = 0; position < Math.min(this.mAdapter.getItemCount(), 9); position++) {
			final ViewHolder holder = this.performCreateViewHolder(position);
			holder.itemView.setOnClickListener(new OnViewHolderClickListener(holder));
			holder.itemView.setOnLongClickListener(new OnViewHoldeLongrClickListener(holder));

			final LayoutParams mLayoutParams = holder.itemView.getLayoutParams();
			LayoutParams preLayoutParams;
			if (mLayoutParams == null) {
				preLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			} else if (!this.checkLayoutParams(mLayoutParams)) {
				preLayoutParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			} else {
				preLayoutParams = mLayoutParams;
			}
			super.addView(holder.itemView, position, preLayoutParams);
			this.mViewHolders.add(holder);
			this.mAdapter.onBindViewHolder(holder, position, null);
		}
	}

	private ViewHolder performCreateViewHolder(int position) {
		final int itemViewType = this.mAdapter.getItemViewType(position);
		ViewHolder holder = this.mRecycledPool.getRecycled(itemViewType);
		if (holder == null) {
			holder = this.mAdapter.onCreateViewHolder(LayoutInflater.from(this.getContext()), this, itemViewType);
		}
		if (holder.itemView.getParent() != null) {
			throw new IllegalStateException("ViewHolder views must not be attached when"
					+ " created. Ensure that you are not passing 'true' to the attachToRoot"
					+ " parameter of LayoutInflater.inflate(..., boolean attachToRoot)");
		}
		holder.position = position;
		holder.itemViewType = itemViewType;
		return holder;
	}

	private void performRecycledViewHolder(boolean recycled) {
		for (int index = 0; index < this.getChildCount(); index++) {
			final View preChildView = this.getChildAt(index);
			final ViewHolder holder = this.getViewHolderForChild(preChildView);
			if (holder == null) {
				continue;
			}
			if (this.performRecycledForViewHolder(holder)) {
				index--;
			}
		}
		if (DEBUG) {
			Log.i(TAG, "RecycledPool : " + this.mRecycledPool.toString());
		}
		if (!recycled) {
			this.mRecycledPool.clear();
			this.mViewHolders.clear();
			this.removeAllViews();
		}
	}

	private boolean performRecycledForViewHolder(@NonNull ViewHolder holder) {
		if (this.mViewHolders.remove(holder)) {
			holder.resetInternal();
			this.removeView(holder.itemView);
			this.mRecycledPool.putRecycled(holder.itemViewType, holder);
			return true;
		}
		return false;
	}

	@Nullable
	private ViewHolder getViewHolderForChild(@NonNull View child) {
		for (int index = 0; index < this.mViewHolders.size(); index++) {
			final ViewHolder holder = this.mViewHolders.get(index);
			if (holder.itemView == child) {
				return holder;
			}
		}
		return null;
	}

	final class OnViewHolderClickListener implements View.OnClickListener {

		final ViewHolder mViewHolder;

		OnViewHolderClickListener(@NonNull ViewHolder holder) {
			this.mViewHolder = holder;
		}

		@Override
		public void onClick(View view) {
			if (mOnItemClickListener != null) {
				mOnItemClickListener.onItemClick(NineGridView.this, view, this.mViewHolder.position);
			}
		}
	}

	final class OnViewHoldeLongrClickListener implements View.OnLongClickListener {

		final ViewHolder mViewHolder;

		OnViewHoldeLongrClickListener(@NonNull ViewHolder holder) {
			this.mViewHolder = holder;
		}

		@Override
		public boolean onLongClick(View view) {
			if (mOnItemLongClickListener != null) {
				return mOnItemLongClickListener.onItemLongClick(NineGridView.this, view, this.mViewHolder.position);
			}
			return false;
		}
	}

	final class ViewObserver extends DataSetObserver {

		@Override
		public void onChanged() {
			dataSetChanged();
		}

		@Override
		public void onInvalidated() {
			dataSetChanged();
		}
	}

	public static abstract class Adapter<VH extends ViewHolder> {

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

		@NonNull
		public abstract VH onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, int itemViewType);

		public abstract void onBindViewHolder(@NonNull VH holder, int position, @Nullable Object object);

		public abstract int getItemCount();

		public int getItemViewType(int position) {
			return 0;
		}

		public void getSingleItemViewSize(@NonNull ViewGroup container, @NonNull int[] itemViewSize) {
			// NO-OP
		}

		final void setViewPagerObserver(@Nullable DataSetObserver observer) {
			synchronized (this) {
				this.mViewPagerObserver = observer;
			}
		}
	}

	public static abstract class ViewHolder {

		private static final int NO_POSITION = -1;
		private static final int INVALID_TYPE = -1;

		@NonNull
		private final View itemView;

		private int position = NO_POSITION;
		private int itemViewType = INVALID_TYPE;

		public ViewHolder(@NonNull View itemView) {
			this.itemView = itemView;
		}

		@NonNull
		public final View getItemView() {
			return this.itemView;
		}

		public final int getPosition() {
			return this.position;
		}

		public final int getItemViewType() {
			return this.itemViewType;
		}

		final void resetInternal() {
			this.position = NO_POSITION;
		}
	}

	public interface OnItemClickListener {

		void onItemClick(@NonNull ViewGroup container, @NonNull View view, int position);
	}

	public interface OnItemLongClickListener {

		boolean onItemLongClick(@NonNull ViewGroup container, @NonNull View view, int position);
	}
}
