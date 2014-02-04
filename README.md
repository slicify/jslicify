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

	// Create a new bid to book a machine - you can change the values here to specify the particular criteria you want
	String country = "";	//can specific a particular location if required
	int minRam = 1;         //include machines with any amount of RAM
	double maxPrice = 0.03; //only pay up to $0.03 per hour. If a machine is too expensive, the booking will be cancelled.
	int bits = 64;          //64-bit machines only
	int ecu = 5;		    //minimum benchmark
	
	bidID = node.addBid(minRam, maxPrice, bits, ecu, country);
	
	// Check result
	if(bidID < 0)
		fail("Error thrown from booking call");
		
	// Get any active booking
	int bookingID = node.getBookingID(bidID);
	
    // Check result
    if(bookingID == -2)
	    throw new Exception("Error thrown from booking call");
    else if(bookingID == -1)
	    throw new Exception("No machines available");

    //Wait for node to be provisioned and launched
    node.waitReady(bookingID);

    //get the SSH password
    String sshPassword = node.getBookingPassword(bookingID);

    //... run some commands over SSH - see below

    //tidy up when finished (delete bid and associated booking)
	node.deleteBid(bidID);


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
  

