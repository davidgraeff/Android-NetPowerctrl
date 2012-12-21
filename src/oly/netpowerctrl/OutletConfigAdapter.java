package oly.netpowerctrl;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;

public class OutletConfigAdapter extends BaseAdapter {

	private List<OutletInfo> items;
    private LayoutInflater inflater;
    private String prefname;
    Context ctx;
    
    public OutletConfigAdapter(Context context, String prefname) {
    	this.prefname = prefname;
    	ctx = context;
        inflater = LayoutInflater.from(context);
        items = new ArrayList<OutletInfo>();
        DeviceInfo di = SharedPrefs.ReadDevice(context, SharedPrefs.getFullPrefname(prefname));
        items = new ArrayList<OutletInfo>(di.Outlets);
    }    

    public int getCount() {
        return items.size();
    }

    public Object getItem(int position) {
        return items.get(position);
    }

    public long getItemId(int position) {
        return position;
    }
    
    public int addItem() {
		items.add(new OutletInfo());
		notifyDataSetChanged();
		return items.size()-1;
	}

    class ViewHolder {
    	EditText etName;
    	EditText etNumber;
    	ImageButton btnDelete;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {

    	ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.outlet_config_item, null);
            holder.etName = (EditText)convertView.findViewById(R.id.outlet_name);
            holder.etNumber = (EditText)convertView.findViewById(R.id.outlet_number);
            holder.btnDelete = (ImageButton)convertView.findViewById(R.id.delete_outlet);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
    	 	               
    	OutletInfo outlet = items.get(position);
    	holder.etName.setText(outlet.Description);
        if (outlet.OutletNumber >= 1) 
        	holder.etNumber.setText(((Integer)outlet.OutletNumber).toString());
        else holder.etNumber.setText("");
    	holder.etName.setTag(position);
    	holder.etNumber.setTag(position);
        holder.btnDelete.setTag(position);

        if (holder.etName.getText().toString().length() == 0)
        	holder.etName.setError(convertView.getContext().getResources().getText(R.string.error_outlet_config_name));
        else
        	holder.etName.setError(null);

        if (holder.etNumber.getText().toString().length() == 0)
        	holder.etNumber.setError(convertView.getContext().getResources().getText(R.string.error_outlet_config_number));
        else
        	holder.etNumber.setError(null);

        //we need to update adapter once we finish with editing
        holder.etName.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus){
                	OutletInfo outlet = items.get((Integer)v.getTag());
                    outlet.Description = ((EditText)v).getText().toString();
                    if (outlet.Description.length() == 0)
                    	((EditText)v).setError(v.getResources().getText(R.string.error_outlet_config_name));
                    else
                    	((EditText)v).setError(null);
					SaveOutlets();
                }
            }
	    });
        holder.etNumber.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus){
                	OutletInfo outlet = items.get((Integer)v.getTag());
                	try {
    					outlet.OutletNumber = Integer.parseInt(((EditText)v).getText().toString());
    					SaveOutlets();
    					((EditText)v).setError(null);
    				}
    				catch (Exception e) {
    					outlet.OutletNumber = -1;
                    	((EditText)v).setError(v.getResources().getText(R.string.error_outlet_config_number));
    				}
                }
            }
	    });
        
        holder.btnDelete.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				items.remove(items.get((Integer)v.getTag()));
				SaveOutlets();
				notifyDataSetChanged();
			}
		});

        return convertView;
    }
    
    private void SaveOutlets() {
		SharedPreferences device_prefs = ctx.getSharedPreferences(SharedPrefs.getFullPrefname(prefname), Context.MODE_PRIVATE);
		SharedPreferences.Editor device_editor = device_prefs.edit();
		device_editor.putInt(SharedPrefs.PREF_NUM_OUTLETS, items.size());
		for (int i=0; i<items.size(); i++) {
			device_editor.putString(SharedPrefs.PREF_OUTLET_NAME+String.valueOf(i), items.get(i).Description);
			device_editor.putInt(SharedPrefs.PREF_OUTLET_NUMBER+String.valueOf(i), items.get(i).OutletNumber);
		}
		device_editor.commit();
	}

}
