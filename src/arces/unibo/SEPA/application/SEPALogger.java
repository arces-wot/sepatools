/* This is the utility class for logging
Copyright (C) 2016-2017 Luca Roffia (luca.roffia@unibo.it)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package arces.unibo.SEPA.application;

//import java.util.ArrayList;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;

public class SEPALogger {

	// Log4J2 logger
	private static final Logger logger = LogManager.getLogger();

	final Level EVAL = Level.forName("EVAL", 700);
	
	public static enum VERBOSITY { 
		DEBUG, INFO, WARNING, ERROR, FATAL, EVAL;
		
		@Override
		public String toString() {
			switch(this){
				case DEBUG: return "DEBUG";
				case INFO: return "INFO";
				case WARNING: return "WARNING";
				case ERROR: return "ERROR";
				case FATAL: return "FATAL";
				case EVAL: return "EVAL";
				default: return "";
			}
		}
	};

	public static synchronized void log(VERBOSITY level, String tag,String message) {

		String messageOut = tag+" -- "+message;

		switch(level.toString()){	
			case "DEBUG":
				logger.debug(messageOut);
				break;
			case "INFO": 
				logger.info(messageOut);
				break;
			case "WARNING": 
				logger.warn(messageOut);
				break;
			case "ERROR": 
				logger.error(messageOut);
				break;
			case "FATAL": 
				logger.fatal(messageOut);
				break;
			case "EVAL": 
				logger.log(Level.getLevel("EVAL"), messageOut);
				break;	
		}				
	}
}