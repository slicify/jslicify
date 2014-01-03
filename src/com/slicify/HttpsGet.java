package com.slicify;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.schmizz.sshj.common.Base64;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class HttpsGet {

	public Object Result;
	public boolean ParseOnQuery = false;
	public String ServiceURL = null;
	public String Username = null;
	public String Password = null;
	
	public Document XMLDoc = null;

	public HttpsGet(String serviceUrl) {
		ServiceURL = serviceUrl;
	}

	/**
	 * Query the web service at the configured ServiceURL, with the passed operation and URL parameters.
	 * Populates the XMLDoc with the returned XML document
	 *  
	 * @param operation
	 * @param urlParameters
	 * @throws Exception
	 */
	public void query(String operation, String urlParameters) throws Exception
	{	
		if(Username == null || Username.length() <= 0 || Password == null || Password.length() <= 0)
			throw new Exception("Must setUsername() / setPassword() before calling any query");
		
		URL url;
		HttpURLConnection connection = null;
		try
		{
			//open target url
			//String targetURL = ServiceURL + "/" + operation;
			String targetURL = ServiceURL + "/" + operation;
			//System.out.println("Connecting to " + targetURL + " : " + urlParameters);

			if(!ServiceURL.startsWith("https://"))
				throw new IllegalArgumentException("Can only be used with https connections - otherwise password is sent plain text");
			
			url = new URL(targetURL + "?" + urlParameters);
			
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			
			connection.setUseCaches(false);
			connection.setDoInput(true);
			connection.setDoOutput(true);
	
			//add basic authentication header			
			String encoded = Base64.encodeBytes((Username+":"+Password).getBytes()); 
			connection.setRequestProperty("Authorization", "Basic "+encoded);
			
			connection.connect();
	 
			//check HTTP response code
			int response = connection.getResponseCode();
			if(response != 200)
				throw new IOException("HTTP response code error reading from web service: " + response);

			if(ParseOnQuery)
			{
				//parse response using xml DOM parser to get the response booking ID
				DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
				DocumentBuilder db = dbf.newDocumentBuilder(); 
				XMLDoc = db.parse(connection.getInputStream());
			}
		}
		finally
		{
			if (connection != null) {
				connection.disconnect();
			}
		}		
	}
	
	/**
	 * Grab the text content of the indexed element and return. For many queries, the XML is a simple structure
	 * and this is sufficient to get the result.
	 *  
	 * @param index
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
	public String parseReply(int index) throws IndexOutOfBoundsException
	{
		//get the contents of the first returned field
		NodeList replyFields = XMLDoc.getElementsByTagName("*");
		
		if(replyFields.getLength() < index + 1)
			throw new IndexOutOfBoundsException("Index out of bounds in web service response (" + index + "/" + replyFields.getLength() + ")");
		
		return replyFields.item(index).getTextContent();
	}
}
