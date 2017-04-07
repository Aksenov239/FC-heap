#!/usr/bin/python3.4

import os
import sys

def filename(warmup, duration, proc, size, benchmark):
    return "out/log/w{}-d{}/{}_{}_{}.txt".format(warmup, duration, benchmark, proc, size)

classpath = "bin"
mainclass = "testing.Measure"

keys = ["throughput"]
procs = range(1, 80)#[1, 2, 4, 8, 15, 16, 23, 24, 31, 32, 39, 47, 55, 63, 71, 79]

warmup = 1000
duration = 1000
iterations = 5
procs = [1, 3, 7, 14, 21, 28, 35, 42, 49, 56, 63]
sizes = [800000, 2000000, 4000000]

max_proc = int(sys.argv[1])

benchmarks=["fc.parallel.FCParallelHeap",
           "fc.parallel.FCHalfParallelHeap",
           "fc.sequential.FCBinaryHeap",
           "fc.sequential.FCPairingHeap",
           "lockbased.BlockingHeap",
           "lockbased.LazySkipListHeap"]

if not os.path.isdir("out/log/w{}-d{}/".format(warmup, duration)):
     os.makedirs("out/log/w{}-d{}/".format(warmup, duration))

for proc in procs:
    if proc > max_proc:
        break
    for size in sizes:
        for benchmark in benchmarks:
            out = filename(warmup, duration, proc, size, benchmark)
            command = "java -server -Xmx2G -Xss256M -cp {} {} -n {} -t {} -w {} -d {} -s {} -r {} -b {} > {}".format(classpath, mainclass, iterations, proc, warmup, duration, size, 2 * size, benchmark, out)
            print(command)
            os.system(command)






