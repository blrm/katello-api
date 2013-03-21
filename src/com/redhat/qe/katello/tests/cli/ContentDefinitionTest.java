package com.redhat.qe.katello.tests.cli;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.katello.base.KatelloCli;
import com.redhat.qe.katello.base.KatelloCliDataProvider;
import com.redhat.qe.katello.base.KatelloCliTestScript;
import com.redhat.qe.katello.base.obj.KatelloContentView;
import com.redhat.qe.katello.base.obj.KatelloOrg;
import com.redhat.qe.katello.base.obj.KatelloProduct;
import com.redhat.qe.katello.base.obj.KatelloProvider;
import com.redhat.qe.katello.base.obj.KatelloRepo;
import com.redhat.qe.katello.common.KatelloUtils;
import com.redhat.qe.tools.SSHCommandResult;

	
@Test(groups={"cfse-cli"})
public class ContentDefinitionTest extends KatelloCliTestScript{

	private SSHCommandResult exec_result;
	private String org_name;
	private String provider_name;
	private String product_name;
	private String repo_name;
	private String provider_name2;
	private String product_name2;
	private String repo_name2;
	private String content_name;
	private String content_name_prod;
	private String content_name_repo;
	
	
	@BeforeClass(description="init: create initial stuff")
	public void setUp() {
		String uid = KatelloUtils.getUniqueID();
		org_name = "org"+uid;
		provider_name = "provider"+uid;
		product_name = "product"+uid;
		repo_name = "repo"+uid;
		provider_name2 = "provider2"+uid;
		product_name2 = "product2"+uid;
		repo_name2 = "repo2"+uid;

		// Create org:
		KatelloOrg org = new KatelloOrg(this.org_name,"Package tests");
		exec_result = org.cli_create();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		
		// Create provider:
		KatelloProvider prov = new KatelloProvider(provider_name, org_name, "Package provider", PULP_RHEL6_x86_64_REPO);
		exec_result = prov.create();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		
		// Create product:
		KatelloProduct prod = new KatelloProduct(product_name, org_name, provider_name, null, null, null, null, null);
		exec_result = prod.create();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		
		KatelloRepo repo = new KatelloRepo(repo_name, org_name, product_name, PULP_RHEL6_x86_64_REPO, null, null);
		exec_result = repo.create();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		
		// Create second provider:
		prov = new KatelloProvider(provider_name2, org_name, "Package provider", REPO_INECAS_ZOO3);
		exec_result = prov.create();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		
		// Create product:
		prod = new KatelloProduct(product_name2, org_name, provider_name2, null, null, null, null, null);
		exec_result = prod.create();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		
		repo = new KatelloRepo(repo_name2, org_name, product_name2, REPO_INECAS_ZOO3, null, null);
		exec_result = repo.create();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
	}

	
	@Test(description = "Create new content definition")
	public void test_Create() {
		
		KatelloContentView content = createContentDefinition();
		assert_ContentViewDefinitionInfo(content);
		assert_contentList(Arrays.asList(content), new ArrayList<KatelloContentView>());
	}
	
	@Test(description = "Create Content Def with empty name, verify error")
	public void test_createContentDefEmptyName() {
		KatelloContentView content = new KatelloContentView("", null, org_name, null);
		exec_result = content.create_definition();
		Assert.assertEquals(exec_result.getExitCode().intValue(), 166, "Check - return code");
		Assert.assertTrue(getOutput(exec_result).contains(KatelloContentView.ERR_NAME_EMPTY), "Check - error string (content create)");
	}
	
	@Test(description = "Create Content Def with long name, verify error")
	public void test_createContentDefLongName() {
		String name = KatelloCliDataProvider.strRepeat("Lorem ipsum dolor sit amet", 14);
		
		KatelloContentView content = new KatelloContentView(name, null, org_name, null);
		exec_result = content.create_definition();
		Assert.assertEquals(exec_result.getExitCode().intValue(), 166, "Check - return code");
		Assert.assertTrue(getOutput(exec_result).equals(KatelloContentView.ERR_NAME_LONG), "Check - error string (content create)");
	}
	
	@Test(description = "Create 2 new content definitions, delete one of them")
	public void test_delete() {
		
		KatelloContentView content = createContentDefinition();
		KatelloContentView content2 = createContentDefinition();
		
		String id2 = assert_ContentViewDefinitionInfo(content2);
		content2.setId(Long.valueOf(id2));
		
		exec_result = content2.definition_delete();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		
		assert_contentList(Arrays.asList(content), Arrays.asList(content2));
	}
	
	@Test(description = "Create new content definition, add product into it")
	public void test_addProduct() {
		KatelloContentView content = createContentDefinition();
		content_name_prod = content.name;
		exec_result = content.add_product(product_name);
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		Assert.assertTrue(getOutput(exec_result).equals(String.format(KatelloContentView.OUT_ADD_PRODUCT, product_name, content.getName())), "Check - output string (add product)");

		exec_result = content.add_product(product_name2);
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		Assert.assertTrue(getOutput(exec_result).equals(String.format(KatelloContentView.OUT_ADD_PRODUCT, product_name2, content.getName())), "Check - output string (add product)");

		content.products = product_name+"\\s+"+product_name2;
		assert_ContentViewDefinitionInfo(content);
	}

	@Test(description = "remove product from definition", dependsOnMethods={"test_addProduct"})
	public void test_removeProduct() {
		KatelloContentView content = new KatelloContentView(content_name_prod, "descritpion", org_name, content_name_prod);
		
		exec_result = content.remove_product(product_name2);
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		Assert.assertTrue(getOutput(exec_result).equals(String.format(KatelloContentView.OUT_REMOVE_PRODUCT, product_name2, content.getName())), "Check - output string (remove product)");

		content.products = product_name;
		assert_ContentViewDefinitionInfo(content);
	}

	@Test(description = "Create new content definition, add repo into it")
	public void test_addRepo() {
		KatelloContentView content = createContentDefinition();
		content_name_repo = content.name;
		exec_result = content.add_repo(product_name, repo_name);
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		Assert.assertTrue(getOutput(exec_result).equals(String.format(KatelloContentView.OUT_ADD_REPO, repo_name, content.getName())), "Check - output string (add repo)");

		exec_result = content.add_repo(product_name2, repo_name2);
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		Assert.assertTrue(getOutput(exec_result).equals(String.format(KatelloContentView.OUT_ADD_REPO, repo_name2, content.getName())), "Check - output string (add repo)");

		content.repos = repo_name+"\\s+"+repo_name2;
		assert_ContentViewDefinitionInfo(content);
	}

	@Test(description = "remove repo from definition", dependsOnMethods={"test_addRepo"})
	public void test_removeRepo() {
		KatelloContentView content = new KatelloContentView(content_name_repo, "descritpion", org_name, content_name_repo);
		
		exec_result = content.remove_repo(product_name2, repo_name2);
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		Assert.assertTrue(getOutput(exec_result).equals(String.format(KatelloContentView.OUT_REMOVE_REPO, repo_name2, content.getName())), "Check - output string (remove repo)");

		content.repos = repo_name;
		assert_ContentViewDefinitionInfo(content);
	}
	
	private void assert_contentList(List<KatelloContentView> contents, List<KatelloContentView> excludeContents) {

		SSHCommandResult res = new KatelloContentView(null, null, org_name, null).definition_list();

		//contents that exist in list
		for(KatelloContentView cont : contents){
			if (cont.description == null) cont.description = "None";
			
			String match_info = String.format(KatelloContentView.REG_DEF_LIST, cont.name, cont.label, cont.description, cont.org).replaceAll("\"", "");
			Assert.assertTrue(getOutput(res).replaceAll("\n", " ").matches(match_info), String.format("Content Definition [%s] should be found in the result list", cont.name));
		}
		
		//contents that should not exist in list
		for(KatelloContentView cont : excludeContents){
			if (cont.description == null) cont.description = "None";
			
			String match_info = String.format(KatelloContentView.REG_DEF_LIST, cont.name, cont.label, cont.description, cont.org).replaceAll("\"", "");
			Assert.assertFalse(getOutput(res).replaceAll("\n", " ").matches(match_info), String.format("Content definition [%s] should not be found in the result list", cont.name));
		}
	}
	
	private String assert_ContentViewDefinitionInfo(KatelloContentView content) {
		SSHCommandResult res;
		if (content.description == null) content.description = "None";
		res = content.definition_info();
		String match_info = String.format(KatelloContentView.REG_DEF_INFO, content.name, content.label, content.description, content.org,
				content.publishedViews, content.componentViews, content.products, content.repos).replaceAll("\"", "");
		Assert.assertTrue(res.getExitCode() == 0, "Check - return code");
		log.finest(String.format("Org (info) match regex: [%s]",match_info));
		Assert.assertTrue(getOutput(res).replaceAll("\n", "").matches(match_info), 
				String.format("Org [%s] should be found in the result info", org_name));	
		
		return KatelloCli.grepCLIOutput("ID", getOutput(res));
	}
	
	private KatelloContentView createContentDefinition() {
		content_name = "content"+KatelloUtils.getUniqueID();
		KatelloContentView content = new KatelloContentView(content_name, "descritpion", org_name, content_name);
		exec_result = content.create_definition();
		Assert.assertTrue(exec_result.getExitCode() == 0, "Check - return code");
		
		return content;
	}

}