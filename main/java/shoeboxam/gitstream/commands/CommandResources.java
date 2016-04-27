package shoeboxam.gitstream.commands;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import shoeboxam.gitstream.settings.GitstreamData;
import shoeboxam.gitstream.utils.FileManager;
import shoeboxam.gitstream.utils.GitManager;

public class CommandResources implements ICommand {

	FileManager file_manager = FileManager.getInstance();
	GitManager git = GitManager.getInstance();

	@Override
	public int compareTo(ICommand arg0) {
		return 0;
	}

	@Override
	public String getCommandName() {
		return "resources";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "commands.resources.usage";
	}

	@Override
	public List<String> getCommandAliases() {
		return Arrays.asList();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		List<String> arguments = Arrays.asList(args);
		boolean status = true;
		
		GitstreamData data;
		try {
			String json_string = IOUtils.toString(new FileInputStream(new File(homeDirectory().toString()+ "\\settings.json")));
			data = new Gson().fromJson(json_string, GitstreamData.class);
			FileUtils.writeStringToFile((data.settings_location), new Gson().toJson(data));
		} catch (IOException e) {
			data = new GitstreamData();
			e.printStackTrace();
		}
		if (arguments.get(0).equals("get")){
			// Get files from remote
			if (data.repository_directory.exists()){
				sender.addChatMessage(new TextComponentTranslation("Downloading changes from Github..."));
				status = git.pull();
			} else {
				sender.addChatMessage(new TextComponentTranslation("Downloading all resources from Github..."));
				status = git.clone(data.config.remote_url);
			}
			if (!status) {
				sender.addChatMessage(new TextComponentTranslation("Error downloading from Github. Additional information in console."));
			}
			
			// Transfer resources
			if (new File(data.resourcepack_directory.toString() + "\\pack.mcmeta").exists()){
				sender.addChatMessage(new TextComponentTranslation("Transferring changes to development pack..."));
				sender.addChatMessage(new TextComponentTranslation("** Your edits are being preserved **"));
				status = file_manager.update_resources();
			} else {
				sender.addChatMessage(new TextComponentTranslation("Creating resource pack..."));
				status = file_manager.create_resource_pack();
			}
			if (!status){
				sender.addChatMessage(new TextComponentTranslation("Error transferring resources to development pack. Additional information in console."));
			}
			
			//Transfer placeholders
			status = file_manager.update_defaults();
			if (!status){
				sender.addChatMessage(new TextComponentTranslation("Error transferring placeholders to development pack. Additional information in console."));
			}
		}
		
		if (arguments.get(0).equals("send")){
			// Get files from remote
			if (data.repository_directory.exists()){
				sender.addChatMessage(new TextComponentTranslation("Downloading changes from Github..."));
				status = git.pull();
			} else {
				sender.addChatMessage(new TextComponentTranslation("Downloading all resources from Github..."));
				status = git.clone(data.config.remote_url);
			}
			if (!status) {
				sender.addChatMessage(new TextComponentTranslation("Error downloading from Github. Additional information in console."));
			}
			
			List<String> edited_mods = file_manager.apply_edits();
			
			// Commit 
			for (String patchname : edited_mods){
				if (status) {
					status = git.commit(patchname + "/*", "GS: " + patchname);
					sender.addChatMessage(new TextComponentTranslation("> Created commit: " + patchname));
				} else if (!status){
					sender.addChatMessage(new TextComponentTranslation("Error creating commit. Additional information in console."));
				}
			}
			
			// Check for credentials
			if (data.git_username == "" || data.git_email == "" || data.git_password == ""){
				sender.addChatMessage(new TextComponentTranslation("Please set login credentials: "));
				sender.addChatMessage(new TextComponentTranslation("  /git config username [username]"));
				sender.addChatMessage(new TextComponentTranslation("  /git config email [email]"));
				sender.addChatMessage(new TextComponentTranslation("  /git config password [password]"));
				sender.addChatMessage(new TextComponentTranslation("Optionally, save credentials with: "));
				sender.addChatMessage(new TextComponentTranslation("  /git config save "));
			}
			
			status = true;
			status = git.push();
			
			// Transfer resources
			sender.addChatMessage(new TextComponentTranslation("Transferring remote changes to development pack..."));
			status = file_manager.update_resources();
			if (!status){
				sender.addChatMessage(new TextComponentTranslation("Error transferring resources to development pack. Additional information in console."));
			}
		}
		
		if (arguments.get(0).equals("save")){
			try {
				FileUtils.writeStringToFile((data.settings_location), new Gson().toJson(data));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		//Check to see if running with filesystem write privileges.
		Preferences prefs = Preferences.systemRoot();
	    try{
	        prefs.put("foo", "bar"); // SecurityException on Windows
	        prefs.remove("foo");
	        prefs.flush(); // BackingStoreException on Linux
	        return true;
	    }catch(Exception e){
	    	sender.addChatMessage(new TextComponentTranslation("Error using command: Launch minecraft as admin or with sudo to grant filesystem access."));
	        return false;
	    }
	}

	@Override
	public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args,
			BlockPos pos) {
		return Arrays.asList("res");
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return false;
	}
	

	
	public File homeDirectory()
	{
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
	    return new File(local_path);
	}

}
