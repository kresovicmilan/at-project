package DTO;

import java.io.Serializable;

public class AgentSpecificDTO implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String hostJSON;
	private String contentJSON;
	
	public AgentSpecificDTO() {
		
	}
	
	public AgentSpecificDTO(String hostJSON, String contentJSON) {
		super();
		this.hostJSON = hostJSON;
		this.contentJSON = contentJSON;
	}

	public String getHostJSON() {
		return hostJSON;
	}

	public void setHostJSON(String hostJSON) {
		this.hostJSON = hostJSON;
	}

	public String getContentJSON() {
		return contentJSON;
	}

	public void setContentJSON(String contentJSON) {
		this.contentJSON = contentJSON;
	}
	
	
}
