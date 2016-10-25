/* ************************************************************************
#
#  designCraft.io
#
#  http://designcraft.io/
#
#  Copyright:
#    Copyright 2014 eTimeline, LLC. All rights reserved.
#
#  License:
#    See the license.txt file in the project's top-level directory for details.
#
#  Authors:
#    * Andy White
#
************************************************************************ */
package dcraft.net;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.googlecode.ipv6.IPv6Address;

import dcraft.lang.op.OperationContext;

public class NetUtil {
    static public String urlEncodeUTF8(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } 
        catch (UnsupportedEncodingException e) {
            // NA
        }
        
        return null;
    }
    
    static public String urlEncodeUTF8(Map<String,String> map) {
        StringBuilder sb = new StringBuilder();
        
        for (Map.Entry<String,String> entry : map.entrySet()) {
            if (sb.length() > 0) 
                sb.append("&");
            
            sb.append(urlEncodeUTF8(entry.getKey()) + "=" + urlEncodeUTF8(entry.getValue()));
        }
        
        return sb.toString();       
    }
    
    static public String formatIpAddress(InetSocketAddress addr) {
    	if (addr.getAddress() instanceof Inet4Address)
    		return addr.getHostString();
    	
		if (addr.getAddress() instanceof Inet6Address) {
			IPv6Address got = IPv6Address.fromInetAddress(addr.getAddress());
			
			return got.toString();
		}
		
		return null;
    }
    
	public static boolean download(String address, String localFileName) {
		boolean ret = false;
		OutputStream out = null;
		URLConnection conn = null;
		InputStream  in = null;
		
		try {
			URL url = new URL(address);
			conn = url.openConnection();
			in = conn.getInputStream();
			
			File f = new File(localFileName);
			
			Files.createDirectories(Paths.get(localFileName).getParent());

			out = new BufferedOutputStream(new FileOutputStream(f));
			byte[] buffer = new byte[1024];
			int numRead;
			//long numWritten = 0;
			while ((numRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, numRead);
				//numWritten += numRead;
			}
			ret = true;
		} 
		catch (Exception x) {
			OperationContext.get().error("Unable to download file. " + x);
		} 
		finally {
			try {
				if (in != null) 
					in.close();
				
			} 
			catch (IOException x) {
				OperationContext.get().error("Unable to close input stream. " + x);
			}
			
			try {
				if (out != null) {
					out.close();
				}
			} 
			catch (IOException x) {
				OperationContext.get().error("Unable to close downloaded file. " + x);
			}
		}
		return ret;
	}    
}
