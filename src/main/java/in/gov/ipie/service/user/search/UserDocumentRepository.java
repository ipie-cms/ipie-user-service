package in.gov.ipie.service.user.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;


/**
 * Public so the {@code searchindex} sibling subpackage (which builds the concrete
 * {@code UserSearchIndex} port implementations) and {@code UserSearchIndexConfig} can use it - by
 * convention, no other class should reference this interface.
 */
public interface UserDocumentRepository extends ElasticsearchRepository<UserDocument, String> {
}

