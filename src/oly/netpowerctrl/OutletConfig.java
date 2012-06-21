package oly.netpowerctrl;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class OutletConfig extends Activity {

	TableLayout tlyoOutlets;
	int RowCounter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.outlet_config);
		tlyoOutlets = (TableLayout)findViewById(R.id.tlyoOutlets);
        DeviceInfo device_info = (DeviceInfo) getIntent().getExtras().get("device_info");

        ((TextView)findViewById(R.id.tvTitle)).setText(device_info.DeviceName);
        
        RowCounter = 0;
        for (OutletInfo oi: device_info.Outlets)
        	AddRow(oi.Description, String.format("%d",oi.OutletNumber));
                
        findViewById(R.id.btnAddOutlet).setOnClickListener(onBtnAddClicked);
	}

	public void AddRow(String name, String outletno) {
    	RowCounter++;
    	
    	EditText edtName = new EditText(this);
    	edtName.setEms(0);
    	edtName.setTextAppearance(this, android.R.attr.textAppearanceSmall);
    	edtName.setText(name);
    	edtName.setTag(new Integer(RowCounter));
    	
    	EditText edtOutlets = new EditText(this);
    	edtOutlets.setEms(0);
    	edtOutlets.setTextAppearance(this, android.R.attr.textAppearanceSmall);
    	edtOutlets.setText(outletno);
    	edtOutlets.setTag(new Integer(RowCounter));
    	
    	ImageButton ibDelete = new ImageButton(this);
    	ibDelete.setBackgroundResource(android.R.drawable.list_selector_background);
    	ibDelete.setImageResource(android.R.drawable.ic_delete);
    	ibDelete.setTag(new Integer(RowCounter));
    	ibDelete.setOnClickListener(onDeleteClicked);
    	
    	TableRow tr = new TableRow(this);
    	tr.addView(edtName);
    	tr.addView(edtOutlets);
    	tr.addView(ibDelete);
    	tr.setTag(new Integer(RowCounter));

    	tlyoOutlets.addView(tr);
	}
	
	@Override
	public void onBackPressed() {
	    
	}
	
	private OnClickListener onBtnAddClicked = new OnClickListener() {
		public void onClick(View v) {
			AddRow("", "");
		}
	};

	private OnClickListener onDeleteClicked = new OnClickListener() {
		public void onClick(View v) {
			int row_no = (Integer)v.getTag();
			for (int i=0; i<tlyoOutlets.getChildCount(); i++) {
				View tr = tlyoOutlets.getChildAt(i);
				Object tag_o = tr.getTag();
				if (tag_o != null) {
					int tr_tag = (Integer)tag_o; 
					if (tr_tag == row_no) {
						tlyoOutlets.removeView(tr);
						break;
					}
				}
			}
		}
	};
}
