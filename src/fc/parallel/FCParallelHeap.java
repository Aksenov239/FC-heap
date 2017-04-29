package fc.parallel;

import abstractions.Heap;
import fc.FC;
import fc.FCRequest;
import org.openjdk.jmh.logic.BlackHole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.PriorityQueue;

/**
 * Created by vaksenov on 24.03.2017.
 */
public class FCParallelHeap implements Heap {
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
        volatile boolean leader;

        public boolean holdsRequest() {
            return status != Status.FINISHED;
        }

        // Information for sift
        int siftStart; // start position of sift down for insert and delete

        // Information for siftUp
        // volatile int sizeOfList;
        // volatile List headList;
        // volatile List insertPosition;
    }

    private Request getLocalRequest() {
        Request request = allocatedRequests.get();
        if (request == null) {
            request = new Request();
            allocatedRequests.set(request);
        }
        return request;
    }

    public class List {
        int value;
        List next = null;

        public List(int value) {
            this.value = value;
        }

        public int length() {
            int size = 0;
            List now = this;
            while (now != null) {
                size++;
                now = now.next;
            }
            return size;
        }
    }

    public class InsertInfo {
        List[] orderedValues;
        int left, right; // Begin and end of captured positions in orderedValues
        List headFromHeap;
        List tailFromHeap;
        int listLength;

        int lrange, rrange; // current range of the info, excluding rrange

        int lneed, rneed; // known range of inserted values, excluding rneed

        public InsertInfo(List[] orderedValues, int left, int right,
                          List headFromHeap, List tailFromHeap, int listLength,
                          int lrange, int rrange, int lneed, int rneed) {
            if (left < right) {
                this.orderedValues = orderedValues;
            }
            this.left = left;
            this.right = right;
            this.headFromHeap = headFromHeap;
            this.tailFromHeap = tailFromHeap;
            this.listLength = listLength;

            this.lrange = lrange;
            this.rrange = rrange;
            this.lneed = lneed;
            this.rneed = rneed;
        }

        public int replaceMinFromHeap(int v) {
            int bestOrdered = orderedValues != null ? orderedValues[left].value : Integer.MAX_VALUE;
            int bestFromHeap = headFromHeap != null ? headFromHeap.value : Integer.MAX_VALUE;
            if (Math.min(bestOrdered, bestFromHeap) >= v) { // The given value is the best
                return v;
            }
            if (bestOrdered < bestFromHeap) { // The first ordered value should be in heap and we replace it
                int res = orderedValues[left].value;
                orderedValues[left].value = v;
                if (headFromHeap != null) {
                    tailFromHeap.next = orderedValues[left];
                    tailFromHeap = tailFromHeap.next;
                } else {
                    headFromHeap = tailFromHeap = orderedValues[left];
                }
                left++;
                if (left == right) {
                    orderedValues = null;
                }
                listLength++;
//                assert headFromHeap.length() == listLength;
                return res;
            } else { // The first values from heap should be now back and we replace it
                int res = headFromHeap.value;
                assert headFromHeap.length() == listLength;
                if (tailFromHeap != headFromHeap) {
                    headFromHeap.value = v;
                    tailFromHeap.next = headFromHeap;
                    List tmp = headFromHeap.next;
                    headFromHeap.next = null;
                    tailFromHeap = headFromHeap;
                    headFromHeap = tmp;
                } else {
                    headFromHeap.value = v;
                }
//                assert headFromHeap.length() == listLength;
                return res;
            }
        }

        private int intersectionLeft() {
            int l = lrange;
            int r = (lrange + rrange) >> 1;
            if (Math.max(l, lneed) < Math.min(r, rneed)) { // Intersects
                return Math.min(r, rneed) - Math.max(l, lneed);
            }
            if (2 * l < rneed) { //Probably, intersection is on next layer
                if (l != r) { // are we on the last level
                    l = l << 1;
                    r = r << 1;
                } else {
                    l = l << 1;
                    r = (r << 1) + 1;
                }
                if (Math.max(l, lneed) < Math.min(r, rneed)) {
                    return Math.min(r, rneed) - Math.max(l, lneed);
                }
            }
            return 0; // Do not intersect
        }

        public InsertInfo split() {
            int toLeft = intersectionLeft();
//            assert toLeft < right - left + listLength;
            if (right - left >= toLeft) { // We leave ourselves part of array
                InsertInfo insertInfo = new InsertInfo(orderedValues, left + toLeft, right,
                        headFromHeap, tailFromHeap, listLength,
                        lrange, rrange, lneed, rneed);
                right = left + toLeft;
                headFromHeap = null;
                tailFromHeap = null;
                listLength = 0;
                return insertInfo;
            }
            int toRight = right - left + listLength - toLeft;
            if (right - left >= toRight) { // We give to new part of array
                InsertInfo insertInfo = new InsertInfo(orderedValues, left, left + toRight,
                        null, null, 0,
                        lrange, rrange, lneed, rneed);
                left = left + toRight;
                if (left == right) {
                    orderedValues = null;
                }
                return insertInfo;
            }
            // Split list
            if (toLeft < toRight) {
                List splitPosition = headFromHeap;
                for (int i = 0; i < toLeft - (right - left) - 1; i++) {
                    splitPosition = splitPosition.next;
                }
                InsertInfo insertInfo = new InsertInfo(null, 0, 0,
                        splitPosition.next, tailFromHeap, toRight,
                        lrange, rrange, lneed, rneed);
                tailFromHeap = splitPosition;
                splitPosition.next = null;
                listLength = toLeft - (right - left);
//                assert headFromHeap.length() == listLength;
//                assert insertInfo.headFromHeap.length() == toRight;
                return insertInfo;
            } else {
                List splitPosition = headFromHeap;
                for (int i = 0; i < toRight - (right - left) - 1; i++) {
                    splitPosition = splitPosition.next;
                }
                InsertInfo insertInfo = new InsertInfo(orderedValues, left, right,
                        headFromHeap, splitPosition, toRight - (right - left),
                        lrange, rrange, lneed, rneed);
                orderedValues = null;
                left = right = 0;
                headFromHeap = splitPosition.next;
                listLength = toLeft;
                splitPosition.next = null;
                return insertInfo;
            }
        }

        public boolean goToLeft() {
            return intersectionLeft() > 0;
        }

        public void slideToLeft() {
            int l = lrange;
            int r = rrange;
            if (l == r - 1) {
                lrange = l << 1;
                rrange = lrange + 1;
            } else {
                rrange = (l + r) >> 1;
            }

        }

        public void slideToRight() {
            int l = lrange;
            int r = rrange;
            if (l == r - 1) {
                lrange = (l << 1) + 1;
                rrange = lrange + 1;
            } else {
                lrange = (l + r) >> 1;
            }
        }

        public boolean finished() {
            return lrange == rrange - 1 && lneed <= lrange && lrange < rneed;
        }
    }

    public class Node {
        int v;

        volatile boolean underProcessing;

        volatile InsertInfo insertInfo; // Wake up thread to work on the right child

        public Node(int v) {
            this.v = v;
        }
    }

    private Node[] heap;
    private int heapSize;

    public FCParallelHeap(int size, int numThreads) {
        fc = new FC();
        size = Integer.highestOneBit(size) * 4;
        heap = new Node[size];
//        for (int i = 0; i < heap.length; i++) {
//            heap[i] = new Node(Integer.MAX_VALUE);
//        }
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
        int current = request.siftStart;
//        System.err.println("Wait on: " + current);
        while (heap[current].insertInfo == null) {
        } // Wait for someone to wake up us

        InsertInfo insertInfo = heap[current].insertInfo;
        heap[current].insertInfo = null;
        while (!insertInfo.finished()) {
//            System.err.println(current + " " + insertInfo.lrange + " " + insertInfo.rrange + " " + insertInfo.lneed + " " + insertInfo.rneed);
            heap[current].v = insertInfo.replaceMinFromHeap(heap[current].v); // Replace current value
            if (heap[current].underProcessing) { // Then I should split the work and give the right child new info
//                System.err.println("Split on " + current);
//                heap[current].underProcessing = false;
                InsertInfo toRight = insertInfo.split();
                toRight.slideToRight();
                heap[(current << 1) + 1].insertInfo = toRight; // Give info to the right child
                heap[current].underProcessing = false;

                insertInfo.slideToLeft();
                current = current << 1;
            } else {
//                System.err.println("Slide: " + current + " " + insertInfo.goToLeft());
                if (insertInfo.goToLeft()) {
                    insertInfo.slideToLeft();
                    current = current << 1;
                } else {
                    insertInfo.slideToRight();
                    current = (current << 1) + 1;
                }
            }
//            System.err.println("Current: " + current);
        }
//        assert insertInfo.lneed <= current && current < insertInfo.rneed;
        heap[current].v = insertInfo.replaceMinFromHeap(Integer.MAX_VALUE); // The last insert position
        request.status = Status.FINISHED;
    }

    volatile FCRequest[] loadedRequests;

    public void sleep() {
//        try {
//            Thread.sleep(1);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        BlackHole.consumeCPU(10);
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

            if (request.leader && request.status == Status.PUSHED) {
//                    (request.status == Status.PUSHED || request.status == Status.FINISHED)) { // I'm the leader
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

                    int deleteSize = 0;
                    for (int i = 0; i < requests.length; i++) {
//                        assert ((Request) requests[i]).status == Status.PUSHED;
                        deleteSize += ((Request) requests[i]).type == OperationType.DELETE_MIN ? 1 : 0;
                    }

                    Request[] deleteRequests = new Request[deleteSize];
                    Request[] insertRequests = new Request[requests.length - deleteSize];
//                    deleteSize = 0;
//                    for (int i = 0; i < requests.length; i++) {
//                        deleteSize += ((Request) requests[i]).type == OperationType.DELETE_MIN ? 1 : 0;
//                    }
//                    assert deleteSize == deleteRequests.length;

                    deleteSize = 0;
                    for (int i = 0; i < requests.length; i++) {
//                        assert requests[i].holdsRequest();
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
//                        for (int i = heapSize + 1; i < newHeap.length; i++) {
//                            newHeap[i] = new Node(Integer.MAX_VALUE);
//                        }
                        heap = newHeap;
                    }

                    if (insertRequests.length > 0) {
                        Arrays.sort(insertRequests);
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
                            deleteRequests[i].siftStart = 0; // initialize start position of sift
                            heap[node].underProcessing = true;

                            if (2 * node <= heapSize) {
                                pq.add(node << 1);
                            }
                            if (2 * node + 1 <= heapSize) {
                                pq.add((node << 1) + 1);
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
                                heap[heapSize + 1].v = Integer.MAX_VALUE; // remove value
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
                                sleep();
                            }
                        }
                    }

                    if (insertStart < insertRequests.length) { // There are insert requests left
                        // give the work to thread from root
                        insertRequests[insertStart].siftStart = 1;

                        List[] orderedValues = new List[insertRequests.length - insertStart];
                        for (int i = 0; i < orderedValues.length; i++) {
                            orderedValues[i] = new List(insertRequests[i + insertStart].v);
                            if (heap[i + heapSize + 1] == null) {
                                heap[i + heapSize + 1] = new Node(Integer.MAX_VALUE); // already in the tree
                            }
                        }

                        int lstart = Integer.highestOneBit(heapSize + 1);
                        heap[1].insertInfo = new InsertInfo(orderedValues, 0, orderedValues.length,
                                null, null, 0,
                                lstart, 2 * lstart, heapSize + 1, heapSize + orderedValues.length + 1);

                        int id = 0;
                        for (int i = 1; i < orderedValues.length; i++) {
                            int left = i + heapSize;
                            int right = i + 1 + heapSize;
                            int lca = 0;
                            if (right == Integer.lowestOneBit(right)) { // We go to the next row
                                lca = 1;
                            } else {
                                lca = ~(left ^ right); // lca of i-th and (i-1)-th
                                lca = (i + heapSize) / Integer.lowestOneBit(lca);
                            }

//                            System.err.println("LCA: " + lca + " " + left + " " + right);

                            heap[lca].underProcessing = true;
                            insertRequests[i + insertStart].siftStart = 2 * lca + 1; // Start sift from the right child of lca
                        }

                        for (int i = insertStart; i < insertRequests.length; i++) {
                            insertRequests[i].status = Status.SIFT_INSERT;
                        }

                        heapSize = heapSize + orderedValues.length;

                        if (request.status == Status.SIFT_INSERT) {
                            insert(request);
                        }
                        for (int i = insertStart; i < insertRequests.length; i++) {
                            while (insertRequests[i].status == Status.SIFT_INSERT) {
                                sleep();
                            } // wait while finish
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
                    sleep();
                }
                if (request.status == Status.PUSHED) { // Someone set me as a leader or leader does not exist
                    continue;
                }
                if (request.status == Status.SIFT_DELETE) { // should know the node for sift down
                    siftDown(request);
                } else if (request.status == Status.SIFT_INSERT) { // I should make a sift up
                    insert(request);
                }
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
