package com.pasdam.universalsearch;

import com.pasdam.universalsearch.ResultItem;

interface Service {
	
	/**
	 * @param text Text to get suggests
	 * @param maxNumberOfSuggests max number of returned suggests
	 * @return a string array (of lenght <b>maxNumberOfSuggests</b>), containing suggests
	 */
	String[] getSuggest(String text, int maxNumberOfSuggests);

	/**
	 * @param textToSearch search term
	 * @param maxNumberOfResults max number of retuned results
	 * @param offset used to return results starting from <b>offset</b>
	 * @return
	 */
	ResultItem[] search(String textToSearch, int maxNumberOfResults, int offset);
	
	/**
	 * @return true if the service has preferences
	 */
	boolean hasPreferences();
	
	/**
	 * Launches preferences activity
	 */
	void launchPreferences();
	
}
