package edu.yu.cs.com3800;

import java.io.IOException;
import java.util.Calendar;
import java.util.logging.*;

public interface LoggingServer {

	default Logger initializeLogging(String str) {
		Logger logger = Logger.getLogger(str);
		FileHandler fh = null;
		try {
			fh = new FileHandler(str + Calendar.getInstance().getTime().toString().replaceAll(":", "-") + ".log");
		} catch (SecurityException | IOException e1) {
			e1.printStackTrace();
		}
		logger.addHandler(fh);
		logger.setLevel(Level.ALL);
		logger.setUseParentHandlers(false);
		SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
		fh.setLevel(Level.ALL);
		return logger;
    }

}