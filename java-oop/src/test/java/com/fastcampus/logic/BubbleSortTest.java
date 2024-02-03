package com.fastcampus.logic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BubbleSortTest { // Junit5는 AccessModifier, 즉 public private를 쓰지 않음 (junit4와 다름)
    @DisplayName("버블소트")
    @Test
    void given_list_whenExecuting_thenReturnSortedList() {
        // given
        BubbleSort<Integer> bubbleSort = new BubbleSort<>();

        // when
        List<Integer> sorted = bubbleSort.sort(List.of(3, 2, 4, 1));// Java 10

        // then
        assertEquals(List.of(1,2,3,4), sorted);
    }
}