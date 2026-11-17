package com.hrd.records.incentive;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IncentiveSchemeRepository extends JpaRepository<IncentiveScheme, Long> {

    boolean existsByName(String name);

    List<IncentiveScheme> findAllByOrderByNameAsc();
}
