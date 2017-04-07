package lockfree;

import abstractions.Heap;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Created by vaksenov on 07.04.2017.
 */
public class SkipListHeap implements Heap {
    final private int maxLevel = 31;

    private static final Object BASE_HEADER = new Object();

    private static final Object VALUE = new Object();
    /**
     * The topmost head index of the skiplist.
     */
    private transient volatile HeadIndex head;

    ThreadLocal<Random> rnd = new ThreadLocal<>();

    private Random getRandom() {
        Random random = rnd.get();
        if (random == null) {
            random = new Random();
            rnd.set(random);
        }
        return random;
    }

    public SkipListHeap(int size, int threadsNum) {
        head = new HeadIndex(new Node(Integer.MAX_VALUE, BASE_HEADER, null),
                null, null, 1);
    }

    /**
     * Updater for casHead
     */
    private static final AtomicReferenceFieldUpdater<SkipListHeap, HeadIndex> headUpdater = AtomicReferenceFieldUpdater
            .newUpdater(SkipListHeap.class, HeadIndex.class,
                    "head");

    /**
     * compareAndSet head node
     */
    private boolean casHead(HeadIndex cmp, HeadIndex val) {
        return headUpdater.compareAndSet(this, cmp, val);
    }

    static final class Node {
        final int key;
        volatile Object value;
        volatile Node next;

        Node(int key, Object value, Node next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }

        Node(Node next) {
            this.key = -1;
            this.value = this;
            this.next = next;
        }

        /**
         * Updater for casNext
         */
        static final AtomicReferenceFieldUpdater<Node, Node> nextUpdater = AtomicReferenceFieldUpdater
                .newUpdater(Node.class, Node.class, "next");

        /**
         * Updater for casValue
         */
        static final AtomicReferenceFieldUpdater<Node, Object> valueUpdater = AtomicReferenceFieldUpdater
                .newUpdater(Node.class, Object.class, "value");

        /**
         * compareAndSet value field
         */
        boolean casValue(Object cmp, Object val) {
            return valueUpdater.compareAndSet(this, cmp, val);
        }

        /**
         * compareAndSet next field
         */
        boolean casNext(Node cmp, Node val) {
            return nextUpdater.compareAndSet(this, cmp, val);
        }

        boolean isMarker() {
            return value == this;
        }

        boolean isBaseHeader() {
            return value == BASE_HEADER;
        }

        boolean appendMarker(Node f) {
            return casNext(f, new Node(f));
        }

        void helpDelete(Node b, Node f) {
            /*
			 * Rechecking links and then doing only one of the help-out stages
			 * per call tends to minimize CAS interference among helping
			 * threads.
			 */
            if (f == next && this == b.next) {
                if (f == null || f.value != f) // not already marked
                    appendMarker(f);
                else
                    b.casNext(this, f.next);
            }
        }
    }

    static class Index {
        final Node node;
        final Index down;
        volatile Index right;

        /**
         * Creates index node with given values.
         */
        Index(Node node, Index down, Index right) {
            this.node = node;
            this.down = down;
            this.right = right;
        }

        /**
         * Updater for casRight
         */
        static final AtomicReferenceFieldUpdater<Index, Index> rightUpdater = AtomicReferenceFieldUpdater
                .newUpdater(Index.class, Index.class, "right");

        /**
         * compareAndSet right field
         */
        final boolean casRight(Index cmp, Index val) {
            return rightUpdater.compareAndSet(this, cmp, val);
        }

        /**
         * Returns true if the node this indexes has been deleted.
         *
         * @return true if indexed node is known to be deleted
         */
        final boolean indexesDeletedNode() {
            return node.value == null;
        }

        /**
         * Tries to CAS newSucc as successor. To minimize races with unlink that
         * may lose this index node, if the node being indexed is known to be
         * deleted, it doesn't try to link in.
         *
         * @param succ    the expected current successor
         * @param newSucc the new successor
         * @return true if successful
         */
        final boolean link(Index succ, Index newSucc) {
            Node n = node;
            newSucc.right = succ;
            return n.value != null && casRight(succ, newSucc);
        }

        /**
         * Tries to CAS right field to skip over apparent successor succ. Fails
         * (forcing a retraversal by caller) if this node is known to be
         * deleted.
         *
         * @param succ the expected current successor
         * @return true if successful
         */
        final boolean unlink(Index succ) {
            // if(STRUCT_MODS)
            // counts.get().structMods++;
            return !indexesDeletedNode() && casRight(succ, succ.right);
        }
    }

    	/* ---------------- Head nodes -------------- */

    /**
     * Nodes heading each level keep track of their level.
     */
    static final class HeadIndex extends Index {
        final int level;

        HeadIndex(Node node, Index down, Index right,
                  int level) {
            super(node, down, right);
            this.level = level;
        }
    }

    /**
     * Specialized variant of findNode to get first valid node.
     *
     * @return first node or null if empty
     */
    Node findFirst() {
        for (; ; ) {
            Node b = head.node;
            Node n = b.next;
            if (n == null)
                return null;
            if (n.value != null)
                return n;
            n.helpDelete(b, n.next);
        }
    }

    /**
     * Possibly reduce head level if it has no nodes. This method can (rarely)
     * make mistakes, in which case levels can disappear even though they are
     * about to contain index nodes. This impacts performance, not correctness.
     * To minimize mistakes as well as to reduce hysteresis, the level is
     * reduced by one only if the topmost three levels look empty. Also, if the
     * removed level looks non-empty after CAS, we try to change it back quick
     * before anyone notices our mistake! (This trick works pretty well because
     * this method will practically never make mistakes unless current thread
     * stalls immediately before first CAS, in which case it is very unlikely to
     * stall again immediately afterwards, so will recover.)
     * <p>
     * We put up with all this rather than just let levels grow because
     * otherwise, even a small map that has undergone a large number of
     * insertions and removals will have a lot of levels, slowing down access
     * more than would an occasional unwanted reduction.
     */
    private void tryReduceLevel() {
        HeadIndex h = head;
        HeadIndex d;
        HeadIndex e;
        if (h.level > 3 && (d = (HeadIndex) h.down) != null
                && (e = (HeadIndex) d.down) != null && e.right == null
                && d.right == null && h.right == null && casHead(h, d) && // try
                // to
                // set
                h.right != null) // recheck
            casHead(d, h); // try to backout
    }

    /**
     * Clears out index nodes associated with deleted first entry.
     */
    private void clearIndexToFirst() {
        for (; ; ) {
            Index q = head;
            for (; ; ) {
                Index r = q.right;
                if (r != null && r.indexesDeletedNode() && !q.unlink(r))
                    break;
                if ((q = q.down) == null) {
                    if (head.right == null)
                        tryReduceLevel();
                    return;
                }
            }
        }
    }

    public int deleteMin() {
        for (; ; ) {
            Node b = head.node;
            Node n = b.next;
            if (n == null)
                return Integer.MAX_VALUE;
            Node f = n.next;
            if (n != b.next)
                continue;
            Object v = n.value;
            if (v == null) {
                n.helpDelete(b, f);
                continue;
            }
            if (!n.casValue(v, null))
                continue;
            if (!n.appendMarker(f) || !b.casNext(n, f))
                findFirst(); // retry
            clearIndexToFirst();
            return n.key;
        }
    }

    private Node findPredecessor(int key) {
        for (;;) {
            Index q = head;
            Index r = q.right;
            for (;;) {
                if (r != null) {
                    Node n = r.node;
                    int k = n.key;
                    if (n.value == null) {
                        if (!q.unlink(r))
                            break; // restart
                        r = q.right; // reread r
                        continue;
                    }
                    if (key - k > 0) {
                        q = r;
                        r = r.right;
                        continue;
                    }
                }
                Index d = q.down;
                if (d != null) {
                    q = d;
                    r = d.right;
                } else
                    return q.node;
            }
        }
    }

    private Node findNode(int key) {
        for (;;) {
            Node b = findPredecessor(key);
            Node n = b.next;
            for (;;) {
                if (n == null)
                    return null;
                Node f = n.next;
                if (n != b.next) // inconsistent read
                    break;
                Object v = n.value;
                if (v == null) { // n is deleted
                    n.helpDelete(b, f);
                    break;
                }
                if (v == n || b.value == null) // b is deleted
                    break;
                int c = key - n.key;
//                if (c == 0)
//                    return n;
                if (c <= 0)
                    return null;
                b = n;
                n = f;
            }
        }
    }
    
    /**
     * Returns a random level for inserting a new node. Hardwired to k=1, p=0.5,
     * max 31 (see above and Pugh's "Skip List Cookbook", sec 3.4).
     *
     * This uses the simplest of the generators described in George Marsaglia's
     * "Xorshift RNGs" paper. This is not a high-quality generator but is
     * acceptable here.
     */
    private int randomLevel() {
        int x = getRandom().nextInt();
        for (int i = 0; i < maxLevel - 1; i++, x >>= 1) {
            if ((x & 1) == 0) {
                return i + 1;
            }
        }
        return maxLevel - 1;
    }

    /**
     * Creates and adds index nodes for the given node.
     *
     * @param z
     *            the node
     * @param level
     *            the level of the index
     */
    private void insertIndex(Node z, int level) {
        HeadIndex h = head;
        int max = h.level;

        if (level <= max) {
            Index idx = null;
            for (int i = 1; i <= level; ++i)
                idx = new Index(z, idx, null);
            addIndex(idx, h, level);

        } else { // Add a new level
			/*
			 * To reduce interference by other threads checking for empty levels
			 * in tryReduceLevel, new levels are added with initialized right
			 * pointers. Which in turn requires keeping levels in an array to
			 * access them while creating new head index nodes from the opposite
			 * direction.
			 */
            level = max + 1;
            Index[] idxs = (Index[]) new Index[level + 1];
            Index idx = null;
            for (int i = 1; i <= level; ++i)
                idxs[i] = idx = new Index(z, idx, null);

            HeadIndex oldh;
            int k;
            for (;;) {
                oldh = head;
                int oldLevel = oldh.level;
                if (level <= oldLevel) { // lost race to add level
                    k = level;
                    break;
                }
                HeadIndex newh = oldh;
                Node oldbase = oldh.node;
                for (int j = oldLevel + 1; j <= level; ++j)
                    newh = new HeadIndex(oldbase, newh, idxs[j], j);
                if (casHead(oldh, newh)) {
                    k = oldLevel;
                    break;
                }
            }
            addIndex(idxs[k], oldh, k);
        }
    }

    /**
     * Adds given index nodes from given level down to 1.
     *
     * @param idx
     *            the topmost index node being inserted
     * @param h
     *            the value of head to use to insert. This must be snapshotted
     *            by callers to provide correct insertion level
     * @param indexLevel
     *            the level of the index
     */
    private void addIndex(Index idx, HeadIndex h, int indexLevel) {
        // Track next level to insert in case of retries
        int insertionLevel = indexLevel;
        int key = idx.node.key;

        // Similar to findPredecessor, but adding index nodes along
        // path to key.
        for (; ; ) {
            int j = h.level;
            Index q = h;
            Index r = q.right;
            Index t = idx;
            for (; ; ) {
                if (r != null) {
                    Node n = r.node;
                    // compare before deletion check avoids needing recheck
                    int c = key - n.key;
                    if (n.value == null) {
                        if (!q.unlink(r))
                            break;
                        r = q.right;
                        continue;
                    }
                    if (c > 0) {
                        q = r;
                        r = r.right;
                        continue;
                    }
                }

                if (j == insertionLevel) {
                    // Don't insert index if node already deleted
                    if (t.indexesDeletedNode()) {
                        findNode(key); // cleans up
                        return;
                    }
                    if (!q.link(r, t))
                        break; // restart
                    if (--insertionLevel == 0) {
                        // need final deletion check before return
                        if (t.indexesDeletedNode())
                            findNode(key);
                        return;
                    }
                }

                if (--j >= insertionLevel && j < indexLevel)
                    t = t.down;
                q = q.down;
                r = q.right;
            }
        }
    }

    public void insert(int key) {
        for (; ; ) {
            Node b = findPredecessor(key);
            Node n = b.next;
            for (; ; ) {
                if (n != null) {
                    Node f = n.next;
                    if (n != b.next) // inconsistent read
                        break;
                    Object v = n.value;
                    if (v == null) { // n is deleted
                        n.helpDelete(b, f);
                        break;
                    }
                    if (v == n || b.value == null) // b is deleted
                        break;
                    int c = key - n.key;
                    if (c > 0) {
                        b = n;
                        n = f;
                        continue;
                    }
                    // else c <= 0; fall through
                }

                Node z = new Node(key, VALUE, n);
                if (!b.casNext(n, z))
                    break; // restart if lost race to append to b
                int level = randomLevel();
                if (level > 0) {
                    // if(STRUCT_MODS)
                    // counts.get().structMods += level - 1;
                    insertIndex(z, level);
                }
                return;
            }
        }
    }

    public void sequentialInsert(int v) {
        insert(v);
    }

    public void clear() {

    }
}
