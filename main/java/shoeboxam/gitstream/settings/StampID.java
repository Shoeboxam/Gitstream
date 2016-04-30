package shoeboxam.gitstream.settings;

import java.io.Serializable;

public class StampID implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public StampID(Long timestamp_set, Long size_set){
		timestamp = timestamp_set;
		size = size_set;
	}
	public Long timestamp;
	public Long size;
}
