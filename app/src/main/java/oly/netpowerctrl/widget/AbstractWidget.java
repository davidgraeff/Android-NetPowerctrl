package oly.netpowerctrl.widget;

import oly.netpowerctrl.data.DataService;
import oly.netpowerctrl.preferences.SharedPrefs;

/**
 * Represents a widget on the home screen.
 */
abstract class AbstractWidget {
    final int widgetID;
    final String widgetType;
    WidgetUpdateService widgetUpdateService;

    public AbstractWidget(WidgetUpdateService widgetUpdateService, int widgetID, String widgetType) {
        this.widgetUpdateService = widgetUpdateService;
        this.widgetID = widgetID;
        this.widgetType = widgetType;
    }

    abstract void forceUpdate(DataService dataService);

    void remove() {
        SharedPrefs.getInstance().deleteWidget(widgetID, widgetType);
    }

    void destroy() {
        widgetUpdateService = null;
    }

    abstract void click(int position);
}