package com.freightfox.github.service;

import com.freightfox.github.dto.GithubSearchPayload;
import com.freightfox.github.dto.RepositoryDto;
import com.freightfox.github.model.GithubRepository;
import com.freightfox.github.repository.GithubRepositoryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RepositoryPersistenceService {
    private final GithubRepositoryRepository repository;

    public RepositoryPersistenceService(GithubRepositoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public List<RepositoryDto> upsert(List<GithubSearchPayload.Item> items) {
        if (items.isEmpty()) return List.of();

        // One lookup and one saveAll call rather than a database query per result.
        Map<Long, GithubSearchPayload.Item> unique = new LinkedHashMap<>();
        for (GithubSearchPayload.Item item : items) unique.put(item.id(), item);

        Map<Long, GithubRepository> existing = repository.findAllById(unique.keySet())
                .stream().collect(Collectors.toMap(RepositoryPersistenceService::idOf, Function.identity()));
        List<GithubRepository> toSave = new ArrayList<>();
        for (GithubSearchPayload.Item item : unique.values()) {
            GithubRepository entity = existing.getOrDefault(item.id(), new GithubRepository(item.id()));
            entity.updateFrom(item);
            toSave.add(entity);
        }
        repository.saveAll(toSave);
        return toSave.stream().map(GithubRepository::toDto).toList();
    }

    private static Long idOf(GithubRepository entity) {
        return entity.toDto().id();
    }
}
