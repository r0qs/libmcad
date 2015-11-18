#!/bin/bash

path=$1
msize=$2
latmax=$3
tpinput=${path}/throughput.log
latinput=${path}/latency.log
powerinput=${path}/power.log
criticals=${path}/criticals.log
output=${path}/tplat.ps
prettypath=$(echo $path | tr _ " ")

gnuplot << END_GNUPLOT
set terminal postscript eps enhanced color solid lw 2 "Helvetica" 18

#set bmargin 4.3
#set tmargin 2
#set rmargin 2

#set ylabel offset 1.8,0
#set xtics offset 0,0.2
#set ytics offset 0.45,0
#set yrange [0:$latrange]

set title "$prettypath" #offset 0,-0.5

#set logscale x

set xlabel "Load (clients)"
set ylabel "Throughput (Mbps)"
set y2label "Latency (ms)"
set y2tics
set grid ytics

#set xrange[1:*]
set xrange[0:*]
set yrange[0:*]
set y2range[0:*]

set output "$output"

XMAX=200

plot "$tpinput"    using 1:(\$2*8*$msize)  /1e6 with linespoints title "throughput" lc rgb "red"  ,\
     "$powerinput" using 1:(8*$msize*\$2)       with linespoints title "power"      lc rgb "blue" ,\
     "$latinput"   using 1:(\$2/1e6)            with linespoints title "latency"    lc rgb "green" axes x1y2 ,\
     "$criticals"  using 1:(\$2*8*$msize)  /1e6:(\$3 )*XMAX with circles title "tpmax"    lc rgb "red"  ,\
     "$criticals"  using 4:(\$5*8*$msize)  /1e6:(\$6 )*XMAX with circles title "tp75"     lc rgb "green",\
     "$criticals"  using 7:(\$8*8*$msize)  /1e6:(\$9 )*XMAX with circles title "maxpower" lc rgb "blue" ,\
     "$criticals"  using 10:(\$11*8*$msize)/1e6:(\$12)*XMAX with circles title "1 client" lc rgb "black"

set output "$output"

XMAX=GPVAL_X_MAX

replot

#plot "$tpinput"    using 1:(\$2*8*$msize)  /1e6 with linespoints title "throughput" lc rgb "red"  ,\
#     "$powerinput" using 1:(8*$msize*\$2)       with linespoints title "power"      lc rgb "blue" ,\
#     "$latinput"   using 1:(\$2/1e6)            with linespoints title "latency"    lc rgb "green" axes x1y2 ,\
#     "$criticals"  using 1:(\$2*8*$msize)  /1e6:(\$3 )*XMAX with circles title "tpmax"    lc rgb "red"  ,\
#     "$criticals"  using 4:(\$5*8*$msize)  /1e6:(\$6 )*XMAX with circles title "tp75"     lc rgb "green",\
#     "$criticals"  using 7:(\$8*8*$msize)  /1e6:(\$9 )*XMAX with circles title "maxpower" lc rgb "blue" ,\
#     "$criticals"  using 10:(\$11*8*$msize)/1e6:(\$12)*XMAX with circles title "1 client" lc rgb "black"

END_GNUPLOT

ps2pdf $output
rm $output

cdfmaxinput=${path}/cdf_max.log
cdf75input=${path}/cdf_75.log
cdfpowerinput=${path}/cdf_power.log
cdf1client=${path}/cdf_1client.log
output=${path}/cdfs.ps

gnuplot << END_GNUPLOT
set terminal postscript eps enhanced color solid lw 2 "Helvetica" 18

set title '$prettypath' #offset 0,-0.5

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

plot "$cdfmaxinput"   using ((\$1)/1e6):2 with lines title "max"      lc rgb "red"   ,\
     "$cdf75input"    using ((\$1)/1e6):2 with lines title "75"       lc rgb "green" ,\
     "$cdfpowerinput" using ((\$1)/1e6):2 with lines title "maxpower" lc rgb "blue"  ,\
     "$cdf1client"    using ((\$1)/1e6):2 with lines title "1 client" lc rgb "black"

END_GNUPLOT

ps2pdf $output
rm $output
