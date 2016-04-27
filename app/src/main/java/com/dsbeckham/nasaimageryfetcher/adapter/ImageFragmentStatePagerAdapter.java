package com.dsbeckham.nasaimageryfetcher.adapter;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.dsbeckham.nasaimageryfetcher.activity.ViewPagerActivity;
import com.dsbeckham.nasaimageryfetcher.fragment.ApodFragment;
import com.dsbeckham.nasaimageryfetcher.fragment.ImageFragment;
import com.dsbeckham.nasaimageryfetcher.fragment.IotdFragment;
import com.dsbeckham.nasaimageryfetcher.model.ApodMorphIoModel;
import com.dsbeckham.nasaimageryfetcher.model.ApodNasaGovModel;
import com.dsbeckham.nasaimageryfetcher.model.IotdRssModel;
import com.dsbeckham.nasaimageryfetcher.util.QueryUtils;

import org.parceler.Parcels;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class ImageFragmentStatePagerAdapter extends SmartFragmentStatePagerAdapter {
    private Activity activity;

    public List<ApodMorphIoModel> apodMorphIoModels = new ArrayList<>();
    public List<ApodNasaGovModel> apodNasaGovModels = new ArrayList<>();
    public List<IotdRssModel.Channel.Item> iotdRssModels = new ArrayList<>();

    public Calendar calendar = Calendar.getInstance();
    public boolean loadingData = false;
    public int nasaGovApiQueryCount = QueryUtils.APOD_NASA_GOV_API_QUERIES;

    public ImageFragmentStatePagerAdapter(final Activity activity, FragmentManager fragmentManager) {
        super(fragmentManager);

        this.activity = activity;

        switch (((ViewPagerActivity) activity).currentFragment) {
            case "iotd":
                iotdRssModels = Parcels.unwrap(activity.getIntent().getParcelableExtra(IotdFragment.EXTRA_IOTD_RSS_MODELS));
                break;
            case "apod":
                switch (((ViewPagerActivity) activity).apodFetchService) {
                    case "morph_io":
                        apodMorphIoModels = Parcels.unwrap(activity.getIntent().getParcelableExtra(ApodFragment.EXTRA_APOD_MORPH_IO_MODELS));
                        break;
                    case "nasa_gov":
                        apodNasaGovModels = Parcels.unwrap(activity.getIntent().getParcelableExtra(ApodFragment.EXTRA_APOD_NASA_GOV_MODELS));
                        break;
                }

                calendar = (Calendar) activity.getIntent().getSerializableExtra(ApodFragment.EXTRA_APOD_CALENDAR);
                break;
        }

        ((ViewPagerActivity) activity).viewPager.post(new Runnable() {
            @Override
            public void run() {
                ((ViewPagerActivity) activity).viewPager.setCurrentItem(((ViewPagerActivity) activity).viewPagerCurrentItem, false);
            }
        });
    }

    @Override
    public int getCount() {
        switch (((ViewPagerActivity) activity).currentFragment) {
            case "iotd":
                return iotdRssModels.size();
            case "apod":
                switch (((ViewPagerActivity) activity).apodFetchService) {
                    case "morph_io":
                        return apodMorphIoModels.size();
                    case "nasa_gov":
                        return apodNasaGovModels.size();
                }
        }
        return 0;
    }

    @Override
    public Fragment getItem(int position) {
        if (((ViewPagerActivity) activity).currentFragment.equals("apod")) {
            if (position == getCount() - 1) {
                QueryUtils.beginApodQuery(activity, QueryUtils.QUERY_MODE_VIEWPAGER);
            }
        }
        return ImageFragment.newInstance(position);
    }
}
