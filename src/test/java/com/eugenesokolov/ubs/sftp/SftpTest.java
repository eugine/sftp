package com.eugenesokolov.ubs.sftp;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import org.apache.sshd.SshServer;
import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.UserAuth;
import org.apache.sshd.server.auth.UserAuthNone;
import org.apache.sshd.server.command.ScpCommandFactory;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.sftp.SftpSubsystem;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;


public class SftpTest {
	
	private static SshServer sshd;
	private static final int PORT = 2002;

	@BeforeClass
	public static void setupSftpServer(){
	    sshd = SshServer.setUpDefaultServer();
	    sshd.setPort(PORT);
	    sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider("ubs_rsa"));

	    List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<NamedFactory<UserAuth>>();
	    userAuthFactories.add(new UserAuthNone.Factory());
	    sshd.setUserAuthFactories(userAuthFactories);
	    sshd.setPublickeyAuthenticator(new PublickeyAuthenticator() {

			public boolean authenticate(String arg0, PublicKey arg1, ServerSession arg2) {
				return true;
			}
	    	
	    });

	    sshd.setCommandFactory(new ScpCommandFactory());

	    List<NamedFactory<Command>> namedFactoryList = new ArrayList<NamedFactory<Command>>();
	    namedFactoryList.add(new SftpSubsystem.Factory());
	    sshd.setSubsystemFactories(namedFactoryList);

	    try {
	        sshd.start();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	
	@AfterClass
	public static void stopSftpServer() {
		try {
			sshd.stop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void test() {
		JSch jsch = new JSch();
		Session session = null;
		try {
	        session = jsch.getSession("Username", "localhost", PORT);
	        session.setConfig("StrictHostKeyChecking", "no");
	        session.setPassword("Password");
	        session.connect();

	        Channel channel = session.openChannel("sftp");
	        channel.connect();
	        ChannelSftp sftpChannel = (ChannelSftp) channel;

	        System.err.println("Pwd : "+ sftpChannel.pwd());
	        mkDirsAndCd(sftpChannel, "target/fxclear");
	        
	        InputStream is = new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8));
	        sftpChannel.put(is, "file.txt");
	        
	        sftpChannel.get("file.txt", "localfile.txt");
	        sftpChannel.exit();
	        session.disconnect();
	    } catch (JSchException e) {
	        e.printStackTrace(); 
	    } catch (SftpException e) {
	        e.printStackTrace();
	    }
	}
	
	private void mkDirsAndCd(ChannelSftp sftp, String folder) throws SftpException {
		String[] complPath = folder.split("/");
//		sftp.cd("/");
        for (String dir : complPath) {
            if (dir.length() > 0) {
                try {
                	sftp.cd(dir);
                } catch (SftpException e2) {
                	sftp.mkdir(dir);
                	sftp.cd(dir);
                }
            }
        }
//        sftp.cd("/");
	}
	
	
}
