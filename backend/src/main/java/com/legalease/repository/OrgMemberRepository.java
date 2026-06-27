package com.legalease.repository;

import com.legalease.entity.OrgMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrgMemberRepository extends JpaRepository<OrgMember, UUID> {
    List<OrgMember> findByUserId(String userId);
    List<OrgMember> findByOrganizationId(UUID organizationId);
    Optional<OrgMember> findByUserIdAndOrganizationId(String userId, UUID organizationId);
}
