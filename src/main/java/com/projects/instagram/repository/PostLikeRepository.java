package com.projects.instagram.repository;

import com.projects.instagram.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    void deleteByPostIdAndUserId(Long postId, Long userId);

    @Query("SELECT l.postId FROM PostLike l WHERE l.userId = :userId AND l.postId IN :postIds")
    List<Long> findPostIdsLikedByUser(@Param("userId") Long userId, @Param("postIds") List<Long> postIds);

    @Query("SELECT l.postId, COUNT(l) FROM PostLike l WHERE l.postId IN :postIds GROUP BY l.postId")
    List<Object[]> findCountsByPostIds(@Param("postIds") List<Long> postIds);


    default Map<Long, Long> countLikesByPostIds(List<Long> postIds) {
        List<Object[]> rows = findCountsByPostIds(postIds);
        Map<Long, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            Long postId = (Long) row[0];
            Long cnt = (Long) row[1];
            map.put(postId, cnt);
        }
        return map;
    }

    void deleteByPostId(Long postId);
}
