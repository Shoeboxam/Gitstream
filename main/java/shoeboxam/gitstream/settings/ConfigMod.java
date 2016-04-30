package shoeboxam.gitstream.settings;

import java.util.List;

import net.minecraft.client.Minecraft;

public class ConfigMod {
	public String mod_id;
	public String mod_name;
	public String mod_dir;
	public String mod_version;
	public String mc_version = Minecraft.getMinecraft().getVersion();
	public List<String> mod_authors;
	public String url_website;
	public String description;
}
