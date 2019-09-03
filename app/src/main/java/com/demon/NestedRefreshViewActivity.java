package com.demon;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.demon.widget.RefreshLayout;
import androidx.demon.widget.RefreshMode;

/**
 * Author create by ok on 2019-07-01
 * Email : ok@163.com.
 */
public class NestedRefreshViewActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_nested);

		final RefreshLayout mParentRefreshView = findViewById(R.id.parentRefreshView);
		mParentRefreshView.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {
			@Override
			public void onRefreshing(@NonNull final RefreshLayout refreshLayout, @NonNull RefreshMode mode) {
				refreshLayout.postDelayed(new Runnable() {
					@Override
					public void run() {
						refreshLayout.setRefreshing(false, 700);
					}
				}, 2000);
			}
		});
	}
}
