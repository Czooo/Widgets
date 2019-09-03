package androidx.demon.widget;

import android.content.Context;
import android.graphics.ColorMatrixColorFilter;
import android.util.AttributeSet;

/**
 * Author create by ok on 2019-07-18
 * Email : ok@163.com.
 */
public class AppCompatImageView extends androidx.appcompat.widget.AppCompatImageView {

	public final float[] BG_PRESSED = new float[]{1, 0, 0, 0, -50, 0, 1,
			0, 0, -50, 0, 0, 1, 0, -50, 0, 0, 0, 1, 0};
	public final float[] BG_NOT_PRESSED = new float[]{1, 0, 0, 0, 0, 0,
			1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0};

	private boolean mShouldTouchEffect = true;

	public AppCompatImageView(Context context) {
		super(context);
	}

	public AppCompatImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public AppCompatImageView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	public void setPressed(boolean pressed) {
		this.performPressedView(pressed);
	}

	public void setShouldTouchEffect(boolean shouldTouchEffect) {
		this.mShouldTouchEffect = shouldTouchEffect;
	}

	private void performPressedView(boolean pressed) {
		if (this.mShouldTouchEffect) {
			if (pressed) {
				// 通过设置滤镜来改变图片亮度
				this.setDrawingCacheEnabled(true);
				this.setColorFilter(new ColorMatrixColorFilter(BG_PRESSED));
				this.getDrawable().setColorFilter(new ColorMatrixColorFilter(BG_PRESSED));
			} else {
				this.setColorFilter(new ColorMatrixColorFilter(BG_NOT_PRESSED));
				this.getDrawable().setColorFilter(new ColorMatrixColorFilter(BG_NOT_PRESSED));
			}
		}
		super.setPressed(pressed);
	}
}
