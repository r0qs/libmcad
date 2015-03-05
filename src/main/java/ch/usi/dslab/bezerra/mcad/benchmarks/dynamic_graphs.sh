#!/bin/bash

input=dynamicThroughputData.log
output=dynamicThroughput.ps

gnuplot << END_GNUPLOT
set terminal postscript eps enhanced color solid lw 2 "Helvetica" 18

#set bmargin 4.3
#set tmargin 2
#set rmargin 2

#set ylabel offset 1.8,0
#set xtics offset 0,0.2
#set ytics offset 0.45,0

set title "Throughput throughout execution" #offset 0,-0.5

#set key samplen 2 inside invert noopaque
#set key samplen 2 inside top left maxrows 2 invert noautotitle width -3
#set key autotitle columnheader

#set style data histogram
#set style histogram rowstacked

#set style fill solid border -1
#set boxwidth 0.75

set xlabel "Time (s)"
set ylabel "Throughput (mps)"

unset xtics

#set yrange [0:$latrange]


set output "$output"



# t_client_send
# t_batch_ready
# t_batch_serialized
# t_learner_delivered
# t_learner_deserialized
# t_command_enqueued
# t_ssmr_dequeued
# t_execution_start
# t_server_send
# t_client_receive

# format of datafile
# "100b, 1P" <tl>
# "100b, 2P"
# "100b, 4P"
# "100b, 8P"
# #
# "1kb, 1P"
# "1kb, 2P"
# "1kb, 4P"
# "1kb, 8P"
# #
# "10kb, 1P"
# "10kb, 2P"
# "10kb, 4P"
# "10kb, 8P"

plot "$input" using 1:3 with lines

END_GNUPLOT

pstopdf $output
#rm $output
