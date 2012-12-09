package oly.netpowerctrl;

import java.util.List;

import android.content.Context;
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
    
    public OutletConfigAdapter(Context context, List<OutletInfo> outlets) {
        inflater = LayoutInflater.from(context);
        items = outlets;
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
    	 	               
    	final OutletInfo outlet = items.get(position);
    	holder.etName.setText(outlet.Description);
        if (outlet.OutletNumber >= 1) 
        	holder.etNumber.setText(((Integer)outlet.OutletNumber).toString());
        else holder.etNumber.setText("");
    	holder.etName.setTag(position);
    	holder.etNumber.setTag(position);
        holder.btnDelete.setTag(position);
    	
        //we need to update adapter once we finish with editing
        holder.etName.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus){
                    outlet.Description = ((EditText)v).getText().toString();
                }
            }
	    });
        holder.etNumber.setOnFocusChangeListener(new OnFocusChangeListener() {
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus){
                	try {
    					outlet.OutletNumber = Integer.parseInt(((EditText)v).getText().toString());
    				}
    				catch (Exception e) {
    					outlet.OutletNumber = -1;
    				}
                }
            }
	    });
        
        holder.btnDelete.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				items.remove(outlet);
				notifyDataSetChanged();
			}
		});

        return convertView;
    }

}
