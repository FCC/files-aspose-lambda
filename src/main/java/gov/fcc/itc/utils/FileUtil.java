package gov.fcc.itc.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class FileUtil {

    /**
     * Checks if a file exists.
     *
     * @param fileName the file name
     * @return true if it exists
     */
	public static boolean fileExists(String fileName) {
		
		return (Files.exists(Paths.get(fileName)));
	}
	
	
    /**
     * Gets the extension of the file.
     *
     * @param fileName the file name
     * @return file extension
     */
	public static String getFileExtension (String fileName) {

		String extension = "";

		if (fileName != null) {
			int i = fileName.lastIndexOf('.');
			int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

			if (i > p) {
				extension = fileName.substring(i+1);
			}
		}
		return extension;
	}

	/**
     * Gets the file name without extension of the file.
     *
     * @param fileName the file name
     * @return file name without extension
     */
	public static String getFileNameNoExtension (String fileName) {

		String file = "";

		if (fileName != null) {
			int i = fileName.lastIndexOf('.');
			int p = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));

			if (i > p) {
				file = fileName.substring(0, i);
			}
		}
		return file;
	}
	
	/**
	 *  Convert byte size to legible format in SI and binary units
	 *  
	 *  @param bytes - length of file in long
	 *  @param si - unit true-SI and false-binary
	 *  @return formated file size with unit
	 */
	public static String formatFileSize (long bytes, boolean si) {
	    int unit = si ? 1000 : 1024;
	    if (bytes < unit) return bytes + " B";
	    int exp = (int) (Math.log(bytes) / Math.log(unit));
	    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
	    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
	}
}
