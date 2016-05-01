package shoeboxam.gitstream.commands;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import shoeboxam.gitstream.GitManager;;

public class CommandGit implements ICommand {
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
		
		if (arguments.get(0).equals("config")){
			
			if (arguments.get(1).equals("username")){
				git.set_username(StringUtils.join(arguments.subList(2, arguments.size()), " "));
				sender.addChatMessage(new TextComponentTranslation("Username updated."));
			}
			else if (arguments.get(1).equals("email")){
				git.set_email(arguments.get(2));
				sender.addChatMessage(new TextComponentTranslation("Email address updated."));
			}
			else if (arguments.get(1).equals("password")){
				git.set_password(arguments.get(2));
				sender.addChatMessage(new TextComponentTranslation("Password updated."));
			} 
			else if (arguments.get(1).equals("save")){
				git.save_credentials();
				sender.addChatMessage(new TextComponentTranslation("Git credentials saved."));
			}
		}
		
		if (arguments.get(0).equals("push")){
			git.push();
		}
		
		if (arguments.get(0).equals("commit")){
			String message = StringUtils.join(arguments.subList(2, arguments.size()), " ");
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
	
}
