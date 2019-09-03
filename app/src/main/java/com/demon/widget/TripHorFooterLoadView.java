package com.demon.widget;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.demon.widget.DefaultHorFooterLoadView;
import androidx.demon.widget.RefreshLayout;

/**
 * Author create by ok on 2019-06-28
 * Email : ok@163.com.
 */
public class TripHorFooterLoadView extends DefaultHorFooterLoadView {

	private boolean mIsRefreshedTrip = true;

	@Override
	public void onViewCreated(@NonNull RefreshLayout refreshLayout, @NonNull View container) {
		super.onViewCreated(refreshLayout, container);
		if (this.mIsRefreshedTrip) {
			((TextView) this.getContentView().findViewById(androidx.demon.widget.R.id.app_refresh_load_text_view)).setText("没有更多");
		}
	}

	@Override
	public int onGetScrollDistance(@NonNull RefreshLayout refreshLayout, @NonNull View container) {
		if (this.mIsRefreshedTrip) {
			return 0;
		}
		return super.onGetScrollDistance(refreshLayout, container);
	}

	@Override
	public void onRefreshPull(@NonNull RefreshLayout refreshLayout, int scrollOffset, float offset) {
		if (this.mIsRefreshedTrip) {
			((TextView) this.getContentView().findViewById(androidx.demon.widget.R.id.app_refresh_load_text_view)).setText("没有更多");
			return;
		}
		super.onRefreshPull(refreshLayout, scrollOffset, offset);
	}
}
