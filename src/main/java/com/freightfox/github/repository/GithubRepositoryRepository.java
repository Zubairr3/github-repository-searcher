package com.freightfox.github.repository;

import com.freightfox.github.model.GithubRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GithubRepositoryRepository
        extends JpaRepository<GithubRepository, Long>, JpaSpecificationExecutor<GithubRepository> {
}
