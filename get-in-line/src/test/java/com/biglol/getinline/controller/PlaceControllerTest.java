package com.biglol.getinline.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.biglol.getinline.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("View 컨트롤러 - 장소")
@WebMvcTest(
        controllers = PlaceController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfig.class)
)
class PlaceControllerTest {

    private final MockMvc mvc;

    public PlaceControllerTest(@Autowired MockMvc mvc) {
        this.mvc = mvc;
    }

    @DisplayName("[view][GET] 장소 리스트 페이지")
    @Test
    void givenNothing_whenRequestingPlacesPage_thenReturnsPlacesPage() throws Exception {
        // Given

        // When & Then
        mvc.perform(get("/places"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(view().name("place/index"));
    }

    @DisplayName("[view][GET] 장소 세부 정보 페이지")
    @Test
    void givenNothing_whenRequestingPlaceDetailPage_thenReturnsPlaceDetailPage() throws Exception {
        // Given
        long placeId = 1L;

        // When & Then
        mvc.perform(get("/places/" + placeId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(view().name("place/detail"));
    }
}
