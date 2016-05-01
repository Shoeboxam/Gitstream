package shoeboxam.gitstream.commands;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import shoeboxam.gitstream.GitManager;
import shoeboxam.gitstream.files.EditManager;
import shoeboxam.gitstream.files.FileManager;
import shoeboxam.gitstream.files.ResourcePackManager;
import shoeboxam.gitstream.settings.ConfigInstance;

public class CommandUpdateThread extends Thread {
	
	ResourcePackManager resource_manager = new ResourcePackManager();
	EditManager edit_manager = new EditManager();
	GitManager git = GitManager.getInstance();
	ConfigInstance config = new ConfigInstance();
	
	List<String> arguments;
	
	public CommandUpdateThread(List<String> argumentsx){
		arguments=argumentsx;
	}
	
	public void run(){
		
		if (arguments.isEmpty()){
			
			if (!remote_get(config)){
				if (!git.has_credentials()){
					help();
				} else {
					System.out.println("Unable to retrieve data from Github. Additional information in console.");
			
				}
			}
			
			if (!git_to_resource(config)){
				System.out.println("Unable to transfer resources to resource pack. Additional information in console.");
			}
			if (!resource_manager.update_defaults()){
				System.out.println("Unable to transfer placeholders to resource pack. Additional information in console.");
			}
			if (!resource_manager.update_metadata()){
				System.out.println("Unable to update metadata. Additional information in console.");
			}
			
			System.out.println("Update completed.");
			return;
		}
		
		if (arguments.get(0).equals("git")){
			if (remote_get(config)){
				System.out.println("Repository is ready.");
			} else {
				if (!git.has_credentials()){
					help();
				} else {
					System.out.println("Unable to retrieve data from Github. Additional information in console.");
			
				}
			}
			System.out.println("Git update completed.");
			return;
		}
		
		if (arguments.get(0).equals("resources")){
			if (git_to_resource(config)){
				System.out.println("Resources are updated.");
			} else {
				System.out.println("Unable to transfer resources to resource pack. Additional information in console.");
			}
			return;
		}
		
		if (arguments.get(0).equals("placeholders")){
			if (resource_manager.update_defaults()){
				System.out.println("Placeholders are updated.");
			} else {
				System.out.println("Unable to transfer placeholders to resource pack. Additional information in console.");
			}
			return;
		}
		
		if (arguments.get(0).equals("metadata")){
			if (resource_manager.update_metadata()){
				System.out.println("Metadata is updated.");
			} else {
				System.out.println("Unable to update metadata. Additional information in console.");
			}
			return;
		}
		
		if (arguments.get(0).equals("sync")){
			// Hack to catch metadata edits in commits
			System.out.println("Transferring edits to clone...");
			List<String> edited_mods = edit_manager.apply_edits();
			resource_manager.update_metadata();
			edited_mods.addAll(edit_manager.apply_edits());
			System.out.println("Edits transferred to clone.");
		}
		
		if (arguments.get(0).equals("clean")){
			System.out.println("Cleaning repository.");
			for (File dir : config.repository_directory.listFiles()){
				if (!dir.getName().equals(".git") && dir.isDirectory()){
					FileManager.del_empty(dir);
				}
			}
			System.out.println("Cleaning completed.");
		}
		
		if (arguments.get(0).equals("push") || arguments.get(0).equals("send")){
			if (arguments.size() < 3){
				System.out.println("Please add a commit message.");
				return;
			}
			
			if (remote_get(config)){
				System.out.println("Remote changes fetched.");
			} else {
				System.out.println("Unable to connect with Github. Additional information in console.");
				return;
			}
			
			// Hack to catch metadata edits in commits
			List<String> edited_mods = edit_manager.apply_edits();
			resource_manager.update_metadata();
			edited_mods.addAll(edit_manager.apply_edits());
			System.out.println("Edits transferred to clone.");

			String message = StringUtils.join(arguments.subList(2, arguments.size()), " ");
			git.commit(".", message);
			
			if (git.has_credentials()){
				git.push();
			} else {
				help();
			}
			
			// Transfer resources
			if (resource_manager.update_resources()){
				System.out.println("Resources updated.");
			} else {
				System.out.println("Unable to refresh resource pack.");
			}
			
		}
	}
	private boolean remote_get(ConfigInstance data){
		
		// Get files from remote
		if (data.repository_directory.exists()){
			System.out.println("Downloading changes from Github...");
			return git.pull();
		} else {
			System.out.println("Downloading all resources from Github...");
			return git.clone(data.remote_url);
		}
	}
	
	private boolean git_to_resource(ConfigInstance data){
		
		if (new File(data.resourcepack_directory.toString() + "\\pack.mcmeta").exists()){
			System.out.println("Transferring changes to development pack...");
			return resource_manager.update_resources();
		} else {
			System.out.println("Creating resource pack...");
			return resource_manager.create_resource_pack();
		}
	}
	
	private void help(){
		System.out.println("Please set login credentials: ");
		System.out.println("  /git config username [username]");
		System.out.println("  /git config email [email]");
		System.out.println("  /git config password [password]");
		System.out.println("Optionally, save credentials with: ");
		System.out.println("  /git config save ");
	}
}
