package com.biglol.getinline.domain;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class AdminPlaceMap {
    private Long id;

    private Long adminId;
    private Long PlaceId;

    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;
}
