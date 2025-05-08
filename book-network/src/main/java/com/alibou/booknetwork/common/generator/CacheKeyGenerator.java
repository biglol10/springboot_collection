package com.alibou.booknetwork.common.generator;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class CacheKeyGenerator {
    /**
     * DTO 객체와 필드 이름 목록을 기반으로 캐시 키를 생성
     * 
     * @param dto DTO 객체
     * @param fieldNames 캐시 키에 포함할 필드 이름 (쉼표로 구분된 문자열)
     * @return 생성된 캐시 키
     */
    public static String createKeyFromDto(Object dto, String fieldNames) {
        if (dto == null) {
            return "null";
        }

        // 필드 이름을 &로 분리
        String[] fields = fieldNames.split("&");

        // 각 필드 값을 추출하여 키 생성
        return Arrays.stream(fields)
            .map(fieldName -> extractFieldValue(dto, fieldName))
            .collect(Collectors.joining(":"));
    }

    /**
     * 리플렉션을 사용하여 객체에서 필드 값 추출
     */
    private static String extractFieldValue(Object dto, String fieldName) {
        try {
            // 클래스에서 필드 찾기
            Field field = findField(dto.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(dto);
                return fieldName + "-" + (value != null ? value.toString() : "null");
            }
        } catch (Exception e) {
            // 예외 발생 시 로깅 처리
            System.err.println("Error extracting field: " + fieldName + " from " + dto.getClass().getName());
        }
        return fieldName + "-unknown";
    }

    /**
     * 클래스와 상위 클래스에서 필드 찾기 (상속 구조 지원)
     */
    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> currentClass = clazz;
        if (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                return findField(currentClass.getSuperclass(), fieldName);
            }
        }
        return null;
    }
}
