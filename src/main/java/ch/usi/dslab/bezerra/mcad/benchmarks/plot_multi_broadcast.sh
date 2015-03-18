#!/bin/bash

directory=$1

input200=${directory}/broadcast_200.log 
input8k=${directory}/broadcast_8192.log
input64k=${directory}/broadcast_65536.log

alglinelength=32
# tp cols
# 8  -> tp max
# 15 -> tp 75
# 22 -> tp power
# 29 -> tp 1client

datatypes[0]="max"
tpcols[0]=8
maxlat64k[0]="*"
maxlat8k[0]="*"
maxlat200[0]="*"

datatypes[1]="75"
tpcols[1]=15
maxlat64k[1]="*"
maxlat8k[1]="*"
maxlat200[1]="*"

datatypes[2]="power"
tpcols[2]=22
maxlat64k[2]="80"
maxlat8k[2]="*"
maxlat200[2]="*"

datatypes[3]="1client"
tpcols[3]=29
maxlat64k[3]="60"
maxlat8k[3]="*"
maxlat200[3]="8"

for i in 0 1 2 3 ;
do

name=${datatypes[i]}
output_tp=${directory}/broadcast_tp_${name}_multi.ps
output_lat=${directory}/broadcast_lat_${name}_multi.ps

tpcol=${tpcols[i]}
let latavgcol=tpcol+1
let lat95col=tpcol+3

# THROUGHPUT MULTIPLOT
 ##########################################################
##########################################################
##########################################################
gnuplot << END_GNUPLOT
set terminal postscript size 5,4 eps enhanced color solid lw 2 "Helvetica" 18
set output "$output_tp"

set size 1,1
set origin 0,0

set multiplot layout 3,1 title ""

##########################################################
# throughput plot 65536 bytes
set size 1,0.35
set origin 0,0.6375

set tmargin 1.25
set bmargin 2

set lmargin 6
set rmargin 5

set ytics offset 0.75

set title "64 kiloBytes messages" offset 0,-0.75

set key top right maxrows 1 samplen 1.5
unset xlabel
unset xtics
set ylabel "Throughput (Gbps)" offset 3
set grid ytics
set style data histogram
set style histogram cluster gap 3

set style fill solid border rgb "black"
set auto x

set yrange[0:1]


plot "$input64k" using (\$$[tpcol+alglinelength*0])/1e3:xtic(1) title column(5  ) fs solid lc rgb "#FFFFFF",\
     ''          using (\$$[tpcol+alglinelength*1])/1e3:xtic(1) title column(37 ) fs solid lc rgb "#CCCCCC",\
     ''          using (\$$[tpcol+alglinelength*2])/1e3:xtic(1) title column(69 ) fs solid lc rgb "#999999",\
     ''          using (\$$[tpcol+alglinelength*3])/1e3:xtic(1) title column(101) fs solid lc rgb "#666666"
##########################################################

##########################################################
# throughput plot 8192 bytes
set size 1,0.35
set origin 0,0.325

set title "8 kiloBytes messages"

unset key

unset xlabel
unset xtics
#set ylabel "Throughput (Mbps)"
set grid ytics
set style data histogram
set style histogram cluster gap 3

set style fill solid border rgb "black"
set auto x

set yrange[0:1]

plot "$input8k" using (\$$[tpcol+alglinelength*0])/1e3:xtic(1) title column(5  ) fs solid lc rgb "#FFFFFF",\
     ''         using (\$$[tpcol+alglinelength*1])/1e3:xtic(1) title column(37 ) fs solid lc rgb "#CCCCCC",\
     ''         using (\$$[tpcol+alglinelength*2])/1e3:xtic(1) title column(69 ) fs solid lc rgb "#999999",\
     ''         using (\$$[tpcol+alglinelength*3])/1e3:xtic(1) title column(101) fs solid lc rgb "#666666"
##########################################################

##########################################################
# throughput plot 200 bytes
set size 1,0.35
set origin 0,0.0125

set title "200 Bytes messages"

set xlabel "Destinations" offset 0,1.0
set ylabel "Throughput (Mbps)" offset 3
set xtics offset 0,0.5
set grid ytics
set style data histogram
set style histogram cluster gap 3

set style fill solid border rgb "black"
set auto x

set yrange[0:100]

plot "$input200" using $[tpcol+alglinelength*0]:xtic(1) title column(5  ) fs solid lc rgb "#FFFFFF",\
     ''          using $[tpcol+alglinelength*1]:xtic(1) title column(37 ) fs solid lc rgb "#CCCCCC",\
     ''          using $[tpcol+alglinelength*2]:xtic(1) title column(69 ) fs solid lc rgb "#999999",\
     ''          using $[tpcol+alglinelength*3]:xtic(1) title column(101) fs solid lc rgb "#666666"
##########################################################
END_GNUPLOT
pstopdf $output_tp
rm $output_tp
##########################################################
##########################################################
##########################################################


# LATENCY MULTIPLOT
##########################################################
##########################################################
##########################################################
gnuplot << END_GNUPLOT
set terminal postscript size 5,4 eps enhanced color solid lw 2 "Helvetica" 18
set output "$output_lat"

set size 1,1
set origin 0,0

set multiplot layout 3,1 title ""

##########################################################
# latency plot 65536 bytes
set size 1,0.35
set origin 0,0.6375

set tmargin 1.25
set bmargin 2

set lmargin 6
set rmargin 5

set ytics offset 0.75

set title "64 kiloBytes messages" offset 0,-0.75

set key top right maxrows 1 samplen 1.5
unset xlabel
unset xtics
set ylabel "Latency (ms)" offset 2
set grid ytics
set style data histogram
set style histogram errorbars gap 3 lw 1

set style fill solid border rgb "black"
set auto x

set yrange[0:${maxlat64k[i]}]


plot "$input64k" using $[lat95col+alglinelength*0]:$[latavgcol+alglinelength*0]:$[latavgcol+alglinelength*0]:xtic(1) title column(5  ) fs solid lc rgb "#FFFFFF",\
     ''          using $[lat95col+alglinelength*1]:$[latavgcol+alglinelength*1]:$[latavgcol+alglinelength*1]:xtic(1) title column(37 ) fs solid lc rgb "#CCCCCC",\
     ''          using $[lat95col+alglinelength*2]:$[latavgcol+alglinelength*2]:$[latavgcol+alglinelength*2]:xtic(1) title column(69 ) fs solid lc rgb "#999999",\
     ''          using $[lat95col+alglinelength*3]:$[latavgcol+alglinelength*3]:$[latavgcol+alglinelength*3]:xtic(1) title column(101) fs solid lc rgb "#666666"

##########################################################

##########################################################
# latency plot 8192 bytes
set size 1,0.35
set origin 0,0.325

set title "8 kiloBytes messages"

unset key

unset xlabel
unset xtics
#set ylabel "Latency (ms)"
set grid ytics
set style data histogram
set style histogram errorbars gap 2 lw 1

set style fill solid border rgb "black"
set auto x

set yrange[0:${maxlat8k[i]}]

plot "$input8k" using $[lat95col+alglinelength*0]:$[latavgcol+alglinelength*0]:$[latavgcol+alglinelength*0]:xtic(1) title column(5  ) fs solid lc rgb "#FFFFFF",\
     ''         using $[lat95col+alglinelength*1]:$[latavgcol+alglinelength*1]:$[latavgcol+alglinelength*1]:xtic(1) title column(37 ) fs solid lc rgb "#CCCCCC",\
     ''         using $[lat95col+alglinelength*2]:$[latavgcol+alglinelength*2]:$[latavgcol+alglinelength*2]:xtic(1) title column(69 ) fs solid lc rgb "#999999",\
     ''         using $[lat95col+alglinelength*3]:$[latavgcol+alglinelength*3]:$[latavgcol+alglinelength*3]:xtic(1) title column(101) fs solid lc rgb "#666666"
##########################################################

##########################################################
# latency plot 200 bytes
set size 1,0.35
set origin 0,0.0125

set title "200 Bytes messages"

set xlabel "Destinations" offset 0,1.0
set ylabel "Latency (ms)" offset 1
set xtics offset 0,0.5
set grid ytics
set style data histogram
set style histogram errorbars gap 2 lw 1

set style fill solid border rgb "black"
set auto x

set yrange[0:${maxlat200[i]}]

plot "$input200" using $[lat95col+alglinelength*0]:$[latavgcol+alglinelength*0]:$[latavgcol+alglinelength*0]:xtic(1) title column(5  ) fs solid lc rgb "#FFFFFF",\
     ''          using $[lat95col+alglinelength*1]:$[latavgcol+alglinelength*1]:$[latavgcol+alglinelength*1]:xtic(1) title column(37 ) fs solid lc rgb "#CCCCCC",\
     ''          using $[lat95col+alglinelength*2]:$[latavgcol+alglinelength*2]:$[latavgcol+alglinelength*2]:xtic(1) title column(69 ) fs solid lc rgb "#999999",\
     ''          using $[lat95col+alglinelength*3]:$[latavgcol+alglinelength*3]:$[latavgcol+alglinelength*3]:xtic(1) title column(101) fs solid lc rgb "#666666"
##########################################################
END_GNUPLOT
pstopdf $output_lat
rm $output_lat
##########################################################
##########################################################
##########################################################



done