#!/bin/bash

directory=$1

input100=${directory}/broadcast_100.log
input200=${directory}/broadcast_200.log 
input1k=${directory}/broadcast_1024.log
input4k=${directory}/broadcast_4096.log
input8k=${directory}/broadcast_8192.log
#input64k=${directory}/broadcast_65536.log

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
maxlat8k[0]="400"
maxlat4k[0]="250"
maxlat1k[0]="120"
maxlat200[0]="120"
maxlat100[0]="140"
maxtp8k[0]="*"
maxtp4k[0]="*"
maxtp1k[0]="*"
maxtp200[0]="*"
maxtp100[0]="*"
latlabeloff64k[0]="2"
latlabeloff8k[0]="2"
latlabeloff4k[0]="2"
latlabeloff1k[0]="2"
latlabeloff200[0]="2"
latlabeloff100[0]="2"

datatypes[1]="75"
tpcols[1]=16
maxlat64k[1]="*"
maxlat8k[1]="130"
maxlat4k[1]="200"
maxlat1k[1]="70"
maxlat200[1]="70"
maxlat100[1]="60"
maxtp8k[1]="*"
maxtp4k[1]="*"
maxtp1k[1]="*"
maxtp200[1]="*"
maxtp100[1]="*"
latlabeloff64k[1]="2.5"
latlabeloff8k[1]="1.5"
latlabeloff4k[1]="1.5"
latlabeloff1k[1]="1.5"
latlabeloff200[1]="1.5"
latlabeloff100[1]="1.5"

datatypes[2]="power"
tpcols[2]=23
maxlat64k[2]="*"
maxlat8k[2]="80"
maxlat4k[2]="80"
maxlat1k[2]="80"
maxlat200[2]="80"
maxlat100[2]="80"
maxtp8k[2]="*"
maxtp4k[2]="*"
maxtp1k[2]="*"
maxtp200[2]="*"
maxtp100[2]="*"
latlabeloff64k[2]="2.75"
latlabeloff8k[2]="1.75"
latlabeloff4k[2]="1.75"
latlabeloff1k[2]="1.75"
latlabeloff200[2]="1.75"
latlabeloff100[2]="1.75"

datatypes[3]="1client"
tpcols[3]=30
maxlat64k[3]="60"
maxlat8k[3]="60"
maxlat4k[3]="60"
maxlat1k[3]="60"
maxlat200[3]="60"
maxlat100[3]="60"
maxtp8k[3]="*"
maxtp4k[3]="*"
maxtp1k[3]="*"
maxtp200[3]="*"
maxtp100[3]="*"
latlabeloff64k[3]="1.75"
latlabeloff8k[3]="1.75"
latlabeloff4k[3]="1.75"
latlabeloff1k[3]="1.75"
latlabeloff200[3]="0.75"
latlabeloff100[3]="0.75"

for i in 0 1 2 3 ;
do

name=${datatypes[i]}
output_tp=${directory}/broadcast_tp_${name}_multi.ps
output_lat=${directory}/broadcast_lat_${name}_multi.ps

tpcol=${tpcols[i]}
let latavgcol=tpcol+1
let lat95col=tpcol+3

##########################################################
# THROUGHPUT MULTIPLOT
##########################################################
gnuplot << END_GNUPLOT
set terminal postscript size 5,4 eps enhanced color solid lw 2 "Helvetica" 18
set output "$output_tp"

set size 1,1
set origin 0,0

set multiplot layout 5,1 title ""

##########################################################
# throughput plot 8192 bytes
set size 1,0.35
set origin 0,1.225

set title "8 kilobytes messages"

unset xlabel
unset xtics
set ylabel "Throughput (Msg/sec)" offset ${latlabeloff8k[i]}
set grid ytics
set style data histogram
set style histogram cluster gap 3

set style fill solid border rgb "black"
set auto x

set yrange[0:${maxtp8k[i]}]

plot "$input8k" using $[tpcol+alglinelength*0]:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''         using $[tpcol+alglinelength*1]:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#CCCCCC",\
     ''         using $[tpcol+alglinelength*2]:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#999999"
##########################################################


##########################################################
# throughput plot 4096 bytes
set size 1,0.35
set origin 0,0.925

set title "4 kilobytes messages"

unset xlabel
unset xtics
set ylabel "Throughput (Msg/sec)" offset ${latlabeloff4k[i]}
set grid ytics
set style data histogram
set style histogram cluster gap 3

set style fill solid border rgb "black"
set auto x

set yrange[0:${maxtp4k[i]}]

plot "$input4k" using $[tpcol+alglinelength*0]:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''         using $[tpcol+alglinelength*1]:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#CCCCCC",\
     ''         using $[tpcol+alglinelength*2]:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#999999"
##########################################################

##########################################################
# throughput plot 1024 bytes
set size 1,0.35
set origin 0,0.625

set title "1 kilobyte messages"

unset xlabel
unset xtics
set ylabel "Throughput (Msg/sec)" offset ${latlabeloff1k[i]}
set grid ytics
set style data histogram
set style histogram cluster gap 3

set style fill solid border rgb "black"
set auto x

unset key

set yrange[0:${maxtp1k[i]}]

plot "$input1k" using (\$$[tpcol+alglinelength*0])/1e3:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''         using (\$$[tpcol+alglinelength*1])/1e3:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#CCCCCC",\
     ''         using (\$$[tpcol+alglinelength*2])/1e3:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#999999"
##########################################################

##########################################################
# throughput plot 200 bytes
set size 1,0.35
set origin 0,0.325

set title "200 bytes messages"

unset xlabel
unset xtics
set ylabel "Throughput (Msg/sec)" offset ${latlabeloff200[i]}
set grid ytics
set style data histogram
set style histogram cluster gap 3

set style fill solid border rgb "black"
set auto x

unset key

set yrange[0:${maxtp200[i]}]

plot "$input200" using (\$$[tpcol+alglinelength*0])/1e3:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''         using (\$$[tpcol+alglinelength*1])/1e3:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#CCCCCC",\
     ''         using (\$$[tpcol+alglinelength*2])/1e3:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#999999"
##########################################################

##########################################################
# throughput plot 100 bytes
set size 1,0.35
set origin 0,0.0125

set title "100 bytes messages"

set xlabel "Replicas" offset 0,0.5
set ylabel "Throughput (Msg/sec)" offset ${latlabeloff100[i]}
set xtics offset 0,0.2
set grid ytics
set style data histogram
set style histogram cluster gap 3

set style fill solid border rgb "black"
set auto x

unset key

set yrange[0:${maxtp100[i]}]

plot "$input100" using (\$$[tpcol+alglinelength*0]):xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''          using (\$$[tpcol+alglinelength*1]):xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#CCCCCC",\
     ''          using (\$$[tpcol+alglinelength*2]):xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#999999"
##########################################################
END_GNUPLOT
ps2pdf $output_tp
#rm $output_tp
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

set multiplot layout 5,1 title ""

##########################################################
# latency plot 8192 bytes
set size 1,0.35
set origin 0,1.225

set title "8 kilobytes messages"

unset xlabel
unset xtics
set ylabel "Latency (ms)" offset ${latlabeloff8k[i]}
set grid ytics
set style data histogram
set style histogram cluster errorbars gap 3 lw 1

set style fill solid border rgb "black"
set auto x

set yrange[0:${maxlat8k[i]}]

plot "$input8k" using $[lat95col+alglinelength*0]:$[latavgcol+alglinelength*0]:$[latavgcol+alglinelength*0]:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''         using $[lat95col+alglinelength*1]:$[latavgcol+alglinelength*1]:$[latavgcol+alglinelength*1]:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#CCCCCC",\
     ''         using $[lat95col+alglinelength*2]:$[latavgcol+alglinelength*2]:$[latavgcol+alglinelength*2]:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#999999"
##########################################################

##########################################################
# latency plot 4096 bytes
set size 1,0.35
set origin 0,0.925

set title "4 kilobytes messages"

unset xlabel
unset xtics
set ylabel "Latency (ms)" offset ${latlabeloff4k[i]}
set grid ytics
set style data histogram
set style histogram cluster errorbars gap 3 lw 1

set style fill solid border rgb "black"
set auto x

set yrange[0:${maxlat4k[i]}]

plot "$input4k" using $[lat95col+alglinelength*0]:$[latavgcol+alglinelength*0]:$[latavgcol+alglinelength*0]:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''         using $[lat95col+alglinelength*1]:$[latavgcol+alglinelength*1]:$[latavgcol+alglinelength*1]:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#CCCCCC",\
     ''         using $[lat95col+alglinelength*2]:$[latavgcol+alglinelength*2]:$[latavgcol+alglinelength*2]:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#999999"
##########################################################

##########################################################
# latency plot 1 kilobytes
set size 1,0.35
set origin 0,0.625

set title "1 kilobyte messages"

unset xlabel
unset xtics
set ylabel "Latency (ms)" offset ${latlabeloff1k[i]}
set grid ytics
set style data histogram
set style histogram cluster errorbars gap 3 lw 1

set style fill solid border rgb "black"
set auto x

unset key

set yrange[0:${maxlat1k[i]}]

plot "$input1k" using $[lat95col+alglinelength*0]:$[latavgcol+alglinelength*0]:$[latavgcol+alglinelength*0]:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''         using $[lat95col+alglinelength*1]:$[latavgcol+alglinelength*1]:$[latavgcol+alglinelength*1]:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#CCCCCC",\
     ''         using $[lat95col+alglinelength*2]:$[latavgcol+alglinelength*2]:$[latavgcol+alglinelength*2]:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#999999"
##########################################################


##########################################################
# latency plot 200 bytes
set size 1,0.35
set origin 0,0.325

set title "200 bytes messages"

unset xlabel
unset xtics
set ylabel "Latency (ms)" offset ${latlabeloff200[i]}
set grid ytics
set style data histogram
set style histogram cluster errorbars gap 3 lw 1

set style fill solid border rgb "black"
set auto x

unset key

set yrange[0:${maxlat200[i]}]

plot "$input200" using $[lat95col+alglinelength*0]:$[latavgcol+alglinelength*0]:$[latavgcol+alglinelength*0]:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''         using $[lat95col+alglinelength*1]:$[latavgcol+alglinelength*1]:$[latavgcol+alglinelength*1]:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#CCCCCC",\
     ''         using $[lat95col+alglinelength*2]:$[latavgcol+alglinelength*2]:$[latavgcol+alglinelength*2]:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#999999"
##########################################################

##########################################################
# latency plot 100 bytes
set size 1,0.35
set origin 0,0.0125

set title "100 bytes messages"

set xlabel "Replicas" offset 0,0.5
set ylabel "Latency (ms)" offset ${latlabeloff100[i]}
set xtics offset 0,0.2
set grid ytics
set style data histogram
set style histogram cluster errorbars gap 3 lw 1

set style fill solid border rgb "black"
set auto x

unset key

set yrange[0:${maxlat100[i]}]

plot "$input100" using $[lat95col+alglinelength*0]:$[latavgcol+alglinelength*0]:$[latavgcol+alglinelength*0]:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''          using $[lat95col+alglinelength*1]:$[latavgcol+alglinelength*1]:$[latavgcol+alglinelength*1]:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#CCCCCC",\
     ''          using $[lat95col+alglinelength*2]:$[latavgcol+alglinelength*2]:$[latavgcol+alglinelength*2]:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#999999"
##########################################################
END_GNUPLOT
ps2pdf $output_lat
#rm $output_lat
##########################################################
##########################################################
##########################################################



done
