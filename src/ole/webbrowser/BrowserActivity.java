package ole.webbrowser;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

/**
 * The main activity of the Web Browser.
 */
public class BrowserActivity extends FragmentActivity implements  ActionBar.TabListener {

	ActionBar mActionBar;
	BrowserManager contentManager;
	FrameLayout parentLayout;
	Bookmarks bookmarks;
	AlertDialog bookmarksDialog;
	AlertDialog contextDialog;
	HitTestResult hitResult;
	private String hitUrl;
	private Menu optionsMenu;
	private boolean browserToolbarShown;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//ensure progress bar is enabled
		getWindow().requestFeature(Window.FEATURE_PROGRESS);
		//ensure keyboard is not open after start
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.activity_browser);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		mActionBar = actionBar;
		mActionBar.setTitle("");
		mActionBar.setDisplayHomeAsUpEnabled(true);
		
		setEventListeners();

		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		contentManager = new BrowserManager(prefs, this);
		bookmarks = new Bookmarks(prefs);
	
		int initialTabIndex = contentManager.getInitialTabIndex();
		
		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < contentManager.getTabCount(); i++) {
			addNewTab(i == initialTabIndex);
		}
		browserToolbarShown = true;
	}
	
	private Tab addNewTab(boolean select) {
		int position = mActionBar.getTabCount();
		// Create a tab with text corresponding to the page title defined by
		// the adapter. Also specify this Activity object, which implements
		// the TabListener interface, as the callback (listener) for when
		// this tab is selected.
		Tab tab = mActionBar.newTab();
		tab.setText(contentManager.getPageTitle(position, this));
		tab.setTabListener(this);
		if (select) {
			mActionBar.addTab(tab, true);
		} else {
			mActionBar.addTab(tab);
		}
		return tab;
	}
	
	private void removeTab() {
		int count = mActionBar.getTabCount();
		if (count == 1) {
			return;
		}
		Tab tab = mActionBar.getSelectedTab();
		if (tab != null) {
			int position = tab.getPosition();
			System.out.println("position=" + position);
			contentManager.deleteTab(position);
			mActionBar.removeTab(tab);
		}
	}
	private void stopOrReload() {
		Tab tab = mActionBar.getSelectedTab();
		if (tab != null) {
			int position = tab.getPosition();
			contentManager.stopOrReload(position);
			setStopOrReloadIcon(contentManager.isLoading(position));
		}
	}
	
	private void setEventListeners() {
		
		ImageButton button = (ImageButton) findViewById(R.id.imageButton1);
         button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 onBackButtonPressed();
             }
         });

		 button = (ImageButton) findViewById(R.id.imageButton2);
         button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 onForwardButtonPressed();
             }
         });
         
		 button = (ImageButton) findViewById(R.id.imageButton3);
         button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 addNewTab(true);
                 contentManager.addTab();
             }
         });
         
         button = (ImageButton) findViewById(R.id.imageButton4);
         button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 removeTab();
                 contentManager.save();
             }
         });

         button = (ImageButton) findViewById(R.id.imageButton5);
         button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 addToBookmarks();
             }
         });

         button = (ImageButton) findViewById(R.id.imageButton6);
         button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 showBookmarks();
             }
         });
         
         button = (ImageButton) findViewById(R.id.imageButton7);
         button.setOnClickListener(new View.OnClickListener() {
             public void onClick(View v) {
                 stopOrReload();
             }
         }); 
         
         EditText editText = (EditText) findViewById(R.id.editText1);
         editText.setOnEditorActionListener(new OnEditorActionListener() {

             public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                 boolean handled = false;
                 if (actionId == EditorInfo.IME_ACTION_NEXT) {
                	 System.out.println("Url Clicked:" + v.getText());
                	 loadUrl(v.getText().toString());
                     handled = true;
                 } else {
                	 System.out.println("Editor event: " + actionId);
                 }
                 return handled;
             }
         });
	}

	private void showToast(String text) {
		Toast.makeText(this,text, Toast.LENGTH_SHORT).show();
	}

	private void setFontSize(int sizeIndex) {
		contentManager.setFontSizeIndex(sizeIndex);
		BrowserContent bContent = getCurrentTabBrowserContent();
		bContent.setFontSizeIndex(sizeIndex);
		updateFontSizeSubMenu(optionsMenu, bContent);
	}
	
	//called when Settings item is clicked
	public boolean onOptionsItemSelected(MenuItem item) {
		System.out.println("Item selected: " + item);
		
		switch (item.getItemId()) {
		case android.R.id.home: {
			toggleNavigationBarVisibility();
		} break;
		case R.id.action_export_bookmarks: {
			if (bookmarks.exportBookmarksToFile()) {
				showToast("Bookmarks exported.");
			} else {
				showToast("Export failed!");
			}
		} break;
		case R.id.action_import_bookamrks: {
			if (bookmarks.importBookmarksFromFile()) {
				showToast("Bookmarks imported OK");
			} else {
				showToast("Import failed!");
			}
		} break;
		case R.id.action_blocking_loading: {
			item.setChecked(!item.isChecked());
			contentManager.setBlockingLoading(item.isChecked());

		} break;
		case R.id.font_tiny: {
			setFontSize(BrowserContent.FONT_TINY);
		} break;
		case R.id.font_small: {
			setFontSize(BrowserContent.FONT_SMALL);
		} break;
		case R.id.font_medium: {
			setFontSize(BrowserContent.FONT_MEDIUM);
		} break;
		case R.id.font_big: {
			setFontSize(BrowserContent.FONT_BIG);
		} break;
		case R.id.font_huge: {
			setFontSize(BrowserContent.FONT_HUGE);
		} break;

		}
		return super.onOptionsItemSelected(item);
	}

	private void addToBookmarks() {
		int position = mActionBar.getSelectedTab().getPosition();
		BrowserContent item = contentManager.getBrowserContent(position, this);
		if (bookmarks.isBookmarkedItem(item.getTitle(), item.getUrl())) {
			Toast.makeText(this, "Already bookmarked: " + item.getTitle(), Toast.LENGTH_SHORT).show();
			return;
		}
		
		if (item != null) {
			String title = item.getTitle();
			String url = item.getUrl();
			bookmarks.addBookmark(title, url);
			Toast.makeText(this, "Added to bookmarks: " + title, Toast.LENGTH_SHORT).show();
			setStarredIcon(true);
		}
	}
	
	private void showBookmarks() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Bookmarks");
		builder.setView(createBookmarksView());
		
		bookmarksDialog = builder.create();
		bookmarksDialog.show();
	}
	
	private View createBookmarksView() {
		final ScrollView sv = new ScrollView(this);
		TableLayout tl = new TableLayout(this);

		int max = bookmarks.getSize();
		
		for (int i = 0; i < max; i++) {
			final Bookmark item = bookmarks.getBookmark(i);
			View view = View.inflate(this, R.layout.bookmarks_table, null);
			//set title
			TextView tv = (TextView) view.findViewById(R.id.column_title);
			tv.setText(item.title);
			//set url
			tv = (TextView) view.findViewById(R.id.column_url);
			tv.setText(item.url);
			
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
			tl.addView(view, lp);
			view.setOnClickListener(new OnClickListener() {	
				public void onClick(View v) {
					loadUrl(item.url);
					if (bookmarksDialog != null) {
						bookmarksDialog.hide();
						bookmarksDialog = null;
					}
				}
			});
		}
		sv.addView(tl);
		return sv;
	}
	
	private void showContextMenu() {
		if (hitResult == null) {
			System.out.println("Hit result is null ?");
			return;
		}
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Hyperlink options");
		builder.setView(createContextView());
		
		contextDialog = builder.create();
		contextDialog.show();
	}
	
	private void hideContexMenu() {
		if (contextDialog != null) {
			contextDialog.hide();
			contextDialog = null;
		}
	}
	
	private View createContextView() {
		final ScrollView sv = new ScrollView(this);
		TableLayout tl = new TableLayout(this);

		// Open in new tab item
		View view = addContextMenuItem(tl, "Open in new tab");
		view.setOnClickListener(new OnClickListener() {	
			public void onClick(View v) {
				contentManager.addTab(hitResult.getExtra());
				addNewTab(true);
				hideContexMenu();
			}
		});

		// Save to downloads... item
		int type = hitResult.getType();
		if ( 
			type != HitTestResult.EMAIL_TYPE && 
			type != HitTestResult.GEO_TYPE &&
			type != HitTestResult.PHONE_TYPE &&
			Helper.isDownloadableContent(hitUrl)
		) {
			view = addContextMenuItem (tl, "Save to Downloads...");
			view.setOnClickListener(new OnClickListener() {	
				public void onClick(View v) {
					startDownload(hitResult.getExtra());
					hideContexMenu();
				}
			});
		}

		sv.addView(tl);
		return sv;
	}
	

	private View addContextMenuItem(TableLayout tl, String text) {
		View view = View.inflate(this, R.layout.context_table, null);
		//set title
		TextView tv = (TextView) view.findViewById(R.id.column_title);
		tv.setText(text);
		
		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		tl.addView(view, lp);
		return view; 
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (!bookmarks.save()) {
			System.err.println("Error: failed to save bookmarks");
		}
		
		if (!contentManager.save()) {
			System.err.println("Error: failed to save opened tabs");
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.browser, menu);
		MenuItem item = menu.findItem(R.id.action_blocking_loading);
		item.setChecked(contentManager.isBlockingLoading());
		optionsMenu = menu;
		
		System.out.println("Create options menu...");
		updateFontSizeSubMenu(menu, getCurrentTabBrowserContent());
		return true;
	}
	
	
	private BrowserContent getCurrentTabBrowserContent() {
		ActionBar.Tab tab = mActionBar.getSelectedTab();
		final int position = tab.getPosition();
		return contentManager.getBrowserContent(position, this);
	}
	
	private void updateFontSizeSubMenu(Menu menu, BrowserContent bContent) {
		if (menu == null || bContent == null) {
			return;
		}
		
		int fontIndex = bContent.getFontSizeIndex();

		MenuItem item = menu.findItem(R.id.font_tiny);
		item.setChecked(fontIndex == 0);
		item = menu.findItem(R.id.font_small);
		item.setChecked(fontIndex == 1);
		item = menu.findItem(R.id.font_medium);
		item.setChecked(fontIndex == 2);
		item = menu.findItem(R.id.font_big);
		item.setChecked(fontIndex == 3);
		item = menu.findItem(R.id.font_huge);
		item.setChecked(fontIndex == 4);
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		final int position = tab.getPosition();
		contentManager.setInitialTabIndex(position);
		BrowserContent bContent = contentManager.getBrowserContent(position, this);
		bContent.onClick();
		WebView view = bContent.getWebView();
		if (parentLayout == null) {
			WebView oldView = (WebView) findViewById(R.id.webView);
			parentLayout = (FrameLayout) oldView.getParent();
		}
		//System.out.println("Tab selected! index=" + position );
		view.onResume();
		parentLayout.removeAllViews();
		parentLayout.addView(view);
		registerForContextMenu(parentLayout);
		updateTabAndUrl(bContent, tab, true);
		setProgress(100 * 100);//hide progress bar
		
		//update font size, depending on the current tab
		updateFontSizeSubMenu(optionsMenu, bContent);
	}

	public void onProgressChanged(BrowserContent bContent, int progress) {
     	// Activities and WebViews measure progress with different scales.
		// The progress meter will automatically disappear when we reach 100%
		int changeIndex = contentManager.getContentIndex(bContent);
		if (changeIndex < 0) {
			return;
		}
		ActionBar.Tab tab = mActionBar.getTabAt(changeIndex);
		boolean isCurrentTab = changeIndex == mActionBar.getSelectedNavigationIndex();
		
		if (isCurrentTab) {
			setProgress(progress * 100);
		}
		
		if (progress >= 100) {	
			updateTabAndUrl(bContent, tab, isCurrentTab);
			if (isCurrentTab) {
				setStopOrReloadIcon(false); // show reload icon
			}
			contentManager.save();
		} else {
			if (isCurrentTab) {
				setStopOrReloadIcon(true); // show stop icon
			}
		}
	}
	
	public void onReceivedTitle(BrowserContent bContent, String title) {
		int changeIndex = contentManager.getContentIndex(bContent);
		if (changeIndex < 0) {
			return;
		}
		ActionBar.Tab tab = mActionBar.getTabAt(changeIndex);
		tab.setText(title);
	}
	
	// Called after a long press on a spot in the web page.
	void setHitResult(HitTestResult result, String url) {
		hitResult = result;
		hitUrl = url;
		System.out.println("setHitResult=" + url);
		//once we get a url 
		if (url != null) {
			showContextMenu();
		}
	}
	
	@Override
	public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
		final int position = tab.getPosition();
		// Tab removed ?
		if (position < 0) {
			return;
		}
		BrowserContent bContent = contentManager.getBrowserContent(position, this);
		bContent.onClick();
		WebView view = bContent.getWebView();
		//System.out.println("Tab unselected! index=" + position );
		view.stopLoading();
		view.onPause();
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {

	}
	
	private void toggleNavigationBarVisibility() {
		View view = findViewById(R.id.browser_tool_bar);
		if (view != null) {
			if (view.isShown()) {
				view.setVisibility(View.GONE);
				view.setEnabled(false);
			} else {
				view.setVisibility(View.VISIBLE);
				view.setEnabled(true);
			}
		}
	}
	
	public void setMaxSize(boolean doMaximize) {
		View view = findViewById(R.id.browser_tool_bar);
		if (view != null) {
			if (doMaximize) {
				//hide toolbar
				browserToolbarShown = view.isShown();
				if (browserToolbarShown) {
					view.setVisibility(View.GONE);
					view.setEnabled(false);
				}
				
				//hide tab view
				mActionBar.hide();
				
				//ensure screen is not dimmed
				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
			} else
			//return to previous (normal) state
			{
				//ensure screen is dimmed
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
				
				//show tab view
				mActionBar.show();
				
				//show toolbar - if it was shown before
				if (browserToolbarShown) {
					view.setVisibility(View.VISIBLE);
					view.setEnabled(true);
				}
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			return onBackButtonPressed();
		}
		
	    // If it wasn't the Back key or there's no web page history, bubble up to the default
	    // system behavior (probably exit the activity)
	    return super.onKeyDown(keyCode, event);
	}
	
	private boolean onBackButtonPressed() {
		int position = mActionBar.getSelectedTab().getPosition();
		
		BrowserContent content = contentManager.getBrowserContent(position, this);
		
		if (content != null) {
			return content.goBack();
		}
		return false;
	}

	private boolean onForwardButtonPressed() {
		int position = mActionBar.getSelectedTab().getPosition();
		
		BrowserContent content = contentManager.getBrowserContent(position, this);
		
		if (content != null) {
			return content.goForward();
		}
		return false;
	}
	
	private boolean loadUrl(String url) {
		int position = mActionBar.getSelectedTab().getPosition();
		BrowserContent bc = contentManager.getBrowserContent(position, this);
		WebView webView = bc.getWebView();
		
		if (webView != null && url != null) {
			if (!(url.startsWith("http://") ||url.startsWith("https://") )) {
				url = "http://" + url.trim();
			}
			bc.onClick();
			webView.loadUrl(url.trim());
			return true;
		}
		return false;
	}
	
	private void updateTabAndUrl(BrowserContent bContent, ActionBar.Tab tab, boolean updateUrl) {
		String title = bContent.getTitle();
		tab.setText(title);

		if (updateUrl) {
			String url = bContent.getUrl();
			EditText editText = (EditText) findViewById(R.id.editText1);
			editText.setText(url);
			
			setStarredIcon(bookmarks.isBookmarkedItem(title, url));
		}
	}
	
	
	private void setStopOrReloadIcon(boolean stop) {
		ImageButton button = (ImageButton) findViewById(R.id.imageButton7);
		if (stop) {
			button.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
		} else {
			button.setImageResource(android.R.drawable.ic_menu_rotate);
		}
	}
	
	private void setStarredIcon(boolean starred) {
		ImageButton button = (ImageButton) findViewById(R.id.imageButton5);
		if (starred) {
			button.setImageResource(android.R.drawable.btn_star_big_on);
		} else {
			button.setImageResource(android.R.drawable.btn_star_big_off);
		}
	}


	private void startDownload(String url) {
		if (hitUrl != null) {
			url = hitUrl;
		}
		Uri uri = Uri.parse(url);
		String fileName = uri.getLastPathSegment();
		//System.out.println("filename = " + fileName);
		if (fileName == null) {
			System.err.println("Download failed, url: " + url);
			return;
		}
		
		try {
			// credits: http://stackoverflow.com/questions/525204/android-download-intent
			DownloadManager.Request r = new DownloadManager.Request(uri);
			r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
		
			// This put the download in the same Download dir the browser uses
			r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
		
			// Start download
			DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
			dm.enqueue(r);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	boolean isBlockingLoading() {
		return contentManager.isBlockingLoading();
	}
}
