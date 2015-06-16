package oly.netpowerctrl.main;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.rey.material.widget.Button;

import oly.netpowerctrl.R;
import oly.netpowerctrl.preferences.SharedPrefs;
import oly.netpowerctrl.utils.GithubAndCloudant;

public class BuyFragment extends Fragment implements GithubAndCloudant.IPollDetails {
    Button btnYes, btnNo;
    boolean voted = false;
    GithubAndCloudant.PollDetails pollDetails;
    
    public BuyFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_buy, container, false);
        assert view != null;

        btnNo = (Button) view.findViewById(R.id.btnNo);
        btnYes = (Button) view.findViewById(R.id.btnYes);

        btnNo.setEnabled(false);
        btnYes.setEnabled(false);
        pollDetails = null;

        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pollDetails.yes++;
                voted = true;
                new GithubAndCloudant().writePollData(BuyFragment.this, "poll_buy_screen", pollDetails);
            }
        });
        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pollDetails.no++;
                voted = true;
                new GithubAndCloudant().writePollData(BuyFragment.this, "poll_buy_screen", pollDetails);
            }
        });

        new GithubAndCloudant().getPollData(this, "poll_buy_screen");

        return view;
    }

    @Override
    public void onPollDetails(GithubAndCloudant.PollDetails pollDetails) {
        if (pollDetails == null) {
            btnNo.setEnabled(false);
            btnYes.setEnabled(false);
            return;
        }

        btnNo.setText(getString(R.string.poll_no, pollDetails.no));
        btnYes.setText(getString(R.string.poll_yes, pollDetails.yes));

        if (voted)
            SharedPrefs.getInstance().vote(pollDetails._id + "2");

        if (SharedPrefs.getInstance().isVoted(pollDetails._id + "2"))
            return;

        this.pollDetails = pollDetails;
        btnNo.setEnabled(true);
        btnYes.setEnabled(true);
    }
}
