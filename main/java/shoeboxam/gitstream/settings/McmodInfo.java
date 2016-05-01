package shoeboxam.gitstream.settings;

import java.io.Serializable;
import java.util.ArrayList;

public class McmodInfo implements Serializable {
	private static final long serialVersionUID = 2L;
	
	public String modid = "";
	public String name = "";
	public String version = "";
	public String description = "";
	public String credits = "";
	public String logoFile = "";
	public String url = "";
	public String updateUrl = "";
	public transient ArrayList<String> authors = new ArrayList<String>();
	public String parent = "";
	public transient ArrayList<Object> screenshots = new ArrayList<Object>();
	public transient ArrayList<Object> dependencies = new ArrayList<Object>();
}