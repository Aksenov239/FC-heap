package clh;

import clh.FCCLH;
import org.openjdk.jmh.logic.BlackHole;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;
import java.util.Arrays;

/**
 * Created by vaksenov on 10.01.2018.
 */
public class CLHParallelHeapFlush {
    private FCCLH fc;
    private int threads;
    private ThreadLocal<Request> allocatedRequests = new ThreadLocal<>();

    private Request getLocalRequest() {
        Request request = allocatedRequests.get();
        if (request == null) {
            request = new Request();
            allocatedRequests.set(request);
        }
        return request;
    }

    private static final Unsafe unsafe;

    static {
        try {
            Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
            unsafeConstructor.setAccessible(true);
            unsafe = unsafeConstructor.newInstance();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

//    OperationType
//        DELETE_MIN -> false
//        INSERT -> true

//    public enum Status {
//        PUSHED, -> 0
//        SIFT_DELETE, -> 1
//        SIFT_INSERT, -> 2
//        FINISHED -> 3
//    }

    private static final int PUSHED = 0;
    private static final int SIFT_DELETE = 1;
    private static final int SIFT_INSERT = 2;
    private static final int FINISHED = 3;

    public class Request extends FCCLH.CLHRequest implements Comparable<Request> {
        boolean type;
        int v;

        public Request() {
            status = PUSHED;
        }

        public void set(boolean operationType) {
            this.type = operationType;
            status = PUSHED;
        }

        public void set(boolean operationType, int value) {
            this.v = value;
            set(operationType);
        }

        public int compareTo(Request request) {
            return Integer.compare(v, request.v);
        }

        int status;

        public boolean holdsRequest() {
            return status != FINISHED;
        }

        public boolean properStatus() {
            return status == FINISHED || status == PUSHED;
        }

        // Information for sift
        int siftStart; // start position of sift down for insert and delete

        // Information for siftUp
        // volatile int sizeOfList;
        // volatile List headList;
        // volatile List insertPosition;
    }

    public class List {
        int value;
        List next = null;

        public List(int value) {
            this.value = value;
        }

        public void set(int value) {
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
            if (Math.max(l, lneed) < Math.min(r, rneed)) { // Intersects on the same level
                return Math.min(r, rneed) - Math.max(l, lneed);
            }
//            if (2 * l < rneed) { //Probably, intersection is on next layer
//                if (l != r) { // are we on the last level
//                    l = l << 1;
//                    r = r << 1;
//                } else {
//                    l = l << 1;
//                    r = (r << 1) + 1;
//                }
//                if (Math.max(l, lneed) < Math.min(r, rneed)) {
//                    return Math.min(r, rneed) - Math.max(l, lneed);
//                }
//            }
            l = 2 * l;
            r = 2 * r;
            if (Math.max(l, lneed) < Math.min(r, rneed)) { // Intersects on the next level
                return Math.min(r, rneed) - Math.max(l, lneed);
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

        public boolean proper() { // [lneed, rneed) always intersects with [lrange, rrange)
            // Intersect on the same level
            if (Math.max(lneed, lrange) < Math.min(rneed, rrange)) {
                return true;
            }
            if (Math.max(lneed, 2 * lrange) < Math.min(rneed, 2 * rrange)) {
                return true;
            }
            return false;
        }
    }

    public class Node {
        int v;

        boolean underProcessing;

        InsertInfo insertInfo; // Wake up thread to work on the right child

        public Node(int v) {
            this.v = v;
        }
    }

    private Node[] heap;
    private int heapSize;

    final private Request[] insertRequests;
    final private Request[] deleteRequests;
    final private InnerHeap innerHeap;
    final private int[] kbest;
    final private List[] orderedValues;

    public CLHParallelHeapFlush(int size, int numThreads) {
        fc = new FCCLH(numThreads);
        threads = numThreads;
        size = Integer.highestOneBit(size) * 4;
        heap = new Node[size];
//        for (int i = 0; i < heap.length; i++) {
//            heap[i] = new Node(Integer.MAX_VALUE);
//        }

        insertRequests = new Request[numThreads];
        deleteRequests = new Request[numThreads];

        innerHeap = new InnerHeap(2 * numThreads + 1);
        kbest = new int[numThreads];
        orderedValues = new List[numThreads];
        for (int i = 0; i < orderedValues.length; i++) {
            orderedValues[i] = new List(0);
        }
    }

    public void siftDown(Request request) {
        int current = request.siftStart;
        if (current == 0) {
            request.status = FINISHED;
            unsafe.storeFence();
            return;
        }

        boolean at1 = current == 1;

        final int to = heapSize >> 1;
        while (current <= to) { // While there exists at least one child in heap
            final int leftChild = current << 1;
            unsafe.loadFence();
            while (heap[leftChild].underProcessing) {
                sleep();
                unsafe.loadFence();
            }
            final int rightChild = leftChild + 1;
            if (rightChild <= heapSize) {
                unsafe.loadFence();
                while (heap[rightChild].underProcessing) {
                    sleep();
                    unsafe.loadFence();
                }
            }

            final int swap = rightChild > heapSize || heap[leftChild].v < heap[rightChild].v ? leftChild : rightChild; // With whom to swap

            if (heap[current].v <= heap[swap].v) { // I'm better than children and could finish
                heap[current].underProcessing = false;

                unsafe.storeFence();
//                if (at1) {
//                    if (heap[1].underProcessing) {
//                        throw new AssertionError("Fuck on " + current);
//                    }
//                }

                request.status = FINISHED; // Ar first update the flag and then finish. Otherwise, we could
                // see the new underProcessing flag
                unsafe.storeFence();

                return;
            }

            heap[swap].underProcessing = true;
            // unsafe.storeFence(); // Probably need fence here
            int tmp = heap[current].v;
            heap[current].v = heap[swap].v;
            heap[swap].v = tmp;

            heap[current].underProcessing = false;
            current = swap;
            unsafe.storeFence();

//            if (at1) {
//                if (heap[1].underProcessing) {
//                    throw new AssertionError("Fuck on " + current);
//                }
//            }
        }

        heap[current].underProcessing = false;
        unsafe.storeFence();

//        unsafe.loadFence();
//        if (at1) {
//            if (heap[1].underProcessing) {
//                throw new AssertionError("Fuck on " + current);
//            }
//        }

        request.status = FINISHED;
        unsafe.storeFence();
    }

    public void insert(Request request) {
        int current = request.siftStart;
//        System.err.println("Wait on: " + current);
        if (current != 1) {
            unsafe.loadFence();
            while (heap[current >> 1].underProcessing) {
                // I'm not in the root and the parent has not split yet
                sleep();
                unsafe.loadFence();
            } // Wait for someone to wake up us
        }

        InsertInfo insertInfo = heap[current].insertInfo;
        heap[current].insertInfo = null;
        while (!insertInfo.finished()) {
//            System.err.println(current + " " + insertInfo.lrange + " " + insertInfo.rrange + " " + insertInfo.lneed + " " + insertInfo.rneed);
//            if (!insertInfo.proper()) {
//                System.err.println(current + " " + heapSize + " "
//                        + insertInfo.lrange + " " + insertInfo.rrange + " "
//                        + insertInfo.lneed + " " + insertInfo.rneed);
//                System.err.println("FTF?!");
//                System.exit(1);
//            }
            heap[current].v = insertInfo.replaceMinFromHeap(heap[current].v); // Replace current value
            if (heap[current].underProcessing) { // Then I should split the work and give the right child new info
//                System.err.println("Split on " + current);
//                heap[current].underProcessing = false;
                InsertInfo toRight = insertInfo.split();
                toRight.slideToRight();
//                if (!toRight.proper()) {
//                    System.err.println(current + " " + insertInfo.lneed + " " + insertInfo.rneed + " " +
//                            insertInfo.lrange + " " + insertInfo.rrange);
//                    throw new RuntimeException("Fuck");
//                }
                heap[(current << 1) + 1].insertInfo = toRight; // Give info to the right child
                heap[current].underProcessing = false;
                unsafe.storeFence();

                insertInfo.slideToLeft();
//                if (!insertInfo.proper()) {
//                    System.err.println(current + " " + insertInfo.lneed + " " + insertInfo.rneed + " " +
//                        insertInfo.lrange + " " + insertInfo.rrange);
//                    throw new RuntimeException("Fuck");
//                }
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
//                if (!insertInfo.proper()) {
//                    throw new RuntimeException("Fuck");
//                }

            }
//            System.err.println("Current: " + current);
        }
//        assert insertInfo.lneed <= current && current < insertInfo.rneed;
        heap[current].v = insertInfo.replaceMinFromHeap(Integer.MAX_VALUE); // The last insert position
        request.status = FINISHED;
        unsafe.storeFence();
    }

    FCCLH.CLHRequest[] loadedRequests;

    public void sleep() {
        BlackHole.consumeCPU(300);
    }

    public class InnerHeap {
        int[] a;
        int size = 0;

        public InnerHeap(int size) {
            a = new int[size + 1];
        }

        public void clear() {
            size = 0;
        }

        public void insert(int x) {
            a[++size] = x;
            int v = size;
            while (v > 1) {
                int p = v >> 1;
                if (heap[a[v]].v < heap[a[p]].v) {
                    int q = a[v];
                    a[v] = a[p];
                    a[p] = q;
                }
                v = p;
            }
        }

        public int extractMin() {
            int ans = a[1];
            a[1] = a[size--];
            int v = 1;
            while (v <= (size >> 1)) {
                int left = v << 1;
                int right = left + 1;
                if (right <= size && heap[a[left]].v > heap[a[right]].v) {
                    left = right;
                }
                if (heap[a[v]].v > heap[a[left]].v) {
                    int q = a[left];
                    a[left] = a[v];
                    a[v] = q;
                    v = left;
                } else {
                    break;
                }
            }
            return ans;
        }
    }

    public void handleRequest(Request request) {
        if (fc.addRequest(request)) {
            FCCLH.CLHRequest[] requests = fc.loadRequests();

            loadedRequests = null;

            int deleteSizeF = 0;
            int insertSizeF = 0;
            for (int i = 0; i < requests.length; i++) {
//                        assert requests[i].holdsRequest();
                Request r = (Request) requests[i];
                if (r == null) {
                    break;
                }
                if (r.type == false) {
                    deleteRequests[deleteSizeF++] = r;
                } else {
                    insertRequests[insertSizeF++] = r;
                }
            }

            final int deleteSize = deleteSizeF;
            final int insertSize = insertSizeF;

            if (heapSize + insertSize >= heap.length) { // Increase heap size
                Node[] newHeap = new Node[2 * heap.length];
                for (int i = 1; i <= heapSize; i++) {
                    newHeap[i] = heap[i];
                }
//                        for (int i = heapSize + 1; i < newHeap.length; i++) {
//                            newHeap[i] = new Node(Integer.MAX_VALUE);
//                        }
                heap = newHeap;
            }

            if (insertSize > 0) {
                Arrays.sort(insertRequests, 0, insertSize);
            }

            int insertStart = 0;

            if (deleteSize > 0) { // Prepare for delete minimums
                InnerHeap pq = innerHeap;
                pq.clear();
                // Looking for elements to remove
                pq.insert(1); // The root should be removed
                for (int i = 0; i < deleteSize; i++) {
                    int node = pq.extractMin();
                    kbest[i] = node;
                    heap[node].underProcessing = true;
                    deleteRequests[i].siftStart = 0; // initialize start position of sift

                    if (2 * node <= heapSize) {
                        pq.insert(2 * node);
                    }
                    if (2 * node + 1 <= heapSize) {
                        pq.insert(2 * node + 1);
                    }
                }
                Arrays.sort(kbest, 0, deleteSize);
                for (int i = 0; i < deleteSize; i++) {
                    int node = kbest[i];
                    deleteRequests[i].v = heap[node].v;

                    if (node >= heapSize - 1) { // We are the last or way later, then do nothing
                        if (node != heapSize - 1) {
                            heap[node].underProcessing = false;
                            continue;
                        } else if (i >= insertSize) { // We are last and there is no inserts left
                            heap[node].underProcessing = false;
                            heapSize--;
                            continue;
                        }
                    }

                    if (insertStart < insertSize) { // We could add insert some values right now
                        heap[node].v = insertRequests[insertStart].v;
                        insertRequests[insertStart++].status = FINISHED;
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

//                        boolean oneAt1 = false;
//                        for (int i = 0; i < deleteSize; i++) {
//                            oneAt1 |= deleteRequests[i].siftStart == 1;
//                        }
//
//                        if (!oneAt1) {
//                            throw new AssertionError("Fuck");
//                        }
//
//                        if (!heap[1].underProcessing) {
//                            throw new AssertionError("Fuck");
//                        }

                unsafe.storeFence();

                for (int i = 0; i < deleteSize; i++) {
                    deleteRequests[i].status = SIFT_DELETE;
                }

                unsafe.storeFence();

                if (request.status == SIFT_DELETE) { // I have to delete too
                    siftDown(request);
                }
                unsafe.loadFence();
                for (int i = 0; i < deleteSize; i++) { // Wait for everybody to finish
                    while (deleteRequests[i].status == SIFT_DELETE) {
                        sleep();
                        unsafe.loadFence(); // Only this load is enough, since we wait consequently
                    }
                }

//                        unsafe.loadFence();
//                        if (heap[1].underProcessing) {
//                            throw new AssertionError("Fuck");
//                        }
            }

            if (insertStart < insertSize) { // There are insert requests left
                // give the work to thread from root
                insertRequests[insertStart].siftStart = 1;

                int orderedValuesLength = insertSize - insertStart;
                for (int i = 0; i < orderedValuesLength; i++) {
                    orderedValues[i].set(insertRequests[i + insertStart].v);
                    if (heap[i + heapSize + 1] == null) {
                        heap[i + heapSize + 1] = new Node(Integer.MAX_VALUE); // already in the tree
                    }
                }

                int lstart = Integer.highestOneBit(heapSize + 1);
                heap[1].insertInfo = new InsertInfo(orderedValues, 0, orderedValuesLength,
                        null, null, 0,
                        lstart, 2 * lstart, heapSize + 1, heapSize + orderedValuesLength + 1);

                int id = 0;
                for (int i = 1; i < orderedValuesLength; i++) {
                    int left = i + heapSize;
                    int right = i + 1 + heapSize;
                    int lca = 0;
                    if (right == Integer.lowestOneBit(right)) { // We go to the next row
                        lca = 1;
                    } else {
                        lca = ~(left ^ right); // lca of i-th and (i-1)-th
                        lca = (i + heapSize) / Integer.lowestOneBit(lca);
                    }
//                      System.err.println("LCA: " + lca + " " + left + " " + right);

                    heap[lca].underProcessing = true;
                    insertRequests[i + insertStart].siftStart = 2 * lca + 1; // Start sift from the right child of lca
                }

                unsafe.storeFence();

                for (int i = insertStart; i < insertSize; i++) {
                    insertRequests[i].status = SIFT_INSERT;
                }

                unsafe.storeFence();

                heapSize = heapSize + orderedValuesLength;

                if (request.status == SIFT_INSERT) {
                    insert(request);
                }
                unsafe.loadFence();
                for (int i = insertStart; i < insertSize; i++) {
                    while (insertRequests[i].status == SIFT_INSERT) {
                        sleep();
                        unsafe.loadFence();
                    } // wait while finish
                }
            }

            fc.release(request, true);
        } else {
            unsafe.loadFence();
            int currentStatus;
            while ((currentStatus = request.status) == PUSHED) {
                sleep();
                unsafe.loadFence();
            }
            if (currentStatus == SIFT_DELETE) { // should know the node for sift down
                siftDown(request);
            } else if (currentStatus == SIFT_INSERT) { // I should make a sift up
                insert(request);
            }
            fc.release(request, false);
        }
    }

    public int deleteMin() {
//        System.err.println("Delete min");
        Request request = getLocalRequest();
        request.set(false, -1); // I assume that the insert value for delete min is -1
        unsafe.storeFence();
        handleRequest(request);
        return request.v;
    }

    public void insert(int v) {
//        System.err.println("Insert " + v);
        Request request = getLocalRequest();
        request.set(true, v); // I assume that the inserted values are >= 0
        unsafe.storeFence();
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
        fc = new FCCLH(threads);
        allocatedRequests = new ThreadLocal<>();
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
