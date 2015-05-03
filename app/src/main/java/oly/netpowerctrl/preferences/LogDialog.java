package oly.netpowerctrl.preferences;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import oly.netpowerctrl.R;
import oly.netpowerctrl.main.App;
import oly.netpowerctrl.ui.LineDividerDecoration;
import oly.netpowerctrl.ui.RecyclerViewWithAdapter;
import oly.netpowerctrl.ui.notifications.InAppNotifications;
import oly.netpowerctrl.utils.Logging;

public class LogDialog extends DialogFragment implements Logging.LogChanged, SwipeRefreshLayout.OnRefreshListener {
    private static int TYPE_MAIN = 0;
    private static int TYPE_POWER = 1;
    private static int TYPE_WIDGETS = 2;
    private static int TYPE_ALARM = 3;
    private static int TYPE_EXTENSIONS = 4;
    private static int TYPE_DETECTION = 5;
    private final ArrayList<LogItem> listItems = new ArrayList<>();
    SwipeRefreshLayout mPullToRefreshLayout;
    private Bitmap[] bitmapsForType = new Bitmap[6];
    private RecyclerView.Adapter<ViewHolder> adapter = new RecyclerView.Adapter<ViewHolder>() {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int type) {
            return new ViewHolder(LayoutInflater.from(viewGroup.getContext())
                    .inflate(R.layout.list_item_log, viewGroup, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, int position) {
            LogItem logItem = listItems.get(position);
            viewHolder.date.setText(logItem.date);
            viewHolder.time.setText(logItem.time);
            viewHolder.type.setImageBitmap(bitmapsForType[logItem.typeInt]);
            viewHolder.text.setText(logItem.text);
        }

        @Override
        public int getItemCount() {
            return listItems.size();
        }
    };
    private RecyclerViewWithAdapter<RecyclerView.Adapter<ViewHolder>> recyclerViewWithAdapter;
    private BufferedReader reader;
    private View btnRemove;

    public LogDialog() {
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_log, null);

        bitmapsForType[TYPE_MAIN] = BitmapFactory.decodeResource(getResources(), R.drawable.netpowerctrl);
        bitmapsForType[TYPE_ALARM] = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_recent_history);
        bitmapsForType[TYPE_EXTENSIONS] = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_gallery);
        bitmapsForType[TYPE_POWER] = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_lock_idle_low_battery);
        bitmapsForType[TYPE_WIDGETS] = BitmapFactory.decodeResource(getResources(), R.drawable.netpowerctrl);
        bitmapsForType[TYPE_DETECTION] = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_menu_zoom);
        recyclerViewWithAdapter = new RecyclerViewWithAdapter<>(getActivity(), view, adapter, R.string.log_no_records);
        recyclerViewWithAdapter.getRecyclerView().addItemDecoration(new LineDividerDecoration(getActivity(), LineDividerDecoration.VERTICAL_LIST) {
            @Override
            public boolean dividerForPosition(int position) {
                return listItems.get(position).typeInt == TYPE_MAIN;
            }
        });

        btnRemove = view.findViewById(R.id.btnRemove);
        btnRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearLog();
            }
        });

        mPullToRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.ptr_layout);
        mPullToRefreshLayout.setOnRefreshListener(this);
        mPullToRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view)
                .setPositiveButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                }).setNegativeButton(R.string.remove, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                clearLog();
            }
        }).setNeutralButton(R.string.menu_log_send_mail, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.menu_help)
                        .setMessage(R.string.log_send_mail_confirmation)
                        .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        })
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                App.setErrorReportContentLogFile(Logging.getInstance().getLogFile().getAbsolutePath());
                                InAppNotifications.silentException(null, null);
                                App.setErrorReportContentCrash();
                                Toast.makeText(getActivity(), R.string.log_data_send, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_info).show();
            }
        });
        return builder.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        listItems.clear();
        Logging logging = Logging.getInstance();
        reader = logging.getReader();
        if (logging.getLogFile().length() > 1024) {
            try {
                reader.skip(logging.getLogFile().length() - 1024);
            } catch (IOException ignored) {
            }
        }
        logging.setLogChangedListener(this);
        onLogChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        Logging.getInstance().setLogChangedListener(null);
    }

    @Override
    public void onLogChanged() {
        try {
            String line;
            do {
                line = reader.readLine();
                if (line != null)
                    listItems.add(0, new LogItem(line));
            } while (line != null);
        } catch (IOException e) {
            e.printStackTrace();
        }
        adapter.notifyDataSetChanged();
        btnRemove.setEnabled(adapter.getItemCount() != 0);
    }

    private void clearLog() {
        Logging.getInstance().clear();
        listItems.clear();
        adapter.notifyDataSetChanged();
        //noinspection ConstantConditions
        getActivity().invalidateOptionsMenu();
    }

    @Override
    public void onRefresh() {
        mPullToRefreshLayout.setRefreshing(false);
    }

    private class ViewHolder extends RecyclerView.ViewHolder {
        ImageView type;
        TextView date;
        TextView time;
        TextView text;

        public ViewHolder(View itemView) {
            super(itemView);
            type = (ImageView) itemView.findViewById(R.id.log_type);
            date = (TextView) itemView.findViewById(R.id.log_date);
            time = (TextView) itemView.findViewById(R.id.log_time);
            text = (TextView) itemView.findViewById(R.id.log_text);
        }
    }

    private class LogItem {
        private String type = "";
        private String date = "";
        private String time = "";
        private String text;
        private int typeInt = TYPE_MAIN;

        LogItem(String line) {
            String[] strings = line.replace('\t', '\n').split("\\|");
            if (strings.length < 4) {
                text = line;
                return;
            }
            date = strings[0];
            time = strings[1];
            type = strings[2];
            text = strings[3];

            switch (type) {
                case "PWR":
                    typeInt = TYPE_POWER;
                    break;
                case "ALM":
                    typeInt = TYPE_ALARM;
                    break;
                case "EXT":
                    typeInt = TYPE_EXTENSIONS;
                    break;
                case "WDG":
                    typeInt = TYPE_WIDGETS;
                case "DTC":
                    typeInt = TYPE_DETECTION;
                    break;
            }
        }


    }
}
