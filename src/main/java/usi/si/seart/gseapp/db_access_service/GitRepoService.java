package usi.si.seart.gseapp.db_access_service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import usi.si.seart.gseapp.model.GitRepo;
import usi.si.seart.gseapp.model.GitRepoLabel;
import usi.si.seart.gseapp.model.GitRepoLanguage;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface GitRepoService {
    Optional<GitRepo> getRepoById(Long id);
    Optional<GitRepo> getByName(String name);
    List<GitRepo> findDynamically(Map<String, Object> parameters);
    Page<GitRepo> findDynamically(Map<String, Object> parameters, Pageable pageable);
    GitRepo createOrUpdateRepo(GitRepo repo);
    List<String> getAllLabels(Integer limit);
    List<String> getAllLanguages();
    List<String> getAllLicenses();
    List<String> getAllRepoNames();
    Map<String, Long> getAllLanguageStatistics();
    Map<String, Long> getMainLanguageStatistics();
    void createUpdateLabels(GitRepo repo, List<GitRepoLabel> labels);
    void createUpdateLanguages(GitRepo repo, List<GitRepoLanguage> languages);
}
