package com.redhat.qe.katello.tests.backup;

import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.redhat.qe.Assert;
import com.redhat.qe.katello.base.KatelloCliTestScript;
import com.redhat.qe.katello.base.obj.KatelloPing;
import com.redhat.qe.katello.common.KatelloUtils;
import com.redhat.qe.tools.SSHCommandResult;

/**
 * Test for checking Katello backup/restore by Katello Server backup Guide https://fedorahosted.org/katello/wiki/GuideServerBackups
 * @author hhovsepy
 */
@Test(groups={"cfse-e2e"})
public class BackupRecovery extends KatelloCliTestScript {
	protected static Logger log = Logger.getLogger(BackupRecovery.class.getName());
	
	private static final String BDIR = "/backup";
	
	@Test(description="prepare system for backup", groups={TNG_BACKUP}, dependsOnGroups={TNG_PRE_BACKUP}, enabled=true)
	public void prepareBackup() {
		SSHCommandResult res = KatelloUtils.sshOnServer("umask 0027;" +
				"mkdir " + BDIR + ";" +
				"chgrp postgres " + BDIR + ";" +
				"chmod 770 " + BDIR + ";" + 
				"cd " + BDIR + ";");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
	}
	
	@Test(description="backup system files", groups={TNG_BACKUP}, dependsOnGroups={TNG_PRE_BACKUP},
			enabled=true, dependsOnMethods={"prepareBackup"})
	public void backupSystemFiles() {
		SSHCommandResult res = KatelloUtils.sshOnServer("tar --selinux -czvf " + BDIR + "/config_files.tar.gz \\" +
				"/etc/katello \\" +
				"/etc/elasticsearch \\" +
				"/etc/candlepin \\" +
				"/etc/pulp \\" +
				"/etc/grinder \\" +
				"/etc/pki/katello \\" +
				"/etc/pki/pulp \\" +
				"/etc/qpidd.conf \\" +
				"/etc/sysconfig/katello \\" +
				"/etc/sysconfig/elasticsearch \\" +
				"/root/ssl-build \\" +
				"/var/www/html/pub/* \\" +
				"/var/lib/katello \\" +
				"/usr/share/katello/candlepin-cert.crt");
		
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("tar --selinux -czvf " + BDIR + "/elastic_data.tar.gz /var/lib/elasticsearch");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
	}
	
	@Test(description="backup pulp repos", groups={TNG_BACKUP}, dependsOnGroups={TNG_PRE_BACKUP},
			enabled=true, dependsOnMethods={"backupSystemFiles"})
	public void backupRepos() {
		SSHCommandResult res = KatelloUtils.sshOnServer("service pulp-server stop");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("tar --selinux -cvf " + BDIR + "/pulp_data.tar /var/lib/pulp /var/www/pub");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("service pulp-server start");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
	}
	
	@Test(description="backup databases", groups={TNG_BACKUP}, dependsOnGroups={TNG_PRE_BACKUP},
			enabled=true, dependsOnMethods={"backupRepos"})
	public void backupDatabases() {
		KatelloUtils.stopKatello();
		SSHCommandResult res = KatelloUtils.sshOnServer("tar --selinux -czvf " + BDIR + "/mongo_data.tar.gz /var/lib/mongodb");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("tar --selinux -czvf " + BDIR + "/pgsql_data.tar.gz /var/lib/pgsql/data/");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		KatelloUtils.startKatello();
	}
	
	@Test(description="backup PSQL", groups={TNG_BACKUP}, dependsOnGroups={TNG_PRE_BACKUP},
			enabled=true, dependsOnMethods={"backupDatabases"})
	public void backupPSQL() {
		SSHCommandResult res = KatelloUtils.sshOnServer("grep db_name /etc/katello/katello-configure.conf /usr/share/katello/install/default-answer-file");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("su postgres -c \"pg_dump -Fc katello > " + BDIR + "/katello.dump\"");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("su postgres -c \"pg_dump -Fc candlepin > " + BDIR + "/candlepin.dump\"");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
	}

	@Test(description="backup MongoDB", groups={TNG_BACKUP}, dependsOnGroups={TNG_PRE_BACKUP},
			enabled=true, dependsOnMethods={"backupPSQL"})
	public void backupMongoDB() {
		SSHCommandResult res = KatelloUtils.sshOnServer("mongodump --host localhost --out " + BDIR + "/mongo_dump");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
	}

	@Test(description="check backup files", groups={TNG_BACKUP}, dependsOnGroups={TNG_PRE_BACKUP},
			enabled=true, dependsOnMethods={"backupMongoDB"})
	public void checkBackup() {
		SSHCommandResult res = KatelloUtils.sshOnServer("ls " + BDIR);
		Assert.assertTrue(getOutput(res).contains("candlepin.dump"), "Result should contain candlepin.dump");
		Assert.assertTrue(getOutput(res).contains("config_files.tar.gz"), "Result should contain config_files.tar.gz");
		Assert.assertTrue(getOutput(res).contains("elastic_data.tar.gz"), "Result should contain elastic_data.tar.gz");
		Assert.assertTrue(getOutput(res).contains("katello.dump"), "Result should contain katello.dump");
		Assert.assertTrue(getOutput(res).contains("mongo_dump"), "Result should contain mongo_dump");
		Assert.assertTrue(getOutput(res).contains("pulp_data.tar"), "Result should contain pulp_data.tar");
		Assert.assertTrue(getOutput(res).contains("mongo_data.tar.gz"), "Result should contain mongo_data.tar.gz");
		Assert.assertTrue(getOutput(res).contains("pgsql_data.tar.gz"), "Result should contain pgsql_data.tar.gz");
	}
	
	@Test(description="reinstall katello on server", groups={TNG_BACKUP}, dependsOnGroups={TNG_PRE_BACKUP},
			enabled=true, dependsOnMethods={"checkBackup"})
	public void reinstallKatello() {
		SSHCommandResult res = KatelloUtils.sshOnServer("wget https://raw.github.com/Katello/katello/master/scripts/katello_remove.sh");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		KatelloUtils.sshOnServer("chmod a+x katello_remove.sh");
		res = KatelloUtils.sshOnServer("./katello_remove.sh");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		KatelloUtils.sshOnServer("subscription-manager clean; yum clean all");
		
		res = KatelloUtils.sshOnServer("cd /mnt/tests/Katello/Installation/SystemEngineLatest/; export CFSE_RELEASE=1.1 && make run");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("cd /mnt/tests/Katello/Configuration/KatelloClient/; export CFSE_RELEASE=1.1 && make run");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
	}
	
	@Test(description="prepare for restores", groups={TNG_BACKUP}, dependsOnGroups={TNG_PRE_BACKUP},
			enabled=true, dependsOnMethods={"reinstallKatello"})
	public void prepareRestore() {
		SSHCommandResult res = KatelloUtils.sshOnServer("restorecon -Rnv /");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("tar --selinux -xzvf " + BDIR + "/config_files.tar.gz -C /tmp");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("katello-configure --answer-file=/tmp/etc/katello/katello-configure.conf");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("chgrp postgres -R " + BDIR);
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		KatelloUtils.stopKatello();
	}
	
	@Test(description="restore system files", groups={TNG_BACKUP}, dependsOnGroups={TNG_PRE_BACKUP},
			enabled=true, dependsOnMethods={"prepareRestore"})
	public void restoreSystemFiles() {
		SSHCommandResult res = KatelloUtils.sshOnServer("cd " + BDIR + ";" +
				" tar --selinux -xzvf config_files.tar.gz -C /;" +
				" tar --selinux -xzvf elastic_data.tar.gz -C /;" +
				" tar --selinux -xvf pulp_data.tar -C /;");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
	}
	
	@Test(description="restore PSQL", groups={TNG_BACKUP}, dependsOnGroups={TNG_PRE_BACKUP},
			enabled=true, dependsOnMethods={"restoreSystemFiles"})
	public void restorePSQL() {
		SSHCommandResult res = KatelloUtils.sshOnServer("service postgresql start");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		try{Thread.sleep(10000);}catch(Exception ex){} // waiting for psql to start
		res = KatelloUtils.sshOnServer("cd " + BDIR + ";" + "su postgres -c \"dropdb katello\"");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("cd " + BDIR + ";" + "su postgres -c \"dropdb candlepin\"");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("cd " + BDIR + ";" + "su postgres -c \"pg_restore -C -d postgres " + BDIR + "/katello.dump\"");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("cd " + BDIR + ";" + "su postgres -c \"pg_restore -C -d postgres " + BDIR + "/candlepin.dump\"");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
	}
	
	@Test(description="restore MongoDB", groups={TNG_BACKUP}, dependsOnGroups={TNG_PRE_BACKUP},
			enabled=true, dependsOnMethods={"restorePSQL"})
	public void restoreMongoDB() {
		SSHCommandResult res = KatelloUtils.sshOnServer("service mongod start");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		try{Thread.sleep(10000);}catch(Exception ex){} // waiting for mongod to start
		res = KatelloUtils.sshOnServer("cd " + BDIR + ";" + "echo 'db.dropDatabase();' | mongo pulp_database");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
		
		res = KatelloUtils.sshOnServer("cd " + BDIR + ";" + "mongorestore --host localhost mongo_dump/pulp_database/");
		Assert.assertTrue(res.getExitCode().intValue()==0, "Check - return code");
	}
	
	@Test(description="finish restore by restarting services and checking ping", groups={TNG_BACKUP},
			dependsOnGroups={TNG_PRE_BACKUP}, enabled=true, dependsOnMethods={"restoreMongoDB"})
	public void finishRestore() {
		String cmd = 
				"service elasticsearch start; " +
				"service tomcat6 start; " +
				"service pulp-server start; " +
				"service katello start; " +
				"service katello-jobs start;";
		KatelloUtils.sshOnServer(cmd);
		
		try{Thread.sleep(60000);}catch(Exception ex){} // waiting for services to start
		
		KatelloPing ping_obj= new KatelloPing();
		SSHCommandResult res = ping_obj.cli_ping(); 
		Assert.assertTrue(res.getExitCode().intValue() == 0, "Check - return code");
	}
}
