package com.veingraph.repository.mongo;

import com.veingraph.model.DocumentMeta;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DocumentMetaRepository extends MongoRepository<DocumentMeta, String> {
}
