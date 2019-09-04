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
public class DefaultHeaderLoadView extends RefreshLayout.SimpleLoadView {

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @NonNull ViewGroup container) {
		return inflater.inflate(R.layout.layout_header_load_default, container);
	}

	@Override
	public void onViewCreated(@NonNull RefreshLayout refreshLayout, @NonNull View container) {
		super.onViewCreated(refreshLayout, container);
		((TextView) this.getContentView().findViewById(R.id.app_refresh_load_text_view)).setText("等待刷新");
	}

	@Override
	public void onRefreshPull(int scrollOffset, float offset) {
		if (offset >= 1.f) {
			((TextView) this.getContentView().findViewById(R.id.app_refresh_load_text_view)).setText("释放刷新");
		} else {
			((TextView) this.getContentView().findViewById(R.id.app_refresh_load_text_view)).setText("下拉刷新");
		}
	}

	@Override
	public void onRefreshing() {
		((TextView)this.getContentView().findViewById(R.id.app_refresh_load_text_view)).setText("刷新中");
	}

	@Override
	public void onRefreshed() {
		((TextView)this.getContentView().findViewById(R.id.app_refresh_load_text_view)).setText("刷新完成");
	}
}
