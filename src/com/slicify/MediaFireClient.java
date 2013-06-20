package com.slicify;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * This class provides a wrapper to access MediaFire for file upload/download.
 * 
 * @author slicify
 *
 */
public class MediaFireClient {
	
	private static final String HTTP_MEDIAFIRE_BASE_API = "http://www.mediafire.com/api/";
	private static final String HTTPS_MEDIAFIRE_BASE_API = "https://www.mediafire.com/api/";
	
	private final String Username;
	private final String Password;
	private final String AppID; 
	private final String AppKey;

	private String SessionToken = null;
	
	/**
	 * Opens a mediafire session. Requires your mediafire username/password, as well as an application id/key, 
	 * which you get when you register your application via mediafire.
	 * 
	 * @param mediaFireUsername
	 * @param mediaFirePassword
	 * @param appId
	 * @param appKey
	 * @throws Exception
	 */
	public MediaFireClient(String mediaFireUsername, String mediaFirePassword,
			String appId, String appKey) throws Exception {
		Username = mediaFireUsername;
		Password = mediaFirePassword;
		AppID = appId;
		AppKey = appKey;
		connect();
	}
	
	/**
	 * Initiate mediafire session. You must call this method before any others, in order to create
	 * the necessary session token.
	 * 
	 * @throws Exception
	 */
	public void connect() throws Exception {

		//mediafire requires an SHA1 of your user/password/appid/appkey for authentication
		String shasig = sha1String(Username + Password + AppID + AppKey);
		String targetURL = HTTPS_MEDIAFIRE_BASE_API + "user/get_session_token.php" + "?email=" + Username + "&password=" + Password + 
				"&application_id=" + AppID + "&signature=" + shasig + "&version=1";

		//send get request, and parse session_token
		Document XMLDoc = httpGet(targetURL);
		NodeList tags = XMLDoc.getElementsByTagName("session_token");
		
		if(tags.getLength() != 1)
			throw new Exception("Missing/multiple session_tokens:" + tags.getLength());

		SessionToken = tags.item(0).getTextContent();
	}
	
	/**
	 * Gets the quickkey (the mediafire hash) for the specified filename in the root directory.
	 * 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	public String getQuickKey(String filename) throws Exception
	{
		Map<String, String> fileKeys = listFiles();
		return fileKeys.get(filename);
	}
	
	/**
	 * Download an entire file as a text string.
	 * 
	 * @param quickKey Requires the mediafire quickkey for the file
	 * @return
	 * @throws Exception
	 */
	public String downloadText(String quickKey) throws Exception 
	{
		String ddLink = getDownloadLink(quickKey);
		
		URL website = new URL(ddLink);
		InputStream webStream = website.openStream();
		return convertStreamToString(webStream);
	}
	
	/**
	 * Download a file and open as an InputStream.
	 * 
	 * @param quickKey Requires the mediafire quickkey for the file
	 * @return
	 * @throws Exception
	 */
	public InputStream downloadBLOB(String quickKey) throws Exception 
	{
		String ddLink = getDownloadLink(quickKey);
		
		URL website = new URL(ddLink);
		InputStream webStream = website.openStream();
		return webStream;
	}

	/**
	 * Gets a URL for the specified file. This is an intermediate step required if you want to download 
	 * a file.
	 * 
	 * @param quickKey Requires the mediafire quickkey for the file
	 * @return
	 * @throws Exception
	 */
	public String getDownloadLink(String quickKey) throws Exception
	{
		String targetURL = HTTP_MEDIAFIRE_BASE_API + "file/get_links.php?session_token=" + SessionToken + 
				"&link_type=direct_download&quick_key=" + quickKey;

		//send get request, and parse file entries
		Document XMLDoc = httpGet(targetURL);
		NodeList tags = XMLDoc.getElementsByTagName("direct_download");
		
		if(tags.getLength() != 1)
			throw new Exception("Missing/multiple direct_download section:" + tags.getLength());

		return tags.item(0).getTextContent();
	}
	
	/**
	 * Delete the specified file.
	 * 
	 * @param quickKey Requires the mediafire quickkey for the file
	 * @throws Exception
	 */
	public void delete(String quickKey) throws Exception
	{
		String targetURL = HTTP_MEDIAFIRE_BASE_API + "file/delete.php?session_token=" + SessionToken + "&quick_key=" + quickKey;

		//send get request, and parse file entries
		Document XMLDoc = httpGet(targetURL);
		NodeList tags = XMLDoc.getElementsByTagName("result");
		
		if(tags.getLength() != 1)
			throw new Exception("Missing/multiple direct_download section:" + tags.getLength());

		String result = tags.item(0).getTextContent();
		
		if(!result.equalsIgnoreCase("success"))
			throw new Exception("File data failed:" + result);
	}
	
	/**
	 * List all the files in the root directory. Returns a Map<FileName, QuickKey> that is useful
	 * for finding the quickkey for a specific filename.
	 * 
	 * @return
	 * @throws Exception
	 */
	public Map<String, String> listFiles() throws Exception
	{
		String targetURL = HTTP_MEDIAFIRE_BASE_API + "folder/get_content.php?session_token=" + SessionToken + "&content_type=files";

		//send get request, and parse file entries
		Document XMLDoc = httpGet(targetURL);
		NodeList fileNames = XMLDoc.getElementsByTagName("filename");
		NodeList quickKeys = XMLDoc.getElementsByTagName("quickkey");
		
		if(fileNames.getLength() != quickKeys.getLength())
			throw new Exception("Mismatch between filenames/keys:" + fileNames.getLength() + " / " + quickKeys.getLength());
		
		Map<String, String> keyNameMap = new HashMap<String, String>();
		
		for(int i=0;i<fileNames.getLength();i++)
		{
			keyNameMap.put(fileNames.item(i).getTextContent(), quickKeys.item(i).getTextContent());			
		}
		
		return keyNameMap;
	}


	/**
	 * Upload a byte[] to mediafire, creating a new file with the specified filename. 
	 * 
	 * @param fileName The file name to create on mediafire.
	 * @param data The contents of the file
	 * @throws IOException
	 */
	public void upload(String fileName, byte[] data) throws IOException
	{
		String targetURL = HTTP_MEDIAFIRE_BASE_API + "upload/upload.php?session_token=" + SessionToken;

	    HttpClient httpclient = new DefaultHttpClient();

	    HttpPost httppost = new HttpPost(targetURL);

	    MultipartEntity mpEntity = new MultipartEntity();
	    ContentBody cbFile = new ByteArrayBody(data, fileName);
	    mpEntity.addPart("myFile", cbFile);


	    httppost.setEntity(mpEntity);
	    System.out.println("executing request " + httppost.getRequestLine());
	    HttpResponse response = httpclient.execute(httppost);
	    HttpEntity resEntity = response.getEntity();

	    System.out.println(response.getStatusLine());
	    if (resEntity != null) {
	      System.out.println(EntityUtils.toString(resEntity));
	    }
	    if (resEntity != null) {
	    	EntityUtils.consume(resEntity);
	    }

	    httpclient.getConnectionManager().shutdown();

	}
		
	/**
	 * HTTP GET implementation using HttpURLConnection
	 * 
	 * @param targetURL
	 * @return
	 * @throws Exception
	 */
	private Document httpGet(String targetURL) throws Exception
	{		
		URL url = new URL(targetURL);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setUseCaches(false);
		connection.setDoInput(true);
		connection.setDoOutput(true);

		// Send request
		connection.getOutputStream().flush();
		connection.getOutputStream().close();
		
		//check HTTP response code
		int response = connection.getResponseCode();
		if(response != 200)
			throw new Exception("Unable to login to MediaFire:" + response + " for " + targetURL);

		//parse response using xml DOM parser
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder(); 
		Document XMLDoc = db.parse(connection.getInputStream());
		
		connection.disconnect();
		
		return XMLDoc;
	}
	
	/**
	 * Calculate the SHA1 hash of the specified string
	 * 
	 * @param myString
	 * @return
	 * @throws Exception 
	 */
	private String sha1String(String myString) throws Exception
	{
	    String sha1 = "";
	    try
	    {
	        MessageDigest crypt = MessageDigest.getInstance("SHA-1");
	        crypt.reset();
	        crypt.update(myString.getBytes("UTF-8"));
	        sha1 = byteToHex(crypt.digest());
	    }
	    catch(Exception e)
	    {
	    	throw new Exception("Exception during creation of SHA1 hash for mediafire login", e.getCause());
	    }
	    return sha1;
	}

	/**
	 * Convert a byte[] into a hex string.
	 * 
	 * @param hash
	 * @return
	 */
	private String byteToHex(final byte[] hash)
	{
	    Formatter formatter = new Formatter();
	    for (byte b : hash)
	    {
	        formatter.format("%02x", b);
	    }
	    String result = formatter.toString();
	    formatter.close();
	    return result;
	}

	/**
	 * Fully read the passed InputStream and return the resutling String.
	 * 
	 * @param is
	 * @return
	 */
	private String convertStreamToString(InputStream is) {
		Scanner s1 = new Scanner(is);
	    Scanner s = s1.useDelimiter("\\A");
	    String result = s.hasNext() ? s.next() : "";
	    s.close();
	    s1.close();
	    
	    return result;
	}

}
