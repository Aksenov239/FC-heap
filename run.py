#!/usr/bin/python3.4

import os
import sys

def filename(warmup, duration, proc, size, benchmark):
    return "out/data/w{}-d{}/{}_{}_{}.txt".format(warmup, duration, benchmark, proc, size)

classpath = "bin"
mainclass = "testing.Measure"

warmup = 1000
duration = 1000
iterations = 5
procs = [1, 2, 5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60, 63]
sizes = [800000, 10000000, 20000000]

max_proc = int(sys.argv[1])

benchmarks=["fc.parallel.FCParallelHeap",
           "fc.parallel.FCHalfParallelHeap",
           "fc.sequential.FCBinaryHeap",
           "fc.sequential.FCPairingHeap"]

if not os.path.isdir("out/data/w{}-d{}/".format(warmup, duration)):
     os.makedirs("out/data/w{}-d{}/".format(warmup, duration))

for proc in procs:
    if proc > max_proc:
        break
    for size in sizes:
        for benchmark in benchmarks:
            out = filename(warmup, duration, proc, size, benchmark)
            command = "java -server -Xmx1G -Xss256M -cp {} {} -n {} -t {} -w {} -d {} -s {} -r {} -b {} > {}".format(classpath, mainclass, iterations, proc, warmup, duration, size, 2 * size, benchmark, out)
            print(command)
            os.system(command)






