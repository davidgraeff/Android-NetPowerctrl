package oly.netpowerctrl.outletconfig;

import oly.netpowerctrl.R;
import oly.netpowerctrl.utils.DeviceInfo;
import oly.netpowerctrl.utils.SharedPrefs;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

public class OutletConfig extends Activity {

	ListView lvOutlets;
	OutletConfigAdapter adapter;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        String prefname = null;
		Intent it = getIntent();
		if (it != null) {
			Bundle extra = it.getExtras();
			if (extra != null) {
				Object o = extra.get("prefname");
				if (o != null) {
					prefname = (String) o; 
				}
			}
		}
		if (prefname == null) {
			Toast.makeText(this,
					getResources().getString(R.string.error_unknown_device),
					Toast.LENGTH_LONG).show();
			finish();
			return;
		}
        
		setContentView(R.layout.outlet_config);
		
        DeviceInfo di = SharedPrefs.ReadDevice(this, SharedPrefs.getFullPrefname(prefname));
        setTitle(di.DeviceName);
		
		lvOutlets = (ListView)findViewById(R.id.lvOutlets);
        adapter = new OutletConfigAdapter(this, prefname);
        lvOutlets.setAdapter(adapter);

        ImageButton btnAdd = (ImageButton)findViewById(R.id.btnAddOutlet);
        btnAdd.setOnClickListener(onBtnAddClicked);
	}

	private OnClickListener onBtnAddClicked = new OnClickListener() {
		public void onClick(View v) {
			lvOutlets.setSelection(adapter.addItem());
		}
	};

}
