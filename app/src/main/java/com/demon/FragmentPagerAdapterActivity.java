package com.demon;

import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;

import com.demon.fragment.PageFragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.demon.widget.adapter.FragmentPagerAdapterCompat;
import androidx.demon.widget.ViewPagerCompat;
import androidx.fragment.app.Fragment;

/**
 * Author create by ok on 2019-07-15
 * Email : ok@163.com.
 */
public class FragmentPagerAdapterActivity extends AppCompatActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_viewpager_2);

		final FragmentPagerAdapterCompat mFragmentPagerAdapterCompat = new FragmentPagerAdapterCompat(this.getSupportFragmentManager()) {

			@NonNull
			@Override
			public Fragment onCreateFragment(@NonNull ViewGroup container, int position) {
				// 不需要缓存，直接创建
				Log.e("FragmentPagerAdapterCompat", "onCreateFragment : " + position);
				Bundle args = new Bundle();
				args.putInt("Position", position);
				Fragment fragment = new PageFragment();
				fragment.setArguments(args);
				return fragment;
			}

			@Override
			public int getItemCount() {
				return 5;
			}
		};
		final ViewPagerCompat mViewPagerCompat = findViewById(R.id.viewPagerCompat);
		mViewPagerCompat.setAdapter(mFragmentPagerAdapterCompat);
		mViewPagerCompat.setNestedScrollingEnabled(true);
		mViewPagerCompat.setScrollingLoop(true);
	}
}
