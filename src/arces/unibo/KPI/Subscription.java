package arces.unibo.KPI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Vector;

import arces.unibo.SEPA.Logger;
import arces.unibo.SEPA.Logger.VERBOSITY;

public class Subscription {
	private Socket ft_kpSocket;
	private iKPIC_subscribeHandler2 f_eh;
	private static String tag ="KPI";
	private InputStream reader = null;
	private BufferedReader ft_in = null;
	
	public Subscription(Socket in_sock, iKPIC_subscribeHandler2 hand)
	{	
		f_eh = hand;
		
		try 
		{
			reader = in_sock.getInputStream();
		} 
		catch (IOException e2) {
			Logger.log(VERBOSITY.ERROR, tag, e2.getMessage());
			e2.printStackTrace();
		}

		ft_in = new BufferedReader(new InputStreamReader(reader));

		Logger.log(VERBOSITY.DEBUG, tag,"Subscription thread starting...");
						
		Thread eventThread = new Thread()
		{
			public void run() {
				
				String msg_event="";   
				String restOfTheMessage="";
				int buffsize= 4 * 1024;
				StringBuilder builder = new StringBuilder();
				char[] buffer = new char[buffsize];
				int charRead =0;
				
				SSAP_XMLTools xmlTools=new SSAP_XMLTools(null,null,null);
				
				try
				{
					while (  ( (charRead = ft_in.read(buffer, 0, buffer.length)) != -1) || (!restOfTheMessage.isEmpty())  ) 
					{
						//LR: exclude ping byte
						if (charRead == 1 && buffer[0] == Constants.PING) {
							Logger.log(VERBOSITY.DEBUG, tag+":Subscription Thread", "Ping");
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

						//One or more messages in the same notification
						if(  msg_event.contains("<SSAP_message>") && msg_event.contains("</SSAP_message>"))
						{
							int index = msg_event.indexOf("</SSAP_message>") + 15;
							int start = msg_event.indexOf("<SSAP_message>");
							restOfTheMessage = msg_event.substring(index);
							msg_event = msg_event.substring(start, index);
							String subID = xmlTools.getSubscriptionID(msg_event);

							// here it starts single message processing and it is possible to launch multiple threads for parallelization
							if(xmlTools.isUnSubscriptionConfirmed(msg_event))
							{
								f_eh.kpic_UnsubscribeEventHandler( subID  );
								return;
							}
							else 
							{
								String indSequence = xmlTools.getSSAPmsgIndicationSequence(msg_event);
								if(xmlTools.isRDFNotification(msg_event))
								{
									Vector<Vector<String>> triples_n = new Vector<Vector<String>>();
									triples_n = xmlTools.getNewResultEventTriple(msg_event);
									Vector<Vector<String>> triples_o = new Vector<Vector<String>>();
									triples_o = xmlTools.getObsoleteResultEventTriple(msg_event);
									f_eh.kpic_RDFEventHandler(triples_n, triples_o, indSequence, subID);
								}
								else
								{
									SSAP_sparql_response resp_new = xmlTools.get_SPARQL_indication_new_results(msg_event);
									SSAP_sparql_response resp_old = xmlTools.get_SPARQL_indication_obsolete_results(msg_event);
									
									f_eh.kpic_SPARQLEventHandler(resp_new, resp_old, indSequence, subID);
								}

								//a complete message in the rest of the message
								if(  restOfTheMessage.contains("<SSAP_message>") && restOfTheMessage.contains("</SSAP_message>"))
								{						
									String test = restOfTheMessage.substring(0, restOfTheMessage.indexOf("</SSAP_message>") +15);
									if (xmlTools.isUnSubscriptionConfirmed(test))
									{
										f_eh.kpic_UnsubscribeEventHandler( subID  );
										return;	
									}
								}

								buffer = new char[buffsize];
								charRead = 0;
								msg_event="";
								builder = new StringBuilder();
							}
						}
					}
					try
					{
						Logger.log(VERBOSITY.WARNING, tag,"Broken subscription!");
						ft_in.close();
						ft_kpSocket.close();
					}
					catch(Exception e)
					{
						Logger.log(VERBOSITY.ERROR, tag,e.getMessage());
						e.printStackTrace();
						f_eh.kpic_ExceptionEventHandler(e);
					}	
				}

				catch(Exception e)
				{
					Logger.log(VERBOSITY.ERROR, tag,e.getMessage());
					e.printStackTrace();
					f_eh.kpic_ExceptionEventHandler(e);
				}
			}};

		eventThread.start(); 
	}
}




