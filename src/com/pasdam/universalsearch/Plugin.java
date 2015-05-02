package com.pasdam.universalsearch;

import java.io.File;
import java.net.URL;
import java.util.List;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Picture;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
import android.os.IBinder;
import android.os.RemoteException;

import com.pasdam.opensearch.description.OpenSearchDescription;
import com.pasdam.opensearch.description.TemplateParameter;
import com.pasdam.opensearch.description.Url;
import com.pasdam.opensearch.description.UrlRole;
import com.pasdam.opensearch.response.SuggestionsResponse;

/**
 * Factory class used to create plugins, from a service or an OpenSearch description file
 * @author paco
 * @version 0.1
 */
public abstract class Plugin {
	
	/**
	 * Creates a plugin from an OpenSearch xml description file
	 * @param filePath path of the OpenSearch xml description file
	 * @param context context of the application
	 * @return the plugin represented by the description file, or null if errors occurs while parsing the file
	 */
	public static Plugin loadPugin(String filePath, Context context) {
		try {
			return new OpenSearchPlugin(filePath, context);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Creates a plugins binding the related service
	 * @param service service fully-qualified name
	 * @param context context of the application
	 * @return the plugin related to the specified service
	 */
	public static Plugin bindService(String service, Context context) {
		return new ServicePlugin(service, context);
	}

	/**
	 * It returns the icon of the plugin as a {@link Drawable}
	 * @return a {@link Drawable} representing the icon of the plugin
	 */
	public abstract Drawable getIcon();
	
	/**
	 * It indicates whether the plugin allows user to set some settings. <br>
	 * For OpenSearch plugins it returns always false.
	 * @return true if it has preferences, false otherwise
	 */
	public abstract boolean hasPreferences();
	
	/**
	 * It launches the activity with the preferences. <br>
	 * For OpenSearch plugins it does nothing.
	 */
	public abstract void launchPreferences();
	
	/**
	 * It returns the suggestions list
	 * @param query search query
	 * @param maxNumberOfSuggests max number of returned results
	 * @return a string array (of lenght <i>maxNumberOfResults</i>), containing suggests
	 */
	public abstract String[] getSuggestions(String query, int maxNumberOfResults);
	
	/**
	 * It performs the search of the specified query
	 * @param query search query
	 * @param maxNumberOfResults max number of retuned results
	 * @param offset used to return results starting from <b>offset</b>
	 * @return
	 */
	public abstract ResultItem[] search(String query, int maxNumberOfResults, int offset);
	
	/**
	 * This class is a wrapper for OpenSearch plugins
	 * 
	 * @author paco
	 * @version 0.1
	 */
	private static class OpenSearchPlugin extends Plugin {
		
		private Drawable icon;
		private SuggestionsResponse suggestiosResponse;
		private Url searchUrl;
		private Context context;
		
		public OpenSearchPlugin(String filePath, Context context) {
			this.context = context;
			
			File pluginFile = new File(context.getFilesDir(), filePath);
			if (pluginFile.exists()) {
				OpenSearchDescription openSearchDescription = OpenSearchDescription
						.parse(pluginFile);
				
				// get icon
				try {
					URL url = new URL(openSearchDescription.images.get(0).value);
					this.icon = new PictureDrawable(
							Picture.createFromStream(url.openStream()));
				} catch (Exception e) {
					e.printStackTrace();
				}
				// get urls
				for (Url url : openSearchDescription.urls) {
					if (url.rel.contains(UrlRole.RESULTS)) {
						this.searchUrl = url;
					}
					if (url.rel.contains(UrlRole.SUGGESTIONS)) {
						this.suggestiosResponse = new SuggestionsResponse(url);
					}
				}
			} else {
				throw new IllegalArgumentException("Plugin file does not exist: " + pluginFile.getAbsolutePath());
			}
		}
		
		@Override
		public Drawable getIcon() {
			return this.icon;
		}

		@Override
		public String[] getSuggestions(String query, int maxNumberOfResults) {
			return this.suggestiosResponse.getSuggestions(query, maxNumberOfResults)[1];
		}

		@Override
		public ResultItem[] search(String query, int maxNumberOfResults,
				int offset) {
			this.searchUrl.setParameterValue(TemplateParameter.SEARCH_TERM, query);
			this.searchUrl.setParameterValue(TemplateParameter.COUNT, "" + maxNumberOfResults);
			this.searchUrl.setParameterValue(TemplateParameter.START_INDEX, "" + offset);
			String url = this.searchUrl.getUrl();
			this.searchUrl.clearParametersValues();
			
			// TODO perform search and fill results list, instead of open the search in the WebView
			
			Intent intent = new Intent(this.context, WebActivity.class);
			intent.putExtra(WebActivity.EXTRA_URL, url);
			this.context.startActivity(intent);
			return null;
		}

		@Override
		public boolean hasPreferences() {
			return false;
		}

		@Override
		public void launchPreferences() {}
	}
	
	/**
	 * This class is a wrapper for service plugins
	 * 
	 * @author paco
	 * @version 0.1
	 */
	private static class ServicePlugin extends Plugin implements ServiceConnection {
		
		private Context context;
		private Service service;
		
		public ServicePlugin(String service, Context context) {
			this.context = context;
			
			try {
				context.unbindService(this);
			} catch (Exception e) {}
			
			Intent i = new Intent(this.context.getString(R.string.ACTION_PICK_PLUGIN));
			i.addCategory(service);
			context.bindService(i, this, Context.BIND_AUTO_CREATE);
		}
		
		@Override
		public Drawable getIcon() {
			PackageManager packageManager = this.context.getPackageManager();
	        Intent baseIntent = new Intent(this.context.getString(R.string.ACTION_PICK_PLUGIN));
			baseIntent.setFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
	        List<ResolveInfo> list = packageManager.queryIntentServices(baseIntent, PackageManager.GET_RESOLVED_FILTER);
	        if (list.size() > 0) {
	        	return packageManager.getApplicationIcon(list.get(0).serviceInfo.applicationInfo);
			}
			
			return null;
		}

		@Override
		public String[] getSuggestions(String query, int maxNumberOfResults) {
			try {
				return this.service.getSuggest(query, maxNumberOfResults);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			return new String[] {}; // empty array
		}

		@Override
		public ResultItem[] search(String query, int maxNumberOfResults,
				int offset) {
			try {
				return this.service.search(query, maxNumberOfResults, offset);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			return new ResultItem[] {}; // empty array
		}
		
		@Override
		protected void finalize() throws Throwable {
			super.finalize();
			this.context.unbindService(this);
		}
		
		@Override
        public void onServiceConnected(ComponentName className, IBinder boundService) {
        	this.service = Service.Stub.asInterface((IBinder) boundService);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
        	this.service = null;
        }

		@Override
		public boolean hasPreferences() {
			try {
				return this.service.hasPreferences();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
			return false;
		}

		@Override
		public void launchPreferences() {
			try {
				this.service.launchPreferences();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
}
