package androidx.demon.widget.cache;

import android.util.SparseArray;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Author create by ok on 2019-07-17
 * Email : ok@163.com.
 */
public class RecycledPool<T> {

	private static final boolean DEBUG = false;

	private static final int DEFAULT_MAX_SCRAP = 5;

	private final SparseArray<ScrapData<T>> mScrap = new SparseArray<>();

	private int maxScrapSize;

	public RecycledPool() {
		this(DEFAULT_MAX_SCRAP);
	}

	public RecycledPool(int maxScrapSize) {
		this.maxScrapSize = maxScrapSize;
	}

	public void setMaxRecycledSize(int type, int size) {
		final ScrapData<T> scrapData = this.getScrapDataForType(type);
		scrapData.mMaxScrap = size;
		final ArrayList<T> scrapHeap = scrapData.mScrapHeap;
		while (scrapHeap.size() > size) {
			scrapHeap.remove(scrapHeap.size() - 1);
		}
	}

	public synchronized void putRecycled(int type, @NonNull T data) {
		final ScrapData<T> scrapData = this.getScrapDataForType(type);
		final ArrayList<T> scrapHeap = scrapData.mScrapHeap;
		if (scrapData.mMaxScrap <= scrapHeap.size()) {
			return;
		}
		if (DEBUG && scrapHeap.contains(data)) {
			throw new IllegalArgumentException("this scrap item already exists");
		}
		scrapHeap.add(data);
	}

	@Nullable
	public synchronized T getRecycled(int type) {
		final ScrapData<T> scrapData = this.mScrap.get(type);
		if (scrapData != null && !scrapData.mScrapHeap.isEmpty()) {
			final ArrayList<T> scrapHeap = scrapData.mScrapHeap;
			return scrapHeap.remove(scrapHeap.size() - 1);
		}
		return null;
	}

	public synchronized void clear() {
		for (int index = 0; index < this.mScrap.size(); index++) {
			final ScrapData<T> scrapData = this.mScrap.valueAt(index);
			scrapData.mScrapHeap.clear();
		}
	}

	@NonNull
	private ScrapData<T> getScrapDataForType(int itemViewType) {
		ScrapData<T> scrapData = this.mScrap.get(itemViewType);
		if (scrapData == null) {
			scrapData = new ScrapData<>();
			scrapData.mMaxScrap = this.maxScrapSize;
			this.mScrap.put(itemViewType, scrapData);
		}
		return scrapData;
	}

	@NonNull
	@Override
	public String toString() {
		return "Type Size : " + this.mScrap.size();
	}

	private final class ScrapData<Data> {

		private final ArrayList<Data> mScrapHeap = new ArrayList<>();

		private int mMaxScrap = DEFAULT_MAX_SCRAP;
	}
}
