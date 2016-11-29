/**
 * SSAP_XMLTools is the class to handle the SIB messages. 
 * <p>
 * It store internally the last XML message received 
 * from the SIB for XML parsing purpose.
 * It maintain a transaction counter 
 * <p>
 * @author      Daniele Manzaroli - VTT-Oulu, Technical Research Centre of Finland. ARCES, University of Bologna, Italy
 * @version     %I%, %G%
 */


package arces.unibo.KPI;

import java.io.ByteArrayInputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.jdom2.*;
import org.jdom2.input.SAXBuilder;

public class SSAP_XMLTools
{

	org.jdom2.input.SAXBuilder builder = null;
	Document messageDoc = null;

	/**
	 * SSAP specification protocol element
	 */
	public  String nodeID="00000000-0000-0000-0000-DEAD0000BEEF";
	private int    transaction_id =0;
	private int    subscription_id=0;

	public static final String ANYURI="http://www.nokia.com/NRC/M3/sib#any";
	private String SMART_SPACE_NAME;

	/**
	 * Object constructor.
	 * To be able to use just the XML parser methods, all param can be null.
	 * 
	 * @param nodeID the KPI node ID
	 * @param SMART_SPACE_NAME the smart space name
	 * @param ANYURI the URI you are going to use in the SIB
	 * 
	 */

	public SSAP_XMLTools(String nodeID, String SMART_SPACE_NAME, String ANYURI) 
	{
		builder = new SAXBuilder();
		this.nodeID           =nodeID;
		this.SMART_SPACE_NAME =SMART_SPACE_NAME;


	}//public KPXML(String HOST,int PORT,String SMART_SPACE_NAME)

	/**
	 * Object constructor.
	 * To be able to use just the XML parser methods
	 * 
	 */

	public SSAP_XMLTools() 
	{
		builder = new SAXBuilder();
		this.nodeID           =null;
		this.SMART_SPACE_NAME =null;


	}

	/**
	 * Collect all the values of each xml tag id specified
	 * into an hashtable looking for into a string representation 
	 * of an xml document. All id passed are not mandatory 
	 * to be found in this version.
	 * 
	 * @param xml the xml documento to parse
	 * @param id array of all xml tag to find into the document 
	 * @return an hashtable containing all the couple id-value
	 */
	public Hashtable<String,String> SibXMLMessageParser(String xml, String id[])
	{   
		if(xml==null)
		{
			System.out.println("ERROR:SSAP_XMLTools:SibXMLMessageParser: XML message is null")
			;return null;
		}
		if(id==null)
		{
			System.out.println("ERROR:SSAP_XMLTools:SibXMLMessageParser:id is null");
			return null;
		}

		Document doc=null;
		try {
			//System.out.println("################" + xml);
			doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		if(doc==null)
		{
			System.out.println("ERROR:SSAP_XMLTools:SibXMLMessageParser:doc is null");
			return null;
		}

		Hashtable<String,String> hashtable = new Hashtable<String,String>();

		Element root = doc.getRootElement();
		Namespace ns = root.getNamespace();

		//String Comm = root.getChild("Command", ns ).getText();
		//Element rootActivity = root.getChild("Activity", ns);	

		for(int i=0; i< id.length; i++)
		{   

			hashtable.put(id[i],root.getChild(id[i], ns ).getText() );

		}//for(int i=0; i< e.size(); i++)

		return hashtable;
	}//public Hashtable SibXMLMessageParser(String xml, String id[])


	/**
	 * Check if every element of the id array have into the last SIB 
	 * XML message a corresponding value from the ref array. 
	 *  
	 * @param id List of SIB XML message tag to check 
	 * @param ref references values for each tag id 
	 * @return true if all the values into the last SIB XML message match 
	 *          with the values in the ref array, false otherwise.
	 *          Each id is mandatory, so if the size of hashtable is
	 *          less then id array, it will return false.
	 */
	public boolean autoCheckSibMessage(String xml,String id[],String ref[])
	{ 
		Hashtable<String,String> hashtable = SibXMLMessageParser(xml,id);

		if(hashtable==null)return false;

		if(hashtable.size() < id.length) return false;

		for(int i=0; i<hashtable.size();i++)
			if(!ref[i].equals(hashtable.get(id[i]))) return false;

	return true;
	}//public boolean autoCheckSibMessage(String id[],String ref[])



	/**
	 * By specifying an xml attribute and value, the xml content
	 * will be return as Element object
	 * 
	 * @param xml the xml SIB message
	 * @param attribute the attribute you are looking for
	 * @param value the value of the attribute 
	 * @return the Element object that contain the attribute xml content
	 */

	public Element getParameterElement(String xml,String attribute, String value)
	{
		if(xml==null){System.out.println("ERROR:SSAP_XMLTools:getParameterElement: XML message is null");return null;}

		Document doc=null;
		try {
			doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		if(doc==null) {System.out.println("ERROR:SSAP_XMLTools:getParameterElement:doc is null");return null;}

		Element root = doc.getRootElement();
		Namespace ns = root.getNamespace();


		List<Element> parameters = root.getChildren("parameter", ns);
		int ip;
		for(ip=0;ip<parameters.size();ip++)
			if( parameters.get(ip).getAttributeValue( attribute ).equals( value ))break;

		if(!(ip<parameters.size())) {System.out.println("ERROR:SSAP_XMLTools:getParameterElement:parameter("
				+attribute+"="+value+") not found!");return null;}

		return parameters.get(ip);
	}
//FG add is

	/**
	 * Looking for the join confirmation in the last XML message 
	 * received from the SIB 
	 * 
	 * @return true if the confirmation is found, false otherwise
	 */
	public boolean isJoinConfirmed(String xml)
	{   String id[]={"transaction_type","message_type"};
	String ref[]={"JOIN","CONFIRM"};
	return    autoCheckSibMessage(xml,id,ref)  
			&& getParameterElement(xml,"name", "status").getValue().equals("m3:Success");
	}//public boolean isJoinConfirmed()


	/**
	 * Looking for the leave confirmation in the last XML message 
	 * received from the SIB 
	 * 
	 * @return true if the confirmation is found, false otherwise
	 */
	public boolean isLeaveConfirmed(String xml)
	{   String id[]={"transaction_type","message_type"};
	String ref[]={"LEAVE","CONFIRM"};
	return autoCheckSibMessage(xml,id,ref)  
			&& getParameterElement(xml,"name", "status").getValue().equals("m3:Success");
	}//public boolean isLeftConfirmed()


	/**
	 * Looking for the Query confirmation in the last XML message 
	 * received from the SIB 
	 * 
	 * @return true if the confirmation is found, false otherwise
	 */
	public boolean isQueryConfirmed(String xml)
	{   String id[]={"transaction_type","message_type"};
	String ref[]={"QUERY","CONFIRM"};
	return autoCheckSibMessage(xml,id,ref)  
			&& getParameterElement(xml,"name", "status").getValue().equals("m3:Success");		
	}//public boolean isLeftConfirmed()


	/**
	 * Looking for the Update confirmation in the last XML message 
	 * received from the SIB 
	 * 
	 * @return true if the confirmation is found, false otherwise
	 */
	public boolean isUpdateConfirmed(String xml)
	{   String id[]={"transaction_type","message_type"};
	String ref[]={"UPDATE","CONFIRM"};
	return autoCheckSibMessage(xml,id,ref)  
			&& getParameterElement(xml,"name", "status").getValue().equals("m3:Success");		
	}//public boolean isLeftConfirmed()



	/**
	 * Get all the triple as list of strings from a RDF query answer message 
	 * 
	 * @param xml the SIB xml message
	 * @return a list of all triples available in the xml SIB message passed.
	 * It is a structure to store every triple. Each element of 
	 * the vector contains another vector formed by four string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the subject type
	 * -the object type
	 * 
	 */
	public Vector<Vector<String>> getQueryTriple(String xml)
	{
		if(xml==null)
		{
			System.out.println("ERROR:SSAP_XMLTools:getQueryTriple:XML message is null");
			return null;
		}

		Document doc=null;
		try {
			doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		if(doc==null)
		{
			System.out.println("ERROR:SSAP_XMLTools:getQueryTriple:doc is null");
			return null;
		}

		Vector<Vector<String>> triple=new Vector<Vector<String>>();

		Element root = doc.getRootElement();
		Namespace ns = root.getNamespace();

		List<Element> parameters = root.getChildren("parameter", ns);
		int ip;
		for(ip=0;ip<parameters.size();ip++)
			if( parameters.get(ip).getAttributeValue("name").equals("results"))break;

		if(!(ip<parameters.size())) {System.out.println("ERROR:SSAP_XMLTools:getQueryTriple:parameter(results) not found!");return null;}

		Element triple_list = parameters.get(ip).getChild("triple_list", ns);

		if(triple_list==null) {System.out.println("ERROR:SSAP_XMLTools:getQueryTriple:triple_list not found");return null;}

		List<Element> triples =  triple_list.getChildren("triple", ns);
		Iterator<Element> i = triples.iterator();

		while(i.hasNext())
		{   Vector<String> singleton=new Vector<String>();

		Element etriple = i.next();
		singleton.add(etriple.getChild("subject", ns).getText());
		singleton.add(etriple.getChild("predicate", ns).getText());
		singleton.add(etriple.getChild("object", ns).getText());
		singleton.add(etriple.getChild("object", ns).getAttributeValue("type"));

		triple.add(singleton);			
		}//while(i.hasNext())

		return triple;
	}//public Vector<Vector<String>> getQueryTriple()


	/**
	 * Get the first occurence of the Object in a triple by specify the Predicate value
	 * 
	 * @param triple_list a list of triple string representation.
	 * It is a structure to store every triple. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the subject type
	 * -the object type 
	 * 
	 * @param predicate xml SIB message triple predicate
	 * @return the string representation of the object
	 */
	public String getTripleObjectByPredicate( Vector<Vector<String>> triple_list, String predicate) 
	{
		for(int i=0; i<triple_list.size() ;i++)
		{ Vector<String> t = triple_list.get(i);
		if( t != null ) 
			if( t.get(1)!=null && ((String)t.get(1)).equals(predicate))
			{return t.get(2);}

		}
		return null;
	}//public String getTripleObjectByPredicate( triple, uris.daysURI) )



	/**
	 * Get the index of the first occurrence of the triple in a triple list 
	 * by specify the Predicate value
	 *  
	 * @param tripleVector a list of triple string representation.
	 * It is a structure to store every triple. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the subject type
	 * -the object type 
	 * 
	 * @param predicate xml SIB message triple predicate
	 * @return the index of the first predicate occurrence in the triple list 
	 */
	public int getTripleIndexByPredicate( Vector<Vector<String>> tripleVector, String predicate) 
	{
		for(int i=0; i<tripleVector.size() ;i++)
		{ Vector<String> t = tripleVector.get(i);
		if( t != null ) 
			if( t.get(1)!=null && ((String)t.get(1)).equals(predicate))
			{return i;}

		}
		return -1;
	}//public String getTripleObjectByPredicate( triple, uris.daysURI) )


	/**
	 * Get the new triple value from an event SIB message 
	 * 
	 * @param xml the SIB xml message
	 * @return a list of the string representation of every triple available 
	 * into the event SIB xml message.
	 * It is a structure to store every triple. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the object type 
	 *
	 */
	public Vector<Vector<String>> getNewResultEventTriple(String xml)
	{return getEventTriple(xml,"new_results");}

	/**
	 * Get the obsolete triple value from an event SIB message 
	 * 
	 * @param xml the SIB xml message
	 * @return a list of the string representation of every triple available
	 *  into the event SIB xml message.
	 * It is a structure to store every triple. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the object type 
	 *
	 */
	public Vector<Vector<String>> getObsoleteResultEventTriple(String xml)
	{return getEventTriple(xml,"obsolete_results");}

	/**
	 *  Get the triple value from an event SIB message by specifying
	 *  the parameter attribute value
	 *  
	 * @param xml the SIB xml message
	 * @param ParamAttValue parameter attribute value like "obsolete_results" or "new_results"
	 * @return a list of the string representation of every triple available
	 *  into the event SIB xml message.
	 * It is a structure to store every triple. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the object type 
	 *
	 */
	private Vector<Vector<String>> getEventTriple(String xml,String ParamAttValue)
	{
		if(xml==null){System.out.println("ERROR:SSAP_XMLTools:getEventTriple: XML message is null");return null;}

		Element parameters = getParameterElement(xml,"name", ParamAttValue);
		if(parameters==null) {System.out.println("ERROR:SSAP_XMLTools:getEventTriple:parameters not found:"+ParamAttValue);return null;}

		Element triple_list = parameters.getChild("triple_list");
		if(triple_list==null) {System.out.println("ERROR:SSAP_XMLTools:getEventTriple:triple_list not found");return null;}

		Vector<Vector<String>> triple=new Vector<Vector<String>>();

		List<Element> triples =  triple_list.getChildren("triple");
		Iterator<Element> i = triples.iterator();

		while(i.hasNext())
		{   Vector<String> singleton=new Vector<String>();

		Element etriple = i.next();
		singleton.add(etriple.getChild("subject").getText());
		singleton.add(etriple.getChild("predicate").getText());
		singleton.add(etriple.getChild("object").getText());
		singleton.add(etriple.getChild("object").getAttributeValue("type"));

		triple.add(singleton);			
		}//while(i.hasNext())

		return triple;
	}//public Vector<Vector<String>> getQueryTriple()


	/**
	 * Looking for the Subscription confirmation in the last XML message 
	 * received from the SIB 
	 * 
	 * @param xml the SIB xml message
	 * 
	 * @return true if the confirmation is found, false otherwise
	 */
	public boolean isSubscriptionConfirmed(String xml)
	{   String id[]={"transaction_type","message_type"};
	String ref[]={"SUBSCRIBE","CONFIRM"};
	return autoCheckSibMessage(xml,id,ref)  
			&& getParameterElement(xml,"name", "status").getValue().equals("m3:Success");		
	}//public boolean isLeftConfirmed()


	/**
	 * Looking for the UnSubscription confirmation in the last XML message 
	 * received from the SIB 
	 *
	 * @param xml the SIB xml message
	 * @return true if the confirmation is found, false otherwise
	 */
	public boolean isUnSubscriptionConfirmed(String xml)
	{   String id[]={"transaction_type","message_type"};
	String ref[]={"UNSUBSCRIBE","CONFIRM"};

	return autoCheckSibMessage(xml,id,ref)  
			&& getParameterElement(xml,"name", "status").getValue().equals("m3:Success");		
	}//public boolean isLeftConfirmed()


	/** FG
	 * Looking for the Policy confirmation in the last XML message 
	 * received from the SIB 
	 * 
	 * @param xml the SIB xml message
	 * 
	 * @return true if the confirmation is found, false otherwise
	 */
	public boolean isPolicyConfirmed(String xml)
	{   String id[]={"transaction_type","message_type"};
	String ref[]={"POLICYADD","CONFIRM"};
	return autoCheckSibMessage(xml,id,ref)  
			&& getParameterElement(xml,"name", "status").getValue().equals("m3:Success");		
	}//public boolean isLeftConfirmed()

	/** FG
	 * Looking for the Policy remove confirmation in the last XML message 
	 * received from the SIB 
	 *
	 * @param xml the SIB xml message
	 * @return true if the confirmation is found, false otherwise
	 */
	public boolean isPolicyRemoveConfirmed(String xml)	{   
		String id[]={"transaction_type","message_type"};
		String ref[]={"POLICYDEL","CONFIRM"};

		return autoCheckSibMessage(xml,id,ref)  
			&& getParameterElement(xml,"name", "status").getValue().equals("m3:Success");		
	}//public boolean isLeftConfirmed()

	// end FG
	
	/**
	 * Looking for the Insert confirmation in the last XML message 
	 * received from the SIB 
	 * 
	 * @param xml the SIB xml message 
	 * @return true if the confirmation is found, false otherwise
	 */
	public boolean isInsertConfirmed(String xml)
	{   String id[]={"transaction_type","message_type"};
	String ref[]={"INSERT","CONFIRM"};
	return autoCheckSibMessage(xml,id,ref)  
			&& getParameterElement(xml,"name", "status").getValue().equals("m3:Success");		
	}//public boolean isLeftConfirmed()


	/**
	 * Looking for the Remove confirmation in the last XML message 
	 * received from the SIB
	 * 
	 * @param xml the SIB xml message 
	 * @return true if the confirmation is found, false otherwise
	 */
	public boolean isRemoveConfirmed(String xml)
	{   String id[]={"transaction_type","message_type"};
	String ref[]={"REMOVE","CONFIRM"};
	return autoCheckSibMessage(xml,id,ref)  
			&& getParameterElement(xml,"name", "status").getValue().equals("m3:Success");		
	}//public boolean isLeftConfirmed()


	/**
	 * The event subscription confirmation message bring with him an eventSubscripptionID
	 * This method return the event subscriptionID
	 *  
	 * @param xml the SIB xml message
	 * @return the event subscription ID if available
	 */
	public String getSubscriptionID(String xml)
	{   //<parameter name = "subscription_id">2</parameter>	  

		return getParameterElement(xml,"name", "subscription_id").getValue();
	}//public String getSubscriptionID(String xml)
	
	/** FG
	 * The event subscription confirmation message bring with him an eventSubscripptionID
	 * This method return the event subscriptionID
	 *  
	 * @param xml the SIB xml message
	 * @return the event subscription ID if available
	 */
	public String getPolicyID(String xml)
	{   //<parameter name = "subscription_id">2</parameter>	  

		return getParameterElement(xml,"name", "policy_id").getValue();
	}//public String getSubscriptionID(String xml)


	/**
	 * The persistent update confirmation message bring with him an updatedID
	 * This method return the updatedID
	 *  
	 * @param xml the SIB xml message
	 * @return the event subscription ID if available
	 */
	public String getUpdateID(String xml)
	{   //<parameter name = "update_id">2</parameter>	  
		return getParameterElement(xml,"name", "update_id").getValue();
	}//public String getSubscriptionID(String xml)


	/**
	 * This method makes one new query triple to add to the query to send to the SIB
	 * 
	 * 
	 * @param s the string representation of the subject. The value 'null' means any value
	 * @param p the string representation of the predicate. The value 'null' means any value
	 * @param o the string representation of the object. The value 'null' means any value
	 * @param s_type the string representation of the subject type. Allowed values are: uri, literal
	 * @param o_type the string representation of the object type. Allowed values are: uri, literal
	 * @return a string list representation of the triple  
	 */

	public Vector<String> newTriple(String s,String p,String o, String s_type, String o_type)
	{
		Vector<String> triple=new Vector<String>();

		triple.add(0, (s==null?ANYURI:s) ); 
		triple.add(1, (p==null?ANYURI:p) );
		triple.add(2, (o==null?ANYURI:o) );
		triple.add(3, s_type);
		triple.add(4, o_type);

		return triple;
	}//	public Vector<String> newTriple(String s,String p,String o, String s_type, String o_type)

	/**
	 * Method to print on the standard output the triple content
	 * 
	 * @param xml the SIB xml message
	 */
	public void printTriple(String xml)
	{
		Vector<Vector<String>> triples = getQueryTriple(xml);

		if(triples!=null)
		{ System.out.println("Triple List:\n");
		for(int i=0; i<triples.size() ; i++ )
		{ Vector<String> t=triples.get(i);
		System.out.println(
				"  S:["+t.get(0)
				+"] P:["+t.get(1)
				+"] O:["+t.get(2)
				+"] Otype:["+t.get(3)+"]");

		}//for(int j=0; i<triple.size() ; i++ )
		}  
		else System.out.println("SSAP_XMLTools:printTriple:NO TRIPLE FOUND!!!");

	}//	public void printTriple(xml)



	/*
	 * 
	 * M3 XML messages makers 
	 *  
	 **/



	/**
	 * Make the JOIN SSAP message 
	 *      
	 * @return a string representation of the XML answer message from the SIB
	 */
	public String join()
	{return 
			"<SSAP_message>"
			+"<message_type>REQUEST</message_type>"
			+"<transaction_type>JOIN</transaction_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			//+"<SIB_VERSION>100</SIB_VERSION>"
			+"</SSAP_message>";
	}//String join()


	/**
	 * Make the LEAVE SSAP message 
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String leave()
	{return 
			"<SSAP_message>"
			+"<message_type>REQUEST</message_type>"
			+"<transaction_type>LEAVE</transaction_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"</SSAP_message>";
	}//String leave()



	/**
	 * Make the RDF-QUERY SSAP message 
	 * 
	 * @param s the string representation of the subject.
	 * @param p the string representation of the predicate. The value 'any' means any value
	 * @param o the string representation of the object. the value 'any' means any value
	 * @param s_type the string representation of the subject type. Allowed values are: uri, literal
	 * @param o_type the string representation of the object type. Allowed values are: uri, literal
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String queryRDF(String s,String p,String o, String s_type, String o_type)
	{String st=s==null?"URI":s_type;
	String ot=o==null?"URI":o_type;

	return     
			"<SSAP_message><transaction_type>QUERY</transaction_type><message_type>REQUEST</message_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"   
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<parameter name = \"type\">RDF-M3</parameter>"
			+"<parameter name = \"query\"><triple_list>"
			+"<triple>"

    +"<subject type=\""+st+"\">"+(s==null?ANYURI:s)+"</subject>"
    +"<predicate>"+                (p==null?ANYURI:p)+"</predicate>"
    +"<object type=\""+ot+"\">"+(o==null?ANYURI:o)+"</object>"

    +"</triple>"
    +"</triple_list>" 
    +"</parameter></SSAP_message>";	   
	}//String queryRDF()



	/**
	 * Make the RDF Query SSAP message 
	 * 
	 * @param queryList is a structure to store every triple. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the subject type
	 * -the object type
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String queryRDF( Vector<Vector<String>> queryList )
	{String query=    
	"<SSAP_message><transaction_type>QUERY</transaction_type><message_type>REQUEST</message_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"   
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<parameter name = \"type\">RDF-M3</parameter>"
			+"<parameter name = \"query\">";

	query=query+this.getTripleListFromTripleVector(queryList);

	//    query=query+"<triple_list>";
	//     for(int i=0; i< queryList.size() ;i++)
	//       {String  s=queryList.elementAt(i).elementAt(0)
	//     	       ,p=queryList.elementAt(i).elementAt(1)
	//     	       ,o=queryList.elementAt(i).elementAt(2);
	//       
	//     	query=query
	//         +" <triple>"        
	//         +"  <subject type=\""+queryList.elementAt(i).elementAt(3)+"\">"+(s==null?ANYURI:s)+"</subject>"
	//         +"  <predicate>"+                                               (p==null?ANYURI:p)+"</predicate>"
	//         +"  <object type=\""+ queryList.elementAt(i).elementAt(4)+"\">"+(o==null?ANYURI:o)+"</object>"
	//         +" </triple>";
	//       }
	//    query=query+"</triple_list>";

	query=query
			+"</parameter></SSAP_message>";

	return query;
	}//public String queryRDF( Vector<Vector<String>> queryList )



	/**
	 * Transform the vector triple representation in String representation 
	 * 
	 * @param tripleList is a structure to store every SSAP triple.
	 * Each element of the vector contains another vector 
	 * formed by five string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the subject type
	 * -the object type
	 * 
	 * @return a string representation of the XML that represent the triple list for the SSAP message 
	 */
	private String getTripleListFromTripleVector( Vector<Vector<String>> tripleVector )
	{String tripleList="<triple_list>";

	for(int i=0; i< tripleVector.size() ;i++)
	{
	String s=tripleVector.elementAt(i).elementAt(0);
	String p=tripleVector.elementAt(i).elementAt(1);
	String o=tripleVector.elementAt(i).elementAt(2);
	String st=s==null?"URI":tripleVector.elementAt(i).elementAt(3);
	String ot=o==null?"URI":tripleVector.elementAt(i).elementAt(4);

	tripleList=tripleList
			+"<triple>"        
			+"<subject type=\""+st+"\">"+ (s==null?ANYURI:s)+"</subject>"
			+"<predicate>"+ (p==null?ANYURI:p)+"</predicate>"
			+"<object type=\"" +ot+"\">"
			+"<![CDATA["
			+ ""+( o==null?ANYURI:(correctEntityReferences(o.replaceAll("[^\\x20-\\x7e]", "??"))))
		  //  + ""+ (o==null?ANYURI:correctEntityReferences(o))
            +"]]>"
			+"</object>"		
			+"</triple>";
	}
	return tripleList+"</triple_list>";
	}//private String getTripleListFromTripleVector( Vector<Vector<String>> tripleList )




	/**
	 * Make the INSERT SSAP message
	 * 
	 * @param s the string representation of the subject.
	 * @param p the string representation of the predicate. The value 'any' means any value
	 * @param o the string representation of the object. the value 'any' means any value
	 * @param s_type the string representation of the subject type. Allowed values are: uri, literal
	 * @param o_type the string representation of the object type. Allowed values are: uri, literal
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String insert(String s,String p,String o, String s_type, String o_type)
	{return 
			"<SSAP_message>"
			+"<message_type>REQUEST</message_type>"
			+"<transaction_type>INSERT</transaction_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<parameter name=\"confirm\">TRUE</parameter>"
			+"<parameter name=\"insert_graph\"  encoding=\"RDF-M3\">"
			+"<triple_list>"
			+"<triple>"

    +"<subject type=\""+s_type+"\">"+ s+"</subject>"
    +"<predicate>"+                   p+"</predicate>"
    
    +"<object type=\""+o_type+"\">"
    
    + "<![CDATA["
    + (correctEntityReferences(o.replaceAll("[^\\x20-\\x7e]", "??")))
    +"]]>"
    +"</object>"

    +"</triple>"
    +"</triple_list>"
    +"</parameter>"
    +"</SSAP_message>";   		
	}//String insert(String s,String p,String o)


	/**
	 * Make the INSERT SSAP message 
	 * 
	 * @param tripleVector is a structure to store every triple. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the subject type
	 * -the object type
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String insert( Vector<Vector<String>>  tripleVector)
	{String SSAP_MSG=
	"<SSAP_message>"
			+"<message_type>REQUEST</message_type>"
			+"<transaction_type>INSERT</transaction_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<parameter name=\"confirm\">TRUE</parameter>"
			+"<parameter name=\"insert_graph\"  encoding=\"RDF-M3\">";

	SSAP_MSG=SSAP_MSG+this.getTripleListFromTripleVector(tripleVector);

	SSAP_MSG=SSAP_MSG
			+"</parameter>"
			+"</SSAP_message>";

	return SSAP_MSG;
	}


	/**
	 * Make the REMOVE SSAP message 
	 * 
	 * @param s the string representation of the subject. The value 'null' means any value
	 * @param p the string representation of the predicate. The value 'null' means any value
	 * @param o the string representation of the object. the value 'null' means any value
	 * @param s_type the string representation of the subject type. Allowed values are: uri, literal
	 * @param o_type the string representation of the object type. Allowed values are: uri, literal
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String remove(String s,String p,String o,String s_type,String o_type)
	{return 
			"<SSAP_message>"
			+"<message_type>REQUEST</message_type>"
			+"<transaction_type>REMOVE</transaction_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<parameter name=\"confirm\">TRUE</parameter>"
			+"<parameter name=\"remove_graph\"  encoding=\"RDF-M3\">"
			+"<triple_list>"
			+"<triple>"+


 		   "<subject type=\""+s_type+"\">"+(s==null?ANYURI:s)+"</subject>"
 		   +"<predicate>"+(p==null?ANYURI:p)+"</predicate>"
 		   
 		   +"<object type=\""+o_type+"\">"
 		    + "<![CDATA["
           + (o==null?ANYURI:o)
           +"]]>"
 		   + "</object>"

 		   +"</triple>"
 		   +"</triple_list>"
 		   +"</parameter>"
 		   +"</SSAP_message>";
	}//String remove(String s,String p,String o)



	/**
	 * Make the REMOVE SSAP message 
	 * 
	 * @param tripleVector is a structure to store every triple. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the subject type
	 * -the object type
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String remove( Vector<Vector<String>>  tripleVector)
	{String  SSAP_MSG=
	"<SSAP_message>"
			+"<message_type>REQUEST</message_type>"
			+"<transaction_type>REMOVE</transaction_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<parameter name=\"confirm\">TRUE</parameter>"
			+"<parameter name=\"remove_graph\"  encoding=\"RDF-M3\">";

	SSAP_MSG=SSAP_MSG+this.getTripleListFromTripleVector(tripleVector);

	SSAP_MSG=SSAP_MSG
			+"</parameter>"
			+"</SSAP_message>";

	return SSAP_MSG;
	}//String remove(String s,String p,String o)



	/**
	 * Make the UPDATE SSAP message 
	 *
	 * New value to insert:
	 * @param sn the string representation of the subject.
	 * @param pn the string representation of the predicate. 
	 * @param on the string representation of the object. 
	 * @param sn_type the string representation of the subject type. Allowed values are: uri, literal
	 * @param on_type the string representation of the object type. Allowed values are: uri, literal
	 * 
	 * Old value to replace:
	 * @param so the string representation of the subject.
	 * @param po the string representation of the predicate. 
	 * @param oo the string representation of the object. 
	 * @param so_type the string representation of the subject type. Allowed values are: uri, literal
	 * @param oo_type the string representation of the object type. Allowed values are: uri, literal
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	//o==old, n==new
	public String update( String sn,String pn,String on,String sn_type,String on_type
			,String so,String po,String oo,String so_type,String oo_type)
	{return 
			"<SSAP_message><transaction_type>UPDATE</transaction_type>"
			+"<message_type>REQUEST</message_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<parameter name = \"insert_graph\" encoding = \""+"RDF-M3"+"\">"
			//insert (NEW)

 		   +"<triple_list><triple>"
 		   +"<subject type=\""+sn_type+"\">"+ sn+"</subject>"
 		   +"<predicate>"+                    pn+"</predicate>"
 		   +"<object type=\""+on_type+"\">"
 		   + "<![CDATA["
           +(correctEntityReferences(on.replaceAll("[^\\x20-\\x7e]", "??")))
           +"]]>"
 		   +"</object>"		   
 		   +"</triple></triple_list>"

          +"</parameter>"

          +"<parameter name = \"remove_graph\" encoding = \""+"RDF-M3"+"\">"
          //remove (OLD)
          +"<triple_list><triple>"

           +"<subject type=\""+so_type+"\">"+(so==null?ANYURI:so)+"</subject>"
           +"<predicate>"+(po==null?ANYURI:po)+"</predicate>"
           +"<object type=\""+oo_type+"\">"
            + "<![CDATA["
           +( oo==null?ANYURI:(correctEntityReferences(oo.replaceAll("[^\\x20-\\x7e]", "??"))))
           +"]]>"
           +"</object>"

          +"</triple></triple_list>"

          +"</parameter>"
          +"<parameter name = \"confirm\">TRUE</parameter>"
          +"</SSAP_message>";
	}//String update(String s,String p,String o)





	/**
	 * Make the UPDATE SSAP message 
	 * 
	 * New value to insert:
	 * @param newTripleVector the structure to store every new triple to insert. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the string representation of the new subject
	 * -the string representation of the new predicate
	 * -the string representation of the new object
	 * -the string representation of the new subject type. Allowed values are: uri, literal
	 * -the string representation of the new object type. Allowed values are: uri, literal
	 *   
	 * Old value to replace:
	 * @param oldTripleVector the structure to store every old triple to replace. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the string representation of the old subject
	 * -the string representation of the old predicate
	 * -the string representation of the object
	 * -the string representation of the old subject type. Allowed values are: uri, literal
	 * -the string representation of the old object type. Allowed values are: uri, literal
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	//o==old, n==new
	public String update( Vector<Vector<String>>  newTripleVector
			, Vector<Vector<String>>  oldTripleVector)
	{return 
			"<SSAP_message><transaction_type>UPDATE</transaction_type>"
			+"<message_type>REQUEST</message_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<parameter name = \"insert_graph\" encoding = \""+"RDF-M3"+"\">"
			//insert (NEW)
			+this.getTripleListFromTripleVector(newTripleVector)
			+"</parameter>"

          +"<parameter name = \"remove_graph\" encoding = \""+"RDF-M3"+"\">"
          //remove (OLD)
          +this.getTripleListFromTripleVector(oldTripleVector)

          +"</parameter>"
          +"<parameter name = \"confirm\">TRUE</parameter>"
          +"</SSAP_message>";
	}//String update(String s,String p,String o)





	/**
	 * Make the SUBSCRIPTION SSAP message 
	 *  
	 * @param s the string representation of the subject. The value null means any value
	 * @param p the string representation of the predicate. The value null means any value
	 * @param o the string representation of the object. The value null means any value
	 * @param o_type the string representation of the object type. Allowed values are: uri, literal
	 * 
	 * @return a string representation of the XML message to send to the SIB 
	 */
	public String subscribeRDF(String s,String p,String o,String o_type)
	{++transaction_id;
	

	subscription_id=transaction_id;

	//String ot=(o==null?"URI":o_type);

	return 
			"<SSAP_message>" 
			+"<transaction_type>SUBSCRIBE</transaction_type><message_type>REQUEST</message_type>" 
			+"<transaction_id>"+ transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<parameter name=\"type\">RDF-M3</parameter>" 
			+"<parameter name=\"query\"><triple_list><triple>"
			+"<subject>"+(s==null?ANYURI:s)+"</subject>"
			+"<predicate>"+                    (p==null?ANYURI:p)+"</predicate>"
			+"<object type=\""+o_type+"\">"
			  + "<![CDATA["
           +(o==null?ANYURI:o)
           +"]]>"
			+"</object>"        
			+"</triple></triple_list></parameter></SSAP_message>";
	}//String subscribe()

	/**
	 * Make the SUBSCRIPTION SSAP message 
	 *  
	 * @param triples the triples patterns to subscribe
	 * 
	 * @return a string representation of the XML message to send to the SIB 
	 */
	public String subscribeRDF(Vector<Vector<String> > triples)
	{
		++transaction_id;
		subscription_id=transaction_id;




		return 
				"<SSAP_message>" 
				+"<transaction_type>SUBSCRIBE</transaction_type><message_type>REQUEST</message_type>" 
				+"<transaction_id>"+ transaction_id +"</transaction_id>"
				+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
				+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
				+"<parameter name=\"type\">RDF-M3</parameter>" 
				+"<parameter name=\"query\">" 
				+ this.getTripleListFromTripleVector(triples)
				+"</parameter>"
				+"</SSAP_message>";        

	}

	public String subscribeSPARQL (String query)
	{
		return 
				"<SSAP_message>" 
				+"<transaction_type>SUBSCRIBE</transaction_type><message_type>REQUEST</message_type>" 
				+"<transaction_id>"+ transaction_id +"</transaction_id>"
				+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
				+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
				+"<parameter name = \"type\">sparql</parameter>" 
				+		"<parameter name = \"query\">" 
				//+ "<![CDATA["
		        +correctEntityReferences(query)
		        //   +"]]>"
				+"</parameter></SSAP_message>";
	}//SubscribeSPARQL

	/**
	 * Make the UNSUBSCRIBE SSAP message
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String unsubscribe()
	{
		return 
				"<SSAP_message>"
				+ "<transaction_type>UNSUBSCRIBE</transaction_type>"
				+ "<message_type>REQUEST</message_type>"

         + "<transaction_id>"+ ++transaction_id +"</transaction_id>"
         + "<node_id>"+nodeID+"</node_id>" //+ "<node_id>{"+nodeID+"}</node_id>"
         +"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
         + "<parameter name = \"subscription_id\">" + subscription_id + "</parameter>"
         + "</SSAP_message>";
	}//String unsubscribe()
	

	public String cancel_persistent_update(String update_id)
	{
		return 
				"<SSAP_message>"
				+ "<transaction_type>CANCEL_PERSISTENT_UPDATE</transaction_type>"
				+ "<message_type>REQUEST</message_type>"

         + "<transaction_id>"+ ++transaction_id +"</transaction_id>"
         + "<node_id>"+nodeID+"</node_id>" //+ "<node_id>{"+nodeID+"}</node_id>"
         +"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
         + "<parameter name = \"update_id\">" + update_id + "</parameter>"
         + "</SSAP_message>";
	}


	/**
	 * Make the UNSUBSCRIBE SSAP message for a specific subscription ID
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String unsubscribe(String subscription_id)
	{return  
			"<SSAP_message>"
			+ "<transaction_type>UNSUBSCRIBE</transaction_type>"
			+ "<message_type>REQUEST</message_type>"

         + "<transaction_id>"+ transaction_id +"</transaction_id>"
         + "<node_id>"+nodeID+"</node_id>" //+ "<node_id>{"+nodeID+"}</node_id>"
         +"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
         + "<parameter name = \"subscription_id\">" + subscription_id + "</parameter>"
         + "</SSAP_message>";
	}//String unsubscribe()





	/* * * * * * * * * * * * * * * * * * *\
         List of every WQL query types
    \* * * * * * * * * * * * * * * * * * */

	public final String WQL_VALUES="WQL-VALUES";
	public final String WQL_RELATED="WQL-RELATED";
	public final String WQL_NODETYPES="WQL-NODETYPES";
	public final String WQL_ISTYPE="WQL-ISTYPE";
	public final String WQL_ISSUBTYPE="WQL-ISSUBTYPE";

	/* * * * * * * * * * * * * * * * * * *\
       WQL querys
   \* * * * * * * * * * * * * * * * * * */

	/**
	 * Make the WQL-QUERY SSAP message
	 * 
	 * @param startNodeURI the string representation of the node URI
	 * @param type the string representation of the node URI type. Can be "literal" or "URI"
	 * @param path the string representation of the WQL graph path
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String queryWQL_VALUES(String startNodeURI,String type,String path)
	{String wql_query_nodes[] = new String[10];
	int i=-1;

	wql_query_nodes[++i]=newXmlNodeTAG("start", type, startNodeURI);
	wql_query_nodes[++i]=newXmlPathExpressionTAG(path);

	return 
			this.queryWQLSurround(wql_query_nodes,this.WQL_VALUES);
	}//String queryWQL_VALUES()



	/**
	 * Make the WQL-QUERY SSAP message 
	 * 
	 * @param startNodeURI the string representation of the start node URI
	 * @param startType the string representation of the node URI type. Can be "literal" or "URI"
	 * @param endNodeURI the string representation of the end node URI
	 * @param endType the string representation of the node URI type. Can be "literal" or "URI"
	 * @param path the string representation of the WQL graph path
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String queryWQL_RELATED(String startNodeURI,String startType, String endNodeURI,String endType, String path)
	{String wql_query_nodes[] = new String[10];
	int i=-1;

	wql_query_nodes[++i]=newXmlNodeTAG("start", startType, startNodeURI);
	wql_query_nodes[++i]=newXmlNodeTAG("end", endType, endNodeURI);
	wql_query_nodes[++i]=newXmlPathExpressionTAG(path);

	return 
			this.queryWQLSurround(wql_query_nodes,this.WQL_RELATED);
	}//String queryWQL_RELATED()



	/**
	 * Make the WQL-QUERY SSAP message 
	 *
	 * 
	 * @param nodeURI the string representation of the node URI
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String queryWQL_NODETYPES(String nodeURI)
	{String wql_query_nodes[] = new String[10];
	int i=-1;

	wql_query_nodes[++i]=newXmlNodeTAG(nodeURI);

	return 
			this.queryWQLSurround(wql_query_nodes,this.WQL_NODETYPES);
	}//String queryWQL_NODETYPES()



	/**
	 * Make the WQL-QUERY SSAP message 
	 *
	 * 
	 * @param nodeURI the string representation of the node URI
	 * @param nodeType the string representation of the node URI type. Can be "literal" or "URI"
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String queryWQL_ISTYPES(String nodeURI, String nodeType)
	{String wql_query_nodes[] = new String[10];
	int i=-1;

	wql_query_nodes[++i]=newXmlNodeTAG("", "", nodeURI);
	wql_query_nodes[++i]=newXmlNodeTAG("type", "", nodeType);

	return 
			this.queryWQLSurround(wql_query_nodes,this.WQL_ISTYPE);
	}//String queryWQL_ISTYPES()


	/**
	 * Make the WQL-QUERY SSAP message 
	 *
	 * 
	 * @param subClassNodeURI the string representation of a sub-class node URI
	 * @param superClassNodeURI the string representation of a super-class node URI
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String queryWQL_ISSUBTYPE(String subClassNodeURI, String superClassNodeURI)
	{String wql_query_nodes[] = new String[10];
	int i=-1;

	wql_query_nodes[++i]=newXmlNodeTAG("subtype", "", subClassNodeURI);
	wql_query_nodes[++i]=newXmlNodeTAG("supertype", "", superClassNodeURI);

	return 
			this.queryWQLSurround(wql_query_nodes,this.WQL_ISSUBTYPE);
	}//String queryWQL_ISSUBTYPE()





	/**
	 * Method to build an XML node tag for WQL query purpose
	 * 
	 * @param name the node name 
	 * @param type the node type. Can be "literal" or "URI"
	 * @param content the node content
	 * 
	 * @return a string representation of WQL query node tag
	 */
	private final String newXmlNodeTAG(String name, String type, String content)
	{
		String str_name=name.equals("")?"":"name=\""+name+"\"";
		String str_type=type.equals("")?"":"type=\""+type+"\"";

		return "<node "+str_name+" "+str_type+" >"+content+"</node>";
		//return "<node name=\""+name+"\" type=\""+type+"\" >"+content+"</node>";
	}//private final String newXmlNodeTAG(String name, String type, String content)


	/**
	 * Method to build an XML node tag for WQL query purpose
	 * 
	 * @param content the node content
	 * 
	 * @return a string representation of WQL query node tag
	 */
	private final String newXmlNodeTAG( String content)
	{
		return "<node>"+content+"</node>";
	}//private final String newXmlNodeTAG( String content)


	/**
	 * Method to build an XML path_expression tag for WQL query purpose
	 * 
	 * @param path the path
	 * 
	 * @return a string representation of WQL query path_expression tag
	 */
	private final String newXmlPathExpressionTAG(String path)
	{
		return "<path_expression>"+path+"</path_expression>";
	}//private final newWQLNode()




	/**
	 * Method to complete the XML message for the wilbur query.
	 * This method surround the string representation of the all 
	 * nodes needed for the query with the minimum basic xml code
	 * to complete the message.
	 * 
	 * @param wql_query_nodes the string representation of all node tags for the wilbur query as show below:
	 * <node>.....</node><node>.....</node><node>.....</node>
	 * 
	 * @return the string representation of the final XML message for the wilbur query
	 */
	private final String queryWQLSurround(String wql_query_nodes[], String queryWQLType)
	{String str_wql_query_nodes="";

	for (int i=0;i<wql_query_nodes.length;i++)
		if(wql_query_nodes[i]!=null)str_wql_query_nodes+=wql_query_nodes[i];


	return
			"<SSAP_message>"
			+"<transaction_type>QUERY</transaction_type><message_type>REQUEST</message_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"   
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<parameter name = \"type\">"+queryWQLType+"</parameter>"
			+"<parameter name = \"query\">"
			+"<wql_query>"

   	       +str_wql_query_nodes

   	       +"</wql_query>"
   	       +"</parameter>"
   	       +"</SSAP_message>";
	}//private final String queryWQLSurround(String wql_query)





	/* * * * * * * * * * * * * * * * * * *\
      WQL subscription
   \* * * * * * * * * * * * * * * * * * */


	/**
	 * Make the WQL-QUERY SUBSCRIPTION SSAP message 
	 *
	 * 
	 * @param startNodeURI the string representation of the node URI
	 * @param type the string representation of the node URI type. Can be "literal" or "URI"
	 * @param path the string representation of the WQL graph path
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String subscribeWQL_VALUES(String startNodeURI,String type,String path)
	{String wql_query_nodes[] = new String[10];
	int i=-1;

	wql_query_nodes[++i]=newXmlNodeTAG("start", type, startNodeURI);
	wql_query_nodes[++i]=newXmlPathExpressionTAG(path);

	return 
			this.subscribeWQLSurround(wql_query_nodes,this.WQL_VALUES);
	}//String subscribeWQL_VALUES()





	/**
	 * Make the WQL-QUERY SUBSCRIPTION SSAP message 
	 *
	 * 
	 * @param startNodeURI the string representation of the start node URI
	 * @param startType the string representation of the node URI type. Can be "literal" or "URI"
	 * @param endNodeURI the string representation of the end node URI
	 * @param endType the string representation of the node URI type. Can be "literal" or "URI"
	 * @param path the string representation of the WQL graph path
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String subscribeWQL_RELATED(String startNodeURI,String startType, String endNodeURI,String endType, String path)
	{String wql_query_nodes[] = new String[10];
	int i=-1;

	wql_query_nodes[++i]=newXmlNodeTAG("start", startType, startNodeURI);
	wql_query_nodes[++i]=newXmlNodeTAG("end", endType, endNodeURI);
	wql_query_nodes[++i]=newXmlPathExpressionTAG(path);

	return 
			this.subscribeWQLSurround(wql_query_nodes,this.WQL_RELATED);
	}//String subscribeWQL_RELATED()


	/**
	 * Make the WQL-QUERY SUBSCRIPTION SSAP message 
	 *
	 * 
	 * @param nodeURI the string representation of the node URI
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String subscribeWQL_NODETYPES(String nodeURI)
	{String wql_query_nodes[] = new String[10];
	int i=-1;

	wql_query_nodes[++i]=newXmlNodeTAG(nodeURI);

	return 
			this.subscribeWQLSurround(wql_query_nodes,this.WQL_NODETYPES);
	}//String subscribeWQL_NODETYPES()


	/**
	 * Make the WQL-QUERY SUBSCRIPTION SSAP message 
	 *
	 * 
	 * @param nodeURI the string representation of the node URI
	 * @param nodeType the string representation of the node URI type. Can be "literal" or "URI"
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String subscribeWQL_ISTYPES(String nodeURI, String nodeType)
	{String wql_query_nodes[] = new String[10];
	int i=-1;

	wql_query_nodes[++i]=newXmlNodeTAG("start", "", nodeURI);
	wql_query_nodes[++i]=newXmlNodeTAG("type", "", nodeType);

	return 
			this.subscribeWQLSurround(wql_query_nodes,this.WQL_ISTYPE);
	}//String subscribeWQL_ISTYPES()


	/**
	 * Make the WQL-QUERY SUBSCRIPTION SSAP message
	 *
	 * 
	 * @param subClassNodeURI the string representation of a sub-class node URI
	 * @param superClassNodeURI the string representation of a super-class node URI
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String subscribeWQL_ISSUBTYPE(String subClassNodeURI, String superClassNodeURI)
	{String wql_query_nodes[] = new String[10];
	int i=-1;

	wql_query_nodes[++i]=newXmlNodeTAG("subtype", "", subClassNodeURI);
	wql_query_nodes[++i]=newXmlNodeTAG("supertype", "", superClassNodeURI);

	return 
			this.subscribeWQLSurround(wql_query_nodes,this.WQL_ISSUBTYPE);
	}//String subscribeWQL_ISSUBTYPE()



	/**
	 * Format the XML message to send to the SIB as a subscription 
	 * @param s the string representation of the subject.
	 * @param p the string representation of the predicate. The value 'any' means any value
	 * @param o the string representation of the object. the value 'any' means any value
	 * @param o_type the string representation of the object type. Allowed values are: uri, literal
	 * 
	 * @return a string representation of the XML message to send to the SIB 
	 */
	private String subscribeWQLSurround(String[] wql_query_nodes,String wql_values) 
	{++transaction_id;
	subscription_id=transaction_id;

	String str_wql_query_nodes="";

	for (int i=0;i<wql_query_nodes.length;i++)
		if(wql_query_nodes[i]!=null)str_wql_query_nodes+=wql_query_nodes[i];


	return 
			"<SSAP_message><transaction_type>SUBSCRIBE</transaction_type><message_type>REQUEST</message_type>" 
			+"<transaction_id>"+ transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"

    +"<parameter name = \"type\">"+wql_values+"</parameter>" 
    +"<parameter name = \"query\">"

           +"<wql_query>"
           +str_wql_query_nodes
           +"</wql_query>"

         +"</parameter>"  
         +"</SSAP_message>";

	}//String subscribe()

	/**
	 * Returns true if the received notification is relative to an RDF-M3 subscription 
	 * @param xml String received from SIB as a notification
	 * @return
	 */
	public Boolean isRDFNotification (String xml)
	{
		//boolean isRDF = false;
		if(xml==null)
		{
			System.out.println("ERROR:SSAP_XMLTools:SibXMLMessageParser: XML message is null");
			return null;
		}


		Document doc=null;
		try {
			doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		if(doc==null)
		{
			System.out.println("ERROR:SSAP_XMLTools:SibXMLMessageParser:doc is null");
			return null;
		}
		Element root = doc.getRootElement();
		Namespace ns = root.getNamespace();
		List<Element> parameters = root.getChildren("parameter", ns);
		Element p_new = null;
		Element p_old = null;

		int ip;
		for(ip=0;ip<parameters.size();ip++)
		{
			if( parameters.get(ip).getAttributeValue("name").equalsIgnoreCase("new_results"))
			{
				p_new = parameters.get(ip);
			}
			if( parameters.get(ip).getAttributeValue("name").equalsIgnoreCase("obsolete_results"))
			{
				p_old = parameters.get(ip);
			}
			if((p_new!=null) && (p_old!= null))
			{
				break;
			}
		}

		Element p_new_child = null;
		Element p_old_child = null;

		//p_new_child = (Element) p_new.getChildren().get(0);
		//p_old_child = (Element) p_new.getChildren().get(0);

		if(p_new.getChildren().size()>0)
		{
			p_new_child =(Element) p_new.getChildren().get(0);
			if(p_new_child.getName().equalsIgnoreCase("triple_list"))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		if( p_old.getChildren().size()>0)
		{
			p_old_child = (Element) p_old.getChildren().get(0);
			if(p_old_child.getName().equalsIgnoreCase("triple_list"))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		return true;


	}




	/* * * * * * * * * * * * * * * * * * *\
        WQL SIB message handlers
    \* * * * * * * * * * * * * * * * * * */
	//[literal|uri , value] 
	/**
	 * @deprecated
	 * @param xml
	 * @return
	 */
	public Vector<Vector<String>> getWQLResultNode(String xml)
	{return getWQLEventNode(xml,"results");}
	/**
	 * @deprecated
	 * @param xml
	 * @return
	 */
	public Vector<Vector<String>> getWQLNewResultEventNode(String xml)
	{return getWQLEventNode(xml,"new_results");}
	/**
	 * @deprecated
	 * @param xml
	 * @return
	 */
	public Vector<Vector<String>> getWQLObsoleteResultEventNode(String xml)
	{return getWQLEventNode(xml,"obsolete_results");}


	/**
	 * @deprecated
	 * @param xml
	 * @return
	 */
	private Vector<Vector<String>> getWQLEventNode(String xml,String ParamAttValue)
	{
		if(xml==null){System.out.println("ERROR:SSAP_XMLTools:getWQLEventTriple: XML message is null");return null;}

		Element parameters = getParameterElement(xml,"name", ParamAttValue);
		if(parameters==null){System.out.println("ERROR:SSAP_XMLTools:getWQLEventTriple:parameters not found:"+ParamAttValue);return null;}

		Element node_list = parameters.getChild("node_list");
		if(node_list==null) {System.out.println("ERROR:SSAP_XMLTools:getWQLEventTriple:node_list not found");return null;}

		List<Element> all_node = node_list.getChildren(); //could be literal or uri
		if(all_node==null /*|| all_node.isEmpty()*/)
		{System.out.println("ERROR:SSAP_XMLTools:getWQLEventTriple:children for node_list not found");return null;}

		Vector<Vector<String>> nodes=new Vector<Vector<String>>();

		Iterator<Element> i = all_node.iterator();

		while(i.hasNext())
		{   Vector<String> singleton=new Vector<String>();

		Element etriple = i.next();

		//System.out.println("E-TRIPLE:"+etriple.getName()+" value:"+etriple.getText());
		singleton.add(etriple.getName());
		singleton.add(etriple.getText());

		nodes.add(singleton);			
		}//while(i.hasNext())

		return nodes;
	}

	/**
	 * @deprecated
	 * @param xml
	 * @return
	 */
	public String getWQLTrueFalseResultEventNode(String xml)
	{
		if(xml==null){System.out.println("ERROR:SSAP_XMLTools:getWQLEventTriple: XML message is null");return null;}

		Element parameters = getParameterElement(xml,"name", "results");
		if(parameters==null){System.out.println("ERROR:SSAP_XMLTools:getWQLEventTriple:parameters not found:"+"results");return null;}

		return parameters.getText();
	}  



	/**
	 * @deprecated
	 * @param xml
	 * @return
	 */
	public void printNodes(Vector<Vector<String>> nodes)
	{if(nodes==null){System.out.println("ERROR:SSAP_XMLTools:printNodes:nodes is null!");return;}

	System.out.println("SSAP_XMLTools:printNodes:");
	for(int i=0;i<nodes.size();i++)
	{System.out.println("Line:"+i+":");
	Vector<String> line=nodes.get(i);

	for(int j=0;j<line.size();j++)
	{System.out.print(j+") "+line.get(j)+" ");
	}
	System.out.println();
	}	
	}


	/**
	 * Returns the subject of a triple with a given triple represented ina Vector<String>
	 * @param t A Vector<String> representing a triple
	 * @return the subject of the triple
	 */
	public String triple_getSubject    (Vector<String> t){return t.get(0);}

	/**
	 * Returns the predicate of a triple with a given triple represented ina Vector<String>
	 * @param t A Vector<String> representing a triple
	 * @return the predicate of the triple
	 */
	public String triple_getPredicate  (Vector<String> t){return t.get(1);}

	/**
	 * Returns the object of a triple with a given triple represented ina Vector<String>
	 * @param t A Vector<String> representing a triple
	 * @return the object of the triple
	 */
	public String triple_getObject     (Vector<String> t)
	{
		return t.get(2);
	}

	/**
	 * Returns the object type of a triple with a given triple represented ina Vector<String>
	 * @param t A Vector<String> representing a triple
	 * @return the object type of the triple
	 */
	public String triple_getObjectType (Vector<String> t){return t.get(3);}


	/**
	 * @deprecated
	 * @param t
	 * @return
	 */
	public String node_getValue        (Vector<String> t){return t.get(1);}

	/**
	 * @deprecated
	 * @param t
	 * @return
	 */
	public String node_getType         (Vector<String> t){return t.get(0);}


	/**
	 * 
	 * @param xml the xml document to parse
	 * @return the value of the element "status"
	 */
	public String getSSAPmsgStatus(String xml)
	{ //System.out.println("SSAP_XMLTools:getSSAPmsgStatus:xml content:\n"+xml);
		if(getParameterElement(xml,"name", "status")!= null)
		{
			return getParameterElement(xml,"name", "status").getValue();
		}
		else
		{
			return null;
		}
	}
	public String getSSAPmsgIndicationSequence(String xml)
	{ //System.out.println("SSAP_XMLTools:getSSAPmsgStatus:xml content:\n"+xml);
		if(getParameterElement(xml,"name", "ind_sequence")!= null)
		{
			return getParameterElement(xml,"name", "ind_sequence").getValue();
		}
		else
		{
			return null;
		}
	}

	/**
	 * Make the SELECT SPARQL query SSAP message
	 * 
	 * @param query_string the SPARQL query to be performed
	 * @return a string representing the SSAP message to be sent to the SIB to perform a SPARQL query
	 **/
	public String querySPARQL(String query_string)
	{

		return     
				"<SSAP_message><transaction_type>QUERY</transaction_type><message_type>REQUEST</message_type>"
				+"<transaction_id>"+ transaction_id +"</transaction_id>"
				+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"   
				+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
				+"<parameter name = \"type\">sparql</parameter>"
				+"<parameter name = \"query\">"
				//+ "<![CDATA["
		        +correctEntityReferences(query_string)
		        //+"]]>"
				+"</parameter></SSAP_message>";	

	}//querySPARQL()
	
	
	
	/** FG
	 * Make the POLICY ADD message
	 * 
	 * @param s the string representation of the subject.
	 * @param p the string representation of the predicate. The value 'any' means any value
	 * @param o the string representation of the object. the value 'any' means any value
	 * @param s_type the string representation of the subject type. Allowed values are: uri, literal
	 * @param o_type the string representation of the object type. Allowed values are: uri, literal
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String policyAdd(String query, String allowed, String protectionMode)
	{ return 
			"<SSAP_message>" 
			+"<transaction_type>POLICYADD</transaction_type>"
			+"<message_type>REQUEST</message_type>" 
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<protection_mode>"+ protectionMode+"</protection_mode>"
			+"<allowed>"+allowed+"</allowed>"
			+"<parameter name = \"type\">sparql</parameter>" 
			+		"<parameter name = \"query\">" 
			//+ "<![CDATA["
	        +correctEntityReferences(query)
	        //   +"]]>"
			+"</parameter></SSAP_message>";	
	}//String insert(String s,String p,String o)

	/** FG
	 * Make the POLICY DELETE SSAP message for a specific subscription ID
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String policyDel(String policy_id)
	{return  
			"<SSAP_message>"
			+ "<transaction_type>POLICYDEL</transaction_type>"
			+ "<message_type>REQUEST</message_type>"
			+ "<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+ "<node_id>"+nodeID+"</node_id>" //+ "<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+ "<parameter name = \"policy_id\">" + policy_id + "</parameter>"
			+ "</SSAP_message>";
	}

	/**
	 * This method corrects the 5 predefined entity references in XML which may occur in the sparql query string
	 * @param query_string:  the SPARQL query to be transformed 
	 * @return the query with correct substitution of the xml entities
	 * 
	 */
	public String correctEntityReferences(String query){

		String new_query = new String();

		new_query = query.replace("&", "&amp;");
		new_query = new_query.replace("<", "&lt;");
		new_query = new_query.replace(">", "&gt;");
		new_query = new_query.replace("'", "&apos;");
		new_query = new_query.replace("\"", "&quot;");

		return new_query;
	}

	/**
	 * Make the SSAP  message to be sent to the SIB to perform SPARQL queries in SIBs supporting it
	 * @param insGraph graph to be inserted
	 * @param remGraph graph to be removed
	 * @return  SSAP  message to be sent to the SIB to support SPARQL queries in SIBs supporting it
	 */

	public String update_sparql (String sparql_update)
	{
		
			return  "<SSAP_message><transaction_type>UPDATE</transaction_type>"
					+"<message_type>REQUEST</message_type>"
					+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
					+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
					+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
					+"<parameter name = \"insert_graph\" encoding = \""+"SPARQL-UPDATE"+"\">"
					//+ "<![CDATA["
					+correctEntityReferences(sparql_update)
					//+"]]>"
					+"</parameter>"
					+"<parameter name = \"confirm\">TRUE</parameter>"
					+"</SSAP_message>";
		
	}

	/**
	 * 
	 * @param query the persistent sparql update query
	 * @return SSAP  message to be sent to the SIB to support SPARQL queries in SIBs supporting it
	 */

	public String persistent_update(String query )
	{
		return  "<SSAP_message><transaction_type>PERSISTENT_UPDATE</transaction_type>"
				+"<message_type>REQUEST</message_type>"
				+"<transaction_id>"+ transaction_id +"</transaction_id>"
				+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
				+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
				+"<parameter name = \"query\" encoding = \""+"SPARQL-UPDATE"+"\">"
			//	+ "<![CDATA["
				+correctEntityReferences(query)
			//	+"]]>"
				+"</parameter>"
				+"<parameter name = \"confirm\">TRUE</parameter>"
				+"</SSAP_message>";
	}
	
	/**
	 * Make the SSAP  message to be sent to the SIB to perform RDF/XML update in SIBs supporting it
	 * Optimized for red-SIB
	 * @param insGraph graph to be inserted
	 * @param remGraph graph to be removed
	 * @return  the SSAP  message to be sent to the SIB to perform RDF/XML update in SIBs supporting it
	 */

	public String update_rdf_xml( String insGraph, String remGraph)
	{
		return 
				"<SSAP_message><transaction_type>UPDATE</transaction_type>"
				+"<message_type>REQUEST</message_type>"
				+"<transaction_id>"+ transaction_id +"</transaction_id>"
				+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
				+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
				+"<parameter name = \"insert_graph\" encoding = \""+"RDF-XML"+"\">"
				//insert (NEW)

             //   + "<![CDATA["
                +correctEntityReferences((insGraph.replaceAll("[^\\x20-\\x7e]", "??")))
              //  +"]]>"


      +"</parameter>"

    +"<parameter name = \"remove_graph\" encoding = \""+"RDF-XML"+"\">"
    //remove (OLD)
  //  + "<![CDATA["
    +correctEntityReferences((remGraph.replaceAll("[^\\x20-\\x7e]", "??")))
  //  +"]]>"

    +"</parameter>"
    +"<parameter name = \"confirm\">TRUE</parameter>"
    +"</SSAP_message>";
	}//String update_rdf_xml( String insGraph, String remGraph)
	

	/**
	 * Make the SSAP  message to be sent to the SIB to perform RDF/XML update in SIBs supporting it
	 * Optimized for OSGI SIB
	 * @param insGraph graph to be inserted
	 * @param remGraph graph to be removed
	 * @return  the SSAP  message to be sent to the SIB to perform RDF/XML update in SIBs supporting it
	 */

	public String update_rdf_xml_2( String insGraph, String remGraph)
	{
		return 
				"<SSAP_message><transaction_type>UPDATE</transaction_type>"
				+"<message_type>REQUEST</message_type>"
				+"<transaction_id>"+ transaction_id +"</transaction_id>"
				+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
				+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
				+"<parameter name = \"insert_graph\" encoding = \""+"RDF-XML"+"\">"
				//insert (NEW)

                + "<![CDATA["
                +(insGraph.replaceAll("[^\\x20-\\x7e]", "??"))
                +"]]>"


      +"</parameter>"

    +"<parameter name = \"remove_graph\" encoding = \""+"RDF-XML"+"\">"
    //remove (OLD)
    + "<![CDATA["
    +(remGraph.replaceAll("[^\\x20-\\x7e]", "??"))
    +"]]>"

    +"</parameter>"
    +"<parameter name = \"confirm\">TRUE</parameter>"
    +"</SSAP_message>";
	}//String update_rdf_xml( String insGraph, String remGraph)


	/**
	 * Make the SSAP  message to be sent to the SIB to perform RDF/XML insert in SIBs supporting it
	 * @param graph the graph to be inserted
	 * @return the SSAP  message to be sent to the SIB to perform RDF/XML insert in SIBs supporting it.
	 * This is optimized for red-SIB
	 */
	public String insert_rdf_xml (String graph)
	{
		//Clean string from control chars
		graph = graph.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
		
		return "<SSAP_message>"
				+"<transaction_type>INSERT</transaction_type>"
				+"<message_type>REQUEST</message_type>"
				+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
				+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
				+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
				+"<parameter name=\"insert_graph\"  encoding=\"RDF-XML\">"
				+ "<![CDATA["
			    //+correctEntityReferences((graph.replaceAll("[^\\x20-\\x7e]", "??")))
			    + graph
			    +"]]>"
				+"</parameter>"
				+"<parameter name = \"confirm\">TRUE</parameter>"
				+"</SSAP_message>"; 
	} //String insert_rdf_xml (String graph)

	
	/**
	 * Make the SSAP  message to be sent to the SIB to perform RDF/XML insert in SIBs supporting it
	 * @param graph the graph to be inserted
	 * @return the SSAP  message to be sent to the SIB to perform RDF/XML insert in SIBs supporting it
	 * optimized for OSGI SIB
	 */
	public String insert_rdf_xml_2 (String graph)
	{
		
		return "<SSAP_message>"
				+"<transaction_type>INSERT</transaction_type>"
				+"<message_type>REQUEST</message_type>"
				+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
				+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
				+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
				+"<parameter name=\"insert_graph\"  encoding=\"RDF-XML\">"
				+ "<![CDATA["
			    +(graph.replaceAll("[^\\x20-\\x7e]", "??"))
			    +"]]>"
				+"</parameter>"
				+"<parameter name = \"confirm\">TRUE</parameter>"
				+"</SSAP_message>"; 
	} //String insert_rdf_xml (String graph)
	

	/**
	 * Make the SSAP  message to be sent to the SIB to perform RDF/XML remove in SIBs supporting it
	 * @param graph the graph to be removed
	 * @return the SSAP  message to be sent to the SIB to perform RDF/XML remove in SIBs supporting it
	 * optimized for red-SIB
	 */
	public String remove_rdf_xml(String graph)
	{return 
			"<SSAP_message>"
			+"<transaction_type>REMOVE</transaction_type>"
			+"<message_type>REQUEST</message_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<parameter name=\"remove_graph\"  encoding=\"RDF-XML\">"
		//	+ "<![CDATA["
			+correctEntityReferences((graph.replaceAll("[^\\x20-\\x7e]", "??")))
		//    +"]]>"
			+"</parameter>"
			+"<parameter name = \"confirm\">TRUE</parameter>"
			+"</SSAP_message>";
	}//String remove(String graph) 
	
	/**
	 * Make the SSAP  message to be sent to the SIB to perform RDF/XML remove in SIBs supporting it
	 * @param graph the graph to be removed
	 * @return the SSAP  message to be sent to the SIB to perform RDF/XML remove in SIBs supporting it
	 * optimized for OSGI-SIB
	 */
	public String remove_rdf_xml_2(String graph)
	{return 
			"<SSAP_message>"
			+"<transaction_type>REMOVE</transaction_type>"
			+"<message_type>REQUEST</message_type>"
			+"<transaction_id>"+ ++transaction_id +"</transaction_id>"
			+"<node_id>"+nodeID+"</node_id>" //+"<node_id>{"+nodeID+"}</node_id>"
			+"<space_id>"+ SMART_SPACE_NAME +"</space_id>"
			+"<parameter name=\"remove_graph\"  encoding=\"RDF-XML\">"
			+ "<![CDATA["
			+(graph.replaceAll("[^\\x20-\\x7e]", "??"))
		    +"]]>"
			+"</parameter>"
			+"<parameter name = \"confirm\">TRUE</parameter>"
			+"</SSAP_message>";
	}//String remove(String graph) 

	/**
	 * Method to obtain an object representing the response to a SPARQL query starting from the SSAP message including it  
	 * @param xml the SSAP message received by the SIB and containing a SPARQL response XML document
	 * @return a SSAP_sparql_response object containing the results of the query performed
	 */
	public SSAP_sparql_response get_SPARQL_query_results(String xml)
	{
		return new SSAP_sparql_response(xml);
	}

	/**
	 * Method to obtain an object representing the response to a SPARQL query representing the new results of a SPARQL subscription
	 * @param xml indication sent by the SIB
	 * @return an object representing the response to a SPARQL query representing the new results of a SPARQL subscription
	 */
	public SSAP_sparql_response get_SPARQL_indication_new_results(String xml)
	{
		if(xml==null)
		{
			System.out.println("ERROR:SSAP_XMLTools:get_SPARQL_indication:XML message is null");
			return null;
		}

		Document doc=null;
		try {
			doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		if(doc==null)
		{
			System.out.println("ERROR:SSAP_XMLTools:get_SPARQL_indication:doc is null");
			return null;
		}

		//SSAP_sparql_response out = new SSAP_sparql_response();

		Element root = doc.getRootElement();
		Namespace ns = root.getNamespace();

		List<Element> parameters = root.getChildren("parameter", ns);
		int ip;
		for(ip=0;ip<parameters.size();ip++)
		{
			if( parameters.get(ip).getAttributeValue("name").equalsIgnoreCase("new_results"))
			{
				break;
			}
		}
		if(!(ip<parameters.size())) 
		{
			System.out.println("ERROR:SSAP_XMLTools:get_SPARQL_indication:(results) not found!");
			return null;
		}

		Element results = null;
		Element e;
		List<Element> results_elements = parameters.get(ip).getChildren();
		Iterator<Element> result_it = results_elements.iterator();
		while (result_it.hasNext())
		{
			e = result_it.next();
			if(e.getName().equals("sparql"))
			{
				results = e;
			}
		}
		if(results==null)
		{
			return null;
		}
		return new SSAP_sparql_response(results);
	}
	/**
	 * Method to obtain an object representing the response to a SPARQL query representing the obsolete results of a SPARQL subscription
	 * @param xml indication sent by the SIB
	 * @return an object representing the response to a SPARQL query representing the new obsolete results of a SPARQL subscription
	 */
	public SSAP_sparql_response get_SPARQL_indication_obsolete_results(String xml)
	{
		if(xml==null)
		{
			System.out.println("ERROR:SSAP_XMLTools:getQueryTriple:XML message is null");
			return null;
		}

		Document doc=null;
		try {
			doc = builder.build(new ByteArrayInputStream(xml.getBytes()));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 

		if(doc==null)
		{
			System.out.println("ERROR:SSAP_XMLTools:getQueryTriple:doc is null");
			return null;
		}

		//SSAP_sparql_response out = new SSAP_sparql_response();

		Element root = doc.getRootElement();
		Namespace ns = root.getNamespace();

		List<Element> parameters = root.getChildren("parameter", ns);
		int ip;
		for(ip=0;ip<parameters.size();ip++)
		{
			if( parameters.get(ip).getAttributeValue("name").equalsIgnoreCase("obsolete_results"))
			{
				break;
			}
		}
		if(!(ip<parameters.size())) 
		{
			System.out.println("ERROR:SSAP_XMLTools:getQueryTriple:parameter(results) not found!");
			return null;
		}

		Element results = null;
		Element e;
		List<Element> results_elements = parameters.get(ip).getChildren();
		Iterator<Element> result_it = results_elements.iterator();
		while (result_it.hasNext())
		{
			e = result_it.next();
			if(e.getName().equals("sparql"))
			{
				results = e;
			}
		}
		if(results==null)
		{
			//	System.out.println("Warning:SSAP_XMLTools:getQueryTriple:get_SPARQL_indication:(results) not found!");
			return null;
		}
		return new SSAP_sparql_response(results);
	}

	public boolean isResponseConfirmed(SIBResponse response)
	{
		return response.Status.equalsIgnoreCase("m3:success");
	}



}//public class SSAP_XMLTools extends KpCore 

