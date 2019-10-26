package com.demon;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @Author create by Zoran on 2019-10-25
 * @Email : 171905184@qq.com
 * @Description :
 */
public class FixedGridViewActivity extends AppCompatActivity {

	private static ArrayList<String> data = new ArrayList<>();

	static {
		data.add("http://img0.imgtn.bdimg.com/it/u=3106526341,3733396167&fm=26&gp=0.jpg");
		data.add("http://img1.imgtn.bdimg.com/it/u=795421968,2817681607&fm=11&gp=0.jpg");
		data.add("http://img0.imgtn.bdimg.com/it/u=1732553485,3379133703&fm=26&gp=0.jpg");
		data.add("https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=4012045560,650010815&fm=26&gp=0.jpg");
		data.add("https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=2771670718,331933807&fm=26&gp=0.jpg");
		data.add("https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=845870984,388666921&fm=26&gp=0.jpg");
		data.add("https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=1151795200,1183783656&fm=26&gp=0.jpg");

		data.add("http://img0.imgtn.bdimg.com/it/u=3106526341,3733396167&fm=26&gp=0.jpg");
		data.add("http://img1.imgtn.bdimg.com/it/u=795421968,2817681607&fm=11&gp=0.jpg");
		data.add("http://img0.imgtn.bdimg.com/it/u=1732553485,3379133703&fm=26&gp=0.jpg");
		data.add("https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=4012045560,650010815&fm=26&gp=0.jpg");
		data.add("https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=2771670718,331933807&fm=26&gp=0.jpg");
		data.add("https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=845870984,388666921&fm=26&gp=0.jpg");
		data.add("https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=1151795200,1183783656&fm=26&gp=0.jpg");

		data.add("http://img0.imgtn.bdimg.com/it/u=3106526341,3733396167&fm=26&gp=0.jpg");
		data.add("http://img1.imgtn.bdimg.com/it/u=795421968,2817681607&fm=11&gp=0.jpg");
		data.add("http://img0.imgtn.bdimg.com/it/u=1732553485,3379133703&fm=26&gp=0.jpg");
		data.add("https://ss1.bdstatic.com/70cFvXSh_Q1YnxGkpoWK1HF6hhy/it/u=4012045560,650010815&fm=26&gp=0.jpg");
		data.add("https://ss1.bdstatic.com/70cFuXSh_Q1YnxGkpoWK1HF6hhy/it/u=2771670718,331933807&fm=26&gp=0.jpg");
		data.add("https://ss3.bdstatic.com/70cFv8Sh_Q1YnxGkpoWK1HF6hhy/it/u=845870984,388666921&fm=26&gp=0.jpg");
		data.add("https://ss0.bdstatic.com/70cFuHSh_Q1YnxGkpoWK1HF6hhy/it/u=1151795200,1183783656&fm=26&gp=0.jpg");
	}

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_fixed_grid_view);

		final RecyclerView fixedGridView = this.findViewById(R.id.fixedGridView);
		fixedGridView.setLayoutManager(new GridLayoutManager(this, 3));
		fixedGridView.setAdapter(new RecyclerView.Adapter() {
			@NonNull
			@Override
			public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
				final View contentView;
				if (1 == viewType) {
					contentView =
							LayoutInflater.from(parent.getContext())
									.inflate(R.layout.item_nine_video_layout, parent, false);
				} else {
					contentView =
							LayoutInflater.from(parent.getContext())
									.inflate(R.layout.item_nine_image_layout, parent, false);
				}
				return new RecyclerView.ViewHolder(contentView) {
				};
			}

			@Override
			@SuppressLint("CheckResult")
			public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
				AppCompatImageView mImageView = holder.itemView.findViewById(R.id.picImageView);

				RequestOptions options = new RequestOptions();
				options.placeholder(new ColorDrawable(Color.GRAY));

				Glide.with(mImageView)
						.load(data.get(position))
						.apply(options)
						.into(mImageView);
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
				return data.size();
			}
		});
	}
}
