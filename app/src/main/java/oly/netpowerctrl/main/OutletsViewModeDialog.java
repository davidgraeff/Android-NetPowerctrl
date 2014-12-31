package oly.netpowerctrl.main;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioButton;

import java.io.IOException;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.LoadStoreIconData;
import oly.netpowerctrl.data.SharedPrefs;
import oly.netpowerctrl.executables.AdapterSource;
import oly.netpowerctrl.executables.AdapterSourceInputDemo;
import oly.netpowerctrl.executables.ExecutablesAdapter;
import oly.netpowerctrl.ui.SoftRadioGroup;

/**
 * Try to setup all found devices, The dialog shows a short log about the actions.
 */
public class OutletsViewModeDialog extends DialogFragment {
    public OutletsViewModeDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View rootView = getActivity().getLayoutInflater().inflate(R.layout.fragment_select_view_type, null);

        final SoftRadioGroup radioGroupTheme = new SoftRadioGroup();
        radioGroupTheme.addView((RadioButton) rootView.findViewById(R.id.theme1));
        radioGroupTheme.addView((RadioButton) rootView.findViewById(R.id.theme2));
        radioGroupTheme.addView((RadioButton) rootView.findViewById(R.id.theme3));
        radioGroupTheme.addView((RadioButton) rootView.findViewById(R.id.theme4));

        String[] icon_themes = getResources().getStringArray(R.array.default_fallback_icon_set_keys);
        int[] icon_theme_layouts = new int[]{R.id.theme1_preview, R.id.theme2_preview, R.id.theme3_preview, R.id.theme4_preview};

        for (int i = 0; i < icon_theme_layouts.length; ++i) {
            try {
                View view = rootView.findViewById(icon_theme_layouts[i]);
                ((ImageView) view.findViewById(R.id.image_off)).setImageBitmap(
                        LoadStoreIconData.loadDefaultBitmap(getActivity(), LoadStoreIconData.IconState.StateOff, icon_themes[i]));
                ((ImageView) view.findViewById(R.id.image_on)).setImageBitmap(
                        LoadStoreIconData.loadDefaultBitmap(getActivity(), LoadStoreIconData.IconState.StateOn, icon_themes[i]));
                ((ImageView) view.findViewById(R.id.image_unknown)).setImageBitmap(
                        LoadStoreIconData.loadDefaultBitmap(getActivity(), LoadStoreIconData.IconState.StateUnknown, icon_themes[i]));
                final int finalI = i;
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        radioGroupTheme.check(finalI);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ////////////////////////////////////////////////////////////////////////////

        RecyclerView recyclerView;

        final SoftRadioGroup radioGroupDesign = new SoftRadioGroup();
        radioGroupDesign.addView((RadioButton) rootView.findViewById(R.id.design1));
        radioGroupDesign.addView((RadioButton) rootView.findViewById(R.id.design2));
        radioGroupDesign.addView((RadioButton) rootView.findViewById(R.id.design3));

        ExecutablesAdapter adapter;
        AdapterSource adapterSource;
        GridLayoutManager gridLayoutManager;
        int rows;

        rows = 1;
        adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartOnServiceReady);
        adapterSource.addInput(new AdapterSourceInputDemo());
        adapter = new ExecutablesAdapter(adapterSource, LoadStoreIconData.iconLoadingThread, R.layout.list_item_executable);
        adapter.setItemsInRow(rows);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.list1);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        gridLayoutManager = new GridLayoutManager(getActivity(), rows);
        gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup());
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(gridLayoutManager);
        final GestureDetector gestureDetector1 = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                radioGroupDesign.check(0);
                return super.onSingleTapConfirmed(e);
            }
        });
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector1.onTouchEvent(motionEvent);
                return true;
            }
        });

        rows = 2;
        adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartOnServiceReady);
        adapterSource.addInput(new AdapterSourceInputDemo());
        adapter = new ExecutablesAdapter(adapterSource, LoadStoreIconData.iconLoadingThread, R.layout.grid_item_executable);
        adapter.setItemsInRow(rows);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.list2);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        gridLayoutManager = new GridLayoutManager(getActivity(), rows);
        gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup());
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(gridLayoutManager);
        final GestureDetector gestureDetector2 = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                radioGroupDesign.check(1);
                return super.onSingleTapConfirmed(e);
            }
        });
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector2.onTouchEvent(motionEvent);
                return true;
            }
        });

        adapterSource = new AdapterSource(AdapterSource.AutoStartEnum.AutoStartOnServiceReady);
        adapterSource.addInput(new AdapterSourceInputDemo());
        adapter = new ExecutablesAdapter(adapterSource, LoadStoreIconData.iconLoadingThread, R.layout.grid_item_compact_executable);
        adapter.setItemsInRow(rows);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.list3);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(adapter);
        gridLayoutManager = new GridLayoutManager(getActivity(), rows);
        gridLayoutManager.setSpanSizeLookup(adapter.getSpanSizeLookup());
        recyclerView.setHasFixedSize(false);
        recyclerView.setLayoutManager(gridLayoutManager);
        final GestureDetector gestureDetector3 = new GestureDetector(getActivity(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                radioGroupDesign.check(2);
                return super.onSingleTapConfirmed(e);
            }
        });
        recyclerView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                gestureDetector3.onTouchEvent(motionEvent);
                return true;
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(rootView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int index = radioGroupTheme.getCheckedRadioButtonIndex();
                        if (index != -1) {
                            String[] icon_themes = getResources().getStringArray(R.array.default_fallback_icon_set_keys);
                            LoadStoreIconData.setDefaultFallbackIconSet(icon_themes[index]);
                        }

                        index = radioGroupDesign.getCheckedRadioButtonIndex();
                        if (index != -1) {
                            SharedPrefs.getInstance().setOutletsViewType(index);
                        }
                    }
                });
        return builder.create();
    }

    private void reloadProcess() {
        App.getMainThreadHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mStartActivity = new Intent(getActivity(), MainActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(getActivity(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                AlarmManager mgr = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                System.exit(0);
            }
        }, 50);
    }
}
