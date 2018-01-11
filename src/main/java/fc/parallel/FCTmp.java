package fc.parallel;

import abstractions.Heap;
import fc.FC;
import fc.FCRequest;

import java.util.Arrays;
import java.util.PriorityQueue;

/**
 * Created by vaksenov on 24.03.2017.
 */
public class FCTmp implements Heap {
    private FC fc;
    private ThreadLocal<Request> allocatedRequests = new ThreadLocal<>();
    private volatile boolean leaderExists;
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
        int v;

        public Node(int v) {
            this.v = v;
        }
    }

    private Node[] heap;
    private int heapSize;

    public FCTmp(int size, int numThreads) {
        fc = new FC();
        size = Integer.highestOneBit(size) * 4;
        heap = new Node[size];
        TRIES = numThreads;//3;
        THRESHOLD = (int) (Math.ceil(numThreads / 1.7));
    }

    public void siftDown(Request request) {
        request.status = Status.FINISHED;
    }

    public void insert(Request request) {
        request.status = Status.FINISHED;
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

            assert !request.leader || request.status == Status.PUSHED;

            if (request.leader && request.status == Status.PUSHED) {
                fc.addRequest(request);

                for (int t = 0; t < TRIES; t++) {
                    FCRequest[] requests = loadedRequests == null ? fc.loadRequests() : loadedRequests;

                    if (requests.length == 0) {
                        fc.cleanup();
                        break;
                    }

                    if (request.status == Status.FINISHED) {
                        request.leader = false;
                        int search = 0;

                        for (int i = 0; i < requests.length; i++) {
                            assert ((Request) requests[i]).status == Status.PUSHED;
                            if (((Request) requests[i]).type == OperationType.INSERT) {
                                search = i;
                                break;
                            }
                        }
                        loadedRequests = requests;
                        ((Request) requests[search]).leader = true;
                        return;
                    }
                    loadedRequests = null;
                    assert request.status == Status.PUSHED;

                    int deleteSize = 0;
                    for (int i = 0; i < requests.length; i++) {
                        assert ((Request) requests[i]).status == Status.PUSHED;
                        deleteSize += ((Request) requests[i]).type == OperationType.DELETE_MIN ? 1 : 0;
                    }

                    Request[] deleteRequests = new Request[deleteSize];
                    Request[] insertRequests = new Request[requests.length - deleteSize];
                    deleteSize = 0;
                    for (int i = 0; i < requests.length; i++) {
                        assert ((Request) requests[i]).status == Status.PUSHED;
                        if (((Request) requests[i]).type == OperationType.DELETE_MIN) {
                            deleteRequests[deleteSize++] = (Request) requests[i];
                        } else {
                            insertRequests[i - deleteSize] = (Request) requests[i];
                        }
                    }

                    for (FCRequest request1 : deleteRequests) {
                        Request r = (Request) request1;
                        r.status = Status.SIFT_DELETE;
                    }

                    if (request.status == Status.SIFT_DELETE) {
                        siftDown(request);
                    }

                    for (FCRequest request1 : deleteRequests) {
                        Request r = (Request) request1;
                        while (r.status == Status.SIFT_DELETE) {
                        }
                    }

                    for (FCRequest request1 : insertRequests) {
                        Request r = (Request) request1;
                        r.status = Status.SIFT_INSERT;
                    }

                    if (request.status == Status.SIFT_INSERT) {
                        insert(request);
                    }
                    assert request.status == Status.FINISHED;

                    for (FCRequest request1 : insertRequests) {
                        Request r = (Request) request1;
                        while (r.status == Status.SIFT_INSERT) {
                        }
                    }

                    fc.cleanup();
                }

                assert request.status == Status.FINISHED;
                request.leader = false;
                leaderExists = false;
                fc.unlock();
            } else {
                while (request.status == Status.PUSHED && !request.leader && leaderExists) {
                    fc.addRequest(request);
                }
                if (request.status == Status.PUSHED) { // Someone set me as a leader or leader does not exist
                    continue;
                }
                if (request.status == Status.SIFT_DELETE) { // should know the node for sift down
                    siftDown(request);
                } else if (request.status == Status.SIFT_INSERT) { // I should make a sift up
                    insert(request);
                }
                if (request.status != Status.FINISHED) {
                    System.err.println(request.status);
                }
                assert request.status == Status.FINISHED;
                return;
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
        if (heap[++heapSize] == null) {
            heap[heapSize] = new Node(Integer.MAX_VALUE);
        }
        heap[heapSize].v = v;
        int current = heapSize;
//        System.out.println(current);
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

    public void clear() {
        fc = new FC();
        for (int i = 0; i < heapSize; i++) {
            heap[i + 1].v = Integer.MAX_VALUE;
        }
        heapSize = 0;
        leaderExists = false;
        loadedRequests = null;
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
