package DTO;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import models.ForeignMessage;

public class MessageDTO implements Serializable, Comparable<MessageDTO> {
	private static final long serialVersionUID = 1L;
	
	private String senderUsername;
	
	private String recieverUsername;
	
	private String messageContent;
	
	private String messageTitle;
	
	private String dateSent;
	
	public MessageDTO() {
		
	}
	
	public MessageDTO(String sender, String reciever, String content, String messageTitle, String dateSent) {
		this.senderUsername = sender;
		this.recieverUsername = reciever;
		this.messageContent = content;
		this.messageTitle = messageTitle;
		this.dateSent = dateSent;
	}
	
	public MessageDTO(ForeignMessage foreignMessage) {
		this.senderUsername = foreignMessage.getSenderUsername();
		this.recieverUsername = foreignMessage.getRecieverUsername();
		this.messageContent = foreignMessage.getMessageContent();
		this.messageTitle = foreignMessage.getMessageTitle();
		this.dateSent = foreignMessage.getDateSent();
	}
	
	public String getSenderUsername() {
		return senderUsername;
	}
	public void setSenderUsername(String senderUsername) {
		this.senderUsername = senderUsername;
	}
	public String getRecieverUsername() {
		return recieverUsername;
	}
	public void setRecieverUsername(String recieverUsername) {
		this.recieverUsername = recieverUsername;
	}
	public String getMessageContent() {
		return messageContent;
	}
	public void setMessageContent(String messageContent) {
		this.messageContent = messageContent;
	}

	public String getMessageTitle() {
		return messageTitle;
	}

	public void setMessageTitle(String messageTitle) {
		this.messageTitle = messageTitle;
	}

	public String getDateSent() {
		return dateSent;
	}

	public void setDateSent(String dateSent) {
		this.dateSent = dateSent;
	}

	@Override
	public int compareTo(MessageDTO m) {
		DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		String first = getDateSent();
		String second = m.getDateSent();
		Date dateFirst = new Date();
		Date dateSecond = new Date();
		try {
			dateFirst = dateFormat.parse(first);
			dateSecond = dateFormat.parse(second);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return dateFirst.compareTo(dateSecond);
	}
}
