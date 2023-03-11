package OneCoin.Server.helper;

import OneCoin.Server.user.entity.Role;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @AuthenticationPrincipal annotation이 붙어있는 controller는 SecurityContext에서 사용자 정보를 찾아야 한다.
 * @WithMockUser annotation은 커스텀한 UserDetails를 반환하지 않는다.
 * @WithUserDetails로 SecurityContext에 UserDetails를 등록할 수 있지만 등록할 사용자도 DB에 저장되어야 하는 단점이 있다.
 * 따라서 @WithSecurityContext annotation을 사용하여 직접 UserDetails를 등록한 새로운 SecurityContext를 생성한다.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
  Role role();
}
