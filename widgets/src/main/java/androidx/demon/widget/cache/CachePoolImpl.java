package androidx.demon.widget.cache;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author create by ok on 2018/7/2 0002
 * Email : ok@163.com.
 */
public abstract class CachePoolImpl<Result, Target> implements CachePool<Result, Target> {

	private static final int DEFAULT_MAX_POOL_SIZE = 5;

	private final AtomicInteger mPoolSize = new AtomicInteger();

	private Node<Result> mNode;
	private int curPoolSize;

	public CachePoolImpl() {
		this(DEFAULT_MAX_POOL_SIZE);
	}

	public CachePoolImpl(int maxPoolSize) {
		this.mNode = new Node<>();
		this.mPoolSize.set(maxPoolSize);
	}

	public int getPoolSize() {
		return mPoolSize.get();
	}

	@Override
	public void prepare(Target target, int position) {
		synchronized (this) {
			final int poolSize = getPoolSize();
			Node<Result> node = this.mNode;
			int curPoolSize = this.curPoolSize;

			while (curPoolSize < poolSize) {
				if (node.node == null) {
					node.node = create(target, position);
				} else {
					Node<Result> n1 = new Node<>();
					n1.next = node;
					n1.node = create(target, position);
					node = n1; //new node is the front
				}
				curPoolSize++;
			}
			this.mNode = node;
			this.curPoolSize = curPoolSize;
		}
	}

	@Override
	public Result obtain(Target target, int position) {
		synchronized (this) {
			if (this.mNode.node != null) {
				Node<Result> node = this.mNode;
				Result result = node.node;
				this.mNode = node.next;
				//may null
				if (this.mNode == null) {
					this.mNode = new Node<>();
				}
				node.next = null;
				this.curPoolSize--;
				return result;
			}
		}
		return create(target, position);
	}

	@Override
	public void recycle(Result result) {
		synchronized (this) {
			final int poolSize = getPoolSize();
			if (this.curPoolSize < poolSize) {
				Node<Result> node = new Node<>();
				node.next = this.mNode;
				node.node = result;
				this.mNode = node;
				this.curPoolSize++;
			}
		}
	}

	@Override
	public void clear() {
		synchronized (this) {
			Node<Result> node = this.mNode;
			while (node != null) {
				node.node = null;
				node = node.next;
			}
			this.mNode = new Node<>();
			this.curPoolSize = 0;
		}
	}

	@SuppressWarnings("hiding")
	private final class Node<T> {

		private T node;

		private Node<T> next;
	}
}
