package testing.uniform;

import abstractions.Heap;

import java.util.Random;

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
    int insertRatio;
    int id;

    public HeapWorker(int id, Heap heap, int range, int insertRatio) {
        this.id = id;
        rnd = new Random(id);
        this.heap = heap;
        this.range = range;
        this.insertRatio = insertRatio;
    }

    public void run() {
        while (!stop) {
            if (rnd.nextInt(100) < insertRatio) {
                heap.insert(rnd.nextInt(range));
                numInserts++;
            } else {
                heap.deleteMin();
                numDeletes++;
            }
        }
        Measure.log("Finished " + id);
    }

    public void stop() {
        stop = true;
    }
}
