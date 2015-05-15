package oly.netpowerctrl.data.graphic;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DialogFragment;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.rey.material.app.Dialog;

import java.io.IOException;
import java.lang.ref.WeakReference;

import oly.netpowerctrl.R;
import oly.netpowerctrl.ui.RecyclerItemClickListener;
import oly.netpowerctrl.ui.ThemeHelper;

/**
 * Select drawable from a list or reset the drawable set before.
 */
public class SelectDrawableDialog extends DialogFragment {
    private static final int PICK_IMAGE_BEFORE_KITKAT = 10;
    private static final int PICK_IMAGE_KITKAT = 11;
    private static WeakReference<Object> icon_callback_context_object;
    private IconSelected callback;
    private Object callback_context_object;
    private ArrayAdapterWithIcons adapter;
    private String assetSet;
    private Bitmap[] list_of_icons = null;

    public SelectDrawableDialog() {
    }

    @SuppressLint("NewApi")
    public static void activityCheckForPickedImage(final Context context,
                                                   final IconSelected callback,
                                                   int requestCode, int resultCode,
                                                   Intent imageReturnedIntent) {
        if (resultCode == Activity.RESULT_OK && (requestCode == PICK_IMAGE_KITKAT || requestCode == PICK_IMAGE_BEFORE_KITKAT)) {
            Uri selectedImage = imageReturnedIntent.getData();
            try {
                Bitmap b = Utils.getDrawableFromUri(context, selectedImage).getBitmap();
                callback.setIcon(icon_callback_context_object != null ? icon_callback_context_object.get() : null,
                        Utils.resizeBitmap(context, b, 128, 128));
                icon_callback_context_object = null;
            } catch (IOException e) {
                Toast.makeText(context, context.getString(R.string.error_icon),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static SelectDrawableDialog createSelectDrawableDialog(String assetSet, IconSelected callback, Object callback_context_object) {
        SelectDrawableDialog s = new SelectDrawableDialog();
        s.callback = callback;
        s.callback_context_object = callback_context_object;
        s.assetSet = assetSet;
        return s;
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_with_list, container, false);
        SwipeRefreshLayout swipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.ptr_layout);
        swipeRefreshLayout.setEnabled(false);
        RecyclerView mRecyclerView = (RecyclerView) rootView.findViewById(android.R.id.list);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        AssetManager assetMgr = getActivity().getAssets();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            String[] list_of_icon_paths = null;
            try {
                list_of_icon_paths = assetMgr.list(assetSet);
                list_of_icons = new Bitmap[list_of_icon_paths.length];
                int c = 0;
                for (String filename : list_of_icon_paths) {
                    list_of_icons[c] = BitmapFactory.decodeStream(assetMgr.open(assetSet + "/" + filename));
                    ++c;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (list_of_icons != null && list_of_icon_paths != null)
                for (int i = 0; i < list_of_icons.length; ++i) {
                    adapter.items.add(new ArrayAdapterWithIcons.Item(list_of_icon_paths[i],
                            new BitmapDrawable(getActivity().getResources(), list_of_icons[i])));
                }
        }

        adapter = new ArrayAdapterWithIcons(getActivity());
        mRecyclerView.setAdapter(adapter);
        if (adapter.getItemCount() == 0) {
            mRecyclerView.setVisibility(View.GONE);
            swipeRefreshLayout.setVisibility(View.GONE);
            TextView textView = (TextView) rootView.findViewById(R.id.empty_text);
            textView.setText(R.string.no_asset_found);
            rootView.findViewById(android.R.id.empty).setVisibility(View.VISIBLE);
        }

        RecyclerItemClickListener onItemClickListener = new RecyclerItemClickListener(getActivity(), new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, boolean isLongClick) {
                assert list_of_icons != null;
                callback.setIcon(callback_context_object, list_of_icons[position]);
                dismiss();
                return true;
            }
        }, null);
        mRecyclerView.addOnItemTouchListener(onItemClickListener);

        return rootView;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        com.rey.material.app.Dialog dialog = new com.rey.material.app.Dialog(getActivity(), ThemeHelper.getDialogRes(getActivity()));

        //SimpleDialog.Builder builder = new SimpleDialog.Builder(ThemeHelper.getDialogRes(context));
        dialog.layoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        dialog.setTitle(getString(R.string.dialog_icon_title));
        dialog.negativeAction(android.R.string.cancel).negativeActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        dialog.neutralAction(R.string.dialog_icon_default).neutralActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callback.setIcon(callback_context_object, null);
                dismiss();
            }
        });
        dialog.positiveAction(R.string.dialog_icon_select).positiveActionClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (callback_context_object != null)
                    icon_callback_context_object = new WeakReference<>(callback_context_object);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/*");
                    callback.startActivityForResult(Intent.createChooser(intent,
                                    getResources().getString(R.string.dialog_icon_select)),
                            PICK_IMAGE_BEFORE_KITKAT
                    );
                } else {
                    try {
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.setType("image/*");
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        callback.startActivityForResult(intent, PICK_IMAGE_KITKAT);
                    } catch (ActivityNotFoundException ignored) {
                        Toast.makeText(getActivity(), "Cannot open file chooser", Toast.LENGTH_SHORT).show();
                    }
                }
                dismiss();
            }
        });

        return dialog;
    }
}
