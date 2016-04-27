package shoeboxam.gitstream.utils;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.imageio.ImageIO;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import shoeboxam.gitstream.settings.ModDescriptor;
import shoeboxam.gitstream.settings.FileInfo;
import shoeboxam.gitstream.settings.GitstreamData;
import shoeboxam.gitstream.settings.McmodInfo;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class FileManager {
	GitstreamData data = get_data();
	
	private GitstreamData get_data(){
		
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
	    
		try {
			String json_string = IOUtils.toString(new FileInputStream(local_path.toString() + "\\" + "settings.json"));
			data = new Gson().fromJson(json_string, GitstreamData.class);
		} catch (IOException e) {
			data = new GitstreamData();
		}
		return data;
	}


	private FileManager(){
	}
	
	
	private static class FileManagerHolder { 
	    private static final FileManager INSTANCE = new FileManager();
	}

	public static FileManager getInstance() {
	    return FileManagerHolder.INSTANCE;
	}
	
	public boolean create_resource_pack(){
		File resource_stamp_location = new File(data.workspace_directory.toString() + "\\" + "resource_stamps.info");
		Map<File, FileInfo> resource_stamps = load_stamps(resource_stamp_location);
		
		// Copy mod patches into resourcepack

		Map<String, String> mod_bindings = modname_bindings();
		Iterator<Entry<String, String>> iter = mod_bindings.entrySet().iterator();
		while (iter.hasNext()){
			Map.Entry<String, String> pair = (Map.Entry<String, String>)iter.next();
			File patch_location = new File(data.repository_directory.toString() + "\\" + pair.getValue() + "\\");
			
			try {
				FileUtils.copyDirectory(new File(Paths.get(patch_location.toString()).normalize().toString()), 
						new File(Paths.get(data.resourcepack_directory.toString()).normalize().toString()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			iter.remove();
		}
		
		// Create a pack mcmeta
		try {
			String json = "{\"pack\": {\"pack_format\": 2,\"description\": \"Edit me!\"}}";
			
			FileUtils.writeStringToFile(new File(data.resourcepack_directory + "\\pack.mcmeta"), json);
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		List<File> paths = get_paths(data.resourcepack_directory);
		for (File path : paths){
			FileInfo info = new FileInfo(path.lastModified(), path.length());
			resource_stamps.put(new File(path.toString().replace(data.resourcepack_directory.toString(), "")), info);
		}
        
		del_empty(data.resourcepack_directory);
        
        try{
        	save_stamps(new File(data.workspace_directory.toString() + "\\" + "resource_stamps.info"), resource_stamps);
        } catch (IOException e) {
        	e.printStackTrace();
        	return false;
        }
        
        return true;
	}
	
	private List<File> get_paths(File folder) {
		List<File> paths = new ArrayList<File>();
	    for (File file : folder.listFiles()) {
	        if (file.isDirectory()) {
	            paths.addAll(get_paths(file));
	        } else {
	            paths.add(file);
	        }
	    }
	    return paths;
	}
	
	private List<File> get_paths(File folder, FilenameFilter filter) {
		List<File> paths = new ArrayList<File>();
	    for (File file : folder.listFiles(filter)) {
	        if (file.isDirectory()) {
	            paths.addAll(get_paths(file, filter));
	        } else {
	            paths.add(file);
	        }
	    }
	    return paths;
	}
	
	public HashMap<File, FileInfo> load_stamps(File stamp_location){
		if (stamp_location.exists()){
			try {
				ObjectInputStream s = new ObjectInputStream(new FileInputStream(stamp_location));
			    @SuppressWarnings("unchecked")
			    HashMap<File, FileInfo> stamp_paths = (HashMap<File, FileInfo>) s.readObject();
			    s.close();
			    return stamp_paths;
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				return new HashMap<File, FileInfo>();
			}
		}
		return new HashMap<File, FileInfo>();
	}
	
	private Map<String, String> modname_bindings_fallback(){
		Map<String, String> mods_installed = new HashMap<String, String>();
    	for (ModContainer mod : Loader.instance().getActiveModList()){
    		// Modid -> modname
    		mods_installed.put(mod.getModId(), mod.getName());
    	}
    	return mods_installed;
	}
	
	private Map<String, String> modname_bindings(){
		Map<String, String> mods_installed = new HashMap<String, String>();
		File[] patch_dirs = data.repository_directory.listFiles(new FilenameFilter() {
	        @Override
	        public boolean accept(File dir, String name) {
	        	if (new File(dir.toString() + "\\" + name).isDirectory() && name != ".git"){
	        		return true;
	        	}
	        	
	            return false;
	        }
	    });
		
		for (File patch : patch_dirs){
			Path patch_path = Paths.get(patch.toString());
			File texture_domains = new File(patch.toString() + "\\" + "assets" + "\\");
			if (texture_domains.exists()){
				//Modid -> modname
				for (File modid_dir : texture_domains.listFiles(new FilenameFilter() {
			        @Override
			        public boolean accept(File dir, String name) {
			        	if (new File(dir.toString() + "\\" + name).isDirectory()){
			        		return true;
			        	}
			        	
			            return false;
			        }
			    })){
					mods_installed.put(modid_dir.toString(), patch_path.getName(patch_path.getNameCount()-1).toString());
				}
			}
		}
		
		return mods_installed;
	}
	
	private Map<String, String> modname_bindings_masked(){
		Map<String, String> mods_installed = new HashMap<String, String>();
		File[] patch_dirs = data.repository_directory.listFiles(new FilenameFilter() {
	        @Override
	        public boolean accept(File dir, String name) {
	        	if (new File(dir.toString() + "\\" + name).isDirectory() && name != ".git"){
	        		return true;
	        	}
	        	
	            return false;
	        }
	    });
		
		for (File patch : patch_dirs){
			Path patch_path = Paths.get(patch.toString());
			File texture_domains = new File(patch.toString() + "\\" + "assets" + "\\");
			if (texture_domains.exists()){
				//Modid -> modname
				for (File modid_dir : texture_domains.listFiles(new FilenameFilter() {
			        @Override
			        public boolean accept(File dir, String name) {
			        	if (new File(dir.toString() + "\\" + name).isDirectory()){
			        		return true;
			        	}
			        	
			            return false;
			        }
			    })){
					Path modid_path = modid_dir.toPath();
					mods_installed.put(modid_path.getName(modid_path.getNameCount() - 1).toString(), 
							patch_path.getName(patch_path.getNameCount()-1).toString());
				}
			}
		}
		
		return mods_installed;
	}
	
	public void save_stamps(File stamp_location, Map<File, FileInfo> stamp_paths) throws IOException {
		if (!stamp_location.exists()){
			stamp_location.getParentFile().mkdirs();
		}
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(stamp_location));
		oos.writeObject(stamp_paths);
		oos.close();
	}
	
	public boolean update_defaults(){
		
		File staging_dir_init = new File(data.workspace_directory.toString() + "\\" + "default_staging");
		staging_dir_init.mkdirs();
		data.modinfo_directory.mkdirs();
		try {
			staging_dir_init = staging_dir_init.getCanonicalFile();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		final File staging_dir = staging_dir_init;
		
		File[] jar_paths;
		try {
			jar_paths = new File(new File(Minecraft.getMinecraft().mcDataDir + "\\mods").getCanonicalPath()).listFiles();
			
			File resource_stamp_location = new File(data.workspace_directory + "\\" + "resource_stamps.info");
			final Map<File, FileInfo> resource_stamps = load_stamps(resource_stamp_location);
			
			File default_stamp_location = new File(data.workspace_directory + "\\" + "placeholder_stamps.info");
			final Map<File, FileInfo> placeholder_stamps = load_stamps(default_stamp_location);
			
			for (File modpath : jar_paths){
				//Extract contents of jar:
				try {
					JarFile jar = new JarFile(modpath);
					Enumeration<JarEntry> enumEntries = jar.entries();
					while (enumEntries.hasMoreElements()) {
					    JarEntry file = (java.util.jar.JarEntry) enumEntries.nextElement();
					    java.io.File f = new java.io.File(staging_dir + File.separator + file.getName());
					    if (file.isDirectory()) {
					        f.mkdir();
					        continue;
					    }
					    try {
					    	InputStream input_stream = jar.getInputStream(file);
						    FileOutputStream output_stream = new FileOutputStream(f);
						    while (input_stream.available() > 0) {
						    	output_stream.write(input_stream.read());
						    }
						    output_stream.close();
						    input_stream.close();
					    } catch (FileNotFoundException e) {
					    	e.printStackTrace();
					    }
					}
					
					//Filter unneeded files out of extracted files:
				    List<File> non_image_files = get_paths(staging_dir, new FilenameFilter() {
				        @Override
				        public boolean accept(File dir, String name) {
				        	
				        	//Delete all non graphics files
				        	if (!name.matches("([^\\s]+(\\.(?i)(png|mcmeta|info))$)")){
				        		return true;
				        	}
				        	//Delete all files that conflict with resources
				        	File subpath = new File(dir.toString().replace(staging_dir.toString(), "") + "\\" + name);
							
				        	if (resource_stamps.containsKey(subpath)){
				        		return true;
				        	}
				        	
				        	//Delete all files that would overwrite an edited placeholder
				        	File path = new File(dir.toString() + "\\" + name);
				        	if (placeholder_stamps.containsKey(subpath) && (path.lastModified() > placeholder_stamps.get(subpath).timestamp || 
				        			path.length() != placeholder_stamps.get(subpath).size)){
				        		return true;
				        	}
				            return false;
				        }
				    });
				    
					jar.close();
					
					Path modid_path = new File(staging_dir.toString() + "//assets//").listFiles()[0].toPath();
					System.out.println(modid_path.toString());
					String modid = modid_path.getName(modid_path.getNameCount() - 1).toString();
					
					Files.copy(new File(staging_dir.toString() + "\\" + "mcmod.info"), 
							new File(data.modinfo_directory.toString() + "\\" + modid + ".info"));
					
					for (File file : non_image_files){
				    	file.delete();
				    }

				} catch (IOException e){
					e.printStackTrace();
					return false;
				}
			}

			
			//Resize images
			
			staging_dir.mkdirs();
			List<File> files = get_paths(staging_dir);
			
			for (File file : files){
				if (file.toString().matches("([^\\s]+(\\.(?i)(png))$)")) {
	        		try {
	        			BufferedImage image = ImageIO.read(file);
	            		int width = image.getWidth(null);
	            		int height = image.getHeight(null);
	            		
	            		BufferedImage image_output = new BufferedImage(
	            				width * data.config.scalar, height * data.config.scalar, BufferedImage.TYPE_INT_ARGB);
	            		Graphics2D graphics2D = image_output.createGraphics();
	            		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	            		graphics2D.drawImage(image, 0, 0, width * data.config.scalar, height * data.config.scalar, null);
						ImageIO.write(image_output, "PNG", file);
						graphics2D.dispose();
						
					} catch (IOException e) {
						e.printStackTrace();
					}
	        	}
				FileInfo info = new FileInfo(file.lastModified(), file.length());
				placeholder_stamps.put(new File(file.toString().replace(staging_dir.toString(), "")), info);
			}
	        
	        try {
	        	save_stamps(default_stamp_location, placeholder_stamps);
	        } catch (IOException e){
	        	e.printStackTrace();
	        	return false;
	        }
	        update_untextured_stamps(data.resourcepack_directory, placeholder_stamps);
	        del_empty(staging_dir);
			
	        //Copy default files into resource pack
	        try {
				FileUtils.copyDirectory(
						new File(Paths.get(staging_dir.toString()).normalize().toString()), 
						new File(Paths.get(data.resourcepack_directory.toString()).normalize().toString()));
//				FileUtils.deleteDirectory(staging_dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
        return true;
	}
	
	
	public boolean update_resources(){
		File resource_stamp_location = new File(data.workspace_directory + "\\" + "resource_stamps.info");
		Map<File, FileInfo> resource_stamps = load_stamps(resource_stamp_location);
		
		File placeholder_stamp_location = new File(data.workspace_directory + "\\" + "placeholder_stamps.info");
		Map<File, FileInfo> placeholder_stamps = load_stamps(placeholder_stamp_location);
		
		for (File patch : data.repository_directory.listFiles(new FilenameFilter() {
	        @Override
	        public boolean accept(File dir, String name) {
	        	File loc = new File(dir.toString() + "\\" + name);
	        	if (loc.toString().contains(".git") || loc.isFile()) {
	        			return false;
	        	}
	        	return true;
	        }
	        })) {
			System.out.println(patch.toString());
			for (File repository_path : get_paths(patch)) {
				File relative_path = new File(repository_path.toPath().normalize().toString().replace(patch.toString(), ""));
				File resource_path = new File(data.resourcepack_directory.toPath().normalize().toString() + "\\" + relative_path);
				try {
					resource_path = resource_path.getCanonicalFile();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				
				// If not resource, or is unedited resource, or DNE, continue
				if (!resource_stamps.containsKey(relative_path) || 
						(resource_stamps.containsKey(relative_path) && 
								(resource_path.lastModified()) == resource_stamps.get(relative_path).timestamp && 
								 resource_path.length() == resource_stamps.get(relative_path).size)){
					// If not placeholder, or is unedited placeholder, or DNE, continue
					if (!(placeholder_stamps.containsKey(relative_path)) || 
							(placeholder_stamps.containsKey(relative_path) && 
									(resource_path.lastModified()) == placeholder_stamps.get(relative_path).timestamp && 
									 resource_path.length() == placeholder_stamps.get(relative_path).size)){
						try {
							Files.copy(repository_path, resource_path);
							
							// Remove from placeholders if it previously was one
							if (placeholder_stamps.containsKey(relative_path)){
								placeholder_stamps.remove(relative_path);
							}
							
							// Add to resource stamps if file is copied over
							FileInfo info = new FileInfo(resource_path.lastModified(), resource_path.length());
							resource_stamps.put(relative_path, info);
							
						} catch (IOException e) {
							e.printStackTrace();
							return false;
						}
					}
				}
			}
		}
		
		try{
			save_stamps(new File(data.workspace_directory + "\\" + "resource_stamps.info"), resource_stamps);
	        save_stamps(new File(data.workspace_directory + "\\" + "placeholder_stamps.info"), placeholder_stamps);
        } catch (IOException e) {
        	e.printStackTrace();
        	return false;
        }
		
		update_untextured_stamps(data.resourcepack_directory, placeholder_stamps);
		
        return true;
	}
	
	public List<String> apply_edits(){
		
		Set<String> modids_edited = new HashSet<String>();
		List<String> patchnames_edited = new ArrayList<String>();
		
		Map<String, String> bindings = modname_bindings_masked();
		Map<String, String> bindings_fallback = modname_bindings_fallback();
		
		File resource_stamp_location = new File(data.workspace_directory + "\\" + "resource_stamps.info");
		Map<File, FileInfo> resource_stamps = load_stamps(resource_stamp_location);
		
		File placeholder_stamp_location = new File(data.workspace_directory + "\\" + "placeholder_stamps.info");
		Map<File, FileInfo> placeholder_stamps = load_stamps(placeholder_stamp_location);
		
		// 1. Transfer additions
		List<File> paths = get_paths(data.resourcepack_directory);
		for (File file : paths){
			File relative_path = new File(file.toString().replace(data.resourcepack_directory.toString(), ""));
			
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
				if (patch_name != "null"){
					try {
						File destination = new File(data.repository_directory.toString() + "\\" + patch_name + "\\" + relative_path);
						destination.getParentFile().mkdir();
						Files.copy(new File(data.resourcepack_directory.toString() + "\\" + relative_path), destination);
					} catch (IOException e) {
						e.printStackTrace();
					}
					
					// Tracking
					modids_edited.add(modid);
					if (placeholder_stamps.containsKey(relative_path)){
						placeholder_stamps.remove(relative_path);
					}
					FileInfo info = new FileInfo(file.lastModified(), file.length());
					resource_stamps.put(relative_path, info);	
				}
			}
		}
		update_untextured_stamps(data.resourcepack_directory, placeholder_stamps);
		// 2. Transfer removals
		for (File file : resource_stamps.keySet()){
			File relative_path = new File(data.resourcepack_directory.toString() + "\\" + 
					file.toString().replace(data.resourcepack_directory.toString(), ""));
			
			Path path = Paths.get(relative_path.toString());
			
			String modid = bindings.get(path.getName(2).toString());
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
			File destination = new File(data.repository_directory.toString() + "\\" + patch_name + "\\" + relative_path);
			destination.delete();
			
			
			// Tracking
			modids_edited.add(modid);
			resource_stamps.remove(relative_path);
			
		}
		
		// Create mod.json for patch file
		for (String modid : modids_edited){
			ModDescriptor descriptor = new ModDescriptor();
			
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
			File modinfo_location = new File(data.modinfo_directory.toString() + "\\" + modid + ".info");
			try {
				McmodInfo[] mod_info_array = gson.fromJson(IOUtils.toString(new FileInputStream(modinfo_location)), McmodInfo[].class);
				int i = 0;
				for (McmodInfo mod_info : mod_info_array){
					descriptor.mod_id = mod_info.modid;
					descriptor.mod_name = mod_info.name;
					descriptor.mod_dir = "/" + patch_name.toString() + "/";
					descriptor.mod_version = mod_info.version;
					descriptor.mod_authors = mod_info.authors;
					descriptor.url_website = mod_info.url;
					descriptor.description = mod_info.description;
					
					File destination;
					if (i == 0){
						destination = new File(data.repository_directory.toString() + "\\" + patch_name + "\\" + "mod.json");
					} else {
						destination = new File(data.repository_directory.toString() + "\\" + patch_name + "\\" + "mod_" + i + ".json");
					}
					i++;
					FileUtils.writeStringToFile((destination), gson.toJson(descriptor));
				}
				
			} catch (JsonSyntaxException | IOException e) {
				e.printStackTrace();
			}
		}
		
		try{ 
			save_stamps(new File(data.workspace_directory + "\\" + "resource_stamps.info"), resource_stamps);
			save_stamps(new File(data.workspace_directory + "\\" + "placeholder_stamps.info"), placeholder_stamps);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return patchnames_edited;
	}
	
	private void del_empty(File directory) {
		if (directory.isDirectory()){
			File[] contents = directory.listFiles();
			
			for (File item : contents){
				del_empty(item);
			}
			if (directory.listFiles() == null){
				directory.delete();
			}
		}
	}
	public boolean update_untextured_stamps(File directory, Map<File, FileInfo> placeholder_stamps){
		File untextured_loc = new File(directory.toString() + "\\" + "~untextured.txt");
		
		if(untextured_loc.exists()){
			untextured_loc.delete();
		}
		List<String> filenames = new ArrayList<String>();
	    for (File file : directory.listFiles()) {
	    	String relative_path = file.toString().replace(data.resourcepack_directory.toString(), "");
	        if (file.isDirectory()) {
	            update_untextured_stamps(file, placeholder_stamps);
	        } else if (placeholder_stamps.containsKey(new File(relative_path))) {
	            filenames.add(file.toPath().getName(file.toPath().getNameCount() - 1).toString());
	        }
	    }
	    
	    if (filenames.size() > 0){
	    	try {
			    FileWriter writer = new FileWriter(untextured_loc); 
			    
			    for(String name: filenames) {
			      writer.write(name + "\n");
			    }
			    writer.close();
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
	    }
		
		return true;
	}
}
