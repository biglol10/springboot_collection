package com.biglol.getinline.domain;

import java.time.LocalDateTime;

import com.biglol.getinline.constant.PlaceType;

import lombok.Data;

@Data
public class Place {
    private Long id;

    private PlaceType placeType;
    private String placeName;
    private String address;
    private String phoneNumber;
    private Integer capacity;
    private String memo;

    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
