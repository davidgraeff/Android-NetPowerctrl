package oly.netpowerctrl.listadapter;

import java.util.ArrayList;

import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.GreenFlasher;
import oly.netpowerctrl.utils.OutletCommandGroup;
import oly.netpowerctrl.utils.SharedPrefs;
import oly.netpowerctrl.utils.UDPSendToDevice;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

public class GroupListAdapter extends BaseAdapter implements OnClickListener, OnLongClickListener {
	boolean remove_group_state = false;
	private Activity context;
	private ArrayList<OutletCommandGroup> groups;
	private LayoutInflater inflater;
	final GroupListAdapter that = this;

	class HandleRemoveClass implements OnClickListener {
		@Override
		public void onClick(View v) {
			int position = (Integer) v.getTag();
			that.groups.remove(position);
			SharedPrefs.SaveGroups(that.groups, that.context);
			that.notifyDataSetChanged();
		}
	}

	private HandleRemoveClass handleRemoveClickListener = new HandleRemoveClass();

	public GroupListAdapter(Activity context) {
		this.context = context;
		inflater = LayoutInflater.from(context);
		groups = SharedPrefs.ReadGroups(context);
	}

	@Override
	public int getCount() {
		return groups.size();
	}

	@Override
	public Object getItem(int position) {
		return groups.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public void onClick(View v) {
		int position = (Integer) v.getTag();
		GreenFlasher.flashBgColor(v);
		OutletCommandGroup og = (OutletCommandGroup) getItem(position);
		UDPSendToDevice.sendOutlet(context, og);
	}

	public View getView(int position, View convertView, ViewGroup parent) {

		if (convertView == null) {
			convertView = inflater.inflate(R.layout.group_list_item, null);
		}

		OutletCommandGroup data = groups.get(position);

		TextView tvName = (TextView) convertView.findViewById(R.id.group_list_name);
		tvName.setText(data.groupname);
		tvName.setTag(position);
		tvName.setOnClickListener(this);
		tvName.setOnLongClickListener(this);
		tvName = (TextView) convertView.findViewById(R.id.group_list_details);
		tvName.setText(data.groupdetails);
		tvName.setTag(position);
		tvName.setOnClickListener(this);
		ImageButton btn = (ImageButton) convertView.findViewById(R.id.btnRemoveGroup);
		btn.setOnClickListener(handleRemoveClickListener);
		btn.setTag(position);

		return convertView;
	}

	public void addGroup(OutletCommandGroup data) {
		if (data == null)
			return;

		groups.add(data);
		SharedPrefs.SaveGroups(groups, context);
		notifyDataSetChanged();
	}

	@Override
	public boolean onLongClick(View v) {
		int position = (Integer) v.getTag();
		final OutletCommandGroup og = (OutletCommandGroup) getItem(position);

		AlertDialog.Builder alert = new AlertDialog.Builder(context);
		alert.setTitle(R.string.groupname);
		alert.setMessage(R.string.groupname_long);

		// Set an EditText view to get user input
		final EditText input = new EditText(context);
		input.setText(og.groupname);
		alert.setView(input);

		alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				String value = input.getText().toString().trim();
				if (value.isEmpty())
					return;
				og.groupname = value;
				SharedPrefs.SaveGroups(groups, context);
				that.notifyDataSetChanged();
			}
		});

		alert.show();
		return true;
	}

}
