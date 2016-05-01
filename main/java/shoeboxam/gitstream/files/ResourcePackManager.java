package shoeboxam.gitstream.files;

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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import shoeboxam.gitstream.settings.McmodInfo;
import shoeboxam.gitstream.settings.StampID;

public class ResourcePackManager extends FileManager {

	Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	public boolean create_resource_pack(){
		Map<File, StampID> resource_stamps = load_stamps(config.resource_stamp_location);
		
		// Copy mod patches into resourcepack
	
		Map<String, String> mod_bindings = modname_bindings();
		Iterator<Entry<String, String>> iter = mod_bindings.entrySet().iterator();
		while (iter.hasNext()){
			Map.Entry<String, String> pair = (Map.Entry<String, String>)iter.next();
			File patch_location = new File(config.repository_directory.toString() + "\\" + pair.getValue() + "\\");
			
			try {
				FileUtils.copyDirectory(new File(Paths.get(patch_location.toString()).normalize().toString()), 
						new File(Paths.get(config.resourcepack_directory.toString()).normalize().toString()));
			} catch (IOException e) {
				e.printStackTrace();
			}
			iter.remove();
		}
		
		// Create a pack mcmeta
		try {
			String json = "{\"pack\": {\"pack_format\": 2,\"description\": \"Edit me!\"}}";
			
			FileUtils.writeStringToFile(new File(config.resourcepack_directory + "\\pack.mcmeta"), json);
			
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		List<File> paths = get_paths(config.resourcepack_directory);
		for (File path : paths){
			StampID info = new StampID(path.lastModified(), path.length());
			resource_stamps.put(new File(path.toString().replace(config.resourcepack_directory.toString(), "")), info);
		}
	    
		del_empty(config.resourcepack_directory);
	    
	    try{
	    	save_stamps(config.resource_stamp_location, resource_stamps);
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return false;
	    }
	    
	    return true;
	}

	public boolean update_defaults(){
		File staging_dir = new File(config.workspace_directory.toString() + "\\" + "default_staging");
		
		try {
			staging_dir = staging_dir.getCanonicalFile();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		staging_dir.mkdirs();
		
		File[] jar_paths;
		try {
			File extraction_dir = new File(config.workspace_directory.toString() + "\\" + "temp_extraction");
			jar_paths = config.mods_directory.listFiles();

			Map<File, StampID> resource_stamps = load_stamps(config.resource_stamp_location);
			Map<File, StampID> placeholder_stamps = load_stamps(config.placeholder_stamp_location);
			
			for (File modpath : jar_paths){
				System.out.println("Preparing: " + modpath.getName());
				if (!modpath.toString().endsWith(".jar")){
					continue;
				}
				
				if (!extract_mod(modpath, extraction_dir)){
					continue;
				}
				
				if (!update_mcmodinfo(extraction_dir)){
					continue;
				}
				
				if (!remove_conflicts(extraction_dir, resource_stamps, placeholder_stamps)){
					continue;
				}
				
				if (!resize_resources(extraction_dir)){
					continue;
				}
				
				FileUtils.copyDirectory(
						new File(Paths.get(extraction_dir.toString()).normalize().toString()), 
						new File(Paths.get(staging_dir.toString()).normalize().toString()));
				FileUtils.deleteDirectory(extraction_dir);

			}
			
			// Update stamps
			for (File file : get_paths(staging_dir)){
				StampID info = new StampID(file.lastModified(), file.length());
				placeholder_stamps.put(new File(file.toString().replace(staging_dir.toString(), "")), info);
			}
	        try {
	        	save_stamps(config.placeholder_stamp_location, placeholder_stamps);
	        } catch (IOException e){
	        	e.printStackTrace();
	        	return false;
	        }
	        update_metadata_recurse(config.resourcepack_directory, placeholder_stamps);
	        del_empty(staging_dir);
			
	        //Copy default files into resource pack
	        try {
				FileUtils.copyDirectory(
						new File(Paths.get(staging_dir.toString()).normalize().toString()), 
						new File(Paths.get(config.resourcepack_directory.toString()).normalize().toString()));
				FileUtils.deleteDirectory(staging_dir);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		del_empty(config.resourcepack_directory);
        return true;
	}

	public boolean update_metadata(){
		return update_metadata_recurse(config.resourcepack_directory,load_stamps(config.placeholder_stamp_location));
	}

	public boolean update_resources(){
	
		Map<File, StampID> resource_stamps =load_stamps(config.resource_stamp_location);
		Map<File, StampID> placeholder_stamps =load_stamps(config.placeholder_stamp_location);
		
		for (File patch : config.repository_directory.listFiles(new FilenameFilter() {
	        @Override
	        public boolean accept(File dir, String name) {
	        	File loc = new File(dir.toString() + "\\" + name);
	        	if (loc.toString().contains(".git") || loc.isFile()) {
	        			return false;
	        	}
	        	return true;
	        }
	        })) {
			for (File repository_path : get_paths(patch)) {
				File relative_path = new File(repository_path.toPath().normalize().toString().replace(patch.toString(), ""));
				File resource_path = new File(config.resourcepack_directory.toPath().normalize().toString() + "\\" + relative_path);
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
							StampID info = new StampID(resource_path.lastModified(), resource_path.length());
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
			save_stamps(config.resource_stamp_location, resource_stamps);
			save_stamps(config.placeholder_stamp_location, placeholder_stamps);
	    } catch (IOException e) {
	    	e.printStackTrace();
	    	return false;
	    }
		
		update_metadata_recurse(config.resourcepack_directory, placeholder_stamps);

		del_empty(config.resourcepack_directory);
	    return true;
	}

	private boolean update_metadata_recurse(File directory, Map<File, StampID> placeholder_stamps){
		File untextured_loc = new File(directory.toString() + "\\" + "~untextured.txt");
		
		if(untextured_loc.exists()){
			untextured_loc.delete();
		}
		List<String> filenames = new ArrayList<String>();
	    for (File file : directory.listFiles()) {
	    	String relative_path = file.toString().replace(config.resourcepack_directory.toString(), "");
	        if (file.isDirectory()) {
	            update_metadata_recurse(file, placeholder_stamps);
	        } else if (placeholder_stamps.containsKey(new File(relative_path)) && relative_path.toString().endsWith(".png")) {
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
	
	private boolean extract_mod(File modpath, File output_dir){
		//Extract contents of jar:
		output_dir.mkdirs();
		try {
			JarFile jar = new JarFile(modpath);
			Enumeration<JarEntry> enumEntries = jar.entries();
			while (enumEntries.hasMoreElements()) {
			    JarEntry file = (java.util.jar.JarEntry) enumEntries.nextElement();
			    java.io.File f = new java.io.File(output_dir + File.separator + file.getName());
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
			    }
			}
			jar.close();
			return true;

		} catch (IOException e){
			e.printStackTrace();
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
	private boolean update_mcmodinfo(File extraction_dir) {
		HashMap<String, ArrayList<McmodInfo>> mod_info_listing = new HashMap<String, ArrayList<McmodInfo>>();
		
		if (config.mcmodinfo_location.exists()){
			try {
				ObjectInputStream s = new ObjectInputStream(new FileInputStream(config.mcmodinfo_location));
			    mod_info_listing = (HashMap<String, ArrayList<McmodInfo>>) s.readObject();
			    s.close();
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				return false;
			}
		}
		
		File domain_directory = new File(extraction_dir.toString() + "//assets//");
		if (domain_directory.exists()){
			
			if (new File(extraction_dir.toString() + "\\mcmod.info").exists()){
				//If mcmod and domains exist
				McmodInfo[] mod_info_array = new McmodInfo[0];
				try {
					FileInputStream text = new FileInputStream(new File(extraction_dir.toString() + "\\mcmod.info"));
					mod_info_array = gson.fromJson(IOUtils.toString(text), McmodInfo[].class);
					text.close();
				} catch (JsonSyntaxException | IOException e) {
				}
				
				for (File domain : domain_directory.listFiles()){
					
					ArrayList<McmodInfo> mod_info_list = new ArrayList<McmodInfo>(Arrays.asList(mod_info_array));
					
					if (mod_info_listing.containsKey(domain.getName())){
						mod_info_list.addAll(mod_info_listing.get(domain.getName()));
					}
					
					mod_info_listing.put(domain.getName(), mod_info_list);
				}
			} else {
				//If domains exist and mcmod.info does not exist
				ArrayList<McmodInfo> mod_info_list = new ArrayList<McmodInfo>();
				for (File domain : domain_directory.listFiles()){
					McmodInfo info = new McmodInfo();
					info.name = domain.getName();
					info.modid = domain.getName();
					mod_info_list.add(info);
					mod_info_listing.put(domain.getName(), mod_info_list);
				}
			}
		}
		// else: If mod does not have domains. Not supported.
		
		if (!config.mcmodinfo_location.exists()){
			config.mcmodinfo_location.getParentFile().mkdirs();
		}
		
		ObjectOutputStream oos;
		try {
			oos = new ObjectOutputStream(new FileOutputStream(config.mcmodinfo_location));
			oos.writeObject(mod_info_listing);
			oos.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private boolean remove_conflicts(final File directory, final Map<File, StampID> resource_stamps, final Map<File, StampID> placeholder_stamps){
		//Filter unneeded files out of extracted files:
	    List<File> non_image_files = get_paths(directory, new FilenameFilter() {
	        @Override
	        public boolean accept(File dir, String name) {
	        	
	        	//Delete all non graphics files
	        	if (!name.matches("([^\\s]+(\\.(?i)(png|mcmeta|info))$)")){
	        		return true;
	        	}
	        	//Delete all files that conflict with resources
	        	File subpath = new File(dir.toString().replace(directory.toString(), "") + "\\" + name);
				
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
		
		for (File file : non_image_files){
	    	file.delete();
	    }
		return true;
	}
	
	private boolean resize_resources(File directory){
		List<File> files = get_paths(directory);
		if (files != null){
			for (File file : files){
				if (file.toString().matches("([^\\s]+(\\.(?i)(png))$)")) {
	        		try {
	        			BufferedImage image = ImageIO.read(file);
	            		int width = image.getWidth(null);
	            		int height = image.getHeight(null);
	            		
	            		BufferedImage image_output = new BufferedImage(
	            				width * config.scalar, height * config.scalar, BufferedImage.TYPE_INT_ARGB);
	            		Graphics2D graphics2D = image_output.createGraphics();
	            		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
	            		graphics2D.drawImage(image, 0, 0, width * config.scalar, height * config.scalar, null);
						ImageIO.write(image_output, "PNG", file);
						graphics2D.dispose();
						
					} catch (NullPointerException | IOException e) {
					}
	        	}
			}
		}
		return true;
	}
}
