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
	 * Process a CORS pre-flight request. <br>
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
		String allowOrigin = null;
		List<String> origins = httpExchange.getRequestHeaders().get("Origin");
		if (origins.size() == 1) {
			allowOrigin = origins.get(0);
		}
		
		String allowMethod = null;
		List<String> methods = httpExchange.getRequestHeaders().get("Access-Control-Request-Method");
		if (methods.size() == 1) {
			allowMethod = methods.get(0);
		}
		
		String allowHeaders = null;
		List<String> headers = httpExchange.getRequestHeaders().get("Access-Control-Request-Method");
		for (String temp : headers) {
			if (allowHeaders == null) allowHeaders = temp;
			else allowHeaders = allowHeaders +","+temp;
		}
			
		if (allowOrigin != null) httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", allowOrigin);
		if (allowMethod != null) httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", allowMethod);
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
