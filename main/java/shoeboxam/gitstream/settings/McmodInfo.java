package shoeboxam.gitstream.settings;

import java.util.ArrayList;
import java.util.List;

public class McmodInfo {
	public String modid;
	public String name;
	public String version;
	public String description;
	public String credits;
	public String logoFile;
	public String url;
	public String updateUrl;
	public List<String> authors = new ArrayList<String>();
	public String parent;
	public List<Object> screenshots = new ArrayList<Object>();
	public List<Object> dependencies = new ArrayList<Object>();
}