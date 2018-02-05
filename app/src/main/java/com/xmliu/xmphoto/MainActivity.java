package com.xmliu.xmphoto;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amap.api.maps2d.model.LatLng;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 参考博文 2
 * http://www.cnblogs.com/plokmju/p/android_exif.html
 * http://blog.5ibc.net/p/5493.html
 */
public class MainActivity extends Activity implements GeocodeSearch.OnGeocodeSearchListener {

    private TextView albumTV;
    private TextView cameraTV;
    private TextView phoneTV;
    private TextView latlngTV;
    private TextView locationTV;
    private ImageView resultIV;

    private static final int RESULT_CAPTURE_CODE = 100;
    private static final int RESULT_IMAGE_CODE = 200;

    private String mImagePath;
    private String TAG = MainActivity.class.getSimpleName();

    private GeocodeSearch geocoderSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        albumTV = (TextView) findViewById(R.id.photo_album_tv);
        cameraTV = (TextView) findViewById(R.id.photo_camera_tv);
        phoneTV = (TextView) findViewById(R.id.photo_phone_tv);
        latlngTV = (TextView) findViewById(R.id.photo_latlng_tv);
        locationTV = (TextView) findViewById(R.id.photo_location_tv);
        resultIV = (ImageView) findViewById(R.id.photo_result_iv);

        albumTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPhoto(RESULT_IMAGE_CODE);
            }
        });
        cameraTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectPhoto(RESULT_CAPTURE_CODE);
            }
        });

    }

    private void selectPhoto(int type) {
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss")
                .format(new Date());
        mImagePath = "/sdcard/" + timeStamp + ".jpg";
        final File tmpCameraFile = new File(mImagePath);
        if (type == RESULT_IMAGE_CODE) {
            startActivityForResult(
                    new Intent(Intent.ACTION_PICK).setType(
                            "image/*").putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(tmpCameraFile)),
                    RESULT_IMAGE_CODE);
        } else if (type == RESULT_CAPTURE_CODE) {
            startActivityForResult(new Intent(
                            MediaStore.ACTION_IMAGE_CAPTURE).putExtra(
                            MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(tmpCameraFile)),
                    RESULT_CAPTURE_CODE);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_CAPTURE_CODE && resultCode == RESULT_OK) {
            Log.i(TAG, "RESULT_CAPTURE_CODE" + mImagePath);
            regeoLatlng(mImagePath, RESULT_CAPTURE_CODE);
        }
        if (requestCode == RESULT_IMAGE_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            Log.i(TAG, "RESULT_IMAGE_CODE" + uriToRealPath(uri));
            regeoLatlng(uriToRealPath(uri), RESULT_IMAGE_CODE);
        }

    }
    private String uriToRealPath(Uri uri) {
        Cursor cursor = managedQuery(uri,
                new String[]{MediaStore.Images.Media.DATA},
                null,
                null,
                null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
        return path;
    }

    private void regeoLatlng(String path, int type) {
        // 结果UI展示
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap bm = BitmapFactory.decodeFile(path, options);
        resultIV.setImageBitmap(bm);
        locationTV.setText("");
        phoneTV.setText("");
        latlngTV.setText("");

        String latLngStr = getPhotoLocation(path);
        double lat = Double.parseDouble(latLngStr.split("-")[0]);
        double lon = Double.parseDouble(latLngStr.split("-")[1]);
        LatLng latLng = new LatLng(lat, lon);
        // 如果经纬度为空并且是来源于拍照的话，那么就调用定位方法，此处逻辑还需要优化
        if (lat == 0 && lon == 0 && type == RESULT_CAPTURE_CODE) {
//            startLocation();
//            latLng = new LatLng(currentLat, currentLon);
        }

        // 反地理编码
        geocoderSearch = new GeocodeSearch(MainActivity.this);
        geocoderSearch.setOnGeocodeSearchListener(MainActivity.this);
        LatLonPoint latLonPoint = new LatLonPoint(latLng.latitude, latLng.longitude);
        RegeocodeQuery query = new RegeocodeQuery(latLonPoint, 200, GeocodeSearch.AMAP);
        geocoderSearch.getFromLocationAsyn(query);
    }



    public String getPhotoLocation(String imagePath) {
        Log.i("TAG", "getPhotoLocation==" + imagePath);
        float output1 = 0;
        float output2 = 0;

        try {
            ExifInterface exifInterface = new ExifInterface(imagePath);
            String datetime = exifInterface.getAttribute(ExifInterface.TAG_DATETIME);// 拍摄时间
            String deviceName = exifInterface.getAttribute(ExifInterface.TAG_MAKE);// 设备品牌
            String deviceModel = exifInterface.getAttribute(ExifInterface.TAG_MODEL); // 设备型号
            String latValue = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String lngValue = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String latRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String lngRef = exifInterface.getAttribute
                    (ExifInterface.TAG_GPS_LONGITUDE_REF);
            if (latValue != null && latRef != null && lngValue != null && lngRef != null) {
                output1 = convertRationalLatLonToFloat(latValue, latRef);
                output2 = convertRationalLatLonToFloat(lngValue, lngRef);
            }
            setDiffColor(phoneTV, "手机型号：" + deviceName + "," + deviceModel);
            setDiffColor(latlngTV, "经纬度：" + output1 + ";" + output2);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return output1 + "-" + output2;
    }

    /**
     * TextView分段设置颜色等样式
     * @param textView
     * @param str
     */
    private void setDiffColor(TextView textView,String str){
        SpannableString sp = new SpannableString(str);
        sp.setSpan(new ForegroundColorSpan(Color.RED),str.indexOf("：")+1,str.length(), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
        textView.setText(sp);
        textView.setMovementMethod(LinkMovementMethod.getInstance());

    }

    private static float convertRationalLatLonToFloat(
            String rationalString, String ref) {

        String[] parts = rationalString.split(",");

        String[] pair;
        pair = parts[0].split("/");
        double degrees = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());

        pair = parts[1].split("/");
        double minutes = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());

        pair = parts[2].split("/");
        double seconds = Double.parseDouble(pair[0].trim())
                / Double.parseDouble(pair[1].trim());

        double result = degrees + (minutes / 60.0) + (seconds / 3600.0);
        if ((ref.equals("S") || ref.equals("W"))) {
            return (float) -result;
        }
        return (float) result;
    }

    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
        Log.i("TAG", "onRegeocodeSearched==" + i);
        if (TextUtils.isEmpty(regeocodeResult.getRegeocodeAddress().getBuilding())) {
            setDiffColor(locationTV, "地理位置：" + regeocodeResult.getRegeocodeAddress().getFormatAddress());
        } else {
            setDiffColor(locationTV, "地理位置：" + regeocodeResult.getRegeocodeAddress().getBuilding());
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }
}
