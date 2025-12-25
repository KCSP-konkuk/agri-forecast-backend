package com.agriforecast.backend.repository;

import com.agriforecast.backend.entity.MemberUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MemberUserRepository extends JpaRepository<MemberUser, Integer> {
    
    // 아이디로 사용자 찾기
    Optional<MemberUser> findById(String id);
    
    // 아이디 존재 여부 확인
    boolean existsById(String id);
}

