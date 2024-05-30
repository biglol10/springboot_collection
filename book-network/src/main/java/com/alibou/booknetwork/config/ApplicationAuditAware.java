package com.alibou.booknetwork.config;

import com.alibou.booknetwork.user.User;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

// since spring does not know created_by and updated_by fields, we need to tell spring to use this class to get the current user
public class ApplicationAuditAware implements AuditorAware<Integer> { // type of the user id is Integer

    @Override
    public Optional<Integer> getCurrentAuditor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // when we implemented the security(jwt), we know we updated the security context holder with the current user
        // In a Spring Security application, the SecurityContextHolder is typically updated with the current user's authentication information during the authentication process. This is usually done in an authentication filter or an authentication provider.
        // In your case, since you're using JWT for authentication, this is likely done in a JWT filter. The filter would extract the JWT from the incoming request, validate it, load the user details associated with the JWT, and then update the SecurityContextHolder.

        if (authentication == null || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) { // if the user is not authenticated or the user is anonymous, we will return empty
            return Optional.empty();
        }

        User userPrincipal = (User) authentication.getPrincipal(); // we will get the user from the principal

        return Optional.ofNullable(userPrincipal.getId()); // we will return the user id
    }
}
