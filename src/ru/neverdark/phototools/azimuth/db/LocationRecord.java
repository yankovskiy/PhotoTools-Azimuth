/*******************************************************************************
 * Copyright (C) 2014 Artem Yankovskiy (artemyankovskiy@gmail.com).
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ru.neverdark.phototools.azimuth.db;

/**
 * Class contains a single record from the "locations" table
 */
public class LocationRecord {
    private String mLocationName;
    private long mId;
    private double mLatitude;
    private double mLongitude;
    private long mLastAccess;
    private int mMapType;
    private float mCameraZoom;

    /**
     * Gets the value of the field camera_zoom
     * 
     * @return value of the field camera_zoom
     */
    public float getCameraZoom() {
        return mCameraZoom;
    }

    /**
     * Gets the record id
     * 
     * @return record id
     */
    public long getId() {
        return mId;
    }

    /**
     * Gets the value of the field last_access
     * 
     * @return value of the field last_access
     */
    public long getLastAccess() {
        return mLastAccess;
    }

    /**
     * Gets the value of the field latitude
     * 
     * @return value of the field latitude
     */
    public double getLatitude() {
        return mLatitude;
    }

    /**
     * Gets the value of the field location_name
     * 
     * @return value of the field location_name
     */
    public String getLocationName() {
        return mLocationName;
    }

    /**
     * Gets the value of the field longitude
     * 
     * @return value of the field longitude
     */
    public double getLongitude() {
        return mLongitude;
    }

    /**
     * Gets the value of the field map_type
     * 
     * @return value of the field map_type
     */
    public int getMapType() {
        return mMapType;
    }

    /**
     * Stores the value of the field camera_zoom
     * 
     * @param cameraZoom
     *            value of the field camera_zoom
     */
    public void setCameraZoom(float cameraZoom) {
        this.mCameraZoom = cameraZoom;
    }

    /**
     * Stores the record id
     * 
     * @param id
     *            record id
     */
    public void setId(long id) {
        this.mId = id;
    }

    /**
     * Stores the value of the field last_access
     * 
     * @param lastAccess
     *            value of the field last_access
     */
    public void setLastAccess(long lastAccess) {
        this.mLastAccess = lastAccess;
    }

    /**
     * Stores the value of the field latitude
     * 
     * @param latitude
     *            value of the field latitude
     */
    public void setLatitude(double latitude) {
        this.mLatitude = latitude;
    }

    /**
     * Stores the value of the field location_name
     * 
     * @param locationName
     *            value of the field location_name
     */
    public void setLocationName(String locationName) {
        this.mLocationName = locationName;
    }

    /**
     * Stores the value of the field longitude
     * 
     * @param longitude
     *            value of the field longitude
     */
    public void setLongitude(double longitude) {
        this.mLongitude = longitude;
    }

    /**
     * Stores the value of the field map_type
     * 
     * @param mapType
     *            value of the field map_type
     */
    public void setMapType(int mapType) {
        this.mMapType = mapType;
    }
}