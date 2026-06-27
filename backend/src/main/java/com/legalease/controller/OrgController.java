package com.legalease.controller;

import com.legalease.entity.Organization;
import com.legalease.entity.Document;
import com.legalease.service.OrgService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations")
public class OrgController {

    private static final Logger log = LoggerFactory.getLogger(OrgController.class);

    private final OrgService orgService;

    public OrgController(OrgService orgService) {
        this.orgService = orgService;
    }

    @PostMapping
    public ResponseEntity<Organization> createOrganization(
            @RequestParam("name") String name,
            @AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Request to create organization name: {} by user: {}", name, clerkUserId);
        Organization org = orgService.createOrganization(name, clerkUserId);
        return ResponseEntity.ok(org);
    }

    @PostMapping("/join")
    public ResponseEntity<Organization> joinOrganization(
            @RequestParam("inviteCode") String inviteCode,
            @AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Request to join organization with inviteCode: {} by user: {}", inviteCode, clerkUserId);
        Organization org = orgService.joinOrganization(inviteCode, clerkUserId);
        return ResponseEntity.ok(org);
    }

    @GetMapping
    public ResponseEntity<List<Organization>> getUserOrganizations(
            @AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Fetching organizations list for user: {}", clerkUserId);
        List<Organization> orgs = orgService.getUserOrganizations(clerkUserId);
        return ResponseEntity.ok(orgs);
    }

    @GetMapping("/{orgId}/documents")
    public ResponseEntity<List<Document>> getOrgDocuments(
            @PathVariable("orgId") UUID orgId,
            @AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Fetching documents list for organization ID: {} for user: {}", orgId, clerkUserId);
        List<Document> docs = orgService.getOrgDocuments(orgId, clerkUserId);
        return ResponseEntity.ok(docs);
    }

    @PostMapping("/{orgId}/share")
    public ResponseEntity<Void> shareDocumentToOrg(
            @PathVariable("orgId") UUID orgId,
            @RequestParam("documentId") UUID documentId,
            @AuthenticationPrincipal Jwt jwt) {
        String clerkUserId = jwt.getSubject();
        log.info("Request to share document: {} to org: {} by user: {}", documentId, orgId, clerkUserId);
        orgService.shareDocumentToOrg(documentId, orgId, clerkUserId);
        return ResponseEntity.ok().build();
    }
}
