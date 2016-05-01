package shoeboxam.gitstream.commands;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import java.util.Arrays;
import java.util.List;

public class CommandUpdate implements ICommand {

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
		CommandUpdateThread execution = new CommandUpdateThread(arguments);
		execution.start();
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
}
