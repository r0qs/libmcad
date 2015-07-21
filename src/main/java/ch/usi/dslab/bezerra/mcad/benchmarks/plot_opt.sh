#!/bin/bash

path=$1
msize=$2
latmax=$3
tpinput=${path}/throughput.log
conslatinput=${path}/consLatency.log
optlatinput=${path}/optLatency.log
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

plot "$tpinput"      using 1:(\$2*8*$msize)  /1e6 with linespoints title "throughput" lc rgb "red"  ,\
     "$powerinput"   using 1:(8*$msize*\$2)       with linespoints title "power"      lc rgb "blue" ,\
     "$conslatinput" using 1:(\$2/1e6)            with linespoints title "latency (atomic)"     lc rgb "green"  axes x1y2 ,\
     "$optlatinput"  using 1:(\$2/1e6)            with linespoints title "latency (optimistic)" lc rgb "purple" axes x1y2 ,\
     "$criticals"    using 1:(\$2*8*$msize)  /1e6:(\$3 )*XMAX with circles title "tpmax"    lc rgb "red"  ,\
     "$criticals"    using 4:(\$5*8*$msize)  /1e6:(\$6 )*XMAX with circles title "tp75"     lc rgb "green",\
     "$criticals"    using 7:(\$8*8*$msize)  /1e6:(\$9 )*XMAX with circles title "maxpower" lc rgb "blue" ,\
     "$criticals"    using 10:(\$11*8*$msize)/1e6:(\$12)*XMAX with circles title "1 client" lc rgb "black"

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

pstopdf $output
rm $output

cdfmaxconsinput=${path}/cdf_max_cons.log
cdf75consinput=${path}/cdf_75_cons.log
cdfpowerconsinput=${path}/cdf_power_cons.log
cdf1clientconsinput=${path}/cdf_1client_cons.log
cdfmaxoptinput=${path}/cdf_max_opt.log
cdf75optinput=${path}/cdf_75_opt.log
cdfpoweroptinput=${path}/cdf_power_opt.log
cdf1clientoptinput=${path}/cdf_1client_opt.log
output=${path}/cdfs.ps

gnuplot << END_GNUPLOT
set terminal postscript eps enhanced color dashed lw 2 "Helvetica" 18

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

plot "$cdfmaxconsinput"     using ((\$1)/1e6):2 with lines title "max"      lc rgb "red"   lt 1 ,\
     "$cdf75consinput"      using ((\$1)/1e6):2 with lines title "75"       lc rgb "green" lt 1 ,\
     "$cdfpowerconsinput"   using ((\$1)/1e6):2 with lines title "maxpower" lc rgb "blue"  lt 1 ,\
     "$cdf1clientconsinput" using ((\$1)/1e6):2 with lines title "1 client" lc rgb "black" lt 1 ,\
     "$cdfmaxoptinput"      using ((\$1)/1e6):2 with lines title "max opt"      lc rgb "red"   lt 2 ,\
     "$cdf75optinput"       using ((\$1)/1e6):2 with lines title "75 opt"       lc rgb "green" lt 2 ,\
     "$cdfpoweroptinput"    using ((\$1)/1e6):2 with lines title "maxpower opt" lc rgb "blue"  lt 2 ,\
     "$cdf1clientoptinput"  using ((\$1)/1e6):2 with lines title "1 client opt" lc rgb "black" lt 2

END_GNUPLOT

pstopdf $output
rm $output
