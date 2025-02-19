package io.cubyz;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A static wrapper for java.util.logging.Logger.
 * Supports limited functionality.
 */

public class Logger {
	private static java.util.logging.Logger logger;
	static {
		logger = java.util.logging.Logger.getGlobal();
		logger.setLevel(java.util.logging.Level.ALL);
		File logs = new File("logs");
		if (!logs.exists()) {
			logs.mkdir();
		}
		logger.setUseParentHandlers(false);
		logger.addHandler(new Handler() {
			
			DateFormat format = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
			DateFormat logFileFormat = new SimpleDateFormat("YYYY-MM-dd-HH-mm-ss");
			
			FileOutputStream latestLogOutput;
			
			{
				try {
					latestLogOutput = new FileOutputStream("logs/latest.log");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			@Override
			public void close() throws SecurityException {
				try {
					flush();
					if (latestLogOutput != null)
						latestLogOutput.close();
					latestLogOutput = null;
				} catch (Exception e) {
					System.err.println(e);
					throw new SecurityException(e);
				}
			}

			@Override
			public void flush() {
				System.out.flush();
				try {
					if (latestLogOutput != null) {
						latestLogOutput.flush();
						Files.copy(Paths.get("logs/latest.log"), Paths.get("logs/" + logFileFormat.format(Calendar.getInstance().getTime()) + ".log"));
					}
				} catch (Exception e) {
					logger.throwing("CubyzLogger", "flush", e);
				}
			}

			@Override
			public void publish(LogRecord log) {
				Date date = new Date(log.getMillis());
				
				StringBuilder sb = new StringBuilder();
				
				sb.append("[" + format.format(date) + " | " + log.getLevel() + " | " + Thread.currentThread().getName() + "] ");
				sb.append(log.getMessage() + "\n");
				
				if (log.getLevel().intValue() >= Level.WARNING.intValue()) {
					System.err.print(sb.toString());
				} else {
					System.out.print(sb.toString());
				}
				
				if (latestLogOutput != null) {
					try {
						latestLogOutput.write(sb.toString().getBytes("UTF-8"));
					} catch (Exception e) {
						throw new Error(e);
					}
				}
			}
			
		});
	}
	
	public static void log(String msg) {
		logger.info(msg);
	}
	
	public static void warning(String msg) {
		logger.warning(msg);
	}
	
	public static void severe(String msg) {
		logger.severe(msg);
	}
	
	public static void throwable(Throwable t) {
		StringWriter w = new StringWriter();
		PrintWriter pw = new PrintWriter(w);
		t.printStackTrace(pw);
		pw.close();
		logger.log(Level.SEVERE, w.toString());
	}
}
