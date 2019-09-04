package androidx.demon.widget;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * Author create by ok on 2019-06-28
 * Email : ok@163.com.
 */
public class DefaultFooterLoadView extends RefreshLayout.SimpleLoadView {

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
		return inflater.inflate(R.layout.layout_footer_load_default, container);
	}

	@Override
	public void onViewCreated(@NonNull RefreshLayout refreshLayout, @NonNull View container) {
		super.onViewCreated(refreshLayout, container);
		((TextView) this.getContentView().findViewById(R.id.app_refresh_load_text_view)).setText("等待加载");
	}

	@Override
	public void onRefreshPull(int scrollOffset, float offset) {
		if (offset >= 1.f) {
			((TextView) this.getContentView().findViewById(R.id.app_refresh_load_text_view)).setText("释放加载");
		} else {
			((TextView) this.getContentView().findViewById(R.id.app_refresh_load_text_view)).setText("加载更多");
		}
	}

	@Override
	public void onRefreshing() {
		((TextView) this.getContentView().findViewById(R.id.app_refresh_load_text_view)).setText("加载中");
	}

	@Override
	public void onRefreshed() {
		((TextView) this.getContentView().findViewById(R.id.app_refresh_load_text_view)).setText("加载完成");
	}
}
