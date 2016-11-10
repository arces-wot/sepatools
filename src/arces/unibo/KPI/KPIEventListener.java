/**
 * KPIEventistener is the base class to handle SIB event messages.
 * Extend this class to have an ad-hoc behaviour in the subscription handler method. 
 * <p>
 * <p>
 * 
 * @author      Daniele Manzaroli - VTT-Oulu, Technical Research Centre of Finland. ARCES, University of Bologna, Italy
 * @version     %I%, %G%
 */



package arces.unibo.KPI;

import java.util.Vector;

public class KPIEventListener extends KPICore implements /*iKPIC,*/ iKPIC_subscribeHandler{
	
	
    //Default configuration parameters
    //SIB connection parameters
    static String HOST = "127.0.0.1";
    static int    PORT = 10010; 
    static String SSNM = "X"; 


	/**
	 * SSAP tools to handle SSAP messages 
	 */
	SSAP_XMLTools xmlTools=null;
	
	
	/**
	 * Data structure to store one subscription information 
	 *
	 */
	private class subscriptionInfoStructure{
		String subscriptionID;
		iKPIC_subscribeHandler subscribeHandler;
	}
	
	
	/**
	 * All subscription information store
	 *
	 */	
	Vector<subscriptionInfoStructure> subscriptionMemo=new Vector<subscriptionInfoStructure>();
	
	
    /**
     * Constructor
     * 
     * @param  HOST  the IP address of the SIB
     * @param  PORT the port of the SIB
     * @param  SMART_SPACE_NAME the smart space name
     * 
     */
	public KPIEventListener(String HOST, int PORT, String SMART_SPACE_NAME)
	{
		/**
		 * EDIT: LINE 64 IS AN EXTRA LINE ADDED TO AVOID COMPILATION ERROR
		 */
		super(HOST, PORT, SMART_SPACE_NAME);
		xmlTools = new SSAP_XMLTools();
//		this.HOST=HOST;
//		this.PORT=PORT;
//		this.SSNM=SMART_SPACE_NAME;
		
	}//KPIEventListener()
	
	
	@Override
    public void kpic_SIBEventHandler(String xml)
    {System.out.println("\n[NEW EVENT(EventListener)]___________________________________");
     
     String subscriptionId=xmlTools.getSubscriptionID(xml);
     
     for(int i=0;i<subscriptionMemo.size();i++)
       {if(subscriptionMemo.get(i).subscriptionID.equals(subscriptionId))
    	   if(subscriptionMemo.get(i).subscribeHandler!=null)
    		   subscriptionMemo.get(i).subscribeHandler.kpic_SIBEventHandler(xml);
       }//for(int i=0;i<subscriptionMemo.size();i++)
     
     System.out.println("\n[Default event handler(EventListener)]___________________________________");
     kpic_SIBEventHandler(xml);
     
    }//public void kp_sib_event(String xml)

    /**
     * Perform the SUBSCRIBE procedure 
     * The default event handler will be use
     * Check the error state with the functions: getErrMess, getErrID
     * @param s the string representation of the subject. The value 'null' means any value
     * @param p the string representation of the predicate. The value 'null' means any value
     * @param o the string representation of the object. The value 'null' means any value
     * @param o_type the string representation of the object type. Allowed values are: uri, literal
     * 
     * @return a null value string in case of error otherwise an empty string 
     */

	public boolean addRDFsubscription(String s,String p,String o,String o_type)
	{
     return addRDFsubscription( s, p, o, o_type, null);		
	}//public boolean addRDFquery(String s,String p,String o,String o_type)
		
	
    /**
     * Perform the SUBSCRIBE procedure 
     * Check the error state with the functions: getErrMess, getErrID
     * @param s the string representation of the subject. The value 'null' means any value
     * @param p the string representation of the predicate. The value 'null' means any value
     * @param o the string representation of the object. The value 'null' means any value
     * @param o_type the string representation of the object type. Allowed values are: uri, literal
     * @param sh the subscription handler. If null the default handler will be use
     * 
     * @return a null value string in case of error otherwise an empty string 
     */

	public boolean addRDFsubscription(String s,String p,String o,String o_type,iKPIC_subscribeHandler2 sh)
	{
	 new KPICore(HOST,PORT,SSNM);	
		
     return false;		
	}//public boolean addRDFquery(String s,String p,String o,String o_type)
		
	
	
}
