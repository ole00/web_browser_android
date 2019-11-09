package ole.webbrowser;


import java.util.Vector;

import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.FrameLayout;

/*
 * A content of a single browser tab - contains the web view and can be navigated back and forth.
 */

public class BrowserContent implements OnLongClickListener {
	public static String HOME_URL = "http://www.google.co.uk";
	
	public static final int FONT_TINY = 0;
	public static final int FONT_SMALL = 1;
	public static final int FONT_MEDIUM = 2;
	public static final int FONT_BIG = 3;
	public static final int FONT_HUGE = 4;
	
	private static final int[] FONT_SIZES = {12, 16, 20, 24, 28};
	private static final int[] TEXT_ZOOM = {70, 90, 100, 110, 130};
	
	private static final int DEFAULT_FONT_SIZE_INDEX = FONT_MEDIUM;

	private static final int MAX_HISTORY = 2;
	
	private final BrowserActivity context;
	private WebView webView;
	Vector<BrowsingHistory> history;
	private String title;
	private String url;
	private boolean loading;
	private long clickTime;
	private int fontSizeIndex;
	private View fullScreenView;
	
	BrowserContent(BrowserActivity context, int fontSizeIndex) {
		this.context = context;
		history = new Vector<BrowsingHistory>();
		this.fontSizeIndex = fontSizeIndex; 
		createWebView();
	}

	BrowserContent(BrowserActivity context, String title, String url, int fontSizeIndex) {
		this.context = context;
		history = new Vector<BrowsingHistory>();
		this.title = title;
		this.url = url;
		this.fontSizeIndex = fontSizeIndex;
	}

	public BrowserContent(BrowserActivity context, Vector<BrowsingHistory> history, int fontSizeIndex) {
		this.context = context;
		this.history = history;
		if (history == null) {
			history = new Vector<BrowsingHistory>();
		} else 
		if (history.size() > 0){
			BrowsingHistory item = removeLastHistoryItem();
			title = item.getTitle();
			url = item.getUrl();
		}
		this.fontSizeIndex = fontSizeIndex;
	}
	
	public Vector<BrowsingHistory> getHistory() {
		storeHistory();
		return history;
	}
	
	public void dispose() {
		if (webView == null) {
			return;
		}
		webView.stopLoading();
		webView.onPause();
		webView.destroy();
		webView = null;
	}
	
	public String getTitle() {
		if (webView != null) {
			String newTitle = webView.getTitle();
			if (newTitle != null && newTitle.length() > 0) {
				title = newTitle;
			}
		}
		return title;
	}
	
	public String getUrl() {
		if (webView != null) {
			url = webView.getUrl();	
		}
		return url;
	}
	
	public WebView getWebView() {
		if (webView == null) {
			createWebView();
		}
		return webView;
	}
	
	public void resolve() {
		if (webView == null) {
			createWebView();
		}
	}

	private BrowsingHistory removeLastHistoryItem() {
		if (history != null && history.size() > 0) {
			BrowsingHistory item = history.lastElement();
			history.removeElement(item);
			return item;
		}
		return null;
	}
	
	public boolean goBack() {
		if (webView == null) {
			return false;
		}
		if (webView.canGoBack()) {
			onClick();
			webView.goBack();
			removeLastHistoryItem();
			return true;
		} else
		//stored history exists
		if (history.size() > 1) {
			onClick();
			BrowsingHistory item = removeLastHistoryItem(); // current item
			item = removeLastHistoryItem(); // previous item
			title = item.getTitle();
			url = item.getUrl();
			webView.loadUrl(url);
			loading = true;
			return true;
		}
		return false;
	}
	
	public boolean goForward() {
		if (webView == null) {
			return false;
		}
		if (webView.canGoForward()) {
			onClick();
			webView.goForward();
			return true;
		}
		return false;
	}

	public void setFontSizeIndex(int fontSize) {
		fontSizeIndex = fontSize;
		if (webView != null) {
			WebSettings webSettings = webView.getSettings();
			System.out.println("Setting font size=" + fontSizeIndex);
			webSettings.setDefaultFontSize(FONT_SIZES[fontSizeIndex]);
			webSettings.setTextZoom(TEXT_ZOOM[fontSizeIndex]);
		}
	}
	
	public int getFontSizeIndex() {
		return fontSizeIndex;
	}
	
	private void createWebView() {
		webView = new WebView(context);
		final BrowserContent bContent = this;
		webView.setWebViewClient(new CustomWebViewClient(context, this));
		
		WebSettings webSettings = webView.getSettings();
		webSettings.setJavaScriptEnabled(true);
		webSettings.setDefaultFontSize(FONT_SIZES[fontSizeIndex]);
		//webSettings.setGeolocationEnabled(false);
		//webSettings.setUseWideViewPort(true);
		webSettings.setTextZoom(TEXT_ZOOM[fontSizeIndex]);
		
		webView.setWebChromeClient(new WebChromeClient() {
			public void onProgressChanged(WebView view, int progress) {
					if (progress < 100) {
						//loading = true;
					}
					context.onProgressChanged(bContent, progress);
			}

			public void onReceivedTitle(WebView view, String title) {
					bContent.title = title;
					context.onReceivedTitle(bContent, title);
				
			}

			//implementing onShowCustomView will enable full screen icon on YouTube web site.
			//What it does: it hides the normal web content and plugs-in a new video view (fullScreenView)
			//into web content view parent.
			@Override
			public void onShowCustomView(View view, CustomViewCallback callback) {
				//ensure the current video play-back is hidden
				hideFullScreenView();

				ViewParent parentView = webView.getParent();
				
				if (parentView instanceof ViewGroup) {
					//System.out.println("It's a view group");
					fullScreenView = view;
					ViewGroup viewGroup = (ViewGroup) parentView;
					webView.setVisibility(View.INVISIBLE);
					viewGroup.addView(fullScreenView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
					fullScreenView.setVisibility(View.VISIBLE);
					fullScreenView.setBackgroundColor(Color.BLACK);
					
					if (context != null) {
						// this will ensure all furniture (tabs, URL bar, tool bar etc.) is hidden
						context.setMaxSize(true);
					}
				}
			}

			@Override
			public void onHideCustomView() {
				hideFullScreenView();
				webView.setVisibility(View.VISIBLE);
				// this will ensure all furniture (tabs, URL bar, tool bar etc.) is displayed
				context.setMaxSize(false);
				super.onHideCustomView();
			}
			
			private void hideFullScreenView() {					
				if (fullScreenView == null) {
					return;
				}
				ViewParent parentView = webView.getParent();
			
				if (parentView instanceof ViewGroup) {
					((ViewGroup)parentView).removeView(fullScreenView);
				}
				fullScreenView.setVisibility(View.INVISIBLE);
				fullScreenView = null;
			}
			
		});
		webView.setLongClickable(true);
		webView.setOnLongClickListener(this);
		clickTime = System.currentTimeMillis();
		
		if (url == null) {
			webView.loadUrl(HOME_URL);
		} else {
			webView.loadUrl(url);
		}
		loading = true;
	}

	void onPageFinished() {
		clickTime = System.currentTimeMillis();
		loading = false;
		onClick();
		getTitle();
		getUrl();
		storeHistory();
	}
	
	boolean shouldInterceptRequest(String url) {
		//no content blocking -> allow all requests
		if (!context.isBlockingLoading()) {
			return true;
		}
		
		if (loading) {
			return true;
		}
		//15 seconds to download the page
		return System.currentTimeMillis() - clickTime < 5000;
	}
	
	void stopOrReload() {
		if (webView  == null) {
			return;
		}
		if (loading) {
			onClick();
			webView.stopLoading();
			loading = false;
		} else {
			webView.reload();
			loading = true;
		}
	}
	
	boolean isLoading() {
		return loading;
	}
	
	private void storeHistory() {
		if (url == null || title == null || url.length() < 1) {
			return;
		}
		
		//check current item is already in history
		if (history.size() > 0) {
			BrowsingHistory item = history.lastElement();
			if (item.title.equals(title) && item.url.equals(url)) {
				return;
			}
		}
		
		//check too many items in history
		if (history.size() > MAX_HISTORY) {
			history.removeElementAt(0);
		}

		history.add(new BrowsingHistory(title, url));
	}

	public boolean onLongClick(View v) {
		if (webView != v) {
			return false;
		}
		
		final HitTestResult hit = webView.getHitTestResult();
		//System.out.println("Long click on: " + hit.getType() + " " + hit.getExtra() + " ->" + hit);
		Message message = new Message();
		message.setTarget(new Handler() {
			 @Override
			 public void handleMessage(Message msg) {
				 System.out.println("message handled!");
				 super.handleMessage(msg);
				 String url = msg.getData().getString("url");
				 context.setHitResult(hit, url);
			 }
		});
		webView.requestFocusNodeHref(message);
		
		//context.setHitResult(hit, null);
		//always return false to ensure the activity.onCreateContextMenu() is called afterwards
		return true;
	}


	void onClick() {
		//System.out.println("click");
		clickTime = System.currentTimeMillis();
	}


}
