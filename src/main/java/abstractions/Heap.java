package abstractions;

/**
 * Created by vaksenov on 24.03.2017.
 */
public interface Heap {
    public int deleteMin();

    public void insert(int v);

    public void sequentialInsert(int v);

    public void clear();
}
