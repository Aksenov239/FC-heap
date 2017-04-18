package lockbased;

import abstractions.Heap;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by vaksenov on 05.04.2017.
 */
public class LazySkipListHeap implements Heap {
    final private int maxLevel = 31;
    private Node head;
    private Node tail;

    public LazySkipListHeap(int size, int numThreads) {
        clear();
    }

    private int randomLevel() {
        int x = ThreadLocalRandom.current().nextInt();
        for (int i = 0; i < maxLevel - 1; i++, x >>= 1) {
            if ((x & 1) == 0) {
                return i + 1;
            }
        }
        return maxLevel - 1;
    }

    private void find(final int key, Node[] preds, Node[] succs) {
        Node pred = head;

        for (int level = maxLevel - 1; level >= 0; level--) {
            Node curr = pred.next[level];
            while (key > curr.key) {
                pred = curr;
                curr = pred.next[level];
            }
            preds[level] = pred;
            succs[level] = curr;
        }
    }

    public void insert(int v) {
        int topLevel = randomLevel();
        Node[] preds = new Node[maxLevel];
        Node[] succs = new Node[maxLevel];

        while (true) {
            find(v, preds, succs);

            int highestLocked = -1;

            try {
                Node pred, succ;
                boolean valid = true;

                for (int level = 0; valid && level < topLevel; level++) {
                    pred = preds[level];
                    succ = succs[level];
                    pred.lock();
                    highestLocked = level;
                    valid = !pred.marked && !succ.marked && pred.next[level] == succ;
                }

                if (!valid) {
                    continue;
                }

                Node newNode = new Node(v, topLevel);
                for (int level = 0; level < topLevel; level++) {
                    newNode.next[level] = succs[level];
                }
                for (int level = 0; level < topLevel; level++) {
                    preds[level].next[level] = newNode;
                }

                newNode.fullyLinked = true;
                return;
            } finally {
                for (int level = 0; level <= highestLocked; level++) {
                    preds[level].unlock();
                }
            }
        }
    }

    public void sequentialInsert(int v) {
        insert(v);
    }

    public int deleteMin() {
        Node min;
        while (true) {
            min = head.next[0];
            min.lock();
            if (min == tail || !min.fullyLinked || min.marked) {
                min.unlock();
                continue;
            }
            head.lock();
            if (head.next[0] != min) {
                head.unlock();
                min.unlock();
                continue;
            }

            min.marked = true;
            for (int level = min.next.length - 1; level >= 0; level--) {
                head.next[level] = min.next[level];
            }

            head.unlock();
            min.unlock();
            return min.key;
        }
    }

    public void clear() {
        head = new Node(Integer.MIN_VALUE, maxLevel);
        tail = new Node(Integer.MAX_VALUE, maxLevel);
        for (int i = 0; i < maxLevel; i++) {
            head.next[i] = tail;
        }
    }

    private final class Node {
        final Lock lock = new ReentrantLock();
        final int key;
        final Node[] next;
        volatile boolean marked = false;
        volatile boolean fullyLinked = false;

        public Node(int key, int height) {
            this.key = key;
            next = new Node[height];
        }

        public void lock() {
            lock.lock();
        }

        public void unlock() {
            lock.unlock();
        }
    }
}
