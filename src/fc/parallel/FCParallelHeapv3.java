package fc.parallel;

import abstractions.Heap;
import fc.FC;
import fc.FCRequest;
import org.openjdk.jmh.logic.BlackHole;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Created by vaksenov on 24.03.2017.
 */
public class FCParallelHeapv3 implements Heap {
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

    private AtomicIntegerArray underProcessing;
    private AtomicReferenceArray<InsertInfo> insertInfo;
    private int[] v;
    private int heapSize;

    public FCParallelHeapv3(int size, int numThreads) {
        fc = new FC();
        size = Integer.highestOneBit(size) * 4;
        underProcessing = new AtomicIntegerArray(size);
        insertInfo = new AtomicReferenceArray<InsertInfo>(size);
        v = new int[size];
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
            while (underProcessing.get(leftChild) == 1) {
                sleep();
            }
            int rightChild = leftChild + 1;
            if (rightChild <= heapSize) {
                while (underProcessing.get(rightChild) == 1) {
                    sleep();
                }
            }

            if (v[current] <= v[leftChild]
                    && (rightChild > heapSize || v[current] <= v[rightChild])) { // I'm better than children and could finish
                underProcessing.lazySet(current, 0);
                request.status = Status.FINISHED;
                return;
            }
            int swap = rightChild > heapSize || v[leftChild] < v[rightChild] ? leftChild : rightChild; // With whom to swap
            underProcessing.set(swap, 1);
            int tmp = v[current];
            v[current] = v[swap];
            v[swap] = tmp;

            underProcessing.set(current, 0);
            current = swap;
        }
        underProcessing.lazySet(current, 0);
        request.status = Status.FINISHED;
    }

    public void insert(Request request) {
        int current = request.siftStart;
//        System.err.println("Wait on: " + current);
        while (insertInfo.get(current) == null) {
            sleep();
        } // Wait for someone to wake up us

        InsertInfo insertInfo = this.insertInfo.get(current);
        this.insertInfo.set(current, null);
        while (!insertInfo.finished()) {
//            System.err.println(current + " " + insertInfo.lrange + " " + insertInfo.rrange + " " + insertInfo.lneed + " " + insertInfo.rneed);
            v[current] = insertInfo.replaceMinFromHeap(v[current]); // Replace current value
            if (underProcessing.get(current) == 1) { // Then I should split the work and give the right child new info
//                System.err.println("Split on " + current);
//                heap[current].underProcessing = false;
                InsertInfo toRight = insertInfo.split();
                toRight.slideToRight();
                this.insertInfo.lazySet((current << 1) + 1, toRight); // Give info to the right child
                underProcessing.set(current, 0);

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
        v[current] = insertInfo.replaceMinFromHeap(Integer.MAX_VALUE); // The last insert position
        request.status = Status.FINISHED;
    }

    volatile FCRequest[] loadedRequests;

    public void sleep() {
        BlackHole.consumeCPU(25);
    }

    public class InnerHeap {
        int[] a;
        int size = 0;

        public InnerHeap(int size) {
            a = new int[size + 1];
        }

        public void insert(int x) {
            a[++size] = x;
            int w = size;
            while (w > 1) {
                int p = w >> 1;
                if (v[a[w]] < v[a[p]]) {
                    int q = a[w];
                    a[w] = a[p];
                    a[p] = q;
                }
                w = p;
            }
        }

        public int extractMin() {
            int ans = a[1];
            a[1] = a[size--];
            int w = 1;
            while (w <= (size >> 1)) {
                int left = w << 1;
                int right = left + 1;
                if (right <= size && v[a[left]] > v[a[right]]) {
                    left = right;
                }
                if (v[a[w]] > v[a[left]]) {
                    int q = a[left];
                    a[left] = a[w];
                    a[w] = q;
                    w = left;
                } else {
                    break;
                }
            }
            return ans;
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

                    if (heapSize + insertRequests.length >= v.length) { // Increase heap size
                        int[] newV = new int[2 * v.length];
                        AtomicIntegerArray newUnderProcessing = new AtomicIntegerArray(2 * v.length);
                        AtomicReferenceArray<InsertInfo> newInsertInfo = new AtomicReferenceArray<>(2 * v.length);

                        for (int i = 1; i <= heapSize; i++) {
                            newV[i] = v[i];
                            newUnderProcessing.lazySet(i, underProcessing.get(i));
                            newInsertInfo.lazySet(i, insertInfo.get(i));
                        }
//                        for (int i = heapSize + 1; i < newHeap.length; i++) {
//                            newHeap[i] = new Node(Integer.MAX_VALUE);
//                        }
                        v = newV;
                        underProcessing = newUnderProcessing;
                        insertInfo = newInsertInfo;
                    }

//                    if (insertRequests.length > 0) {
//                        Arrays.sort(insertRequests);
//                    }

                    int insertStart = 0;

                    if (deleteRequests.length > 0) { // Prepare for delete minimums
                        InnerHeap pq = new InnerHeap(2 * deleteRequests.length);
                        // Looking for elements to remove
                        int[] kbest = new int[deleteRequests.length];
                        pq.insert(1); // The root should be removed
                        for (int i = 0; i < deleteRequests.length; i++) {
                            int node = pq.extractMin();
                            kbest[i] = node;
                            underProcessing.lazySet(node, 1);
                            deleteRequests[i].siftStart = 0; // initialize start position of sift

                            if (2 * node <= heapSize) {
                                pq.insert(2 * node);
                            }
                            if (2 * node + 1 <= heapSize) {
                                pq.insert(2 * node + 1);
                            }
                        }
                        Arrays.sort(kbest);
                        for (int i = 0; i < deleteRequests.length; i++) {
                            int node = kbest[i];
                            deleteRequests[i].v = v[node];

                            if (node >= heapSize - 1) { // We are the last or way later, then do nothing
                                if (node != heapSize - 1) {
                                    underProcessing.lazySet(node, 0);
                                    continue;
                                } else if (i >= insertRequests.length) { // We are last and there is no inserts left
                                    underProcessing.lazySet(node, 0);
                                    heapSize--;
                                    continue;
                                }
                            }

                            if (insertStart < insertRequests.length) { // We could add insert some values right now
                                v[node] = insertRequests[insertStart].v;
                                insertRequests[insertStart++].status = Status.FINISHED;
                            } else {
                                while (underProcessing.get(heapSize) == 1) { // We should swap only with unprocessed vertices
                                    underProcessing.lazySet(heapSize, 0);
                                    heapSize--;
                                }
                                if (node >= heapSize - 1) { // If we again are last or already out then do nothing
                                    if (node == heapSize - 1) {
                                        heapSize--;
                                    }
                                    underProcessing.lazySet(node, 0);
                                    continue;
                                }
                                v[node] = v[heapSize--];
                                v[heapSize + 1] = Integer.MAX_VALUE; // remove value
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
//                            if (heap[i + heapSize + 1] == null) {
//                                heap[i + heapSize + 1] = new Node(Integer.MAX_VALUE); // already in the tree
//                            }
                        }

                        int lstart = Integer.highestOneBit(heapSize + 1);
                        insertInfo.lazySet(1, new InsertInfo(orderedValues, 0, orderedValues.length,
                                null, null, 0,
                                lstart, 2 * lstart, heapSize + 1, heapSize + orderedValues.length + 1));

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

                            underProcessing.lazySet(lca, 1);
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

    public void sequentialInsert(int w) {
//        if (heap[++heapSize] == null) {
//            heap[heapSize] = new Node(Integer.MAX_VALUE);
//        }
        v[++heapSize] = w;
        int current = heapSize;
//        System.out.println(current);
        while (current > 1) {
            if (v[current] < v[current / 2]) {
                int q = v[current];
                v[current] = v[current / 2];
                v[current / 2] = q;
                current /= 2;
            } else {
                break;
            }
        }
    }

    public void clear() {
        fc = new FC();
        for (int i = 0; i < heapSize; i++) {
            v[i + 1] = Integer.MAX_VALUE;
        }
        heapSize = 0;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 1; i <= heapSize; i++) {
            if (i != 1)
                sb.append(", ");
            sb.append("" + v[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
