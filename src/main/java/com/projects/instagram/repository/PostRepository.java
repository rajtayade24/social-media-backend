package com.projects.instagram.repository;

import com.projects.instagram.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;


public interface PostRepository extends JpaRepository<Post, Long> {
    //    @Query("""
//                select new com.projects.instagram.dto.PostDto(
//                    p.id,
//                    u.id,
//                    p.type,
//                    p.caption,
//                    p.contentType,
//                    p.createdAt,
//                    p.fileUrl,
//                    u.username,
//                    u.email,
//                    u.profilePhotoUrl,
//                            (select count(pl.id) from PostLike pl where pl.postId = p.id),
//                    (case when exists (
//                        select 1 from PostLike pl where pl.postId = p.id and pl.userId = :userId
//                    ) then true else false end)
//                )
//                from Post p
//                join User u on u.id = p.userId
//                order by p.createdAt desc
//            """)
    Page<Post> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Post> findByTypeOrderByCreatedAtDesc(String type);

//    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
                select p
                from Post p
                order by p.createdAt desc
            """)
    Page<Post> findAllPosts(Pageable pageable);

    List<Post> findByUserId(Long userId);

    Long countAllByUserId(Long userId);
}
