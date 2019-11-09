package ole.webbrowser;

/**
 * Web browser bookmark.
 */
public class Bookmark implements Browsable{
	public static final String UNDEFINED = "-undefined-";
	String title;
	String url;
	
	public Bookmark(String title, String url) {
		this.title = title;
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}
}
