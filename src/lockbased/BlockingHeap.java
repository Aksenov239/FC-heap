package lockbased;

import abstractions.Heap;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * Created by vaksenov on 05.04.2017.
 */
public class BlockingHeap implements Heap {

    PriorityBlockingQueue<Integer> pq;

    public BlockingHeap(int size, int numThreads) {
        pq = new PriorityBlockingQueue<>(4 * size);
    }

    public int deleteMin() {
        return pq.poll();
    }

    public void insert(int v) {
        pq.add(v);
    }

    public void sequentialInsert(int v) {
        insert(v);
    }

    public void clear() {
        pq.clear();
    }
}
