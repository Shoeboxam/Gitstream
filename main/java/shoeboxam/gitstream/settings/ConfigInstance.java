package shoeboxam.gitstream.settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class ConfigInstance {
	
	public ConfigInstance() {
		
		if (instance_config_location.exists()){
			try {
				String json_string = IOUtils.toString(new FileInputStream(instance_config_location));
				
				JsonElement root = new JsonParser().parse(json_string);
				
				name = root.getAsJsonObject().get("name").getAsString();
				
				remote_url = root.getAsJsonObject().get("remote_url").getAsString();
				scalar = root.getAsJsonObject().get("scalar").getAsInt();
				minecraft_version = root.getAsJsonObject().get("minecraft_version").getAsString();
				
				symlink = root.getAsJsonObject().get("symlink").getAsBoolean();
				symlink_dir = new File(root.getAsJsonObject().get("symlink_dir").getAsString());
				
				home_directory = new File(root.getAsJsonObject().get("home_directory").getAsString());
				instance_config_location = new File(root.getAsJsonObject().get("instance_config_location").getAsString());
				workspace_directory = new File(root.getAsJsonObject().get("workspace_directory").getAsString());
				repository_directory = new File(root.getAsJsonObject().get("repository_directory").getAsString());
				resourcepack_directory = new File(root.getAsJsonObject().get("resourcepack_directory").getAsString());
				mods_directory = new File(root.getAsJsonObject().get("mods_directory").getAsString());
				
				placeholder_stamp_location =new File(root.getAsJsonObject().get("placeholder_stamp_location").getAsString());
				resource_stamp_location = new File(root.getAsJsonObject().get("resource_stamp_location").getAsString());
				mcmodinfo_location = new File(root.getAsJsonObject().get("mcmodinfo_location").getAsString());
			} catch (IOException e) {
			}
		}
		else {
			JsonObject instance = new JsonObject();
			instance.addProperty("name", name);
			instance.addProperty("remote_url", remote_url);
			instance.addProperty("scalar", scalar);
			instance.addProperty("minecraft_version", minecraft_version);
			
			instance.addProperty("symlink", symlink);
			instance.addProperty("symlink_dir", symlink_dir.toString());
			
			instance.addProperty("home_directory", home_directory.toString());
			instance.addProperty("instance_config_location", instance_config_location.toString());
			instance.addProperty("workspace_directory", workspace_directory.toString());
			instance.addProperty("repository_directory", repository_directory.toString());
			instance.addProperty("resourcepack_directory", resourcepack_directory.toString());
			instance.addProperty("mods_directory", mods_directory.toString());
			
			instance.addProperty("placeholder_stamp_location", placeholder_stamp_location.toString());
			instance.addProperty("resource_stamp_location", resource_stamp_location.toString());
			instance.addProperty("mcmodinfo_location", mcmodinfo_location.toString());
			
			try {
				FileUtils.writeStringToFile((instance_config_location), new GsonBuilder().setPrettyPrinting().create().toJson(instance));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		
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
	
	private ConfigResourcepack config = get_config();
	public String name = config.name;
	public String remote_url = config.remote_url;
	public int scalar = config.scalar;
	public String minecraft_version = config.minecraft_version;
	
	public boolean symlink = false;
	public File symlink_dir = new File(Minecraft.getMinecraft().mcDataDir + "\\resourcepacks\\" + name);
	
	public File home_directory = homeDirectory();
	public File workspace_directory = new File(home_directory.toString() + "\\" + name);
	public File instance_config_location = new File(workspace_directory.toString() + "\\" + "instance_config.json");
	public File repository_directory = new File(workspace_directory.toString() + "\\" + "repository");
	public File resourcepack_directory = new File(workspace_directory + "\\" + "resources");
	public File mods_directory = new File(Minecraft.getMinecraft().mcDataDir + "\\mods");
	
	public File placeholder_stamp_location = new File(workspace_directory + "\\" + "placeholder_stamps.info");
	public File resource_stamp_location = new File(workspace_directory + "\\" + "resource_stamps.info");
	public File mcmodinfo_location = new File(workspace_directory + "\\" + "mcmod.info");
	
	public ConfigResourcepack get_config(){
		ConfigResourcepack config = new ConfigResourcepack();
		try {
			 Gson json = new Gson();
			 String json_string = IOUtils.toString(Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation("gitstream", "/config.json")).getInputStream());
			 config = json.fromJson(json_string, ConfigResourcepack.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return config;
	}
	
	public static File homeDirectory()
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
	
	public static ConfigInstance get_data(){
		ConfigInstance data;
		try {
			String json_string = IOUtils.toString(new FileInputStream(homeDirectory().toString() + "\\" + "settings.json"));
			data = new Gson().fromJson(json_string, ConfigInstance.class);
		} catch (IOException e) {
			data = new ConfigInstance();
		}
		return data;
	}
}