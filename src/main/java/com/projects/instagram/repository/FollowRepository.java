//package com.projects.instagram.repository;
//
//import com.projects.instagram.entity.Follow;
//import com.projects.instagram.entity.Post;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//
//import java.util.List;
//import java.util.Optional;
//
//public interface FollowRepository extends JpaRepository<Follow, Long> {
//
//    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
//
//    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
//
//    List<Follow> findByFollowerId(Long followerId);
//
//    Page<Post> findByFollowerIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
//
//    List<Follow> findByFollowingId(Long followingId);
//
//    List<Follow> findByFollowerIdIn(List<Long> followerIds);
//
//    long countByFollowingId(Long userId); // followers count for a user
//
//    long countByFollowerId(Long userId);  // following count for a user
//}


// -----------------------------------------------------------------------------
// File: FollowRepository.java
// -----------------------------------------------------------------------------
package com.projects.instagram.repository;


import com.projects.instagram.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;
import java.util.Set;


@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {

    Optional<Follow> findByFollowerIdAndFollowingId(Long followerId, Long followingId);

    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);

    List<Follow> findByFollowingId(Long id);

    List<Follow> findByFollowerId(Long id);

    List<Follow> findByFollowerIdIn(List<Long> ids);

    long countByFollowingId(Long id);

    long countByFollowerId(Long id);

    // fetch-join variants to avoid LazyInitializationException when mapping outside
    @Query("SELECT f FROM Follow f JOIN FETCH f.follower JOIN FETCH f.following WHERE f.following.id = :id")
    List<Follow> findByFollowingIdFetchUsers(@Param("id") Long id);

//    @Query("SELECT f FROM Follow f JOIN FETCH f.follower JOIN FETCH f.following WHERE f.follower.id = :id")
//    List<Follow> findByFollowerIdFetchUsers(@Param("id") Long id);
//

    // popularity: returns Object[] with [0]=followingId (Long), [1]=count (Long)
    @Query("SELECT f.following.id, COUNT(f) as cnt FROM Follow f GROUP BY f.following.id ORDER BY cnt DESC")
    List<Object[]> findPopularFollowingIds(Pageable pageable);

    boolean existsByFollowingId(Long id);
//
//        @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId")
//        List<Long> findFollowingIdsByFollowerId(@Param("userId") Long userId);
//
//        @Query("SELECT f.follower.id FROM Follow f WHERE f.following.id = :userId")
//        List<Long> findFollowerIdsByFollowingId(@Param("userId") Long userId);

//    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId")
//    Set<Long> findFollowingIdsByFollowerId(@Param("userId") Long userId);
//
//    @Query("SELECT f.follower.id FROM Follow f WHERE f.following.id = :userId")
//    Set<Long> findFollowerIdsByFollowingId(@Param("userId") Long userId);

    // IDs of users who follow the given user
    @Query("SELECT f.follower.id FROM Follow f WHERE f.following.id = :userId")
    List<Long> findFollowerIdsByUserId(Long userId);

    // IDs of users that the given user is following
    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId")
    List<Long> findFollowingIdsByUserId(Long userId);

    @EntityGraph(attributePaths = {"follower", "following"})
    Page<Follow> findByFollowerId(Long followerId, Pageable pageable);

    @EntityGraph(attributePaths = {"follower", "following"})
    Page<Follow> findByFollowingId(Long followingId, Pageable pageable);


    List<Follow> findAllByFollowingId(Long userId);
}