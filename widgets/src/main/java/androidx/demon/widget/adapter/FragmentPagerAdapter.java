package androidx.demon.widget.adapter;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.demon.widget.ViewPagerCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.Lifecycle;

/**
 * Author create by ok on 2019-07-15
 * Email : ok@163.com.
 */
public abstract class FragmentPagerAdapter extends ViewPagerCompat.Adapter {

	private final FragmentManager mFragmentManager;
	private FragmentTransaction mCurTransaction = null;
	private Fragment mCurrentPrimaryFragment = null;

	public FragmentPagerAdapter(@NonNull FragmentManager fragmentManager) {
		this.mFragmentManager = fragmentManager;
	}

	@NonNull
	@Override
	public Object onCreateItem(@NonNull ViewGroup container, int position, int pagePosition) {
		if (this.mCurTransaction == null) {
			this.mCurTransaction = this.mFragmentManager.beginTransaction();
		}

		final String name = this.makeFragmentName(container.getId(), pagePosition);
		Fragment fragment = this.mFragmentManager.findFragmentByTag(name);

		if (fragment == null) {
			fragment = this.onCreateFragment(container, position);
			this.mCurTransaction.add(container.getId(), fragment, name);
		} else {
			this.mCurTransaction.attach(fragment);
		}
		if (this.mCurrentPrimaryFragment != fragment) {
			fragment.setMenuVisibility(false);
			this.mCurTransaction.setMaxLifecycle(fragment, Lifecycle.State.STARTED);
		}
		return fragment;
	}

	@Override
	public void onDestroyItem(@NonNull ViewGroup container, @NonNull Object object, int position) {
		if (this.mCurTransaction == null) {
			this.mCurTransaction = this.mFragmentManager.beginTransaction();
		}
		this.mCurTransaction.detach((Fragment) object);
	}

	@Override
	public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
		return view == ((Fragment) object).getView();
	}

	@Override
	public void onPrimaryItem(@NonNull ViewGroup container, @NonNull Object object, int position) {
		super.onPrimaryItem(container, object, position);
		final Fragment fragment = (Fragment) object;
		if (this.mCurrentPrimaryFragment != fragment) {
			if (this.mCurTransaction == null) {
				this.mCurTransaction = this.mFragmentManager.beginTransaction();
			}
			if (this.mCurrentPrimaryFragment != null) {
				this.mCurrentPrimaryFragment.setMenuVisibility(false);
				this.mCurTransaction.setMaxLifecycle(this.mCurrentPrimaryFragment, Lifecycle.State.STARTED);
			}
			fragment.setMenuVisibility(true);
			this.mCurTransaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED);
			this.mCurTransaction.setPrimaryNavigationFragment(fragment);
			this.mCurrentPrimaryFragment = fragment;
		}
	}

	@CallSuper
	@Override
	public void onStartUpdate(@NonNull ViewGroup container) {
		super.onStartUpdate(container);
		if (container.getId() == View.NO_ID) {
			throw new IllegalStateException("ViewPager with adapter " + this + " requires a view id");
		}
	}

	@Override
	public void onFinishUpdate(@NonNull ViewGroup container) {
		super.onFinishUpdate(container);
		if (this.mCurTransaction != null) {
			this.mCurTransaction.commitNowAllowingStateLoss();
			this.mCurTransaction = null;
		}
	}

	@NonNull
	public final FragmentManager getFragmentManager() {
		return this.mFragmentManager;
	}

	@NonNull
	public final Fragment getCurrentPrimaryFragment() {
		return this.mCurrentPrimaryFragment;
	}

	@NonNull
	public String makeFragmentName(int containerId, int position) {
		return "androidx:switcher:" + containerId + ":" + position;
	}

	@NonNull
	public abstract Fragment onCreateFragment(@NonNull ViewGroup container, int position);
}
