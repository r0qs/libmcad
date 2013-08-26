#!/bin/bash

killall -9 java
sleep 4
$HOME/zoo/bin/zkServer.sh start
sleep 1
echo -n "Ready @ "
date
