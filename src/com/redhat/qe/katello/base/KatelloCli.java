package com.redhat.qe.katello.base;

import java.util.ArrayList;
import java.util.List;

import javax.management.Attribute;

import com.redhat.qe.katello.common.KatelloUtils;
import com.redhat.qe.katello.tasks.KatelloTasks;
import com.redhat.qe.tools.SSHCommandResult;

public class KatelloCli{
	static{new com.redhat.qe.auto.testng.TestScript();}// to make properties be initialized (if they don't still)
	
	public static final String OUT_EMPTY_LIST = "[  ]";
	
	private String command;
	private List<Attribute> args;
	private List<Attribute> opts;
	
	public KatelloCli(String command,List<Attribute> args,List<Attribute> options){
		this.command = command;
		this.args = args;
		this.opts = options;
		if(this.args==null) this.args = new ArrayList<Attribute>();
		if(this.opts==null) this.opts = new ArrayList<Attribute>();
	}
	
	public KatelloCli(String command,List<Attribute> options){
		this.command = command;
		this.args = new ArrayList<Attribute>();
		this.args.add(new Attribute("username", System.getProperty("katello.admin.user", "admin")));
		this.args.add(new Attribute("password", System.getProperty("katello.admin.password", "admin")));
		this.opts = options;
		if(this.opts==null) this.opts = new ArrayList<Attribute>();
	}
		
	public SSHCommandResult run(){
		String cmd = System.getProperty("katello.engine", "katello");
		for(int i=0;i<this.args.size();i++){
			cmd = cmd + " --" + args.get(i).getName()+" \""+args.get(i).getValue().toString()+"\"";
		}
		cmd = cmd + " " + this.command;
		for(int i=0;i<this.opts.size();i++){
			if(this.opts.get(i).getValue()!=null)
				cmd = cmd + " --" + opts.get(i).getName()+" \""+opts.get(i).getValue().toString()+"\"";
		}
		
		try {
			return KatelloUtils.sshOnClient(cmd);
		}
		catch (Exception e) {
			e.printStackTrace();
		}return null;
	}
	
	/**
	 * Returns katello cli output block (usually: [command] list -v options) that has: <BR>
	 * [Property]:  [Value] in its block.<br>
	 * As an example would be getting a pool information for:<BR> 
	 * ("ProductName","High-Availability (8 sockets)",org.subscriptions())
	 * @param property
	 * @param value
	 * @param output
	 * @return
	 */
	public static String grepOutBlock(String property, String value, String output){
		String _return = null;
		String[] lines = output.split("\\n\\n");
		
		for(String line:lines ){
			if(line.startsWith("---") || line.trim().equals("")) continue; // skip it.
			if(KatelloTasks.grepCLIOutput(property, line).equals(value)){
				_return = line.trim();
				break;
			}
		}
		return _return;
	}
	
}
