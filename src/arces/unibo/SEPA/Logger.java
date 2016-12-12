package arces.unibo.SEPA;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
	private static String BUNDLETAGS = "timing,LOGGER";
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
		configuration.put("BUNDLETAGS", BUNDLETAGS);
		configuration.put("VERBOSITY", verbosity.toString());
		configuration.put("CONSOLELOG",  Boolean.toString(consoleLog));
		configuration.put("FILELOG",  Boolean.toString(fileLog));
		
		try  
		{
			configuration.load(new FileInputStream(PROPERTIES_FILE));
		}
		catch(Exception e){ 
			Logger.log(VERBOSITY.ERROR, "LOGGER", e.getMessage());
			try {
				configuration.store(new FileOutputStream(PROPERTIES_FILE), "Default configuration");
			} catch (FileNotFoundException e1) {
				Logger.log(VERBOSITY.FATAL, "LOGGER", PROPERTIES_FILE+ " not found");
			} catch (IOException e1) {
				Logger.log(VERBOSITY.FATAL, "LOGGER", PROPERTIES_FILE+ " can not write");
			}
			return;
		}

		if (configuration.getProperty("BUNDLETAGS") != null) {
			String[] enabledBundles = configuration.getProperty("BUNDLETAGS").split(",");
			if (enabledBundles != null) for (String tag : enabledBundles) Logger.registerTag(tag);
		}
	
		if (configuration.getProperty("VERBOSITY")!=null) {
			switch(configuration.getProperty("VERBOSITY")) {
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
		}
		else Logger.setVerbosityLevel(VERBOSITY.INFO);
		
		if (configuration.getProperty("CONSOLELOG") != null) {
			if (Boolean.parseBoolean(configuration.getProperty("CONSOLELOG"))) Logger.enableConsoleLog();
			else Logger.disableConsoleLog();
		}
		else {
			Logger.enableConsoleLog();
			Logger.log(VERBOSITY.INFO, "LOGGER", "Add CONSOLELOG=FALSE to "+PROPERTIES_FILE+" to disable console messages");
		}

		if (configuration.getProperty("FILELOG") != null) {
			if (Boolean.parseBoolean(configuration.getProperty("FILELOG"))) Logger.enableFileLog();
			else Logger.disableFileLog();
		}
		else {
			Logger.enableFileLog();
			Logger.log(VERBOSITY.INFO, "LOGGER",  "Add FILELOG=FALSE to "+PROPERTIES_FILE+" to disable file logging");
		}					
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
			if(!tags.contains(tag) && !tag.equals("LOGGER")) return;		
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