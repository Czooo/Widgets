package androidx.demon.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

/**
 * Author create by ok on 2019-09-11
 * Email : ok@163.com.
 */
public class MonthPickerView extends ViewGroup {

	public MonthPickerView(Context context) {
		super(context);
	}

	public MonthPickerView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public MonthPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

	}
}
