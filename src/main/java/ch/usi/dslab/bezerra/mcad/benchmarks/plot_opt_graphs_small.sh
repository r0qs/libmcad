#!/bin/bash

shopt -s expand_aliases
source ~/.bashrc

#  1 load
#  2 load
#  3 tpconsall,
#  4 latconsall,
#  5 tpconspost,
#  6 latconspost,
#  7 tpconsfollow,
#  8 latconsfollow,
#  9 tpconsunfollow,
# 10 latconsunfollow,
# 11 tpconstl,
# 12 latconstl,
# 13 tpoptall,
# 14 latoptall,
# 15 tpoptpost,
# 16 latoptpost,
# 17 tpoptfollow,
# 18 latoptfollow,
# 19 tpoptunfollow,
# 20 latoptunfollow,
# 21 tpopttl,
# 22 latopttl,
# 23 mistakes,


gnuplot=gnuplot
  
latrange=10
ticinterval=5

directory=$1


tpmistakesfile=${directory}/tp_mistakes.log
latconsfile=${directory}/consLatency.log
latoptfile=${directory}/optLatency.log

outfile=${directory}/optimistic_small.ps

gnuplot << END_GNUPLOT

min(x,y) = (x < y ? x : y)

set terminal postscript size 5,2.8333333 eps enhanced color solid lw 2 "Helvetica" 18
set output "$outfile"
set size 1,1
set origin 0,0

set lmargin 5.5

set multiplot layout 2, 1 title ""
set size 1,0.5
set origin 0,0.55

set tmargin 2
set bmargin 1

#set lmargin 1
#set rmargin 1



#set ylabel "" offset 1.5

#################
#################
# RE-ENABLE!
set yrange [0 : 16]
set ytics 4
set ytics offset 0.8
set xrange [0 : 20]
set xtics 4
#################
#################

#set auto x


#set xlabel "Message size (bytes)"

# the xrange is like this: the center of the first cluster is 0;
#                          the center of the second cluster is 1
#                          and so on: the center of the i-th cluster is i
#set xrange [ -0.6 : 2.6 ]
# set xtics ('0.00' 0.0, '0.25' 0.25, '0.50' 0.5, '0.75' 0.75, '1.00' 1.0)
#set noxtics
set xtics format " "
#set noxlabel
set grid ytics xtics
set ylabel "Latency (ms)" offset 2
set key top left Left reverse
$setkey
#set key bottom right maxrows 6
#set key samplen 3
#set key spacing 1.5
#set key width -3

set object rect from 9.5,0 to 10.5,16 fc rgb "#6" fillstyle solid 0.15 noborder

plot "$latconsfile" using (\$1):(\$2 / 1e6) title "Atomic delivery" with linespoints lt 1 lw 2 pt 7 ps 2 lc rgb "#000000",\
     "$latoptfile"  using (\$1):(\$2 / 1e6) title "Optimistic delivery"   with linespoints lt 1 lw 2 pt 4 ps 2 lc rgb "#000000"


set size 1,0.5
set origin 0,0.0

set tmargin 0
set bmargin 2.5

#set nokey
#set output "$latsizeoutput"
#set noylabel


#################
#################
# RE-ENABLE!
set yrange [0 : 1]
set ytics 0.2
#set xtics
#set xrange [0 : 1]
#set xtics 100 offset 0,0.4
set xtics format "%.0f" offset 0,0.5
set xtics 4,4,20
#################
#################


set grid ytics xtics
set xlabel "Clients" offset 0,1
set ylabel "Throughput (Gbps)" offset 3
#set ytics 0.2
#set ytics auto

# plot "$workloadfiletp75" using ((\$4) / 1e6):xtic(1)    title "Retwis"  fs pattern 1 lc rgb "black",\

#set label "MAX. POWER" font "Helvetica-Bold, 14" center rotate by 90 at 10,0.3
set label "MAXIMUM"  font "Helvetica-Bold, 16" center rotate by 90 at  9.790,0.3
set label "POWER" font "Helvetica-Bold, 16" center rotate by 90 at 10.240,0.3

set label "Percentages represent" at 11.5,0.36
set label "mistaken optimistic deliveries" at 11.5,0.24
set object rect from 11.3,0.15 to 19.1,0.45 fc rgb "white" fillstyle solid 0.15 noborder
#set object 5 rect from 1+1.0*BOXWIDTH,39 to 2.5*BOXWIDTH,31 fc rgb "white" fillstyle   solid 0.15 noborder

prettymistakes(colnum)=(sprintf("%.1f\%",column(colnum)))
mbpsthroughput(colnum)=(8*65536*column(colnum)/1e9)
#mbpsthroughput(colnum)=column(colnum)

plot "$tpmistakesfile" using (\$1):(mbpsthroughput(2))       title "Throughput"          with linespoints lt 1 lw 2 pt 7 ps 2 lc rgb "#000000",\
     ""                using (\$1):(mbpsthroughput(2)):(prettymistakes(3)) notitle with labels offset 0.9,-1

unset multiplot

# (sprintf("%.0f\%",column(13)*100))

set size 1,0.5
set origin 0,0.0
set grid noytics


# plot "$workloadfiletp75" u (column(0)-1+BOXWIDTH*(COL-STARTCOL+GAPSIZE/2+1)-0.5):(\$12) / 1e6):13 notitle w labels
     
END_GNUPLOT

ps2pdf $outfile  ; rm $outfile
