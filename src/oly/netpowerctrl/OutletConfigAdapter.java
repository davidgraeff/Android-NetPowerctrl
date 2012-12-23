package oly.netpowerctrl;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
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
    	TextWatcher etNameTextWatcher;
    	TextWatcher etNumberTextWatcher;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {

    	final ViewHolder holder;
        if (convertView == null) {
            holder = new ViewHolder();
            convertView = inflater.inflate(R.layout.outlet_config_item, null);
            holder.etName = (EditText)convertView.findViewById(R.id.outlet_name);
            holder.etNumber = (EditText)convertView.findViewById(R.id.outlet_number);
            holder.btnDelete = (ImageButton)convertView.findViewById(R.id.delete_outlet);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            holder.etName.removeTextChangedListener(holder.etNameTextWatcher);
            holder.etNumber.removeTextChangedListener(holder.etNumberTextWatcher);
        }
    	 	               
    	OutletInfo outlet = items.get(position);
    	holder.etName.setText(outlet.Description);
        if (outlet.OutletNumber >= 1) 
        	holder.etNumber.setText(((Integer)outlet.OutletNumber).toString());
        else holder.etNumber.setText("");
    	holder.etName.setTag(outlet);
    	holder.etNumber.setTag(outlet);
        holder.btnDelete.setTag(outlet);

        if (holder.etName.getText().toString().length() == 0)
        	holder.etName.setError(convertView.getContext().getResources().getText(R.string.error_outlet_config_name));
        else
        	holder.etName.setError(null);

        if (holder.etNumber.getText().toString().length() == 0)
        	holder.etNumber.setError(convertView.getContext().getResources().getText(R.string.error_outlet_config_number));
        else
        	holder.etNumber.setError(null);

        //we need to update adapter once we finish with editing
        holder.etNameTextWatcher = new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count)  {/*nop*/}
			public void beforeTextChanged(CharSequence s, int start, int count, int after)  {/*nop*/}
			public void afterTextChanged(Editable s) {
            	OutletInfo outlet = (OutletInfo)holder.etName.getTag();
                outlet.Description = s.toString();
                if (outlet.Description.length() == 0)
                	holder.etName.setError(holder.etName.getResources().getText(R.string.error_outlet_config_name));
                else
                	holder.etName.setError(null);
				SaveOutlets();
            }
	    };
        holder.etName.addTextChangedListener(holder.etNameTextWatcher);
        
        holder.etNumberTextWatcher = new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count)  {/*nop*/}
			public void beforeTextChanged(CharSequence s, int start, int count, int after)  {/*nop*/}
			public void afterTextChanged(Editable s) {
				OutletInfo outlet = (OutletInfo)holder.etNumber.getTag();
            	try {
					outlet.OutletNumber = Integer.parseInt(s.toString());
					SaveOutlets();
					holder.etNumber.setError(null);
				}
				catch (Exception e) {
					outlet.OutletNumber = -1;
					holder.etNumber.setError(holder.etNumber.getResources().getText(R.string.error_outlet_config_number));
				}
            }
	    };
        holder.etNumber.addTextChangedListener(holder.etNumberTextWatcher);

        holder.btnDelete.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				items.remove((OutletInfo)v.getTag());
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
