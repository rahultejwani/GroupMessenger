package edu.buffalo.cse.cse486586.groupmessenger;

import java.io.Serializable;

/**
 * Bean class to store a message
 * @author rahul
 *
 */
public class MessageBean implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String text;
	private String sequence;
	private String port;
	
	MessageBean(String text, String sequence,String port)
	{
		this.text= text;
		this.sequence = sequence;
		this.port = port;
	}
	String getText() {
		return text;
	}
	void setText(String text) {
		this.text = text;
	}
	String getSequence() {
		return sequence;
	}
	void setSequence(String sequence) {
		this.sequence = sequence;
	}
	String getPort() {
		return port;
	}
	void setPort(String port) {
		this.port = port;
	}
	

}
