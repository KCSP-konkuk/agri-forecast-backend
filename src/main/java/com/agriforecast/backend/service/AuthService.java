package com.agriforecast.backend.service;

import com.agriforecast.backend.dto.LoginRequest;
import com.agriforecast.backend.dto.LoginResponse;
import com.agriforecast.backend.entity.AuthPassword;
import com.agriforecast.backend.entity.MemberProfile;
import com.agriforecast.backend.entity.MemberUser;
import com.agriforecast.backend.repository.AuthPasswordRepository;
import com.agriforecast.backend.repository.MemberUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class AuthService {
    
    @Autowired
    private MemberUserRepository memberUserRepository;
    
    @Autowired
    private AuthPasswordRepository authPasswordRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public LoginResponse login(LoginRequest request) {
        // 1. 사용자 찾기
        Optional<MemberUser> userOpt = memberUserRepository.findById(request.getUsername());
        
        if (userOpt.isEmpty()) {
            return new LoginResponse(false, "아이디 또는 비밀번호가 올바르지 않습니다.", null);
        }
        
        MemberUser user = userOpt.get();
        
        // 2. 활성 여부 확인
        if (!user.getIsActive()) {
            return new LoginResponse(false, "비활성화된 계정입니다.", null);
        }
        
        // 3. 비밀번호 확인
        Optional<AuthPassword> authPasswordOpt = authPasswordRepository.findByMemberUser(user);
        
        if (authPasswordOpt.isEmpty()) {
            return new LoginResponse(false, "비밀번호 정보를 찾을 수 없습니다.", null);
        }
        
        AuthPassword authPassword = authPasswordOpt.get();
        
        // 4. 비밀번호 검증
        // BCrypt로 해시된 비밀번호와 비교
        String storedPassword = authPassword.getPassword();
        
        // BCrypt를 사용한 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), storedPassword)) {
            return new LoginResponse(false, "아이디 또는 비밀번호가 올바르지 않습니다.", null);
        }
        
        // 5. 프로필 정보 가져오기
        MemberProfile profile = user.getMemberProfile();
        
        // 6. 응답 생성
        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo();
        userInfo.setSeqNoA010(user.getSeqNoA010());
        userInfo.setId(user.getId());
        userInfo.setName(profile != null ? profile.getName() : "");
        userInfo.setEmail(profile != null ? profile.getEmail() : "");
        
        return new LoginResponse(true, "로그인 성공", userInfo);
    }
}

