package androidx.demon.widget.managers;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Author create by ok on 2019-07-19
 * Email : ok@163.com.
 */
public class PagerLayoutManager extends RecyclerView.LayoutManager {

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
		return new RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.MATCH_PARENT);
	}

	@Override
	public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
		if(state.getItemCount() == 0) {
			this.removeAndRecycleAllViews(recycler);
			return;
		}
		this.detachAndScrapAttachedViews(recycler);

		this.populate();
	}

	private int populate() {

		return 0;
	}

	@Override
	public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
		return super.scrollVerticallyBy(dy, recycler, state);
	}
}
