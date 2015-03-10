#!/bin/bash

path=$1
msize=$2
latmax=$3
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
set grid ytics

set xrange[0:*]
set yrange[0:*]
set y2range[0:*]

set output "$output"

plot "$tpinput"  using 1:(\$2*8*$msize)/1e6 with linespoints title "throughput", \
     "$latinput" using 1:(\$2/1e6)          with linespoints title "latency" axes x1y2

END_GNUPLOT

pstopdf $output
rm $output

cdfmaxinput=${path}/cdf_max.log
cdf75input=${path}/cdf_75.log
output=${path}/cdfs.ps

gnuplot << END_GNUPLOT
set terminal postscript eps enhanced color solid lw 2 "Helvetica" 18

set title "Latency CDF" #offset 0,-0.5

set xlabel "Latency (ms)"
set xtics
set yrange [0:1]
set xrange [0:$latmax/1e6]
set ylabel
set ytics
set ytics add (0.90)
set ytics add (0.95)
set ytics add (0.99)
set grid xtics ytics
set output "$output"

plot "$cdfmaxinput" using ((\$1)/1e6):2 with lines title "max", \
     "$cdf75input"  using ((\$1)/1e6):2 with lines title "75"

END_GNUPLOT

pstopdf $output
rm $output