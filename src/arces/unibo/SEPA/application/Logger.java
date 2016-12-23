package arces.unibo.SEPA.application;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

public class Logger {
	private static boolean consoleLog = false;
	private static boolean fileLog = false;
	private static VERBOSITY verbosity = VERBOSITY.INFO;
	
	private static FileWriter file = null; 
	private static String filename = "";
	
	private static ArrayList<String> tags = new ArrayList<String>();
	private static Properties configuration = new Properties();
	private static String PROPERTIES_FILE ="logging.properties";
	
	public static enum VERBOSITY { 
		DEBUG, INFO, WARNING, ERROR, FATAL;
		
		@Override
		public String toString() {
			switch(this){
				case DEBUG: return "DEBUG";
				case INFO: return "INFO";
				case WARNING: return "WARNING";
				case ERROR: return "ERROR";
				case FATAL: return "FATAL";
				default: return "";
			}
		}
	};
	
	public static void loadSettings(){
		FileInputStream in;
		try {
			in = new FileInputStream(PROPERTIES_FILE);
		} catch (FileNotFoundException e) {
			Logger.log(VERBOSITY.ERROR, "LOGGER", "Error on opening properties file: "+PROPERTIES_FILE);
			return ;
		}
		try {
			configuration.load(in);
		} catch (IOException e) {
			Logger.log(VERBOSITY.ERROR, "LOGGER", "Error on loading properties file: "+PROPERTIES_FILE);
			return ;
		}
		try {
			in.close();
		} catch (IOException e) {
			Logger.log(VERBOSITY.ERROR, "LOGGER", "Error on closing properties file: "+PROPERTIES_FILE);
			return ;
		}

		String property = null;
		
		property = configuration.getProperty("BUNDLETAGS","*"); 
		String[] enabledBundles = property.split(",");
		if (enabledBundles != null) for (String tag : enabledBundles) Logger.registerTag(tag);
		
		property = configuration.getProperty("VERBOSITY","INFO");
		switch(property) {
			case "DEBUG":
				Logger.setVerbosityLevel(VERBOSITY.DEBUG);
				break;
			case "INFO":
				Logger.setVerbosityLevel(VERBOSITY.INFO);
				break;
			case "WARNING":
				Logger.setVerbosityLevel(VERBOSITY.WARNING);
				break;
			case "ERROR":
				Logger.setVerbosityLevel(VERBOSITY.ERROR);
				break;
			case "FATAL":
				Logger.setVerbosityLevel(VERBOSITY.FATAL);
				break;
			default:
				Logger.setVerbosityLevel(VERBOSITY.INFO);
				break;	
		}
		
		property = configuration.getProperty("CONSOLELOG","false");
		if (Boolean.parseBoolean(property)) Logger.enableConsoleLog();
		else Logger.disableConsoleLog();
		
		property = configuration.getProperty("FILELOG","false");
		if (Boolean.parseBoolean(property)) Logger.enableFileLog();
		else Logger.disableFileLog();					
	}
	
	public static void registerTag(String tag) {
		tags.add(tag);
	}
	
	public static void enableConsoleLog(){
		consoleLog = true;
	}
	
	public static void disableConsoleLog(){
		consoleLog = false;
	}
	
	public static void enableFileLog(){
		fileLog = true;
		
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String timestamp = sdf.format(date);
		timestamp = timestamp.replaceAll("-", "_");
		timestamp = timestamp.replaceAll(":", "_");
		timestamp = timestamp.replaceAll(" ", "_");
		
		filename = "Log"+timestamp+".txt";
	}
	
	public static void disableFileLog(){
		fileLog = false;
	}
	
	public static void setVerbosityLevel(VERBOSITY level) {
		verbosity = level;
	}
	
	public static synchronized void log(VERBOSITY level, String tag,String message) {
		long nano = System.nanoTime();
		
		int nTab = 20 - tag.length();
		
		if (level.compareTo(VERBOSITY.WARNING)<=0) {
			if(!tags.contains(tag) && !tag.equals("LOGGER") && !tags.contains("*")) return;		
			if (!consoleLog && !fileLog) return;
			if (level.compareTo(verbosity) < 0) return; 
		}
		
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		String timestamp = sdf.format(date);
		
		for (int i=0; i < nTab; i++) tag += " ";
		
		String messageOut = timestamp+"\t"+nano+"\t"+level.toString()+"\t"+tag+"\t"+message;
		
		if (consoleLog || level.compareTo(VERBOSITY.WARNING) > 0) 
			if (level.compareTo(VERBOSITY.WARNING) > 0) System.err.println(messageOut);
			else System.out.println(messageOut);
		if (fileLog)
			try {
				file = new FileWriter(filename,true);
				file.write(messageOut+"\n");
				file.close();
			} catch (IOException e) {
				e.printStackTrace();
				fileLog = false;
			}
	}
}