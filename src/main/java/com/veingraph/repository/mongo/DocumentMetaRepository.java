package com.veingraph.repository.mongo;

import com.veingraph.model.DocumentMeta;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface DocumentMetaRepository extends MongoRepository<DocumentMeta, String> {

    List<DocumentMeta> findByStatus(String status);
}
