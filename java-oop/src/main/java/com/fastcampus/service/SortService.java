package com.fastcampus.service;

import com.fastcampus.logic.JavaSort;
import com.fastcampus.logic.Sort;

import java.util.List;

public class SortService {
    private final Sort<String> sort;  // 정렬 인터페이스를 가지고 있긴 하지만 구체적으로 어떤 구현체를 이용해서 정렬할 것인는 이제 이 코드는 알지못함

    public SortService(Sort<String> sort) {
        this.sort = sort;
        System.out.println("구현체: " + sort.getClass().getName());
    }

    public List<String> doSort(List<String> list) {
//        Sort<String> sort = new JavaSort<>(); // Javasort에서 BubbleSort로 바꾸고 싶을 땐 기존에는 간결합 되어있을 때는 여기에서 코드를 바꿨어야 했는데
        // 이제는 코드는 그대로 있고 이 서비스를 사용하는 곳에서 다른 구현체를 주입. 이게 의존성 주입

        return sort.sort(list);
    }
}
