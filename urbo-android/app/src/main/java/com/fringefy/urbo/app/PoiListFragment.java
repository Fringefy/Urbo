package com.fringefy.urbo.app;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fringefy.urbo.Poi;


public class PoiListFragment extends Fragment implements AdapterView.OnItemClickListener {

	//List adapter inner class
	private class PoiAdapter extends BaseAdapter implements ListAdapter {

		@Override
		public int getCount() {
			return poiMatchList.length;
		}

		@Override
		public Object getItem(int position) {
			return poiMatchList[position];
		}

		@Override
		public long getItemId(int position) {
			return getItem(position).hashCode();
		}

		@Override
		public boolean hasStableIds() {
			return true;
		}

		@Override public View getView(int position, View convertView, ViewGroup parent) {

			if (convertView == null) {
				convertView = layoutInflater.inflate(R.layout.listview_item, parent, false);
			}

			TextView name = (TextView) convertView.findViewById(R.id.text_name);
			TextView value = (TextView) convertView.findViewById(R.id.text_value);
			Poi poi = poiMatchList[position];
			name.setText(poi.getName());
			value.setText(poi.getAddress());

			return convertView;
		}

	}

    // Members
	private Poi[] poiMatchList;
	private LayoutInflater layoutInflater;
	private PoiAdapter poiAdapter = new PoiAdapter();


    // Construction

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.listview_fragment, container, false);

		layoutInflater = LayoutInflater.from(getActivity());
		ListView poiList = (ListView) rootView.findViewById(R.id.poi_list);
		poiList.setAdapter(poiAdapter);
		poiList.setOnItemClickListener(this);

		return rootView;
	}
    //events

    //Public methods

    /**
     * Use setList to give PoiListFragment a list of POI's to show.
     * <BR> for example the PoiCache.
     * @param matchList List of POI's.
     */
	public void setList(Poi[] matchList) {
		if (matchList != null) {
			poiMatchList = matchList;
		}
		poiAdapter.notifyDataSetChanged();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Toast.makeText(getActivity(),
			"clicked Id=" + position + poiMatchList[position].getId(),
			Toast.LENGTH_LONG).show();
	}
}
