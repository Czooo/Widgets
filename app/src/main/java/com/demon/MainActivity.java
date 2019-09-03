package com.demon;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.demon.widget.TripFooterLoadView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.demon.widget.BannerLayout;
import androidx.demon.widget.IndicatorView;
import androidx.demon.widget.RefreshLayout;
import androidx.demon.widget.RefreshMode;
import androidx.demon.widget.adapter.PagerAdapter;
import androidx.demon.widget.transformers.HorDepthPageTransformer;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_main);

		final RefreshLayout mRefreshLayout = this.findViewById(R.id.refreshLayout);
		mRefreshLayout.setFooterLoadView(new TripFooterLoadView());
		mRefreshLayout.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {

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

		final ArrayList<String> data = new ArrayList<>();
		data.add("http://img0.imgtn.bdimg.com/it/u=3106526341,3733396167&fm=26&gp=0.jpg");
		data.add("http://img1.imgtn.bdimg.com/it/u=795421968,2817681607&fm=11&gp=0.jpg");
		data.add("http://img0.imgtn.bdimg.com/it/u=1732553485,3379133703&fm=26&gp=0.jpg");
		data.add("http://img0.imgtn.bdimg.com/it/u=3043400348,2388911000&fm=15&gp=0.jpg");
		final PagerAdapter<PagerAdapter.ViewHolder> mAdapter = new PagerAdapter<PagerAdapter.ViewHolder>() {

			@Override
			public int getItemCount() {
				return data.size();
			}

			@NonNull
			@Override
			public ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, int itemViewType) {
				final View itemView = inflater.inflate(R.layout.item_banner_layout, container, false);
				return new ViewHolder(itemView) {
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
		};
		mAdapter.setOnItemClickListener(new PagerAdapter.OnItemClickListener() {

			@Override
			public void onItemClick(@NonNull ViewGroup container, @NonNull View view, int position) {
				Toast.makeText(container.getContext(), "Pos " + position, Toast.LENGTH_LONG).show();
			}
		});

		final IndicatorView mIndicatorView = this.findViewById(R.id.indicatorView);
		final BannerLayout mBannerLayout = this.findViewById(R.id.bannerLayout);
		// 滚动方向
		mBannerLayout.setScrollDirection(BannerLayout.SCROLL_DIRECTION_START);
		// 滚动动画
		mBannerLayout.setPageTransformer(new HorDepthPageTransformer());
		// 指示器
		mBannerLayout.setIndicator(mIndicatorView);
		// 用户手势操作
		mBannerLayout.setAllowUserScrollable(true);
		// 播放间隔时间：毫秒
		mBannerLayout.setAutoPlayDelayMillis(2500);
		// 控件滚动间隔时间：毫秒
		mBannerLayout.setScrollingDuration(600);
		// 自动管理生命周期
		mBannerLayout.setLifecycleOwner(this);
		// 自动循环播放
		mBannerLayout.setAutoPlaying(true);
		// 资源适配器
		mBannerLayout.setAdapter(mAdapter);
		// 开始循环播放
		mBannerLayout.startPlay();

		this.findViewById(R.id.viewPagerCompat)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainActivity.this, ViewPagerCompatActivity.class));
					}
				});
		this.findViewById(R.id.fragmentPagerAdapter)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainActivity.this, FragmentPagerAdapterActivity.class));
					}
				});
		this.findViewById(R.id.joinRefreshLayout)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainActivity.this, RefreshLayoutActivity.class));
					}
				});
		this.findViewById(R.id.joinNestedRefreshLayout)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainActivity.this, NestedRefreshLayoutActivity.class));
					}
				});
		this.findViewById(R.id.joinNineGridView)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainActivity.this, NineGridViewActivity.class));
					}
				});
		this.findViewById(R.id.joinSwipeSideLayout)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainActivity.this, SwipeSideLayoutActivity.class));
					}
				});
		this.findViewById(R.id.joinFlowLabelView)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainActivity.this, FlowLabelViewActivity.class));
					}
				});
		this.findViewById(R.id.stickLayoutManager)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainActivity.this, StickLayoutManagerActivity.class));
					}
				});
	}
}
