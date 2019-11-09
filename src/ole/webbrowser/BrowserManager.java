package ole.webbrowser;

import java.util.Vector;

import android.content.SharedPreferences;

/**
 * Manages the {@link BrowserContent} items.
 */
public class BrowserManager {
	private static final String TAG_TABS = "tabs";
	private static final String TAG_LOADING_BLOCKED = "loadingBlocked";
	private static final String TAG_TAB_INDEX = "tab_index";
	private static final String TAG_FONT_SIZE_INDEX = "font_size_index";
	
	private int tabCount = 2;
	private Vector<BrowserContent> contents;
	private SharedPreferences prefs;
	private String contentUrl;			//new content url
	private int contentUrlPosition;
	private boolean loadingBlocked;
	private int initialTabIndex;
	private int fontSizeIndex;
	
	public BrowserManager(SharedPreferences prefs, BrowserActivity activity) {
		this.prefs = prefs;
		fontSizeIndex = prefs.getInt(TAG_FONT_SIZE_INDEX, BrowserContent.FONT_MEDIUM);
		contents = new Vector<BrowserContent>();
		load(prefs.getString(TAG_TABS, null), activity);
		loadingBlocked = prefs.getBoolean(TAG_LOADING_BLOCKED, false);
		initialTabIndex = prefs.getInt(TAG_TAB_INDEX, 0);
		//System.out.println("********* initial tab index:" + initialTabIndex);
	}
	
	public int getInitialTabIndex() {
		//System.out.println("initial tab index:" + initialTabIndex);
		return initialTabIndex;
	}
	
	public void setInitialTabIndex(int index) {
		initialTabIndex = index;
		prefs.edit().putInt(TAG_TAB_INDEX, initialTabIndex).commit();
	}
	
	private void load(String data, BrowserActivity activity) {
		if (data == null) {
			return;
		}
		try {
			String[] parts = data.split(Helper.DELIMITER);
			if (parts.length < 3) {
				return;
			}
			int i = 0;
			tabCount = 0;
			while (i < parts.length) {
				int max = Integer.parseInt(parts[i++]);
				int j = 0;
				tabCount++;
				Vector<BrowsingHistory> history = new Vector<BrowsingHistory>();
				while (j < max) {
					String title = parts[i++];
					String url = parts[i++];
					j++;
					history.add(new BrowsingHistory(title, url));
				}
				contents.add(new BrowserContent(activity, history, fontSizeIndex));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void addTab() {
		addTab(null);
	}
	public void addTab(String url) {
		contentUrlPosition = tabCount;
		contentUrl = url;
		tabCount++;
	}

	public int getTabCount() {
		return tabCount;
	}

	public void deleteTab(int position) {
		System.out.println("delete tab at position:" + position);
		BrowserContent item = contents.remove(position);
		item.dispose();
		tabCount--;
		System.gc();
	}
	
	public void stopOrReload(int position) {
		BrowserContent content = contents.get(position);
		content.stopOrReload();
	}
	
	public boolean isLoading(int position) {
		BrowserContent content = contents.get(position);
		return content.isLoading();
	}

	public BrowserContent getBrowserContent(int position, final BrowserActivity context) {	
		
		BrowserContent item = null;
		
		if (position < contents.size()) {
			item = contents.get(position);
		}
		
		if (item == null) {
			if (position == contentUrlPosition && contentUrl != null) {
				item = new BrowserContent(context, "New tab", contentUrl, fontSizeIndex);
			} else {
				item = new BrowserContent(context, fontSizeIndex);		
			}
			
			contents.insertElementAt(item, position);
		}
			
		return item;
	}

	public void setFontSizeIndex(int fontSizeIndex) {
		this.fontSizeIndex = fontSizeIndex;
	}

	public String getPageTitle(int position, BrowserActivity context) {
		BrowserContent item = getBrowserContent(position, context);
		if (item == null) {
			return "New tab";
		}
		String title = item.getTitle();
		if (title == null || title.length() < 1) {
			return "New tab";
		}
		return title;
	}
	
	public boolean save() {
		StringBuilder sb = new StringBuilder();
		int max = contents.size();
		for (int i = 0; i < max; i++) {
			BrowserContent content = contents.get(i);
			Vector<BrowsingHistory> history = content.getHistory();
			
			//add size and delimiter
			sb.append(history.size()).append(Helper.DELIMITER);
			Helper.storeBrowsables(history.toArray(), sb);
			if (i < max - 1) {
				sb.append(Helper.DELIMITER);
			}
		}
		prefs.edit().putString(TAG_TABS, sb.toString()).commit();
		prefs.edit().putBoolean(TAG_LOADING_BLOCKED, loadingBlocked).commit();
		prefs.edit().putInt(TAG_TAB_INDEX, initialTabIndex).commit();
		prefs.edit().putInt(TAG_FONT_SIZE_INDEX, fontSizeIndex).commit();
		return true;
	}
	
	public int getContentIndex(BrowserContent bContent) {
		for (int i = 0; i < contents.size(); i++) {
			if (contents.get(i).equals(bContent)) {
				return i;
			}
		}
		return -1;
	}
	
	void setBlockingLoading(boolean state) {
		System.out.println("block loading is now: " + state);
		loadingBlocked = state;
	}
	
	boolean isBlockingLoading() {
		return loadingBlocked;
	}
}
