package ru.neverdark.phototools.azimuth;

import java.util.ArrayList;
import java.util.List;

import ru.neverdark.phototools.azimuth.utils.Log;

import android.content.Context;
import android.database.SQLException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Адаптер для связки UI и БД
 */
public class LocationAdapter extends ArrayAdapter<LocationRecord> {
    /**
     * Интерфейс для обработки клика по кнопке "удалить"
     */
    public interface OnRemoveClickListener {
        public void onRemoveClickHandler(final int position);
    }

    private static class RowHolder {
        private TextView mLocationName;
        private ImageView mLocationRemoveButton;
    }

    private static final String EXCEPTION_MESSAGE = "Database is not open";

    private OnRemoveClickListener mCallback;
    private List<LocationRecord> mObjects;
    private final int mResource;
    private final Context mContext;

    private LocationsDbAdapter mDbAdapter;

    /**
     * Конструктор
     * 
     * @param context
     *            контекст активити
     * @param resource
     *            id ресурса содержащего разметку для одной записи списка
     */
    public LocationAdapter(Context context, int resource) {
        this(context, resource, new ArrayList<LocationRecord>());
    }

    /**
     * Конструктор
     * 
     * @param context
     *            контекст активити
     * @param resource
     *            id ресурса содержащего разметку для одной записи списка
     * @param objects
     *            список объектов
     */
    private LocationAdapter(Context context, int resource,
            List<LocationRecord> objects) {
        super(context, resource, objects);
        mContext = context;
        mResource = resource;
        mObjects = objects;
    }

    /**
     * Закрывает соединиение с базой данных
     */
    public void closeDb() {
        Log.enter();
        if (mDbAdapter != null) {
            mDbAdapter.close();
            mDbAdapter = null;
        }
    }

    /**
     * Добавляет новое место в базу
     * 
     * @param locationName
     *            название местоположения
     * @param latitude
     *            широта
     * @param longitude
     *            долгота
     * @param mapType
     *            тип карты
     * @param cameraZoom
     *            зум камеры
     * @return id добавленной записи
     */
    public long createLocation(String locationName, double latitude,
            double longitude, int mapType, float cameraZoom) {
        Log.enter();
        long id = 0;
        if (mDbAdapter.isOpen()) {
            id = mDbAdapter.createLocation(locationName, latitude, longitude,
                    mapType, cameraZoom);
            mDbAdapter.fetchAllLocations(mObjects);
        } else {
            // бросить исключение
        }

        notifyDataSetChanged();
        return id;
    }

    /**
     * Удаляет выбранную запись из локального списка и из базы данных
     * 
     * @param record
     *            объект содержащий запись для удаления
     * 
     * @return true в случае успешного удаления записи
     */
    public boolean deleteLocation(LocationRecord record) {
        Log.enter();
        long recordId = record.getId();
        boolean deleteStatus = false;

        if (mDbAdapter.isOpen()) {
            remove(record);
            deleteStatus = mDbAdapter.deleteLocation(recordId);
        } else {
            throw new SQLException(EXCEPTION_MESSAGE);
        }

        notifyDataSetChanged();

        return deleteStatus;
    }

    /**
     * Получает Id записи в базе по позиции элемента в списке
     * 
     * @param position
     *            позиция элемента в списке
     * @return id записи в базе
     */
    public long getIdByPosition(final int position) {
        Log.enter();
        LocationRecord record = getItem(position);
        return record.getId();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Log.enter();
        View row = convertView;
        RowHolder holder = null;

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(mResource, parent, false);
            holder = new RowHolder();
            holder.mLocationRemoveButton = (ImageView) row
                    .findViewById(R.id.locationRow_image_remove);
            holder.mLocationName = (TextView) row
                    .findViewById(R.id.locationRow_label);
            row.setTag(holder);
        } else {
            holder = (RowHolder) row.getTag();
        }

        LocationRecord record = mObjects.get(position);
        holder.mLocationName.setText(record.getLocationName());

        setRemoveClickListener(holder, position);

        return row;
    }

    /**
     * Проверяет существование местоположения с указанным именем
     * 
     * @param locationName
     *            название местоположения для проверки
     * @return true если местоположение существует
     */
    public boolean isLocationExists(String locationName) {
        Log.enter();
        boolean exist = false;
        if (mDbAdapter.isOpen()) {
            exist = mDbAdapter.isLocationExists(locationName);
        } else {
            throw new SQLException(EXCEPTION_MESSAGE);
        }

        return exist;
    }

    /**
     * Загружает данные с базы данных
     */
    public void loadData() {
        Log.enter();
        if (mDbAdapter.isOpen()) {
            mDbAdapter.fetchAllLocations(mObjects);
        } else {
            throw new SQLException(EXCEPTION_MESSAGE);
        }
        notifyDataSetChanged();
    }

    /**
     * Открывает соединение с базой данных
     */
    public void openDb() {
        Log.enter();
        mDbAdapter = new LocationsDbAdapter(mContext);
        mDbAdapter.open();
    }

    /**
     * Устанавливает callback - объект реализующий интерфейс для обработки
     * кликов
     * 
     * @param callback
     *            объект
     */
    public void setCallback(OnRemoveClickListener callback) {
        mCallback = callback;
    }

    /**
     * Устанавливает обработчик клика по кнопке "удалить"
     * 
     * @param holder
     *            запись - строчка
     */
    private void setRemoveClickListener(RowHolder holder, final int position) {
        holder.mLocationRemoveButton
                .setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        try {
                            mCallback.onRemoveClickHandler(position);
                        } catch (NullPointerException e) {
                            Log.message("Callback not seted");
                        }
                    }

                });
    }

    /**
     * Обновляет время доступа к записи, передвигая новые наверх списка
     * 
     * @param position
     *            позиция выбранного элемента в списке
     */
    public void updateLastAccessTime(final int position) {
        Log.enter();
        long recordId = getIdByPosition(position);

        if (mDbAdapter.isOpen()) {
            mDbAdapter.udateLastAccessTime(recordId);
            mDbAdapter.fetchAllLocations(mObjects);
        } else {
            throw new SQLException(EXCEPTION_MESSAGE);
        }

        notifyDataSetChanged();
    }

    /**
     * Изменяет сохраненное место в базе данных
     * 
     * @param recordId
     *            id записи в базе
     * @param locationName
     *            название местоположения
     * @param latitude
     *            широта
     * @param longitude
     *            долгота
     * @param mapType
     *            тип карты
     * @param cameraZoom
     *            зум камеры
     */
    public void updateLocation(final long recordId, String locationName,
            double latitude, double longitude, int mapType, float cameraZoom) {
        Log.enter();
        if (mDbAdapter.isOpen()) {
            mDbAdapter.updateLocation(recordId, locationName, latitude,
                    longitude, mapType, cameraZoom);

            mDbAdapter.fetchAllLocations(mObjects);
        } else {
            throw new SQLException(EXCEPTION_MESSAGE);
        }

        notifyDataSetChanged();
    }
}
