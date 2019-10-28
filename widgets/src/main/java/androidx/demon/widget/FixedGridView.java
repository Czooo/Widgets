package androidx.demon.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.demon.widget.adapter.FixedGridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @Author create by Zoran on 2019-10-25
 * @Email : 171905184@qq.com
 * @Description :
 */
public class FixedGridView extends RecyclerView {

	private final FixedGridLayoutManager mLayoutManager =
			new FixedGridLayoutManager();

	public FixedGridView(@NonNull Context context) {
		this(context, null);
	}

	public FixedGridView(@NonNull Context context, @Nullable AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public FixedGridView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.setLayoutManager(this.mLayoutManager);
	}

	@Override
	public void setLayoutManager(@Nullable LayoutManager layoutManager) {
		if (layoutManager instanceof FixedGridLayoutManager) {
			super.setLayoutManager(layoutManager);
			return;
		}
		throw new IllegalStateException("layoutManager " + layoutManager + " different types");
	}

	@NonNull
	@Override
	public FixedGridLayoutManager getLayoutManager() {
		return this.mLayoutManager;
	}

	public void setSpanCount(int spanCount) {
		this.getLayoutManager().setSpanCount(spanCount);
	}

	public void setSpanSizeLookup(FixedGridLayoutManager.SpanSizeLookup spanSizeLookup) {
		this.getLayoutManager().setSpanSizeLookup(spanSizeLookup);
	}
}
