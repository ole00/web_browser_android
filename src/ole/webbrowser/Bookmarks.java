package ole.webbrowser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Vector;

import android.content.SharedPreferences;
import android.os.Environment;

/**
 * A collection of bookmarks. Loading & saving support.
 */
public class Bookmarks {
	// "/storage/sdcard1/wb_bookmarks.bin"
	//"/storage/emulated/wb_bookmarks.bin"
	private static final String EXP_IMP_FILE = "/wb_bookmarks.bin";
	
	private Vector<Bookmark> list;
	SharedPreferences prefs;
	private int changeCount;
	private String expImpFilename;
	
	public Bookmarks(SharedPreferences prefs) {
		this.prefs = prefs;
		list = new Vector<Bookmark>();
		
		setExpImpFilename();
		
		load(prefs.getString("bookmarks", null));
	}
	
	private void setExpImpFilename() {
		File f = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
		expImpFilename = f.getAbsolutePath() + EXP_IMP_FILE;
		System.out.println("file=" + expImpFilename);
		
	}
	
	public boolean exportBookmarksToFile() {
		//make sure the new bookmarks are stored
		changeCount = 1;
		save();
		return exportToFile(prefs.getString("bookmarks", null));
	}
	
	public boolean importBookmarksFromFile() {
		String data = importFromFile();
		if (data == null) {
			return false;
		}
		Vector<Bookmark> listBackup = (Vector<Bookmark>)list.clone();
		
		list.removeAllElements();
		if (load(data)) {
			//make sure the new bookmarks are also stored
			changeCount = 1;
			save();
			return true;
		}
		//restore backup
		list = listBackup;
		return false;
	}
	
	private boolean exportToFile(String data) {
		try {
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(expImpFilename));
			dos.writeUTF(data);
			dos.close();
		} catch (Exception e) {
			System.out.println("failed to export bookmark data");
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private String importFromFile() {
		try {
			DataInputStream din = new DataInputStream(new FileInputStream(expImpFilename));
			String result = din.readUTF();
			din.close();
			return result;
		} catch (Exception e) {
			System.out.println("failed to import bookmark data");
			e.printStackTrace();
			return null;
		}
	}

	
	private boolean load(String data) {
		if (data == null) {
			return false;
		}
		
		try {
			String[] parts = data.split(Helper.DELIMITER);
			int max = parts.length;
			
			int i = 0;
			while(i < max) {
				String title = parts[i++];
				String url = parts[i++];
				list.add(new Bookmark(title, url));
				System.out.println("bookmark title=" + title + " url=" + url);
			}
			
			/*
			//when importing save it now
			{
				changeCount = 1;
				save();
			}
			*/
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public int getSize() {
		return list.size();
	}
	
	public Bookmark getBookmark(int index) {
		if (index < 0 || index >= list.size()) {
			return null;
		}
		return list.get(index);
	}
	
	public void addBookmark(String title, String url) {
		list.add(new Bookmark(title, url));
		changeCount ++;
	}
	
	public boolean save() {
		if (changeCount == 0) {
			return true;
		}

		return Helper.saveBrowsables(prefs, "bookmarks", list.toArray());
	}
	
	public boolean isBookmarkedItem(String title, String url) {
		if (list == null ) {
			return false;
		}
		
		for (int i = 0; i < list.size(); i++) {
			Bookmark b = list.get(i);
			if (b.getTitle().equals(title) && b.getUrl().equals(url)) {
				return true;
			}
		}
		return false;
	}

}
