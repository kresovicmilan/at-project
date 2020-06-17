package DTO;

import java.io.Serializable;

public class ReceivingMessageDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String type;
	private String name;
	private String hostIp;
	
	public ReceivingMessageDTO() {
		
	}
	
	

	public ReceivingMessageDTO(String type, String name, String hostIp) {
		super();
		this.type = type;
		this.name = name;
		this.hostIp = hostIp;
	}



	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getHostIp() {
		return hostIp;
	}

	public void setHostIp(String hostIp) {
		this.hostIp = hostIp;
	}
	
	
	
	
}
