package ole.webbrowser;

import java.io.ByteArrayInputStream;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

/**
 * This class controls the flow of http traffic through the browser.
 */
public class CustomWebViewClient extends WebViewClient  {
	private static boolean VERBOSE = false;
	
	Context context;
	BrowserContent content;
	WebResourceResponse dummyResponse;

	
	public CustomWebViewClient(Context context, BrowserContent content) {
		this.context = context;
		this.content = content;
		dummyResponse = new WebResourceResponse("text/html", "UTF-8", new ByteArrayInputStream(new byte[] {0}));
	}
	
	private String getShortUrl(String url) {
		if (url == null) {
			return null;
		}
		return  (url.length() > 40 ?  url.substring(0, 40) : url);
	}
	
	@Override
	public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
		boolean allow = content.shouldInterceptRequest(url);
		if (VERBOSE) {
			System.out.println("shouldInterceptRequest: " + getShortUrl(url));
			System.out.println("Allow? " + allow);
		}
		if (allow) {
			return super.shouldInterceptRequest(view, url);
		} else {
			return dummyResponse;
		}
	}

	@Override
	public void onLoadResource(WebView view, String url) {
		if (VERBOSE) {
			System.out.println("on load resource: " + getShortUrl(url));
		}
		super.onLoadResource(view, url);
	}

	@Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
		if (VERBOSE) {
			System.out.println("************* shouldOverrideUrlLoading...");
		}
		content.onClick();
		return false;
    }
	
	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
	   Toast.makeText(context, "Oh no! " + description, Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		super.onPageFinished(view, url);
		content.onPageFinished();
	}
	
	

}
