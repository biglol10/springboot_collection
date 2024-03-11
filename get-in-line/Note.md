*********

JPA N+1 문제  
https://dev-coco.tistory.com/165  
1. 똑똑한 lazy
    1. 비즈니스 로직을 면밀히 분석하여, 불필요한 연관 관계 테이블 정보를 불러오는 부분을 제거
    2. 가장 똑똑하고 효율적인 방법
2. eager fetch + join jpql
    1. join 쿼리를 직젖ㅂ 작성하는 방법은 다양 (@Query, querydsl, ...)
    2. 쿼리 한번에 오긴 하겠지만 join쿼리 연산 비용과 네트워크로 전달되는 데이터가 클 수 있음
3. 후속 쿼리를 in으로 묶어주기: N + 1 -> 1 + 1로 I/O를 줄일 수 있음
    1. hibernate 프로퍼티: default_batch_fetch_size
    2. 스프링 부트에서 쓰는 법: spring.jpa.properties.hibernate.default_batch_fetch_size
    3. 100~1000 사이를 추천
    4. 모든 쿼리에 적용되고 복잡한 도메인에 join 쿼리를 구성하는 것이 골치아플 때 효율적

* eager fetch, lazy fetch, N + 1 문제 다시보기

*********


