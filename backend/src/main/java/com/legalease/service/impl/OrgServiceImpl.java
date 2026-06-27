package com.legalease.service.impl;

import com.legalease.entity.Organization;
import com.legalease.entity.OrgMember;
import com.legalease.entity.Document;
import com.legalease.entity.User;
import com.legalease.repository.OrganizationRepository;
import com.legalease.repository.OrgMemberRepository;
import com.legalease.repository.DocumentRepository;
import com.legalease.repository.UserRepository;
import com.legalease.service.OrgService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrgServiceImpl implements OrgService {

    private static final Logger log = LoggerFactory.getLogger(OrgServiceImpl.class);

    private final OrganizationRepository organizationRepository;
    private final OrgMemberRepository orgMemberRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    public OrgServiceImpl(
            OrganizationRepository organizationRepository,
            OrgMemberRepository orgMemberRepository,
            DocumentRepository documentRepository,
            UserRepository userRepository) {
        this.organizationRepository = organizationRepository;
        this.orgMemberRepository = orgMemberRepository;
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public Organization createOrganization(String name, String creatorClerkUserId) {
        log.info("Creating organization: {} by user: {}", name, creatorClerkUserId);
        User creator = getOrCreateUser(creatorClerkUserId);

        // Generate a clean, readable invite code (e.g., L-EASE-XXXX)
        String inviteCode = "L-EASE-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Organization org = Organization.builder()
                .name(name)
                .inviteCode(inviteCode)
                .build();
        org = organizationRepository.save(org);

        // Add creator as ADMIN member
        OrgMember membership = OrgMember.builder()
                .user(creator)
                .organization(org)
                .role("ADMIN")
                .build();
        orgMemberRepository.save(membership);

        return org;
    }

    @Override
    @Transactional
    public Organization joinOrganization(String inviteCode, String clerkUserId) {
        log.info("User: {} joining organization with code: {}", clerkUserId, inviteCode);
        User user = getOrCreateUser(clerkUserId);

        Organization org = organizationRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid invite code: " + inviteCode));

        // Check if already a member
        orgMemberRepository.findByUserIdAndOrganizationId(clerkUserId, org.getId())
                .ifPresent(m -> {
                    throw new IllegalStateException("User is already a member of this workspace.");
                });

        OrgMember membership = OrgMember.builder()
                .user(user)
                .organization(org)
                .role("MEMBER")
                .build();
        orgMemberRepository.save(membership);

        return org;
    }

    @Override
    public List<Organization> getUserOrganizations(String clerkUserId) {
        log.info("Fetching organizations for user: {}", clerkUserId);
        List<OrgMember> memberships = orgMemberRepository.findByUserId(clerkUserId);
        return memberships.stream()
                .map(OrgMember::getOrganization)
                .collect(Collectors.toList());
    }

    @Override
    public List<Document> getOrgDocuments(UUID orgId, String clerkUserId) {
        log.info("Fetching documents for org: {} requested by user: {}", orgId, clerkUserId);
        // Verify user is a member of the organization
        orgMemberRepository.findByUserIdAndOrganizationId(clerkUserId, orgId)
                .orElseThrow(() -> new SecurityException("User is not authorized to access this workspace."));

        return documentRepository.findByOrganizationIdOrderByCreatedAtDesc(orgId);
    }

    @Override
    @Transactional
    public void shareDocumentToOrg(UUID documentId, UUID orgId, String clerkUserId) {
        log.info("Sharing document: {} to org: {} by user: {}", documentId, orgId, clerkUserId);
        // Verify membership
        orgMemberRepository.findByUserIdAndOrganizationId(clerkUserId, orgId)
                .orElseThrow(() -> new SecurityException("User is not authorized to access this workspace."));

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document not found: " + documentId));

        // Verify document ownership
        if (!doc.getUser().getId().equals(clerkUserId)) {
            throw new SecurityException("User is not authorized to share this document.");
        }

        doc.setOrganization(organizationRepository.getReferenceById(orgId));
        documentRepository.save(doc);
    }

    private User getOrCreateUser(String clerkUserId) {
        return userRepository.findById(clerkUserId)
                .orElseGet(() -> {
                    String email = "user-" + clerkUserId + "@legalease.com";
                    String name = "LegalEase User";

                    org.springframework.security.core.Authentication auth =
                            org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() instanceof org.springframework.security.oauth2.jwt.Jwt) {
                        org.springframework.security.oauth2.jwt.Jwt jwt = (org.springframework.security.oauth2.jwt.Jwt) auth.getPrincipal();
                        String jwtEmail = jwt.getClaim("email");
                        String jwtName = jwt.getClaim("name");
                        if (jwtEmail != null) email = jwtEmail;
                        if (jwtName != null) name = jwtName;
                    }

                    User newUser = User.builder()
                            .id(clerkUserId)
                            .email(email)
                            .name(name)
                            .role("USER")
                            .build();
                    return userRepository.save(newUser);
                });
    }
}
