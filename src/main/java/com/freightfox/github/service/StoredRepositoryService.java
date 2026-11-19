package com.freightfox.github.service;

import com.freightfox.github.api.BadRequestException;
import com.freightfox.github.dto.StoredRepositoriesResponse;
import com.freightfox.github.model.GithubRepository;
import com.freightfox.github.repository.GithubRepositoryRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Predicate;

@Service
public class StoredRepositoryService {
    private final GithubRepositoryRepository repository;

    public StoredRepositoryService(GithubRepositoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public StoredRepositoriesResponse find(String language, Long minStars, String sortValue) {
        if (minStars != null && minStars < 0) {
            throw new BadRequestException("minStars must be zero or greater");
        }
        SortOption sort = SortOption.parse(sortValue);
        Specification<GithubRepository> filters = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (language != null && !language.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("language")),
                        language.trim().toLowerCase(java.util.Locale.ROOT)));
            }
            if (minStars != null) {
                predicates.add(cb.ge(root.get("stars"), minStars));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        Sort order = Sort.by(Sort.Direction.DESC, sort.entityField())
                .and(Sort.by(Sort.Direction.ASC, "id"));
        var result = repository.findAll(filters, order).stream()
                .map(GithubRepository::toDto)
                .toList();
        return new StoredRepositoriesResponse(result);
    }
}
