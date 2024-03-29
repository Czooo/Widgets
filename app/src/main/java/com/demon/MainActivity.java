package com.demon;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
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
import androidx.demon.widget.RefreshLayout;
import androidx.demon.widget.RefreshMode;
import androidx.demon.widget.ViewPagerCompat;
import androidx.demon.widget.adapter.PagerAdapterCompat;
import androidx.demon.widget.helper.NestedScrollingHelper;
import androidx.demon.widget.transformers.HorDepthPageTransformer;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_main);

		final RefreshLayout mRefreshLayout = this.findViewById(R.id.refreshLayout);
		mRefreshLayout.setFooterLoadView(new TripFooterLoadView());
		mRefreshLayout.addOnScrollListener(new RefreshLayout.OnScrollListener() {
			@Override
			public void onScrolled(@NonNull ViewGroup container, int dx, int dy) {
				final RefreshLayout refreshLayout = (RefreshLayout) container;
				final NestedScrollingHelper helper = refreshLayout.getNestedScrollingHelper();
				final int scrollOffsetY = helper.getScrollOffsetY();
				final int preScrollOffsetY = scrollOffsetY + dy;

				final ViewGroup headerParent = refreshLayout.getHeaderParent();
				Log.e("Main", "onScrolled " + preScrollOffsetY + " == " + (int) (preScrollOffsetY * refreshLayout.getFrictionRatio()) + " == " + headerParent.getMeasuredHeight());
			}

			@Override
			public void onScrollStateChanged(@NonNull ViewGroup container, int scrollState) {

			}
		});
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
		mRefreshLayout.setRefreshing(true);

		this.findViewById(R.id.headerFloatingView).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				mRefreshLayout.setRefreshing(true);
			}
		});

		final ArrayList<String> data = new ArrayList<>();
		data.add("https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=845870984,388666921&fm=26&gp=0.jpg");
		data.add("http://img0.imgtn.bdimg.com/it/u=1732553485,3379133703&fm=26&gp=0.jpg");
		data.add("https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=4012045560,650010815&fm=26&gp=0.jpg");
		data.add("https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=2771670718,331933807&fm=26&gp=0.jpg");
		final PagerAdapterCompat<PagerAdapterCompat.ViewHolder> mAdapter = new PagerAdapterCompat<PagerAdapterCompat.ViewHolder>() {
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

			@Override
			public int getItemViewType(int position) {
				return super.getItemViewType(position);
			}
		};
		mAdapter.setOnItemClickListener(new PagerAdapterCompat.OnItemClickListener() {
			@Override
			public void onItemClick(@NonNull ViewGroup container, @NonNull View view, int position) {
				Toast.makeText(container.getContext(), "Pos " + position, Toast.LENGTH_LONG).show();
			}
		});

		final BannerLayout mBannerLayout = this.findViewById(R.id.bannerLayout);
		// 滚动方向
		mBannerLayout.setPlayScrollDirection(BannerLayout.PLAY_SCROLL_DIRECTION_START);
		// 滚动动画
		mBannerLayout.setPageTransformer(new HorDepthPageTransformer());
		// 用户手势操作
		mBannerLayout.setAllowUserScrollable(true);
		// 播放间隔时间：毫秒
//		mBannerLayout.setAutoPlayDelayMillis(3250);
		// 控件滚动间隔时间：毫秒
//		mBannerLayout.setScrollingDuration(850);
		// 自动管理生命周期
		mBannerLayout.setLifecycleOwner(this);
		// 自动循环播放
		mBannerLayout.setAutoPlayFlags(true);
		// 资源适配器
		mBannerLayout.setAdapter(mAdapter);
		// 开始循环播放
		mBannerLayout.startPlay();
		mBannerLayout.addOnPageChangeListener(new ViewPagerCompat.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				Log.e("Adapter", "onPageSelected " + mBannerLayout.isShouldPlayInProgress() + " => " + position);
			}
		});
		mBannerLayout.addOnPlayStateListener(new BannerLayout.OnPlayStateListener() {
			@Override
			public void onPlayStateChanged(@NonNull BannerLayout container, int playState) {
				Log.e("Adapter", "onPlayStateChanged " + mBannerLayout.isShouldPlayInProgress() + " => " + playState);
			}
		});

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
		this.findViewById(R.id.joinCalendarView)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainActivity.this, CoordinatorLayoutActivity.class));
//						startActivity(new Intent(MainActivity.this, CalendarViewActivity.class));
					}
				});
		this.findViewById(R.id.joinFixedGridView)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						startActivity(new Intent(MainActivity.this, FixedGridViewActivity.class));
					}
				});
	}
}
