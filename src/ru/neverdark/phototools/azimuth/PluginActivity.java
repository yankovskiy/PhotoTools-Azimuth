package ru.neverdark.phototools.azimuth;

import java.util.Calendar;
import java.util.TimeZone;

import ru.neverdark.phototools.azimuth.DeleteConfirmationDialog.OnDeleteConfirmationListener;
import ru.neverdark.phototools.azimuth.TimeZoneSelectionDialog.OnTimeZoneSelectionListener;
import ru.neverdark.phototools.azimuth.controller.AsyncCalculator;
import ru.neverdark.phototools.azimuth.model.SunCalculator;
import ru.neverdark.phototools.azimuth.utils.Log;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

public class PluginActivity extends SherlockFragmentActivity implements
        OnMapLongClickListener, OnCameraChangeListener {

    private class CalculationResultListener implements
            AsyncCalculator.OnCalculationResultListener {
        @Override
        public void onGetResultFail() {
            showErrorDialog(R.string.error_timeZoneIsNotDefined);
        }

        @Override
        public void onGetResultSuccess(
                SunCalculator.CalculationResult calculationResult) {
            mAzimuth = calculationResult.getAzimuth();
            mAltitude = calculationResult.getAltitude();
            
            if (mAltitude < 0) {
                showErrorDialog(R.string.error_noSun);
            }
            drawAzimuth();
        }
    }

    private class ConfirmDateTimeListener implements
            DateTimeDialog.OnConfirmDateTimeListener {
        @Override
        public void onConfirmDateTimeHandler(Calendar calendar) {
            mCalendar = calendar;

            if (mLocation != null) {
                calculate();
            }
        }
    }

    private class DeleteConfirmationListener implements
            OnDeleteConfirmationListener {
        @Override
        public void onDeleteConfirmationHandler(LocationRecord locationRecord) {
            mAdapter.deleteLocation(locationRecord);
        }
    }

    private class LocationItemClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position,
                long id) {
            selectItem(position);
        }
    }

    private class RemoveClickListener implements
            LocationAdapter.OnRemoveClickListener {
        @Override
        public void onRemoveClickHandler(final int position) {
            LocationRecord record = mAdapter.getItem(position);
            DeleteConfirmationDialog dialog = DeleteConfirmationDialog
                    .getInstance(mContext);
            dialog.setLocationRecord(record);
            dialog.setCallback(new DeleteConfirmationListener());
            dialog.show(getSupportFragmentManager(),
                    DeleteConfirmationDialog.DIALOG_TAG);
        }
    }

    private class SaveLocationListener implements
            SaveLocationDialog.OnSaveLocationListener {
        @Override
        public void onSaveLocationHandler(SaveLocationDialog.SaveDialogData data) {
            final int actionType = data.getActionType();
            switch (actionType) {
            case SaveLocationDialog.ACTION_TYPE_NEW:
                long id = mAdapter.createLocation(data.getLocationRecord()
                        .getLocationName(), data.getLocationRecord()
                        .getLatitude(),
                        data.getLocationRecord().getLongitude(), data
                                .getLocationRecord().getMapType(), data
                                .getLocationRecord().getCameraZoom());
                mSaveDialogData.getLocationRecord().setId(id);
                mSaveDialogData
                        .setActionType(SaveLocationDialog.ACTION_TYPE_EDIT);
                break;
            case SaveLocationDialog.ACTION_TYPE_EDIT:
                mAdapter.updateLocation(data.getLocationRecord().getId(), data
                        .getLocationRecord().getLocationName(), data
                        .getLocationRecord().getLatitude(), data
                        .getLocationRecord().getLongitude(), data
                        .getLocationRecord().getMapType(), data
                        .getLocationRecord().getCameraZoom());
                break;
            }
            mLocationList.setItemChecked(0, true);
            setTitle(data.getLocationRecord().getLocationName());
        }
    }

    private class TimeZoneSelectionListener implements
            OnTimeZoneSelectionListener {
        @Override
        public void onTimeZoneSelectionHandler(TimeZone timeZone) {
            mTimeZone = timeZone;
            if (mMarker != null) {
                calculate();
            }
        }
    }

    private static final String CAMERA_ZOOM = "zoom";

    private static final String IS_SAVED = "isSaved";
    private static final String LATITUDE = "latitude";
    private static final String LONGITUDE = "longitude";
    private static final String MAP_TYPE = "mapType";
    private static final float MINIMUM_ZOOM_SIZE = 5.0f;
    private LocationAdapter mAdapter;
    private double mAltitude;
    private double mAzimuth;
    private Calendar mCalendar;
    private Context mContext;
    private DrawerLayout mDrawerLayout;
    private CharSequence mDrawerTitle;
    private ActionBarDrawerToggle mDrawerToggle;
    private boolean mIsMenuItemDoneVisible;
    private LatLng mLocation;
    private ListView mLocationList;
    private GoogleMap mMap;

    private Marker mMarker;

    private MenuItem mMenuItemDateTime;
    private MenuItem mMenuItemDone;
    private MenuItem mMenuItemTimeZone;
    private double mOldZoom = -1;
    private final SaveLocationDialog.SaveDialogData mSaveDialogData;
    private TimeZone mTimeZone;

    private CharSequence mTitle;

    public PluginActivity() {
        mSaveDialogData = new SaveLocationDialog.SaveDialogData();
        mSaveDialogData.setLocationRecord(new LocationRecord());
        mIsMenuItemDoneVisible = false;
        mTimeZone = null;
    }

    private void bindObjectToResource() {
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mLocationList = (ListView) findViewById(R.id.location_list);
    }

    private void calculate() {
        boolean isInternetTimezone = Settings.isInternetTimeZone(mContext);
        AsyncCalculator asyncCalc = new AsyncCalculator(this,
                new CalculationResultListener());
        asyncCalc.setIsInternetTimeZone(isInternetTimezone);
        asyncCalc.setTimeZone(mTimeZone);
        asyncCalc.setLocation(mLocation);
        asyncCalc.setCalendar(mCalendar);
        asyncCalc.execute();
    }

    private void clearMap() {
        if (mMarker != null) {
            mMap.clear();
            mMarker = null;
        }
    }

    private void drawAzimuth() {
        setMarker();

        if (mMap.getCameraPosition().zoom >= MINIMUM_ZOOM_SIZE) {
            if (mAltitude > 0) {
                double size = mMap.getProjection().getVisibleRegion().farLeft.longitude
                        - mMap.getProjection().getVisibleRegion().nearRight.longitude;
                size = Math.abs(size);

                PolylineOptions options = new PolylineOptions();
                options.add(mLocation);
                options.add(SunCalculator.getDestLatLng(mLocation, mAzimuth,
                        size));
                options.width(5);
                options.color(Color.RED);

                mMap.addPolyline(options);
            }
        } else {
            showErrorMessage(R.string.error_zoomToSmall);
        }
    }

    /**
     * Inits map
     */
    private void initMap() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map)).getMap();
        }

        mMap.setMyLocationEnabled(true);
        mMap.setOnMapLongClickListener(this);
        mMap.setOnCameraChangeListener(this);

        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);

        int mapType = prefs.getInt(MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);
        mMap.setMapType(mapType);

        if (prefs.getBoolean(IS_SAVED, false) == true) {
            double latitude = prefs.getFloat(LATITUDE, 0);
            double longitude = prefs.getFloat(LONGITUDE, 0);
            float zoom = prefs.getFloat(CAMERA_ZOOM, 0);
            CameraPosition currentPosition = new CameraPosition.Builder()
                    .target(new LatLng(latitude, longitude)).zoom(zoom).build();
            mMap.moveCamera(CameraUpdateFactory
                    .newCameraPosition(currentPosition));
        }
    }

    @Override
    public void onCameraChange(CameraPosition camera) {
        if (mMarker != null) {
            if (mOldZoom != camera.zoom) {
                drawAzimuth();
                mOldZoom = camera.zoom;
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.plugin_activity);
        bindObjectToResource();

        initMap();

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mTitle = getTitle();
        mDrawerTitle = getString(R.string.drawer_title);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.open_drawer,
                R.string.close_drawer) {
            @Override
            public void onDrawerClosed(View view) {
                supportInvalidateOptionsMenu();
                getSupportActionBar().setTitle(mTitle);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                supportInvalidateOptionsMenu();
                getSupportActionBar().setTitle(mDrawerTitle);
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mCalendar = Calendar.getInstance();
        mContext = this;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getSupportMenuInflater().inflate(R.menu.main, menu);
        mMenuItemDone = menu.findItem(R.id.item_confirmSelection);
        mMenuItemDateTime = menu.findItem(R.id.item_dateTime);
        mMenuItemTimeZone = menu.findItem(R.id.item_timeZone);
        return true;
    }

    @Override
    public void onMapLongClick(LatLng location) {
        if (mMarker == null) {
            mSaveDialogData.setActionType(SaveLocationDialog.ACTION_TYPE_NEW);
        } else {
            mSaveDialogData.setActionType(SaveLocationDialog.ACTION_TYPE_EDIT);
        }

        mLocation = location;
        calculate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            if (mDrawerLayout.isDrawerOpen(mLocationList)) {
                mDrawerLayout.closeDrawer(mLocationList);
            } else {
                mDrawerLayout.openDrawer(mLocationList);
            }
            break;
        case R.id.item_map_normal:
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            break;
        case R.id.item_map_terrain:
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            break;
        case R.id.item_map_hybrid:
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            break;
        case R.id.item_map_satellite:
            mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            break;
        case R.id.item_confirmSelection:
            showSaveLocationDialog();
            break;
        case R.id.item_dateTime:
            showDateTimeDialog();
            break;
        case R.id.item_settings:
            showSettings();
            break;
        case R.id.item_timeZone:
            showTimeZoneSelectionDialog();
            break;
        case R.id.item_rate:
            showRate();
            break;
        case R.id.item_feedback:
            showFeedback();
            break;
        }
        return true;
    }

    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        Editor editor = prefs.edit();
        editor.putInt(MAP_TYPE, mMap.getMapType());
        editor.putFloat(CAMERA_ZOOM, mMap.getCameraPosition().zoom);
        editor.putFloat(LATITUDE,
                (float) mMap.getCameraPosition().target.latitude);
        editor.putFloat(LONGITUDE,
                (float) mMap.getCameraPosition().target.longitude);
        editor.putBoolean(IS_SAVED, true);
        editor.commit();

        mAdapter.closeDb();
        mAdapter.clear();
        mAdapter = null;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content
        // view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mLocationList);
        mMenuItemDateTime.setVisible(!drawerOpen);
        mMenuItemDone.setVisible(!drawerOpen && mIsMenuItemDoneVisible);
        mMenuItemTimeZone.setVisible(!Settings.isInternetTimeZone(mContext));
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        Log.enter();
        super.onResume();
        mAdapter = new LocationAdapter(this, R.layout.location_row);

        mAdapter.setCallback(new RemoveClickListener());
        mAdapter.openDb();
        mAdapter.loadData();

        mLocationList.setAdapter(mAdapter);

        mLocationList.setOnItemClickListener(new LocationItemClickListener());
    }

    private void selectItem(int position) {
        LocationRecord record = mAdapter.getItem(position);
        mSaveDialogData.setActionType(SaveLocationDialog.ACTION_TYPE_EDIT);
        mSaveDialogData.setLocationRecord(record);
        mLocation = new LatLng(record.getLatitude(), record.getLongitude());

        // move camera to saved position
        CameraPosition currentPosition = new CameraPosition.Builder()
                .target(mLocation).zoom(record.getCameraZoom()).build();
        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(currentPosition));

        // sets saved zoom
        mMap.setMapType(record.getMapType());

        mAdapter.updateLastAccessTime(position);
        mLocationList.setItemChecked(0, true);
        mDrawerLayout.closeDrawer(mLocationList);
        setTitle(record.getLocationName());
        // calculate azimuth
        calculate();
    }

    /**
     * Sets marker to the long tap position If marker already exists - remove
     * old marker and set new marker in new position
     * 
     * @param location
     *            location for setting marker
     */
    private void setMarker() {
        clearMap();

        // set new marker
        mMarker = mMap.addMarker(new MarkerOptions().position(mLocation));
        mMenuItemDone.setVisible(true);
        mIsMenuItemDoneVisible = true;
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    private void showDateTimeDialog() {
        DateTimeDialog dialog = DateTimeDialog.getInstance(mContext);
        dialog.setCalendar(mCalendar);
        dialog.setCallBack(new ConfirmDateTimeListener());
        dialog.show(getSupportFragmentManager(), DateTimeDialog.DIALOG_TAG);
    }

    private void showErrorDialog(int resourceId) {
        ErrorDialog dialog = ErrorDialog.getIntstance(mContext);
        dialog.setErrorMessage(resourceId);
        dialog.show(getSupportFragmentManager(), ErrorDialog.DIALOG_TAG);
    }

    private void showErrorMessage(int resourceId) {
        Toast.makeText(mContext, resourceId, Toast.LENGTH_LONG).show();
    }

    private void showFeedback() {
        // TODO Auto-generated method stub

    }

    private void showRate() {
        // TODO Auto-generated method stub

    }

    private void showSaveLocationDialog() {
        mSaveDialogData.getLocationRecord().setLatitude(mLocation.latitude);
        mSaveDialogData.getLocationRecord().setLongitude(mLocation.longitude);
        mSaveDialogData.getLocationRecord().setMapType(mMap.getMapType());
        mSaveDialogData.getLocationRecord().setCameraZoom(
                mMap.getCameraPosition().zoom);

        SaveLocationDialog dialog = SaveLocationDialog.getInstance(mContext);
        dialog.setCallback(new SaveLocationListener());
        dialog.setSaveDialogData(mSaveDialogData);
        dialog.show(getSupportFragmentManager(), SaveLocationDialog.DIALOG_TAG);
    }

    private void showSettings() {
        Intent settings = new Intent(this, SettingsActivity.class);
        startActivity(settings);
    }

    private void showTimeZoneSelectionDialog() {
        TimeZoneSelectionDialog dialog = TimeZoneSelectionDialog
                .getInstance(mContext);
        dialog.setCallback(new TimeZoneSelectionListener());
        dialog.show(getSupportFragmentManager(),
                TimeZoneSelectionDialog.DIALOG_TAG);
    }

}
