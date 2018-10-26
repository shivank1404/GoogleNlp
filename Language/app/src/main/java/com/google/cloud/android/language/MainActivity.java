/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.android.language;

import com.google.cloud.android.language.model.EntityInfo;
import com.google.cloud.android.language.model.SentimentInfo;
import com.google.cloud.android.language.model.TokenInfo;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;

public class MainActivity extends AppCompatActivity implements ApiFragment.Callback {

    private static final int API_ENTITIES = 0;
    private static final int API_SENTIMENT = 1;
    private static final int API_SYNTAX = 2;

    private static final String FRAGMENT_API = "api";

    private static final int LOADER_ACCESS_TOKEN = 1;

    private static final String STATE_SHOWING_RESULTS = "showing_results";

    private ViewPager mViewPager;
    private ProgressBar mProgressBar;

    private ResultPagerAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        DisplayMetrics m = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(m);

        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.width = m.widthPixels*4/5;
        params.height = m.heightPixels*3/4;
        params.gravity = Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;
        getWindow().setAttributes(params);

        overridePendingTransition(R.anim.slide_in_up, android.R.anim.slide_out_right);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressBar = findViewById(R.id.loader);

        // Set up the view pager
        final FragmentManager fm = getSupportFragmentManager();
        mAdapter = new ResultPagerAdapter(fm, this);
        mViewPager = (ViewPager) findViewById(R.id.pager);
        final Resources resources = getResources();
        mViewPager.setPageMargin(resources.getDimensionPixelSize(R.dimen.page_margin));
        mViewPager.setPageMarginDrawable(
                ResourcesCompat.getDrawable(resources, R.drawable.page_margin, getTheme()));
        mViewPager.setOffscreenPageLimit(3);
        mViewPager.setAdapter(mAdapter);
        TabLayout tab = (TabLayout) findViewById(R.id.tab);
        tab.setupWithViewPager(mViewPager);

        if (savedInstanceState == null) {
            // The app has just launched; handle share intent if it is necessary
            handleShareIntent();
        } else {
            // Configuration changes; restore UI states
            boolean results = savedInstanceState.getBoolean(STATE_SHOWING_RESULTS);
        }

        // Prepare the API
        if (getApiFragment() == null) {
            fm.beginTransaction().add(new ApiFragment(), FRAGMENT_API).commit();
        }
        prepareApi();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        startAnalyze();
    }

    private void handleShareIntent() {
        final Intent intent = getIntent();
        if (TextUtils.equals(intent.getAction(), Intent.ACTION_SEND)
                && TextUtils.equals(intent.getType(), "text/plain")) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
        }
    }

    private ApiFragment getApiFragment() {
        return (ApiFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_API);
    }

    private void prepareApi() {
        // Initiate token refresh
        getSupportLoaderManager().initLoader(LOADER_ACCESS_TOKEN, null,
                new LoaderManager.LoaderCallbacks<String>() {
                    @Override
                    public Loader<String> onCreateLoader(int id, Bundle args) {
                        return new AccessTokenLoader(MainActivity.this);
                    }

                    @Override
                    public void onLoadFinished(Loader<String> loader, String token) {
                        getApiFragment().setAccessToken(token);
                    }

                    @Override
                    public void onLoaderReset(Loader<String> loader) {
                    }
                });
    }

    private void startAnalyze() {
        // Call the API
        final String text = getIntent().getStringExtra("text");

        getApiFragment().analyzeEntities(text);
        getApiFragment().analyzeSentiment(text);
        getApiFragment().analyzeSyntax(text);
    }


    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public void onEntitiesReady(EntityInfo[] entities) {
        if (mViewPager.getCurrentItem() == API_ENTITIES) {
            if(mProgressBar.getVisibility()!=View.GONE)
                mProgressBar.setVisibility(View.GONE);
        }
        mAdapter.setEntities(entities);
    }

    @Override
    public void onSentimentReady(SentimentInfo sentiment) {
        if (mViewPager.getCurrentItem() == API_SENTIMENT) {
            if(mProgressBar.getVisibility()!=View.GONE)
                mProgressBar.setVisibility(View.GONE);
        }
        mAdapter.setSentiment(sentiment);
    }

    @Override
    public void onSyntaxReady(TokenInfo[] tokens) {
        if (mViewPager.getCurrentItem() == API_SYNTAX) {
            if(mProgressBar.getVisibility()!=View.GONE)
                mProgressBar.setVisibility(View.GONE);
        }
        mAdapter.setTokens(tokens);
    }

    /**
     * Provides content of the {@link ViewPager}.
     */
    public static class ResultPagerAdapter extends FragmentPagerAdapter {

        private final String[] mApiNames;

        private final Fragment[] mFragments = new Fragment[3];

        public ResultPagerAdapter(FragmentManager fm, Context context) {
            super(fm);
            mApiNames = context.getResources().getStringArray(R.array.api_names);
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            Fragment fragment = (Fragment) super.instantiateItem(container, position);
            mFragments[position] = fragment;
            return fragment;
        }

        @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case API_ENTITIES:
                    return EntitiesFragment.newInstance();
                case API_SENTIMENT:
                    return SentimentFragment.newInstance();
                case API_SYNTAX:
                    return SyntaxFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mApiNames[position];
        }

        @SuppressWarnings("ConstantConditions")
        @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
        public void setEntities(EntityInfo[] entities) {
            final EntitiesFragment fragment = (EntitiesFragment) mFragments[API_ENTITIES];
            if (fragment != null) {
                fragment.setEntities(entities);
            }
        }

        public void setSentiment(SentimentInfo sentiment) {
            final SentimentFragment fragment = (SentimentFragment) mFragments[API_SENTIMENT];
            if (fragment != null) {
                fragment.setSentiment(sentiment);
            }
        }

        public void setTokens(TokenInfo[] tokens) {
            final SyntaxFragment fragment = (SyntaxFragment) mFragments[API_SYNTAX];
            if (fragment != null) {
                fragment.setTokens(tokens);
            }
        }

    }

}
