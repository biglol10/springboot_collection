package com.alibou.booknetwork.book;

import org.springframework.data.jpa.domain.Specification;

public class BookSpecification {
    public static Specification<Book> withOwnerId(Integer ownerId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("owner").get("id"), ownerId);

        // criteriaBuilder will point to the Book. Get the owner field from the Book and get the id field from the owner. Then, compare the owner's id with the ownerId.
    }
}

// The BookSpecification class in your project is used to create a specification for querying books based on certain criteria.
// In this case, it's used to create a specification for querying books with a specific owner ID.
// Specifications in Spring Data JPA are used to create dynamic queries based on criteria.
// They are typically used when you need to create complex queries that are not easily achievable with standard query methods.
// In your BookSpecification class, you have a static method withOwnerId(Integer ownerId).
// This method returns a Specification<Book> that matches all books where the owner's ID is equal to the provided ownerId

// root.get("owner").get("id") is used to navigate to the owner's ID attribute of the book entity.
// criteriaBuilder.equal(root.get("owner").get("id"), ownerId) creates a predicate that checks if the owner's ID is equal to the provided ownerId.
// The lambda function (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("owner").get("id"), ownerId) is the actual specification. It's a function that takes a Root, CriteriaQuery, and CriteriaBuilder and returns a Predicate.