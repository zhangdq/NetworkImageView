package com.darrenmowat.imageloader.demo;

import java.util.ArrayList;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.darrenmowat.imageloader.library.NetworkImageView;

public class ImageAdapter extends BaseAdapter {
    
    private Activity activity;
    public ArrayList<String> urls;
    
    public ImageAdapter(ArrayList<String> urls, Activity activity) {
        this.activity = activity;
        this.urls = urls;
    }

    @Override
    public int getCount() {
        return urls.size();
    }

    @Override
    public Object getItem(int position) {
        if(position >= urls.size()) {
            return null;
        }
        return urls.get(position);
    }

    @Override
    public long getItemId(int position) {
        if(position >= urls.size()) {
            return 0;
        }
        return urls.get(position).hashCode();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
    	NetworkImageView iv;
        if (convertView == null) {
            convertView = iv = new NetworkImageView(activity);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        } else {
            iv = (NetworkImageView) convertView;
        }
        
        iv.setImageUrl(urls.get(position), activity);
        
        return iv;
    }

    
}