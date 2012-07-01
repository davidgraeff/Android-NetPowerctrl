package oly.netpowerctrl;

import java.util.List;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
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

    public View getView(int position, View convertView, ViewGroup parent) {

    	// deliberately a new view because of the textwatchers below
    	convertView = inflater.inflate(R.layout.outlet_config_item, null);

    	final OutletInfo outlet = items.get(position);
        EditText tvName = (EditText) convertView.findViewById(R.id.outlet_name);
        tvName.setText(outlet.Description);
        tvName.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count)  {/*nop*/}
			public void beforeTextChanged(CharSequence s, int start, int count, int after)  {/*nop*/}
			
			public void afterTextChanged(Editable s) {
				outlet.Description = s.toString();
			}
		});
      
        EditText tvNumber = (EditText) convertView.findViewById(R.id.outlet_number);
        if (outlet.OutletNumber >= 1) 
        	tvNumber.setText(((Integer)outlet.OutletNumber).toString());
        else tvNumber.setText("");
        tvNumber.addTextChangedListener(new TextWatcher() {
			public void onTextChanged(CharSequence s, int start, int before, int count)  {/*nop*/}
			public void beforeTextChanged(CharSequence s, int start, int count, int after)  {/*nop*/}
			
			public void afterTextChanged(Editable s) {
				try {
					outlet.OutletNumber = Integer.parseInt(s.toString());
				}
				catch (Exception e) {
					outlet.OutletNumber = -1;
				}
			}
		});

        ImageButton btnDelete = (ImageButton)convertView.findViewById(R.id.delete_outlet);
        btnDelete.setTag(position);
        btnDelete.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				items.remove(outlet);
				notifyDataSetChanged();
			}
		});

        return convertView;
    }

}
