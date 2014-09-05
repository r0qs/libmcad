To use libmcad, you should:

1) deploy the multicast infrastructure
2) start your servers
3) launch your clients

In this example, you must first adjust the config path to that in your local machine, in all the .py files.
Such path must point to the example Ridge config file contained here. Then, execute (in different windows):

$ ./deployMcast.py
$ ./deployReceiver.py  9 1
$ ./deployReceiver.py 10 2
$ ./deploySender.py