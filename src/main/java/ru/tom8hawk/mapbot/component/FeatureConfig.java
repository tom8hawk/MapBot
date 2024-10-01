package ru.tom8hawk.mapbot.component;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Component
public class FeatureConfig {

    @Getter
    private String markerColor;

    private SimpleDateFormat dateFormat;

    @Autowired
    public FeatureConfig(
            @Value("${feature.marker-color}") String markerColor,
            @Value("${feature.date-format}") String dateFormat
    ) {

        this.markerColor = markerColor;
        this.dateFormat = new SimpleDateFormat(dateFormat, new Locale("ru"));
    }

    public String formatDate(Date date) {
        return dateFormat.format(date);
    }
}