/* This class implements a token handler used to assign tokens to requests and responses
    Copyright (C) 2016-2017 Luca Roffia (luca.roffia@unibo.it)

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

package arces.unibo.SEPA.server;

import java.util.Properties;
import java.util.Vector;

import arces.unibo.SEPA.application.Logger;
import arces.unibo.SEPA.application.Logger.VERBOSITY;

/**
 * Utility class to handle requests' tokens
 * 
 * 
* @author Luca Roffia (luca.roffia@unibo.it)
* @version 0.1
* */

public class TokenHandler {
	private String tag="TokenHandler";   
	
	private long timeout;	
	private long maxTokens;
	private Vector<Integer> jar=new Vector<Integer>();
	
	public TokenHandler(Properties properties) {
		if (properties == null) Logger.log(VERBOSITY.ERROR, tag, "Properties are null");
		else {
			this.timeout = Integer.parseInt(properties.getProperty("tokenTimeout", "0"));
			this.maxTokens = Integer.parseInt(properties.getProperty("maxTokens", "1000"));
		}
		for (int i=0; i < maxTokens; i++) jar.addElement(i);
	}
	
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}
	
	public long getTimeout() {
		return timeout;
	}
	
	/**
	 * Returns the number of available tokens
	 * @returns the number of available tokens
	 */
	public int availableTokens() {
		return jar.size();
	}
	
	/**
	 * Returns a new token if more tokens are available or null otherwise
	 * @returns an Integer representing the token
	 */
	public synchronized Integer getToken()
	{
		Integer token;
		
		synchronized (jar){
			if (jar.size() == 0){
				Logger.log(VERBOSITY.WARNING, tag,"No token available...wait...");
				try {
					jar.wait(timeout);
				} catch (InterruptedException e) {
					Logger.log(VERBOSITY.DEBUG, tag, e.getMessage());
					e.printStackTrace();
				}
			}
			if (jar.size()==0) return null;
		
			token =  jar.get(0);
			jar.removeElementAt(0);
		}
		
		Logger.log(VERBOSITY.DEBUG, tag, "Get token #"+token+" (Available: " + jar.size()+")");
		
		return token;	
	}

	/**
	 * Release an used token
	 * @returns true if success, false if the token to be released has not been acquired
	 */
	public synchronized boolean releaseToken(Integer token)
	{	
		boolean ret = true;
     	synchronized(jar) {
     		if (jar.contains(token)) {
     			ret = false;
     			Logger.log(VERBOSITY.WARNING, tag, "Request to release a unused token: "+token+" (Available tokens: " + jar.size()+")");	
     		}
     		else
     		{
         		jar.insertElementAt( token , jar.size());
         		jar.notify();
         		Logger.log(VERBOSITY.DEBUG, tag, "Release token #"+token+" (Available: " + jar.size()+")");
         	}	
     	}
     	
     	return ret;
	}	
}
