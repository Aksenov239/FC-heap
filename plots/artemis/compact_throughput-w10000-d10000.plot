\begin{tikzpicture}
   \begin{groupplot}[
       group style={
           group size= 3 by 5,
       },
       height=5cm,
       width=5cm,
   ]

% -- 800000

   \nextgroupplot[title=Range: 100, ylabel={Size: 800000}, cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_100_FCParallelHeap.dat};\label{plots:fcparallel}
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_100_FCParallelHeapFlush.dat};\label{plots:fcparallelv2}
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_100_FCBinaryHeap.dat};\label{plots:fcbinary}
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_100_FCPairingHeap.dat};\label{plots:fcpairing}
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_100_LazySkipListHeap.dat};\label{plots:lazyskiplist}
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_100_LindenSkipList.dat};\label{plots:skiplist}
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_100_SkipQueue.dat};\label{plots:skipqueue}
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_100_BlockingHeap.dat};\label{plots:blocking}
       \coordinate (top) at (rel axis cs:0,1);% coordinate at top of the first plot

   \nextgroupplot[title=Range: $10^4$, cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_10000_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_10000_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_10000_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_10000_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_10000_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_10000_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_10000_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_10000_BlockingHeap.dat};

%   \nextgroupplot[title=Range: $2 \times $\,size, cycle list name=color]
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_1600000_FCParallelHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_1600000_FCParallelHeapFlush.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_1600000_FCBinaryHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_1600000_FCPairingHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_1600000_LazySkipListHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_1600000_LindenSkipList.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_1600000_SkipQueue.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_1600000_BlockingHeap.dat};

   \nextgroupplot[title=Range: $2^{31}$, cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_2147483647_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_2147483647_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_2147483647_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_2147483647_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_2147483647_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_2147483647_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_2147483647_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_800000_2147483647_BlockingHeap.dat};

% -- 2000000

   \nextgroupplot[ylabel={Size: 2000000}, cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_100_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_100_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_100_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_100_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_100_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_100_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_100_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_100_BlockingHeap.dat};

   \nextgroupplot[cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_10000_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_10000_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_10000_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_10000_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_10000_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_10000_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_10000_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_10000_BlockingHeap.dat};

%   \nextgroupplot[cycle list name=color]
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_4000000_FCParallelHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_4000000_FCParallelHeapFlush.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_4000000_FCBinaryHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_4000000_FCPairingHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_4000000_LazySkipListHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_4000000_LindenSkipList.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_4000000_SkipQueue.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_4000000_BlockingHeap.dat};

   \nextgroupplot[cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_2147483647_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_2147483647_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_2147483647_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_2147483647_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_2147483647_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_2147483647_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_2147483647_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_2000000_2147483647_BlockingHeap.dat};

% -- 4000000

   \nextgroupplot[ylabel={Size: 4000000}, cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_100_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_100_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_100_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_100_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_100_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_100_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_100_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_100_BlockingHeap.dat};

   \nextgroupplot[cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_10000_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_10000_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_10000_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_10000_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_10000_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_10000_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_10000_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_10000_BlockingHeap.dat};

%   \nextgroupplot[cycle list name=color]
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_8000000_FCParallelHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_8000000_FCParallelHeapFlush.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_8000000_FCBinaryHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_8000000_FCPairingHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_8000000_LazySkipListHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_8000000_LindenSkipList.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_8000000_SkipQueue.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_8000000_BlockingHeap.dat};

   \nextgroupplot[cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_2147483647_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_2147483647_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_2147483647_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_2147483647_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_2147483647_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_2147483647_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_2147483647_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_4000000_2147483647_BlockingHeap.dat};

% -- 8000000

  \nextgroupplot[ylabel={Size: 8000000}, cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_100_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_100_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_100_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_100_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_100_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_100_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_100_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_100_BlockingHeap.dat};

   \nextgroupplot[cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_10000_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_10000_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_10000_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_10000_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_10000_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_10000_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_10000_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_10000_BlockingHeap.dat};
 
%   \nextgroupplot[cycle list name=color]
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_16000000_FCParallelHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_16000000_FCParallelHeapFlush.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_16000000_FCBinaryHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_16000000_FCPairingHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_16000000_LazySkipListHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_16000000_LindenSkipList.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_16000000_SkipQueue.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_16000000_BlockingHeap.dat};

   \nextgroupplot[cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_2147483647_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_2147483647_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_2147483647_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_2147483647_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_2147483647_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_2147483647_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_2147483647_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_8000000_2147483647_BlockingHeap.dat};

% -- 20000000

  \nextgroupplot[xlabel={Number of Threads}, ylabel={Size: 20000000}, cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_100_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_100_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_100_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_100_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_100_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_100_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_100_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_100_BlockingHeap.dat};

   \nextgroupplot[xlabel={Number of Threads}, cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_10000_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_10000_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_10000_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_10000_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_10000_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_10000_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_10000_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_10000_BlockingHeap.dat};
 
%   \nextgroupplot[xlabel={Number of Threads}, cycle list name=color]
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_40000000_FCParallelHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_40000000_FCParallelHeapFlush.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_40000000_FCBinaryHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_40000000_FCPairingHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_40000000_LazySkipListHeap.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_40000000_LindenSkipList.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_40000000_SkipQueue.dat};
%       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_40000000_BlockingHeap.dat};

   \nextgroupplot[xlabel={Number of Threads}, cycle list name=color]
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_2147483647_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_2147483647_FCParallelHeapFlush.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_2147483647_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_2147483647_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_2147483647_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_2147483647_LindenSkipList.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_2147483647_SkipQueue.dat};
       \addplot table {data/w10000-d10000/uniform/comparison_throughput_20000000_2147483647_BlockingHeap.dat};

       \coordinate (bot) at (rel axis cs:1,0);% coordinate at bottom of the last plot
  \end{groupplot}
  \path (top-|current bounding box.west)--
       node[anchor=south,rotate=90] {Throughput, mops/s}
       (bot-|current bounding box.west);

  %legend
  \path (top|-current bounding box.north)--
        coordinate(legendpos)
       (bot|-current bounding box.north);
  \matrix[
     matrix of nodes,
     anchor=south,
      draw,
      inner sep=0.2em,
  ] at ([yshift=1ex]legendpos) {
     \ref{plots:fcparallel}& FC Parallel&[5pt]
     \ref{plots:fcparallelv2}& FC Parallel Flush&[5pt]
     \ref{plots:fcbinary}& FC Binary \\
     \ref{plots:fcpairing}& FC Pairing &[5pt]
     \ref{plots:lazyskiplist}& Lazy Skip-List &[5pt]
     \ref{plots:skiplist}& Lock-free Skip-List \\
     \ref{plots:skipqueue}& Skip Queue &[5pt]
     \ref{plots:blocking}& Blocking\\
  };
\end{tikzpicture}



