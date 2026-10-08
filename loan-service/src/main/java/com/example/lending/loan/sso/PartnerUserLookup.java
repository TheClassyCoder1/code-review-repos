package com.example.lending.loan.sso;

import java.sql.*;

public final class PartnerUserLookup {

    private PartnerUserLookup() {
    }

    public static Long getUserIdByName(String userNameOrClientId, Connection connection) throws SQLException {

        String select = "select user_id from lending.partner_users where client_id = ? or username = ?";
        PreparedStatement statement = connection.prepareStatement(select);
        statement.setString(1, userNameOrClientId);
        statement.setString(2, userNameOrClientId);
        Long userId = null;
        if (statement.execute()) {
            ResultSet results = statement.getResultSet();
            if (results.next()) {
                userId = results.getLong(1);
            }
        }
        return userId;
    }
}
