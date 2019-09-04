package com.demon;

import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.demon.widget.SwipeSideLayout;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Author create by ok on 2019-07-31
 * Email : ok@163.com.
 */
public class SwipeSideLayoutActivity extends AppCompatActivity {

	private final ArrayList<RecyclerView.ViewHolder> mViewHolders = new ArrayList<>();

	public int getStatusBarHeight() {
		int result = 0;
		//获取状态栏高度的资源id
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}

	@Override
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.setContentView(R.layout.activity_widget_swipe_side_layout);

		this.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
				| View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
		this.getWindow().setStatusBarColor(Color.TRANSPARENT);
		this.findViewById(R.id.toolbar).getLayoutParams().height = getStatusBarHeight();

		final SwipeSideLayout mSwipeSideLayout = this.findViewById(R.id.swipeSideLayout);
		mSwipeSideLayout.setMinScrollScale(0.1F);
		mSwipeSideLayout.setFrictionRatio(0.25F);

		final RecyclerView.Adapter<RecyclerView.ViewHolder> mAdapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {

			@NonNull
			@Override
			public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
				final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
				final View view = inflater.inflate(R.layout.item_widget_slide_layout, parent, false);
				return new RecyclerView.ViewHolder(view) {
				};
			}

			@Override
			public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder holder, final int position) {
				final SwipeSideLayout mSwipeSideLayout = holder.itemView.findViewById(R.id.slideLayout);
				mSwipeSideLayout.setOnOpenStateListener(new SwipeSideLayout.OnOpenStateListener() {

					@Override
					public void onOpenStateChanged(@NonNull SwipeSideLayout slideLayout) {
						if (slideLayout.isOpenState()) {
							if (!mViewHolders.contains(holder)) {
								dispatchCloseOtherItem();
								mViewHolders.add(holder);
							}
						} else {
							mViewHolders.remove(holder);
						}
					}
				});
				final TextView mTextView = holder.itemView.findViewById(R.id.contentTextView);
				mTextView.setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View view) {
						if (mSwipeSideLayout.isOpenState()) {
							mSwipeSideLayout.setOpenState(false);
							return;
						}
						dispatchCloseOtherItem();
						Toast.makeText(view.getContext(), "Click Item " + position, Toast.LENGTH_LONG).show();
					}
				});
				holder.itemView.findViewById(R.id.follow).setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View view) {
						Toast.makeText(view.getContext(), "Follow " + position, Toast.LENGTH_LONG).show();
						mSwipeSideLayout.setOpenState(false);
					}
				});
				holder.itemView.findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {

					@Override
					public void onClick(View view) {
						Toast.makeText(view.getContext(), "Delete " + position, Toast.LENGTH_LONG).show();
						mSwipeSideLayout.setOpenState(false);
					}
				});
			}

			@Override
			public int getItemCount() {
				return 20;
			}
		};
		final RecyclerView mRecyclerView = this.findViewById(R.id.recyclerView);
		mRecyclerView.addOnItemTouchListener(new OnItemTouchListener());
		mRecyclerView.setAdapter(mAdapter);
	}

	final class OnItemTouchListener extends RecyclerView.SimpleOnItemTouchListener {

		private float mInitialMotionX, mInitialMotionY;

		private final float mTouchSlop;

		OnItemTouchListener() {
			final ViewConfiguration mViewConfiguration = ViewConfiguration.get(SwipeSideLayoutActivity.this);
			this.mTouchSlop = mViewConfiguration.getScaledTouchSlop();
		}

		@Override
		public boolean onInterceptTouchEvent(@NonNull RecyclerView recyclerView, @NonNull MotionEvent event) {
			final int mActionMasked = event.getActionMasked();
			if (MotionEvent.ACTION_DOWN == mActionMasked) {
				this.mInitialMotionX = event.getX();
				this.mInitialMotionY = event.getY();
			} else if (MotionEvent.ACTION_MOVE == event.getActionMasked()) {
				final float x = event.getX();
				final float y = event.getY();
				final float dx = this.mInitialMotionX - x;
				final float dy = this.mInitialMotionY - y;

				if ((Math.abs(dy) > this.mTouchSlop && Math.abs(dy) * 0.5F > Math.abs(dx))
						|| (Math.abs(dx) > this.mTouchSlop && Math.abs(dx) * 0.5F > Math.abs(dy))) {
					final View underView = recyclerView.findChildViewUnder(x, y);
					if (underView != null) {
						dispatchCloseOtherItem(recyclerView.getChildViewHolder(underView));
					} else {
						dispatchCloseOtherItem();
					}
				}
			}
			return false;
		}
	}

	void dispatchCloseOtherItem() {
		dispatchCloseOtherItem(null);
	}

	void dispatchCloseOtherItem(@Nullable RecyclerView.ViewHolder underViewHolder) {
		for (int index = 0; index < this.mViewHolders.size(); index++) {
			final RecyclerView.ViewHolder holder = this.mViewHolders.get(index);

			if (underViewHolder != null && underViewHolder == holder) {
				continue;
			}

			final SwipeSideLayout mSwipeSideLayout = holder.itemView.findViewById(R.id.slideLayout);
			if (mSwipeSideLayout.isOpenState()) {
				mSwipeSideLayout.setOpenState(false);
			}
			this.mViewHolders.remove(holder);
			index--;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_BACK == keyCode) {
			boolean intercept = false;
			if (!this.mViewHolders.isEmpty()) {
				this.dispatchCloseOtherItem();
				intercept = true;
			}
			final SwipeSideLayout mSwipeSideLayout = this.findViewById(R.id.swipeSideLayout);
			if (mSwipeSideLayout.isOpenState()) {
				mSwipeSideLayout.setOpenState(false);
				intercept = true;
			}
			if (intercept) {
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}
}
