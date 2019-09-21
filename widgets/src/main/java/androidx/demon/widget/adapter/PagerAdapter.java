package androidx.demon.widget.adapter;

import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.os.TraceCompat;
import androidx.demon.widget.ViewPagerCompat;
import androidx.demon.widget.cache.RecycledPool;

/**
 * Author create by ok on 2019-07-15
 * Email : ok@163.com.
 */
public abstract class PagerAdapter<VH extends PagerAdapter.ViewHolder> extends ViewPagerCompat.Adapter {

	private final RecycledPool<VH> mRecycledPool = new RecycledPool<>();
	private final SparseArray<VH> mViewHolderPool = new SparseArray<>();
	private OnItemClickListener mOnItemClickListener;
	private OnItemLongClickListener mOnItemLongClickListener;

	@CallSuper
	@Override
	public void onAttachedToWindow(@NonNull ViewGroup container) {
		super.onAttachedToWindow(container);
		final OnAdapterChangeListener mOnAdapterChangeListener = new OnAdapterChangeListener();
		final ViewPagerCompat mViewPagerCompat = (ViewPagerCompat) container;
		mViewPagerCompat.addOnAdapterChangeListener(mOnAdapterChangeListener);
	}

	@NonNull
	@Override
	public final Object onCreateItem(@NonNull ViewGroup container, int position, int pagePosition) {
		try {
			TraceCompat.beginSection("dispatchCreateViewHolder");
			final int itemViewType = this.getItemViewType(position);
			ViewHolder holder = this.mRecycledPool.getRecycled(itemViewType);
			if (holder == null) {
				holder = this.onCreateViewHolder(LayoutInflater.from(container.getContext()), container, itemViewType);
			}
			if (holder.itemView.getParent() != null) {
				throw new IllegalStateException("ViewHolder views must not be attached when"
						+ " created. Ensure that you are not passing 'true' to the attachToRoot"
						+ " parameter of LayoutInflater.inflate(..., boolean attachToRoot)");
			}
			holder.position = position;
			holder.itemViewType = itemViewType;
			container.addView(holder.getItemView());

			final VH tempViewHolder = (VH) holder;
			this.onViewAttachedToWindow(container, tempViewHolder);
			this.onBindViewHolder(tempViewHolder, position, null);
			this.mViewHolderPool.put(position, tempViewHolder);
			return holder;
		} finally {
			TraceCompat.endSection();
		}
	}

	@Override
	public final void onDestroyItem(@NonNull ViewGroup container, @NonNull Object object, int position) {
		final VH holder = (VH) object;
		container.removeView(holder.getItemView());
		this.onViewDetachedFromWindow(container, holder);
		this.mRecycledPool.putRecycled(holder.getItemViewType(), holder);
		this.mViewHolderPool.remove(position);
	}

	@Override
	public final boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
		final VH holder = (VH) object;
		return view == holder.getItemView();
	}

	@Override
	public final void onPrimaryItem(@NonNull ViewGroup container, @NonNull Object object, int position) {
		super.onPrimaryItem(container, object, position);
		this.onPrimaryViewHolder(container, (VH) object);
	}

	@Override
	public int getItemPosition(@NonNull ViewGroup container, @NonNull Object object) {
		return ViewPagerCompat.Adapter.POSITION_NONE;
	}

	@NonNull
	public abstract VH onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, int itemViewType);

	public abstract void onBindViewHolder(@NonNull VH holder, int position, @Nullable Object object);

	public int getItemViewType(int position) {
		// NO-OP
		return 0;
	}

	@CallSuper
	public void onViewAttachedToWindow(@NonNull final ViewGroup container, @NonNull final VH holder) {
		if (this.mOnItemClickListener != null) {
			holder.getItemView().setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					mOnItemClickListener.onItemClick(container, view, holder.getPosition());
				}
			});
		}
		if (this.mOnItemLongClickListener != null) {
			holder.getItemView().setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View view) {
					return mOnItemLongClickListener.onItemLongClick(container, view, holder.getPosition());
				}
			});
		}
	}

	@CallSuper
	public void onViewDetachedFromWindow(@NonNull ViewGroup container, @NonNull VH holder) {
		holder.resetInternal();
	}

	@CallSuper
	public void onPrimaryViewHolder(@NonNull ViewGroup container, @NonNull VH holder) {
		// NO-OP
	}

	public final VH findViewHolderByPoition(int position) {
		return this.mViewHolderPool.get(position);
	}

	public final RecycledPool<VH> getRecycledPool() {
		return this.mRecycledPool;
	}

	public void setOnItemClickListener(@NonNull OnItemClickListener listener) {
		this.mOnItemClickListener = listener;
	}

	public void setOnItemLongClickListener(@NonNull OnItemLongClickListener listener) {
		this.mOnItemLongClickListener = listener;
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

	final class OnAdapterChangeListener implements ViewPagerCompat.OnAdapterChangeListener {

		/**
		 * Called when the adapter for the given view pager has changed.
		 *
		 * @param container  ViewPagerCompat where the adapter change has happened
		 * @param oldAdapter the previously set adapter
		 * @param newAdapter the newly set adapter
		 */
		@Override
		public void onAdapterChanged(@NonNull ViewPagerCompat container, @Nullable ViewPagerCompat.Adapter oldAdapter, @Nullable ViewPagerCompat.Adapter newAdapter) {
			if (oldAdapter != null) {
				PagerAdapter.this.mRecycledPool.clear();
			}
		}
	}

	public interface OnItemClickListener {

		void onItemClick(@NonNull ViewGroup container, @NonNull View view, int position);
	}

	public interface OnItemLongClickListener {

		boolean onItemLongClick(@NonNull ViewGroup container, @NonNull View view, int position);
	}
}
