package shoeboxam.gitstream.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import shoeboxam.gitstream.settings.StampID;
import shoeboxam.gitstream.settings.McmodInfo;
import shoeboxam.gitstream.settings.ConfigMod;

public class EditManager extends FileManager {

	@SuppressWarnings("unchecked")
	public List<String> apply_edits(){
		Map<String, List<McmodInfo>> mod_info_listing = new HashMap<String, List<McmodInfo>>();
		
		if (config.mcmodinfo_location.exists()){
			try {
				ObjectInputStream s = new ObjectInputStream(new FileInputStream(config.mcmodinfo_location));
			    mod_info_listing = (HashMap<String, List<McmodInfo>>) s.readObject();
			    s.close();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
			}
		}
			
			Set<String> modids_edited = new HashSet<String>();
			List<String> patchnames_edited = new ArrayList<String>();
			
			Map<String, String> bindings = modname_bindings_masked();
			Map<String, String> bindings_fallback = modname_bindings_fallback();
			
			Map<File, StampID> resource_stamps = FileManager.load_stamps(config.resource_stamp_location);
			Map<File, StampID> placeholder_stamps = FileManager.load_stamps(config.placeholder_stamp_location);
			
			// 1. Transfer additions
			List<File> paths = get_paths(config.resourcepack_directory);
			for (File file : paths){
				File relative_path = new File(file.toString().replace(config.resourcepack_directory.toString(), ""));
				
				//File is added to the repository...
						// IF untracked
				if ((!resource_stamps.containsKey(relative_path) && !placeholder_stamps.containsKey(relative_path))
						// OR if it is a resource with an updated filesize or timestamp
						|| (resource_stamps.containsKey(relative_path) && 
								(file.lastModified() != resource_stamps.get(relative_path).timestamp ||
								 file.length() != resource_stamps.get(relative_path).size)
						// OR if it is a placeholder with an updated filesize or timestamp
						|| (placeholder_stamps.containsKey(relative_path) && 
								(file.lastModified() > placeholder_stamps.get(relative_path).timestamp ||
								 file.length() != placeholder_stamps.get(relative_path).size)))){
	
					// Determine the name of the patch to send the updated file to:
					String modid;
					try {
						modid = relative_path.toPath().getName(1).toString();
					} catch (IllegalArgumentException e) {
						continue;
					}
					String patch_name = "";
					
					// Preserve mod repo patch names
					if (bindings.containsKey(modid)) {
						patch_name = bindings.get(modid);
	//					System.out.println("# Primary: " + patch_name);
					// Otherwise, attempt to name the patch after the mod name
					} else if (bindings_fallback.containsKey(modid)){
						patch_name = bindings_fallback.get(modid).replaceAll(" ", "_");
	//					System.out.println("# Secondary: " + patch_name);
					// As a last resort, name the patch after the assets directory
					} else {
						patch_name = modid;
	//					System.out.println("# Modid: " + patch_name);
					}
	
					// Copy the resource back to the repository
					if (patch_name != null){
						try {
							File destination = new File(config.repository_directory.toString() + "\\" + patch_name + "\\" + relative_path);
							destination.getParentFile().mkdirs();
							Files.copy(new File(config.resourcepack_directory.toString() + "\\" + relative_path), destination);
						} catch (IOException e) {
							e.printStackTrace();
						}
						
						// Tracking
						modids_edited.add(modid);
						if (placeholder_stamps.containsKey(relative_path)){
							placeholder_stamps.remove(relative_path);
						}
						StampID info = new StampID(file.lastModified(), file.length());
						resource_stamps.put(relative_path, info);	
					}
				}
			}
			// TODO: Fix me
			// 2. Transfer removals
			for (File file : resource_stamps.keySet()){
				File relative_path = new File("\\" + file.toString().replace(config.resourcepack_directory.toString(), ""));
				Path path_subtractor = config.resourcepack_directory.toPath();
				Path path = file.toPath();
				
				String modid = null;
				if (path.getNameCount() > path_subtractor.getNameCount() + 2){
					modid = bindings.get(path.getName(path_subtractor.getNameCount() + 2).toString());
				}
				String patch_name = "";
				
				if (!file.exists()){
					// Preserve mod repo patch names
					if (bindings.containsKey(modid)) {
						patch_name = bindings.get(modid);
					// Otherwise, attempt to name the patch after the mod name
					} else if (bindings_fallback.containsKey(modid)){
						patch_name = bindings_fallback.get(modid).replaceAll(" ", "_");
					// As a last resort, name the patch after the assets directory
					} else {
						patch_name = modid;
					}
				}
				// Apply the deletion back at the repository
				File destination = new File(config.repository_directory.toString() + "\\" + patch_name + "\\" + relative_path);
				destination.delete();
				
				
				// Tracking
				modids_edited.add(modid);
				resource_stamps.remove(relative_path);
				
			}
			
			// Create mod.json for patch file
			for (String modid : modids_edited){
				ConfigMod descriptor = new ConfigMod();
				
				String patch_name = "";
				// Preserve mod repo patch names
				if (bindings.containsKey(modid)) {
					patch_name = bindings.get(modid);
				// Otherwise, attempt to name the patch after the mod name
				} else if (bindings_fallback.containsKey(modid)){
					patch_name = bindings_fallback.get(modid).replaceAll(" ", "_");
				// As a last resort, name the patch after the assets directory
				} else {
					patch_name = modid;
				}
				
				patchnames_edited.add(patch_name);
				
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				
				if (mod_info_listing.containsKey(modid)){
					
					int i = 0;
					for (McmodInfo mod_info : mod_info_listing.get(modid)){
						descriptor.mod_id = mod_info.modid;
						descriptor.mod_name = mod_info.name.replace("\u0027", "'"); //REALLY, BOP?!
						descriptor.mod_dir = "/" + patch_name.toString() + "/";
						descriptor.mod_version = mod_info.version;
						descriptor.mod_authors = mod_info.authors;
						descriptor.url_website = mod_info.url;
						descriptor.description = mod_info.description;
						descriptor.mc_version = config.minecraft_version;
						
						File destination;
						if (i == 0){
							destination = new File(config.repository_directory.toString() + "\\" + patch_name + "\\" + "mod.json");
						} else {
							destination = new File(config.repository_directory.toString() + "\\" + patch_name + "\\" + "mod_" + i + ".json");
						}
						i++;
						try {
							FileUtils.writeStringToFile((destination), gson.toJson(descriptor));
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			
			try{ 
				save_stamps(config.resource_stamp_location, resource_stamps);
				save_stamps(config.placeholder_stamp_location, placeholder_stamps);
			} catch (IOException e) {
				e.printStackTrace();
			}
	
			return patchnames_edited;
		}

}
