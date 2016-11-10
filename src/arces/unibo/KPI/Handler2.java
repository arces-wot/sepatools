/**
 * Example of subscribe handler implementing the interface iKPIC_subscribeHandler2
 * 
 * @author Alfredo D'Elia
 */

package arces.unibo.KPI;

import java.util.Vector;

public class Handler2 implements iKPIC_subscribeHandler2 {

	public Handler2() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public void kpic_RDFEventHandler(Vector<Vector<String>> newTriples,
			Vector<Vector<String>> oldTriples, String indSequence, String subID) {

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
	



		// TODO Auto-generated method stub

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
