/**
 * iKPICore is the base class for all java KPI application
 * which allow an application to comunicate in M£ with the SIB.
 * In this class there is not any functionality to handle the XML 
 * messages from the SIB 
 * <p>
 * <p>
 * This class is intended to be a link to the M3 infrastructure.
 * This class export methods to be able to send and receive messages 
 * from and to the SIB.
 * <p>
 * 
 * @author      Daniele Manzaroli - VTT-Oulu, Technical Research Centre of Finland. ARCES, University of Bologna, Italy
 * @version     %I%, %G%
 */

package arces.unibo.KPI;

import java.io.*;
import java.net.*;
import java.util.*;

import arces.unibo.SEPA.Logger;
import arces.unibo.SEPA.Logger.VERBOSITY;

public class KPICore implements iKPIC, iKPIC_subscribeHandler2,  iKPIC_subscribeHandler
{

	private Socket         kpSocket = null;
	private PrintWriter    out      = null;
	private BufferedReader in       = null;
	//private InputStream    is 	= null;
	public String         HOST     = null;
	public int            PORT     = -1;
	public String         SMART_SPACE_NAME=null;

	public static String   S_HOST     = null;    //"S_" means STATIC!
	public static int      S_PORT     = -1;
	public static String   S_SMART_SPACE_NAME=null;

	private int protocol_version = 0;

	public int getProtocol_version() {
		return protocol_version;
	}

	public void setProtocol_version(int protocol_version) {
		this.protocol_version = protocol_version;
	}

	private final int      SOCKET_TIMEOUT_DELAY = 1000000;//5000

	/**
	 * SSAP specification protocol element
	 */
	public  String nodeID="00000000-0000-0000-0000-DEAD0000BEEF";


	/**
	 * Set the node ID
	 * @param newNodeID
	 */
	public void setNodeID(String newNodeID)
	{
		this.nodeID          = newNodeID;
		this.xmlTools = new SSAP_XMLTools();
		this.xmlTools.nodeID = newNodeID;
	}


	private String ANYURI="http://www.nokia.com/NRC/M3/sib#any";

	/**
	 * SSAP tools to handle SSAP messages 
	 */
	public SSAP_XMLTools xmlTools=null;


	/**
	 * The SIB event handler specified by the user
	 */
	private iKPIC_subscribeHandler2 eventHandler_v2=null; //event handler
	protected iKPIC_subscribeHandler2 getEventHanlder2() {
		return eventHandler_v2;
	}

	@Override
	public void setEventHandler2(iKPIC_subscribeHandler2 eh2) {
		this.eventHandler_v2 = eh2;
	}


	private iKPIC_subscribeHandler eh=null; //event handler
	/**
	 * Thread to handle incoming events
	 */
	public Thread eventThread=null;

	/**
	 * This is just an event counter, it is completely pointless.
	 * It is used to DEMO purpose
	 */
	//private int    event_counter=0;


	/**
	 * Constants for use of protection at triple level. their use is currently not fully supported
	 */
	private final String AR_NameSpace = "http://ProtectionOntology.org";
	private final String AR_Property  = AR_NameSpace+"#Has_Access_Restriction"; //FIXME This will be changed (AR = access restriction)
	private final String AR_Owner     = AR_NameSpace+"#Has_Owner";              //FIXME This will be changed (AR = access restriction)
	private final String AR_Target    = AR_NameSpace+"#Has_Target";             //FIXME This will be changed (AR = access restriction)


	/**
	 * This variable store the last error message code.
	 * Users can use this code to understand what occurs.
	 * Negative value means ERROR!!!
	 */
	private int KP_ERROR_ID=0;

	/**
	 * Error message code list
	 */

	final int ERR_NO_ERROR = 0;
	final int ERR_Conected = 1;
	final int ERR_UnknownHost = -2;
	final int ERR_IOException = -3;

	final int ERR_ConnectionClose=4;
	final int ERR_EXC_CLOSE_CONN = -5;

	final int ERR_MsgSent=6;
	final int ERR_MsgSent_EXC_CONN=-7;
	final int ERR_MsgSent_CONN_UNAVAILABLE =-8;
	final int ERR_RECEIVE_FAIL = -9;
	final int ERR_SOCKET_TIMEOUT=-10;   
	final int ERR_EVENT_HANDLER_NULL=-11; 
	final int ERR_Subscription_DONE=12;
	final int ERR_Subscription_NOT_DONE=-13;

	final int ERR_InsertProtection_FAIL=-14;
	final int ERR_RemoveProtection_FAIL=-15;

	/**
	 * Error message explanation
	 */
	String ERR_MSG[]={
			"@NO ERROR"
			,"@Conected"
			,"@ERR_UnknownHost"
			,"@ERR_IOException" 

			,"@ConnectionClose"
			,"@ERR_EXC_CLOSE_CONN"

			,"@MsgSent"
			,"@ERR_MsgSent_EXC_CONN"
			,"@ERR_MsgSent_CONN_UNAVAILABLE"
			,"@ERR_RECEIVE_FAIL"
			,"@ERR_SOCKET_TIMEOUT"
			,"@ERR_EVENT_HANDLER_NULL"

			,"@Subscription DONE"
			,"@ERR_Subscription NOT DONE"
			,"@ERR_InsertProtection_FAIL"
			,"@ERR_RemoveProtection_FAIL"

	};


	/**
	 * Constructor
	 * 
	 * @param  HOST  the IP address of the SIB
	 * @param  PORT the port of the SIB
	 * @param  SMART_SPACE_NAME the smart space name
	 * 
	 */

	public KPICore(String HOST,int PORT,String SMART_SPACE_NAME)
	{
		this.HOST=HOST;
		this.PORT=PORT;
		this.SMART_SPACE_NAME=SMART_SPACE_NAME;
		this.nodeID=""+UUID.randomUUID();

		this.xmlTools = new SSAP_XMLTools(nodeID,SMART_SPACE_NAME,this.ANYURI);
	}//public KpCore(String HOST,int port)

	/**
	 * 
	 * 
	 */

	private boolean PRINT_ALL_ERR=false;
	private boolean PRINT_ALL_DEBUG=false;
	private String tag ="KPICore";

	/**
	 * print msg if error printing is not disabled
	 * @param msg
	 */
	void err_print(String msg)
	{ if(PRINT_ALL_ERR)System.out.println(msg);
	}//void err_print(String mess)

	/**
	 * Print msg if debug is not disabled
	 * @param msg
	 */
	void deb_print(String msg)
	{ if(PRINT_ALL_DEBUG)System.out.println(msg);
	}//void err_print(String mess)


	public void enable_error_message(){PRINT_ALL_ERR=true;}
	public void enable_debug_message(){PRINT_ALL_DEBUG=true;}
	public void disable_error_message(){PRINT_ALL_ERR=false;}
	public void disable_debug_message(){PRINT_ALL_DEBUG=false;}

	/**
	 * Establish a socket connection to the SIB
	 * 
	 * @return return an error code
	 */
	int openConnect()
	{
		//----------------------------------------------
		try
		{
			kpSocket = new Socket( HOST , PORT );
			/*TO TEST  WITH WINDOWS*/               
			kpSocket.setKeepAlive(true);
			
			out = new PrintWriter(kpSocket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(kpSocket.getInputStream()));
			
			//TODO: questa sotto non ha senso...l'ho commentata
			//is = kpSocket.getInputStream();
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host");
			return this.ERR_UnknownHost;
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection");
			return this.ERR_IOException;
		}	

		return this.ERR_Conected;
	}//int connect()


	/**
	 * Send a pre-formed XML message to the SIB
	 * 
	 * @param  msg the string representation of the XML message to send. null value are not allowed.
	 * @return return an error code 
	 */
	int send(String msg)
	{
		if(this.kpSocket.isOutputShutdown()) 
			return ERR_MsgSent_CONN_UNAVAILABLE;

		out.println(msg);

		try
		{ 
			if(msg.indexOf("SUBSCRIBE")<0) {/*deb_print(  "[ATTENTION! THE FOLLOWING OPERATION IS NOT PERFORMED DUE TO BE ABLE TO USE A SSH TUNNEL]\n"
    				                                      +"[REF:KPICore.java:send(String msg){}]\n"
    				                                      +" this.kpSocket.shutdownOutput();");
			 */this.kpSocket.shutdownOutput();
			}
			else deb_print("\n*** KpCore:send:shutdownOutput:OPERATION_NOT_TO_BE_DONE:this.kpSocket.shutdownOutput() \n\nSSAP MSG:"+msg);  
		}
		catch(Exception e) 
		{ err_print("KpCore:send:shutdownOutput:EXCEPTION:"+e);
		return ERR_MsgSent_EXC_CONN;
		}

		return this.ERR_MsgSent;
	}//int send(String msg)       


	/**
	 * Close the socket connection with the SIB 
	 * 
	 * @return return an error code 
	 */

	int closeConnection()
	{ 
		try
		{
			out.close();
			in.close();
			kpSocket.close();
		}
		catch(Exception e)
		{err_print("KpCore:closeConnection:Exception:\n"+e);	return this.ERR_EXC_CLOSE_CONN;}	
		return ERR_ConnectionClose;
	}//int closeConnect()


	/**
	 * Method that wait until a message is received from the SIB 
	 * 
	 * @return return a string representation of the 
	 *         XML message received from the SIB. A null value means 
	 *         that an error occurs. In this case, check the error state
	 *         with the functions: getErrMess, getErrID          
	 *          
	 * @throws java.net.SocketException         
	 */

	String receive() throws Exception 
	{  
		String ret=null;
		this.kpSocket.setSoTimeout(SOCKET_TIMEOUT_DELAY);

	try
	{ 
		//ret = this.in.readLine();
		ret=this.readSSAPMessage();
	}
	//catch(java.net.SocketTimeoutException e)
	//{this.KP_ERROR_ID=ERR_SOCKET_TIMEOUT; return null;}
	catch(Exception e)
	{this.KP_ERROR_ID=ERR_SOCKET_TIMEOUT; return null;}

	return ret;
	}//String receive() throws Exception


	//---------------------------------------------------


	/**
	 *  Reads from the socket connected with the SIB
	 * @return the String read
	 */
	String readSSAPMessage()
	{
		StringBuilder builder = new StringBuilder();
		char[] buffer = new char[4 * 1024];
		String msg = "";
		int charRead = 0;
		
		try
		{
			while ((charRead = in.read(buffer, 0, buffer.length)) != -1) 
			{
				//LR: exclude ping byte
				if (charRead == 1 && buffer[0] == Constants.PING) {
					Logger.log(VERBOSITY.DEBUG, tag+":readSSAPMessage", "Ping");
					continue;
				}
				
				builder.append(buffer, 0 , charRead);
			}
			msg = builder.toString();

			if(msg.contains("<SSAP_message>") && msg.contains("</SSAP_message>") )
			{	
				int start = msg.indexOf("<SSAP_message>");
				int stop = msg.indexOf("</SSAP_message>") + 15;
				
				//The first message is not an event but just the confirmation message!!!
				if(this.xmlTools.isSubscriptionConfirmed(msg.substring(start,stop)))
				{
					this.KP_ERROR_ID=this.ERR_Subscription_DONE;

					startEventHandlerThread(kpSocket,in,out);

					return msg.substring(start,stop);
				}

				else
				{
					/*flush all chars in the message instead:break;*/
					deb_print("KpCore:readByteXByteSIBmsg:SSAP message recognized");
				}
			}//if(tmp_msg.startsWith("<SSAP_message>") && tmp_msg.endsWith("</SSAP_message>"))
			deb_print("KpCore:readByteXByteSIBmsg:READ LOOP TERMINATED");
			closeConnection();


		}catch(Exception e)
		{
			err_print("KPICore:readByteXByteSIBmsg:Exception on EVENT HANDLER:RECEIVE:\n"+e);
			this.KP_ERROR_ID=this.ERR_SOCKET_TIMEOUT;
		}

		//       		return responseData.toString();
		return msg;
	}

	public void sendSSAPMsgOneWay(String msg)
	{
		int err=0;

		deb_print("KpCore:message to send:_"+msg.replace("\n", "")+"_");
		deb_print("KpCore:SSAP:Open connection...");

		if((err = openConnect())  <0)
		{
			err_print("KpCore:SSAP:ERROR:"+ERR_MSG[(err*-1)] ); 
			KP_ERROR_ID=err; 
		}
		else 
		{
			deb_print("KpCore:SSAP:"+ERR_MSG[(err)] );
		}

		deb_print("KpCore:SSAP:Send the message...");
		if((err =send( msg     ))  <0)
		{
			err_print("KpCore:SSAP:ERROR:"+ERR_MSG[(err*-1)] );
			KP_ERROR_ID=err; 
		}
		else
		{
			deb_print("KpCore:SSAP:"+ERR_MSG[(err)] );
		}
	}

	/**
	 * The SSAP protocol is based on some operation to do atomically:<p/>
	 * - open the connection<br/>
	 * - send a message<br/>
	 * - wait for the answer<br/>
	 * <p/>  
	 * This method encapsulate all these operations
	 * <p/>  
	 * Send a pre-formed XML message to the SIB
	 * 
	 * @param  msg the string representation of the XML message to send. null value are not allowed.
	 * @return return a string representation of the 
	 *         XML message received from the SIB. A null value means 
	 *         that an error occurs. In this case, check the error state
	 *         with the functions: getErrMess, getErrID          
	 */
	String sendSSAPMsg(String msg)
	{ 
		String ret="";
		int err=0;

		deb_print("KpCore:message to send:_"+msg.replace("\n", "")+"_");
		deb_print("KpCore:SSAP:Open connection...");

		if((err = openConnect())  <0)
		{
			err_print("KpCore:SSAP:ERROR:"+ERR_MSG[(err*-1)] ); 
			KP_ERROR_ID=err; return null;
		}
		else 
		{
			deb_print("KpCore:SSAP:"+ERR_MSG[(err)] );
		}

		deb_print("KpCore:SSAP:Send the message...");
		if((err =send( msg     ))  <0)
		{
			err_print("KpCore:SSAP:ERROR:"+ERR_MSG[(err*-1)] );
			KP_ERROR_ID=err; return null;
		}
		else
		{
			deb_print("KpCore:SSAP:"+ERR_MSG[(err)] );
		}

		deb_print("KpCore:SSAP:Read the answer...");

		//TODO: no answer for unsubscribe?
		if(msg.indexOf("<transaction_type>UNSUBSCRIBE</transaction_type>")<0)
			try
		{ 
				ret = receive();
				deb_print("KpCore:SSAP:answer:"+"ok _"+ret.replace("\n", "")+"_\n");
		}
		catch(Exception e)
		{
			err=this.KP_ERROR_ID;
			if(err<0) 
			{
				err_print("KpCore:SSAP:ERROR:"+ERR_MSG[(err*-1)] );
			}
			else
			{
				err=ERR_RECEIVE_FAIL;
				if(err<0) 
				{
					err_print("KpCore:SSAP:ERROR:"+ERR_MSG[(err*-1)] );
				}
			}
			err_print("EXCEPTION: "+e.toString());
			e.printStackTrace();
			KP_ERROR_ID=err; 
			return null;
		}

		//if(msg.indexOf("")>=0)
		else 
		{
			deb_print("KpCore:SSAP:UnSubscription Request:no answer expected:"+"ok"+"\n"); 
		}

		deb_print("KpCore:SSAP:Close connection...");
		if((err= closeConnection()) <0) 
		{
			err_print("KpCore:SSAP:ERROR:"+ERR_MSG[(err*-1)] ); 
			KP_ERROR_ID=err;
			return null;
		}

		if(this.KP_ERROR_ID>=0)
		{
			this.KP_ERROR_ID=err;
		}
		return ret;
	}//String SSAP(String msg)



	/**
	 * This method return the string representation of the last
	 * operation result .
	 * It is possible to know the final state of each operation 
	 * by calling this method.
	 * 
	 * @return return a string explanation message for the last error occurred
	 */

	String getErrMess()
	{  
		if(KP_ERROR_ID<0)
		{
			return "ERROR MESSAGE:"+ERR_MSG[(KP_ERROR_ID *-1)];
		}
		else     
		{
			return "SERVICE NOTE:"+ERR_MSG[(KP_ERROR_ID    )];
		}
	}//String getErrMess()

	/**
	 * This method return the error ID of the last operation result.
	 * 
	 * @return return the ID for the last error occurred, negative value means error
	 */

	int getErrID()
	{  
		return KP_ERROR_ID;
	}//String getErrID()


	/**
	 * Method to freeze the application 
	 * 
	 * @param msec milliseconds to sleep 
	 */

	public void Sleep(final int msec)
	{
		Thread x = new Thread()
		{ 
			public void run() 
			{
				try 
				{
					sleep(msec);
				}
				catch (InterruptedException e)
				{
					System.out.println("[sleep exception!]"+e);
				}                  
			}
		};
		x.start();     
		try
		{
			x.join(); 
		}
		catch(Exception e)
		{

		}
	}//public void Sleep(final int msec)


	/****************************************************\

         Event handler 

       \****************************************************/ 

	/**
	 * Set the event SIB handler
	 * @param eh an object that implement the kp_subscribeHandler interface
	 * 
	 * @see sofia_kp_old.iKPIC_subscribeHandler#kpic_SIBEventHandler(java.lang.String) "SIB Event handler"
	 */
	@Override
	@Deprecated
	public void setEventHandler(iKPIC_subscribeHandler eh)
	{
		this.eh=eh;    	
	}//public String setEventHandler(kp_subscribeHandler sh)


	/**
	 * Simple implementation of the event handler
	 * @param xml the string representation of the XML event message received from the SIB
	 * @see sofia_kp.iKPIC_subscribeHandler#kpic_SIBEventHandler(java.lang.String) "SIB Event handler"
	 */
	//	@Override
	//	public void kpic_SIBEventHandler(String xml)
	//	{System.out.println("\n[EVENT(KpCore)]___________________________________");
	//	System.out.println("("+ this.event_counter++ +")EVENT:\n"+xml);      
	//	}//public void kp_sib_event(String xml)
	//



	/****************************************************\

           Following the available SIB operations

       \****************************************************/ 


	/**
	 * Perform the JOIN procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * @return a string representation of the XML answer message from the SIB
	 */
	public SIBResponse join()
	{
		deb_print("\n[JOIN]___________________________________"); 
		return new SIBResponse( sendSSAPMsg( this.xmlTools.join() ));
	}//String join()


	/**
	 * Perform the LEAVE procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public SIBResponse leave()
	{
		deb_print("\n[LEAVE]___________________________________");
		return new SIBResponse(sendSSAPMsg( this.xmlTools.leave() ));
	}//String leave()



	/**
	 * Perform the RDF-QUERY procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param s the string representation of the subject. The value 'null' means any value
	 * @param p the string representation of the predicate. The value 'null' means any value
	 * @param o the string representation of the object. the value 'null' means any value
	 * @param s_type the string representation of the subject type. Allowed values are: uri, literal
	 * @param o_type the string representation of the object type. Allowed values are: uri, literal
	 * 
	 *  
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public SIBResponse queryRDF(String s,String p,String o, String s_type, String o_type)
	{
		deb_print("\n[QUERY RDF]___________________________________");
		return new SIBResponse(sendSSAPMsg( this.xmlTools.queryRDF(s, p, o, s_type, o_type) ));
	}//String queryRDF()

	/**
	 * Perform the RDF Query procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param queryList It is a structure to store every triple. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the subject type
	 * -the object type
	 * 
	 *  A null value for subject, predicate or object means any value
	 *  
	 * @return a string representation of the XML answer message from the SIB 
	 */     
	public SIBResponse queryRDF( Vector<Vector<String>> queryList )
	{
		deb_print("\n[QUERY RDF]___________________________________");
		return new SIBResponse(sendSSAPMsg( this.xmlTools.queryRDF(queryList) )); 
	}//public String queryRDF( Vector<Vector<String>> queryList )


	/* * * * * * * * * * *\
          WILBUR QUERY STUFF
       \* * * * * * * * * * */


	/**
	 * @deprecated
	 * Perform the WQL Values query procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 *     
	 * @param startNodeURI the string representation of the node URI
	 * @param type the string representation of the node URI type. Can be "literal" or "URI"
	 * @param path the string representation of the WQL graph path
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String queryWQL_VALUES(String startNodeURI,String type,String path)
	{deb_print("\n[QUERY WQL_VALUES]___________________________________");
	return sendSSAPMsg( this.xmlTools.queryWQL_VALUES(startNodeURI, type, path) ); 
	}//public String queryWQL_VALUES(String startNodeURI,String path)

	/**
	 * @deprecated
	 * Perform the WQL related query procedure 
	 * Check the error state with the functions: getErrMess, getErrID
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
	{deb_print("\n[QUERY WQL_RELATED]___________________________________");
	return sendSSAPMsg( this.xmlTools.queryWQL_RELATED(startNodeURI,startType, endNodeURI,endType, path) ); 
	}//public String queryWQL_RELATED(String startNodeURI, String endNodeURI, String path)


	/**
	 * @deprecated
	 * Perform the WQL node-type query procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param nodeURI the string representation of the node URI
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String queryWQL_NODETYPES(String nodeURI)
	{deb_print("\n[QUERY WQL_NODETYPES]___________________________________");
	return sendSSAPMsg( this.xmlTools.queryWQL_NODETYPES(nodeURI) ); 
	}//public String queryWQL_NODETYPES(String nodeURI)


	/**
	 * @deprecated
	 * Perform the WQL is-type query procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param nodeURI the string representation of the node URI
	 * @param nodeType the string representation of the node URI type. Can be "literal" or "URI"
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String queryWQL_ISTYPES(String nodeURI, String nodeType)
	{deb_print("\n[QUERY WQL_ISTYPES]___________________________________");
	return sendSSAPMsg( this.xmlTools.queryWQL_ISTYPES(nodeURI, nodeType) ); 
	}//public String queryWQL_ISTYPES(String nodeURI, String nodeType)


	/**
	 * @deprecated
	 * Perform the WQL is-sub-type query procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param subClassNodeURI the string representation of a sub-class node URI
	 * @param superClassNodeURI the string representation of a super-class node URI
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public String queryWQL_ISSUBTYPE(String subClassNodeURI, String superClassNodeURI)
	{deb_print("\n[QUERY WQL_ISSUBTYPE]___________________________________");
	return sendSSAPMsg( this.xmlTools.queryWQL_ISSUBTYPE(subClassNodeURI, superClassNodeURI) ); 
	}//public String queryWQL_ISSUBTYPE(String subClassNodeURI, String superClassNodeURI)




	/**
	 * Perform the INSERT procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param s the string representation of the subject. The value 'null' means any value
	 * @param p the string representation of the predicate. The value 'null' means any value
	 * @param o the string representation of the object. the value 'null' means any value
	 * @param s_type the string representation of the subject type. Allowed values are: uri, literal
	 * @param o_type the string representation of the object type. Allowed values are: uri, literal
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public SIBResponse insert(String s,String p,String o, String s_type, String o_type)
	{
		deb_print("\n[INSERT]___________________________________");
		return new SIBResponse(sendSSAPMsg( this.xmlTools.insert(s, p, o, s_type, o_type) ));
	}//String insert(String s,String p,String o)


	/**
	 * Perform the INSERT procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param queryList It is a structure to store every triple. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the subject type
	 * -the object type
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */     
	public SIBResponse insert( Vector<Vector<String>> queryList )
	{
		deb_print("\n[INSERT]___________________________________");
		return new SIBResponse(sendSSAPMsg( this.xmlTools.insert(queryList) ));
	}


	/**
	 * Perform the REMOVE procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param s the string representation of the subject. The value 'null' means any value
	 * @param p the string representation of the predicate. The value 'null' means any value
	 * @param o the string representation of the object. the value 'null' means any value
	 * @param s_type the string representation of the subject type. Allowed values are: uri, literal
	 * @param o_type the string representation of the object type. Allowed values are: uri, literal
	 * 
	 *  
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public SIBResponse remove(String s,String p,String o,String s_type,String o_type)
	{
		deb_print("\n[REMOVE]___________________________________");
		return new SIBResponse(sendSSAPMsg( this.xmlTools.remove(s, p, o, s_type, o_type) ));
	}//String remove(String s,String p,String o)


	/**
	 * Perform the REMOVE procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param queryList It is a structure to store every triple. Each element of 
	 * the vector contains another vector formed by five string elements :
	 * -the subject
	 * -the predicate
	 * -the object 
	 * -the subject type
	 * -the object type
	 * 
	 * A null value for subject, predicate or object means any value
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */     
	public SIBResponse remove( Vector<Vector<String>> queryList )
	{
		deb_print("\n[REMOVE]___________________________________");
		return new SIBResponse(sendSSAPMsg( this.xmlTools.remove(queryList) ));
	}   

	//       public boolean remove( Vector<Vector<String>> queryList )
	//       {deb_print("\n[REMOVE]___________________________________");
	//        String xml = sendSSAPMsg( this.xmlTools.remove(queryList) );
	//        return xmlTools.isRemoveConfirmed(xml);
	//       }       
	/**
	 * Perform the UPDATE procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * <p/>
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
	public SIBResponse update( String sn,String pn,String on,String sn_type,String on_type
			,String so,String po,String oo,String so_type,String oo_type)
	{
		deb_print("\n[UPDATE]___________________________________");
		return new SIBResponse(sendSSAPMsg( this.xmlTools.update(sn, pn, on, sn_type, on_type 
				, so, po, oo, so_type, oo_type) ));
	}//String update(String s,String p,String o)

	/**
	 * Perfor a SPARQL update on SIBs supporting it
	 * @param sparql_update a SPARQL update query
	 * @return the SIB answer
	 * @Todo This method currently supports sparql update for SIB 0.9 only with protocol version = 0 (default) 
	 */


	public SIBResponse update_sparql(String sparql_update )
	{

		deb_print("\n[UPDATE]___________________________________");
		if(protocol_version == 0)
		{
			return new SIBResponse(sendSSAPMsg( this.xmlTools.querySPARQL(sparql_update)));
		}
		else
		{
			return new SIBResponse(sendSSAPMsg( this.xmlTools.update_sparql(sparql_update)));
		}
	}//String update(String s,String p,String o)


	/**
	 * Perform the UPDATE procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * <p/>
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
	public SIBResponse update( Vector<Vector<String>>  newTripleVector
			, Vector<Vector<String>>  oldTripleVector)
	{deb_print("\n[UPDATE]___________________________________");
	return new SIBResponse(sendSSAPMsg( this.xmlTools.update(newTripleVector, oldTripleVector) ));
	}


	/**
	 * Perform a persistent sparql update, acting like a rule on the store
	 */
	public SIBResponse persistent_update( String query)
	{deb_print("\n[PERSISTENT UPDATE]___________________________________");
	return new SIBResponse(sendSSAPMsg( this.xmlTools.persistent_update(query )));
	}

	/**
	 * Remove a persistent update
	 * @param rule_id the id of the persistent update
	 * @return the SIB response message
	 */
	public SIBResponse cancel_persistent_update(String update_id)
	{
		deb_print("\n[CANCEL PERSISTENT UPDATE]___________________________________");
		return new SIBResponse(sendSSAPMsg( this.xmlTools.cancel_persistent_update(update_id )));
	}



	/**
	 * Perform the SUBSCRIBE procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * @param s the string representation of the subject. The value 'null' means any value
	 * @param p the string representation of the predicate. The value 'null' means any value
	 * @param o the string representation of the object. The value 'null' means any value
	 * @param o_type the string representation of the object type. Allowed values are: uri, literal
	 * 
	 * @return a null value string in case of error otherwise an empty string 
	 */

	@Deprecated
	public SIBResponse subscribeRDF(String s,String p,String o,String o_type)
	{
		deb_print("\n[SUBSCRIBE RDF]___________________________________"); 
		//System.out.println("DEBUG: " + this.xmlTools.subscribeRDF(s, p, o, o_type));
		return subscribe( this.xmlTools.subscribeRDF(s, p, o, o_type) );
	}//public String subscribeRDF(String s,String p,String o,String o_type)

	@Deprecated
	public SIBResponse subscribeRDF(Vector<Vector<String> > triples)
	{
		deb_print("\n[SUBSCRIBE RDF]___________________________________"); 
		//System.out.println("DEBUG: " + this.xmlTools.subscribeRDF(s, p, o, o_type));
		return subscribe( this.xmlTools.subscribeRDF(triples));
	}//public String subscribeRDF(String s,String p,String o,String o_type)


	public SIBResponse subscribeRDF(String s,String p,String o,String o_type, iKPIC_subscribeHandler2 handler)
	{
		deb_print("\n[SUBSCRIBE RDF]___________________________________"); 
		//		System.out.println("DEBUG: " + this.xmlTools.subscribeRDF(s, p, o, o_type));



		return subscribe( this.xmlTools.subscribeRDF(s, p, o, o_type ) , handler);
	}//public String subscribeRDF(String s,String p,String o,String o_type)

	public SIBResponse subscribeRDF(Vector<Vector<String> > triples, iKPIC_subscribeHandler2 handler)
	{
		deb_print("\n[SUBSCRIBE RDF]___________________________________"); 
		//System.out.println("DEBUG: " + this.xmlTools.subscribeRDF(s, p, o, o_type));
		return subscribe( this.xmlTools.subscribeRDF(triples), handler);
	}//public String subscribeRDF(String s,String p,String o,String o_type)


	/**
	 * Perform the SUBSCRIBE procedure with a sparql query
	 * @param query a sparql query
	 * @return the SIB answer
	 */

	@Deprecated
	public SIBResponse subscribeSPARQL(String query)
	{
		deb_print("\n[SUBSCRIBE SPARQL]___________________________________"); 

		return subscribe( this.xmlTools.subscribeSPARQL(query) );

	}//public String subscribeSPARQL(String query)

	public SIBResponse subscribeSPARQL(String query, iKPIC_subscribeHandler2 handler)
	{
		deb_print("\n[SUBSCRIBE SPARQL]___________________________________"); 

		return subscribe( this.xmlTools.subscribeSPARQL(query), handler );

	}//public String subscribeSPARQL(String query)


	/* * * * * * * * * * * * * *\
         WILBUR SUBSCRIPTION STUFF
       \* * * * * * * * * * * * * */

	/**
	 * @deprecated
	 * Perform the WQL-QUERY SUBSCRIPTION procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param startNodeURI the string representation of the node URI
	 * @param type the string representation of the node URI type. Can be "literal" or "URI"
	 * @param path the string representation of the WQL graph path
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	//	public String subscribeWQL_VALUES(String startNodeURI,String type,String path)
	//	{deb_print("\n[SUBSCRIPTION WQL_VALUES]___________________________________");
	//	return subscribe( this.xmlTools.subscribeWQL_VALUES(startNodeURI, type, path) ); 
	//	}//public String subscribeWQL_VALUES(String startNodeURI,String path)

	/**
	 * @deprecated
	 * Perform the WQL-QUERY SUBSCRIPTION procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param startNodeURI the string representation of the start node URI
	 * @param startType the string representation of the node URI type. Can be "literal" or "URI"
	 * @param endNodeURI the string representation of the end node URI
	 * @param endType the string representation of the node URI type. Can be "literal" or "URI"
	 * @param path the string representation of the WQL graph path
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	//	public String subscribeWQL_RELATED(String startNodeURI,String startType, String endNodeURI,String endType, String path)
	//	{deb_print("\n[SUBSCRIPTION WQL_RELATED]___________________________________");
	//	return subscribe( this.xmlTools.subscribeWQL_RELATED(startNodeURI,startType, endNodeURI,endType, path) ); 
	//	}//public String subscribeWQL_RELATED(String startNodeURI, String endNodeURI, String path)

	/**
	 * @deprecated
	 * Perform the WQL-QUERY SUBSCRIPTION procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param nodeURI the string representation of the node URI
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */    
	//	public String subscribeWQL_NODETYPES(String nodeURI)
	//	{deb_print("\n[SUBSCRIPTION WQL_NODETYPES]___________________________________");
	//	return subscribe( this.xmlTools.subscribeWQL_NODETYPES(nodeURI) ); 
	//	}//public String subscribeWQL_NODETYPES(String nodeURI)

	/**
	 * @deprecated
	 * Perform the WQL-QUERY SUBSCRIPTION procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param nodeURI the string representation of the node URI
	 * @param nodeType the string representation of the node URI type. Can be "literal" or "URI"
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */    
	//	public String subscribeWQL_ISTYPES(String nodeURI, String nodeType)
	//	{deb_print("\n[SUBSCRIPTION WQL_ISTYPES]___________________________________");
	//	return subscribe( this.xmlTools.subscribeWQL_ISTYPES(nodeURI, nodeType) ); 
	//	}//public String subscribeWQL_ISTYPES(String nodeURI, String nodeType)

	/**
	 * @deprecated
	 * Perform the WQL-QUERY SUBSCRIPTION procedure
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param subClassNodeURI the string representation of a sub-class node URI
	 * @param superClassNodeURI the string representation of a super-class node URI
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	//	public String subscribeWQL_ISSUBTYPE(String subClassNodeURI, String superClassNodeURI)
	//	{deb_print("\n[SUBSCRIPTION WQL_ISSUBTYPE]___________________________________");
	//	return subscribe( this.xmlTools.subscribeWQL_ISSUBTYPE(subClassNodeURI, superClassNodeURI) ); 
	//	}//public String subscribeWQL_ISSUBTYPE(String subClassNodeURI, String superClassNodeURI)





	/**
	 * Private method to start the subscription event handler
	 * 
	 * @param msg SSAP message to send to the SIB
	 * 
	 * @return null in case of error otherwise the SIB answer to the 
	 * subscription request
	 */
	private SIBResponse subscribe(String msg, iKPIC_subscribeHandler2 handler)
	{
		deb_print("KpCore:subscribe method");
		int ret=0;
		//		System.out.println("HERE!");
		if(handler==null)
		{   this.KP_ERROR_ID=this.ERR_EVENT_HANDLER_NULL;
		err_print("EVENT HANDLER IS NULL!!!"); 

		return null;
		}

		deb_print("KpCore:SSAP:XML MESSAGE:\n"+msg);
		deb_print("KpCore:SSAP:Open connection...");

		ret=openConnect(); 
		if(ret!=this.ERR_Conected )
		{

			this.KP_ERROR_ID=ret;
			err_print("ERROR:subscribeRDF:connection error:" + this.getErrMess()); 
			return null;
		}

		deb_print("KpCore:SSAP:Send message...");
		ret=send( msg );   
		if(ret!=this.ERR_MsgSent  )
		{

			this.KP_ERROR_ID=ret;
			err_print("ERROR:subscribeRDF:send error:"+this.getErrMess()); 
			return null;
		}

		deb_print("KpCore:SSAP:Message Sent...");

		//****************************
		int buffsize= 4 *1024;

		StringBuilder builder = new StringBuilder();


		char[] buffer = new char[buffsize];
		msg = "";
		int charRead =0;

		try
		{
			while ((charRead = in.read(buffer, 0, buffer.length)) != (-1)) 
			{
				//LR: exclude ping byte
				if (charRead == 1 && buffer[0] == Constants.PING) {
					Logger.log(VERBOSITY.DEBUG, tag+":subscribe", "Ping");
					continue;
				}
				
				builder.append(buffer, 0 , charRead);
				//  System.out.println("Into while msg=" + builder.toString());

				msg = builder.toString();

				if(msg.contains("<SSAP_message>") && msg.contains(  "</SSAP_message>") )
				{	//The first message is not an event but just the confirmation message!!!
					if(this.xmlTools.isSubscriptionConfirmed(msg))
					{
						this.KP_ERROR_ID=this.ERR_Subscription_DONE;

						kpSocket.setKeepAlive(true);

						//Subscription s = new Subscription(kpSocket, handler);						
						new Subscription(kpSocket, handler);
						
						//startEventHandlerThread(kpSocket,in,out);
						int start = msg.indexOf("<SSAP_message>");
						int stop = msg.indexOf("</SSAP_message>") + 15;
						
						return new SIBResponse(msg.substring(start,stop));
					}//if(this.xmlTools.isSubscriptionConfirmed(ret))
					else 
					{ 
						System.out.println("[90] UNKNOW MESSAGE:"+msg);
						break;
					}

				}//if(tmp_msg.startsWith("<SSAP_message>") && tmp_msg.endsWith("</SSAP_message>"))
			}     //if(tmp_msg.startsWith("<SSAP_message>") && tmp_msg.endsWith("</SSAP_message>"))
			deb_print("KpCore:readByteXByteSIBmsg:READ LOOP TERMINATED");
			closeConnection();

		}catch(Exception e)
		{

			err_print("KPICore:readByteXByteSIBmsg:Exception on EVENT HANDLER:RECEIVE:\n"+e);
			this.KP_ERROR_ID=this.ERR_SOCKET_TIMEOUT;
		}

		this.KP_ERROR_ID=this.ERR_Subscription_NOT_DONE;

		deb_print("KpCore:SSAP:Message received:("+msg+")");

		return new SIBResponse(msg);

	}//private String subscribeRDF(String msg)



	/**
	 * Private method to start the subscription event handler
	 * 
	 * @param msg SSAP message to send to the SIB
	 * 
	 * @return null in case of error otherwise the SIB answer to the 
	 * subscription request
	 */
	private SIBResponse subscribe(String msg)
	{
		deb_print("KpCore:subscribe method");
		int ret=0;

		if(eventHandler_v2==null)
		{   this.KP_ERROR_ID=this.ERR_EVENT_HANDLER_NULL;
		err_print("EVENT HANDLER IS NULL!!!"); 
		return null;
		}

		deb_print("KpCore:SSAP:XML MESSAGE:\n"+msg);
		deb_print("KpCore:SSAP:Open connection...");

		ret=openConnect(); 
		if(ret!=this.ERR_Conected )
		{
			this.KP_ERROR_ID=ret;
			err_print("ERROR:subscribeRDF:connection error:" + this.getErrMess()); 
			return null;
		}

		deb_print("KpCore:SSAP:Send message...");
		ret=send( msg );   
		if(ret!=this.ERR_MsgSent  )
		{
			this.KP_ERROR_ID=ret;
			err_print("ERROR:subscribeRDF:send error:"+this.getErrMess()); 
			return null;
		}

		deb_print("KpCore:SSAP:Message Sent...");

		//****************************
		int buffsize= 4 *1024;

		StringBuilder builder = new StringBuilder();


		char[] buffer = new char[buffsize];
		msg = "";
		int charRead =0;
		try
		{
			while ((charRead = in.read(buffer, 0, buffer.length)) != (-1)) 
			{
				//LR: exclude ping byte
				if (charRead == 1 && buffer[0] == Constants.PING) {
					Logger.log(VERBOSITY.DEBUG, tag+":subscribe", "Ping");
					continue;
				}
				
				builder.append(buffer, 0 , charRead);
				//  System.out.println("Into while msg=" + builder.toString());

				msg = builder.toString();



				if(msg.contains("<SSAP_message>") && msg.contains(  "</SSAP_message>") )
				{	//The first message is not an event but just the confirmation message!!!
					if(this.xmlTools.isSubscriptionConfirmed(msg))
					{
						this.KP_ERROR_ID=this.ERR_Subscription_DONE;

						/*
						 * Event Handler Thread stuff 
						 */
						/*t_kpSocket = kpSocket;
                 	    t_out      = out;
                 	    t_in       = in;*/

						deb_print("KpCore:subscribe:event message recognized:_"+msg.replace("\n", "")+"_");

						startEventHandlerThread(kpSocket,in,out);

						int start = msg.indexOf("<SSAP_message>");
						int stop = msg.indexOf("</SSAP_message>") + 15;
						
						return new SIBResponse(msg.substring(start,stop));
					}//if(this.xmlTools.isSubscriptionConfirmed(ret))
					else 
					{ 
						System.out.println("[90] UNKNOW MESSAGE:"+msg);
						break;
					}

				}//if(tmp_msg.startsWith("<SSAP_message>") && tmp_msg.endsWith("</SSAP_message>"))
			}     //if(tmp_msg.startsWith("<SSAP_message>") && tmp_msg.endsWith("</SSAP_message>"))
			deb_print("KpCore:readByteXByteSIBmsg:READ LOOP TERMINATED");
			closeConnection();

		}catch(Exception e)
		{

			err_print("KPICore:readByteXByteSIBmsg:Exception on EVENT HANDLER:RECEIVE:\n"+e);
			this.KP_ERROR_ID=this.ERR_SOCKET_TIMEOUT;
		}

		this.KP_ERROR_ID=this.ERR_Subscription_NOT_DONE;

		deb_print("KpCore:SSAP:Message received:("+msg+")");

		return new SIBResponse(msg);

	}//private String subscribeRDF(String msg)



	/**
	 * Method to run the SIB event handler
	 * 
	 * @param socket the socket descriptor of the opened socket with the SIB 
	 * @param in input stream to receive message from the SIB
	 * @param out output stream to sent message to the SIB
	 */
	private void startEventHandlerThread(Socket socket,BufferedReader in,PrintWriter out)
	{
		final iKPIC_subscribeHandler f_eh=this.eh;
		final Socket         ft_kpSocket = socket;
		final PrintWriter    ft_out      = out;
		final BufferedReader ft_in       = in;

		eventThread = new Thread()
		{
			public void run() {
				SSAP_XMLTools xmlTools=new SSAP_XMLTools(null,null,null);
				String msg_event="";   
				String restOfTheMessage="";

				deb_print("\n[EVENT HANDLER THREAD STARTED]___________________________________");

				//******************************************
				int buffsize= 4 * 1024;

				StringBuilder builder = new StringBuilder();

				char[] buffer = new char[buffsize];

				int charRead =0;
				try
				{	
					while (  ( (charRead = ft_in.read(buffer, 0, buffer.length)) != (-1)) || (!restOfTheMessage.isEmpty())  ) 
					{
						//LR: exclude ping byte
						if (charRead == 1 && buffer[0]==0x1B) {
							Logger.log(VERBOSITY.DEBUG, tag+":startEventHandlerThread", "Ping");
							continue;
						}
						
						if(!restOfTheMessage.equals(""))
						{
							builder.append(restOfTheMessage);
							restOfTheMessage = "";
						}
						if(charRead != -1)
						{
							builder.append(buffer, 0 , charRead);
						}

						msg_event = builder.toString();

						if(  msg_event.contains("<SSAP_message>") 
								&& msg_event.contains("</SSAP_message>"))
						{ 
							restOfTheMessage = msg_event.substring(msg_event.indexOf("</SSAP_message>") + 15);
							msg_event = msg_event.substring(0, msg_event.indexOf("</SSAP_message>") + 15);

							//	System.out.println("##1 real_msg = " + msg_event);
							//	System.out.println("##2 rest_of_msg = " + restOfTheMessage);
							// This control is mandatory
							deb_print("KpCore:EventHandlerThread:is this a UnSubscriptionConfirmed message?");   
							if(xmlTools.isUnSubscriptionConfirmed(msg_event))
							{
								deb_print( "KpCore:EventHandlerThread:YES, UnSubscription Confirmed!\n"
										+"EVENT HANDLER THREAD:stop");
								f_eh.kpic_SIBEventHandler( msg_event );
								return;
							}//if(xmlTools.isUnSubscriptionConfirmed(msg_event))
							else 
							{
								deb_print("KpCore:EventHandlerThread:NO, this is not a UnSubscriptionConfirmed");}


							deb_print("KpCore:EventHandlerThread:passing the event message passed to event handler...:"+msg_event.replace("\n", ""));   
							f_eh.kpic_SIBEventHandler( msg_event );
							deb_print("KpCore:EventHandlerThread:event message passed to event handler");   



							if(  restOfTheMessage.contains("<SSAP_message>") 
									&& restOfTheMessage.contains("</SSAP_message>"))
							{						
								deb_print( "KpCore:EventHandlerThread:YES, UnSubscription Confirmed!\n"
										+"EVENT HANDLER THREAD:stop");
								//System.out.println( "Rest of the message = " + restOfTheMessage);
								String test = restOfTheMessage.substring(0, restOfTheMessage.indexOf("</SSAP_message>") +15);
								if (xmlTools.isUnSubscriptionConfirmed(test))
								{
									return;	
								}
							}


							buffer = new char[buffsize];
							charRead = 0;
							msg_event="";
							builder = new StringBuilder();


							//     builder= new StringBuilder();

						}//if(tmp_msg.startsWith("<SSAP_message>") && tmp_msg.endsWith("</SSAP_message>"))

						//else if(  msg_event.contains("</SSAP_message>")) deb_print("***NOT YET RECOGNIZED EVENT MESSAGE:_"+msg_event.replace("\n", "")+"_"); 

					}
					try
					{
						ft_out.close();
						ft_in.close();
						ft_kpSocket.close();
					}
					catch(Exception e)
					{
						err_print("KpCore:startEventHandlerThread:closing connection:Exception:\n"+e);
						e.printStackTrace();
						//this.ERR_EXC_CLOSE_CONN;
					}	
				}
				catch(Exception e)
				{
					err_print("KpCore:startEventHandlerThread:reading:Exception:\n"+e);
					e.printStackTrace();
				}
			}/*RUN*/}/*new Thread()*/;

			eventThread.start(); 

	}//private void startThreadEventHandler()



	/**
	 * This method is still available but never used.
	 * The SIB event handler thread should stop himself.
	 * 
	 */
	public boolean stopEventThread()
	{
		this.eventThread.interrupt();
		return this.eventThread.isInterrupted();
	}        


	/**
	 * Perform the UNSUBSCRIBE procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @return a string representation of the XML answer message from the SIB 
	 */
	public SIBResponse unsubscribe()
	{
		deb_print("\n[UNSUBSCRIBE]___________________________________");            
		return new SIBResponse(sendSSAPMsg( this.xmlTools.unsubscribe() ));
	}//String unsubscribe()


	/**
	 * Perform the UNSUBSCRIBE procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param subscription_id the string representation of the subscription ID
	 * for the subscription to remove
	 * 
	 * @return null because unsubscription confirmation message is sent to handler 
	 */
	public SIBResponse unsubscribe(String subscription_id)
	{deb_print("\n[UNSUBSCRIBE]___________________________________");
	//	return new SIBResponse( sendSSAPMsg(this.xmlTools.unsubscribe(subscription_id) ));

	
	sendSSAPMsgOneWay(this.xmlTools.unsubscribe(subscription_id));
	//workaround because there is no confirmation message in SSAP for unsubscribe
	return new SIBResponse("UNSUB", null, subscription_id);
	}//String unsubscribe()



	/**
	 * 
	 * @param EntityI Specify the entity which the protection must be applied 
	 * @param properties EntityI's Property list to protect 
	 * @return a string representation of the XML answer message from the SIB
	 */
	@Override
	public SIBResponse insertProtection(String EntityI, Vector<String> properties)
	{
		Vector<Vector<String>> triples= new Vector<Vector<String>>(); 

		String xml="";
		boolean ack=false;	

		//String Protection_Entity = "http://" + Double.toString(Math.random());
		String Protection_Entity = "http://" + UUID.randomUUID();

		deb_print("KpCore:insertProtection:Praparing all the protection triples...");

		triples.add( this.xmlTools.newTriple( EntityI, AR_Property, Protection_Entity, "URI", "URI"));
		triples.add(this.xmlTools.newTriple(Protection_Entity, AR_Owner, this.nodeID, "URI", "URI"));

		deb_print("KpCore:insertProtection:For loop on all triples to insert...");

		for(int i = 0; i < properties.size();i++)
		{
			triples.add(this.xmlTools.newTriple(Protection_Entity, AR_Target, properties.elementAt(i), "URI", "literal"));
		}

		deb_print("KpCore:insertProtection:Insert triples...");
		SIBResponse out = insert(triples);

		ack=xmlTools.isResponseConfirmed(out);

		deb_print("KpCore:insertProtection:SIB message responce:\n"+xml+"\n");
		deb_print("Insert confirmed:"+(ack?"YES":"NO"));

		KP_ERROR_ID=(ack?ERR_NO_ERROR:ERR_InsertProtection_FAIL);
		if(KP_ERROR_ID<0) err_print("KpCore:insertProtection:ERROR:"+ERR_MSG[(KP_ERROR_ID*-1)] );

		return out;
	}//public boolean insertProtection()



	/**
	 * 
	 * @param EntityI Specify the entity on which the protection was applied
	 * @param properties EntityI's Property list protected
	 * @return a string representation of the XML answer message from the SIB
	 */
	@Override
	public SIBResponse removeProtection(String EntityI, Vector<String> properties)
	{
		Vector<Vector<String>> triples= new Vector<Vector<String>>(); 
		String xml="";
		boolean ack=false;	

		//String Protection_Entity = "http://" + Double.toString(Math.random());
		String Protection_Entity = "http://" + UUID.randomUUID();

		deb_print("KpCore:removeProtection:Praparing all the protection triples...");

		triples.add( this.xmlTools.newTriple( EntityI, AR_Property, Protection_Entity, "URI", "URI"));
		triples.add(this.xmlTools.newTriple(Protection_Entity, AR_Owner, nodeID, "URI", "URI"));

		deb_print("KpCore:removeProtection:For loop on all triples to remove...");

		for(int i = 0; i < properties.size();i++)
		{
			triples.add(this.xmlTools.newTriple(Protection_Entity, AR_Target, properties.elementAt(i), "URI", "literal"));
		}

		deb_print("KpCore:removeProtection:Remove triples...");
		SIBResponse out = remove(triples);

		ack=xmlTools.isResponseConfirmed(out);

		deb_print("KpCore:removeProtection:SIB message responce:\n"+xml+"\n");
		deb_print("Remove confirmed:"+(ack?"YES":"NO"));

		KP_ERROR_ID=(ack?ERR_NO_ERROR:ERR_RemoveProtection_FAIL);
		if(KP_ERROR_ID<0) err_print("KpCore:removeProtection:ERROR:"+ERR_MSG[(KP_ERROR_ID*-1)] );

		return out;
	}//public boolean removeProtection() 

	/**
	 * Perform the RDF-QUERY procedure 
	 * Check the error state with the functions: getErrMess, getErrID
	 * 
	 * @param string
	 * @return  a string representation of the XML answer message from the SIB 
	 */
	public SIBResponse querySPARQL(String string) {
		deb_print("\n[QUERY SPARQL]___________________________________");
		return new SIBResponse(sendSSAPMsg( this.xmlTools.querySPARQL(string) ));
	}//public String querySparql(String string)

	/**
	 * Remove specified in RDFXML
	 * @param graph
	 * @return SIB response
	 */
	public SIBResponse remove_rdf_xml(String graph){
		deb_print("\n[REMOVE_RDF-XML]___________________________________");
		if(protocol_version == 0)
		{
		return new SIBResponse(sendSSAPMsg( this.xmlTools.remove_rdf_xml(graph) ));
		}
		else
		{
			return new SIBResponse(sendSSAPMsg( this.xmlTools.remove_rdf_xml(graph) ));

		}
	}//String remove_rdf_xml(String graph)

	/**
	 * 
	 * @param graph
	 * @return
	 */
	public SIBResponse insert_rdf_xml(String graph){
		deb_print("\n[INSERT_RDF-XML]___________________________________");
		deb_print(this.xmlTools.insert_rdf_xml(graph));
		if(protocol_version == 0)
		{
		return new SIBResponse(sendSSAPMsg( this.xmlTools.insert_rdf_xml(graph) ));
		}
		else
		{
			return new SIBResponse(sendSSAPMsg( this.xmlTools.insert_rdf_xml_2(graph) ));
		}
	}//String insert_rdf_xml(String graph)

	/**
	 * 
	 * @param insGraph
	 * @param remGraph
	 * @return
	 */
	public SIBResponse update_rdf_xml( String insGraph, String remGraph){
		deb_print("\n[UPDATE]___________________________________");
		if(protocol_version == 0)
		{
		return new SIBResponse(sendSSAPMsg( this.xmlTools.update_rdf_xml(insGraph, remGraph)));
		}
		else
		{
		return new SIBResponse(sendSSAPMsg( this.xmlTools.update_rdf_xml_2(insGraph, remGraph)));

		}
	}//String update_rdf_xml( String insGraph, String remGraph){
		
	// FG
		
	public SIBResponse policyAdd(String query, String allowed, String protectionMode){
		deb_print("\n[POLICY SPARQL]___________________________________"); 

		return new SIBResponse(sendSSAPMsg( this.xmlTools.policyAdd(query, allowed, protectionMode)));
	}
		
	public SIBResponse policyDel(String policy_id){
		deb_print("\n[POLICY DELETE]___________________________________");
		return new SIBResponse(sendSSAPMsg( this.xmlTools.policyDel(policy_id)));
		//sendSSAPMsgOneWay(this.xmlTools.policyDel(policy_id));
		//workaround because there is no confirmation message in SSAP for unsubscribe
		//return new SIBResponse("POLICY", null, policy_id);
	}
	

	@Override
	public void kpic_RDFEventHandler(Vector<Vector<String>> newTriples,
			Vector<Vector<String>> oldTriples, String indSequence, String subID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void kpic_SPARQLEventHandler(SSAP_sparql_response newResults,
			SSAP_sparql_response oldResults, String indSequence, String subID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void kpic_UnsubscribeEventHandler(String sub_ID) {
		// TODO Auto-generated method stub

	}

	@Override
	public void kpic_ExceptionEventHandler(Throwable SocketException) {
		// TODO Auto-generated method stub

	}

	@Override
	public void kpic_SIBEventHandler(String xml) {
		// TODO Auto-generated method stub

	}

}//public class KpCore implements kp_subscribeHandler
