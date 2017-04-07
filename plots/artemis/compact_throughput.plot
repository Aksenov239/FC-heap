\begin{tikzpicture}
   \begin{groupplot}[
       group style={
           group size= 1 by 33,
       },
       height=5cm,
       width=5cm,
   ]
   \nextgroupplot[title=Update rate: 50\%, ylabel={Size: 800000}, cycle list name=color]
       \addplot table {data/w5000-d5000/comparison_throughput_800000_FCParallelHeap.dat};\label{plots:fcparallel}
       \addplot table {data/w5000-d5000/comparison_throughput_800000_FCHalfParallelHeap.dat};\label{plots:fchalfparallel}
       \addplot table {data/w5000-d5000/comparison_throughput_800000_FCBinaryHeap.dat};\label{plots:fcbinary}
       \addplot table {data/w5000-d5000/comparison_throughput_800000_FCPairingHeap.dat};\label{plots:fcpairing}
       \addplot table {data/w5000-d5000/comparison_throughput_800000_LazySkipListHeap.dat};\label{plots:lazyskiplist}
       \addplot table {data/w5000-d5000/comparison_throughput_800000_BlockingHeap.dat};\label{plots:blocking}
       \coordinate (top) at (rel axis cs:0,1);% coordinate at top of the first plot

   \nextgroupplot[ylabel={Size: 2000000}, cycle list name=color]
       \addplot table {data/w5000-d5000/comparison_throughput_2000000_FCParallelHeap.dat};
       \addplot table {data/w5000-d5000/comparison_throughput_2000000_FCHalfParallelHeap.dat};
       \addplot table {data/w5000-d5000/comparison_throughput_2000000_FCBinaryHeap.dat};
       \addplot table {data/w5000-d5000/comparison_throughput_2000000_FCPairingHeap.dat};
       \addplot table {data/w5000-d5000/comparison_throughput_2000000_LazySkipListHeap.dat};
       \addplot table {data/w5000-d5000/comparison_throughput_2000000_BlockingHeap.dat};

   \nextgroupplot[xlabel={Number of Threads}, ylabel={Size: 4000000}, cycle list name=color]
       \addplot table {data/w5000-d5000/comparison_throughput_4000000_FCParallelHeap.dat};
       \addplot table {data/w5000-d5000/comparison_throughput_4000000_FCHalfParallelHeap.dat};
       \addplot table {data/w5000-d5000/comparison_throughput_4000000_FCBinaryHeap.dat};
       \addplot table {data/w5000-d5000/comparison_throughput_4000000_FCPairingHeap.dat};
       \addplot table {data/w5000-d5000/comparison_throughput_4000000_LazySkipListHeap.dat};
       \addplot table {data/w5000-d5000/comparison_throughput_4000000_BlockingHeap.dat};

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
     \ref{plots:fchalfparallel}& FC Half Parallel&[5pt]
     \ref{plots:fcbinary}& FC Binary \\
     \ref{plots:fcpairing}& FC Pairing &[5pt]
     \ref{plots:lazyskiplist}& Lazy Skip-List &[5pt]
     \ref{plots:blocking}& Blocking\\
  };
\end{tikzpicture}



