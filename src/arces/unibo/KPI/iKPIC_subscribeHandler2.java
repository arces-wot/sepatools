/**
 *Interface class to fix the method that is 
 *involved to handle the SIB event messages. 
 * <p>
 * @author      Daniele Manzaroli - VTT-Oulu, Technical Research Centre of Finland. ARCES, University of Bologna, Italy
 * @version     %I%, %G%
 */

package arces.unibo.KPI;

import java.util.Vector;

public interface iKPIC_subscribeHandler2 {
	
	String subID = "";

    /**
     * This method is the handler for event 
	 * messages received from the SIB
	 *
     * @param xml the string representation of the XML event message received from the SIB
     * 
     */
	//public void kpic_SIBEventHandler(String xml);
	
	public void kpic_RDFEventHandler(Vector<Vector<String>> newTriples, Vector<Vector<String>> oldTriples, String indSequence, String subID );
	
	public void kpic_SPARQLEventHandler(SSAP_sparql_response newResults, SSAP_sparql_response oldResults, String indSequence, String subID );
	
	public void kpic_UnsubscribeEventHandler(String sub_ID );
	
	public void kpic_ExceptionEventHandler(Throwable SocketException );

	
}
