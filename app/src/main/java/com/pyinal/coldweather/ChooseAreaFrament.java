package com.pyinal.coldweather;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.pyinal.coldweather.db.City;
import com.pyinal.coldweather.db.County;
import com.pyinal.coldweather.db.Province;
import com.pyinal.coldweather.util.HttpUtil;
import com.pyinal.coldweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by admin on 2018/6/10.
 */

public class ChooseAreaFrament extends Fragment {
    public static final int LEVEL_PROVINCE = 0;
    public static final int LEVEL_CITY = 1;
    public static final int LEVEL_COUNTY = 2;
    private ProgressDialog progressDialog;
    private TextView titleText;
    private Button backButton;
    private ListView listView;
    private ArrayAdapter<String> adapter;
    private List<String> dataList = new ArrayList<>();
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private Province selectProvince;
    private City selectCity;
    private int currentLeve;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        }else {
            adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, dataList);
        }
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (currentLeve == LEVEL_PROVINCE) {
                selectProvince = provinceList.get(position);
                queryCities();
            }else if (currentLeve == LEVEL_CITY){
                selectCity = cityList.get(position);
                queryCounties();
            }else if (currentLeve == LEVEL_COUNTY){
                String weatherId = countyList.get(position).getWeatherId();
                Intent intent = new Intent(getActivity(),WeatherActivity.class);
                intent.putExtra("weather_id",weatherId);
                startActivity(intent);
                getActivity().finish();
            }
        });
        backButton.setOnClickListener(v -> {
            if (currentLeve == LEVEL_COUNTY){
                queryCities();

            }else if (currentLeve == LEVEL_CITY){
                queryProvinces();
            }
        });
        queryProvinces();
    }

    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size()>0){
            dataList.clear();
            for (Province province:provinceList){
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLeve = LEVEL_PROVINCE;
        }else {
            String address = "http://guolin.tech/api/china";
            queryFromServe(address,"province");
        }
    }

    private void queryCities(){
        titleText.setText(selectProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList=DataSupport.where("provinceid = ?",String.valueOf(selectProvince.getId())).find(City.class);
        if (cityList.size()>0){
            dataList.clear();
            for (City city:cityList){
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLeve = LEVEL_CITY;
        }else {
            int provinceCode = selectProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china"+"/"+provinceCode;
            queryFromServe(address,"city");
        }
    }

    private void queryCounties(){
        titleText.setText(selectCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?",String.valueOf(selectCity.getId())).find(County.class);
        if (countyList.size()>0){
            dataList.clear();
            for (County county:countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLeve = LEVEL_COUNTY;
        }else {
            int provinceCode = selectProvince.getProvinceCode();
            int cityCode = selectCity.getCityCode();
            String address = "http://guolin.tech/api/china"+"/"+provinceCode+"/"+cityCode;
            queryFromServe(address,"county");
        }
    }

    private void queryFromServe(String address,final String type){
        showProgressDialog();
        HttpUtil.sendOkhttpResquest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(()->{
                    closeProgressDialog();
                    Toast.makeText(getActivity(),"加载失败",Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = false;
                if ("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText);
                }else if ("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectProvince.getId());
                }else if ("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,selectCity.getId());
                }
                if(result){
                    getActivity().runOnUiThread(()->{
                        closeProgressDialog();
                        if ("province".equals(type)){
                            queryProvinces();
                        }else if ("city".equals(type)){
                            queryCities();
                        }else if ("county".equals(type)){
                            queryCounties();
                        }
                    });
                }
            }
        });
    }

    private void showProgressDialog(){
        if (progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("Loading...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    private void closeProgressDialog(){
        if (progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
