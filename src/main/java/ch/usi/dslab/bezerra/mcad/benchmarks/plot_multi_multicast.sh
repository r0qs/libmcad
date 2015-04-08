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
maxlat64k[2]="60"
maxlat8k[2]="15"
maxlat200[2]="20"
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
set ylabel "Throughput (Gbps)" offset 0.5
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
set ylabel "Throughput (Gbps)" offset 2.5
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
set ylabel "Throughput (Mbps)" offset 2.5
set ytics 0,80
set xtics offset 0,0.5
set grid ytics
set style data histogram
set style histogram cluster gap 3

set style fill solid border rgb "black"
set auto x

set yrange[0:320]

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
set ytics 0,12
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
set ytics 0,3
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
set ytics 0,4
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




# SCALABILITY MULTIPLOT
##########################################################
##########################################################
##########################################################

tpnormalspread64k=465.847672373
tpnormalspread8k=392.026248446
tpnormalspread200=39.702140016
tpnormalmrp64k=813.586920243
tpnormalmrp8k=393.640110719
tpnormalmrp200=51.40373708
tpnormalridge64k=804.669443015
tpnormalridge8k=361.43646389
tpnormalridge200=55.5787152944

i=0
name=${datatypes[i]}
output_scale=${directory}/multicast_scalability_${name}_multi.ps

tpcol=${tpcols[i]}
let latavgcol=tpcol+1
let lat95col=tpcol+3

gnuplot << END_GNUPLOT
set terminal postscript size 5,4 eps enhanced color solid lw 2 "Helvetica" 18
set output "$output_scale"

set size 1,1
set origin 0,0



set multiplot layout 3,1 title ""

##########################################################
# scalability plot 65536 bytes
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
set ylabel "Throughput (norm.)" offset 2
set ytics (2, 4, 6, 8)
set grid ytics
set style data histogram
set style histogram cluster gap 3

#set obj 3 rect from -0.5,4.1 to 1.875,7.9 fs solid fc rgb "white" border 1 lw 0.5
#set object 3  rectangle from -0.5,4.1 to 1.875,7.9 fs solid 1.0 border lw 1
#set obj 3 rect from -0.5,4.1 to 1.875,7.9 fs solid fc rgb "white" border 1 front

set obj 3 rect back from -0.52,4.3 to 1.885,7.7 fs solid fc rgb "white" lw 0
set label 1 "Absolute values for 1 group (Mbps):" at -0.5,6.8
set label 2 "Spread: $(echo "$tpnormalspread64k/1" | bc), MRP: $(echo "$tpnormalmrp64k/1" | bc), Ridge: $(echo "$tpnormalridge64k/1" | bc)" at -0.5,5.2
#set label 3 "Multi-Ring Paxos: $(echo "$tpnormalmrp64k/1" | bc)" at -0.5,4
#set label 4 "Ridge: $(echo "$tpnormalridge64k/1" | bc)" at -0.5,2.5

set style fill solid border rgb "black"
set auto x

set yrange[0:10]

f(x) = 1

plot "$input64k" using (\$$[tpcol+alglinelength*0])/$tpnormalspread64k:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''          using (\$$[tpcol+alglinelength*1])/$tpnormalmrp64k:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#999999",\
     ''          using (\$$[tpcol+alglinelength*2])/$tpnormalridge64k:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#666666",\
     f(x) notitle lc rgb "black" lw 1.5

##########################################################

##########################################################
# scalability plot 8192 bytes
set size 1,0.35
set origin 0,0.325

set title "8 kiloBytes messages"

unset key

unset xlabel
unset xtics
#set ylabel "Throughput (norm.)" offset 3
set ytics 1,1,4
set grid ytics
set style data histogram
set style histogram cluster gap 3

set obj 3 rect back from -0.52,3.15 to 1.885,4.85 fs solid fc rgb "white" lw 0
set label 1 "Absolute values for 1 group (Mbps):" at -0.5,4.4
set label 2 "Spread: $(echo "$tpnormalspread8k/1" | bc), MRP: $(echo "$tpnormalmrp8k/1" | bc), Ridge: $(echo "$tpnormalridge8k/1" | bc)" at -0.5,3.6

set style fill solid border rgb "black"
set auto x

set yrange[0:5]

plot "$input8k" using (\$$[tpcol+alglinelength*0])/$tpnormalspread8k:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''         using (\$$[tpcol+alglinelength*1])/$tpnormalmrp8k:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#999999",\
     ''         using (\$$[tpcol+alglinelength*2])/$tpnormalridge8k:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#666666",\
     f(x) notitle lc rgb "black" lw 1.5
##########################################################

##########################################################
# scalability plot 200 bytes
set size 1,0.35
set origin 0,0.0125

set title "200 Bytes messages"

set xlabel "Multicast groups" offset 0,1.0
#set ylabel "Throughput (norm.)" offset 3
set ytics 1,1,4
set xtics offset 0,0.5
set grid ytics
set style data histogram
set style histogram cluster gap 3

#set obj 3 rect back from -0.52,3.15 to 1.885,4.85 fs solid fc rgb "white" lw 0
set label 1 "Absolute values for 1 group (Mbps):" at -0.5,4.4
set label 2 "Spread: $(echo "$tpnormalspread200/1" | bc), MRP: $(echo "$tpnormalmrp200/1" | bc), Ridge: $(echo "$tpnormalridge200/1" | bc)" at -0.5,3.6

set style fill solid border rgb "black"
set auto x

set yrange[0:5]

plot "$input200" using (\$$[tpcol+alglinelength*0])/$tpnormalspread200:xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
     ''          using (\$$[tpcol+alglinelength*1])/$tpnormalmrp200:xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#999999",\
     ''          using (\$$[tpcol+alglinelength*2])/$tpnormalridge200:xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#666666",\
     f(x) notitle lc rgb "black" lw 1.5
##########################################################
END_GNUPLOT
pstopdf $output_scale
rm $output_scale
##########################################################
##########################################################
##########################################################







# SCALE_EFFICIENCY MULTIPLOT
##########################################################
##########################################################
##########################################################

tpnormalspread64k=465.847672373
tpnormalspread8k=392.026248446
tpnormalspread200=39.702140016
tpnormalmrp64k=813.586920243
tpnormalmrp8k=393.640110719
tpnormalmrp200=51.40373708
tpnormalridge64k=804.669443015
tpnormalridge8k=361.43646389
tpnormalridge200=55.5787152944

i=0
name=${datatypes[i]}
output_scale=${directory}/multicast_efficiency_${name}_multi.ps

tpcol=${tpcols[i]}
let latavgcol=tpcol+1
let lat95col=tpcol+3

gnuplot << END_GNUPLOT
set terminal postscript size 5,4 eps enhanced color solid lw 2 "Helvetica" 18
set output "$output_scale"

set size 1,1
set origin 0,0



set multiplot layout 3,1 title ""

##########################################################
# scale_efficiency plot 65536 bytes
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
set ylabel "Scalability efficiency" offset 2
set ytics ("0" 0, "" 0.25, "0.5" 0.5, "" 0.75, 1)
set grid ytics
set style data histogram
set style histogram cluster gap 3

#set obj 3 rect from -0.5,4.1 to 1.875,7.9 fs solid fc rgb "white" border 1 lw 0.5
#set object 3  rectangle from -0.5,4.1 to 1.875,7.9 fs solid 1.0 border lw 1
#set obj 3 rect from -0.5,4.1 to 1.875,7.9 fs solid fc rgb "white" border 1 front

#set obj 3 rect back from -0.52,4.3 to 1.885,7.7 fs solid fc rgb "white" lw 0
#set label 1 "Absolute values for 1 group (Mbps):" at -0.5,6.8
#set label 2 "Spread: $(echo "$tpnormalspread64k/1" | bc), MRP: $(echo "$tpnormalmrp64k/1" | bc), Ridge: $(echo "$tpnormalridge64k/1" | bc)" at -0.5,5.2
#set label 3 "Multi-Ring Paxos: $(echo "$tpnormalmrp64k/1" | bc)" at -0.5,4
#set label 4 "Ridge: $(echo "$tpnormalridge64k/1" | bc)" at -0.5,2.5
set label 5 font "Helvetica-Oblique, 14" "ideal\nscalability" at 3.4,0.94

set style fill solid border rgb "black"
set auto x

set yrange[0:1.25]

f(x) = 1

plot "$input64k" using (\$$[tpcol+alglinelength*0])/($tpnormalspread64k*(\$1)):xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
''          using (\$$[tpcol+alglinelength*1])/($tpnormalmrp64k*(\$1)):xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#999999",\
''          using (\$$[tpcol+alglinelength*2])/($tpnormalridge64k*(\$1)):xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#666666",\
f(x) notitle lc rgb "black" lw 1.5

##########################################################

##########################################################
# scale_efficiency plot 8192 bytes
set size 1,0.35
set origin 0,0.325

set title "8 kiloBytes messages"

unset key

unset xlabel
unset xtics
#set ylabel "Throughput (norm.)" offset 3
#set ytics 1,1,4
#set grid ytics
set style data histogram
set style histogram cluster gap 3

#set obj 3 rect back from -0.52,3.15 to 1.885,4.85 fs solid fc rgb "white" lw 0
#set label 1 "Absolute values for 1 group (Mbps):" at -0.5,4.4
#set label 2 "Spread: $(echo "$tpnormalspread8k/1" | bc), MRP: $(echo "$tpnormalmrp8k/1" | bc), Ridge: $(echo "$tpnormalridge8k/1" | bc)" at -0.5,3.6

set style fill solid border rgb "black"
set auto x

#set yrange[0:5]

plot "$input8k" using (\$$[tpcol+alglinelength*0])/($tpnormalspread8k*(\$1)):xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
''         using (\$$[tpcol+alglinelength*1])/($tpnormalmrp8k*(\$1)):xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#999999",\
''         using (\$$[tpcol+alglinelength*2])/($tpnormalridge8k*(\$1)):xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#666666",\
f(x) notitle lc rgb "black" lw 1.5
##########################################################

##########################################################
# scale_efficiency plot 200 bytes
set size 1,0.35
set origin 0,0.0125

set title "200 Bytes messages"

set xlabel "Multicast groups" offset 0,1.0
#set ylabel "Throughput (norm.)" offset 3
#set ytics 1,1,4
set xtics offset 0,0.5
#set grid ytics
set style data histogram
set style histogram cluster gap 3

#set obj 3 rect back from -0.52,3.15 to 1.885,4.85 fs solid fc rgb "white" lw 0
#set label 1 "Absolute values for 1 group (Mbps):" at -0.5,4.4
#set label 2 "Spread: $(echo "$tpnormalspread200/1" | bc), MRP: $(echo "$tpnormalmrp200/1" | bc), Ridge: $(echo "$tpnormalridge200/1" | bc)" at -0.5,3.6

set style fill solid border rgb "black"
set auto x

#set yrange[0:5]

plot "$input200" using (\$$[tpcol+alglinelength*0])/($tpnormalspread200*(\$1)):xtic(1) title column($[algnamecol+alglinelength*0]) fs solid lc rgb "#FFFFFF",\
''          using (\$$[tpcol+alglinelength*1])/($tpnormalmrp200*(\$1)):xtic(1) title column($[algnamecol+alglinelength*1]) fs solid lc rgb "#999999",\
''          using (\$$[tpcol+alglinelength*2])/($tpnormalridge200*(\$1)):xtic(1) title column($[algnamecol+alglinelength*2]) fs solid lc rgb "#666666",\
f(x) notitle lc rgb "black" lw 1.5
##########################################################
END_GNUPLOT
pstopdf $output_scale
rm $output_scale
##########################################################
##########################################################
##########################################################

