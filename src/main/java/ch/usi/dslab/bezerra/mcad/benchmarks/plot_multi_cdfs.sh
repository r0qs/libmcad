#!/bin/bash

learners=$1
outpath=$2
alg1=$3
alg2=$4
alg3=$5
alg4=$6
alg1path64k=$7
alg2path64k=$8
alg3path64k=$9
alg4path64k=${10}
alg1path8k=${11}
alg2path8k=${12}
alg3path8k=${13}
alg4path8k=${14}
alg1path200=${15}
alg2path200=${16}
alg3path200=${17}
alg4path200=${18}
maxlat64k=${19}
maxlat8k=${20}
maxlat200=${21}
tptype=${22}

output=${outpath}/cdfs_${tptype}_${learners}_learners_multi.ps

# CDF MULTIPLOT
##########################################################
##########################################################
##########################################################
gnuplot << END_GNUPLOT
set terminal postscript size 5,4 eps enhanced color dashed lw 2 "Helvetica" 18
set output "$output"

set size 1,1
set origin 0,0

set multiplot layout 3,1 title ""

##########################################################
# throughput plot 65536 bytes
set size 1,0.33333
set origin 0,0.66667

set tmargin 1.25
set bmargin 2

set lmargin 5
set rmargin 5

set ytics offset 0.75

set title "64 kilobytes messages" offset 0,-0.75

set key top left samplen 1.5 reverse Left
unset xlabel
set xtics offset 0,0.5
#set ylabel "Latency (ms)" offset 2
set grid ytics

#set style fill solid border rgb "black"
set auto x

set xrange[0:${maxlat64k}]
set yrange[0:1]

plot "$alg1path64k" using (\$1/1e6):2 with lines title "$alg1" lc rgb "red"     lt 4 lw 2,\
     "$alg2path64k" using (\$1/1e6):2 with lines title "$alg2" lc rgb "#006400" lt 3 lw 2.5,\
     "$alg3path64k" using (\$1/1e6):2 with lines title "$alg3" lc rgb "blue"    lt 2 lw 3,\
     "$alg4path64k" using (\$1/1e6):2 with lines title "$alg4" lc rgb "black"   lt 1 lw 2
##########################################################

##########################################################
# throughput plot 8192 bytes
set size 1,0.33333
set origin 0,0.33333

set title "8 kilobytes messages"

unset key

unset xlabel
#set ylabel "Latency (ms)"
set grid ytics

#set style fill solid border rgb "black"
set auto x

set xrange[0:${maxlat8k}]

plot "$alg1path8k" using ((\$1)/1e6):2 with lines title "$alg1" lc rgb "red"     lt 4 lw 2,\
     "$alg2path8k" using ((\$1)/1e6):2 with lines title "$alg2" lc rgb "#006400" lt 3 lw 2.5,\
     "$alg3path8k" using ((\$1)/1e6):2 with lines title "$alg3" lc rgb "blue"    lt 2 lw 3,\
     "$alg4path8k" using ((\$1)/1e6):2 with lines title "$alg4" lc rgb "black"   lt 1 lw 2
##########################################################

##########################################################
# throughput plot 200 bytes
set size 1,0.33333
set origin 0,0.0

set title "200 bytes messages"

set xlabel "Latency (ms)" offset 0,1.1
#set ylabel "Latency (ms)" offset 2
set grid ytics

#set style fill solid border rgb "black"
set auto x

set xrange[0:${maxlat200}]

plot "$alg1path200" using ((\$1)/1e6):2 with lines title "$alg1" lc rgb "red"     lt 4 lw 2,\
     "$alg2path200" using ((\$1)/1e6):2 with lines title "$alg2" lc rgb "#006400" lt 3 lw 2.5,\
     "$alg3path200" using ((\$1)/1e6):2 with lines title "$alg3" lc rgb "blue"    lt 2 lw 3,\
     "$alg4path200" using ((\$1)/1e6):2 with lines title "$alg4" lc rgb "black"   lt 1 lw 2
##########################################################
END_GNUPLOT
ps2pdf $output
rm $output
##########################################################
##########################################################
##########################################################
