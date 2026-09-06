package in.gov.ipie.service.user.search;

import org.elasticsearch.client.RestClient;
import org.springframework.boot.actuate.elasticsearch.ElasticsearchRestClientHealthIndicator;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

import in.gov.ipie.service.user.repository.UserRepository;
import in.gov.ipie.service.user.repository.UserSearchIndex;
import in.gov.ipie.service.user.repository.OrganisationHierarchyRepository;

/**
 * Chooses the {@link UserSearchIndex} implementation: {@link ElasticsearchUserSearchIndex} when
 * {@code spring.elasticsearch.uris} is configured (e.g. {@code IPIE_ELASTICSEARCH_URIS} in
 * docker-compose.yml), {@link JpaUserSearchIndex} otherwise - mirrors
 * {@code EventPublisherConfig}'s Kafka/logging fallback pattern.
 */
@Configuration
public class UserSearchIndexConfig {

    /**
     * Manually re-enables {@link UserDocumentRepository} scanning, gated by the same
     * {@code spring.elasticsearch.uris} property this class's own beans are gated by -
     * {@code application.yml} excludes Spring Boot's automatic {@code
     * ElasticsearchRepositoriesAutoConfiguration} because it is *not* itself conditional on that
     * property (classpath presence of {@code spring-boot-starter-data-elasticsearch} alone is
     * enough for it to fire, eagerly building {@link UserDocumentRepository} and checking index
     * existence against whatever {@code spring.elasticsearch.uris} defaults to - {@code
     * localhost:9200} - when unset, which fails with a connection error rather than silently
     * doing nothing). This nested class restores the same auto-scan behaviour, but only when the
     * property is actually present, matching {@code EventConsumerConfig}/{@code
     * RabbitConsumerConfig}'s class-level {@code @ConditionalOnProperty} for the identical reason.
     */
    @Configuration
    @ConditionalOnProperty(prefix = "spring.elasticsearch", name = "uris")
    @EnableElasticsearchRepositories(basePackageClasses = UserDocumentRepository.class)
    static class ElasticsearchRepositoryEnabler {
    }

    /**
     * Restores the "elasticsearch" {@code /actuator/health} entry the excluded {@code
     * ElasticsearchRestHealthContributorAutoConfiguration}/{@code
     * ElasticsearchReactiveHealthContributorAutoConfiguration} would otherwise have provided
     * unconditionally - gated the same way as {@link #elasticsearchUserSearchIndex} and {@link
     * ElasticsearchRepositoryEnabler} so it only appears once Elasticsearch is actually
     * configured, not just present on the classpath. {@code RestClient} itself still comes from
     * {@code ElasticsearchDataAutoConfiguration} (not excluded - it builds the client lazily, with
     * no eager connection at bean-creation time, which is why only the repository/health
     * auto-configurations needed excluding).
     */
    @Bean
    @ConditionalOnProperty(prefix = "spring.elasticsearch", name = "uris")
    public HealthIndicator elasticsearchHealthIndicator(RestClient restClient) {
        return new ElasticsearchRestClientHealthIndicator(restClient);
    }

    @Bean
    @ConditionalOnProperty(prefix = "spring.elasticsearch", name = "uris")
    public UserSearchIndex elasticsearchUserSearchIndex(
            ElasticsearchOperations operations, UserDocumentRepository documentRepository,
            UserSearchDocumentMapper mapper, OrganisationHierarchyRepository hierarchy) {
        IndexOperations indexOps = operations.indexOps(UserDocument.class);
        if (!indexOps.exists()) {
            indexOps.create();
            indexOps.putMapping();
        }
        return new ElasticsearchUserSearchIndex(operations, documentRepository, mapper, hierarchy);
    }

    @Bean
    @ConditionalOnMissingBean(UserSearchIndex.class)
    public UserSearchIndex jpaUserSearchIndex(UserRepository userRepository) {
        return new JpaUserSearchIndex(userRepository);
    }
}

