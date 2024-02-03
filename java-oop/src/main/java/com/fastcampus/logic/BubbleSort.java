package com.fastcampus.logic;

import java.util.ArrayList;
import java.util.List;

public class BubbleSort <T extends Comparable<T>> implements Sort<T> { // 2 타입이 어느쪽이 크고 작은지 비교하기 위한 클래스가 따로 있음. 대표적인게 String, Integer
    @Override
    public List<T> sort(List<T> list) {
        List<T> output = new ArrayList<>(list);

        for (int i = output.size() -1; i > 0; i--){
            for (int j = 0; j < i; j++) {
                if (output.get(j).compareTo(output.get(j + 1)) > 0) {
                    T temp = output.get(j);
                    output.set(j, output.get(j + 1));
                    output.set(j + 1, temp);
                }
            }
        }

        return output;
    }
}
