package com.redhat.qe.katello.base.obj;

import java.util.logging.Logger;

import javax.management.Attribute;

import com.redhat.qe.tools.SSHCommandResult;

public class KatelloArchitecture extends _KatelloObject{
    protected static Logger log = Logger.getLogger(KatelloArchitecture.class.getName());

	// ** ** ** ** ** ** ** Public constants
	public static final String CLI_CMD_CREATE = "architecture create";
	public static final String CLI_CMD_INFO = "architecture info";
	public static final String CLI_CMD_LIST = "architecture list";
	public static final String CMD_DELETE = "architecture delete";
	public static final String CMD_UPDATE = "architecture update";
	
	public static final String OUT_CREATE = 
			"Architecture [ %s ] created.";
	public static final String OUT_UPDATE = 
			"Architecture [ %s ] updated.";
	public static final String OUT_DELETE = 
			"Architecture [ %s ] deleted.";
	
	public static final String ERR_NAME_EXISTS = 
			"Name has already been taken";
	public static final String ERR_NOT_FOUND =
			"Architecture with id '%s' not found";
	
	// ** ** ** ** ** ** ** Class members
	public String name;
	
	public KatelloArchitecture(){super();}
	
	public KatelloArchitecture(String pName){
		this.name = pName;
	}
	
	public String getName() {
	    return name;
	}
	
	public void setName(String name) {
	    this.name = name;
	}

	public SSHCommandResult cli_create(){		
		opts.clear();
		opts.add(new Attribute("name", this.name));
		return run(CLI_CMD_CREATE);
	}
	
	public SSHCommandResult cli_info(){
		opts.clear();
		opts.add(new Attribute("name", this.name));
		return run(CLI_CMD_INFO);
	}
	
	public SSHCommandResult cli_list(){
		opts.clear();
		return run(CLI_CMD_LIST+" -v");
	}

	public SSHCommandResult delete(){
		opts.clear();
		opts.add(new Attribute("name", this.name));
		return run(CMD_DELETE);
	}
	
	public SSHCommandResult update(String new_name){
		opts.clear();
		opts.add(new Attribute("name", this.name));
		opts.add(new Attribute("new_name", new_name));
		return run(CMD_UPDATE);
	}


	// ** ** ** ** ** ** **
	// ASSERTS
	// ** ** ** ** ** ** **
}
