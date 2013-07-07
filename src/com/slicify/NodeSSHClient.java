package com.slicify;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Shell;

/**
 * Simple SSH client that allows you to open a shell and send basic commands.
 * @author slicify
 *
 */
public class NodeSSHClient {

	//ssh client library
	private final SSHClient ssh = new SSHClient();
	private Session session = null; 
	private Shell shell = null;
	private PrintStream Print = System.out;
	
	public static final String SERVER = "www.slicify.com";
	public static final String PROMPT = "slicify@slicify:~";
	
	public void setOutputStream(OutputStream out)
	{
		Print = new PrintStream(out);
	}
	
	/**
	 * Connect to the Slicify SSH terminal service, and login with the specified username, and booking password. 
	 * @param username
	 * @param bookingPassword
	 * @return
	 * @throws IOException
	 */
	public String connect(String username, String bookingPassword) throws IOException 
	{
		//validate host key
		ssh.addHostKeyVerifier("e9:5d:51:34:ec:8d:96:6d:1f:70:94:a3:ad:ef:0e:09");

		//connect
		ssh.connect(SERVER);
		ssh.authPassword(username, bookingPassword);

		//start a session
	    session = ssh.startSession();
	    
	    //request a shell (note ssh exec is not supported by Slicify at this time)
	    shell = session.startShell();
	    
	    //wait for prompt
	    return expectPrompt();
	}

	/**
	 * Wait for the default prompt to be sent back from the server.
	 * @return
	 * @throws IOException
	 */
	public String expectPrompt() throws IOException
	{
	    return expectLiteral(PROMPT);
	}
	
	/**
	 * Wait for the specified literal text to be sent back from the server (regex not currently supported)
	 * @param text
	 * @return
	 * @throws IOException
	 */
	public String expectLiteral(String text) throws IOException
	{
		//buffer the output until we find the passed literal string
    	StringBuffer buffer = new StringBuffer();
    	boolean waiting = true;
    	while(waiting)
    	{
    		//read next char from input stream (blocking call)
    		char nextChar = (char) shell.getInputStream().read();
    		buffer.append(nextChar);
    		Print.print(Character.toString(nextChar));
    		
    		//wait for prompt
    		if(buffer.lastIndexOf(text) > 1)
    		{
    			waiting = false;
    		}
    	}
    	
    	//return buffer
    	return buffer.toString();
	}
	
	public String sendRaw(String characters) throws IOException
	{
		return send(characters, null, false);
	}
	
	
	/**
	 * Send a string to the shell. This will also wait for the default shell prompt to be echoed back before returning.
	 * @param shellCommand
	 * @return
	 * @throws IOException
	 */
	public String send(String shellCommand) throws IOException
	{
		return send(shellCommand, PROMPT);
    }
	
	/**
	 * Send a string to the shell. This will also wait until the specified literal expression is seen in the reply from the
	 * terminal before returning.
	 * @param shellCommand
	 * @param expectLiteral
	 * @return
	 * @throws IOException
	 */
	public String send(String shellCommand, String expectLiteral) throws IOException
	{
		//default to always send \n at end of line
    	return send(shellCommand, expectLiteral, false);
    }

	/**
	 * Send a string to the shell. This will also wait until the specified literal expression is seen in the reply from the
	 * terminal before returning. Setting noCRLF=true will also prevent the method from automatically inserting a carriage
	 * return after the command. 
	 * @param shellCommand
	 * @param expectLiteral
	 * @param noCRLF
	 * @return
	 * @throws IOException
	 */
	public String send(String shellCommand, String expectLiteral, boolean noCRLF) throws IOException
	{
		String result = null;
		
		//write data out to stream
    	DataOutputStream out = new DataOutputStream(shell.getOutputStream());
    	out.write(shellCommand.getBytes());
    	if(!noCRLF)
    		out.writeChars("\n");
    	out.flush();
    	
    	//wait on the expected response
    	if(expectLiteral != null && expectLiteral.length() > 0)
    		result = expectLiteral(expectLiteral);
    	
    	return result;
    }

	/**
	 * Disconnect the SSH session.
	 * @throws IOException
	 */
	public void disconnect() throws IOException
	{
        session.close();
	    ssh.disconnect();
	}
}
