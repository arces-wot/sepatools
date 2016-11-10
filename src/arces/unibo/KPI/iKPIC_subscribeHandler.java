/**
 *Interface class to fix the method that is 
 *involved to handle the SIB event messages. 
 * <p>
 * @author      Daniele Manzaroli - VTT-Oulu, Technical Research Centre of Finland. ARCES, University of Bologna, Italy
 * @version     %I%, %G%
 */

package arces.unibo.KPI;

public interface iKPIC_subscribeHandler {
	
	String subID = "";

    /**
     * This method is the handler for event 
	 * messages received from the SIB
	 *
     * @param xml the string representation of the XML event message received from the SIB
     * 
     */
	public void kpic_SIBEventHandler(String xml);
	
	

	
}
