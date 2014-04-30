package com.bradnicolle.distractinator;

import android.os.Bundle;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener {
	public static final String EXTRA_TIME = "time";
	
	ViewPager mViewPager;
	AppPagerAdapter mAppPagerAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final ActionBar actionBar = getActionBar();
		
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		actionBar.setDisplayShowHomeEnabled(false);              
		actionBar.setDisplayShowTitleEnabled(false);
		
		mAppPagerAdapter = new AppPagerAdapter(getSupportFragmentManager());
		
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mAppPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });

        for (int i = 0; i < mAppPagerAdapter.getCount(); i++) {
        	ActionBar.Tab tab = actionBar.newTab();
        	tab.setIcon(mAppPagerAdapter.getIcon(i));
        	switch(i) {
        	case 0:
        		tab.setText(R.string.right_now);
        		break;
        	case 1:
        		tab.setText(R.string.timed);
        	}
        	tab.setTabListener(this);
            actionBar.addTab(tab);
        }
	}
	
	public void startDistraction(View view) {
		Intent serviceIntent = new Intent(this, OverlayService.class);
		// Pass the service the duration we wish it to run for
		serviceIntent.putExtra(EXTRA_TIME, ((TimerView)view).getTime());
		
		startService(serviceIntent);
		
		finish();
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		mViewPager.setCurrentItem(tab.getPosition());
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}
	
	public static class AppPagerAdapter extends FragmentPagerAdapter {

		public AppPagerAdapter(FragmentManager fm) {
			super(fm);
		}

		@Override
		public Fragment getItem(int i) {
			switch(i) {
			case 0:
				return new ImmediateDistraction();
			case 1:
				return new FutureDistractions();
			}
			return null;
		}

		@Override
		public int getCount() {
			return 2; // Only 2 tabs in this activity
		}
		
		public int getIcon(int i) {
			switch(i) {
			case 0:
				return android.R.drawable.ic_menu_today;
			case 1:
				return android.R.drawable.ic_menu_recent_history;
			}
			return -1;
		}
	}
}
