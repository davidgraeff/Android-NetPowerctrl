package oly.netpowerctrl.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import oly.netpowerctrl.R;
import oly.netpowerctrl.listadapter.OutledListAdapter;
import oly.netpowerctrl.utils.DeviceInfo;
import oly.netpowerctrl.utils.OutletCommand;
import oly.netpowerctrl.utils.OutletCommandGroup;
import oly.netpowerctrl.utils.SharedPrefs;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

public class ShortcutCreatorActivity extends Activity
{
	ArrayList<DeviceInfo> alDevices;
	OutledListAdapter adpOutlets;
	ListView lvOutletSelect;
	final Activity that = this;
	
	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED,null);
		setContentView(R.layout.shortcut_activity);
		setTitle(R.string.choose_shortcut_outlets);

    	alDevices = SharedPrefs.ReadConfiguredDevices(this);
  		adpOutlets = new OutledListAdapter(this, alDevices);

		lvOutletSelect = (ListView)findViewById(R.id.lvOutletSelect);
  		lvOutletSelect.setAdapter(adpOutlets);
	
  		((Button)findViewById(R.id.btnAcceptShortcut)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Create shortcut intent
				Intent shortcutIntent=new Intent(that, oly.netpowerctrl.service.ShortcutExecutionActivity.class);
				
				// Generate list of checked items
				List<OutletCommand> commands = adpOutlets.getCheckedItems();
				if (commands.isEmpty()) {
					return;
				}
				
				OutletCommandGroup og = new OutletCommandGroup();
				for (OutletCommand c: commands) {
					og.add(c);
				}
				Calendar t = Calendar.getInstance();
				og.groupname = DateFormat.getMediumDateFormat(that).format(t.getTime()) + " - " + DateFormat.getTimeFormat(that).format(t.getTime());
				shortcutIntent.putExtra("commands", og.toString());
				
				// Return result
				// Shortcut name is "app_name (9)" where 9 is the amount of commands
				Intent intent=new Intent();
				intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT,shortcutIntent);
				intent.putExtra(Intent.EXTRA_SHORTCUT_NAME,
						getResources().getString(R.string.app_name) + " ("+ Integer.valueOf(commands.size()).toString()+")");
				intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
						Intent.ShortcutIconResource.fromContext(that,R.drawable.netpowerctrl));
				setResult(RESULT_OK,intent);
				
				// Show info message
				finish();
			}
		});
  		
  		((Button)findViewById(R.id.btnCancelShortcut)).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
  		});
	}
}