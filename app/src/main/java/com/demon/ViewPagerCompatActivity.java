package com.demon;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.demon.widget.TripFooterLoadView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.demon.widget.RefreshLayout;
import androidx.demon.widget.RefreshMode;
import androidx.demon.widget.ViewPagerCompat;
import androidx.demon.widget.adapter.PagerAdapter;
import androidx.demon.widget.transformers.DepthPageTransformer;

/**
 * Author create by ok on 2019-07-11
 * Email : ok@163.com.
 */
public class ViewPagerCompatActivity extends AppCompatActivity {

	private static final boolean DEBUG = true;
	private static final String TAG = "ViewPagerCompatActivity";

	final int[] colors = new int[]{
			android.R.color.background_dark,
			android.R.color.holo_green_light,
			android.R.color.holo_blue_bright,
	};

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_viewpager);

		final RefreshLayout mRefreshLayout = this.findViewById(R.id.refreshLayout);
		mRefreshLayout.setFooterLoadView(new TripFooterLoadView());
		mRefreshLayout.setOnRefreshListener(new RefreshLayout.OnRefreshListener() {

			@Override
			public void onRefreshing(@NonNull RefreshLayout refreshLayout, @NonNull RefreshMode mode) {
				mRefreshLayout.postDelayed(new Runnable() {
					@Override
					public void run() {
						mRefreshLayout.setRefreshing(false, 700);
					}
				}, 2000);
			}
		});

		final PagerAdapter<ViewHolder> mPagerAdapter = new PagerAdapter<ViewHolder>() {

			@NonNull
			@Override
			public ViewPagerCompatActivity.ViewHolder onCreateViewHolder(@NonNull LayoutInflater inflater, @NonNull ViewGroup container, int itemViewType) {
				if (DEBUG) {
					Log.v(TAG, "onCreateViewHolder === " + itemViewType);
				}
				final View preChildView;
				if (itemViewType == 1) {
					preChildView = inflater.inflate(R.layout.item_page_layout_2, container, false);
				} else {
					preChildView = inflater.inflate(R.layout.item_page_layout, container, false);
				}
				return new ViewPagerCompatActivity.ViewHolder(preChildView);
			}

			@Override
			public void onBindViewHolder(@NonNull ViewPagerCompatActivity.ViewHolder holder, final int position, @Nullable Object object) {
				final View view = holder.getItemView();
				// 最后1个用于装饰
//				if (position == this.getItemCount() - 1) {
//					view.setBackgroundColor(Color.TRANSPARENT);
//					((TextView) view.findViewById(R.id.text)).setText("");
//					return;
//				}
				view.setBackgroundResource(colors[position % colors.length]);
				((TextView) view.findViewById(R.id.text)).setTextColor(Color.WHITE);
				((TextView) view.findViewById(R.id.text)).setText("Position " + position);
				view.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						ViewPagerCompat mViewPagerCompat = findViewById(R.id.viewPagerCompat);
//						if (mViewPagerCompat.getCurrentItem() == 5) {
//							mViewPagerCompat.setCurrentItem(getItemCount() - 1, true);
//						} else if (mViewPagerCompat.getCurrentItem() == getItemCount() - 1) {
//							mViewPagerCompat.setCurrentItem(5, true);
//						} else {
//							int currentItemPosition = mViewPagerCompat.getCurrentItem() - 1;
//							currentItemPosition = currentItemPosition >= getItemCount() ? 0 : (currentItemPosition < 0 ? getItemCount() - 1 : currentItemPosition);
//							mViewPagerCompat.setCurrentItem(currentItemPosition, true);
//						}
						Toast.makeText(v.getContext(), "Position " + position, Toast.LENGTH_LONG).show();
					}
				});
			}

			@Override
			public int getItemViewType(int position) {
				if (position % 2 == 0) {
					return 1;
				}
				return super.getItemViewType(position);
			}

			@Override
			public int getItemCount() {
				// 最后1个用于装饰
				return 12;
			}

			@Override
			public float getPageWeight(int position) {
//				if (position == this.getItemCount() - 1) {
//					// 最后1个用于装饰
//					return 0.4f;
//				}
//				return 0.6f;
				return super.getPageWeight(position);
			}
		};
		final ViewPagerCompat mViewPagerCompat = findViewById(R.id.viewPagerCompat);
		mViewPagerCompat.setPageTransformer(true, new DepthPageTransformer());
		mViewPagerCompat.setAdapter(mPagerAdapter);
		mViewPagerCompat.setOffscreenPageLimit(2);
//		ColorDrawable drawable = new ColorDrawable(Color.RED);
//		mViewPagerCompat.setPageMarginDrawable(drawable);
//		mViewPagerCompat.setPageMargin(20);

		this.findViewById(R.id.oVertical)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mRefreshLayout.setOrientation(RefreshLayout.VERTICAL);
						mViewPagerCompat.setOrientation(ViewPagerCompat.VERTICAL);
						Toast.makeText(v.getContext(), "Orientation VERTICAL" , Toast.LENGTH_LONG).show();
					}
				});
		this.findViewById(R.id.oHorizontal)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mRefreshLayout.setOrientation(RefreshLayout.HORIZONTAL);
						mViewPagerCompat.setOrientation(ViewPagerCompat.HORIZONTAL);
						Toast.makeText(v.getContext(), "Orientation HORIZONTAL" , Toast.LENGTH_LONG).show();
					}
				});

		this.findViewById(R.id.sLooping)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mViewPagerCompat.setScrollingLoop(true);
						Toast.makeText(v.getContext(), "ScrollingLoop true" , Toast.LENGTH_LONG).show();
					}
				});
		this.findViewById(R.id.sUnLooping)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						mViewPagerCompat.setScrollingLoop(false);
						Toast.makeText(v.getContext(), "ScrollingLoop false" , Toast.LENGTH_LONG).show();
					}
				});
	}

	static class ViewHolder extends PagerAdapter.ViewHolder {

		ViewHolder(@NonNull View itemView) {
			super(itemView);
		}
	}
}
