package OneCoin.Server.helper;

import OneCoin.Server.user.entity.Role;
import OneCoin.Server.user.entity.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WithMockCustomUserSecurityContextFactory implements WithSecurityContextFactory<WithMockCustomUser> {
  @Override
  public SecurityContext createSecurityContext(WithMockCustomUser customUser) {
    Role role = customUser.role();
    User user = StubData.MockUser.getMockEntity();
    List<GrantedAuthority> authorities = AuthorityUtils.createAuthorityList(role.name());
    Map<String, Object> claims = new HashMap<>();
    claims.put("id", user.getUserId());
    claims.put("username", user.getEmail());
    claims.put("roles", user.getUserRole());
    claims.put("displayName", user.getDisplayName());
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(claims, "password", authorities);
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(token);
    return context;
  }
}
