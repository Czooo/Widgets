package com.demon;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.demon.widget.RefreshLayout;
import androidx.demon.widget.RefreshMode;
import androidx.demon.widget.ViewPagerCompat;
import androidx.demon.widget.adapter.PagerAdapterCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

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

		final ArrayList<String> data = new ArrayList<>();
		data.add("https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=845870984,388666921&fm=26&gp=0.jpg");
		data.add("http://img0.imgtn.bdimg.com/it/u=1732553485,3379133703&fm=26&gp=0.jpg");
		data.add("https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=4012045560,650010815&fm=26&gp=0.jpg");
		data.add("https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=2771670718,331933807&fm=26&gp=0.jpg");
		final PagerAdapterCompat<PagerAdapterCompat.ViewHolder> adapter =
				new PagerAdapterCompat<PagerAdapterCompat.ViewHolder>() {
					@NonNull
					@Override
					public ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, int itemViewType) {
						return new ViewHolder(inflater.inflate(R.layout.item_banner_layout, container, false)) {
						};
					}

					@Override
					@SuppressLint("CheckResult")
					public void onBindViewHolder(@NonNull ViewHolder holder, int position, @Nullable Object object) {
						final AppCompatImageView imageView = holder.getItemView().findViewById(R.id.icon);
						RequestOptions options = new RequestOptions();
						options.error(new ColorDrawable(Color.BLACK));

						Glide.with(imageView)
								.load(data.get(position))
								.apply(options)
								.into(imageView);
					}

					@Override
					public int getItemCount() {
						return data.size();
					}
				};
		final ViewPagerCompat viewPagerCompat = this.findViewById(R.id.viewPagerCompat);
		viewPagerCompat.addOnPageChangeListener(new ViewPagerCompat.SimpleOnPageChangeListener() {
			@Override
			public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
				Log.e("NestedLayoutActivity", "onPageScrolled " + position + " positionOffset " + positionOffset + " positionOffsetPixels " + positionOffsetPixels);
			}

			@Override
			public void onPageSelected(int position) {
				Log.e("NestedLayoutActivity", "onPageSelected " + position);
			}

			@Override
			public void onPageScrollStateChanged(int state) {
				Log.e("NestedLayoutActivity", "onPageScrollStateChanged " + state);
			}
		});
		viewPagerCompat.setScrollingLoop(false);
		viewPagerCompat.setAdapter(adapter);

		final int px = 24;
//		viewPagerCompat.setPadding(px, 0, px, 0);
//		viewPagerCompat.setClipToPadding(false);
		viewPagerCompat.setPageMargin(px);

		final PagerAdapter adapter2 = new PagerAdapter() {
			@NonNull
			@Override
			@SuppressLint("CheckResult")
			public View instantiateItem(@NonNull ViewGroup container, int position) {
				View contentView = LayoutInflater.from(container.getContext())
						.inflate(R.layout.item_banner_layout, container, false);
				container.addView(contentView);

				final AppCompatImageView imageView = contentView.findViewById(R.id.icon);
				RequestOptions options = new RequestOptions();
				options.error(new ColorDrawable(Color.BLACK));

				Glide.with(imageView)
						.load(data.get(position))
						.apply(options)
						.into(imageView);
				return contentView;
			}

			@Override
			public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
				container.removeView(((View) object));
			}

			@Override
			public int getCount() {
				return data.size();
			}

			@Override
			public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
				return object==view;
			}
		};
		final ViewPager viewPager = this.findViewById(R.id.viewPager2);
//		viewPager.setPadding(px, 0, px, 0);
//		viewPager.setClipToPadding(false);
		viewPager.setAdapter(adapter2);
		viewPager.setPageMargin(px);
	}
}
