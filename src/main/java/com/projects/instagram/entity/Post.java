package com.projects.instagram.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "posts")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // The user id that owns this post:
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // "POST" or "REEL"
    @Column(name = "type", nullable = false, length = 20)
    private String type;

    @Column(name = "caption", length = 2000)
    private String caption;

    @Column(name = "content_type", length = 200)
    private String contentType;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // stored filename (not full url). We'll build the public URL in controller
    @Column(name = "file_url")
    private String fileUrl;

//    @ManyToOne
//    @JoinColumn(name = "user_id", nullable = false)
//    private User user;


    // convenience constructor
    public Post(Long userId, String type, String caption, String fileUrl, String contentType) {
        this.userId = userId;
        this.type = type;
        this.caption = caption;
        this.fileUrl = fileUrl;
        this.contentType = contentType;
        this.createdAt = OffsetDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Post)) return false;
        Post other = (Post) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
