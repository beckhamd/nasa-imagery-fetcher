package com.dsbeckham.nasaimageryfetcher.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.dsbeckham.nasaimageryfetcher.R;
import com.dsbeckham.nasaimageryfetcher.model.UniversalImageModel;
import com.dsbeckham.nasaimageryfetcher.util.DateTimeUtils;
import com.mikepenz.fastadapter.items.AbstractItem;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import butterknife.Bind;
import butterknife.ButterKnife;

public class RecyclerViewAdapter extends AbstractItem<RecyclerViewAdapter, RecyclerViewAdapter.ViewHolder> {
    public UniversalImageModel universalImageModel;

    public RecyclerViewAdapter(UniversalImageModel universalImageModel) {
        this.universalImageModel = universalImageModel;
    }

    @Override
    public void bindView(final ViewHolder viewHolder) {
        super.bindView(viewHolder);

        viewHolder.progressBar.setVisibility(View.VISIBLE);

        viewHolder.date.setText(DateTimeUtils.convertDateToLongDateFormat(viewHolder.date.getContext(), universalImageModel.getDate(), "yyyy-MM-dd"));
        viewHolder.title.setText(universalImageModel.getTitle());

        Picasso.with(viewHolder.imageView.getContext())
                .load(universalImageModel.getImageThumbnailUrl())
                .fit()
                .centerCrop()
                .into(viewHolder.imageView, new Callback() {
                    @Override
                    public void onSuccess() {
                        viewHolder.progressBar.setVisibility(View.GONE);
                    }

                    @Override
                    public void onError() {
                        viewHolder.progressBar.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public int getType() {
        return R.id.item_recyclerview;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.item_recylerview;
    }

    protected static class ViewHolder extends RecyclerView.ViewHolder {
        @Bind(R.id.item_recyclerview_date_textview) TextView date;
        @Bind(R.id.item_recyclerview_imageview) ImageView imageView;
        @Bind(R.id.item_recyclerview_progressbar) View progressBar;
        @Bind(R.id.item_recyclerview_title_textview) TextView title;

        public ViewHolder(View view) {
            super(view);
            ButterKnife.bind(this, view);
        }
    }
}