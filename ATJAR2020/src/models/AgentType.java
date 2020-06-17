package models;

import java.io.Serializable;

public class AgentType implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String name;
	private String module;
	
	public AgentType() {
		
	}
	
	public AgentType(String name, String module) {
		super();
		this.name = name;
		this.module = module;
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getModule() {
		return module;
	}
	public void setModule(String module) {
		this.module = module;
	}
	
	
}
