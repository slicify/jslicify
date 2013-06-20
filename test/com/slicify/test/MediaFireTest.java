package com.slicify.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import com.slicify.MediaFireClient;

/**
 * This is a quick test of the MediaFire integration library. It creates a file, uploads it, downloads it again
 * and checks that the file contents match.
 * 
 * @author slicify
 *
 */
public class MediaFireTest {
	
	//NOTE - you need to replace these with your specific mediafire login details
	private static String username = "mediafire-user";
	private static String password = "mediafire-password";
	private static String appid = "mediafire-appid";
	private static String appkey = "mediafire-appkey";
	
	//other test objects
	private static MediaFireClient mediaClient;
	private static String testData;

	private static String tempFilename = "helloworld.txt";

	@BeforeClass
	public static void setup() throws Exception
	{
		assertFalse("Remember to set your mediafire login details before running the test", username.equals("mediafire-user"));		
		testData = "Hello World\n A timestamp:" + System.currentTimeMillis() + "\n";
		mediaClient = new MediaFireClient(username, password, appid, appkey);
	}
	
	@Test
	public void MFUpload() throws Exception {
		//get filename/key map
		
		Map<String, String> fileKeys = mediaClient.listFiles();
		System.out.println(fileKeys.toString());
		
		if(fileKeys.containsKey(tempFilename))
		{
			//delete the existing temp file
			String quickKey = fileKeys.get(tempFilename);
			mediaClient.delete(quickKey);
		}
		
		//upload some test data
		mediaClient.upload(tempFilename, testData.getBytes());
	}
	
	@Test
	public void MFDownload() throws Exception {
		//refresh filename/key map - usually the file doesnt appear immediately, and you have to query a few times
		String quickKey = null;
		while(quickKey == null)
		{
			quickKey = mediaClient.getQuickKey(tempFilename);
			if(quickKey == null)
				Thread.sleep(1000); //throttle queries
		}
		
		System.out.println("QuickKey:" + quickKey);

		//download data again
		String contents = mediaClient.downloadText(quickKey);
		
		//verify contents
		assertEquals("test data downloaded didnt match original", testData, contents);
	}
	

}
