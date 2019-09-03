package com.demon;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.demon.widget.RefreshLayout;
import androidx.demon.widget.RefreshMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Author create by ok on 2019-07-20
 * Email : ok@163.com.
 */
public class NestedRefreshLayoutActivity extends AppCompatActivity {

	final int[] colors = new int[]{
			android.R.color.holo_green_light,
			android.R.color.holo_orange_dark,
			android.R.color.holo_blue_dark,
			android.R.color.holo_green_dark,
	};

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_widget_nested_refresh_layout);

		final RefreshLayout mParentRefreshLayout = this.findViewById(R.id.parentRefreshLayout);
		mParentRefreshLayout.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {
			@Override
			public void onRefreshing(@NonNull RefreshLayout refreshLayout, @NonNull RefreshMode mode) {
				refreshLayout.postDelayed(new Runnable() {
					@Override
					public void run() {
						mParentRefreshLayout.setRefreshing(false, 700);
					}
				}, 2000);
			}
		});

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

		final RecyclerView.Adapter<RecyclerView.ViewHolder> mAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

			@NonNull
			@Override
			public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
				final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
				final View view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
				RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(-1, -1);
				view.setLayoutParams(layoutParams);
				return new RecyclerView.ViewHolder(view) {
				};
			}

			@Override
			public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
				holder.itemView.setBackgroundResource(colors[position % colors.length]);
			}

			@Override
			public int getItemCount() {
				return 20;
			}
		};

		final RecyclerView mRecyclerView = this.findViewById(R.id.recyclerView);
		mRecyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.HORIZONTAL, false));
		mRecyclerView.setAdapter(mAdapter);
	}
}
