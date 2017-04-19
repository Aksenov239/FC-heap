package lockbased;

import abstractions.Heap;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by vaksenov on 05.04.2017.
 */
public class BlockingHeap implements Heap {

//    PriorityBlockingQueue<Integer> pq;
    int[] heap;
    int heapSize;
    final ReentrantLock lock = new ReentrantLock();

    public BlockingHeap(int size, int numThreads) {
        heap = new int[4 * size];
        heapSize = 0;
//        pq = new PriorityBlockingQueue<>(4 * size);
    }

    public int deleteMin() {
        lock.lock();
        int ans = heap[1];
        heap[1] = heap[heapSize--];
        int v = 1;
        int half = heapSize >> 1;
        while (v <= half) {
            int l = v << 1;
            if (l + 1 <= heapSize && heap[l] > heap[l + 1]) {
                l++;
            }
            if (heap[l] < heap[v]) {
                int q = heap[l];
                heap[l] = heap[v];
                heap[v] = q;
                v = l;
            } else {
                break;
            }
        }

        lock.unlock();
        return ans;
//        return pq.poll();
    }

    public void insert(int v) {
        lock.lock();

        if (heapSize + 2 >= heap.length) {
            int[] newHeap = new int[2 * heap.length];
            for (int i = 1; i <= heapSize; i++){
                newHeap[i] = heap[i];
            }
            heap = newHeap;
        }

        heap[++heapSize] = v;
        int current = heapSize;
        while (current > 1) {
            int p = current >> 1;
            if (heap[current] < heap[p]) {
                int q = heap[p];
                heap[p] = heap[current];
                heap[current] = q;
                current = p;
            } else {
                break;
            }
        }

        lock.unlock();
//        pq.add(v);
    }

    public void sequentialInsert(int v) {
        insert(v);
    }

    public void clear() {
        heapSize = 0;
//        pq.clear();
    }
}
