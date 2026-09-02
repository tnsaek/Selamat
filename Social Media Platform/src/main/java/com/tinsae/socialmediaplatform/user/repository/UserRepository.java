package com.tinsae.socialmediaplatform.user.repository;

import com.tinsae.socialmediaplatform.user.entity.User;
import com.tinsae.socialmediaplatform.common.enums.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    @Query("""
            select u
            from User u
            left join fetch u.profile p
            where u.status = :status
              and (
                  lower(u.username) like lower(concat('%', :query, '%'))
                  or lower(u.email) like lower(concat('%', :query, '%'))
                  or lower(p.displayName) like lower(concat('%', :query, '%'))
              )
            order by u.username asc
            """)
    List<User> searchUsers(@Param("query") String query, @Param("status") UserStatus status, Pageable pageable);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
