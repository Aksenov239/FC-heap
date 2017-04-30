\begin{tikzpicture}
   \begin{groupplot}[
       group style={
           group size= 3 by 3,
       },
       height=5cm,
       width=5cm,
   ]
   \nextgroupplot[title=Range: 100, ylabel={Size: 800000}, cycle list name=color]
       \addplot table {data/w10000-d10000/comparison_throughput_800000_100_FCParallelHeap.dat};\label{plots:fcparallel}
%       \addplot table {data/w10000-d10000/comparison_throughput_800000_FCHalfParallelHeap.dat};\label{plots:fchalfparallel}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_100_FCBinaryHeap.dat};\label{plots:fcbinary}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_100_FCPairingHeap.dat};\label{plots:fcpairing}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_100_LazySkipListHeap.dat};\label{plots:lazyskiplist}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_100_SkipListHeap.dat};\label{plots:skiplist}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_100_BlockingHeap.dat};\label{plots:blocking}
       \coordinate (top) at (rel axis cs:0,1);% coordinate at top of the first plot

   \nextgroupplot[title=Range: $10^4$, cycle list name=color]
       \addplot table {data/w10000-d10000/comparison_throughput_800000_10000_FCParallelHeap.dat};\label{plots:fcparallel}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_10000_FCBinaryHeap.dat};\label{plots:fcbinary}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_10000_FCPairingHeap.dat};\label{plots:fcpairing}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_10000_LazySkipListHeap.dat};\label{plots:lazyskiplist}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_10000_SkipListHeap.dat};\label{plots:skiplist}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_10000_BlockingHeap.dat};\label{plots:blocking}
       \coordinate (top) at (rel axis cs:0,1);% coordinate at top of the first plot

   \nextgroupplot[title=Range: $2 \times $\,size, cycle list name=color]
       \addplot table {data/w10000-d10000/comparison_throughput_800000_1600000_FCParallelHeap.dat};\label{plots:fcparallel}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_1600000_FCBinaryHeap.dat};\label{plots:fcbinary}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_1600000_FCPairingHeap.dat};\label{plots:fcpairing}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_1600000_LazySkipListHeap.dat};\label{plots:lazyskiplist}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_1600000_SkipListHeap.dat};\label{plots:skiplist}
       \addplot table {data/w10000-d10000/comparison_throughput_800000_1600000_BlockingHeap.dat};\label{plots:blocking}
       \coordinate (top) at (rel axis cs:0,1);% coordinate at top of the first plot

   \nextgroupplot[ylabel={Size: 2000000}, cycle list name=color]
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_100_FCParallelHeap.dat};
%       \addplot table {data/w10000-d10000/comparison_throughput_2000000_100_FCHalfParallelHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_100_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_100_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_100_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_100_SkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_100_BlockingHeap.dat};

   \nextgroupplot[cycle list name=color]
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_10000_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_10000_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_10000_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_10000_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_10000_SkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_10000_BlockingHeap.dat};

   \nextgroupplot[cycle list name=color]
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_4000000_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_4000000_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_4000000_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_4000000_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_4000000_SkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_2000000_4000000_BlockingHeap.dat};

   \nextgroupplot[ylabel={Size: 4000000}, cycle list name=color]
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_100_FCParallelHeap.dat};
%       \addplot table {data/w10000-d10000/comparison_throughput_4000000_100_FCHalfParallelHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_100_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_100_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_100_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_100_SkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_100_BlockingHeap.dat};

   \nextgroupplot[cycle list name=color]
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_10000_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_10000_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_10000_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_10000_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_10000_SkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_10000_BlockingHeap.dat};

   \nextgroupplot[cycle list name=color]
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_8000000_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_8000000_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_8000000_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_8000000_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_8000000_SkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_4000000_8000000_BlockingHeap.dat};

  \nextgroupplot[xlabel={Number of Threads}, ylabel={Size: 8000000}, cycle list name=color]
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_100_FCParallelHeap.dat};
%       \addplot table {data/w10000-d10000/comparison_throughput_8000000_100_FCHalfParallelHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_100_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_100_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_100_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_100_SkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_100_BlockingHeap.dat};

   \nextgroupplot[xlabel={Number of Threads}, cycle list name=color]
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_10000_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_10000_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_10000_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_10000_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_10000_SkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_10000_BlockingHeap.dat};
 
   \nextgroupplot[xlabel={Number of Threads}, cycle list name=color]
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_16000000_FCParallelHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_16000000_FCBinaryHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_16000000_FCPairingHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_16000000_LazySkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_16000000_SkipListHeap.dat};
       \addplot table {data/w10000-d10000/comparison_throughput_8000000_16000000_BlockingHeap.dat};


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
%     \ref{plots:fchalfparallel}& FC Half Parallel&[5pt]
     \ref{plots:fcbinary}& FC Binary \\
     \ref{plots:fcpairing}& FC Pairing &[5pt]
%     \ref{plots:lazyskiplist}& Lazy Skip-List &[5pt]
     \ref{plots:lazyskiplist}& Lock-free Skip-List \\
     \ref{plots:blocking}& Blocking\\
  };
\end{tikzpicture}



