package shoeboxam.gitstream.commands;
import shoeboxam.gitstream.settings.GitstreamData;
import shoeboxam.gitstream.utils.FileManager;
import shoeboxam.gitstream.utils.GitManager;

import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;;

public class CommandGit implements ICommand {
	FileManager file_manager = FileManager.getInstance();
	GitManager git = GitManager.getInstance();
	
	@Override
	public int compareTo(ICommand arg0) {
		return 0;
	}

	@Override
	public String getCommandName() { 
		return "git";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "commands.git.usage";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		List<String> arguments = Arrays.asList(args);
		
		GitstreamData data;
		try {
			String json_string = IOUtils.toString(new FileInputStream(new File(homeDirectory().toString()+ "\\" + "settings.json")));
			data = new Gson().fromJson(json_string, GitstreamData.class);
		} catch (IOException e) {
			data = new GitstreamData();
		}
		
		if (arguments.get(0).equals("config")){
			
			if (arguments.get(1).equals("username")){
				String username = StringUtils.join(arguments.subList(2, arguments.size()), " ");
				git.set_username(username);
				sender.addChatMessage(new TextComponentTranslation("Username updated to " + username));
				data.git_username = username;
			}
			else if (arguments.get(1).equals("email")){
				String email = arguments.get(2);
				git.set_email(email);
				sender.addChatMessage(new TextComponentTranslation("Email address updated to " + email));
				data.git_email = email;
			}
			else if (arguments.get(1).equals("password")){
				String password = arguments.get(2);
				git.set_password(password);
				sender.addChatMessage(new TextComponentTranslation("Password updated"));
				if (data.save_password){
					data.git_password = password;
				}
			}
		}
		
		if (arguments.get(0).equals("push")){
			git.push();
		}
		
		if (arguments.get(0).equals("commit")){
			String message = String.join("", arguments.subList(2, arguments.size()).toString());
			if (git.commit(arguments.get(1), message)){
				sender.addChatMessage(new TextComponentTranslation("Committed"));
			} else {
				sender.addChatMessage(new TextComponentTranslation("Commit failed. Additional information in console."));
			}
		}
		
		if (arguments.get(0).equals("clone")){
			// pass in url as argument
			if (git.clone(arguments.get(1))){
				sender.addChatMessage(
		        		new TextComponentTranslation("Cloned repository to: " + git.get_repo_directory().toString()));
			} else {
				sender.addChatMessage(new TextComponentTranslation("Clone failed. Additional information in console."));
			}
		}
		
		if (arguments.get(0).equals("pull")){
			if (git.pull()){
				sender.addChatMessage(new TextComponentTranslation("Pulled changes."));
			} else {
				sender.addChatMessage(new TextComponentTranslation("Pull failed. Additional information in console."));
			}
		}
		
		if (arguments.get(0).equals("status")){
			sender.addChatMessage(
					new TextComponentTranslation(git.status()));
		}
		
		if (arguments.get(0).equals("save") && arguments.get(1).equals("password")){
			data.save_password = true;
			sender.addChatMessage(
					new TextComponentTranslation("The next time the password is entered, it will be saved."));
		}

		try {
			FileUtils.writeStringToFile((data.settings_location), new Gson().toJson(data));
		} catch (IOException e) {
			e.printStackTrace();
			sender.addChatMessage(
					new TextComponentTranslation("Error saving preference. Additional information in console."));
		}
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) { 
		return true;
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) { 
		return false;
	}

	@Override
	public List<String> getCommandAliases() { 
		return Arrays.asList();
	}

	@Override
	public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args,
			BlockPos pos) { 
		return Arrays.asList("g");
	}
	
	private File homeDirectory()
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
