package com.slicify.test;
import java.io.IOException;
import static org.junit.Assert.*;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.slicify.NodeSSHClient;
import com.slicify.SlicifyNode;

/**
 * Runs a simple node booking cycle: 
 * 1. Book a node
 * 2. Wait until its ready.
 * 3. Retrieve the booking password (this is used to SSH to the machine)
 * 4. SSH to www.slicify.com and login with the booking password to access the machine
 * 5. Open the SSH shell and send some commands to the remote node
 * 6. Cancel the booking
 * 
 * @author slicify
 *
 */
public class SlicifyNodeSSHTest {
	
	//NOTE - you need to replace these with your specific slicify login details
	private static String username = "slicify-user";
	private static String password = "slicify-password";
	
	private static SlicifyNode node;
	private static int bidID = -1;
	private static int bookingID = -1;
	private String bookingOTP;

	@BeforeClass
	public static void setup() {

		assertFalse("Remember to set your slicify user/password before running the test", username.equals("slicify-user"));
		
		//get the web services wrapper & set username/password
		node = new SlicifyNode();
		node.setUsername(username);
		node.setPassword(password);
	}
	
	@Test
	public void runAll() throws Exception {
	
		addBid();
		getBooking();
		getStatus();
		getPassword();
		//sshTest();
	}
	
	private void addBid() throws Exception {

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

		System.out.println("BidID:" + bidID);
	}

	private void getBooking() throws Exception {
		
		//check if our bid was successful
		bookingID = node.getBookingID(bidID);

		//check result
		if(bookingID == -2)
			fail("getBookingID returned an error");
		else if(bookingID == -1)
			fail("No machines available - try increasing the price, or decreasing the requirements");
		
		System.out.println("BookingID:" + bookingID);
	}
	
	private void getStatus() throws Exception {
		if(bookingID > 0)
		{
			//Wait until Node is ready
			String status = "Unknown";
			while(!status.equals("Ready"))
			{
				//check it hasnt faulted
				assertFalse("Machine wasnt able to be booked succesfully", status.equals("Closed"));

				//check the status every 10 seconds
				Thread.sleep(10000);					
				status = node.getBookingStatus(bookingID);
				System.out.println("Status:" + status);					
			}
		}
	}
	
	private void getPassword() throws Exception {
	
		bookingOTP = node.getBookingPassword(bookingID);
		
		assertNotNull("Null booking password", bookingOTP);
		assertFalse("Empty booking password", bookingOTP.length() < 1);

		System.out.println("Booking OTP:" + bookingOTP);					
	}
	
	private void sshTest() throws IOException {
		
		//Send some commands via the SSH command session
		NodeSSHClient sshClient = new NodeSSHClient();
		sshClient.connect(username, bookingOTP);
		
		String pwdResult = sshClient.send("pwd", true);
		assertTrue(pwdResult.contains("/home/slicify"));
		
		String llResult = sshClient.send("ls -l", true);
		assertTrue(llResult.contains("total 0"));
		
		String wgetResult = sshClient.send("tsocks wget www.google.com", true);
		assertTrue(wgetResult.contains("`index.html' saved"));
		
		String catResult = sshClient.send("cat index.html", true);
		assertTrue(catResult.contains("</html>"));
		
		sshClient.disconnect();				
	}
	
	@AfterClass
	public static void cancelBooking() {
		if(node != null && bidID > 0) 
		{
			// Cancel booking again
			try {
				node.deleteBid(bidID);
				System.out.println("Bid cancelled, booking terminating automatically");
				
				if(bookingID > 0)
					assertEquals("Booking not closed", "Closed", node.getBookingStatus(bookingID));
				
			} catch (Exception e) {
				fail("Booking may not be cancelled - check from www.slicify.com web site, and cancel manually if needed");
			}
		}		
	}

}
