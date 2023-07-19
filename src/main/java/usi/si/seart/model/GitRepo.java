package usi.si.seart.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.Hibernate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "git_repo")
@Entity
public class GitRepo {

    @Id
    @GeneratedValue(
        strategy = GenerationType.SEQUENCE,
        generator = "hibernate_sequence"
    )
    @SequenceGenerator(
        name = "hibernate_sequence",
        allocationSize = 1
    )
    @Column(name = "id")
    Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "language_id")
    Language mainLanguage;

    @Column(name = "name")
    String name;

    @Column(name = "is_fork_project")
    Boolean isFork;

    @Column(name = "commits")
    Long commits;

    @Column(name = "branches")
    Long branches;

    @Column(name = "default_branch")
    String defaultBranch;

    @Column(name = "releases")
    Long releases;

    @Column(name = "contributors")
    Long contributors;

    @Column(name = "license")
    String license;

    @Column(name = "watchers")
    Long watchers;

    @Column(name = "stargazers")
    Long stargazers;

    @Column(name = "forks")
    Long forks;

    @Column(name = "size")
    Long size;

    @Column(name = "created_at")
    Date createdAt;

    @Column(name = "pushed_at")
    Date pushedAt;

    @Column(name = "updated_at")
    Date updatedAt;

    @Column(name = "homepage")
    String homepage;

    @Column(name = "total_issues")
    Long totalIssues;

    @Column(name = "open_issues")
    Long openIssues;

    @Column(name = "total_pull_requests")
    Long totalPullRequests;

    @Column(name = "open_pull_requests")
    Long openPullRequests;

    @Column(name = "last_commit")
    Date lastCommit;

    @Column(name = "last_commit_sha")
    String lastCommitSHA;

    @Column(name = "has_wiki")
    Boolean hasWiki;

    @Column(name = "archived")
    Boolean isArchived;

    @Column(name = "crawled")
    Date crawled;

    @Column(name = "cloned")
    Date cloned;

    @Builder.Default
    @ManyToMany(cascade = {
        CascadeType.PERSIST,
        CascadeType.MERGE,
        CascadeType.REFRESH,
        CascadeType.DETACH
    })
    @JoinTable(
        name = "git_repo_label",
        joinColumns = @JoinColumn(name = "repo_id"),
        inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    @Fetch(value = FetchMode.JOIN)
    Set<Label> labels = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "repo", cascade = {
        CascadeType.PERSIST,
        CascadeType.MERGE,
        CascadeType.REFRESH,
        CascadeType.DETACH
    })
    @Fetch(value = FetchMode.JOIN)
    Set<GitRepoLanguage> languages = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "repo", cascade = {
        CascadeType.PERSIST,
        CascadeType.MERGE,
        CascadeType.REFRESH,
        CascadeType.DETACH
    })
    @Fetch(value = FetchMode.JOIN)
    Set<GitRepoMetric> metrics = new HashSet<>();

    @PrimaryKeyJoinColumn
    @OneToOne(mappedBy = "repo")
    @Fetch(value = FetchMode.JOIN)
    GitRepoMetricAggregate totalMetrics;

    @Builder.Default
    @ManyToMany(cascade = {
        CascadeType.PERSIST,
        CascadeType.MERGE,
        CascadeType.REFRESH,
        CascadeType.DETACH
    })
    @JoinTable(
        name = "git_repo_topic",
        joinColumns = @JoinColumn(name = "repo_id"),
        inverseJoinColumns = @JoinColumn(name = "topic_id")
    )
    @Fetch(value = FetchMode.JOIN)
    Set<Topic> topics = new HashSet<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        GitRepo gitRepo = (GitRepo) o;
        return getId() != null && Objects.equals(getId(), gitRepo.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    /**
     * To be called when the repository has been crawled through GitHub's API.
     */
    public void setCrawled() {
        crawled = new Date();
    }

    /**
     * To be called when the repository's code metrics have been mined.
     */
    public void setCloned() {
        cloned = new Date();
    }
}
