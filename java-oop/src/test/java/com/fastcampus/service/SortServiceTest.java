package com.fastcampus.service;

import com.fastcampus.logic.JavaSort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SortServiceTest {
    private SortService sortService = new SortService(new JavaSort<String>());

    @Test
    void test() {
        // given


        // when
        List<String> strings = sortService.doSort(List.of("3", "2", "1"));

        // then
        assertEquals(List.of("1","2","3"), strings);
    }
}