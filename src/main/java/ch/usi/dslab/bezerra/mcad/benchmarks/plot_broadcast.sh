#!/bin/bash

directory=$1
size=$2

input=${directory}/broadcast_${size}.log
output_tp=${directory}/broadcast_tp_${size}.ps
output_lat=${directory}/broadcast_lat_${size}.ps

gnuplot << END_GNUPLOT
set terminal postscript eps enhanced color solid lw 2 "Helvetica" 18

set xlabel "Destinations"
set ylabel "Throughput (Mbps)"
set grid ytics
set style data histogram
set style histogram cluster gap 1

set style fill solid border rgb "black"
set auto x

set output "$output_tp"

plot "$input" using 4 :xtic(1) title column(2 ) fs solid lc rgb "#FFFFFF",\
    ''        using 10:xtic(1) title column(8 ) fs solid lc rgb "#CCCCCC",\
    ''        using 16:xtic(1) title column(14) fs solid lc rgb "#999999"

END_GNUPLOT

pstopdf $output_tp
rm $output_tp

gnuplot << END_GNUPLOT
set terminal postscript eps enhanced color solid lw 2 "Helvetica" 18

set xlabel "Destinations"
set ylabel "Latency (ms)"
set grid ytics
set style data histogram
set style histogram errorbars gap 2 lw 1

set style fill solid border rgb "black"
set auto x

set output "$output_lat"

plot "$input" using 5 :6 :6 :xtic(1) title column(2 ) fs solid lc rgb "#FFFFFF",\
    ''        using 11:12:12:xtic(1) title column(8 ) fs solid lc rgb "#CCCCCC",\
    ''        using 17:18:18:xtic(1) title column(14) fs solid lc rgb "#999999"    

END_GNUPLOT

pstopdf $output_lat
rm $output_lat