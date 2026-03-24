package com.veingraph.auth.repository;

import com.veingraph.auth.model.SysUser;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SysUserRepository extends MongoRepository<SysUser, String> {

    Optional<SysUser> findByUsername(String username);

    Optional<SysUser> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByUsername(String username);
}
