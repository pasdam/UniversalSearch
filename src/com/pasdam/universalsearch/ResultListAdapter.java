package com.pasdam.universalsearch;

import java.util.ArrayList;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Adapter for the results list
 * 
 * @author paco
 * @version 0.1
 */
public class ResultListAdapter extends ArrayAdapter<ResultItem> {

	private ArrayList<ResultItem> items;
	private Context context;

	/**
	 * Initialize the adapter with the specified items
	 * 
	 * @param context
	 *            context of the application
	 * @param textViewResourceId
	 *            the resource ID for a layout file containing a TextView to use
	 *            when instantiating views.
	 * @param items
	 *            the objects to represent in the ListView.
	 */
	public ResultListAdapter(Context context, int textViewResourceId,
			ArrayList<ResultItem> items) {
		super(context, textViewResourceId, items);
		this.items = items;
		this.context = context;
	}

	/**
	 * Initialize an empty adapter
	 * 
	 * @param context
	 *            context of the application
	 * @param textViewResourceId
	 *            the resource ID for a layout file containing a TextView to use
	 *            when instantiating views.
	 */
	public ResultListAdapter(Context context, int textViewResourceId) {
		super(context, textViewResourceId);
		this.items = new ArrayList<ResultItem>();
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) this.context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.search_results_list_item, null);
		}
		ResultItem item = items.get(position);
		if (item != null) {
			TextView titleTxt = (TextView) v
					.findViewById(R.id.search_results_item_titleTxt);
			TextView descTxt = (TextView) v
					.findViewById(R.id.search_results_item_snippetTxt);
			if (titleTxt != null) {
				titleTxt.setText(item.title);
			}
			if (descTxt != null) {
				descTxt.setText(Html.fromHtml(item.snippet));
			}
		}
		return v;
	}

	@Override
	public void add(ResultItem object) {
		super.add(object);
		this.items.add(object);
	}

	@Override
	public void clear() {
		super.clear();
		this.items.clear();
	}
}