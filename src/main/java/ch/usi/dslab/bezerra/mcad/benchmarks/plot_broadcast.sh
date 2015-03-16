#!/bin/bash

directory=$1
size=$2

input=${directory}/broadcast_${size}.log

alglinelength=32
# tp cols
# 8  -> tp max
# 15 -> tp 75
# 22 -> tp power
# 29 -> tp 1client

datatypes[0]="max"
tpcols[0]=8

datatypes[1]="75"
tpcols[1]=15

datatypes[2]="power"
tpcols[2]=22

datatypes[3]="1client"
tpcols[3]=29

for i in 0 1 2 3 ;
do

name=${datatypes[i]}
output_tp=${directory}/broadcast_tp_${name}_${size}.ps
output_lat=${directory}/broadcast_lat_${name}_${size}.ps

tpcol=${tpcols[i]}
let latavgcol=tpcol+1
let lat95col=tpcol+3

gnuplot << END_GNUPLOT
set terminal postscript eps enhanced color solid lw 2 "Helvetica" 18

set title "Maximum throughput - ${size}-bytes messages - ${name}"

set xlabel "Destinations"
set ylabel "Throughput (Mbps)"
set grid ytics
set style data histogram
set style histogram cluster gap 1

set style fill solid border rgb "black"
set auto x

#set yrange[0:1000]

set output "$output_tp"

plot "$input" using $[tpcol+alglinelength*0]:xtic(1) title column(5  ) fs solid lc rgb "#FFFFFF",\
    ''        using $[tpcol+alglinelength*1]:xtic(1) title column(37 ) fs solid lc rgb "#CCCCCC",\
    ''        using $[tpcol+alglinelength*2]:xtic(1) title column(69 ) fs solid lc rgb "#999999"

END_GNUPLOT

pstopdf $output_tp
rm $output_tp

gnuplot << END_GNUPLOT
set terminal postscript eps enhanced color solid lw 2 "Helvetica" 18

set title "Latency - ${size}-bytes messages - ${name}"

set xlabel "Destinations"
set ylabel "Latency (ms)"
set grid ytics
set style data histogram
set style histogram errorbars gap 2 lw 1

set style fill solid border rgb "black"
set auto x

set yrange[0:50]

set output "$output_lat"

plot "$input" using $[lat95col+alglinelength*0]:$[latavgcol+alglinelength*0]:$[latavgcol+alglinelength*0]:xtic(1) title column(5 ) fs solid lc rgb "#FFFFFF",\
    ''        using $[lat95col+alglinelength*1]:$[latavgcol+alglinelength*1]:$[latavgcol+alglinelength*1]:xtic(1) title column(37) fs solid lc rgb "#CCCCCC",\
    ''        using $[lat95col+alglinelength*2]:$[latavgcol+alglinelength*2]:$[latavgcol+alglinelength*2]:xtic(1) title column(69) fs solid lc rgb "#999999"

END_GNUPLOT

pstopdf $output_lat
rm $output_lat

done