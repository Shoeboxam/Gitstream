package shoeboxam.gitstream.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.google.gson.Gson;

import shoeboxam.gitstream.settings.GitConfig;
import shoeboxam.gitstream.settings.InstanceConfig;


public class GitManager {
	GitConfig config = new GitConfig();
	File git_config;
	InstanceConfig data = new InstanceConfig();
	
	private GitManager() {
		InstanceConfig instance_config = new InstanceConfig();
		git_config = new File(instance_config.workspace_directory.toString() + "\\" + "git_config.json");
		
		try {
			String json_string = IOUtils.toString(new FileInputStream(git_config));
			config = new Gson().fromJson(json_string, GitConfig.class);
		} catch (IOException e) {
		}
	}
	
	private static class GitManagerHolder { 
	    private static final GitManager INSTANCE = new GitManager();
	}

	public static GitManager getInstance() {
	    return GitManagerHolder.INSTANCE;
	}
	
	public void set_username(String username_in){
		config.username = username_in;
	}
	
	public void set_email(String email_in){
		config.email = email_in;
	}
	
	public void set_password(String password_in){
		config.password = password_in;
	}
	
	public File get_repo_directory(){
		return data.repository_directory;
	}
	
	public boolean push(){
		try {
			Git repository = Git.open(data.repository_directory);
			
			repository.push().setCredentialsProvider(
					new UsernamePasswordCredentialsProvider(config.username, config.password)).call();
			return true;
		} catch (IOException|GitAPIException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean commit(String filepattern, String message){
		if (has_credentials()){
			try {
				Git repository = Git.open(data.repository_directory);
				repository.commit()
					.setOnly(filepattern)
					.setMessage(message)
					.setAuthor(new PersonIdent(config.username, config.email))
					.call();
				repository.close();
				return true;
			} catch (IOException|GitAPIException e) {
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public boolean clone(String URL){
		
		if (data.repository_directory.exists()){
			try {
				Git repository = Git.open(data.repository_directory);
				repository.pull().setCredentialsProvider(
						new UsernamePasswordCredentialsProvider(config.username, config.password));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try (Git repository = Git.cloneRepository()
				.setCredentialsProvider(
						new UsernamePasswordCredentialsProvider(config.username, config.password))
                .setURI(URL)
                .setDirectory(data.repository_directory)
                .call()) {
	        System.out.println("Cloning repository: " + repository.getRepository().getDirectory());
	        repository.close();
	        return true;
	        
        } catch (GitAPIException e) {
			e.printStackTrace();
        }
		return false;
	}
	
	public boolean pull(){
		try {
			Git repository = Git.open(data.repository_directory);
			repository.pull().call().getMergeResult().getMergedCommits().toString();
			return true;
		} catch (IOException|GitAPIException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public String status(){
		try {
			Git repository = Git.open(data.repository_directory);
			String status =  repository.status()
					.call()
					.getUncommittedChanges()
					.toString().replaceAll(",", "\n");
			repository.close();
			
			return status;
		} catch (IOException|GitAPIException e) {
			e.printStackTrace();
			return "Failed to get status.";
		}
	}
	
	public boolean save_credentials(){
		try {
			FileUtils.writeStringToFile((git_config), new Gson().toJson(config));
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean has_credentials(){
		return !(config.username == null || config.email == null || config.password == null);
	}
}
