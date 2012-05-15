package oly.netpowerctrl;

import java.io.InputStream;

import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.util.Linkify;
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
        //tvAboutTitle.setLinkTextColor(Color.WHITE);
        Linkify.addLinks(tvAboutTitle, Linkify.ALL);
    }

    public Spanned ReadHTMLResource(int id) {
	    try {
	        Resources res = context.getResources();
	        InputStream in_s = res.openRawResource(id);
	        byte[] b = new byte[in_s.available()];
	        in_s.read(b);
	        return Html.fromHtml(new String(b));
	    } catch (Exception e) {
	    	// nop
	    }
		return new SpannableString("");
    }

}
