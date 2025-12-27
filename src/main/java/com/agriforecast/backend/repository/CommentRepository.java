package com.agriforecast.backend.repository;

import com.agriforecast.backend.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    // 게시글별 댓글 조회 (생성일시 오름차순)
    List<Comment> findByPostIdOrderByCreatedAtAsc(Long postId);
    
    // 사용자별 댓글 조회 (MemberUser의 seqNoA010 사용)
    @Query("SELECT c FROM Comment c WHERE c.user.seqNoA010 = :userId ORDER BY c.createdAt DESC")
    List<Comment> findByUserIdOrderByCreatedAtDesc(@Param("userId") Integer userId);
    
    // 게시글별 댓글 개수
    long countByPostId(Long postId);
}

