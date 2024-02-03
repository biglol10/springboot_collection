package com.fastcampus;

import com.fastcampus.logic.BubbleSort;
import com.fastcampus.logic.Sort;

import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        BubbleSort<String> sort = new BubbleSort<>(); // DIP 위반, 구현체를 인스턴스화해서 집어넣고 있음. 상황이 바꼈을 때 이 구현코드를 바꾸기 어렵고 테스트가 어려움
        // 메인 클래스가 BubbleSort에 간결합되어 있다고 말함

        Sort<String> sort2 = new BubbleSort<>(); // interface를 적용하므로써 변수 선언부를 바꾸지 않고 구현체만 바뀌는게 가능해짐 (ButtonSort대신 JavaSort써도 되는 상태)
        // 하지만 이걸로도 충분하지 않음. 메인 메소드는 정렬 알고리즘이 뭔지 알고있음.

        System.out.println("[result]: " + sort.sort(Arrays.asList(args)));

        System.out.println("Hello world!");
    }
}