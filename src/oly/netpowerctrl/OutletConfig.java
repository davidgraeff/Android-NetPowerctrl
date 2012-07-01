package oly.netpowerctrl;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ListView;

public class OutletConfig extends Activity {

	ListView lvOutlets;
	OutletConfigAdapter adapter;
	DeviceInfo device_info;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.outlet_config);
		lvOutlets = (ListView)findViewById(R.id.lvOutlets);
        device_info = (DeviceInfo) getIntent().getExtras().get("device_info");

        adapter = new OutletConfigAdapter(this, device_info.Outlets);
        lvOutlets.setAdapter(adapter);

        ImageButton btnAdd = (ImageButton)findViewById(R.id.btnAddOutlet);
        btnAdd.setOnClickListener(onBtnAddClicked);
	}

	@Override
	public void onBackPressed() {
		boolean allok = true;
		for (OutletInfo oi: device_info.Outlets) {
			if (oi.Description == "") {
				allok = false;
				break;
			}
		}
		if (! allok) {
			show_error(getResources().getString(R.string.error_outlet_config_name));
			return;
		}

		allok = true;
		for (OutletInfo oi: device_info.Outlets) {
			if (oi.OutletNumber < 1) {
				allok = false;
				break;
			}
		}
		if (! allok) {
			show_error(getResources().getString(R.string.error_outlet_config_number));
			return;
		}
		
        Intent ret_intent = new Intent(); // for returning data to onActivityResult()
        ret_intent.putExtra("device_info", device_info);
        setResult(RESULT_OK, ret_intent);

		super.onBackPressed();
	}
	
	private void show_error(String message) {
		AlertDialog aDialog = new AlertDialog.Builder(this)
			.setMessage(message)
			.setTitle(getResources().getString(R.string.error_outlet_title))
			.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					return;
				}
			})
			.create();
		aDialog.show();
	}
	
	private OnClickListener onBtnAddClicked = new OnClickListener() {
		public void onClick(View v) {
			device_info.Outlets.add(new OutletInfo());
			adapter.notifyDataSetChanged();
		}
	};

}
