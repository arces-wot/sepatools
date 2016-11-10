package arces.unibo.KPI;


import java.io.*;
import java.net.*;
import java.util.*;

public class ArcesServiceRegistry
{
    private String registry_url="";
	
	public ArcesServiceRegistry(String serviceURL)
	{
		this.registry_url=serviceURL;
	}//public serviceRegistry()

	
	
	/**
	 * Search for all service registered on the service registry.
	 * @return Return an array of Properties. Each property contains all the available 
	 * parameters related to the service. The array could be include zero, one or more 
	 * that one property object, it depends on the registry content 
	 *  
	 */
	
	public Vector<Properties> search()
	{
	 return this.search(null);	
	}//public Properties search()

	
	/**
	 * 
	 * Search for all service registered on the service registry that match the parameters value passed.
	 * 
     * @param serviceProperties the service properties (e.g. p1=a, p2=b, kk=hello) 
	 * @return Return an array of Properties. Each property contains all the available 
	 * parameters related to the service. The array could be include zero, one or more 
	 * that one property object, it depends on the registry content.
	 * 
	 *  
	 */
	
	public Vector<Properties> search(Properties serviceProperties)
	{String GETCommand="", ret="";
     Vector<Properties> vSearchResult=new Vector<Properties>();
     
	
		if(serviceProperties!=null && !serviceProperties.equals(""))
		{
			GETCommand="";
			
		    Enumeration<?> e = serviceProperties.propertyNames();

		    while (e.hasMoreElements()) 
		    {
		      String key = (String) e.nextElement();
		      GETCommand+= key+"="+serviceProperties.getProperty(key)+"&";
		    }
		    
		}//if(serviceProperties!=null)
		
		ret=this.readURL(this.registry_url+"/search.php?"+GETCommand);
		
		if(ret==null || ret.equals("")) return vSearchResult;
		
		//From registry-string-format to properties:
	    String line[]=ret.split("\\n");
	    if(line==null || line.length==0)return vSearchResult;  //service not found

	    for(int s=0;s<line.length;s++)
	    {String serviceInfo[]=line[s].split("\\|");
	     Properties serviceFoundProperties=new Properties();
	     
	    	for(int p=0;p<serviceInfo.length;p++)
	    	{ String param[]=serviceInfo[p].split("=");  int name=0, value=1;
	    	  serviceFoundProperties.setProperty(param[name], param[value]);
	    	}//for(int p=0;p<serviceInfo.length;p++)
	    	
    	 if(serviceFoundProperties.size()>0)vSearchResult.add(serviceFoundProperties);
    	 
    	}//for(int s=0;s<line.length;s++)

		
		return vSearchResult;	
	}//public Properties search()
	
	
	
	/**
	 * Get a web page content by passing an url 
	 * @param url the url to invoke
	 * @return the web page content
	 */
	
    private String readURL(String url)
    {
 	   URL myuri;
		   try { myuri = new URL(url);	}
		   catch (MalformedURLException e) {e.printStackTrace();return null;}
 	
        BufferedReader in;
		   try   { in = new BufferedReader( new InputStreamReader( myuri.openStream())); }
		   catch (IOException e) { e.printStackTrace(); return null; }

 	   String inputLine;
 	   String acc="";

 	   try {
			
 		   while ((inputLine = in.readLine()) != null)
			   //System.out.println(inputLine);
 			   acc+=inputLine+"\n";
			
			   in.close();
			
		    } catch (IOException e) { e.printStackTrace(); return null;}

 	   return acc;
    }//private String readURL(String url)

    
    /**
	 * 
	 * Add a service to the register 
	 * 
     * @param serviceProperties the service properties (e.g. p1=a, p2=b, kk=hello) that describe the service 
	 * @return Return the service response. It should be one of the following: 
	 * 
	 * ERROR:Service NO_SERVICE_01 is already registered!ERROR:Please check!
	 * ERROR:The "sid", service ID, is need
	 * ERROR:At least one parameter is need! Use "sid" the service ID.
	 * OK:Done!
	 * 
	 * Return null in case of unplanned error
	 * 
	 */
	
	public String add(Properties serviceProperties)
	{String GETCommand="", ret="";     
	
		if(serviceProperties!=null && !serviceProperties.equals(""))
		{
			GETCommand="";
			
		    Enumeration<?> e = serviceProperties.propertyNames();

		    while (e.hasMoreElements()) 
		    {
		      String key = (String) e.nextElement();
		      GETCommand+= key+"="+serviceProperties.getProperty(key)+"&";
		    }
		    
		}//if(serviceProperties!=null)
		
		ret=this.readURL(this.registry_url+"/add.php?"+GETCommand);
		
		if(ret==null || ret.equals("")) return null;

		return ret;
	}//public Properties search()
	
    
    /**
	 * 
	 * Remove a service from the register 
	 * 
     * @param serviceProperties the service properties (e.g. p1=a, p2=b, kk=hello) that describe the service 
	 * @return Return the service response. It should be one of the following: 
	 * 
	 * ERROR:The "sid", service ID, parameter is needed!
	 * ERROR:The service "NO_SERVICE_01" is not registered yet!
	 * OK:Done!
	 * 
	 * Return null in case of unplanned error
	 * 
	 */
	
	public String remove(Properties serviceProperties)
	{String GETCommand="", ret="";     
	
		if(serviceProperties!=null && !serviceProperties.equals(""))
		{
			GETCommand="";
			
		    Enumeration<?> e = serviceProperties.propertyNames();

		    while (e.hasMoreElements()) 
		    {
		      String key = (String) e.nextElement();
		      GETCommand+= key+"="+serviceProperties.getProperty(key)+"&";
		    }
		    
		}//if(serviceProperties!=null)
		
		ret=this.readURL(this.registry_url+"/remove.php?"+GETCommand);
		
		if(ret==null || ret.equals("")) return null;

		return ret;
	}//public Properties search()
	
	
	
}//public class serviceRegistry