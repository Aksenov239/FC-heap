#!/usr/bin/python3.4

import os
import sys

def filename(warmup, duration, proc, size, r, benchmark):
    return "out/log/w{}-d{}/{}_{}_{}_{}.txt".format(warmup, duration, benchmark, proc, size, r)

classpath = "bin:lib/jmh-core-0.1.jar"
mainclass = "testing.Measure"

keys = ["throughput"]

warmup = 10000
duration = 10000
iterations = 5
procs = [1, 3, 7, 14, 21, 28, 35, 42, 49, 56, 63]
sizes = [20000000, 800000, 2000000, 4000000, 8000000]
ranges = [100, 10000, 2147483647]

max_proc = int(sys.argv[1])

benchmarks=[
#           "fc.parallel.FCParallelHeap",
#           "fc.parallel.FCParallelHeapv2",
#           "fc.parallel.FCParallelHeapFlush",
#           "fc.parallel.TFCParallelHeap",
#           "fc.sequential.FCBinaryHeap",
#           "fc.sequential.FCPairingHeap",
#           "lockfree.SkipQueue",
           "lockfree.LindenSkipList",
           "lockbased.BlockingBinaryHeap",
           "lockbased.BlockingPairingHeap",
#           "lockbased.LazySkipListHeap"
         ]

if not os.path.isdir("out/log/w{}-d{}/".format(warmup, duration)):
     os.makedirs("out/log/w{}-d{}/".format(warmup, duration))

for proc in procs:
    if proc > max_proc:
        break
    for size in sizes:
        allranges = list(ranges)
        allranges.append(2 * size)
        for r in allranges:
            for benchmark in benchmarks:
                out = filename(warmup, duration, proc, size, r, benchmark)
                command = "java -server -Xmx30G -Xss256M -XX:+UseCompressedOops -cp {} {} -n {} -t {} -w {} -d {} -s {} -r {} -b {} > {}".format(classpath, mainclass, iterations, proc, warmup, duration, size, r, benchmark, out)
                print(command)
                os.system(command)






