package com.legalease.service;

import com.legalease.entity.Organization;
import com.legalease.entity.Document;
import java.util.List;
import java.util.UUID;

public interface OrgService {
    Organization createOrganization(String name, String creatorClerkUserId);
    Organization joinOrganization(String inviteCode, String clerkUserId);
    List<Organization> getUserOrganizations(String clerkUserId);
    List<Document> getOrgDocuments(UUID orgId, String clerkUserId);
    void shareDocumentToOrg(UUID documentId, UUID orgId, String clerkUserId);
}
