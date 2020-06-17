package DTO;

import java.io.Serializable;

public class ReceivingMessageDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String message;
	
	public ReceivingMessageDTO() {
		
	}
	
	public ReceivingMessageDTO(String message) {
		super();
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	
}
