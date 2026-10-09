package com.example.lending.loan.servicing.collections.activity;

import com.example.lending.loan.servicing.collections.debtors.Debtor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Activity timeline queries for a debtor: field visits plus contact attempts made outside a visit. */
@Repository
@Transactional(readOnly = true)
public class CollectionActivityDao {

    private final EntityManager entityManager;

    public CollectionActivityDao(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Integer getVisitsAndContactAttemptsCount(Debtor debtor, boolean includeVoided, String query) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> visitQuery = cb.createQuery(Long.class);
        Root<FieldVisit> visitRoot = visitQuery.from(FieldVisit.class);

        visitQuery.select(cb.count(visitRoot));

        List<Predicate> visitPredicates = createVisitsByDebtorPredicates(cb, visitRoot, debtor, includeVoided, query);
        visitQuery.where(visitPredicates.toArray(new Predicate[] {}));

        Long visitCount = entityManager.createQuery(visitQuery).getSingleResult();

        CriteriaQuery<Long> attemptQuery = cb.createQuery(Long.class);
        Root<ContactAttempt> attemptRoot = attemptQuery.from(ContactAttempt.class);
        attemptQuery.select(cb.count(attemptRoot));

        List<Predicate> attemptPredicates = createContactAttemptsByDebtorPredicates(cb, attemptRoot, debtor, includeVoided,
            query);
        attemptQuery.where(attemptPredicates.toArray(new Predicate[] {}));

        Long attemptCount = entityManager.createQuery(attemptQuery).getSingleResult();

        return visitCount.intValue() + attemptCount.intValue();
    }

    private List<Predicate> createVisitsByDebtorPredicates(CriteriaBuilder cb, Root<FieldVisit> root, Debtor debtor,
                                                           boolean includeVoided, String query) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("debtorIdentifier"), debtor.getIdentifier()));
        if (!includeVoided) {
            predicates.add(cb.isFalse(root.get("voided")));
        }
        if (query != null && !query.isBlank()) {
            String pattern = likePattern(query);
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("outcome")), pattern, '\\'),
                    cb.like(cb.lower(root.get("notes")), pattern, '\\')));
        }
        return predicates;
    }

    private List<Predicate> createContactAttemptsByDebtorPredicates(CriteriaBuilder cb, Root<ContactAttempt> root,
                                                                    Debtor debtor, boolean includeVoided, String query) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("debtorIdentifier"), debtor.getIdentifier()));
        predicates.add(cb.isNull(root.get("fieldVisitId")));
        if (!includeVoided) {
            predicates.add(cb.isFalse(root.get("voided")));
        }
        if (query != null && !query.isBlank()) {
            String pattern = likePattern(query);
            predicates.add(cb.or(
                    cb.like(cb.lower(root.get("channel")), pattern, '\\'),
                    cb.like(cb.lower(root.get("outcome")), pattern, '\\')));
        }
        return predicates;
    }

    private static String likePattern(String query) {
        String escaped = query.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
