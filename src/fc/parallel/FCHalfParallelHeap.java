package fc.parallel;

import abstractions.Heap;
import fc.FC;
import fc.FCRequest;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Created by vaksenov on 24.03.2017.
 */
public class FCHalfParallelHeap implements Heap {
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
        SIFT_DELETE,
        SIFT_INSERT,
        FINISHED
    }

    public class Request extends FCRequest implements Comparable<Request> {
        volatile OperationType type;
        volatile int v;

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
        volatile boolean leader;

        public boolean holdsRequest() {
            return status != Status.FINISHED;
        }

        // Information for sift
        volatile int siftStart; // start position of sift down for insert and delete
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

        volatile boolean underProcessing;

        public Node(int v) {
            this.v = v;
        }
    }

    private Node[] heap;
    private int heapSize;

    public FCHalfParallelHeap(int size, int numThreads) {
        fc = new FC();
        size = Integer.highestOneBit(size) * 4;
        heap = new Node[size];
        for (int i = 0; i < heap.length; i++) {
            heap[i] = new Node(Integer.MAX_VALUE);
        }
        TRIES = numThreads;//3;
        THRESHOLD = (int) (Math.ceil(numThreads / 1.7));
    }

    public void siftDown(Request request) {
        int current = request.siftStart;
        if (current == 0) {
            request.status = Status.FINISHED;
            return;
        }
        int to = heapSize >> 1;
        while (current <= to) { // While there exists at least one child in heap
            int leftChild = current << 1;
            while (heap[leftChild].underProcessing) {
            }
            int rightChild = leftChild + 1;
            if (rightChild <= heapSize) {
                while (heap[rightChild].underProcessing) {
                }
            }

            if (heap[current].v <= heap[leftChild].v
                    && (rightChild > heapSize || heap[current].v <= heap[rightChild].v)) { // I'm better than children and could finish
                heap[current].underProcessing = false;
                request.status = Status.FINISHED;
                return;
            }
            int swap = rightChild > heapSize || heap[leftChild].v < heap[rightChild].v ? leftChild : rightChild; // With whom to swap
            heap[swap].underProcessing = true;
            int tmp = heap[current].v;
            heap[current].v = heap[swap].v;
            heap[swap].v = tmp;

            heap[current].underProcessing = false;
            current = swap;
        }
        heap[current].underProcessing = false;
        request.status = Status.FINISHED;
    }

    public void insert(Request request) {
        int v = request.v;
        request.status = Status.FINISHED;
        sequentialInsert(v);
    }

    volatile FCRequest[] loadedRequests;

    public void sleep() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void handleRequest(Request request) {
        fc.addRequest(request);
        while (request.leader || request.holdsRequest()) {
            if (!leaderExists) {
                if (fc.tryLock()) {
                    leaderExists = true;
                    request.leader = true;
                }
            }

            if (request.leader && request.status == Status.PUSHED) { // I'm the leader
                fc.addRequest(request);

                for (int t = 0; t < TRIES; t++) {
                    FCRequest[] requests = loadedRequests == null ? fc.loadRequests() : loadedRequests;

                    if (requests[0] == null) {
                        fc.cleanup();
                        break;
                    }

                    if (request.status == Status.FINISHED) {
                        request.leader = false;
                        int search = 0;

                        for (int i = 0; i < requests.length; i++) {
                            FCRequest r = requests[i];
                            if (r == null) {
                                break;
                            }
                            if (((Request) r).type == OperationType.DELETE_MIN) {
                                search = i;
                                break;
                            }
                        }
                        loadedRequests = requests;
                        ((Request) requests[search]).leader = true;
                        return;
                    }
                    loadedRequests = null;

                    int deleteSize = 0;
                    int length = requests.length;
                    for (int i = 0; i < requests.length; i++) {
                        FCRequest r = requests[i];
                        if (r == null) {
                            length = i;
                            break;
                        }
                        deleteSize += ((Request) r).type == OperationType.DELETE_MIN ? 1 : 0;
                    }

                    Request[] deleteRequests = new Request[deleteSize];
                    Request[] insertRequests = new Request[length - deleteSize];
                    deleteSize = 0;
                    for (int i = 0; i < length; i++) {
                        if (((Request) requests[i]).type == OperationType.DELETE_MIN) {
                            deleteRequests[deleteSize++] = (Request) requests[i];
                        } else {
                            insertRequests[i - deleteSize] = (Request) requests[i];
                        }
                    }

                    if (heapSize + insertRequests.length >= heap.length) { // Increase heap size
                        Node[] newHeap = new Node[2 * heap.length];
                        for (int i = 1; i <= heapSize; i++) {
                            newHeap[i] = heap[i];
                        }
                        for (int i = heapSize + 1; i < newHeap.length; i++) {
                            newHeap[i] = new Node(Integer.MAX_VALUE);
                        }
                        heap = newHeap;
                    }

                    int insertStart = 0;

                    if (deleteRequests.length > 0) { // Prepare for delete minimums
                        PriorityQueue<Integer> pq = new PriorityQueue<>((l, r) -> {
                            return Integer.compare(heap[l].v, heap[r].v);
                        });
                        // Looking for elements to remove
                        int[] kbest = new int[deleteRequests.length];
                        pq.add(1); // The root should be removed
                        for (int i = 0; i < deleteRequests.length; i++) {
                            int node = pq.poll();
                            kbest[i] = node;
                            heap[node].underProcessing = true;
                            deleteRequests[i].siftStart = 0; // initialize start position of sift

                            if (2 * node <= heapSize) {
                                pq.add(2 * node);
                            }
                            if (2 * node + 1 <= heapSize) {
                                pq.add(2 * node + 1);
                            }
                        }
                        Arrays.sort(kbest);
                        for (int i = 0; i < deleteRequests.length; i++) {
                            int node = kbest[i];
                            deleteRequests[i].v = heap[node].v;

                            if (node >= heapSize - 1) { // We are the last or way later, then do nothing
                                if (node != heapSize - 1) {
                                    heap[node].underProcessing = false;
                                    continue;
                                } else if (i >= insertRequests.length) { // We are last and there is no inserts left
                                    heap[node].underProcessing = false;
                                    heapSize--;
                                    continue;
                                }
                            }

                            if (insertStart < insertRequests.length) { // We could add insert some values right now
                                heap[node].v = insertRequests[insertStart].v;
                                insertRequests[insertStart++].status = Status.FINISHED;
                            } else {
                                while (heap[heapSize].underProcessing) { // We should swap only with unprocessed vertices
                                    heap[heapSize].underProcessing = false;
                                    heapSize--;
                                }
                                if (node >= heapSize - 1) { // If we again are last or already out then do nothing
                                    if (node == heapSize - 1) {
                                        heapSize--;
                                    }
                                    heap[node].underProcessing = false;
                                    continue;
                                }
                                heap[node].v = heap[heapSize--].v;
                                heap[heapSize + 1].v = Integer.MAX_VALUE;
                            }
                            deleteRequests[i].siftStart = node;
                        }
                        for (int i = 0; i < deleteRequests.length; i++) {
                            deleteRequests[i].status = Status.SIFT_DELETE;
                        }
                        if (request.status == Status.SIFT_DELETE) { // I have to delete too
                            siftDown(request);
                        }
                        for (int i = 0; i < deleteRequests.length; i++) { // Wait for everybody to finish
                            while (deleteRequests[i].status == Status.SIFT_DELETE) {
//                                sleep();
                            }
                        }
                    }

                    if (insertStart < insertRequests.length) { // There are insert requests left
                        for (int i = 0; i < insertRequests.length - insertStart; i++) {
                            insert(insertRequests[i + insertStart]);
                        }
                    }

                    fc.cleanup();
//                    if (requests.length < THRESHOLD) {
//                        break;
//                    }
//                    if (!request.leader) {
//                        leaderInTransition = false;
//                        return;
//                    }
                }

//                leaderInTransition = false;
                request.leader = false;
                leaderExists = false;
                fc.unlock();
            } else {
                while (request.status == Status.PUSHED && !request.leader && leaderExists) {
                    fc.addRequest(request);
//                    sleep();
                }
                if (request.status == Status.PUSHED) { // Someone set me as a leader or leader does not exist
                    continue;
                }
                if (request.status == Status.SIFT_DELETE) { // should know the node for sift down
                    siftDown(request);
                } else if (request.status == Status.SIFT_INSERT) { // I should make a sift up
                    while (request.status != Status.FINISHED) {}
                }
                if (!request.leader) {
                    return;
                }
            }
        }
    }

    public int deleteMin() {
//        System.err.println("Delete min");
        Request request = getLocalRequest();
        request.set(OperationType.DELETE_MIN, -1); // I assume that the insert value for delete min is -1
        handleRequest(request);
        return request.v;
    }

    public void insert(int v) {
//        System.err.println("Insert " + v);
        Request request = getLocalRequest();
        request.set(OperationType.INSERT, v); // I assume that the inserted values are >= 0
        handleRequest(request);
    }

    public void sequentialInsert(int v) {
        heap[++heapSize].v = v;
        int current = heapSize;
//        System.out.println(current);
        while (current > 1) {
            if (heap[current].v < heap[current >> 1].v) {
                int q = heap[current].v;
                heap[current].v = heap[current >> 1].v;
                heap[current >> 1].v = q;
                current >>= 1;
            } else {
                break;
            }
        }
    }

    public void clear() {
        fc = new FC();
        for (int i = 0; i < heapSize; i++) {
            heap[i + 1].v = Integer.MAX_VALUE;
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
