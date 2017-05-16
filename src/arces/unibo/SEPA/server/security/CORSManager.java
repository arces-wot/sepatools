/* This class has been implemented for CORS management
 * 
 * Author: Luca Roffia (luca.roffia@unibo.it)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package arces.unibo.SEPA.server.security;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpExchange;

/**
 * The Class CORSManager.
 */
public class CORSManager {
	
	/** The logger. */
	protected static Logger logger = LogManager.getLogger("CORSManager");
	
	/**
	 * Process a CORS (Cross-Origin Resource Sharing) pre-flight request. <br>
	 * 	 
	 * References:<br>
	 * <a href="https://www.w3.org/TR/cors">CORS<a/><br>
	 * <a href="https://www.w3.org/wiki/CORS">CORS wiki<a/><br>
	 * <a href="https://fetch.spec.whatwg.org/">CORS specification<a/><br>
	 *
	 * @param httpExchange the http exchange
	 * @return true, if the CORS request is successfully handled
	 */
	public static boolean processCORSPreFlightRequest(HttpExchange httpExchange,String responseBody){
		if(!httpExchange.getRequestMethod().toUpperCase().equals("OPTIONS")) return false;

		logger.debug("CORS pre-flight request");

		/*
		 * If the Origin header is not present terminate this set of steps. The request is outside the scope of this specification.
		 */
		
		String allowOrigin = null;
		if (!httpExchange.getRequestHeaders().containsKey("Origin")) return false;
		List<String> origins = httpExchange.getRequestHeaders().get("Origin");
		if (origins.size() != 1) return false;
		
		allowOrigin = origins.get(0);

		/*
		 * If the value of the Origin header is not a case-sensitive match for any of the values in list of origins do not set any additional headers and terminate this set of steps.
		 */
		
		//TODO check origin against a list of allowed origins
		
		/*
		 * Let method be the value as result of parsing the Access-Control-Request-Method header.
		 * If there is no Access-Control-Request-Method header or if parsing failed, do not set any additional headers and terminate this set of steps. 
		 * The request is outside the scope of this specification.
		 */
		
		String allowMethod = null;
		if (!httpExchange.getRequestHeaders().containsKey("Access-Control-Request-Method")) return false;
		List<String> methods = httpExchange.getRequestHeaders().get("Access-Control-Request-Method");
		if (methods.size() != 1) return false;
		
		allowMethod = methods.get(0);
		
		/*
		 * If method is not a case-sensitive match for any of the values in list of methods do not set any additional headers and terminate this set of steps.
		 */
		
		//TODO check method against a list of allowed methods
		
		/*
		 * Let header field-names be the values as result of parsing the Access-Control-Request-Headers headers.
		 * If there are no Access-Control-Request-Headers headers let header field-names be the empty list.
		 * If parsing failed do not set any additional headers and terminate this set of steps. The request is outside the scope of this specification.
		 */
		
		String allowHeaders = "";
		if (!httpExchange.getRequestHeaders().containsKey("Access-Control-Request-Headers")) return false;
		List<String> headers = httpExchange.getRequestHeaders().get("Access-Control-Request-Headers");
		for (String temp : headers) {
			if (allowHeaders.equals("")) allowHeaders = temp;
			else allowHeaders = allowHeaders +","+temp;
		}
		
		/*
		 * If any of the header field-names is not a ASCII case-insensitive match for any of the values in list of headers do not set any additional headers and terminate this set of steps.
		 */
		
		//TODO check headers
		
		/*
		 * If the resource supports credentials add a single Access-Control-Allow-Origin header, with the value of the Origin header as value, 
		 * and add a single Access-Control-Allow-Credentials header with the case-sensitive string "true" as value.
		 * Otherwise, add a single Access-Control-Allow-Origin header, with either the value of the Origin header or the string "*" as value.
		 */
		if (allowOrigin != null) httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", allowOrigin);
		
		/*
		 * If method is a simple method this step may be skipped.
		 * Add one or more Access-Control-Allow-Methods headers consisting of (a subset of) the list of methods.
		 */
		
		//TODO returns only allowed methods
		if (allowMethod != null) httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", allowMethod);
		
		/*
		 * If each of the header field-names is a simple header and none is Content-Type, this step may be skipped.
		 * Add one or more Access-Control-Allow-Headers headers consisting of (a subset of) the list of headers.
		 */
		if (allowHeaders != null)httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", allowHeaders);
		
		
	    try {				
	    	if (responseBody != null) httpExchange.sendResponseHeaders(200, responseBody.getBytes("UTF-8").length);
	    	else httpExchange.sendResponseHeaders(200, 0);
	    	OutputStream os = httpExchange.getResponseBody();			    
		    if (responseBody != null) os.write(responseBody.getBytes("UTF-8"));
		    else os.write(0);
		    os.close();
	    } catch (IOException e) {
	    	logger.error(e.getMessage());
	    	return false;
	    }
		    		
		return false;
	}
	
	public static boolean processCORSPreFlightRequest(HttpExchange httpExchange){
		return processCORSPreFlightRequest(httpExchange,null);
	}
}
