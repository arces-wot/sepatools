/**
 * @author Alfredo D'Elia
 * @author Carlo Antenucci
 */

package arces.unibo.KPI;

import java.util.Vector;

/**
 *  This file explains basic information about java KPI usage.
 */

// A class that uses KPI must implement iKPIC_subscribeHandler (only for compatibility) and/or
// iKPIC_subscribeHandler2. iKPIC_subscribeHandler2 is strongly suggested since old
// subscribe handler will be no longer supported
public class KPIPrimitiveUsage implements iKPIC_subscribeHandler, iKPIC_subscribeHandler2{

	/**
	 * Typical declarations
	 */

	public KPICore kp;  //direct interface with the SIB
	public SIBResponse resp; // The class representing SIB response

	/**
	 * SIB constants to be opportunely modified statically or at run-time in order to interact 
	 * with the SIB
	 */
	
	public String SIB_Host = "127.0.0.1";
	public  int SIB_Port = 10010;
	public  String SIB_Name = "X";
	
	boolean isRedSIB = true;//true if working with redSIB
	
	// at Sept 2014 SIB_Name is still not a relevant parameter checked by SIB, but must be specified

	/**
	 * Use the following declarations if you want to specify an external handler, different form the
	 * one defined in this class
	 */
	public Handler handler = new Handler(); //Old handled version (deprecated)   
	public Handler2 handler2 = new Handler2(); //New suggested version 
	
	
	public static void main(String [] argv){
		KPIPrimitiveUsage test = new KPIPrimitiveUsage();
		test.test_primitives();
	}

	public void test_primitives(){
		//Create a new kp
		System.out.println("Connecting to \""+ SIB_Name+ "\" @ " + SIB_Host + ":" + SIB_Port );
		KPICore kp = new KPICore(SIB_Host, SIB_Port, SIB_Name);
		System.out.println("Connected!");
		//Remove debug and error print
		kp.disable_debug_message();
		kp.disable_error_message();

		// Following variables are used to work with SIB
		/**
		 * A triple is an useful structure that contains:
		 *   - subject
		 *   - predicate
		 *   - object
		 *   - subject type (always "uri")
		 *   - object type
		 */
		Vector<String> triple = new Vector<String>(); 
		// that are lists of triple
		Vector<Vector<String>> triples = new Vector<Vector<String>>();
		Vector<Vector<String>> triples_ins = new Vector<Vector<String>>();  //Structure that can be useful in many programs
		Vector<Vector<String>> triples_rem = new Vector<Vector<String>>();  //Structure that can be useful in many programs

		//create 5 example triples that has http://examplens# as name space 
		String ns = "http://examplens#";
		for(int i = 0; i < 5; i++){
			triple = new Vector<String>();
			triple.add(ns+ "subject_" + i);
			triple.add(ns+ "predicate_" + i);
			triple.add(ns+ "object_" + i);
			triple.add("uri");
			triple.add("uri");
			triples_ins.add(triple);
			triples_rem.add(triple);
		}
		
		/**
		 * First of all we must join the SIB
		 */

		//resp represents the SIB response
		resp = kp.join();
		if(!resp.isConfirmed())
			System.err.println ("Error joining the SIB");
		else
			System.out.println ("SIB joined correctly");

		/**
		 * Now we can try to insert some triples into the SIB.
		 */
		
		//we use the list triples_ins filled previously
		resp = kp.insert(triples_ins);
		if(!resp.isConfirmed())
			System.err.println("Error inserting into the SIB");
		else
			System.out.println("triples_ins inserted into the SIB");
		
		/**
		 * After that we are going to insert two single triples into SIB, which will be removed later
		 * with the same query, using a wild-card (ANYURI)
		 */
		resp = kp.insert(ns + "subject_triple_old", ns + "predicate_triple_old",ns + "object_triple_old","uri","uri");
		resp = kp.insert(ns + "subject_triple_old", ns + "predicate_triple_old",ns + "object_triple_old2","uri","uri");
		if(!resp.isConfirmed())
			System.err.println ("Error Inserting into the SIB");
		else{
			System.out.println ("single triples inserted into SIB");
		}
		//Removing triples is similar to insert. The only difference is that we can use the wild-card
		//ANYURI to remove more than a row for each query, for example:
		resp = kp.remove(ns + "subject_triple_old", ns + "predicate_triple_old",SSAP_XMLTools.ANYURI,"uri","uri");
		//Previous query removes the two rows inserted. 
		if(!resp.isConfirmed())
			System.err.println ("Error removing from the SIB");
		else
			System.out.println ("single triples removed from the SIB");
		/**
		 * Following query removes all other triples inserted before (triples_rem) 
		 */
		resp = kp.remove(triples_rem); 
		if(!resp.isConfirmed())
			System.err.println ("Error removing from the SIB");
		else
			System.out.println ("initial triples removed from the SIB");
		
		/**
		 * Next we can update a triple:
		 */
		// First we insert a new triple into the SIB to update it
		kp.insert(ns + "subject_triple_old", ns + "predicate_triple_old",ns + "object_triple_old","uri","uri");
		/**
		 * We can update SIB content using two set of triples (such as triples_ins or triples_rem) 
		 * the update operation takes a set of triples to be removed and one to be inserted. The two operations are
		 * executed in atomic way and the delete is performed before
		 * 
		 * 
		 * We can use the update function not only to update single arcs of graph, but also as a 
		 * shortcut for two independent and successive remove and insert operations (in this order)
		 */

		// update a single triple specifying elements
		resp = kp.update(ns + "subject_triple_new", ns + "predicate_triple_new",ns + "object_triple_new","uri","uri" , ns + "subject_triple_old", ns + "predicate_triple_old",ns + "object_triple_old","uri","uri");
		if(!resp.isConfirmed())
			System.err.println ("Error updating the SIB");
		else
			System.out.println ("Rows updated the SIB");
		
		
		/**
		 * Subscription:
		 * If this class doesn't implements iKPIC_subscribeHandler2 and if it needs to subscribe
		 * to the SIB, we must specify a subscribe handler implementing the needed interface . 
		 * 
		 * There are two kinds of subscription: RDF-M3 and SPARQL
		 * 
		 * The subscription RDF-M3 is based on triple patterns, accepts one or more triple patterns 
		 * and returns all the existing triples matching the patterns. 
		 * A notification is received when triples matching one or more of the patterns are inserted 
		 * (new triples) or removed (obsolete). When we defines a list of patterns the notification is
		 * received when only one pattern matches with the inserted (or removed) triple.
		 * 
		 * The SPARQL subscription is based on SPARQL queries, the subscription returns all the results 
		 * matching the query (like a query to SIB) then a  notification is received when the results 
		 * of the query change. Only the new or old results are sent back in notifications
		 */

		// If we want to specify an external handler we can use: 
//			kp.setEventHandler(handler)

		// While if we want to use the handler defined in this class we can use:
//		kp.setEventHandler(this);  
		//this handler is deprecated, left for compatibility, like the following subscription
//		resp = kp.subscribeRDF(ns+ "subject_1", null , null , "uri");
		
		//null is an alias equivalent to SSAP_XMLTools.ANYURI
		
		/**
		 * Subscribing to the SIB, we receive a notification each time "subject_1" appears as 
		 * subject in a triple, with any predicate or object 
		 */
		resp = kp.subscribeRDF(ns+ "subject_a", null , null , "uri", this); 
		//or alternatively (if we want to use an external handler) 
		//resp = kp.subscribeRDF(ns+ "subject_1", null , null , "uri", handler2); 
		String subID_1 = null;		
		if(resp.isConfirmed()){
			try{
				subID_1=resp.subscription_id;
				System.out.println("RDF Subscription confirmed: id = " + subID_1);
			}
			catch(Exception e){}
		}else{
			System.err.println ("Error during subscription");
		}
		
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// Now the rule is not yet active

		//This insert should fire a notification
		kp.insert(triples_ins); 

		//For a SPARQL subscription to all triples we can use
//		resp = kp.subscribeSPARQL("Select ?a ?b ?c where { ?a ?b ?c }");
		//this method is deprecated, with new KPI we have to define into the method the handler
		resp = kp.subscribeSPARQL("Select ?a ?b ?c where { ?a ?b ?c }", handler2);
		String subID_2=null;		
		if(resp.isConfirmed()){
			try{
				subID_2=resp.subscription_id;
				System.out.println("SPARQL Subscription confirmed : id = "  + subID_2 );
			}
			catch(Exception e){}
		}
		else{
			System.err.println ("Error during subscription");
		}
		
		//SSAP_sparql_response is an object useful to manage the sparql query results
		SSAP_sparql_response SPARQLresp = resp.sparqlquery_results;
		//For example it provides the representation of variables and corresponding values 
		//in human readable format
		System.out.println("Respose to sparql subscription:\n" + SPARQLresp.print_as_string());

		
		/**
		 * Persistent update: it behaves like a rule.
		 * another triple that specifies that the subject (?a) hasPredicate the predicate inserted (?b)
		 * The persistent update, when confirmed is identified by an ID that can be used to cancel it.
		 * 
		 * Works only with osgiSIB v3, if you are using an older version, please comment following rows
		 */
		
		if(!isRedSIB)
		{
		// We define the persistent update that specifies that for each triple inserted into the SIB
		// will be inserted another triple which specifies that the subject inserted (?a) hasPredicate
		// the predicate inserted (?b)
		System.out.println("inserting rule : \"Insert { ?a <http://examplens#hasPredicate> ?b } where { ?a ?b ?c }\"");
		resp = kp.persistent_update("Insert { ?a <http://examplens#hasPredicate> ?b } where { ?a ?b ?c } ");
		String ruleID = resp.update_id;
		System.out.println("Rule inserted rule_id = " + ruleID);
		// Now, if we are going to insert the following triple,
		kp.insert(ns + "subject_a", "http://ns#predicate_b1", "http://ns#object_c", "URI", "URI");
		// the rule inserts: http://ns#subject_a, http://examplens#hasPredicate, http://ns#predicate_b
		// For deleting a persistent update we can use the following method
		resp = kp.cancel_persistent_update(ruleID);
		}
		
		
		
		/**
		 * Unsubscription
		 */

		
		if(subID_1!= null){
			try{
				resp = kp.unsubscribe(subID_1);
				if(resp.isConfirmed())
					System.out.println("Subscription " + subID_1 + " unsubscribed correctly");
				else
					System.err.println("Error! "+ subID_1 +" unsubscription not confirmed");
			}catch (Exception e){
				System.out.println("Error during RDF unsubscription");
			}
		}
		
		if(subID_2!= null){
			try{
				resp = kp.unsubscribe(subID_2);
				if(resp.isConfirmed())
					System.out.println("Subscription " + subID_2 + " unsubscribed correctly");
				else
					System.err.println("Error! "+ subID_2 +" unsubscription not confirmed");
			}catch (Exception e){
				System.out.println("Error during RDF unsubscription");
			}
		}

		/** 
		 * Query RDF-M3:
		 * The query RDF-M3 is based on triple patterns, it accepts one or more triple patterns and 
		 * returns all the existing triples matching the patterns
		 * 
		 * Query SPARQL:
		 * The  SPARQL query primitive is based on a SPARQL query, it accepts one SPARQL query the 
		 * result can be wrapped in an object.
		 * 
		 */

		//This query returns all triples which have "subject_1" as subject
		// null is an alias equivalent to SSAP_XMLTools.ANYURI
		resp=kp.queryRDF (ns+ "subject_a", SSAP_XMLTools.ANYURI, null, "uri","uri");		
		if(!resp.isConfirmed())
			System.err.println ("Error during RDF-M3 query");
		else{
			System.out.println("\nPrinting RDF-M3 query results:\n");
			//if query is confirmed, copy into triples query results and if triples is not null
			//print results in human readable format
			triples = null;
			triples = resp.query_results;
			if (triples != null){
				for(int i=0; i<triples.size() ; i++ ){
					Vector<String> t=triples.get(i);
					System.out.println(" S:["+t.get(0)
							+"] P:["+t.get(1)
							+"] O:["+t.get(2)
							+"] Otype:["+t.get(3)+"]");

				}
			}
		}
		
		/**
		 * It is possible but not recommended to use RDF/XML primitives, this primitives naturally involve blank nodes which are difficult to manage 
		 * in normal Smart-M3 based applications. There are two main versions of SIB and SSAP is evolving. A little difference between the two SIB versions
		 * is in the management of rdf-xml
		 */
		
if(isRedSIB)
{
kp.insert_rdf_xml("<rdf:RDF "
+"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
+"xmlns:cd=\"http://www.recshop.fake/cd#\"> "

+"<rdf:Description "
+"rdf:about=\"http://www.recshop.fake/cd/Empire Burlesque\"> "
+  "<cd:artist>Bob Dylan</cd:artist> "
+  "<cd:country>USA</cd:country> "
+  "<cd:company>Columbia</cd:company> "
+  "<cd:price>10.90</cd:price> "
+  "<cd:year>1985</cd:year> "
+"</rdf:Description> "
+"<rdf:Description "
+"rdf:about=\"http://www.recshop.fake/cd/Hide your heart\"> "
+  "<cd:artist>Bonnie Tyler</cd:artist> "
+  "<cd:country>UK</cd:country> "
+  "<cd:company>CBS Records</cd:company> "
+  "<cd:price>9.90</cd:price> "
+  "<cd:year>1988</cd:year> "
+"</rdf:Description> "
+"</rdf:RDF>");
}
else
{
	kp.setProtocol_version(1);
	kp.insert_rdf_xml("<rdf:RDF "
			+"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "
			+"xmlns:cd=\"http://www.recshop.fake/cd#\"> "

			+"<rdf:Description "
			+"rdf:about=\"http://www.recshop.fake/cd/Empire Burlesque\"> "
			+  "<cd:artist>Bob Dylan</cd:artist> "
			+  "<cd:country>USA</cd:country> "
			+  "<cd:company>Columbia</cd:company> "
			+  "<cd:price>10.90</cd:price> "
			+  "<cd:year>1985</cd:year> "
			+"</rdf:Description> "
			+"<rdf:Description "
			+"rdf:about=\"http://www.recshop.fake/cd/Hide your heart\"> "
			+  "<cd:artist>Bonnie Tyler</cd:artist> "
			+  "<cd:country>UK</cd:country> "
			+  "<cd:company>CBS Records</cd:company> "
			+  "<cd:price>9.90</cd:price> "
			+  "<cd:year>1988</cd:year> "
			+"</rdf:Description> "
			+"</rdf:RDF>");
}
		
		
		/**
		 * This method has to be used also to make sparql updates on RedSIB, on OSGI-JAVA SIB  is possible to use kp.update_sparql(String sparql_update) 
		 */
		resp=kp.querySPARQL("Select ?a ?b ?c where { ?a ?b ?c }") ;
		if(!resp.isConfirmed())
			System.err.println ("Error during SPARQL query");
		else{
			System.out.println("\nPrinting SPARQL query results:\n");
			SSAP_sparql_response query_response = resp.sparqlquery_results;
			System.out.println(query_response.print_as_string());
		}

		/**
		 * The last operation that we can do is leaving the SIB using the following methods
		 */

		resp = kp.leave();
		if(!resp.isConfirmed())
			System.err.println ("Error during leave");
		else
			System.out.println("SIB left correctly");
	}


/**
 * Make a rule and delete it, new functionality introduced in 2014 in java SIB
 */

	
	
	/**
	 * Below you can see the differences between the old and new subscription handler.
	 * The first handler shown is the old, in which we had to parse the notification received
	 * to use correctly the informations received. In this example, is a best practice to start
	 * a new thread for each notification received because if many notifications arrives with high
	 * frequency is possible to match a Parsing Exception
	 */
	
	@Override 
	@Deprecated
	public void kpic_SIBEventHandler( String xml_received)
	{
		final String xml = xml_received;


		new Thread(
				new Runnable() {
					public void run() {
						String id = "";
						SSAP_XMLTools xmlTools = new SSAP_XMLTools();
						boolean isunsubscription = xmlTools.isUnSubscriptionConfirmed(xml);
						if(!isunsubscription)
						{
							String k = xmlTools.getSSAPmsgIndicationSequence(xml);
							id = xmlTools.getSubscriptionID(xml);
							

							if(xmlTools.isRDFNotification(xml))
							{
								Vector<Vector<String>> triples_n = new Vector<Vector<String>>();
								triples_n = xmlTools.getNewResultEventTriple(xml);
								Vector<Vector<String>> triples_o = new Vector<Vector<String>>();
								triples_o = xmlTools.getObsoleteResultEventTriple(xml);
								String temp = "\nNotification " + k + "; ID = " + id +"\n";
								for(int i = 0; i < triples_n.size(); i++ )
								{
									temp+="New triple s =" + triples_n.elementAt(i).elementAt(0) + "  + predicate" + triples_n.elementAt(i).elementAt(1) + "object =" + triples_n.elementAt(i).elementAt(2) +"\n";
								}
								for(int i = 0; i < triples_o.size(); i++ )
								{
									temp+="Obsolete triple s =" + triples_o.elementAt(i).elementAt(0) + "  + predicate" + triples_o.elementAt(i).elementAt(1) + "object =" + triples_o.elementAt(i).elementAt(2) + "\n";
								}
								System.out.println(temp);
							}
							else
							{
								System.out.println("Notif. " + k + " id = " + id +"\n");
								SSAP_sparql_response resp_new = xmlTools.get_SPARQL_indication_new_results(xml);
								SSAP_sparql_response resp_old = xmlTools.get_SPARQL_indication_obsolete_results(xml);
								if (resp_new != null)
								{
									System.out.println("new: \n " + resp_new.print_as_string());
								}
								if (resp_old != null)
								{
									System.out.println("obsolete: \n " + resp_old.print_as_string());
								}
							}
						}

					}
				}).start();

	}
	
	
	/**
	 * Example of new subscription handler :
	 *  kpic_RDFEventHandler receives data from RDF subscriptions
	 *  kpic_SPARQLEventHandler receives data from sparql subscriptions
	 *  kpic_UnsubscribeEventHandler receives the unsubscription event
	 *  kpic_ExceptionEventHandler receives possible exceptions
	 *  
	 */

	@Override
	public void kpic_RDFEventHandler(Vector<Vector<String>> newTriples,
			Vector<Vector<String>> oldTriples, String indSequence, String subID) {
		// TODO Auto-generated method stub
		

			String temp = "\nNotification " + indSequence + " id = " + subID +"\n";
			for(int i = 0; i < newTriples.size(); i++ )
			{
				temp+="New triple s =" + newTriples.elementAt(i).elementAt(0) + "  + predicate" + newTriples.elementAt(i).elementAt(1) + "object =" + newTriples.elementAt(i).elementAt(2) +"\n";
			}
			for(int i = 0; i < oldTriples.size(); i++ )
			{
				temp+="Obsolete triple s =" + oldTriples.elementAt(i).elementAt(0) + "  + predicate" + oldTriples.elementAt(i).elementAt(1) + "object =" + oldTriples.elementAt(i).elementAt(2) + "\n";
			}
			System.out.println(temp);
		
		
	}

	@Override
	public void kpic_SPARQLEventHandler(SSAP_sparql_response newResults,
			SSAP_sparql_response oldResults, String indSequence, String subID) {
		// TODO Auto-generated method stub
		System.out.println("\nNotification " + indSequence  +" id = " + subID + "\n");
		
		if (newResults != null)
		{
			System.out.println("new: \n " + newResults.print_as_string());
			
		}
		if (oldResults != null)
		{
			System.out.println("obsolete: \n " + oldResults.print_as_string());
			
		}
		
	}

	@Override
	public void kpic_UnsubscribeEventHandler(String sub_ID) {
		// TODO Auto-generated method stub
		System.out.println("Unsubscribed " + sub_ID);
		
	}

	@Override
	public void kpic_ExceptionEventHandler(Throwable SocketException) {
		// TODO Auto-generated method stub
		System.out.println("Exception in subscription handler " + SocketException.toString());
		
	}

}
