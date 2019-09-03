package androidx.demon.widget.cacher;

/**
 * Author create by ok on 2018/7/2 0002
 * Email : ok@163.com.
 */
public interface Cacher<Result, Target> {

	void prepare(Target target, int position);

	Result create(Target target, int position);

	Result obtain(Target target, int position);

	void recycle(Result result);

	void clear();
}
