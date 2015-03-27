#!/bin/bash

directory=$1

input200=${directory}/multicast_200.log 
input8k=${directory}/multicast_8192.log
input64k=${directory}/multicast_65536.log

alglinelength=33
# tp cols
# 9  -> tp max
# 16 -> tp 75
# 23 -> tp power
# 30 -> tp 1client

algnamecol=6

datatypes[0]="max"
tpcols[0]=9
maxlat64k[0]="*"
maxlat8k[0]="*"
maxlat200[0]="*"
latlabeloff64k[0]="2"
latlabeloff8k[0]="2"
latlabeloff200[0]="2"

datatypes[1]="75"
tpcols[1]=16
maxlat64k[1]="*"
maxlat8k[1]="*"
maxlat200[1]="*"
latlabeloff64k[1]="2.5"
latlabeloff8k[1]="1.5"
latlabeloff200[1]="2.5"

datatypes[2]="power"
tpcols[2]=23
maxlat64k[2]="40"
maxlat8k[2]="10"
maxlat200[2]="10"
latlabeloff64k[2]="1.75"
latlabeloff8k[2]="1.75"
latlabeloff200[2]="1.75"

datatypes[3]="1client"
tpcols[3]=30
maxlat64k[3]="60"
maxlat8k[3]="*"
maxlat200[3]="8"
latlabeloff64k[3]="1.75"
latlabeloff8k[3]="1.75"
latlabeloff200[3]="0.75"

for i in 0 1 2 3 ;
do

name=${datatypes[i]}
output_tp=${directory}/multicast_tp_${name}_multi.ps
output_lat=${directory}/multicast_lat_${name}_multi.ps

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
set ylabel "Throughput (Gbps)" offset 1
set ytics 0,2,8
set grid ytics
set style data histogram
set style histogram cluster gap 3

set style fill solid border rgb "black"
set auto x

set yrange[0:8]


plot "$input64k" using (\$$[tpcol+alglinelength*0])/1e3:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''          using (\$$[tpcol+alglinelength*1])/1e3:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#999999",\
     ''          using (\$$[tpcol+alglinelength*2])/1e3:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#666666"
##########################################################

##########################################################
# throughput plot 8192 bytes
set size 1,0.35
set origin 0,0.325

set title "8 kiloBytes messages"

unset key

unset xlabel
unset xtics
set ylabel "Throughput (Gbps)" offset 3
set ytics 0,0.5,2
set grid ytics
set style data histogram
set style histogram cluster gap 3

set style fill solid border rgb "black"
set auto x

set yrange[0:2]

plot "$input8k" using (\$$[tpcol+alglinelength*0])/1e3:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''         using (\$$[tpcol+alglinelength*1])/1e3:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#999999",\
     ''         using (\$$[tpcol+alglinelength*2])/1e3:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#666666"
##########################################################

##########################################################
# throughput plot 200 bytes
set size 1,0.35
set origin 0,0.0125

set title "200 Bytes messages"

set xlabel "Multicast groups" offset 0,1.0
set ylabel "Throughput (Mbps)" offset 3
set ytics 0,30,120
set xtics offset 0,0.5
set grid ytics
set style data histogram
set style histogram cluster gap 3

set style fill solid border rgb "black"
set auto x

set yrange[0:120]

plot "$input200" using (\$$[tpcol+alglinelength*0]):xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''          using (\$$[tpcol+alglinelength*1]):xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#999999",\
     ''          using (\$$[tpcol+alglinelength*2]):xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#666666"
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
set ylabel "Latency (ms)" offset ${latlabeloff64k[i]}
set ytics 0,10
set grid ytics
set style data histogram
set style histogram cluster errorbars gap 3 lw 1

set style fill solid border rgb "black"
set auto x

set yrange[0:${maxlat64k[i]}]


plot "$input64k" using $[lat95col+alglinelength*0]:$[latavgcol+alglinelength*0]:$[latavgcol+alglinelength*0]:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''          using $[lat95col+alglinelength*1]:$[latavgcol+alglinelength*1]:$[latavgcol+alglinelength*1]:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#999999",\
     ''          using $[lat95col+alglinelength*2]:$[latavgcol+alglinelength*2]:$[latavgcol+alglinelength*2]:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#666666"

##########################################################

##########################################################
# latency plot 8192 bytes
set size 1,0.35
set origin 0,0.325

set title "8 kiloBytes messages"

unset key

unset xlabel
unset xtics
set ylabel "Latency (ms)" offset ${latlabeloff8k[i]}
set ytics 0,2
set grid ytics
set style data histogram
set style histogram cluster errorbars gap 3 lw 1

set style fill solid border rgb "black"
set auto x

set yrange[0:${maxlat8k[i]}]

plot "$input8k" using $[lat95col+alglinelength*0]:$[latavgcol+alglinelength*0]:$[latavgcol+alglinelength*0]:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''         using $[lat95col+alglinelength*1]:$[latavgcol+alglinelength*1]:$[latavgcol+alglinelength*1]:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#999999",\
     ''         using $[lat95col+alglinelength*2]:$[latavgcol+alglinelength*2]:$[latavgcol+alglinelength*2]:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#666666"
##########################################################

##########################################################
# latency plot 200 bytes
set size 1,0.35
set origin 0,0.0125

set title "200 Bytes messages"

set xlabel "Multicast groups" offset 0,1.0
set ylabel "Latency (ms)" offset ${latlabeloff200[i]}
set ytics 0,2
set xtics offset 0,0.5
set grid ytics
set style data histogram
set style histogram cluster errorbars gap 3 lw 1

set style fill solid border rgb "black"
set auto x

set yrange[0:${maxlat200[i]}]

plot "$input200" using $[lat95col+alglinelength*0]:$[latavgcol+alglinelength*0]:$[latavgcol+alglinelength*0]:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''          using $[lat95col+alglinelength*1]:$[latavgcol+alglinelength*1]:$[latavgcol+alglinelength*1]:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#999999",\
     ''          using $[lat95col+alglinelength*2]:$[latavgcol+alglinelength*2]:$[latavgcol+alglinelength*2]:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#666666"
##########################################################
END_GNUPLOT
pstopdf $output_lat
rm $output_lat
##########################################################
##########################################################
##########################################################



done