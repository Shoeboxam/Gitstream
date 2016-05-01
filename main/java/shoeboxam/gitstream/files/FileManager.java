package shoeboxam.gitstream.files;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import shoeboxam.gitstream.settings.StampID;
import shoeboxam.gitstream.settings.ConfigInstance;

public class FileManager {
	ConfigInstance config = ConfigInstance.get_data();

	protected static HashMap<File, StampID> load_stamps(File stamp_location){
		if (stamp_location.exists()){
			try {
				ObjectInputStream s = new ObjectInputStream(new FileInputStream(stamp_location));
			    @SuppressWarnings("unchecked")
			    HashMap<File, StampID> stamp_paths = (HashMap<File, StampID>) s.readObject();
			    s.close();
			    return stamp_paths;
			} catch (ClassNotFoundException | IOException e) {
				e.printStackTrace();
				return new HashMap<File, StampID>();
			}
		}
		return new HashMap<File, StampID>();
	}
	
	protected static void save_stamps(File stamp_location, Map<File, StampID> stamp_paths) throws IOException {
		if (!stamp_location.exists()){
			stamp_location.getParentFile().mkdirs();
		}
		ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(stamp_location));
		oos.writeObject(stamp_paths);
		oos.close();
	}
	
	protected static List<File> get_paths(File folder) {
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

	protected static List<File> get_paths(File folder, FilenameFilter filter) {
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

	protected Map<String, String> modname_bindings(){
		Map<String, String> mods_installed = new HashMap<String, String>();
		File[] patch_dirs = config.repository_directory.listFiles(new FilenameFilter() {
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

	protected Map<String, String> modname_bindings_fallback(){
		Map<String, String> mods_installed = new HashMap<String, String>();
		for (ModContainer mod : Loader.instance().getActiveModList()){
			// Modid -> modname
			mods_installed.put(mod.getModId(), mod.getName());
		}
		return mods_installed;
	}

	protected Map<String, String> modname_bindings_masked(){
		Map<String, String> mods_installed = new HashMap<String, String>();
		File[] patch_dirs = config.repository_directory.listFiles(new FilenameFilter() {
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

	public static void del_empty(File directory) {
		if (directory.isDirectory() && !directory.getName().equals(".git")){

			for (File item : directory.listFiles()){
				del_empty(item);
			}
			File[] contents = directory.listFiles();
			if (contents.length == 0){
				directory.delete();
			}
			
			boolean has_graphics = false;
			for (File item : contents){
				if (item.getName().contains(".png")){
					has_graphics = true;
				}
			}
			if (!has_graphics){
				for (File item : contents){
					if (!item.getName().equals("mod.json")){
						item.delete();
					}
				}
				directory.delete();
			}
		}
	}
}
