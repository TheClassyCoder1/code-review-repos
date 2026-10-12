package com.example.lending.loan.servicing.statements.portal;

import com.example.lending.loan.servicing.common.Digests;
import org.springframework.stereotype.Service;

/** Registration of statement portal users. */
@Service
public class PortalUserService {

    private final PortalUserRepository userDAO;

    public PortalUserService(PortalUserRepository userDAO) {
        this.userDAO = userDAO;
    }

    public void createUser(String username, String email, String password) {
        PortalUser user = new PortalUser();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(Digests.sha256Hex(password));
        userDAO.createUser(user);
    }
}
