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
    int start;
    int dx;

    public HeapWorker(int id, Heap heap, int range, int insertRation, int start, int dx) {
        rnd = new Random(id);
        this.heap = heap;
        this.range = range;
        this.insertRation = insertRation;
        this.start = start;
        this.dx = dx;
    }

    public void run() {
        while (!stop) {
            if (rnd.nextInt(100) < insertRation) {
                heap.insert(start);
                start -= dx;
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
