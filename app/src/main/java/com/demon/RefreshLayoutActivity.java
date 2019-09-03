package com.demon;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.demon.widget.RefreshLayout;
import androidx.demon.widget.RefreshMode;

/**
 * Author create by ok on 2019-07-20
 * Email : ok@163.com.
 */
public class RefreshLayoutActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_widget_refresh_layout);

		final RefreshLayout mRefreshLayout = this.findViewById(R.id.refreshLayout);
		mRefreshLayout.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {
			@Override
			public void onRefreshing(@NonNull RefreshLayout refreshLayout, @NonNull RefreshMode mode) {
				refreshLayout.postDelayed(new Runnable() {
					@Override
					public void run() {
						mRefreshLayout.setRefreshing(false, 700);
					}
				}, 2000);
			}
		});
		View view = mRefreshLayout.findViewById(R.id.app_refresh_view_id);
		view.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Toast.makeText(view.getContext(), "Click", Toast.LENGTH_LONG).show();
			}
		});
	}
}
