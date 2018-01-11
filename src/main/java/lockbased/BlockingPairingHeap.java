package lockbased;

import abstractions.Heap;
import fc.sequential.FCPairingHeap;

import java.util.ArrayList;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by vaksenov on 05.04.2017.
 */
public class BlockingPairingHeap implements Heap {

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

    ReentrantLock lock = new ReentrantLock();

    public BlockingPairingHeap(int size, int numThreads) {
        heap = null;
    }

    public int deleteMin() {
        lock.lock();
        int ans = heap.v;

        heap = mergePairs(heap.children);

        lock.unlock();
        return ans;
    }

    public void insert(int v) {
        lock.lock();

        Node node = new Node(v);
        heap = merge(node, heap);

        lock.unlock();
    }

    public void sequentialInsert(int v) {
        insert(v);
    }

    public void clear() {
        heap = null;
    }
}
