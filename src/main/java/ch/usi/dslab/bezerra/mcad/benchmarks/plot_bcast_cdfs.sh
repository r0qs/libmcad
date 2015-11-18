#!/bin/bash

size=$1
learners=$2
outpath=$3
tptype=$4
alg1=$5
alg1path=$6
alg2=$7
alg2path=$8
alg3=$9
alg3path=${10}
alg4=${11}
alg4path=${12}
maxlat=${13}

output=${outpath}/cdfs_${learners}_learners_${size}_bytes_${tptype}.ps

gnuplot << END_GNUPLOT
set terminal postscript eps enhanced color solid lw 2 "Helvetica" 18

set title '$path' #offset 0,-0.5

set xlabel "Latency (ms)"
set xtics
set yrange [0:1]
set xrange [0:${maxlat}*1.5]
set ylabel
set ytics
set ytics add (0.90)
set ytics add (0.95)
set ytics add (0.99)
set grid xtics ytics
set output "$output"

plot "$alg1path" using ((\$1)/1e6):2 with lines title "$alg1" lc rgb "red"   ,\
     "$alg2path" using ((\$1)/1e6):2 with lines title "$alg2" lc rgb "green" ,\
     "$alg3path" using ((\$1)/1e6):2 with lines title "$alg3" lc rgb "blue"  ,\
     "$alg4path" using ((\$1)/1e6):2 with lines title "$alg4" lc rgb "black"

END_GNUPLOT

ps2pdf $output
rm $output
