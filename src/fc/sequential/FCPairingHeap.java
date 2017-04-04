package fc.sequential;

import abstractions.Heap;
import fc.FC;
import fc.FCRequest;

import java.util.ArrayList;

/**
 * Created by vaksenov on 24.03.2017.
 */
public class FCPairingHeap implements Heap {
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
        OperationType type;
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

        ArrayList<Node> children;

        public Node(int v) {
            this.v = v;
            children = new ArrayList<>();
        }

        public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append("(" + v + ", [");
                for (int i = 0; i < children.size(); i++) {
                    if (i != 0) {
                        sb.append(", ");
                    }
                    sb.append(children.get(i));
                }
            sb.append("])");
            return sb.toString();
        }
    }

    public Node merge(Node left, Node right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        if (left.v < right.v) {
            left.children.add(right);
            return left;
        } else {
            right.children.add(left);
            return right;
        }
    }

    public Node mergePairs(ArrayList<Node> nodes) {
        int l = 0;
        for (int i = 0; i < nodes.size(); i += 2) {
            nodes.set(l++, merge(nodes.get(i), i < nodes.size() - 1 ? nodes.get(i + 1) : null));
        }

        Node ans = null;
        for (int i = l - 1; i >= 0; i--) {
            ans = merge(ans, nodes.get(i));
        }

        return ans;
    }

    private Node heap;

    public FCPairingHeap(int size, int numThreads) {
        fc = new FC();
        size = Integer.highestOneBit(size) * 4;
        heap = null;
        TRIES = numThreads;
        THRESHOLD = (int) Math.ceil(1. * numThreads / 1.7);
    }

    public void remove(Request request) {
        request.v = heap.v;
        request.status = Status.FINISHED;
        heap = mergePairs(heap.children);
    }

    public void sequentialInsert(int v) {
        Node node = new Node(v);
        heap = merge(node, heap);
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

                    for (int i = 0; i < requests.length; i++) {
                        if (((Request) requests[i]).type == OperationType.DELETE_MIN) {
                            remove(request);
                        } else {
                            insert(request);
                        }
                        ((Request) requests[i]).status = Status.FINISHED;
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
        heap = null;
    }

    public String toString() {
        return heap == null ? "null" : heap.toString();
    }
}
