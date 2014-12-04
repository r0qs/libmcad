#!/usr/bin/python

import benchCommon
import os
import time

while True :
    os.system("sudo ntpdate -b node249 &> /dev/null")
    time.sleep(benchCommon.clockSyncInterval)
