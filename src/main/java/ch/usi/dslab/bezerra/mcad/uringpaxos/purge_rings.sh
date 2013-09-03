#!/bin/bash

echo -n "Purging all java processes... "
killall -9 java
for i in `seq $1 $2`
do
   echo -n "in node$i... "
   ssh node$i killall -9 java
done
sleep 1
echo "[done]"
$HOME/zoo/bin/zkServer.sh start
ssh node$3 $HOME/zoo/bin/zkServer.sh start
echo -n "Waiting for zookeeper's ephemeral nodes to vanish..."
sleep 4
echo " [done]"
echo -n "Ready @ "
date
