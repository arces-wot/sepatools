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

import java.util.Vector;

public interface iKPIC 
{

    /**
     * Set the event handler
     * @param eh an object that implement the kp_subscribeHandler interface
     * 
     * @see sofia_kp_old.iKPIC_subscribeHandler#kpic_SIBEventHandler(java.lang.String) "SIB Event handler"
     *
     */
    public void setEventHandler(iKPIC_subscribeHandler eh);
    
    /**
     * Set the event handler
     * @param eh an object that implement the kp_subscribeHandler2 interface
     * 
     *
     */
    
    public void setEventHandler2(iKPIC_subscribeHandler2 eh);
    
    
    
    /****************************************************\
       
        Follow the operation available with the SIB
      
    \****************************************************/ 

    
    /**
     * Perform the JOIN procedure 
     * Check the error state with the functions: getErrMess, getErrID
     * @return a string representation of the XML answer message from the SIB
     */
    public SIBResponse join();

    
    /**
     * Perform the LEAVE procedure 
     * Check the error state with the functions: getErrMess, getErrID
     * @return a string representation of the XML answer message from the SIB 
     */
    public SIBResponse leave();
    
    
    /**
     * Perform the RDF-QUERY procedure 
     * Check the error state with the functions: getErrMess, getErrID
     * @param s the string representation of the subject.
     * @param p the string representation of the predicate. The value 'any' means any value
     * @param o the string representation of the object. the value 'any' means any value
     * @param s_type the string representation of the subject type. Allowed values are: uri, literal
     * @param o_type the string representation of the object type. Allowed values are: uri, literal
     * 
     * @return a string representation of the XML answer message from the SIB 
     */
    public SIBResponse queryRDF(String s,String p,String o, String s_type, String o_type);
    
    
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
    public SIBResponse queryRDF( Vector<Vector<String>> queryList );
    
    /**
     * Perform the INSERT procedure 
     * Check the error state with the functions: getErrMess, getErrID
     * @param s the string representation of the subject.
     * @param p the string representation of the predicate. The value 'any' means any value
     * @param o the string representation of the object. the value 'any' means any value
     * @param s_type the string representation of the subject type. Allowed values are: uri, literal
     * @param o_type the string representation of the object type. Allowed values are: uri, literal
     * 
     * @return a string representation of the XML answer message from the SIB 
     */
    public SIBResponse insert(String s,String p,String o, String s_type, String o_type);

    
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
    public SIBResponse insert( Vector<Vector<String>> queryList );
    
    /**
     * Perform the REMOVE procedure 
     * Check the error state with the functions: getErrMess, getErrID
     * @param s the string representation of the subject.
     * @param p the string representation of the predicate. The value 'any' means any value
     * @param o the string representation of the object. the value 'any' means any value
     * @param s_type the string representation of the subject type. Allowed values are: uri, literal
     * @param o_type the string representation of the object type. Allowed values are: uri, literal
     * 
     * @return a string representation of the XML answer message from the SIB 
     */
    public SIBResponse remove(String s,String p,String o,String s_type,String o_type); 	   
    
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
    public SIBResponse remove( Vector<Vector<String>> queryList );
    
    
    /**
     * Perform the UPDATE procedure 
     * Check the error state with the functions: getErrMess, getErrID
     * <p/>
     * New value to insert:
     * @param sn the string representation of the subject.
     * @param pn the string representation of the predicate. The value 'any' means any value
     * @param on the string representation of the object. the value 'any' means any value
     * @param sn_type the string representation of the subject type. Allowed values are: uri, literal
     * @param on_type the string representation of the object type. Allowed values are: uri, literal
     * 
     * Old value to replace:
     * @param so the string representation of the subject.
     * @param po the string representation of the predicate. The value 'any' means any value
     * @param oo the string representation of the object. the value 'any' means any value
     * @param so_type the string representation of the subject type. Allowed values are: uri, literal
     * @param oo_type the string representation of the object type. Allowed values are: uri, literal
     * 
     * @return a string representation of the XML answer message from the SIB 
     */
    //o==old, n==new
    public SIBResponse update( String sn,String pn,String on,String sn_type,String on_type
 		             ,String so,String po,String oo,String so_type,String oo_type);    

    
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
 		                , Vector<Vector<String>>  oldTripleVector);
   
    
    
    
    /**
     * Perform the SUBSCRIBE procedure 
     * Check the error state with the functions: getErrMess, getErrID
     * @param s the string representation of the subject.
     * @param p the string representation of the predicate. The value 'any' means any value
     * @param o the string representation of the object. the value 'any' means any value
     * @param o_type the string representation of the object type. Allowed values are: uri, literal
     * 
     * @return a null value string in case of error otherwise an empty string 
     */
    
    public SIBResponse subscribeRDF(String s,String p,String o,String o_type);      
    
    
    /**
     * Perform the UNSUBSCRIBE procedure 
     * Check the error state with the functions: getErrMess, getErrID
     * 
     * @return a string representation of the XML answer message from the SIB 
     */
    public SIBResponse unsubscribe();
	
	
    /**
     * 
     * @param EntityI Specify the entity which the protection must be applied 
     * @param properties EntityI's Property list to protect 
     * @return a string representation of the XML answer message from the SIB
     */
    public SIBResponse insertProtection(String EntityI, Vector<String> properties);

    /**
     * 
     * @param EntityI Specify the entity on which the protection was applied
     * @param properties EntityI's Property list protected
     * @return a string representation of the XML answer message from the SIB
     */
    public SIBResponse removeProtection(String EntityI, Vector<String> properties);
    
    
}//
