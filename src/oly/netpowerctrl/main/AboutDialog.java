package oly.netpowerctrl.main;

import java.io.InputStream;
import oly.netpowerctrl.R;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class AboutDialog extends Dialog {

	Context context;
	
	public AboutDialog(Context context) {
		super(context);
		this.context = context;
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        TextView tvAboutTitle = (TextView)findViewById(R.id.tvAboutTitle);
        tvAboutTitle.setText(ReadHTMLResource(R.raw.about));
        Button tvAboutStore = (Button)findViewById(R.id.tvAboutStore);
        tvAboutStore.setText("Rate and Comment");
        tvAboutStore.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse( "market://details?id="+context.getPackageName() ) );
				 context.startActivity( browse );
			}
		});
        Button tvAboutSource = (Button)findViewById(R.id.tvAboutSource);
        tvAboutSource.setText("Bug reports and Source code");
        tvAboutSource.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				 Intent browse = new Intent( Intent.ACTION_VIEW , Uri.parse( "https://github.com/davidgraeff/netpowerctrl-sf-mirror" ) );
				 context.startActivity( browse );
			}
		});
    }

    public Spanned ReadHTMLResource(int id) {
	    try {
	        Resources res = context.getResources();
	        InputStream in_s = res.openRawResource(id);
	        byte[] b = new byte[in_s.available()];
	        in_s.read(b);
	        PackageInfo pInfo = context.getPackageManager().getPackageInfo( context.getPackageName(), 0);
	        String s = new String(b);
	        s = s.replace("VersionX", "Version "+ pInfo.versionName);
	        return Html.fromHtml(s);
	    } catch (Exception e) {
	    	// nop
	    }
		return new SpannableString("");
    }

}
