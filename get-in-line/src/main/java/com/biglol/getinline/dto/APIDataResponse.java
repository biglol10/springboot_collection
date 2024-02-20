package com.biglol.getinline.dto;

import com.biglol.getinline.constant.ErrorCode;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@EqualsAndHashCode(
        callSuper =
                true) // extend한 ErrorResponse안에 있는 equalshascode도 불러서 사용할 수 있게끔 안에 있는 필드들도 동일한지 검사를
// 해야됨, 그걸 가능하게 해줌
// 이 경우, Child 클래스의 인스턴스 두 개가 동등한지 비교할 때, Child 클래스의 필드뿐만 아니라 Parent 클래스의 필드값도 비교에 포함됩니다.
public class APIDataResponse<T> extends APIErrorResponse {

    private final T data;

    private APIDataResponse(T data) {
        super(true, ErrorCode.OK.getCode(), ErrorCode.OK.getMessage());
        this.data = data;
    }

    public static <T> APIDataResponse<T> of(T data) {
        return new APIDataResponse<>(data);
    }

    public static <T> APIDataResponse<T> empty() {
        return new APIDataResponse<>(null);
    }
}
