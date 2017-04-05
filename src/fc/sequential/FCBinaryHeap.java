package fc.sequential;

import abstractions.Heap;
import fc.FC;
import fc.FCRequest;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Created by vaksenov on 24.03.2017.
 */
public class FCBinaryHeap implements Heap {
    private FC fc;
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

    public class Request extends FCRequest implements Comparable<Request> {
        volatile OperationType type;
        volatile int v;

        public Request() {
            status = Status.PUSHED;
        }

        public void set(OperationType operationType) {
            status = Status.PUSHED;
            this.type = operationType;
        }

        public void set(OperationType operationType, int value) {
            set(operationType);
            this.v = value;
        }

        public int compareTo(Request request) {
            return Integer.compare(v, request.v);
        }

        volatile Status status;
        volatile boolean leader;

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

    public class Node {
        volatile int v;

        public Node(int v) {
            this.v = v;
        }
    }

    private Node[] heap;
    private int heapSize;

    public FCBinaryHeap(int size, int numThreads) {
        fc = new FC();
        size = Integer.highestOneBit(size) * 4;
        heap = new Node[size];
        TRIES = numThreads;
        THRESHOLD = (int) Math.ceil(1. * numThreads / 1.7);
    }

    public void remove(Request request) {
        request.v = heap[1].v;
        request.status = Status.FINISHED;
        heap[1].v = heap[heapSize--].v;
        int current = 1;
        while (2 * current <= heapSize) { // While there exists at least one child in heap
            int leftChild = 2 * current;
            int rightChild = 2 * current + 1;
            if (heap[current].v <= heap[leftChild].v
                    && (rightChild > heapSize || heap[current].v <= heap[rightChild].v)) { // I'm better than children and could finish
                return;
            }
            int swap = rightChild > heapSize || heap[leftChild].v < heap[rightChild].v ? leftChild : rightChild; // With whom to swap
            int tmp = heap[current].v;
            heap[current].v = heap[swap].v;
            heap[swap].v = tmp;

            current = swap;
        }
    }

    public void sequentialInsert(int v) {
        heap[++heapSize] = new Node(v);
        int current = heapSize;
        while (current > 1) {
            if (heap[current].v < heap[current / 2].v) {
                int q = heap[current].v;
                heap[current].v = heap[current / 2].v;
                heap[current / 2].v = q;
                current /= 2;
            } else {
                break;
            }
        }
    }

    public void insert(Request request) {
        int v = request.v;
        request.status = Status.FINISHED;
        sequentialInsert(v);
    }

    public void handleRequest(Request request) {
        fc.addRequest(request);
        while (true) {
            if (fc.tryLock()) { // I'm the leader
                fc.addRequest(request);

                for (int t = 0; t < TRIES; t++) {
                    FCRequest[] requests = fc.loadRequests();

                    if (heapSize + requests.length >= heap.length) { // Increase heap size
                        Node[] newHeap = new Node[2 * heap.length];
                        for (int i = 1; i <= heapSize; i++) {
                            newHeap[i] = heap[i];
                        }
                        heap = newHeap;
                    }

                    for (int i = 0; i < requests.length; i++) {
                        if (((Request) requests[i]).type == OperationType.DELETE_MIN) {
                            remove(request);
                        } else {
                            insert(request);
                        }
                        ((Request)requests[i]).status = Status.FINISHED;
                    }

                    fc.cleanup();
                    if (requests.length < THRESHOLD) {
                        break;
                    }
                }

                request.leader = false;
                leaderExists = false;
                fc.unlock();
                return;
            } else {
                while (request.status == Status.PUSHED && fc.isLocked()) {
//                    try {
//                        Thread.sleep(1);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
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
        fc = new FC();
        for (int i = 0; i < heapSize; i++) {
            heap[i + 1] = null;
        }
        heapSize = 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 1; i <= heapSize; i++) {
            if (i != 1)
                sb.append(", ");
            sb.append("" + heap[i].v);
        }
        sb.append("]");
        return sb.toString();
    }
}
