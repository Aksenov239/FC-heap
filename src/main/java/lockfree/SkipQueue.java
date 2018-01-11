package lockfree;

import abstractions.Heap;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicMarkableReference;

/**
 * User: Aksenov Vitaly
 * Date: 16.08.2017
 * Time: 17:51
 */
public class SkipQueue implements Heap {
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

    public static final class Node {
        final int priority;
        AtomicBoolean marked;
        final AtomicMarkableReference<Node>[] next;
        int topLevel;

        /**
         * Constructor for sentinel nodes
         *
         * @param myPriority should be min or max integer value
         */
        public Node(int myPriority, int level) {
            priority = myPriority;
            marked = new AtomicBoolean(false);
            next = (AtomicMarkableReference<Node>[]) new AtomicMarkableReference[level + 1];
            for (int i = 0; i < next.length; i++) {
                next[i] = new AtomicMarkableReference<Node>(null, false);
            }
            topLevel = level;
        }
    }

    ThreadLocal<Node[]> preds = ThreadLocal.withInitial(() -> new Node[MAX_LEVEL + 1]);

    ThreadLocal<Node[]> succs = ThreadLocal.withInitial(() -> new Node[MAX_LEVEL + 1]);

    static final int MAX_LEVEL = 32;
    Node head;
    Node tail;

    public SkipQueue(int size, int threads) {
        head = new Node(Integer.MIN_VALUE, MAX_LEVEL);
        tail = new Node(Integer.MAX_VALUE, MAX_LEVEL);
        for (int i = 0; i < head.next.length; i++) {
            head.next[i]
                    = new AtomicMarkableReference<Node>(tail, false);
        }
    }

    /**
     * Finds node preds and succs and cleans up and does
     * not traverse marked nodes.
     * Found means node with equal key reached at bottom level
     * This differs from lazy list and allow wait-free contains
     * since new nodes are always inserted before removed ones
     * and will be found at bottom level so if a marked node
     * found at bottom level then there is no node with same
     * value in the set. This means that remove cannot start
     * until node is threaded by add() at the bottomLevel
     */
    boolean find(Node node, Node[] preds, Node[] succs) {
        int bottomLevel = 0;
        boolean[] marked = {false};
        boolean snip;
        Node pred = null, curr = null, succ = null;
        retry:
        while (true) {
            pred = head;
            // curr = null; not needed line removed by Nir
            for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
                curr = pred.next[level].getReference();
                while (true) {
                    succ = curr.next[level].get(marked);
                    while (marked[0]) {           // replace curr if marked
                        snip = pred.next[level].compareAndSet(curr, succ, false, false);
                        if (!snip) continue retry;
                        curr = pred.next[level].getReference();
                        succ = curr.next[level].get(marked);
                    }
                    if (curr.priority < node.priority){ // move forward same level
                        pred = curr;
                        curr = succ;
                    } else {
                        break; // move to next level
                    }
                }
                preds[level] = pred;
                succs[level] = curr;
            }
            return (curr.priority == node.priority); // bottom level curr.key == v
        }
    }

    /**
     * Add at bottomLevel and thus to the set
     * Afterwords all links at higher levels are added
     */
    boolean add(Node node) {
        int bottomLevel = 0;
        Node[] preds = this.preds.get();
        Node[] succs = this.succs.get();
        while (true) {
            boolean found = find(node, preds, succs);
            if (found) { // if found it's not marked
                return false;
            } else {
                for (int level = bottomLevel; level <= node.topLevel; level++) {
                    Node succ = succs[level];
                    node.next[level].set(succ, false);
                }
                // try to splice in new node in bottomLevel going up
                Node pred = preds[bottomLevel];
                Node succ = succs[bottomLevel];
                node.next[bottomLevel].set(succ, false);
                if (!pred.next[bottomLevel].compareAndSet(succ, node, false, false)) {// lin point
                    continue; // retry from start
                }
                // splice in remaining levels going up
                for (int level = bottomLevel+1; level <= node.topLevel; level++) {
                    while (true) {
                        pred = preds[level];
                        succ = succs[level];
                        if (pred.next[level].compareAndSet(succ, node, false, false))
                            break;
                        find(node, preds, succs); // find new preds and succs
                    }
                }
                return true;
            }
        }
    }

    /**
     * start at highest level then continue marking down the levels
     * if lowest marked successfully node is removed
     * other threads could be modifying node's pointers concurrently
     * the node could also still be in the process of being added
     * so node could end up connected on some levels and disconnected on others
     * find traversals will eventually physically remove node
     */
    boolean remove(Node node) {
        int bottomLevel = 0;
        Node[] preds = this.preds.get();
        Node[] succs = this.succs.get();
        Node succ;
        while (true) {
            boolean found = find(node, preds, succs);
            if (!found) {
                return false;
            } else {
                // proceed to mark all levels
                // some levels could stil be unthreaded by concurrent add() while being marked
                // other find()s could be modifying node's pointers concurrently
                for (int level = node.topLevel; level >= bottomLevel+1; level--) {
                    boolean[] marked = {false};
                    succ = node.next[level].get(marked);
                    while (!marked[0]) { // until I succeed in marking
                        node.next[level].attemptMark(succ, true);
                        succ = node.next[level].get(marked);
                    }
                }
                // proceed to remove from bottom level
                boolean[] marked = {false};
                succ = node.next[bottomLevel].get(marked);
                while (true) { // until someone succeeded in marking
                    boolean iMarkedIt = node.next[bottomLevel].compareAndSet(succ, succ, false, true);
                    succ = succs[bottomLevel].next[bottomLevel].get(marked);
                    if (iMarkedIt) {
                        // run find to remove links of the logically removed node
                        find(node, preds, succs);
                        return true;
                    } else if (marked[0]) return false; // someone else removed node
                    // else only succ changed so repeat
                }
            }
        }
    }

    public Node findAndMarkMin() {
        Node curr = null, succ = null;
        curr = head.next[0].getReference();
        while (curr != tail) {
            if (!curr.marked.get()) {
                if (curr.marked.compareAndSet(false, true)) {
                    return curr;
                }
            }
            curr = head.next[0].getReference();
        }
        return null; // no unmarked nodes
    }

    public void insert(int v) {
        Node node = new Node(v, randomLevel());
        add(node);
    }

    public int deleteMin() {
        Node node = findAndMarkMin();
        if (node == null) {
            return -1;
        }
        remove(node);
        return node.priority;
    }

    public void sequentialInsert(int v) {
        insert(v);
    }

    public void clear() {
        head = new Node(Integer.MIN_VALUE, MAX_LEVEL);
        tail = new Node(Integer.MAX_VALUE, MAX_LEVEL);
        for (int i = 0; i < head.next.length; i++) {
            head.next[i]
                    = new AtomicMarkableReference<>(tail, false);
        }
    }
}
