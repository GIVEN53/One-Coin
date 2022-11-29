package OneCoin.Server.config.auth.utils;

import OneCoin.Server.user.entity.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 권한 정보 생성, 저장
 */
@Service
public class CustomAuthorityUtils {
    @Value("${mail.address.admin}")
    private String adminMailAddress;

    private final List<GrantedAuthority> ADMIN_ROLES = AuthorityUtils.createAuthorityList("ROLE_ADMIN", "ROLE_USER", "ROLE_NOT_AUTH");
    private final List<GrantedAuthority> USER_ROLES = AuthorityUtils.createAuthorityList("ROLE_NOT_AUTH", "ROLE_USER");
    private final List<GrantedAuthority> NOT_AUTH_ROLES = AuthorityUtils.createAuthorityList("ROLE_NOT_AUTH");
    // List 지정 방식은 일단 보류
//    private final List<String> ADMIN_ROLES_STRING = List.of("ADMIN", "USER");
//    private final List<String> USER_ROLES_STRING = List.of("USER");

    // 메모리 상의 Role 을 기반으로 권한 정보 생성.
    public List<GrantedAuthority> createAuthorities(String email) {
        if (email.equals(adminMailAddress)) {
            return ADMIN_ROLES;
        }
        return NOT_AUTH_ROLES;
    }

    // DB에 저장된 Role 을 기반으로 권한 정보 생성
    public List<GrantedAuthority> createAuthorities(Role role) {
        if (role.equals(Role.ROLE_ADMIN)) {
            return ADMIN_ROLES;
        }
        else if (role.equals(Role.ROLE_USER)) {
            return USER_ROLES;
        }
        else return NOT_AUTH_ROLES;
    }

//    public List<GrantedAuthority> createAuthorities(List<String> roles) {
//        List<GrantedAuthority> authorities = roles.stream()
//                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
//                .collect(Collectors.toList());
//        return authorities;
//    }

    // DB 저장 용
    public Role createRoles(String email) {
        if (email.equals(adminMailAddress)) {
            return Role.ROLE_ADMIN;
        }
        return Role.ROLE_NOT_AUTH;
    }

//    public List<String> createRoles(String email) {
//        if (email.equals(adminMailAddress)) {
//            return ADMIN_ROLES_STRING;
//        }
//        return USER_ROLES_STRING;
//    }
}