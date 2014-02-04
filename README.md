jslicify
========

Java libraries for slicify. See www.slicify.com for more details.

Copyright (c) Affine Group Ltd. Released under LGPLv3 (Lesser General Public License Version 3).


SlicifyNode.java
================
This is the basic wrapper for the Slicify Web Services API. Example use:

    //create node object and set your slicify username/password
    SlicifyNode node = new SlicifyNode();

    node.setUsername(username);
    node.setPassword(password);

    // Book a node - you can change the values here to specify the particular criteria you want
    int bookingID = node.bookNode(1, 1, 0.01, 64, 10);
		
    // Check result
    if(bookingID == -1)
	    throw new Exception("Error thrown from booking call");
    else if(bookingID == 0)
	    throw new Exception("No machines available");

    //Wait for node to be provisioned and launched
    node.waitReady(bookingID);

    //get the SSH password
    String sshPassword = node.getBookingPassword(bookingID);

    //... run some commands over SSH - see below

    //tidy up when finished
    node.cancelBooking(bookingID);


NodeSSHClient.java
==================
This is a simple SSH client based on the open-source sshj library. Example use:

    //login with your slicify username (same as above) and the per-booking password
    NodeSSHClient sshClient = new NodeSSHClient();
    sshClient.connect(username, sshPassword);

    //Send some commands via the SSH shell
    sshClient.send("pwd");
    sshClient.send("ls -l");

    //close ssh when finished
    sshClient.disconnect();


Dependencies
============
All required libraries are bundled in the /lib subdirectory.

Summary:
* bouncy castle (crypto libraries needed for SSH)
* sshj
* apache commons codec / logging / http / fluent HC
* slf4j
  

