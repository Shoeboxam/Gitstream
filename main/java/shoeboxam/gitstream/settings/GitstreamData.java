package shoeboxam.gitstream.settings;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class GitstreamData {
	
	public GitstreamData() {
		symlink_dir.mkdirs();
		
		if (!symlink){
			resourcepack_directory = symlink_dir;
		} else {
			resourcepack_directory.mkdirs();
			if (!java.nio.file.Files.isSymbolicLink(resourcepack_directory.toPath())){
				try {
					java.nio.file.Files.createSymbolicLink(resourcepack_directory.toPath(), symlink_dir.toPath());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}	
	
	public ResourcepackConfig config = get_config();
	
	public boolean symlink = false;
	public File symlink_dir = new File(Minecraft.getMinecraft().mcDataDir + "\\resourcepacks\\" + config.name);
	
	public File home_directory = homeDirectory();
	public File settings_location = new File(home_directory.toString()+ "\\" + "settings.json");
	public File workspace_directory = new File(home_directory.toString() + "\\" + config.name);
	public File repository_directory = new File(workspace_directory.toString() + "\\" + "repository");
	public File resourcepack_directory = new File(workspace_directory + "\\" + "resources");
	public File modinfo_directory = new File(workspace_directory.toString() + "\\" + "modinfo");
	
	public boolean save_password = false;
	public String git_username = "";
	public String git_email = "";
	public String git_password = "";

	
	public ResourcepackConfig get_config(){
		ResourcepackConfig config = new ResourcepackConfig();
		try {
			 Gson json = new Gson();
			 String json_string = IOUtils.toString(Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("gitstream", "/config.json")).getInputStream());
			 config = json.fromJson(json_string, ResourcepackConfig.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return config;
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