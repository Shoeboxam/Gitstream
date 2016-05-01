package shoeboxam.gitstream;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;

import shoeboxam.gitstream.commands.CommandGit;
import shoeboxam.gitstream.commands.CommandUpdate;

@Mod(modid = Gitstream.MODID, version = Gitstream.VERSION)
public class Gitstream
{
    public static final String MODID = "gitstream";
    public static final String VERSION = "1.0";
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
    }
    
    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandGit());
        event.registerServerCommand(new CommandUpdate());
    }
}
