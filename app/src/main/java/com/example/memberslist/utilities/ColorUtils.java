package com.example.memberslist.utilities;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.NonNull;

import com.example.memberslist.R;

import java.util.LinkedHashMap;
import java.util.Map;

public class ColorUtils {
    private static ColorUtils instance;
    private final Map<Integer, String> colorMap;
    public ColorUtils(Context context) {
        colorMap = new LinkedHashMap<>();
        initializeColorMap(context);
    }
    public static synchronized ColorUtils getInstance(Context context) {
        if (instance == null) instance = new ColorUtils(context);
        return instance;
    }
    private void initializeColorMap(@NonNull Context context) {
        colorMap.put(Color.RED, context.getString(R.string.color_red));
        colorMap.put(Color.BLUE, context.getString(R.string.color_blue));
        colorMap.put(Color.GREEN, context.getString(R.string.color_green));
        colorMap.put(Color.YELLOW, context.getString(R.string.color_yellow));
        colorMap.put(Color.BLACK, context.getString(R.string.color_black));
        colorMap.put(Color.CYAN, context.getString(R.string.color_cyan));
        colorMap.put(Color.MAGENTA, context.getString(R.string.color_magenta));
        colorMap.put(Color.DKGRAY, context.getString(R.string.color_dark_gray));
        colorMap.put(Color.LTGRAY, context.getString(R.string.color_light_gray));
        colorMap.put(context.getColor(R.color.water), context.getString(R.string.color_water));
        colorMap.put(context.getColor(R.color.crimson_red), context.getString(R.string.color_crimson_red));
        colorMap.put(context.getColor(R.color.light_green), context.getString(R.string.color_light_green));
        colorMap.put(context.getColor(R.color.orange), context.getString(R.string.color_orange));
        colorMap.put(context.getColor(R.color.maroon), context.getString(R.string.color_maroon));
        colorMap.put(context.getColor(R.color.dark_green), context.getString(R.string.color_dark_green));
        colorMap.put(context.getColor(R.color.gold), context.getString(R.string.color_gold));
        colorMap.put(context.getColor(R.color.baby_pink), context.getString(R.string.color_baby_pink));
        colorMap.put(context.getColor(R.color.sky_blue), context.getString(R.string.color_sky_blue));
    }
    public Map<Integer, String> getColorMap() {
        return colorMap;
    }
}
