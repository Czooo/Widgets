package androidx.demon.widget;

import androidx.annotation.NonNull;

/**
 * Author create by ok on 2019-06-25
 * Email : ok@163.com.
 */
public enum RefreshMode {

	REFRESH_MODE_NONE(0),
	REFRESH_MODE_START(-1),
	REFRESH_MODE_END(1),
	REFRESH_MODE_ALL(2);

	private int key;

	RefreshMode(int key) {
		this.key = key;
	}

	public int getKey() {
		return key;
	}

	public boolean hasStartMode() {
		return REFRESH_MODE_START == this || REFRESH_MODE_ALL == this;
	}

	public boolean hasEndMode() {
		return REFRESH_MODE_END == this || REFRESH_MODE_ALL == this;
	}

	public boolean hasRefresh() {
		return hasStartMode() || hasEndMode();
	}

	public boolean containTo(@NonNull RefreshMode mode) {
		if (REFRESH_MODE_NONE == this ||
				REFRESH_MODE_NONE == mode) {
			return false;
		}
		if (REFRESH_MODE_ALL == this) {
			return true;
		}
		return mode == this;
	}

	public static RefreshMode parse(int direction) {
		for (RefreshMode mode : RefreshMode.values()) {
			if (mode.key == direction) {
				return mode;
			}
		}
		return RefreshMode.REFRESH_MODE_NONE;
	}
}
