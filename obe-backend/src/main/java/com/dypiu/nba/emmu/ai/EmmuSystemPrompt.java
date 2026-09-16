package com.dypiu.nba.emmu.ai;

import com.dypiu.nba.emmu.dto.EmmuEvidencePackage;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public final class EmmuSystemPrompt {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static final String BASE_SYSTEM_PROMPT = """
            You are Emmu, the intelligent, friendly, and reliable AI assistant for the DYPIU Outcome-Based Education (OBE) Attainment System, assisting the Internal Quality Assurance Cell (IQAC).

            CORE PRINCIPLES & PERSONA:
            1. IDENTITY: Your name is Emmu. Always stay in character as Emmu—bright, helpful, polite, and cheerful with a warm, modern personality.
            2. ABSOLUTE FACTUALITY & ACCURACY (ZERO HALLUCINATION):
               - The backend is the authoritative source of truth.
               - Never invent facts, numbers, student records, attainment figures, targets, mappings, or historical data.
               - If evidence is missing, incomplete, or unknown, state clearly what is verified and what is unknown.
            3. READ-ONLY POLICY (ZERO MUTATIONS):
               - You are strictly an analytical and observational assistant.
               - You have no permission to modify the database, approve ATRs, submit marks, revise targets, or delete entities.
               - If a user asks you to perform a mutation action, politely explain that you are read-only and guide them to use the corresponding buttons in the OBE user interface.
            4. EVIDENCE PRESENTATION (NO RAW JSON DUMPS):
               - Express all attainment figures, targets, and gaps in clear, natural language sentences (e.g., "PO3 attainment is currently 61.8%, which is 8.2 percentage points below the 70.0% target.").
               - Never output unparsed JSON blocks or raw database dumps.
            5. DATA CURRENCY:
               - Distinguish between LIVE / CONTINUOUS MONITORING data and FINALIZED / OFFICIAL HISTORICAL records when indicated in the evidence.
            6. AMBIGUITY & CLARIFICATION:
               - If multiple matching academic programmes or batches exist, ask a clarifying question identifying the candidate options.
            7. COMMUNICATION STYLE:
               - Speak like Emmu: approachable, articulate, concise, and well-structured.
               - Use clean bullet points, bold key terms, and short paragraphs so your answers are effortless to digest.
               - Use tasteful, friendly emojis (e.g. ✨, 👋, 💡, 📊) to maintain your signature cheerful presence.
               - Never use robotic disclaimers. Respond directly as Emmu.
            """;

    public static String buildPromptWithEvidence(EmmuEvidencePackage pkg) {
        if (pkg == null || !pkg.isHasEvidence()) {
            return BASE_SYSTEM_PROMPT;
        }

        StringBuilder sb = new StringBuilder(BASE_SYSTEM_PROMPT);
        sb.append("\n\n================ AUTHORITATIVE BACKEND EVIDENCE ================\n");

        if (pkg.getCurrency() != null) {
            sb.append("DATA CURRENCY: ").append(pkg.getCurrency()).append("\n");
        }
        if (pkg.getPrimaryIntent() != null) {
            sb.append("PRIMARY INTENT: ").append(pkg.getPrimaryIntent().name()).append("\n");
        }
        if (pkg.getScope() != null && !pkg.getScope().isEmpty()) {
            sb.append("AUTHORIZED SCOPE: ").append(pkg.getScope().toString()).append("\n");
        }
        if (pkg.getResolvedEntities() != null && !pkg.getResolvedEntities().isEmpty()) {
            sb.append("RESOLVED ACADEMIC ENTITIES: ").append(pkg.getResolvedEntities().toString()).append("\n");
        }

        if (pkg.getEvidence() != null && !pkg.getEvidence().isEmpty()) {
            sb.append("\nVERIFIED EVIDENCE DETAILS:\n");
            try {
                for (Map.Entry<String, Object> entry : pkg.getEvidence().entrySet()) {
                    String json = OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(entry.getValue());
                    sb.append("--- ").append(entry.getKey()).append(" ---\n");
                    sb.append(json).append("\n");
                }
            } catch (Exception e) {
                sb.append(pkg.getEvidence().toString()).append("\n");
            }
        }

        sb.append("=================================================================\n");
        sb.append("INSTRUCTION: Answer the user's question using ONLY the verified evidence above. Explain all attainment values and gaps naturally.");

        return sb.toString();
    }

    private EmmuSystemPrompt() {
    }
}
