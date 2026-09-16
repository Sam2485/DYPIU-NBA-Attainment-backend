package com.dypiu.nba.emmu.resolver;

import com.dypiu.nba.emmu.dto.AcademicEntityType;
import com.dypiu.nba.emmu.dto.EmmuConversationContext;
import com.dypiu.nba.emmu.dto.EntityResolutionRequest;
import com.dypiu.nba.emmu.dto.EntityResolutionResult;

import java.security.Principal;

/**
 * Centralized, authoritative academic entity resolution contract for Emmu.
 * Resolves natural-language references to canonical academic entities within authorized user scopes.
 */
public interface AcademicEntityResolver {

    /**
     * Resolves an entity reference based on a structured resolution request.
     *
     * @param request Resolution request containing query, optional entity type, and conversation context.
     * @param principal Authenticated user principal for strict RBAC candidate scoping.
     * @return Structured resolution result.
     */
    EntityResolutionResult resolve(EntityResolutionRequest request, Principal principal);

    EntityResolutionResult resolveSchool(String query, EmmuConversationContext context, Principal principal);

    EntityResolutionResult resolveDepartment(String query, EmmuConversationContext context, Principal principal);

    EntityResolutionResult resolveProgramme(String query, EmmuConversationContext context, Principal principal);

    EntityResolutionResult resolveBatch(String query, EmmuConversationContext context, Principal principal);

    EntityResolutionResult resolveCourse(String query, EmmuConversationContext context, Principal principal);

    EntityResolutionResult resolveCourseOffering(String query, EmmuConversationContext context, Principal principal);

    EntityResolutionResult resolveCO(String query, EmmuConversationContext context, Principal principal);

    EntityResolutionResult resolvePO(String query, EmmuConversationContext context, Principal principal);

    EntityResolutionResult resolvePSO(String query, EmmuConversationContext context, Principal principal);

    EntityResolutionResult resolveFaculty(String query, EmmuConversationContext context, Principal principal);

    EntityResolutionResult resolveSemester(String query, EmmuConversationContext context, Principal principal);
}
