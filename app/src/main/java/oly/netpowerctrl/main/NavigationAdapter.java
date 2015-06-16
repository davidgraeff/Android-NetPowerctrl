package oly.netpowerctrl.main;

import android.content.Context;
import android.support.v4.content.ContextCompat;

import oly.netpowerctrl.R;
import oly.netpowerctrl.groups.GroupAdapter;
import oly.netpowerctrl.ui.EmptyListener;
import oly.netpowerctrl.ui.ThemeHelper;

/**
 *
 */
public class NavigationAdapter extends GroupAdapter {
    public int offset = 4;

    public NavigationAdapter() {
        super(true, new EmptyListener() {
            public void onEmptyListener(boolean empty) {
            }
        });
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, int position) {
        int res;
        Context context;

        switch (position) {
            case 0:
                context = viewHolder.textView.getContext();
                viewHolder.textView.setText(context.getText(R.string.devices));
                res = ThemeHelper.getDrawableRes(context, R.attr.ic_action_dock);
                viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, res), null, null, null);
                break;
            case 1:
                context = viewHolder.textView.getContext();
                viewHolder.textView.setText(context.getText(R.string.preferences));
                res = ThemeHelper.getDrawableRes(context, R.attr.ic_action_settings);
                viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, res), null, null, null);
                break;
            case 2:
                context = viewHolder.textView.getContext();
                viewHolder.textView.setText(context.getText(R.string.drawer_buy));
                res = ThemeHelper.getDrawableRes(context, R.attr.ic_action_buy);
                viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, res), null, null, null);
                break;
            case 3:
                context = viewHolder.textView.getContext();
                viewHolder.textView.setText(context.getText(R.string.drawer_feedback));
                res = ThemeHelper.getDrawableRes(context, R.attr.ic_action_good);
                viewHolder.textView.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(context, res), null, null, null);
                break;

            default:
                super.onBindViewHolder(viewHolder, position - offset);
        }
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + offset;
    }

    @Override
    protected void _notifyItemInserted(int pos) {
        notifyItemInserted(pos + offset);
    }

    @Override
    protected void _notifyItemRemoved(int pos) {
        notifyItemRemoved(pos + offset);
    }

    @Override
    protected void _notifyItemChanged(int pos) {
        notifyItemChanged(pos + offset);
    }
}
