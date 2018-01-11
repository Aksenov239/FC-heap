package testing;

import abstractions.Heap;

import java.lang.reflect.InvocationTargetException;
import java.util.PriorityQueue;
import java.util.Random;

/**
 * Created by vaksenov on 30.03.2017.
 */
public class Test {
    Heap heap;
    int size = 0;

    public Test(String[] args) {
        try {
            Class<Heap> clazz = (Class<Heap>)Class.forName(args[1]);
            size = Integer.parseInt(args[0]);
            heap = clazz.getConstructor(int.class, int.class).newInstance(size, 1);
        } catch (Exception e) {
            System.err.println("Could not found class " + args[0]);
            System.exit(1);
        }
    }

    public void run() {
        Random rnd = new Random(239);
        PriorityQueue<Integer> pq = new PriorityQueue<>();
        for (int i = 0; i < size; i++) {
            int r = rnd.nextInt(1_000_000_000);
            pq.add(r);
            heap.sequentialInsert(r);
        }

        int it = 0;
        while (true) {
            int r = rnd.nextInt(1_000_000_000);
            pq.add(r);
            heap.insert(r);

//            System.err.println(pq);

//            System.err.println(heap);

            int pqA = pq.poll();
            int hA = heap.deleteMin();

//            System.err.println(heap);

            if (pqA != hA) {
                System.err.println(String.format("Wrong. PQ returned %d, heap returned %d", pqA, hA));
                System.exit(1);
            }
            it++;
            if (it % 100_000 == 0) {
                System.out.println("Finished " + it + "-th iteration");
            }
        }
    }

    public static void main(String[] args) {
        new Test(args).run();
    }
}
