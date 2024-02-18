package com.biglol.getinline.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/admin")
@Controller
public class AdminController {
    @GetMapping("/places")
    public String adminPlaces() {
        return "admin/places";
    }

    @GetMapping("/places/{placeId}")
    public String adminPlaceDetail(@PathVariable Integer placeId) { // webMvc autoConfiguration에 의해 자동으로 Integer로 컨버팅됨
        return "admin/place-detail";
    }

    @GetMapping("/events")
    public String adminEvents() {
        return "admin/events";
    }

    @GetMapping("/events/{eventId}")
    public String adminEventDetail(@PathVariable Integer eventId) {
        return "admin/event-detail";
    }
}
