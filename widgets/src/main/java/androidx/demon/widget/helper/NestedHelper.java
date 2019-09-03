package androidx.demon.widget.helper;

/**
 * Author create by ok on 2019-07-31
 * Email : ok@163.com.
 */
public class NestedHelper {

	public static float getDirectionDifference(int direction) {
		return direction < 0 ? -0.5F : (direction > 0 ? 0.5F : 0.F);
	}
}
