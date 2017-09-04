package testing.skewed;

import abstractions.Heap;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by vaksenov on 30.03.2017.
 */
public class HeapWorker implements Runnable {
    volatile boolean stop;

    volatile int numInserts;
    volatile int numDeletes;

    Random rnd;
    Heap heap;
    int range;
    int insertRation;
    AtomicInteger keys;

    public HeapWorker(int id, Heap heap, int range, int insertRation, AtomicInteger keys) {
        rnd = new Random(id);
        this.heap = heap;
        this.range = range;
        this.insertRation = insertRation;
        this.keys = keys;
    }

    public void run() {
        while (!stop) {
            if (rnd.nextInt(100) < insertRation) {
                heap.insert(keys.getAndDecrement());
                numInserts++;
            } else {
                heap.deleteMin();
                numDeletes++;
            }
        }
    }

    public void stop() {
        stop = true;
    }
}
