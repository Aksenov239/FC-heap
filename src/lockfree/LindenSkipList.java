package lockfree;

import abstractions.Heap;

import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by vaksenov on 25.08.2017.
 */
public class LindenSkipList implements Heap {
    static int randomSeed = (int) (System.currentTimeMillis()) | 0x0100;
    private static int randomLevel() {
        int x = randomSeed;
        x ^= x << 13;
        x ^= x >>> 17;
        randomSeed = x ^= x << 5;
        if ((x & 0x80000001) != 0) // test highest and lowest bits
            return 0;
        int level = 1;
        while (((x >>>= 1) & 1) != 0) ++level;
        return Math.min(level, MAX_LEVEL-1);
    }

    public class Node {
        int key;
        volatile boolean inserting;

        AtomicMarkableReference<Node> bottom;
        AtomicReference<Node>[] next;

        public Node(int key, int level) {
            this.key = key;
            bottom = new AtomicMarkableReference<>(null, false);
            next = new AtomicReference[level];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicReference<>(null);
            }
        }

    }

    ThreadLocal<Node[]> preds = ThreadLocal.withInitial(() -> new Node[MAX_LEVEL]);
    ThreadLocal<Node[]> succs = ThreadLocal.withInitial(() -> new Node[MAX_LEVEL]);

    static final int MAX_LEVEL = 32;
    static final int OFFSET_BOUND = 100;
    Node head;
    Node middle;
    Node tail;

    public LindenSkipList(int size, int threads) {
        clear();
    }

    public Node locatePreds(int k, Node[] preds, Node[] succs) {
        int i = head.next.length - 1;
        Node pred = head;
        Node del = null;
        while (i >= 0) {
            Node cur = i == 0 ? pred.bottom.getReference() : pred.next[i].get();
            boolean d = pred.bottom.isMarked();
            while (cur.key < k || cur.bottom.isMarked() ||
                    (d && i == 0)) {
                if (d && i == 0) {
                    del = cur;
                }
                pred = cur;

                cur = i == 0 ? pred.bottom.getReference() : pred.next[i].get();

                d = pred.bottom.isMarked();
            }
            preds[i] = pred;
            succs[i] = cur;
            i--;
        }
        return del;
    }

    public void insert(int k) {
        int level = randomLevel();
        Node x = new Node(k, level);
        x.inserting = true;
        Node[] preds = this.preds.get();
        Node[] succs = this.succs.get();
        Node del;
        do {
            del = locatePreds(k, preds, succs);
            x.bottom.set(succs[0], false);
        } while (!preds[0].bottom.compareAndSet(succs[0], x, false, false));
        int i = 1;
        while (i < level) {
            x.next[i].set(succs[i]);
            if (x.bottom.isMarked() || succs[i].bottom.isMarked() || succs[i] == del) {
                break;
            }
            if (preds[i].next[i].compareAndSet(succs[i], x)) {
                i++;
            } else {
                del = locatePreds(k, preds, succs);
                if (succs[0] != x) {
                    break;
                }
            }
        }
        x.inserting = false;
    }

    public int deleteMin() {
        Node x = head;
        int offset = 0;
        Node newhead = null;
        Node obshead = head.bottom.getReference();
        boolean[] d = new boolean[1];
        do {
            Node next = x.bottom.get(d);
            if (x.inserting && newhead == null) {
                newhead = x;
            }
            if (next == tail || next.bottom.getReference() == tail) {
                return -1;
            }
            if (!d[0]) {
                d[0] = !x.bottom.attemptMark(next, true);
            }
            offset++;
            x = next;
        } while (d[0]);

        int ans = x.key;

        if (offset < OFFSET_BOUND) {
            return ans;
        }

        if (newhead == null) {
            newhead = x;
        }

        if (head.bottom.compareAndSet(obshead, newhead, true, true)) {
            int i = head.next.length - 1;
            Node pred = head;
            while (i > 0) {
                Node h = head.next[i].get();
                Node cur = pred.next[i].get();
                if (!h.bottom.isMarked()) {
                    i--;
                    continue;
                }
                while (cur.bottom.isMarked()) {
                    pred = cur;
                    cur = pred.next[i].get();
                }
                if (head.next[i].compareAndSet(h, pred.next[i].get())) {
                    i--;
                }
            }
        }

        return ans;
    }

    public void sequentialInsert(int v) {
        insert(v);
    }

    public void clear() {
        head = new Node(Integer.MIN_VALUE, MAX_LEVEL);
        middle = new Node(Integer.MIN_VALUE, 1);
        tail = new Node(Integer.MAX_VALUE, MAX_LEVEL);

        head.bottom.set(middle, true);
        for (int i = 1; i < head.next.length; i++) {
            head.next[i] = new AtomicReference<>(tail);
        }

        middle.bottom.set(tail, false);
    }
}
