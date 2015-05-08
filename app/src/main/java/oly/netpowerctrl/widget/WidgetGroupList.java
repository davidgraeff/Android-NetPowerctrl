package oly.netpowerctrl.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.Nullable;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.List;

import oly.netpowerctrl.R;
import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.data.graphic.LoadStoreIconData;
import oly.netpowerctrl.data.onServiceReady;
import oly.netpowerctrl.executables.Executable;
import oly.netpowerctrl.executables.ExecutableCollection;
import oly.netpowerctrl.groups.Group;
import oly.netpowerctrl.main.ExecutionActivity;
import oly.netpowerctrl.network.ReachabilityStates;
import oly.netpowerctrl.preferences.SharedPrefs;

;

/**
 * Represents
 */
public class WidgetGroupList extends AbstractWidget implements RemoteViewsService.RemoteViewsFactory {
    static int nextViewID = 1;
    private final int listViewID;
    private final List<WidgetGroupListItem> items = new ArrayList<>();
    private Group group;
    private onServiceReady forceUpdateOnServiceReady = new onServiceReady() {
        @Override
        public boolean onServiceReady(DataService service) {
            forceUpdate(service);
            return false;
        }

        @Override
        public void onServiceFinished(DataService service) {

        }
    };
    private android.os.Handler updateRemoteListHandler = new android.os.Handler() {
        @Override
        public void handleMessage(Message msg) {
            widgetUpdateService.appWidgetManager.notifyAppWidgetViewDataChanged(widgetID, listViewID);
        }
    };

    public WidgetGroupList(WidgetUpdateService widgetUpdateService, int widgetID, @Nullable DataService dataService) {
        super(widgetUpdateService, widgetID, ProviderGroup.getTypeString());
        listViewID = R.id.widget_group_item_list;
        if (dataService == null)
            DataService.observersServiceReady.register(forceUpdateOnServiceReady);
        else
            forceUpdate(dataService);
    }

    @Override
    void destroy() {
        for (WidgetGroupListItem e : items) e.destroy();
        items.clear();
        super.destroy();
    }

    @Override
    void forceUpdate(DataService dataService) {
        String mGroupID = SharedPrefs.getInstance().loadWidget(widgetID, ProviderGroup.getTypeString());
        if (mGroupID != null)
            group = dataService.groups.getByUID(mGroupID);
        else
            group = null;

        // Setup remote view
        RemoteViews views = new RemoteViews(widgetUpdateService.getPackageName(), R.layout.widget_group);

        Intent intent = new Intent(widgetUpdateService, WidgetUpdateService.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
        // When intents are compared, the extras are ignored, so we need to embed the extras
        // into the data so that the extras will not be ignored.
        intent.setData(Uri.parse(intent.toUri(Intent.URI_INTENT_SCHEME)));
        views.setRemoteAdapter(listViewID, intent);

        for (WidgetGroupListItem e : items) e.destroy();
        items.clear();
        widgetUpdateService.appWidgetManager.notifyAppWidgetViewDataChanged(widgetID, listViewID);

        // Setup list items
        if (group != null) {
            views.setTextViewText(R.id.widget_group_name, group.name);

            Intent clickIntent = new Intent(widgetUpdateService, WidgetUpdateService.class);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
            clickIntent.putExtra(WidgetUpdateService.EXTRA_WIDGET_TYPE, ProviderGroup.getTypeString());
            clickIntent.putExtra(WidgetUpdateService.EXTRA_WIDGET_COMMAND, WidgetUpdateService.CLICK_WIDGET);
            PendingIntent pendingIntent = PendingIntent.getService(widgetUpdateService, (int) System.currentTimeMillis(), clickIntent, 0);
            views.setPendingIntentTemplate(listViewID, pendingIntent);

            // Load items from group
            List<Executable> l = dataService.executables.filterExecutables(new ExecutableCollection.PredicateExecutable() {
                public boolean accept(Executable e) {
                    return e.getGroupUIDs().contains(group.uid);
                }
            });

            for (Executable e : l) {
                items.add(new WidgetGroupListItem(this, e));
            }

        } else {
            views.setTextViewText(R.id.widget_group_name, widgetUpdateService.getString(R.string.error_widget_needs_configuration));

            Intent clickIntent = new Intent(widgetUpdateService, ConfigGroupActivity.class);
            clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetID);
            PendingIntent pendingIntent = PendingIntent.getActivity(widgetUpdateService, (int) System.currentTimeMillis(), clickIntent, 0);
            views.setOnClickPendingIntent(R.id.widget_group_name, pendingIntent);
        }

        widgetUpdateService.appWidgetManager.updateAppWidget(widgetID, views);
    }

    void requestUpdateRemoteList() {
        updateRemoteListHandler.removeMessages(0);
        updateRemoteListHandler.sendEmptyMessageDelayed(0, 100);
    }

    @Override
    void click(int position) {

    }

    @Override
    public void onCreate() {

    }

    @Override
    public void onDataSetChanged() {

    }

    @Override
    public void onDestroy() {
        destroy();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        //Log.w("WIDGET_GROUP", "update " + String.valueOf(position));

        // Get current item, extract executable, sync cache with current value.
        // If a cached value equals the known current value of an executable, it will not issue an update
        // to the RemoveView list later on.
        WidgetGroupListItem widgetGroupListItem = items.get(position);
        Executable executable = widgetGroupListItem.executable;
        widgetGroupListItem.cached_last_state = executable.getCurrentValue();

        RemoteViews rv = new RemoteViews(widgetUpdateService.getPackageName(), R.layout.widget_group_item);
        rv.setTextViewText(R.id.title, executable.getTitle());

        LoadStoreIconData.IconState iconState;
        if (executable.reachableState() == ReachabilityStates.NotReachable) { // unreachable
            iconState = LoadStoreIconData.IconState.StateUnknown;
        } else if (executable.getCurrentValue() > 0) { // On
            iconState = LoadStoreIconData.IconState.StateOn;
        } else {
            iconState = LoadStoreIconData.IconState.StateOff;
        }

        Bitmap bitmap = LoadStoreIconData.loadBitmap(widgetUpdateService, executable, iconState, null);
        rv.setImageViewBitmap(R.id.icon_bitmap, bitmap);

        // Next, set a fill-intent, which will be used to fill in the pending intent template
        // that is set on the collection view in StackWidgetProvider.
        Bundle extras = new Bundle();
        extras.putInt(WidgetUpdateService.EXTRA_WIDGET_CLICK_POSITION, position);
        extras.putString(ExecutionActivity.EXECUTE_ACTION_UUID, executable.getUid());
        extras.putInt(ExecutionActivity.EXECUTE_ACTION_COMMAND, Executable.TOGGLE);

        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        // Make it possible to distinguish the individual on-click
        // action of a given item
        rv.setOnClickFillInIntent(R.id.title, fillInIntent);
        rv.setOnClickFillInIntent(R.id.icon_bitmap, fillInIntent);
        return rv;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int i) {
        return items.get(i).viewID;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
