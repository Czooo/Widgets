package androidx.demon.widget.cacher;

import android.view.View;
import android.view.ViewGroup;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author create by ok on 2018/7/2 0002
 * Email : ok@163.com.
 */
public abstract class ViewCacher implements Cacher<View, ViewGroup> {

	private static final int DEFAULT_MAX_POOL_SIZE = 10;

	private final AtomicInteger mPoolSize = new AtomicInteger();

	private Node mNode;
	private int curPoolSize;

	public ViewCacher() {
		this(DEFAULT_MAX_POOL_SIZE);
	}

	public ViewCacher(int maxPoolSize) {
		this.mNode = new Node();
		this.mPoolSize.set(maxPoolSize);
	}

	public int getPoolSize() {
		return mPoolSize.get();
	}

	@Override
	public void prepare(ViewGroup viewGroup, int position) {
		synchronized (this) {
			final int poolSize = getPoolSize();
			Node node = this.mNode;
			int curPoolSize = this.curPoolSize;

			while (curPoolSize < poolSize) {
				if (node.children == null) {
					node.children = create(viewGroup, position);
				} else {
					Node n1 = new Node();
					n1.next = node;
					n1.children = create(viewGroup, position);
					node = n1; //new node is the front
				}
				curPoolSize++;
			}
			this.mNode = node;
			this.curPoolSize = curPoolSize;
		}
	}

	@Override
	public View obtain(ViewGroup viewGroup, int position) {
		synchronized (this) {
			if (mNode.children != null) {
				Node node = this.mNode;
				View children = node.children;
				mNode = node.next;
				//may null
				if (mNode == null) {
					mNode = new Node();
				}
				node.next = null;
				curPoolSize--;
				return children;
			}
		}
		return create(viewGroup, position);
	}

	@Override
	public void recycle(View view) {
		synchronized (this) {
			final int poolSize = getPoolSize();
			if (curPoolSize < poolSize) {
				Node node = new Node();
				node.next = mNode;
				node.children = view;
				this.mNode = node;
				curPoolSize++;
			}
		}
	}

	@Override
	public void clear() {
		synchronized (this) {
			Node node = this.mNode;
			while (node != null) {
				node.children = null;
				node = node.next;
			}
			this.mNode = new Node();
			curPoolSize = 0;
		}
	}

	@SuppressWarnings("hiding")
	public class Node {

		View children;

		Node next;
	}
}
