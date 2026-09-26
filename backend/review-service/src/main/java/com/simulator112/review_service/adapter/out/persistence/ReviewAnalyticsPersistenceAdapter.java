package com.simulator112.review_service.adapter.out.persistence;

import com.simulator112.review_service.adapter.out.persistence.entity.CriterionResultJpaEntity;
import com.simulator112.review_service.adapter.out.persistence.entity.ReviewJpaEntity;
import com.simulator112.review_service.adapter.out.persistence.repository.SpringDataReviewRepository;
import com.simulator112.review_service.application.port.out.ReviewAnalyticsStore;
import com.simulator112.review_service.domain.model.ReviewAnalytics;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReviewAnalyticsPersistenceAdapter implements ReviewAnalyticsStore {
    private final SpringDataReviewRepository repository;
    private final EntityManager entityManager;

    @Override
    public ReviewAnalytics analyze(ReviewAnalytics.Query query, int page, int size) {
        var reviewPage = repository.findAll(specification(query),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        var items = reviewPage.getContent().stream().map(ReviewPersistenceAdapter::toDomain).toList();
        return new ReviewAnalytics(summary(query), trend(query), criteria(query), heatmap(query), scenarios(query),
                new ReviewAnalytics.Page(items, page, size, reviewPage.getTotalElements(),
                        reviewPage.getTotalPages()));
    }

    private Specification<ReviewJpaEntity> specification(ReviewAnalytics.Query filter) {
        return (root, query, builder) -> predicate(filter, root, query, builder);
    }

    private ReviewAnalytics.Summary summary(ReviewAnalytics.Query filter) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = builder.createQuery(Object[].class);
        Root<ReviewJpaEntity> review = query.from(ReviewJpaEntity.class);
        Expression<Integer> score = builder.coalesce(review.get("finalScore"), review.get("automaticScore"));
        Expression<Integer> denominator = builder.nullif(review.get("maxScore"), 0);
        Expression<Number> percent = builder.prod(builder.quot(score.as(Double.class), denominator.as(Double.class)), 100.0);
        Expression<Long> overtime = builder.<Long>selectCase()
                .when(builder.greaterThan(review.get("overtimeSeconds"), 0L), 1L).otherwise(0L);
        query.multiselect(builder.countDistinct(review.get("contextId")),
                builder.countDistinct(review.get("userId")), builder.avg(percent),
                builder.avg(review.<Long>get("durationSeconds")), builder.sum(overtime));
        query.where(predicate(filter, review, query, builder));
        Object[] row = entityManager.createQuery(query).getSingleResult();
        long attempts = number(row[0]).longValue();
        return new ReviewAnalytics.Summary(attempts, number(row[1]).longValue(),
                row[2] == null ? null : (int) Math.round(number(row[2]).doubleValue()),
                row[3] == null ? null : Math.round(number(row[3]).doubleValue()),
                row[4] == null ? 0 : number(row[4]).longValue());
    }

    private List<ReviewAnalytics.TrendPoint> trend(ReviewAnalytics.Query filter) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = builder.createQuery(Object[].class);
        Root<ReviewJpaEntity> review = query.from(ReviewJpaEntity.class);
        Expression<LocalDate> date = builder.function("date", LocalDate.class, review.get("createdAt"));
        Expression<Integer> score = builder.coalesce(review.get("finalScore"), review.get("automaticScore"));
        Expression<Integer> denominator = builder.nullif(review.get("maxScore"), 0);
        Expression<Number> percent = builder.prod(builder.quot(score.as(Double.class), denominator.as(Double.class)), 100.0);
        query.multiselect(date, builder.avg(percent));
        query.where(predicate(filter, review, query, builder));
        query.groupBy(date);
        query.orderBy(builder.asc(date));
        return entityManager.createQuery(query).getResultList().stream()
                .map(row -> new ReviewAnalytics.TrendPoint((LocalDate) row[0],
                        (int) Math.round(number(row[1]).doubleValue())))
                .toList();
    }

    private List<ReviewAnalytics.CriterionStat> criteria(ReviewAnalytics.Query filter) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = builder.createQuery(Object[].class);
        Root<ReviewJpaEntity> review = query.from(ReviewJpaEntity.class);
        Join<ReviewJpaEntity, CriterionResultJpaEntity> criterion = review.join("results");
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(predicate(filter, review, query, builder));
        if (filter.incidentId() != null) predicates.add(builder.equal(criterion.get("incidentId"), filter.incidentId()));
        if (filter.criterion() != null) predicates.add(builder.equal(criterion.get("criterionName"), filter.criterion()));
        query.multiselect(criterion.get("criterionName"), builder.count(criterion.get("id")),
                builder.sum(criterion.<Integer>get("score")), builder.sum(criterion.<Integer>get("maxScore")));
        query.where(predicates.toArray(Predicate[]::new));
        query.groupBy(criterion.get("criterionName"));
        return entityManager.createQuery(query).getResultList().stream()
                .map(row -> new ReviewAnalytics.CriterionStat((String) row[0], number(row[1]).longValue(),
                        number(row[2]).longValue(), number(row[3]).longValue()))
                .sorted(Comparator.comparingDouble(this::errorRate).reversed())
                .toList();
    }

    private List<ReviewAnalytics.ScenarioStat> scenarios(ReviewAnalytics.Query filter) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = builder.createQuery(Object[].class);
        Root<ReviewJpaEntity> review = query.from(ReviewJpaEntity.class);
        Join<ReviewJpaEntity, CriterionResultJpaEntity> criterion = review.join("results");
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(predicate(filter, review, query, builder));
        if (filter.incidentId() != null) predicates.add(builder.equal(criterion.get("incidentId"), filter.incidentId()));
        if (filter.criterion() != null) predicates.add(builder.equal(criterion.get("criterionName"), filter.criterion()));
        query.multiselect(criterion.get("incidentId"), criterion.get("incidentOrder"),
                builder.countDistinct(review.get("contextId")), builder.sum(criterion.<Integer>get("score")),
                builder.sum(criterion.<Integer>get("maxScore")));
        query.where(predicates.toArray(Predicate[]::new));
        query.groupBy(criterion.get("incidentId"), criterion.get("incidentOrder"));
        query.orderBy(builder.asc(criterion.get("incidentOrder")));
        return entityManager.createQuery(query).getResultList().stream()
                .map(row -> new ReviewAnalytics.ScenarioStat((String) row[0], number(row[1]).intValue(),
                        number(row[2]).longValue(), number(row[3]).longValue(), number(row[4]).longValue()))
                .toList();
    }

    private List<ReviewAnalytics.StudentCriterionStat> heatmap(ReviewAnalytics.Query filter) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Object[]> query = builder.createQuery(Object[].class);
        Root<ReviewJpaEntity> review = query.from(ReviewJpaEntity.class);
        Join<ReviewJpaEntity, CriterionResultJpaEntity> criterion = review.join("results");
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(predicate(filter, review, query, builder));
        if (filter.incidentId() != null) predicates.add(builder.equal(criterion.get("incidentId"), filter.incidentId()));
        if (filter.criterion() != null) predicates.add(builder.equal(criterion.get("criterionName"), filter.criterion()));
        query.multiselect(review.get("userId"), criterion.get("criterionName"),
                builder.sum(criterion.<Integer>get("score")), builder.sum(criterion.<Integer>get("maxScore")));
        query.where(predicates.toArray(Predicate[]::new));
        query.groupBy(review.get("userId"), criterion.get("criterionName"));
        return entityManager.createQuery(query).getResultList().stream()
                .map(row -> new ReviewAnalytics.StudentCriterionStat((java.util.UUID) row[0], (String) row[1],
                        number(row[2]).longValue(), number(row[3]).longValue()))
                .toList();
    }

    private Predicate predicate(ReviewAnalytics.Query filter, Root<ReviewJpaEntity> review,
                                CriteriaQuery<?> query, CriteriaBuilder builder) {
        List<Predicate> predicates = new ArrayList<>();
        Predicate[] access = filter.accessScopes().stream()
                .map(scope -> builder.and(review.get("userId").in(scope.studentIds()),
                        review.get("assignmentId").in(scope.assignmentIds())))
                .toArray(Predicate[]::new);
        predicates.add(access.length == 0 ? builder.disjunction() : builder.or(access));
        if (filter.from() != null) predicates.add(builder.greaterThanOrEqualTo(review.get("createdAt"), filter.from()));
        if (filter.to() != null) predicates.add(builder.lessThan(review.get("createdAt"), filter.to()));
        if (filter.incidentId() != null || filter.criterion() != null) {
            Subquery<Integer> exists = query.subquery(Integer.class);
            Root<CriterionResultJpaEntity> criterion = exists.from(CriterionResultJpaEntity.class);
            List<Predicate> criterionPredicates = new ArrayList<>();
            criterionPredicates.add(builder.equal(criterion.get("review"), review));
            if (filter.incidentId() != null) criterionPredicates.add(builder.equal(criterion.get("incidentId"), filter.incidentId()));
            if (filter.criterion() != null) criterionPredicates.add(builder.equal(criterion.get("criterionName"), filter.criterion()));
            exists.select(builder.literal(1)).where(criterionPredicates.toArray(Predicate[]::new));
            predicates.add(builder.exists(exists));
        }
        return builder.and(predicates.toArray(Predicate[]::new));
    }

    private double errorRate(ReviewAnalytics.CriterionStat stat) {
        return stat.maxScore() == 0 ? 0 : (double) (stat.maxScore() - stat.score()) / stat.maxScore();
    }

    private Number number(Object value) {
        return value == null ? 0 : (Number) value;
    }
}
