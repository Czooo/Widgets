package androidx.demon.widget.transformers;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.demon.widget.ViewPagerCompat;

/**
 * Author create by ok on 2019-07-07
 * Email : ok@163.com.
 */
public class DepthPageTransformer implements ViewPagerCompat.PageTransformer {

	private float mMinScale = 0.8f;
	/**
	 * Apply a property transformation to the given page.
	 *
	 * @param page     Apply the transformation to this page
	 * @param position Position of page relative to the current front-and-center
	 *                 position of the pager. 0 is front and center. 1 is one full
	 */
	@Override
	public void transformPage(@NonNull View page, float position) {
		if (position < 0) {
			page.setAlpha(1);
			page.setTranslationY(0);
			page.setScaleX(1);
			page.setScaleY(1);
		} else {
			final float scaleFactor = mMinScale + (1 - mMinScale) * (1 - Math.abs(position));
			page.setAlpha(1 - position);
			page.setPivotX(0.5f * page.getWidth());
			page.setPivotY(0.5f * page.getHeight());
			page.setTranslationY(page.getHeight() * -position);
			page.setScaleX(scaleFactor);
			page.setScaleY(scaleFactor);
		}
	}
}
