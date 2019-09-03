package com.demon.fragment;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.demon.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * Author create by ok on 2019-07-15
 * Email : ok@163.com.
 */
public class PageFragment extends Fragment {

	final int[] colors = new int[]{
			android.R.color.background_dark,
			android.R.color.holo_green_light,
			android.R.color.holo_blue_bright,
	};

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_page_layout, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		final int position = this.getArguments().getInt("Position", -1);
		((TextView) view.findViewById(R.id.text)).setText("Position : " + position);
		((TextView) view.findViewById(R.id.text)).setTextColor(Color.WHITE);
		view.setBackgroundResource(colors[position % colors.length]);
	}
}
