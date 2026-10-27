package com.example.lending.ops.part6.outreach;

import com.example.lending.ops.part6.common.OpsText;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/** Reads and updates the contact details and channel opt-ins used by collections outreach. */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContactPreferenceService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9 ()-]{7,20}$");
    private static final Set<String> LANGUAGES = Set.of("en", "cy", "pl", "ro");

    private final JdbcTemplate jdbcTemplate;

    public Optional<CustomerContact> find(Long customerId) {
        return jdbcTemplate.query("SELECT customer_id, full_name, email, phone, date_of_birth, email_opt_in, "
                                + "sms_opt_in, preferred_language FROM customer_contact WHERE customer_id = ?",
                        new BeanPropertyRowMapper<>(CustomerContact.class), customerId)
                .stream().findFirst();
    }

    public List<CustomerContact> findByIds(List<Long> customerIds) {
        if (customerIds.isEmpty()) {
            return List.of();
        }
        String placeholders = String.join(",", customerIds.stream().map(id -> "?").toList());
        return jdbcTemplate.query("SELECT customer_id, full_name, email, phone, date_of_birth, email_opt_in, "
                        + "sms_opt_in, preferred_language FROM customer_contact WHERE customer_id IN (" + placeholders + ")",
                new BeanPropertyRowMapper<>(CustomerContact.class), customerIds.toArray());
    }

    public void update(Long customerId, CustomerContact record) {
        List<String> errors = new ArrayList<>();
        String fullName = record.getFullName() == null ? "" : record.getFullName().trim();
        if (fullName.length() < 2 || fullName.length() > 120) {
            errors.add("fullName must be between 2 and 120 characters");
        }
        String email = record.getEmail() == null ? "" : record.getEmail().trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            errors.add("email is not a valid address");
        }
        String phone = record.getPhone() == null ? "" : record.getPhone().trim();
        if (!PHONE.matcher(phone).matches()) {
            errors.add("phone must contain 7 to 20 digits");
        }
        LocalDate dob = record.getDateOfBirth();
        if (dob == null || Period.between(dob, LocalDate.now()).getYears() < 18) {
            errors.add("customer must be at least 18 years old");
        }
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }
        String language = OpsText.orDefault(record.getPreferredLanguage(), "en");
        if (!LANGUAGES.contains(language)) {
            throw new IllegalArgumentException("unsupported preferredLanguage");
        }

        int updated = jdbcTemplate.update("UPDATE customer_contact SET full_name = ?, email = ?, phone = ?, "
                        + "date_of_birth = ?, email_opt_in = ?, sms_opt_in = ?, preferred_language = ? "
                        + "WHERE customer_id = ?",
                fullName, email, phone, java.sql.Date.valueOf(dob), record.isEmailOptIn(), record.isSmsOptIn(),
                language, customerId);
        if (updated == 0) {
            throw new IllegalArgumentException("customer " + customerId + " has no contact record");
        }
        log.info("contact preferences updated for customer {}", customerId);
    }

    public void optOutOfSms(Long customerId) {
        jdbcTemplate.update("UPDATE customer_contact SET sms_opt_in = FALSE WHERE customer_id = ?", customerId);
        log.info("customer {} opted out of sms", customerId);
    }
}
