#!/bin/bash

echo -n "Purging all java processes..."
killall -9 java
sleep 1
echo " [done]"
$HOME/zoo/bin/zkServer.sh start
echo -n "Waiting for zookeeper's ephemeral nodes to vanish..."
sleep 4
echo " [done]"
echo -n "Ready @ "
date
