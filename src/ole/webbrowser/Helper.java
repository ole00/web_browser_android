package ole.webbrowser;

import android.content.SharedPreferences;

/**
 * Miscellaneous helper functions.
 */
public class Helper {
	static final String[] DOWNLOADABLE_FILE_EXTENSIONS = {
		".txt", ".pdf", ".doc", ".xls", ".ppt", ".eps",
		".png", ".jpg", ".jpeg", ".gif", ".bmp", ".tif", ".svg", ".raw",
		".zip", ".gz", ".tgz" , ".tbz", ".xz", ".7z", ".bz", ".tar", ".rar", ".cbz", ".cbr",
		".mp3", ".ogg", ".wav", ".3g",
		".mp4", ".mkv", ".avi", ".ts",
	};
	
	
	static String DELIMITER = "->#<-";
	
	static boolean saveBrowsables(SharedPreferences prefs, String id, Object[] list) {
		if (prefs == null || list == null) {
			return false;
		}
		if (list.length == 0) {
			return true;
		}
		
		StringBuilder sb = new StringBuilder();
		storeBrowsables(list, sb);
		return prefs.edit().putString(id, sb.toString()).commit();
	}
	
	// Convert Browsables list to a string builder
	static void storeBrowsables(Object[] list, StringBuilder sb) {
		final int max = list.length;
		for (int i = 0; i < max; i++) {
			Browsable item = (Browsable)list[i];
			sb.append(item.getTitle()).append(DELIMITER);
			sb.append(item.getUrl());
			if (i < max - 1) {
				sb.append(DELIMITER);
			}
		}
	}
	
	static boolean isDownloadableContent(String url) {
		if (null == url) {
			return false;
		}
		url = url.toLowerCase();
		for (int i = 0; i < DOWNLOADABLE_FILE_EXTENSIONS.length; i++) {
			if (url.endsWith(DOWNLOADABLE_FILE_EXTENSIONS[i])) {
				return true;
			}
		}
		return false;
	}
}
