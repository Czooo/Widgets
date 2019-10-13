package com.demon;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
import androidx.demon.widget.NineGridView;
import androidx.demon.widget.RefreshLayout;
import androidx.demon.widget.RefreshMode;
import androidx.demon.widget.adapter.PagerAdapterCompat;
import androidx.demon.widget.transformers.HorDepthPageTransformer;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Author create by ok on 2019-07-17
 * Email : ok@163.com.
 */
public class NineGridViewActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_recyclerview);

		final RefreshLayout mRefreshLayout = this.findViewById(R.id.refreshLayout);
		mRefreshLayout.setFooterLoadView(new TripFooterLoadView());
		mRefreshLayout.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {

			@Override
			public void onRefreshing(@NonNull RefreshLayout refreshLayout, @NonNull RefreshMode mode) {
				Log.e("RefreshRecyclerView", "Requesting");
				mRefreshLayout.postDelayed(new Runnable() {
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
				LayoutInflater inflater = LayoutInflater.from(parent.getContext());
				if (1 == viewType) {
					return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_header_banner_layout, parent, false)) {
					};
				}
				return new RecyclerView.ViewHolder(inflater.inflate(R.layout.item_comment_layout, parent, false)) {
				};
			}

			@Override
			public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
				if (0 == position) {
					final IndicatorView mIndicatorView = holder.itemView.findViewById(R.id.indicatorView);
					final BannerLayout mBannerLayout = holder.itemView.findViewById(R.id.bannerLayout);

					if (mBannerLayout.getAdapter() != null) {
						return;
					}

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
						public PagerAdapterCompat.ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, int itemViewType) {
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
					mBannerLayout.addOnPlayStateListener(new BannerLayout.OnPlayStateListener() {
						@Override
						public void onPlayStateChanged(@NonNull BannerLayout container, int playState) {
							Log.e("NineGridView", "Playing State " + playState);
						}
					});
					// 自动管理生命周期
					mBannerLayout.setLifecycleOwner(NineGridViewActivity.this);
					// 滚动方向
					mBannerLayout.setPlayScrollDirection(BannerLayout.PLAY_SCROLL_DIRECTION_START);
					// 滚动动画
					mBannerLayout.setPageTransformer(new HorDepthPageTransformer());
					// 指示器
					mBannerLayout.addPlayIndicator(mIndicatorView);
					// 用户手势操作
					mBannerLayout.setAllowUserScrollable(true);
					// 播放间隔时间：毫秒
					mBannerLayout.setAutoPlayDelayMillis(2500);
					// 控件滚动间隔时间：毫秒
					mBannerLayout.setScrollingDuration(600);
					// 自动循环播放
					mBannerLayout.setAutoPlayFlags(true);
					// 资源适配器
					mBannerLayout.setAdapter(mAdapter);
					// 开始循环播放
					mBannerLayout.startPlay();
					return;
				}
				TextView mTextView = holder.itemView.findViewById(R.id.contentTextView);
				mTextView.setText("外包来了 外包来了 找技术空余时间做包上运用市场(百度 小米 华为  小米 运用宝  魅族 OPPO  VIVO 联想 360  等等)");

				NineGridView mNineGridView = holder.itemView.findViewById(R.id.nineGridView);
				mNineGridView.setOnItemClickListener(new NineGridView.OnItemClickListener() {
					@Override
					public void onItemClick(@NonNull ViewGroup container, @NonNull View view, int position) {
						Toast.makeText(container.getContext(), "Position " + position, Toast.LENGTH_LONG).show();
					}
				});

				// itemViewTyp = 0 的最大缓存大小
				mNineGridView.getRecycledPool().setMaxRecycledSize(0, 9);
				// itemViewTyp = 1 的最大缓存大小
				mNineGridView.getRecycledPool().setMaxRecycledSize(1, 9);
				/**
				 * 优化项：recycledOldViewHolders = true, 避免每次都需新建ViewHolder
				 * @see NineGridView#ViewHolder
				 *
				 * 同时: 设置ViewHolder的最大缓存大小，默认最大缓存大小是 9.
				 * @see NineGridView#getRecycledPool()
				 * */
				mNineGridView.setAdapter(new Adapter(position), true);
			}

			@Override
			public int getItemViewType(int position) {
				if (position == 0) {
					return 1;
				}
				return super.getItemViewType(position);
			}

			@Override
			public int getItemCount() {
				return 21;
			}
		};
		final RecyclerView mRecyclerView = this.findViewById(R.id.recyclerView);
		mRecyclerView.getRecycledViewPool().setMaxRecycledViews(0, 10);
		mRecyclerView.setAdapter(mAdapter);
	}

	static class Adapter extends NineGridView.Adapter<NineGridView.ViewHolder> {

		private final int position;

		private final ArrayList<String> data = new ArrayList<>();

		Adapter(int position) {
			this.position = position - 1;
			final ArrayList<String> data = new ArrayList<>();
			data.add("http://img0.imgtn.bdimg.com/it/u=3106526341,3733396167&fm=26&gp=0.jpg");
			data.add("http://img1.imgtn.bdimg.com/it/u=795421968,2817681607&fm=11&gp=0.jpg");
			data.add("http://img0.imgtn.bdimg.com/it/u=1732553485,3379133703&fm=26&gp=0.jpg");
			data.add("https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=4012045560,650010815&fm=26&gp=0.jpg");
			data.add("https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=2771670718,331933807&fm=26&gp=0.jpg");
			data.add("https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=845870984,388666921&fm=26&gp=0.jpg");
			data.add("https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=1151795200,1183783656&fm=26&gp=0.jpg");

			for (int i = 0; i < 9; i++) {
				this.data.add(data.get((int) (Math.random() * data.size())));
			}
		}

		@NonNull
		@Override
		public NineGridView.ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, int itemViewType) {
			Log.i("Project", "Position " + this.position + " => onCreateViewHolder(NineGridView) " + itemViewType);
			if (itemViewType == 1) {
				return new NineGridView.ViewHolder(inflater.inflate(R.layout.item_nine_video_layout, container, false)) {
				};
			}
			return new NineGridView.ViewHolder(inflater.inflate(R.layout.item_nine_image_layout, container, false)) {
			};
		}

		@Override
		@SuppressLint("CheckResult")
		public void onBindViewHolder(@NonNull NineGridView.ViewHolder holder, int position, @Nullable Object object) {
			AppCompatImageView mImageView = holder.getItemView().findViewById(R.id.picImageView);

			RequestOptions options = new RequestOptions();
			options.placeholder(new ColorDrawable(Color.GRAY));

			Glide.with(mImageView)
					.load(data.get(position))
					.apply(options)
					.into(mImageView);
		}

		@Override
		public int getItemCount() {
			if (this.position == 1) {
				return 2;
			}
			if (this.position == 3) {
				return 1;
			}
			if (this.position == 4) {
				return 6;
			}
			if (this.position == 5) {
				return 3;
			}
			if (this.position == 7) {
				return 7;
			}
			if (this.position == 8) {
				return 2;
			}
			if (this.position == 10) {
				return 8;
			}
			if (this.position == 12) {
				return 7;
			}
			if (this.position == 14) {
				return 3;
			}
			if (this.position == 15) {
				return 1;
			}
			if (this.position == 16) {
				return 8;
			}
			if (this.position == 18) {
				return 4;
			}
			if (this.position == 19) {
				return 1;
			}
			return data.size();
		}

		@Override
		public int getItemViewType(int position) {
			if (position == 4) {
				return 1;
			}
			if (this.position == 0 && position == 0) {
				return 1;
			}
			if (this.position == 1 && position == 0) {
				return 1;
			}
			if (this.position == 19) {
				return 1;
			}
			return super.getItemViewType(position);
		}

		@Override
		public void getSingleItemViewSize(@NonNull ViewGroup container, @NonNull int[] itemViewSize) {
			super.getSingleItemViewSize(container, itemViewSize);
			if (this.position == 19) {
				itemViewSize[0] = 400;
				itemViewSize[1] = 600;
				return;
			}
			itemViewSize[0] = 600;
			itemViewSize[1] = 400;
		}
	}
}
