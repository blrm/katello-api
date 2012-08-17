package com.redhat.qe.katello.base.obj;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.redhat.qe.Assert;
import com.redhat.qe.katello.base.KatelloApi;
import com.redhat.qe.katello.base.KatelloTestScript;
import com.redhat.qe.katello.common.KatelloConstants;
import com.redhat.qe.katello.common.KatelloUtils;
import com.redhat.qe.tools.SSHCommandResult;

public class KatelloMisc {

	public static final String API_CMD_GET_POOLS = "/pools"; 
	
	
	public SSHCommandResult api_getPools(){
		return new KatelloApi().get(API_CMD_GET_POOLS);
	}
	
	public JSONObject api_getPoolByProduct(String productName){
		JSONObject pool =null;
		try{
			JSONArray jpools = KatelloTestScript.toJSONArr(api_getPools().getStdout()); 
			if(jpools ==null) return null;
			for(int i=0;i<jpools.size();i++){
				pool = (JSONObject)jpools.get(i);
				if(((String)pool.get("productName")).equals(productName))
					return pool;
			}
		}catch (Exception e) {}
		return null;
	}
	
	public String cli_getPoolBySubscription(String subscriptionName, int quantity) {
		
		SSHCommandResult exec_result = KatelloUtils.sshOnClient("subscription-manager list --available --all | sed  -e 's/^ \\{1,\\}//'");
		
		String match_info = null;
		
		if (KatelloConstants.KATELLO_PRODUCT == "katello") {
			match_info = String.format(KatelloSystem.REG_SUBSCRIPTION, subscriptionName, String.valueOf(quantity)).replaceAll("\"", "");
		} else {
			match_info = String.format(KatelloSystem.REG_SUBSCRIPTION_CFSE, subscriptionName, String.valueOf(quantity)).replaceAll("\"", "");
		}
		
		Pattern pattern = Pattern.compile(match_info);
		Matcher matcher = pattern.matcher(exec_result.getStdout().replaceAll("\n", " "));
		Assert.assertTrue(matcher.find(), "Check - Subscription should exist in subscription manager list");
		String subscription = matcher.group();
		
		match_info = KatelloSystem.REG_POOL_ID;
		pattern = Pattern.compile(match_info);
		matcher = pattern.matcher(subscription);
		Assert.assertTrue(matcher.find(), "Check - Pool Id should exist in subscription manager list");
		String id = matcher.group().trim();
		
		return id;
	}
}
