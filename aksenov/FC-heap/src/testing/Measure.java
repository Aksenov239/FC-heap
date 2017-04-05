package testing;

import abstractions.Heap;

import java.util.Random;

/**
 * Created by vaksenov on 30.03.2017.
 */
public class Measure {
    int threads = 1;
    int insertRatio = 50;
    int warmup;
    int duration;
    int size;
    int range;
    String benchClassname;

    int iterations = 1;

    Heap heap;

    public void prepopulate() {
        Random rnd = new Random(239);
        for (int i = 0; i < size; i++) {
            heap.sequentialInsert(rnd.nextInt(range));
        }
    }

    public void evaluateFor(int milliseconds, boolean withStats) {
        prepopulate();

        Thread[] thrs = new Thread[threads];
        HeapWorker[] workers = new HeapWorker[threads];
        for (int i = 0; i < threads; i++) {
            workers[i] = new HeapWorker(i, heap, range, insertRatio);
            thrs[i] = new Thread(workers[i]);
        }

        long start = System.nanoTime();

        for (int i = 0; i < threads; i++) {
            thrs[i].start();
        }

        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (int i = 0; i < threads; i++) {
            workers[i].stop();
        }

        for (int i = 0; i < threads; i++) {
            try {
                thrs[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        double totalTime = 1. * (System.nanoTime() - start) / 1_000_000_000;

        if (!withStats) {
            System.out.println("Total time spent:    \t" + totalTime);
            return;
        }

        int totalInserts = 0;
        int totalDeletes = 0;
        for (int i = 0; i < threads; i++) {
            totalInserts += workers[i].numInserts;
            totalDeletes += workers[i].numDeletes;
        }

        String result = "Results:\n" + "-----------------\n" +
                "Total time spent:     \t" + totalTime + "\n" +
                "Throughput:           \t" + (totalInserts + totalDeletes) / totalTime + " ops/sec" + "\n" +
                "Total operations:     \t" + (totalInserts + totalDeletes) + "\n" +
                " -- Total insertions: \t" + totalInserts + "\n" +
                " -- Total deletions:  \t" + totalDeletes;

        System.out.println(result);

        heap.clear();
    }

    public void run() {
        printParams();

        try {
            Class<Heap> heapClazz = (Class<Heap>) Class.forName(benchClassname);
            heap = heapClazz.getConstructor(int.class, int.class).newInstance(size, threads);

            if (warmup > 0) {
                System.out.println("Warmup started");
                evaluateFor(warmup, false);
                System.out.println("Warmup finished");
            }

            for (int i = 0; i < iterations; i++) {
                System.out.println("Start iteration " + (i + 1));
                evaluateFor(duration, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Cannot find benchmark class: " + benchClassname);
            System.exit(1);
        }
    }

    public Measure(String[] args) {
        parseCommandLine(args);
    }

    private void parseCommandLine(String[] args) {
        int argNumber = 0;
        while (argNumber < args.length) {
            String parameter = args[argNumber++];
            try {
                String option = args[argNumber++];
                if (parameter.equals("-t")) {
                    threads = Integer.parseInt(option);
                } else if (parameter.equals("-i")) {
                    insertRatio = Integer.parseInt(option);
                } else if (parameter.equals("-w")) {
                    warmup = Integer.parseInt(option);
                } else if (parameter.equals("-d")) {
                    duration = Integer.parseInt(option);
                } else if (parameter.equals("-s")) {
                    size = Integer.parseInt(option);
                } else if (parameter.equals("-r")) {
                    range = Integer.parseInt(option);
                } else if (parameter.equals("-b")) {
                    benchClassname = option;
                } else if (parameter.equals("-n")) {
                    iterations = Integer.parseInt(option);
                }
            } catch (IndexOutOfBoundsException e) {
                System.err.println("Missing value after option " + parameter + ". Ignoring.");
            } catch (NumberFormatException e) {
                System.err.println("Expecting number after option " + parameter + ". Ignoring.");
            }
        }
    }

    private void printParams() {
        String params = "Benchmark parameters" + "\n" + "-------------------\n" +
                "Number of threads:\t" + threads + "\n" +
                "Iterations:       \t" + iterations + "\n" +
                "Length:           \t" + duration + "\n" +
                "Warmup:           \t" + warmup + "\n" +
                "Initial size:     \t" + size + "\n" +
                "Range:            \t" + range + "\n" +
                "Insert ration:    \t" + insertRatio + "%\n" +
                "Benchmark:        \t" + benchClassname;
        System.out.println(params);
    }

    public static void main(String[] args) {
        new Measure(args).run();
    }
}
