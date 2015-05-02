package com.pasdam.universalsearch;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pasdam.opensearch.description.OpenSearchDescription;
import com.pasdam.quickaction.ActionItem;
import com.pasdam.quickaction.QuickAction;

/**
 * Activiy with the search panel and the results list
 * 
 * @author paco
 * @version 0.1
 */
public class SearchActivity extends Activity {
	
	public static final String LOG = "UniversalSearch";
	
	private static final int BUTTON_ID_APP = 0;
	private static final int BUTTON_ID_SEARCH = 1;
	
	private ImageView appIconBtn;
	private AutoCompleteTextView srcTxt;
	private Button goBtn;
	private ListView resultsList;
	private ArrayAdapter<ResultItem> resultsAdapter;
	
	// preferences
	private boolean suggests;
	private int numberOfSuggests;
	private boolean loadMore;
	private int numberOfResults;

	private QuickAction pluginsQA;
	private Plugin plugin;
	private String lastSearch;
	private AlertDialog dialog;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		// set the UI layout
		setContentView(R.layout.search_main);
		
		// obtain widgets
		this.appIconBtn = (ImageView) findViewById(R.id.search_app_icon);
		this.srcTxt = (AutoCompleteTextView) findViewById(R.id.search_src_text);
		this.goBtn = (Button) findViewById(R.id.search_go_btn);
		
		// set search bar events handler
		SearchBarEventsListener searchBarEventsListener = new SearchBarEventsListener();
		this.appIconBtn.setTag(BUTTON_ID_APP);
		this.appIconBtn.setOnClickListener(searchBarEventsListener);
		this.srcTxt.addTextChangedListener(searchBarEventsListener);
		this.srcTxt.setOnKeyListener(searchBarEventsListener);
		this.srcTxt.setOnItemClickListener(searchBarEventsListener);
		this.goBtn.setTag(BUTTON_ID_SEARCH);
		this.goBtn.setOnClickListener(searchBarEventsListener);
		
		// set resutls list events handler
		this.resultsList = (ListView) findViewById(R.id.search_resultsList);
		ResultsListEventsListener resultsListEventsListener = new ResultsListEventsListener();
		this.resultsList.setOnItemClickListener(resultsListEventsListener);
		this.resultsList.setOnScrollListener(resultsListEventsListener);
		
		// setting up ListView adapter
		this.resultsAdapter = new ResultListAdapter(this, R.layout.search_results_list_item);
		this.resultsList.setAdapter(this.resultsAdapter);

		// fill plugins list
		this.pluginsQA = new QuickAction(this.appIconBtn, QuickAction.STYLE_POP_DOWN);
		fillAvailableOpenSearchDescriptions();
		fillAvailableServicesList();
		
		// TODO sort plugins by name
		
		// TODO load the last or the default plugin
		//loadPlugin();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		// TODO update plugins list and reload active plugin 
		//loadPlugin();

		//TODO load prefs
		// SharedPreferences prefs = SearchActivity.this.getSharedPreferences(getString(R.string.PREFS), MODE_PRIVATE);
		
		this.loadMore = true;
		this.suggests = true;
		this.numberOfSuggests = 10;
		this.numberOfResults = 10;
	}

	/**
	 * This methods find all search services installed
	 */
	private void fillAvailableServicesList(){
		// find available services
		PackageManager packageManager = getPackageManager();
        Intent baseIntent = new Intent(getString(R.string.ACTION_PICK_PLUGIN));
		baseIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
        List<ResolveInfo> list = packageManager.queryIntentServices(baseIntent, PackageManager.GET_RESOLVED_FILTER);
        if (list.size() > 0) {
			if (this.pluginsQA == null) {
				this.pluginsQA = new QuickAction(this.appIconBtn, QuickAction.STYLE_POP_DOWN);
				this.pluginsQA.setAnimStyle(QuickAction.ANIM_AUTO);
			}
			
			ResolveInfo info = null;
			ActionItem currentItem;
			for(int i = 0; i < list.size(); ++i) {
				info = list.get(i);
				currentItem = new ActionItem();
				currentItem.setTitle(info.serviceInfo.name.substring(info.serviceInfo.name.lastIndexOf(".")+1));
				currentItem.setIcon(packageManager.getApplicationIcon(info.serviceInfo.applicationInfo));
				currentItem.setOnClickListener(new ActionItemOnClickListener(true, info.filter.getCategory(0))); // listener for services
				this.pluginsQA.addActionItem(currentItem);
			}
		}
        
        Log.i(getPackageName(), "Available services: " + list.size());
	}

	/**
	 * It loads OpenSearch xml files from app data folder
	 */
	private void fillAvailableOpenSearchDescriptions() {
		File pluginsPath = new File(getFilesDir(), getString(R.string.PLUGINS_SUBDIR));
		String[] fileslist = pluginsPath.list();
		
		if (fileslist != null) {
			OnClickListener listener = new ActionItemOnClickListener(false); // listener for opensearch plugins
			
			if (this.pluginsQA == null) {
				this.pluginsQA = new QuickAction(this.appIconBtn,
						QuickAction.STYLE_POP_DOWN);
			}
			
			ActionItem currentItem = null;
			for (int i = 0; i < fileslist.length; i++) {
				if (fileslist[i].endsWith(".xml")) {
					currentItem = new ActionItem();
					currentItem.setOnClickListener(listener);
					currentItem.setTitle(fileslist[i].replace(".xml", ""));
					this.pluginsQA.addActionItem(currentItem);
				}
			}
			
			Log.i(getPackageName(), "Available plugins: " + fileslist.length);
		}
	}
	
	/**
	 * This methods load the previous selected plugin
	 */
//	private void loadPlugin() {
//		SharedPreferences prefs = SearchActivity.this.getSharedPreferences(getString(R.string.PREFS), MODE_PRIVATE);
//		
//		String pluginName = prefs.getString(getString(R.string.PREFS_PLUGIN_CURRENT_NAME), null); // TODO set the default plugin
//		boolean pluginIsService = prefs.getBoolean(getString(R.string.PREFS_PLUGIN_CURRENT_IS_SERVICE), false);
//		
//		if (pluginName != null && pluginName.length() > 0) {
//			loadPlugin(pluginName, pluginIsService);
//		}
//	}
	
	/**
	 * This methods load the specified plugin
	 * 
	 * @param pluginName name of the plugin to load.
	 * @param isService if true indicates that <i>pluginName</i> refers to a service id, if false it refers to an OpenSearch xml file
	 */
	private void loadPlugin(String pluginName, boolean isService) {
		Log.i(getPackageName(), "pluginName=" + pluginName + ", isService=" + isService);
		if (isService) {
			// bind to service
			this.plugin = Plugin.bindService(pluginName, this);
		} else {
			// load opensearch xml description
			this.plugin = Plugin.loadPugin(getString(R.string.PLUGINS_SUBDIR) + File.separator + pluginName + ".xml", this);
		}

		if (this.plugin != null) {
			this.appIconBtn.setImageDrawable(this.plugin.getIcon());
		}
	}

	/**
	 * Adds result items to the list
	 * @param results items to add to the list
	 */
	private void addResultToList(ResultItem[] results) {
		ResultItem currentItem;
		for (int i = 0; i < results.length; i++) {
			currentItem = new ResultItem();
			currentItem.title = results[i].title;
			currentItem.snippet = results[i].snippet;
			currentItem.uri = results[i].uri;
			SearchActivity.this.resultsAdapter.add(currentItem);
		}
	}
	
	/**
	 * It launches the {@link WebActivity} with the specified url
	 * @param pageUrl url to open in the {@link WebActivity}
	 */
	private void launchWebView(String pageUrl) {
		Intent intent = new Intent(this, WebActivity.class);
		intent.putExtra(WebActivity.EXTRA_URL, pageUrl);
		startActivity(intent);
	}
	
//	private void showAdvancedSearch(){
//		// TODO show panel to set prefs (loadMore, suggests, numberOfSuggests, numberOfResults)
//	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.main_manu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
		final int content;
	    switch (item.getItemId()) {
	        case R.id.importOpenSearchDescriptionFromText:
	            content = R.layout.dialog_import_from_text;
	            break;
	        
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	    
	    AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    // Get the layout inflater
	    LayoutInflater inflater = getLayoutInflater();

	    // Inflate and set the layout for the dialog
	    // Pass null as the parent view because its going in the dialog layout
	    final View customDialog = inflater.inflate(content, null);
		builder.setView(customDialog)
				// Add action buttons
				.setPositiveButton(
						R.string.ok,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								// import the OpenSearch xml description
								OpenSearchDescription openSearchDescription;
								if (content == R.layout.dialog_import_from_text) {
									EditText textField = (EditText) customDialog.findViewById(R.id.inputText);
									try {
										openSearchDescription = OpenSearchDescription.parse(textField.getText().toString());
									} catch (Exception e) {
										e.printStackTrace();
										return;
									}
									
								} else {
									return;
								}
								
								Log.i(getPackageName(), "Description: \n" + openSearchDescription.toString());
								
								// TODO check if file already exists and prompt the user with a dialog (overwrite/rename)

								// save description to file in the app data folder
								try {
									File file = new File(getFilesDir(), getString(R.string.PLUGINS_SUBDIR)
													+ File.separator
													+ openSearchDescription.shortName + ".xml");
									file.getParentFile().mkdirs();
									FileWriter fileWriter = new FileWriter(
											file);
									fileWriter.write(openSearchDescription.toString());
									fileWriter.flush();
									fileWriter.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						})
				.setNegativeButton(
						R.string.cancel, 
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								SearchActivity.this.dialog.cancel();
								SearchActivity.this.dialog = null;
							}
				});
		this.dialog = builder.create();
		this.dialog.show();
	    
	    return true;
	}

	/**
	 * {@link OnClickListener} used for QuickAction items
	 * 
	 * @author paco
	 * @version 0.1
	 */
	private class ActionItemOnClickListener implements OnClickListener {
		
		private boolean isService = false;
		private String serviceId = null;
		
		/**
		 * @param isService if true indicates that this {@link OnClickListener} refers to a service, if false it refers to an OpenSearch xml file
		 */
		public ActionItemOnClickListener(boolean isService) {
			this(isService, null);
		}
		
		/**
		 * @param isService if true indicates that this {@link OnClickListener} refers to a service, if false it refers to an OpenSearch xml file
		 * @param serviceId id of the service to bind
		 */
		public ActionItemOnClickListener(boolean isService, String serviceId) {
			this.isService = isService;
			this.serviceId = serviceId;
		}

		@Override
		public void onClick(View v) {
			TextView text = (TextView) v.findViewById(R.id.title);
			if (text != null) {
				SharedPreferences.Editor editor = SearchActivity.this.getSharedPreferences(getString(R.string.PREFS), MODE_PRIVATE).edit();
				
				String pluginName = text.getText().toString();
				
				editor.putString(getString(R.string.PREFS_PLUGIN_CURRENT_NAME), pluginName);
    	    	editor.putBoolean(getString(R.string.PREFS_PLUGIN_CURRENT_IS_SERVICE), isService);
    	    	editor.commit();
    	    	
    	    	SearchActivity.this.loadPlugin(isService ? serviceId : pluginName, isService);
    	    	
    	    	SearchActivity.this.pluginsQA.dismiss();
			}
		}
	}

	/**
	 * Events listener for search bar components
	 * 
	 * @author paco
	 * @version 0.1
	 */
	private class SearchBarEventsListener implements View.OnClickListener, TextWatcher, OnKeyListener, OnItemClickListener {
		@Override
		public void onClick(View view) { // app btn
			Integer buttonId = (Integer) view.getTag();
			
			switch (buttonId) {
			
				case BUTTON_ID_APP:
					// manage click on app button
					if (SearchActivity.this.pluginsQA != null) {
						SearchActivity.this.pluginsQA.show();
					} else {
						// TODO replace the toast message with an alert dialog
						Toast.makeText(SearchActivity.this, R.string.noPluginsAvailable, Toast.LENGTH_LONG).show();
					}
					return;
					
				case BUTTON_ID_SEARCH:
					// manage click on search button
					String textToSearch = SearchActivity.this.srcTxt.getText().toString();
					if (!textToSearch.equals("")) {
						SearchActivity.this.resultsAdapter.clear();
						ResultItem[] searchResults = SearchActivity.this.plugin.search(textToSearch, SearchActivity.this.numberOfResults, SearchActivity.this.resultsList.getCount());
						if (searchResults != null) {
							addResultToList(searchResults);
						}
						SearchActivity.this.lastSearch = textToSearch;
					}
					return;
			}
		}
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (SearchActivity.this.suggests) {
				if (count != 0) {
					try {
						// set suggestions with a simple ArrayAdapter
						ArrayAdapter<String> adapter = new ArrayAdapter<String>(
								SearchActivity.this,
								R.layout.search_suggest_item,
								SearchActivity.this.plugin.getSuggestions(s.toString(), SearchActivity.this.numberOfSuggests));
						SearchActivity.this.srcTxt.setAdapter(adapter);
					} catch (Exception e) {e.printStackTrace();}
				}
			}
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {}
		
		@Override
		public void afterTextChanged(Editable s) {}
		
		@Override
		public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (keyCode == KeyEvent.KEYCODE_ENTER) {
				return SearchActivity.this.goBtn.performClick();
			}
			return false;
		}

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1,
				int arg2, long arg3) {
			ResultItem[] searchResults = SearchActivity.this.plugin.search(
							((TextView) arg1).getText().toString().replaceAll(" ", "+"), 
							SearchActivity.this.numberOfResults, 
							SearchActivity.this.resultsList.getCount()
					);
			if (searchResults != null) {
				addResultToList(searchResults);
			}
		}
	}
	
	/**
	 * Events listener for resutls list
	 * 
	 * @author paco
	 * @version 0.1
	 */
	private class ResultsListEventsListener implements OnScrollListener, OnItemClickListener{

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			SearchActivity.this.launchWebView(SearchActivity.this.resultsAdapter.getItem(arg2).uri.toString());
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {}
		
		@Override
		public void onScroll(AbsListView view, int firstVisibleItem,
				int visibleItemCount, int totalItemCount) {
			if(SearchActivity.this.loadMore && ((firstVisibleItem + visibleItemCount) == totalItemCount) && !(SearchActivity.this.srcTxt.getText().toString().equals(""))){
				try {
					SearchActivity.this.addResultToList(
						SearchActivity.this.plugin.search(SearchActivity.this.lastSearch, SearchActivity.this.numberOfResults, totalItemCount));
				} catch (Exception e) {
					Toast.makeText(SearchActivity.this, SearchActivity.this.getString(R.string.unableToLoadMoreResults), Toast.LENGTH_LONG).show();
				}
			}
		}
	}
}
