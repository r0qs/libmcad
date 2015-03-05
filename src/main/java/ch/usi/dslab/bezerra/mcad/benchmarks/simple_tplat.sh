#!/bin/bash

path=$1
msize=$2
tpinput=${path}/throughput.log
latinput=${path}/latency.log
output=${path}/tplat.ps

gnuplot << END_GNUPLOT
set terminal postscript eps enhanced color solid lw 2 "Helvetica" 18

#set bmargin 4.3
#set tmargin 2
#set rmargin 2

#set ylabel offset 1.8,0
#set xtics offset 0,0.2
#set ytics offset 0.45,0
#set yrange [0:$latrange]

set title "Throughput and latency" #offset 0,-0.5

set xlabel "Load (clients)"
set ylabel "Throughput (Mbps)"
set y2label "Latency (ms)"
set y2tics

set output "$output"

plot "$tpinput"  using 1:(\$2*8*$msize)/1e6 with lines title "throughput", \
     "$latinput" using 1:(\$2/1e6)          with lines title "latency" axes x1y2

END_GNUPLOT

pstopdf $output
#rm $output
