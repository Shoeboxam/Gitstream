package shoeboxam.gitstream.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import com.google.gson.Gson;

import shoeboxam.gitstream.settings.GitstreamData;


public class GitManager {
	
	private GitManager() {
	}
	
	private static class GitHelperHolder { 
	    private static final GitManager INSTANCE = new GitManager();
	}

	public static GitManager getInstance() {
	    return GitHelperHolder.INSTANCE;
	}

	private String username = "";
	private String password = "";
	private String email = "";
	CredentialsProvider creds = new UsernamePasswordCredentialsProvider(username, password);
	
	GitstreamData data = get_data();
	
	private GitstreamData get_data(){
		
		String local_path = "";
	    String OS = System.getProperty("os.name").toUpperCase();
	    if (OS.contains("WIN"))
	        local_path = System.getenv("APPDATA");
	    else if (OS.contains("MAC"))
	    	local_path = System.getProperty("user.home") + "/Library/Application"
	                + "Support";
	    else if (OS.contains("NUX"))
	    	local_path = System.getProperty("user.home");
	    else{
	    	local_path = System.getProperty("user.dir");
	    }
	    local_path = local_path.concat("\\.graphics_stream");
	    
		try {
			String json_string = IOUtils.toString(new FileInputStream(local_path.toString() + "\\" + "settings.json"));
			data = new Gson().fromJson(json_string, GitstreamData.class);
		} catch (IOException e) {
			data = new GitstreamData();
		}
		return data;
	}
	
	public void set_username(String username_in){
		username = username_in;
		creds = new UsernamePasswordCredentialsProvider(username, password);
	}
	
	public void set_email(String email_in){
		username = email_in;
	}
	
	public void set_password(String password_in){
		username = password_in;
		creds = new UsernamePasswordCredentialsProvider(username, password);
	}
	
	public File get_repo_directory(){
		return data.repository_directory;
	}
	
	public boolean push(){
		try {
			Git repository = Git.open(data.repository_directory);
			
			creds = new UsernamePasswordCredentialsProvider(data.git_username, data.git_password);
			repository.push().setCredentialsProvider(creds).call();
			return true;
		} catch (IOException|GitAPIException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean commit(String filepattern, String message){
		try {
			Git repository = Git.open(data.repository_directory);
			repository.add()
				.addFilepattern(filepattern)
				.call();
			repository.commit()
				.setAuthor(new PersonIdent(username, email))
				.setMessage(message)
				.call();
			repository.close();
			return true;
		} catch (IOException|GitAPIException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean clone(String URL){
		
		if (data.repository_directory.exists()){
			try {
				Git repository = Git.open(data.repository_directory);
				repository.pull().setCredentialsProvider(creds);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		try (Git repository = Git.cloneRepository()
				.setCredentialsProvider(creds)
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
}
