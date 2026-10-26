package com.example.lending.ops.part2.sandbox;

import lombok.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;
import java.lang.annotation.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** In-process fake of the partner's directory service for connector tests, seeded with generated fixture accounts. */
@Component
@Conditional(PartnerSandboxCondition.class)
@PartnerDirectoryStub.Path("users")
public class PartnerDirectoryStub {

    @Value
    public static class User { UUID key; String username, password; }

    @Value
    public static class UserMetadata { User user; boolean deleted; }

    private static final Map<UUID, UserMetadata> USERS = new ConcurrentHashMap<>();

    public PartnerDirectoryStub() {
        for (int i = 1; i <= 3; i++) {
            User user = new User(UUID.randomUUID(), "fixture-user-" + i, Long.toHexString(new SecureRandom().nextLong()));
            USERS.put(user.getKey(), new UserMetadata(user, false));
        }
    }

    @POST
    @Path("authenticate")
    @Produces({ MediaType.APPLICATION_JSON })
    public User authenticate(
            @QueryParam("username") final String username,
            @QueryParam("password") final String password) {

        Optional<User> user = USERS.values().stream().
                filter(meta -> !meta.isDeleted() && username.equals(meta.getUser().getUsername())).
                findFirst().map(UserMetadata::getUser);

        if (user.isEmpty()) {
            throw new NotFoundException(username);
        }

        return user.filter(u -> password.equals(u.getPassword())).
                orElseThrow(ForbiddenException::new);
    }

    // resource annotations of the partner's API, kept so the copied resource keeps its contract
    @Retention(RetentionPolicy.RUNTIME) @interface POST { }
    @Retention(RetentionPolicy.RUNTIME) @interface Path { String value(); }
    @Retention(RetentionPolicy.RUNTIME) @interface Produces { String[] value(); }
    @Retention(RetentionPolicy.RUNTIME) @interface QueryParam { String value(); }

    interface MediaType { String APPLICATION_JSON = "application/json"; }

    static class NotFoundException extends RuntimeException {
        NotFoundException(String message) { super(message); }
    }

    static class ForbiddenException extends RuntimeException { }
}
