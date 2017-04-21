#!/usr/bin/python3.4

import os
import sys

def filename(warmup, duration, proc, size, r, benchmark):
    return "out/log/w{}-d{}/{}_{}_{}_{}.txt".format(warmup, duration, benchmark, proc, size, r)

classpath = "bin"
mainclass = "testing.Measure"

keys = ["throughput"]

warmup = 5000
duration = 5000
iterations = 5
procs = [1, 3, 7, 14, 21, 28, 35, 42, 49, 56, 63]
sizes = [800000, 2000000, 4000000, 8000000]
ranges = [2147483647] #[100, 10000]

max_proc = int(sys.argv[1])

benchmarks=[
#           "fc.parallel.FCParallelHeap",
#           "fc.parallel.FCHalfParallelHeap",
#           "fc.sequential.FCBinaryHeap",
#           "fc.sequential.FCPairingHeap",
#           "lockbased.BlockingHeap",
           "lockbased.LazySkipListHeap",
           "lockfree.SkipListHeap"
           ]

if not os.path.isdir("out/log/w{}-d{}/".format(warmup, duration)):
     os.makedirs("out/log/w{}-d{}/".format(warmup, duration))

for proc in procs:
    if proc > max_proc:
        break
    for size in sizes:
        allranges = list(ranges)
#        allranges.append(2 * size)
        for r in allranges:
            for benchmark in benchmarks:
                out = filename(warmup, duration, proc, size, r, benchmark)
                command = "java -server -Xmx2G -Xss256M -cp {} {} -n {} -t {} -w {} -d {} -s {} -r {} -b {} > {}".format(classpath, mainclass, iterations, proc, warmup, duration, size, r, benchmark, out)
                print(command)
                os.system(command)






