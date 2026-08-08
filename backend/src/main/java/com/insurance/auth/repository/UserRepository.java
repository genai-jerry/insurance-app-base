package com.insurance.auth.repository;

import com.insurance.common.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByResetToken(String resetToken);

    /**
     * Case-insensitive "contains" match on the email address, ordered by id so the
     * filtered and unfiltered listings agree on ordering. Derived query: Spring Data
     * binds the term as a JDBC parameter and escapes {@code %}/{@code _}, so no term
     * can alter the query's meaning and wildcards are matched literally.
     */
    List<User> findByEmailContainingIgnoreCaseOrderByIdAsc(String email);
}
