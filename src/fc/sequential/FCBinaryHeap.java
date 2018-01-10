package fc.sequential;

import abstractions.Heap;
import fc.FC;
import fc.FCArray;
import fc.FCRequest;
import org.openjdk.jmh.logic.BlackHole;

import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by vaksenov on 24.03.2017.
 */
public class FCBinaryHeap implements Heap {
    private FCArray fc;
    private ThreadLocal<Request> allocatedRequests = new ThreadLocal<>();
    private volatile boolean leaderExists;
    private volatile boolean leaderInTransition;
    private final int TRIES;
    private final int THRESHOLD;

    public enum OperationType {
        DELETE_MIN,
        INSERT
    }

    public enum Status {
        PUSHED,
        FINISHED
    }

    public class Request extends FCArray.FCRequest implements Comparable<Request> {
        OperationType type;
        int v;

        public Request() {
            status = Status.PUSHED;
        }

        public void set(OperationType operationType) {
            this.type = operationType;
            status = Status.PUSHED;
        }

        public void set(OperationType operationType, int value) {
            this.v = value;
            set(operationType);
        }

        public int compareTo(Request request) {
            return Integer.compare(v, request.v);
        }

        volatile Status status;

        public boolean holdsRequest() {
            return status != Status.FINISHED;
        }
    }

    private Request getLocalRequest() {
        Request request = allocatedRequests.get();
        if (request == null) {
            request = new Request();
            allocatedRequests.set(request);
        }
        return request;
    }

    private int[] heap;
    private int heapSize;

    int threads;

    public FCBinaryHeap(int size, int numThreads) {
        fc = new FCArray(numThreads);
        size = Integer.highestOneBit(size) * 4;
        heap = new int[size];
        TRIES = numThreads;
        THRESHOLD = (int) Math.ceil(1. * numThreads / 1.7);
        threads = numThreads;
    }

    public void remove(Request request) {
        request.v = heap[1];
        heap[1] = heap[heapSize--];
        int current = 1;
        int to = heapSize >> 1;
        while (current <= to) { // While there exists at least one child in heap
            int leftChild = current << 1;
            int rightChild = leftChild + 1;
            if (heap[current] <= heap[leftChild]
                    && (rightChild > heapSize || heap[current] <= heap[rightChild])) { // I'm better than children and could finish
                return;
            }
            int swap = rightChild > heapSize || heap[leftChild] < heap[rightChild] ? leftChild : rightChild; // With whom to swap
            int tmp = heap[current];
            heap[current] = heap[swap];
            heap[swap] = tmp;

            current = swap;
        }
    }

    public void sequentialInsert(int v) {
        heap[++heapSize] = v;
        int current = heapSize;
        while (current > 1) {
            int parent = current >> 1;
            if (heap[current] < heap[parent]) {
                int q = heap[current];
                heap[current] = heap[parent];
                heap[parent] = q;
                current = parent;
            } else {
                break;
            }
        }
    }

    public void insert(Request request) {
        int v = request.v;
        sequentialInsert(v);
    }

    private ReentrantLock lock = new ReentrantLock();

    public void sleep() {
        BlackHole.consumeCPU(300);
    }

    public void handleRequest(Request request) {
        fc.addRequest(request);
        while (true) {
            if (lock.tryLock()) { // I'm the leader
                fc.addRequest(request);

                for (int t = 0; t < TRIES; t++) {
                    FCArray.FCRequest[] requests = fc.loadRequests();

                    if (heapSize + requests.length >= heap.length) { // Increase heap size
                        int[] newHeap = new int[2 * heap.length];
                        for (int i = 1; i <= heapSize; i++) {
                            newHeap[i] = heap[i];
                        }
                        heap = newHeap;
                    }

                    int length = requests.length;
                    for (int i = 0; i < requests.length; i++) {
                        FCArray.FCRequest r = requests[i];
                        if (r == null) {
                            length = i;
                            break;
                        }
                        if (((Request) r).type == OperationType.DELETE_MIN) {
                            remove((Request) r);
                        } else {
                            insert((Request) r);
                        }
                        ((Request) r).status = Status.FINISHED;
                    }

                    fc.cleanup();
                    if (length < THRESHOLD) {
                        break;
                    }
                }
                lock.unlock();
                return;
            } else {
                while (request.status == Status.PUSHED && lock.isLocked()) {
                    sleep();
                    fc.addRequest(request);
                }
                if (request.status == Status.FINISHED) {
                    return;
                }
            }
        }
    }

    public int deleteMin() {
        Request request = getLocalRequest();
        request.set(OperationType.DELETE_MIN, -1); // I assume that the insert value for delete min is -1
        handleRequest(request);
        return request.v;
    }

    public void insert(int v) {
        Request request = getLocalRequest();
        request.set(OperationType.INSERT, v); // I assume that the inserted values are >= 0
        handleRequest(request);
    }


    public void clear() {
        fc = new FCArray(threads);
        for (int i = 0; i < heapSize; i++) {
            heap[i + 1] = 0;
        }
        heapSize = 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 1; i <= heapSize; i++) {
            if (i != 1)
                sb.append(", ");
            sb.append("" + heap[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
