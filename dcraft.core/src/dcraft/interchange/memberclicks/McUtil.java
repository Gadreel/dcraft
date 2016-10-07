package dcraft.interchange.memberclicks;

import java.io.DataOutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;

import dcraft.interchange.mws.Util;
import dcraft.lang.op.FuncResult;
import dcraft.lang.op.OperationContext;
import dcraft.struct.CompositeParser;
import dcraft.struct.CompositeStruct;
import dcraft.struct.FieldStruct;
import dcraft.struct.RecordStruct;
import dcraft.util.StringUtil;
import dcraft.xml.XElement;
/*
Accept: application/xml
Accept: application/json

Below is a quick description of how to use MemberClicks RESTFUL API

To get an authorization token, use a RESTFUL POST to the following link with the following body variables:
    Link:
        orgid.memberclicks.net/services/auth  (use your orgid in place of the text orgid)
    Body Variables (please note capitalization is important here):
        apiKey=1111111111 (replace with your API Key)                     
        username=myusername (replace with your user name, or leave off for public level access)
        password=mypassword (replace with your password, or leave off for public level access)

The token will be included in the response.

Example: curl -d "apiKey=2406471784&username=demouser&password=demopass" https://demo.memberclicks.net/services/auth
        
After you have the token, add it to your header:
    Header:
        Authorization=8874270a-d3b2-4387-9ffd-00ebf824583c (This is not a real token, replace with the auth token you receive from the previous steps)        
        
Once this token is added to your header, you can use Get and Put functions to the API's links to update and query your data.
    API services template:
        orgid.memberclicks.net/services/(service)/(method or id)

Example: curl --header "Authorization:8874270a-d3b2-4387-9ffd-00ebf824583c" https://demo.memberclicks.net/services/user/21838877
 * 					
 */
public class McUtil {
	
	/*
	 * 
		Response Code : 200
		Response Data: {
			"userId":"11111111",
			"groupId":"333333",
			"orgId":"aaaa",
			"contactName":"Stan Wise",
			"userName":"StanW",
			"active":"true",
			"validated":"true",
			"deleted":"false",
			"formStatus":"0",
			"lastModify":"2012-09-18T11:46:59-04:00",
			"noMassEmail":"false",
			"prefBBContact":"",
			"prefBBImage":"",
			"token":"ea99029d-f7d7-93df-9211-6bd747e43d4b",
			"password":"nnnnn"
		}	
	*/	
	static public String signIn(String org, String apikey, String username, String password) {
		RecordStruct res = (RecordStruct) McUtil.call(org, null, "auth", "POST", new RecordStruct()
			.withField("apiKey", apikey)
			.withField("username", username)
			.withField("password", password)
		);
		
		if ((res == null) || (res.isFieldEmpty("token"))) {
			OperationContext.get().error("MemberClicks sign in failed.");
			return null;
		}
		
		return res.getFieldAsString("token");
	}
	
	/*
	{ 
		"group":  [  
			{ 
				"groupType": "2", 
				"groupName": "Admin",
				"ghostUser": "false", 
				"groupID": "134524", 
				"specialGroup": "false", 
				"bypassFormLogin": "false" 
			} ,  
			{ 
				"groupType": "1", 
				"groupName": "Member", 
				"ghostUser": "false", 
				"groupID": "136037", 
				"specialGroup": "false", 
				"bypassFormLogin": "false" 
			} 
		]  
	} 
	*/
	
	static public RecordStruct listGroups(String org, String token) {
		return (RecordStruct) McUtil.call(org, token, "group", "GET", null);
	}

	/*
	{ 
		"groupType": "1", 
		"groupName": "Newsletter Group", 
		"ghostUser": "false", 
		"groupID": "150297", 
		"specialGroup": "false", 
		"orgId": "aaaa", 
		"bypassFormLogin": "false" 
	} 
	*/
	
	static public RecordStruct getGroupDetail(String org, String token, String id) {
		return (RecordStruct) McUtil.call(org, token, "group/" + id, "GET", null);
	}
	
	/*
		{ 
			"user":  [  
				{ 
					"lastName": "Jones", 
					"lastModify": "2015-10-16T11:42:10-04:00", 
					"contactName": "Steve Jones", 
					"groupId": "136037", 
					"active": "true", 
					"noMassEmail": "false", 
					"userName": "yvette@designcraftadvertising.com", 
					"userId": "22173604", 
					"orgId": "nari", 
					"firstName": "Steve", 
					"password": "freetext", 
					"deleted": "false", 
					"prefBBContact": "", 
					"validated": "true", 
					"prefBBImage": "", 
					"formStatus": "0" 
				}  
			]  
		} 
	 * 
	 */
	static public RecordStruct listUsers(String org, String token) {
		return (RecordStruct) McUtil.call(org, token, "user", "GET", null);
	}
	
	/*
		{ 
			"lastModify": "2015-10-16T11:42:10-04:00", 
			"contactName": "Steve Jones", 
			"groupId": "136037", 
			"active": "true", 
			"noMassEmail": "false", 
			"userName": "", 
			"userId": "22173604", 
			"orgId": "aaaaaa", 
			"password": "", 
			"deleted": "false", 
			"prefBBContact": "", 
			"validated": "true", 
			"prefBBImage": "", 
			"formStatus": "0" 
		} 
	 * 
	 * 
		{ 
			"lastName": "Jones", 
			"lastModify": "2015-10-16T11:42:10-04:00", 
			"contactName": "Yvette Jones", 
			"groupId": "136037", 
			"active": "true", 
			"noMassEmail": "false", 
			"userName": "yvette@designcraftadvertising.com", 
			"userId": "22173604", 
			"orgId": "nari", 
			"firstName": "Yvette", 
			"password": "dcalaugh1809", 
			"deleted": "false", 
			"prefBBContact": "", 
			"validated": "true", 
			"prefBBImage": "", 
			"formStatus": "0", 
			"attribute":  [  
				{ "attId": "496045", "hidden": "false", "lastModify": "2012-06-29T09:06:10-04:00", "viewOnly": "true", "editable": "true", "attName": "Company", "attData": "designCraft Advertising LLC", "attTypeId": "2", "userId": "22173604", "attTypeName": "First Name" } ,  
				{ "attId": "490553", "hidden": "false", "lastModify": "2012-06-29T09:06:10-04:00", "viewOnly": "true", "editable": "true", "attName": "Contact Name", "attData": "Steve Jones", "attTypeId": "16", "userId": "22173604", "attTypeName": "Contact Center Greeting" } ,  
				{ "attId": "496046", "hidden": "false", "lastModify": "2012-06-29T09:06:10-04:00", "viewOnly": "true", "editable": "true", "attName": "First Name", "attData": "Steve", "attTypeId": "2", "userId": "22173604", "attTypeName": "First Name" } ,  
				{ "attId": "496047", "hidden": "false", "lastModify": "2012-06-29T09:06:10-04:00", "viewOnly": "true", "editable": "true", "attName": "Last Name", "attData": "Jones", "attTypeId": "3", "userId": "22173604", "attTypeName": "Last Name" } ,  
				{ "attId": "496049", "hidden": "false", "lastModify": "2012-06-29T09:06:10-04:00", "viewOnly": "true", "editable": "true", "attName": "Address 1", "attData": "707 S Park St", "attTypeId": "28", "userId": "22173604", "attTypeName": "Address Line 1" } ,  
			]  
		} 
	 * 
	 */
	static public RecordStruct getUserDetail(String org, String token, String id) {
		return (RecordStruct) McUtil.call(org, token, "user/" + id + "?includeAtts=true", "GET", null);
	}

	
	static public CompositeStruct call(String org, String token, String service, String meth, RecordStruct params) {
		try {
			String parameters = "";
	        boolean hasparam = false;
	        
			if (params != null) {
		        for (FieldStruct fld : params.getFields()) {
		        	if (fld.isEmpty())
		        		continue;
		        	
		        	if (!hasparam)
		        		hasparam = true;
		        	else
		        		parameters += "&";
		        	
		        	parameters += Util.urlEncode(fld.getName(), false) + "=" + Util.urlEncode(fld.getValue().toString(), false);
		        }
			}
			
			URL url = (hasparam && "GET".equals(meth))
				? new URL("https://" + org + ".memberclicks.net/services/" + service + "?" + parameters)
				: new URL("https://" + org + ".memberclicks.net/services/" + service);
			
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
	 
			con.setRequestMethod(meth);
			con.setRequestProperty("User-Agent", "DivConq/1.0 (Language=Java/8)");
			con.setRequestProperty("Accept", "application/json");
			
			if (StringUtil.isNotEmpty(token))
				con.setRequestProperty("Authorization", token);
	 
			// Send post request
			if ("POST".equals(meth) || "PUT".equals(meth)) {
				con.setDoOutput(true);
				DataOutputStream wr = new DataOutputStream(con.getOutputStream());
				wr.writeBytes(parameters);
				wr.flush();
				wr.close();
			}
			
			int responseCode = con.getResponseCode();
			
			if (responseCode != 200) {
				OperationContext.get().error("MemberClicks call failed, code: " + responseCode);
				return null;
			}
	        
	        FuncResult<CompositeStruct> h = CompositeParser.parseJson(con.getInputStream());
	        
	        if (h.isNotEmptyResult()) 
	        	return h.getResult();
	        
			OperationContext.get().error("MemberClicks call failed, JSON incomplete or missing.");
		}
		catch (Exception x) {
			
		}
		
		/*
		File dir = new File("./public/dcw/nari/files/mc-update-queue");
		
		File[] filesList = dir.listFiles();
		
		for (File file : filesList) {
		    if (file.isFile() && file.getName().endsWith(".csv")) {
		        System.out.println(file.getName());
		    }
		}
		*/
		
		// CSV 
		//IOUtil.readEntireFile(file);

		return null;
	}
	
	static public void callXml(String org, String token, String service, XElement data) {
		try {
			URL url = new URL("https://" + org + ".memberclicks.net/services/" + service);
			
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
	 
			con.setRequestMethod("PUT");
			con.setRequestProperty("User-Agent", "DivConq/1.0 (Language=Java/8)");
			con.setRequestProperty("Content-Type", "application/xml; charset=\"utf-8\"");
			con.setRequestProperty("Accept", "text/xml");
			
			if (StringUtil.isNotEmpty(token))
				con.setRequestProperty("Authorization", token);
	 
			// Send post request
			con.setDoOutput(true);
			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(data.toString(true));
			wr.flush();
			wr.close();
			
			int responseCode = con.getResponseCode();
			
			if (responseCode != 200) 
				OperationContext.get().error("MemberClicks call failed, code: " + responseCode);
		}
		catch (Exception x) {
			OperationContext.get().error("MemberClicks callXml failed, error: " + x);
		}
	}

	public static void updateUserAttr(String org, String token, String userid, String attrid, String value) {
		XElement xml = new XElement("userAttribute")
				.with(new XElement("userId").withText(userid))
				.with(new XElement("attId").withText(attrid))
				.with(new XElement("attData").withText(value));
		
		McUtil.callXml(org, token, "user/" + userid + "/attribute/" + attrid, xml);
	}

	public static XElement buildUser(String userid, String groupid, boolean active, HashMap<String,String> attrs) {
		XElement xml = new XElement("user")
				.with(new XElement("userId").withText(userid))
				.with(new XElement("groupId").withText(groupid))
				.with(new XElement("active").withText(active ? "true" : "false"))
				.with(new XElement("validated").withText("true"))
				.with(new XElement("deleted").withText("false"));
		
		if (attrs != null) {
			for (Entry<String, String> ent : attrs.entrySet()) {
				xml.with(
					new XElement("attribute")
						.with(new XElement("attId").withText(ent.getKey()))
						.with(new XElement("attData").withText(ent.getValue()))
				);
			}
		}
		
		return xml;
	}

	public static void updateUser(String org, String token, String userid, String groupid, boolean active, HashMap<String,String> attrs) {
		XElement xml = McUtil.buildUser(userid, groupid, active, attrs);
		
		McUtil.callXml(org, token, "user/" + userid, xml);
	}
}
