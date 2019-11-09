package ole.webbrowser;

/**
 * A browsing history item.
 */
public class BrowsingHistory implements Browsable {
	String title;
	String url;
	
	public BrowsingHistory(String title, String url) {
		
		if (title == null || title.length() < 1) {
			title = "?";
		}
		this.title = title;
		
		if (url == null || url.length() < 1) {
			url = "?";
		}
		this.url = url;
	}


	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}
	
}
