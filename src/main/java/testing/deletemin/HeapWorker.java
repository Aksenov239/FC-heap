package testing.deletemin;

import abstractions.Heap;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by vaksenov on 30.03.2017.
 */
public class HeapWorker implements Runnable {
    volatile boolean stop;

    volatile int numDeletes;

    Random rnd;
    Heap heap;
    AtomicInteger total;

    public HeapWorker(int id, Heap heap, AtomicInteger total) {
        rnd = new Random(id);
        this.heap = heap;
        this.total = total;
    }

    public void run() {
        while (total.getAndDecrement() > 0) {
            heap.deleteMin();
            numDeletes++;
        }
    }

    public void stop() {
        stop = true;
    }
}
