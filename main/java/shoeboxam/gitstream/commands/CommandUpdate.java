package shoeboxam.gitstream.commands;

import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import shoeboxam.gitstream.GitManager;
import shoeboxam.gitstream.files.EditManager;
import shoeboxam.gitstream.files.FileManager;
import shoeboxam.gitstream.files.ResourcePackManager;
import shoeboxam.gitstream.settings.ConfigInstance;

import java.io.File;
import java.util.Arrays;

public class CommandUpdate implements ICommand {
	
	ResourcePackManager resource_manager = new ResourcePackManager();
	EditManager edit_manager = new EditManager();
	GitManager git = GitManager.getInstance();

	@Override
	public int compareTo(ICommand arg0) {
		return 0;
	}

	@Override
	public String getCommandName() {
		return "update";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "commands.update.usage";
	}

	@Override
	public List<String> getCommandAliases() {
		return Arrays.asList();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		List<String> arguments = Arrays.asList(args);
		ConfigInstance config = new ConfigInstance();
		
		if (arguments.isEmpty()){
			
			if (!remote_get(config, sender)){
				if (!git.has_credentials()){
					help(sender);
				} else {
					sender.addChatMessage(new TextComponentTranslation("Unable to retrieve data from Github. Additional information in console."));
			
				}
			}
			
			if (!git_to_resource(config, sender)){
				sender.addChatMessage(new TextComponentTranslation("Unable to transfer resources to resource pack. Additional information in console."));
			}
			if (!resource_manager.update_defaults()){
				sender.addChatMessage(new TextComponentTranslation("Unable to transfer placeholders to resource pack. Additional information in console."));
			}
			if (!resource_manager.update_metadata()){
				sender.addChatMessage(new TextComponentTranslation("Unable to update metadata. Additional information in console."));
			}
			
			sender.addChatMessage(new TextComponentTranslation("Update completed."));
			return;
		}
		
		if (arguments.get(0).equals("git")){
			if (remote_get(config, sender)){
				sender.addChatMessage(new TextComponentTranslation("Repository is ready."));
			} else {
				if (!git.has_credentials()){
					help(sender);
				} else {
					sender.addChatMessage(new TextComponentTranslation("Unable to retrieve data from Github. Additional information in console."));
			
				}
			}
			return;
		}
		
		if (arguments.get(0).equals("resources")){
			if (git_to_resource(config, sender)){
				sender.addChatMessage(new TextComponentTranslation("Resources are updated."));
			} else {
				sender.addChatMessage(new TextComponentTranslation("Unable to transfer resources to resource pack. Additional information in console."));
			}
			return;
		}
		
		if (arguments.get(0).equals("placeholders")){
			if (resource_manager.update_defaults()){
				sender.addChatMessage(new TextComponentTranslation("Placeholders are updated."));
			} else {
				sender.addChatMessage(new TextComponentTranslation("Unable to transfer placeholders to resource pack. Additional information in console."));
			}
			return;
		}
		
		if (arguments.get(0).equals("metadata")){
			if (resource_manager.update_metadata()){
				sender.addChatMessage(new TextComponentTranslation("Metadata is updated."));
			} else {
				sender.addChatMessage(new TextComponentTranslation("Unable to update metadata. Additional information in console."));
			}
			return;
		}
		
		if (arguments.get(0).equals("sync")){
			// Hack to catch metadata edits in commits
			List<String> edited_mods = edit_manager.apply_edits();
			resource_manager.update_metadata();
			edited_mods.addAll(edit_manager.apply_edits());
			sender.addChatMessage(new TextComponentTranslation("Edits transferred to clone."));
		}
		
		if (arguments.get(0).equals("clean")){
			for (File dir : config.repository_directory.listFiles()){
				if (!dir.getName().equals(".git") && dir.isDirectory()){
					FileManager.del_empty(config.repository_directory);
				}
			}
		}
		
		if (arguments.get(0).equals("push") || arguments.get(0).equals("send")){
			
			if (remote_get(config, sender)){
				sender.addChatMessage(new TextComponentTranslation("Remote changes fetched."));
			} else {
				sender.addChatMessage(new TextComponentTranslation("Unable to connect with Github. Additional information in console."));
				return;
			}
			
			// Hack to catch metadata edits in commits
			List<String> edited_mods = edit_manager.apply_edits();
			resource_manager.update_metadata();
			edited_mods.addAll(edit_manager.apply_edits());
			sender.addChatMessage(new TextComponentTranslation("Edits transferred to clone."));

			for (String patchname : edited_mods){
				if (patchname != null){
					git.commit(patchname, patchname);
				}
			}
			
			if (git.has_credentials()){
				git.push();
			} else {
				help(sender);
			}
			
			// Transfer resources
			if (resource_manager.update_resources()){
				sender.addChatMessage(new TextComponentTranslation("Resources updated."));
			} else {
				sender.addChatMessage(new TextComponentTranslation("Unable to refresh resource pack."));
			}
			
		}
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return true;
	}

	@Override
	public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender, String[] args,
			BlockPos pos) {
		return Arrays.asList("up");
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return false;
	}
	
	private boolean remote_get(ConfigInstance data, ICommandSender sender){
		
		// Get files from remote
		if (data.repository_directory.exists()){
			sender.addChatMessage(new TextComponentTranslation("Downloading changes from Github..."));
			return git.pull();
		} else {
			sender.addChatMessage(new TextComponentTranslation("Downloading all resources from Github..."));
			return git.clone(data.remote_url);
		}
	}
	
	private boolean git_to_resource(ConfigInstance data, ICommandSender sender){
		
		if (new File(data.resourcepack_directory.toString() + "\\pack.mcmeta").exists()){
			sender.addChatMessage(new TextComponentTranslation("Transferring changes to development pack..."));
			return resource_manager.update_resources();
		} else {
			sender.addChatMessage(new TextComponentTranslation("Creating resource pack..."));
			return resource_manager.create_resource_pack();
		}
	}
	
	private void help(ICommandSender sender){
		sender.addChatMessage(new TextComponentTranslation("Please set login credentials: "));
		sender.addChatMessage(new TextComponentTranslation("  /git config username [username]"));
		sender.addChatMessage(new TextComponentTranslation("  /git config email [email]"));
		sender.addChatMessage(new TextComponentTranslation("  /git config password [password]"));
		sender.addChatMessage(new TextComponentTranslation("Optionally, save credentials with: "));
		sender.addChatMessage(new TextComponentTranslation("  /git config save "));
	}

}
