package com.dsbeckham.nasaimageryfetcher.util;

import android.app.Activity;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.View;

import com.dsbeckham.nasaimageryfetcher.BuildConfig;
import com.dsbeckham.nasaimageryfetcher.activity.ViewPagerActivity;
import com.dsbeckham.nasaimageryfetcher.adapter.ApodAdapter;
import com.dsbeckham.nasaimageryfetcher.adapter.ImageFragmentStatePagerAdapter;
import com.dsbeckham.nasaimageryfetcher.adapter.IotdAdapter;
import com.dsbeckham.nasaimageryfetcher.fragment.ApodFragment;
import com.dsbeckham.nasaimageryfetcher.fragment.IotdFragment;
import com.dsbeckham.nasaimageryfetcher.model.ApodMorphIoModel;
import com.dsbeckham.nasaimageryfetcher.model.ApodNasaGovModel;
import com.dsbeckham.nasaimageryfetcher.model.IotdRssModel;

import org.parceler.Parcels;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.simplexml.SimpleXmlConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class QueryUtils {
    public static final String APOD_NASA_GOV_BASE_URL = "https://api.nasa.gov/";
    public static final String APOD_NASA_GOV_API_KEY = BuildConfig.APOD_NASA_GOV_API_KEY;
    public static final int APOD_NASA_GOV_API_QUERIES = 5;

    public static final String APOD_MORPH_IO_BASE_URL = "https://api.morph.io/";
    public static final String APOD_MORPH_IO_API_KEY = BuildConfig.APOD_MORPH_IO_API_KEY;

    public static final String IOTD_RSS_BASE_URL = "https://www.nasa.gov/";

    public static final int APOD_MODEL_MORPH_IO = 0;
    public static final int APOD_MODEL_NASA_GOV = 1;

    public static final int QUERY_MODE_RECYCLERVIEW = 0;
    public static final int QUERY_MODE_VIEWPAGER = 1;

    public interface ApodMorphIoService {
        @GET("dsbeckham/apod-scraper/data.json")
        Call<List<ApodMorphIoModel>> get(
                @Query("key") String key,
                @Query("query") String query);
    }

    public interface ApodNasaGovService {
        @GET("planetary/apod")
        Call<ApodNasaGovModel> get(
                @Query("api_key") String apiKey,
                @Query("date") String date);
    }

    public interface IotdRssService {
        @GET("rss/dyn/lg_image_of_the_day.rss")
        Call<IotdRssModel> get();
    }

    public static ApodMorphIoService apodMorphIoService;
    public static ApodNasaGovService apodNasaGovService;
    public static IotdRssService iotdRssService;

    public static void setUpIoServices() {
        Retrofit retrofit = new Retrofit.Builder().baseUrl(APOD_MORPH_IO_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apodMorphIoService = retrofit.create(ApodMorphIoService.class);

        retrofit = new Retrofit.Builder().baseUrl(APOD_NASA_GOV_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apodNasaGovService = retrofit.create(ApodNasaGovService.class);

        retrofit = new Retrofit.Builder().baseUrl(IOTD_RSS_BASE_URL)
                .addConverterFactory(SimpleXmlConverterFactory.createNonStrict())
                .build();

        iotdRssService = retrofit.create(IotdRssService.class);
    }

    public static void beginApodQuery(Activity activity, int mode) {
        if (mode == QUERY_MODE_RECYCLERVIEW) {
            ApodFragment apodFragment = (ApodFragment) activity.getFragmentManager().findFragmentByTag("apod");

            if (apodFragment == null) {
                return;
            }

            if (!apodFragment.loadingData) {
                if (apodFragment.apodMorphIoModels.isEmpty()) {
                    apodFragment.progressBar.setVisibility(View.VISIBLE);
                }

                switch (PreferenceManager.getDefaultSharedPreferences(activity).getString(PreferenceUtils.PREF_APOD_FETCH_SERVICE, "")) {
                    case "morph_io":
                        queryApodMorphIoApi(activity, QUERY_MODE_RECYCLERVIEW);
                        break;
                    case "nasa_gov":
                        apodFragment.nasaGovApiQueryCount = APOD_NASA_GOV_API_QUERIES;
                        queryApodNasaGovApi(activity, QUERY_MODE_RECYCLERVIEW);
                        break;
                }
            }
        } else if (mode == QUERY_MODE_VIEWPAGER) {
            final ImageFragmentStatePagerAdapter imageFragmentStatePagerAdapter = ((ViewPagerActivity) activity).imageFragmentStatePagerAdapter;

            if (imageFragmentStatePagerAdapter == null) {
                return;
            }

            if (!imageFragmentStatePagerAdapter.loadingData) {
                switch (PreferenceManager.getDefaultSharedPreferences(activity).getString(PreferenceUtils.PREF_APOD_FETCH_SERVICE, "")) {
                    case "morph_io":
                        queryApodMorphIoApi(activity, QUERY_MODE_VIEWPAGER);
                        break;
                    case "nasa_gov":
                        imageFragmentStatePagerAdapter.nasaGovApiQueryCount = APOD_NASA_GOV_API_QUERIES;
                        queryApodNasaGovApi(activity, QUERY_MODE_VIEWPAGER);
                        break;
                }
            }
        }
    }

    public static void clearApodData(Activity activity) {
        ApodFragment apodFragment = (ApodFragment) activity.getFragmentManager().findFragmentByTag("apod");

        if (apodFragment == null) {
            return;
        }

        if (!apodFragment.loadingData) {
            apodFragment.apodMorphIoModels.clear();
            apodFragment.apodNasaGovModels.clear();
            apodFragment.calendar = Calendar.getInstance();
            apodFragment.endlessRecyclerOnScrollListener.resetPageCount();
            apodFragment.fastItemAdapter.clear();
            apodFragment.footerAdapter.clear();
        }
    }

    public static void queryApodMorphIoApi(final Activity activity, final int mode) {
        if (mode == QUERY_MODE_RECYCLERVIEW) {
            final ApodFragment apodFragment = (ApodFragment) activity.getFragmentManager().findFragmentByTag("apod");

            if (apodFragment == null) {
                return;
            }

            apodFragment.loadingData = true;

            String query = String.format(Locale.US, "SELECT * FROM data WHERE date <= date('%d-%02d-%02d') ORDER BY date DESC LIMIT 30", apodFragment.calendar.get(Calendar.YEAR), (apodFragment.calendar.get(Calendar.MONTH) + 1), apodFragment.calendar.get(Calendar.DAY_OF_MONTH));
            Call<List<ApodMorphIoModel>> call = apodMorphIoService.get(APOD_MORPH_IO_API_KEY, query);
            call.enqueue(new Callback<List<ApodMorphIoModel>>() {
                @Override
                public void onResponse(Call<List<ApodMorphIoModel>> call, Response<List<ApodMorphIoModel>> response) {
                    if (response.isSuccessful()) {
                        apodFragment.footerAdapter.clear();
                        apodFragment.progressBar.setVisibility(View.GONE);

                        for (ApodMorphIoModel apodMorphIoModel : response.body()) {
                            if (!apodFragment.apodMorphIoModels.contains(apodMorphIoModel) && !apodMorphIoModel.getPictureThumbnailUrl().isEmpty()) {
                                apodFragment.apodMorphIoModels.add(apodMorphIoModel);
                                apodFragment.fastItemAdapter.add(apodFragment.fastItemAdapter.getAdapterItemCount(), new ApodAdapter<>(apodMorphIoModel, QueryUtils.APOD_MODEL_MORPH_IO));
                            }

                            apodFragment.calendar.add(Calendar.DAY_OF_YEAR, -1);
                        }

                        apodFragment.loadingData = false;
                        apodFragment.swipeRefreshLayout.setRefreshing(false);
                    }
                }

                @Override
                public void onFailure(Call<List<ApodMorphIoModel>> call, Throwable t) {
                    apodFragment.footerAdapter.clear();
                    apodFragment.loadingData = false;
                    apodFragment.progressBar.setVisibility(View.GONE);
                    apodFragment.swipeRefreshLayout.setRefreshing(false);
                }
            });
        } else if (mode == QUERY_MODE_VIEWPAGER) {
            final ImageFragmentStatePagerAdapter imageFragmentStatePagerAdapter = ((ViewPagerActivity) activity).imageFragmentStatePagerAdapter;

            if (imageFragmentStatePagerAdapter == null) {
                return;
            }

            imageFragmentStatePagerAdapter.loadingData = true;

            String query = String.format(Locale.US, "SELECT * FROM data WHERE date <= date('%d-%02d-%02d') ORDER BY date DESC LIMIT 30", imageFragmentStatePagerAdapter.calendar.get(Calendar.YEAR), (imageFragmentStatePagerAdapter.calendar.get(Calendar.MONTH) + 1), imageFragmentStatePagerAdapter.calendar.get(Calendar.DAY_OF_MONTH));
            Call<List<ApodMorphIoModel>> call = apodMorphIoService.get(APOD_MORPH_IO_API_KEY, query);
            call.enqueue(new Callback<List<ApodMorphIoModel>>() {
                @Override
                public void onResponse(Call<List<ApodMorphIoModel>> call, Response<List<ApodMorphIoModel>> response) {
                    if (response.isSuccessful()) {
                        for (ApodMorphIoModel apodMorphIoModel : response.body()) {
                            if (!imageFragmentStatePagerAdapter.apodMorphIoModels.contains(apodMorphIoModel) && !apodMorphIoModel.getPictureThumbnailUrl().isEmpty()) {
                                imageFragmentStatePagerAdapter.apodMorphIoModels.add(apodMorphIoModel);
                            }

                            imageFragmentStatePagerAdapter.calendar.add(Calendar.DAY_OF_YEAR, -1);
                        }

                        imageFragmentStatePagerAdapter.loadingData = false;
                        imageFragmentStatePagerAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(Call<List<ApodMorphIoModel>> call, Throwable t) {
                    imageFragmentStatePagerAdapter.loadingData = false;
                }
            });
        }
    }

    public static void queryApodNasaGovApi(final Activity activity, final int mode) {
        if (mode == QUERY_MODE_RECYCLERVIEW) {
            final ApodFragment apodFragment = (ApodFragment) activity.getFragmentManager().findFragmentByTag("apod");

            if (apodFragment == null) {
                return;
            }

            apodFragment.loadingData = true;

            String date = String.format(Locale.US, "%d-%02d-%02d", apodFragment.calendar.get(Calendar.YEAR), (apodFragment.calendar.get(Calendar.MONTH) + 1), apodFragment.calendar.get(Calendar.DAY_OF_MONTH));
            Call<ApodNasaGovModel> call = apodNasaGovService.get(APOD_NASA_GOV_API_KEY, date);
            call.enqueue(new Callback<ApodNasaGovModel>() {
                @Override
                public void onResponse(Call<ApodNasaGovModel> call, Response<ApodNasaGovModel> response) {
                    if (response.isSuccessful()) {
                        apodFragment.footerAdapter.clear();
                        apodFragment.progressBar.setVisibility(View.GONE);

                        if (!apodFragment.apodNasaGovModels.contains(response.body()) && response.body().getMediaType().equals("image")) {
                            apodFragment.apodNasaGovModels.add(response.body());
                            apodFragment.fastItemAdapter.add(apodFragment.fastItemAdapter.getAdapterItemCount(), new ApodAdapter<>(response.body(), QueryUtils.APOD_MODEL_NASA_GOV));
                        }

                        apodFragment.calendar.add(Calendar.DAY_OF_YEAR, -1);
                        apodFragment.nasaGovApiQueryCount--;

                        if (apodFragment.nasaGovApiQueryCount > 0) {
                            queryApodNasaGovApi(activity, mode);
                        } else {
                            apodFragment.loadingData = false;
                            apodFragment.swipeRefreshLayout.setRefreshing(false);
                        }

                    }
                }

                @Override
                public void onFailure(Call<ApodNasaGovModel> call, Throwable t) {
                    apodFragment.footerAdapter.clear();
                    apodFragment.loadingData = false;
                    apodFragment.progressBar.setVisibility(View.GONE);
                    apodFragment.swipeRefreshLayout.setRefreshing(false);
                }
            });
        } else if (mode == QUERY_MODE_VIEWPAGER) {
            final ImageFragmentStatePagerAdapter imageFragmentStatePagerAdapter = ((ViewPagerActivity) activity).imageFragmentStatePagerAdapter;

            if (imageFragmentStatePagerAdapter == null) {
                return;
            }

            imageFragmentStatePagerAdapter.loadingData = true;

            String date = String.format(Locale.US, "%d-%02d-%02d", imageFragmentStatePagerAdapter.calendar.get(Calendar.YEAR), (imageFragmentStatePagerAdapter.calendar.get(Calendar.MONTH) + 1), imageFragmentStatePagerAdapter.calendar.get(Calendar.DAY_OF_MONTH));
            Call<ApodNasaGovModel> call = apodNasaGovService.get(APOD_NASA_GOV_API_KEY, date);
            call.enqueue(new Callback<ApodNasaGovModel>() {
                @Override
                public void onResponse(Call<ApodNasaGovModel> call, Response<ApodNasaGovModel> response) {
                    if (response.isSuccessful()) {
                        if (!imageFragmentStatePagerAdapter.apodNasaGovModels.contains(response.body()) && !response.body().getUrl().isEmpty()) {
                            imageFragmentStatePagerAdapter.apodNasaGovModels.add(response.body());
                        }

                        imageFragmentStatePagerAdapter.calendar.add(Calendar.DAY_OF_YEAR, -1);
                        imageFragmentStatePagerAdapter.nasaGovApiQueryCount--;

                        if (imageFragmentStatePagerAdapter.nasaGovApiQueryCount > 0) {
                            queryApodNasaGovApi(activity, mode);
                        } else {
                            imageFragmentStatePagerAdapter.loadingData = false;
                        }

                        imageFragmentStatePagerAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(Call<ApodNasaGovModel> call, Throwable t) {
                    imageFragmentStatePagerAdapter.loadingData = false;
                }
            });
        }
    }

    public static void updateApodData(Activity activity, Intent intent) {
        ApodFragment apodFragment = (ApodFragment) activity.getFragmentManager().findFragmentByTag("apod");

        if (apodFragment == null) {
            return;
        }

        apodFragment.calendar = (Calendar) intent.getSerializableExtra(ApodFragment.EXTRA_APOD_CALENDAR);
        apodFragment.fastItemAdapter.clear();

        switch (PreferenceManager.getDefaultSharedPreferences(activity).getString(PreferenceUtils.PREF_APOD_FETCH_SERVICE, "")) {
            case "morph_io":
                apodFragment.apodMorphIoModels = Parcels.unwrap(intent.getParcelableExtra(ApodFragment.EXTRA_APOD_MORPH_IO_MODELS));

                for (ApodMorphIoModel apodMorphIoModel : apodFragment.apodMorphIoModels) {
                    apodFragment.fastItemAdapter.add(apodFragment.fastItemAdapter.getAdapterItemCount(), new ApodAdapter<>(apodMorphIoModel, QueryUtils.APOD_MODEL_MORPH_IO));
                }
                break;
            case "nasa_gov":
                apodFragment.apodNasaGovModels = Parcels.unwrap(intent.getParcelableExtra(ApodFragment.EXTRA_APOD_NASA_GOV_MODELS));

                for (ApodNasaGovModel apodNasaGovModel : apodFragment.apodNasaGovModels) {
                    apodFragment.fastItemAdapter.add(apodFragment.fastItemAdapter.getAdapterItemCount(), new ApodAdapter<>(apodNasaGovModel, QueryUtils.APOD_MODEL_NASA_GOV));
                }
                break;
        }

        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, typedValue, true);

        apodFragment.linearLayoutManager.scrollToPositionWithOffset(intent.getIntExtra(ApodFragment.EXTRA_APOD_POSITION, 0), activity.getResources().getDimensionPixelSize(typedValue.resourceId));
    }

    public static void beginIotdFetch(Activity activity) {
        IotdFragment iotdFragment = (IotdFragment) activity.getFragmentManager().findFragmentByTag("iotd");

        if (iotdFragment == null) {
            return;
        }

        if (!iotdFragment.loadingData) {
            if (iotdFragment.iotdRssModels.isEmpty()) {
                iotdFragment.progressBar.setVisibility(View.VISIBLE);
            }

            fetchIotdRssFeed(activity);
        }
    }

    public static void clearIotdData(Activity activity) {
        IotdFragment iotdFragment = (IotdFragment) activity.getFragmentManager().findFragmentByTag("iotd");

        if (iotdFragment == null) {
            return;
        }

        if (!iotdFragment.loadingData) {
            iotdFragment.iotdRssModels.clear();
            iotdFragment.fastItemAdapter.clear();
            iotdFragment.footerAdapter.clear();
        }
    }

    public static void fetchIotdRssFeed(final Activity activity) {
        final IotdFragment iotdFragment = (IotdFragment) activity.getFragmentManager().findFragmentByTag("iotd");

        if (iotdFragment == null) {
            return;
        }

        iotdFragment.loadingData = true;

        Call<IotdRssModel> call = iotdRssService.get();
        call.enqueue(new Callback<IotdRssModel>() {
            @Override
            public void onResponse(Call<IotdRssModel> call, Response<IotdRssModel> response) {
                if (response.isSuccessful()) {
                    iotdFragment.footerAdapter.clear();
                    iotdFragment.progressBar.setVisibility(View.GONE);

                    for (IotdRssModel.Channel.Item iotdRssModelItem : response.body().getChannel().getItems()) {
                        if (!iotdFragment.iotdRssModels.contains(iotdRssModelItem) && !iotdRssModelItem.getEnclosure().getUrl().isEmpty()) {
                            iotdFragment.iotdRssModels.add(iotdRssModelItem);
                            iotdFragment.fastItemAdapter.add(iotdFragment.fastItemAdapter.getAdapterItemCount(), new IotdAdapter(iotdRssModelItem));
                        }
                    }

                    iotdFragment.loadingData = false;
                    iotdFragment.swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onFailure(Call<IotdRssModel> call, Throwable t) {
                iotdFragment.footerAdapter.clear();
                iotdFragment.loadingData = false;
                iotdFragment.progressBar.setVisibility(View.GONE);
                iotdFragment.swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    public static void updateIotdData(Activity activity, Intent intent) {
        final IotdFragment iotdFragment = (IotdFragment) activity.getFragmentManager().findFragmentByTag("iotd");

        if (iotdFragment == null) {
            return;
        }

        TypedValue typedValue = new TypedValue();
        activity.getTheme().resolveAttribute(android.support.v7.appcompat.R.attr.actionBarSize, typedValue, true);

        iotdFragment.linearLayoutManager.scrollToPositionWithOffset(intent.getIntExtra(IotdFragment.EXTRA_IOTD_POSITION, 0), activity.getResources().getDimensionPixelSize(typedValue.resourceId));
    }
}
