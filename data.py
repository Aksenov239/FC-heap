import sys
import os
import re
from statistics import mean, stdev

def read_from_file(filename, keys):
    inf = open(filename, 'r')
    values = dict()
    for key in keys:
        values[key] = []
    for line in inf.readlines():
        ll = line.lower()
        good = None
        for key in keys:
            if key.lower() in ll:
                good = key
        if good == None:
            continue
        value = re.sub('(.*?)', '', ll).strip().split(" ")[-2]
        values[key].append(float(value))
    return values

def filename(warmup, duration, proc, size, r, benchmark):
    return "out/log/w{}-d{}/{}_{}_{}_{}.txt".format(warmup, duration, benchmark, proc, size, r)

keys = ["throughput"]
procs = range(1, 80)

warmup = 5000
duration = 5000
sizes = [800000, 2000000, 4000000, 8000000]
ranges = [100, 10000]

benchmarks=["fc.parallel.FCParallelHeap",
           "fc.parallel.FCHalfParallelHeap",
           "fc.parallel.TFCParallelHeap",
           "fc.sequential.FCBinaryHeap",
           "fc.sequential.FCPairingHeap",
           "lockbased.BlockingHeap",
           "lockbased.LazySkipListHeap"]

directory = "out/data/w{}-d{}/".format(warmup, duration)
if not os.path.isdir(directory):
    os.makedirs(directory)

for key in keys:
    for size in sizes:
        allranges = list(ranges)
        allranges.append(2 * size)
        for r in allranges:
            for i in range(len(benchmarks)):
                bench = benchmarks[i]
                out = open((directory + "comparison_{}_{}_{}_{}.dat").format(key, size, r, bench.split(".")[-1]), 'w')
                if not os.path.exists(filename(warmup, duration, 1, size, r, bench)):
                    continue
                for proc in procs:
                    if not os.path.exists(filename(warmup, duration, proc, size, r, bench)):
                        continue
                    results = read_from_file(filename(warmup, duration, proc, size, r, bench), keys)[key][1:]
                    out.write(str(proc) + " " + str(mean(results) / 1000 / 1000) + " " + str(stdev(results) / 1000 / 1000) + "\n")
