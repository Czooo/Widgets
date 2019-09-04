package com.demon.widget;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.demon.widget.DefaultFooterLoadView;
import androidx.demon.widget.RefreshLayout;

/**
 * Author create by ok on 2019-06-28
 * Email : ok@163.com.
 */
public class TripFooterLoadView extends DefaultFooterLoadView {

	private boolean mIsRefreshedTrip = true;

	@Override
	public void onViewCreated(@NonNull RefreshLayout refreshLayout, @NonNull View container) {
		super.onViewCreated(refreshLayout, container);
		if (this.mIsRefreshedTrip) {
			((TextView) this.getContentView().findViewById(androidx.demon.widget.R.id.app_refresh_load_text_view)).setText("没有更多");
		}
	}

	@Override
	public void onRefreshPull(int scrollOffset, float offset) {
		if (this.mIsRefreshedTrip) {
			((TextView) this.getContentView().findViewById(androidx.demon.widget.R.id.app_refresh_load_text_view)).setText("没有更多");
			return;
		}
		super.onRefreshPull(scrollOffset, offset);
	}

	@Override
	public int onGetScrollDistance() {
		if (this.mIsRefreshedTrip) {
			return 0;
		}
		return super.onGetScrollDistance();
	}
}
