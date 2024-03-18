package com.biglol.getinline.controller.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.biglol.getinline.dto.ApiDataResponse;
import com.biglol.getinline.dto.PlaceRequest;

/**
 * Spring Data REST 로 API 를 만들어서 당장 필요가 없어진 컨트롤러. 우선 deprecated 하고, 향후 사용 방안을 고민해 본다. 필요에 따라서는 다시 살릴
 * 수도 있음
 *
 * @deprecated 0.1.2
 */
@Deprecated
// @RequestMapping("/api")
// @RestController
public class ApiPlaceController {
    //    @GetMapping("/places")
    //    public APIDataResponse<List<PlaceDto>> getPlaces() {
    //        return APIDataResponse.of(
    //                List.of(
    //                        PlaceDto.of(
    //                                PlaceType.COMMON,
    //                                "랄라배드민턴장",
    //                                "서울시 강남구 강남대로 1234",
    //                                "010-1234-5678",
    //                                30,
    //                                "신장개업")));
    //    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/places")
    public ApiDataResponse<Void> createPlace(@RequestBody PlaceRequest placeRequest) {
        return ApiDataResponse.empty();
    }

    //    @GetMapping("/places/{placeId}")
    //    public APIDataResponse<PlaceDto> getPlace(@PathVariable Long placeId) {
    //        if (placeId.equals(2L)) {
    //            return APIDataResponse.empty();
    //        }
    //
    //        return APIDataResponse.of(
    //                PlaceDto.of(
    //                        PlaceType.COMMON,
    //                        "랄라배드민턴장",
    //                        "서울시 강남구 강남대로 1234",
    //                        "010-1234-5678",
    //                        30,
    //                        "신장개업"));
    //    }

    @PutMapping("/places/{placeId}")
    public ApiDataResponse<Void> modifyPlace(
            @PathVariable Long placeId, @RequestBody PlaceRequest placeRequest) {
        return ApiDataResponse.empty();
    }

    @DeleteMapping("/places/{placeId}")
    public ApiDataResponse<Void> removePlace(@PathVariable Long placeId) {
        return ApiDataResponse.empty();
    }
}
