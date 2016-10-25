package dcraft.interchange.instagram;

import java.net.URL;

import dcraft.lang.op.FuncResult;
import dcraft.struct.CompositeParser;
import dcraft.struct.CompositeStruct;
import dcraft.struct.ListStruct;
import dcraft.struct.RecordStruct;

public class User {
	static public ListStruct mediaFeed(String token) {
		try {
			URL url = new URL("https://api.instagram.com/v1/users/self/media/recent/?access_token=" + token);
			
			FuncResult<CompositeStruct> res = CompositeParser.parseJson(url);
			
			if (res.isEmptyResult()) 
				return null;
			
			return ((RecordStruct) res.getResult()).getFieldAsList("data");
		}
		catch (Exception x) {
			// TODO error handling?
		}
		
		return null;
	}
}
