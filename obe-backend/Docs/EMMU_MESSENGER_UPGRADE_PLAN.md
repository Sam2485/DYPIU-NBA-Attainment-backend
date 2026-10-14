# EMMU MESSENGER UPGRADE PLAN: ARCHITECTURAL SPECIFICATION & TECHNICAL BLUEPRINT

**Target System:** DYPIU NBA/OBE Attainment Platform  
**Document Status:** FINAL ARCHITECTURAL SPECIFICATION — PLAN ONLY (NO CODE MODIFICATIONS APPLIED)  
**Author:** Antigravity Agentic Architecture Pair  
**Date:** October 14, 2026  

---

## 1. Executive Summary

This document specifies the complete engineering plan to upgrade **Emmu** from its current role as the single-user **IQAC OBE Intelligence Assistant** into an institutional, multi-role **Three-Domain Communication System**:

```text
                                  ┌───────────────────────────────┐
                                  │      EMMU MESSENGER HUB       │
                                  │  (Preserved Mascot / Launcher)│
                                  └───────────────┬───────────────┘
                                                  │
            ┌─────────────────────────────────────┼─────────────────────────────────────┐
            ▼                                     ▼                                     ▼
 ┌──────────────────────┐             ┌─────────────────────────┐             ┌───────────────────────┐
 │     1. CHATS         │             │     2. SUGGESTIONS      │             │   3. NOTIFICATIONS    │
 │ One-to-one text      │             │ Contextual academic     │             │ System-generated      │
 │ messaging between    │             │ recommendations routed  │             │ authoritative OBE     │
 │ eligible users       │             │ from Analytics views    │             │ workflow transitions  │
 └──────────────────────┘             └─────────────────────────┘             └───────────────────────┘
```

### Core Tenets of the Upgrade
1. **Preserve Visual Identity & UX:** The existing draggable floating Emmu mascot, Three.js WebGL Genie animation, and modal window architecture remain intact.
2. **Three Communication Modes:** The single chat view is replaced with a 3-tab hub: **Chats**, **Suggestions**, and **Notifications**.
3. **Preserve Existing AI Safely:** The existing Ollama/LLM streaming implementation (`/api/v1/emmu`, `/api/v1/ws/chat`) is preserved intact but made dormant as the non-default mode, avoiding breaking changes while leaving it available for future reactivation.
4. **Contextual Academic Suggestions:** Initiated **only** from active Analytics views with server-side validation and automated recipient resolution.
5. **Authoritative Workflow Notifications:** Emitted directly from existing OBE approval state transitions (`ApprovalRequest`, `ApprovalHistory`) with strict deduplication.
6. **Universal Role Access & Profile Settings:** Emmu is decoupled from the `role === 'IQAC'` restriction and made available to all authenticated roles, with user-level controls in the Profile modal (streamlined to **Info** and **Settings** only).

---

## 2. Existing Emmu Implementation Audit

### 2.1 Frontend Assets & Components
- **`src/App.jsx`**:
  - Dynamically imports `IqacEmmuAssistant` via `lazy()`.
  - Mounted unconditionally at root under `<BrowserRouter>`, but inner component renders conditionally.
- **`src/components/emmu/IqacEmmuAssistant.jsx`**:
  - Contains strict guard: `if (!isAuthenticated || role !== 'IQAC') return null;`.
  - Maintains `chatState`: `'closed' | 'opening' | 'open' | 'closing'`.
  - Holds live DOM references (`widgetRef`, `chatCardRef`) connected to `GenieAnimation`.
  - Derives `academicContext` from `useAcademic()` and `useLocation()`.
- **`src/components/emmu/EmmuWidget.jsx` & `EmmuWidget.css`**:
  - Draggable 120×120px circular launcher pinned to `bottom: 28px, right: 28px`.
  - Contains `.emmu-portal` (circular radial gradient `#465366` to `#171c24` with `border: 2px solid rgba(255,255,255,0.78)` and `border-radius: 50%`).
  - Contains `.emmu` character container submerged at `bottom: -66px` when idle, animating to `bottom: -8px` on hover/peek.
  - Hosts interactive canvas eye-tracking (`EmmuEyes.jsx`), speech bubble, notification badge, and "Ask Emmu" badge.
- **`src/components/emmu/EmmuChatModal.jsx` & `EmmuChatModal.css`**:
  - Resizable floating modal card (default 480×640px, max 860×900px).
  - Header: Emmu avatar (`emmu_message.png`), title "Emmu Intelligence Assistant", connection status dot, minimize/expand controls, and close button.
  - Body: Message history area displaying chat bubbles (`bot` vs `user`), currency chips (`LIVE`), and drill-down buttons.
  - Composer: Quoted context preview, auto-expanding textarea, attachment button (currently mock), model badge dropdown (`Llama 3.1 8B`), voice input button, and submit button.
- **`src/components/emmu/GenieAnimation.jsx` & `OffscreenChatPanel.jsx`**:
  - Three.js WebGL mesh warping providing a smooth macOS-style "genie effect" between `EmmuWidget` and `EmmuChatModal`.
  - `GenieTextureCache.js` pre-renders rasterized snapshots of the chat panel.
- **`src/components/emmu/services/chatSocket.js`**:
  - Native browser WebSocket client connecting to `ws://localhost:8010/api/v1/ws/chat?token=<JWT>`.

### 2.2 Backend Architecture Audit
- **WebSocket Infrastructure (`com.dypiu.nba.emmu.websocket`)**:
  - `WebSocketConfig.java`: Registers `EmmuChatWebSocketHandler` at `/ws/chat` and `/obe/ws/chat`.
  - `JwtWebSocketHandshakeInterceptor.java`: Extracts query parameter `token` or `Authorization` header, validates JWT, and populates `SecurityContext`.
  - `EmmuChatWebSocketHandler.java`: Handles bidirectional JSON payloads (`type: "chat"`, `"stop"`), invokes `EmmuOrchestrationService`, and streams tokens back (`type: "token"`, `"metadata"`, `"done"`, `"error"`).
- **AI Integration (`com.dypiu.nba.emmu.ai`)**:
  - `OllamaAiClient.java`: HTTP client communicating with local/remote Ollama instance.
  - `EmmuSystemPrompt.java`: Formulates prompt templates with PO/PSO benchmarks and curriculum attainment guidelines.
  - `EmmuEvidenceService.java`: Gathers academic data packages (`EmmuEvidencePackage`) from repositories.
- **Entity Resolution (`com.dypiu.nba.emmu.resolver`)**:
  - `AcademicEntityResolver.java` & `DefaultAcademicEntityResolver.java`: Resolves free-form natural language queries into exact academic entity IDs (`programmeBatchId`, `masterProgrammeId`, `programmeBatchCourseId`).
- **REST Endpoints (`EmmuController.java`)**:
  - `/api/v1/emmu/resolve`: Resolves entity queries against authenticated scope.
  - `/api/v1/emmu/context-summary`: Summary KPI indicators for context.
  - `/api/v1/emmu/root-cause-drilldown`: PO/PSO gap drilldown data.
  - `/api/v1/emmu/course-rankings` & `/course-intelligence`: Course-level performance metrics.

---

## 3. UI Modification Plan: Preserving Emmu & Meeting New Requirements

### 3.1 Launcher Transparency & Styling Analysis
The user requested an investigation into whether we can:
> *"remove the emmu black bg and circle and keep only the image or just make the circle boarder and bg color transparent"*

**Technical Finding from CSS & Asset Inspection:**
1. In `EmmuWidget.css`:
   - `.emmu-portal` has `border-radius: 50%`, `overflow: hidden`, and a dark radial gradient background (`#465366` to `#171c24`).
   - The character image `emmu.png` has a transparent background, but its lower body ends in a horizontal flat crop designed specifically to hide inside the portal.
   - The container `.emmu` is translated down by `bottom: -66px` when idle and rises to `bottom: -8px` on hover.
2. **If we set `background: transparent; border: none; box-shadow: none;` on `.emmu-portal`**:
   - Because `overflow: hidden` and `border-radius: 50%` are preserved, the character appears directly on top of the host webpage without any black circle, while still peeking up cleanly through an invisible circular aperture!
   - This achieves a completely transparent, floating mascot look with zero visual clipping artifacts.
3. **If `overflow: visible` were set without the circle**:
   - The flat bottom edge of the cropped character graphic would float visibly in mid-air unless the resting position is recalibrated to `bottom: 0`.

**Architectural Recommendation:**
Support two launcher aesthetic modes via Profile Settings:
- **`portal` (Classic):** The original circular aperture with subtle border and depth shadow.
- **`transparent` (Floating Mascot):** `background: transparent; border: none; box-shadow: none;`, keeping `overflow: hidden` so Emmu emerges cleanly from the page canvas.

### 3.2 Launcher Icon Selection: Emmu Mascot vs Standard Messenger FAB
Users can choose between:
- **`emmu` (Mascot):** Draggable Emmu character with animated eye-tracking, peek gestures, and speech bubble.
- **`messenger` (Standard FAB):** A clean, modern circular floating action button (56×56px) featuring a sleek `MessageSquare` / `Send` icon, subtle gradient (`#4f46e5` to `#6366f1`), elevation shadow, and a badge pill displaying total unread count.

**Persistence:**  
Stored in browser `localStorage` under key `nba_messenger_launcher_choice` (`'emmu' | 'messenger'`) and mirrored into user preferences so the choice survives sessions.

### 3.3 Profile Modal Streamlining (Info & Settings Only)
In `UserProfileModal.jsx` and `AccountPanelSection.jsx`:
- **Tabs to Remove:** `security` and `notifications` are deprecated and removed from the tabs array.
- **Preserved Tabs:**
  1. **`profile` (Info):** Personal information (Name, Role, Email, Department, School, Employee ID), About narrative, Course count, Active batch.
  2. **`settings` (Settings):**
     - **Emmu Messenger Active Toggle:** Switch to enable or completely hide the floating messenger widget (stored under `nba_emmu_enabled`, default `true`).
     - **Launcher Icon Style:** Radio selection between *"Emmu Mascot"* and *"Standard Messenger Icon"*.
     - **Emmu Visual Style:** (Active when Mascot is selected) Toggle between *"Classic Portal Circle"* and *"Transparent / Floating Character"*.
     - **Interface Density & Motion:** Preserved compact mode and reduced motion toggles.

### 3.4 Three-Tab Header Architecture for Emmu Window
The internal structure of `EmmuChatModal.jsx` will be modified into a tabbed layout:

```text
┌────────────────────────────────────────────────────────────────────────┐
│ [Emmu Icon]  Emmu Messenger                   [—] [⤢] [✕]              │
├────────────────────────────────────────────────────────────────────────┤
│   [ 💬 Chats (3) ]    [ 💡 Suggestions (1) ]    [ 🔔 Alerts (5) ]      │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│                                                                        │
│                         MODE CONTENT AREA                              │
│                                                                        │
│                                                                        │
├────────────────────────────────────────────────────────────────────────┤
│ COMPOSER AREA (Context-sensitive: Chats or Suggestions)                │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Header Navigation:**
   - Tabs: `Chats`, `Suggestions`, `Notifications`.
   - Each tab displays an active pill with dedicated category unread counts:
     - `unreadChatsCount`
     - `unreadSuggestionsCount`
     - `unreadNotificationsCount`
   - Overall widget badge = Sum of unread items across all 3 categories.
2. **Mode-Specific Views:**
   - **Chats Tab:** WhatsApp-style conversation list, search bar, active direct chat history, message bubble stream, and standard text composer.
   - **Suggestions Tab:** Split/tabbed view of *Received Suggestions* and *Sent Suggestions*, contextual detail view with *"View in Analytics"* action, and clear attribution badges.
   - **Notifications Tab:** Feed of system-generated workflow alerts grouped by date/status, with direct deep-links to approval workspaces and *"Mark all as read"* action. (No bottom composer).
3. **Preserving Dormant AI:**
   - The existing AI intelligence engine is preserved on backend (`/ws/chat` with `type: "chat"` and `/api/v1/emmu/*`).
   - In frontend, the AI chat code is encapsulated inside a dormant component (`LegacyAiChatView.jsx`), decoupled from the default active messaging tabs. It can be triggered in the future via an administrative feature flag or an "AI Assistant" mode switch without modifying core messenger logic.

---

## 4. Feature One: Normal Messenger (Chats)

### 4.1 Functional Requirements
- **User Directory & Search:** Fast autocomplete search by name, email, or role against authorized system users.
- **Canonical Direct Conversations:** A unique 1-to-1 conversation is created or retrieved for any user pair (`min(userA, userB)` + `max(userA, userB)`). Duplicate conversations between the same two users are strictly prevented by database constraints.
- **Conversation List:** Displays active conversations sorted descending by `last_message_at`, showing peer avatar, peer name, role badge, last message preview, unread message count badge, and relative timestamp.
- **Message Stream:** WhatsApp-style bubbles (incoming left in `#f1f5f9`, outgoing right in `#4f46e5`), with delivery timestamp, read receipt indicators (`sent`, `read`), and infinite scrolling/cursor-based pagination.
- **Read State Tracking:** Messages contain `read_at` timestamp. Opening a conversation triggers an immediate batch mark-read update for unread messages sent by the peer.

### 4.2 Eligibility Policy (Open Decision Analyzed)
- **Codebase Finding:** DYPIU NBA users belong to specific organizational scopes (`schoolId`, `departmentId`, `masterProgrammeId`) via `User` and `UserOrganizationalAssignment`.
- **Recommended Secure Default:**
  - Administrative & Governance roles (`IQAC`, `TRUSTEE`, `DIRECTOR`, `SCHOOL_PROGRAMME_COORDINATOR`) can message **any active user** across the institution.
  - Departmental roles (`HOD`, `PROGRAMME_COORDINATOR`) can message users within their School/Department hierarchy as well as institutional governance.
  - Operational roles (`FACULTY`, `COURSE_COORDINATOR`) can message coordinators, HODs, and faculty within their school/department courses.
  - *Explicitly documented in Section 11 as an open decision for product approval.*

---

## 5. Feature Two: Contextual Suggestions

### 5.1 Concept & Distinction
Suggestions are **actionable academic feedback** tied directly to specific curriculum attainment metrics. They are **not** free-form chat messages and **cannot** be initiated arbitrarily from the inbox.

### 5.2 Analytics-Only Initiation Flow
1. **Trigger Condition:** The user is currently navigating an eligible Analytics screen (e.g., `QuickAnalysisDashboard`, `BatchAnalyticsPage`, `CourseAnalyticsPage`).
2. **Context Detection:** Emmu detects the active route and academic parameters:
   - Programme Batch View: `masterProgrammeId` + `programmeBatchId`
   - Course Instance View: `programmeBatchCourseId` (plus parent `programmeBatchId`)
3. **Composer Adaptation:**
   - In standard views, composer placeholder displays `"Type a message..."`.
   - In eligible Analytics views, composer displays:  
     `"Have suggestions? Drop them."`
4. **Contextual Suggestion Modal / Drawer:**
   - Clicking the prompt expands the Suggestion Composer displaying:
     - **Context Card:** Academic Year, Programme Name, Batch Year, Course Code/Title.
     - **Target Recipient:** Automatically determined role (e.g., *"Assigned Programme Coordinator"* or *"Assigned Course Coordinator"*). The sender **cannot** alter or pick the recipient ID.
     - **Severity / Category Tag:** Optional academic tag (*Attainment Gap*, *Curriculum Action*, *General Observation*).
     - **Content Input:** Suggestion text with minimum character requirements.
     - **Actions:** *"Cancel"* and *"Send Suggestion"*.

### 5.3 Context-Specific Recipient Routing Rules
- **Rule A (Programme-Batch Context):**
  - Triggered from: Batch Attainment, Batch Comparison, PO/PSO Attainment screens.
  - Parameters: `programmeBatchId` (authoritative).
  - Recipient Resolution: Backend looks up `ProgrammeBatch` by ID and extracts `coordinatorId` / `coordinatorEmail`. If absent, backend checks `UserOrganizationalAssignment` for active `PROGRAMME_COORDINATOR` assigned to `masterProgrammeId`.
- **Rule B (Course Context):**
  - Triggered from: Course Attainment, CO Direct/Indirect Drilldown screens.
  - Parameters: `programmeBatchCourseId` (authoritative).
  - Recipient Resolution: Backend looks up `ProgrammeBatchCourse` by ID and extracts `courseCoordinatorId` / `courseCoordinatorEmail`.
- **Rule C (CO-Level Context):**
  - In this system, CO drilldown pages (`OutcomeDirectDrilldownPage`, `OutcomeIndirectDrilldownPage`) always operate under a parent `programmeBatchCourseId` or `programmeBatchId`. The suggestion automatically binds to the underlying course coordinator.
- **Rule D (School-Wide / Programme-Wide Contexts):**
  - *Marked as an Open Decision in Section 11.* School-level suggestions route to the School Director; programme-level suggestions route to the HOD.

### 5.4 Suggestion Lifecycle & Integrity
- **Persistence:** Suggestion record stores immutable snapshots of sender identity, recipient identity, academic context keys, suggestion content, creation timestamp, and read timestamp.
- **Zero Academic Side-Effects:** A suggestion is an authoritative communication record. It **never** alters attainment scores, marks, thresholds, or approval states.
- **Navigation:** Recipients can click *"View Analytics Context"* on any received suggestion to deep-link directly back to the exact Analytics dashboard with filters pre-populated.

---

## 6. Role & Permission Hierarchy for Suggestions

### 6.1 Formal Suggestion Permission Matrix

| Sender Role | May Send Suggestions To | Prohibited Recipient Roles | Scope Constraints |
| :--- | :--- | :--- | :--- |
| **TRUSTEE** *(New)* | Director, School PC, HOD, PC, CC | Super Admin, IQAC, Trustee | Institution-wide (Read-only analytics) |
| **IQAC** | Director, School PC, HOD, PC, CC | Super Admin, Trustee | Institution-wide |
| **DIRECTOR** | HOD, PC, CC | Super Admin, IQAC, Trustee, Director | Restricted to sender's `schoolId` |
| **SCHOOL PC** *(New)*| HOD, PC, CC | Super Admin, IQAC, Trustee, Director | Restricted to sender's `schoolId` |
| **HOD** | PC, CC | Governance roles, HODs of other depts | Restricted to sender's `departmentId` |
| **PROGRAMME COORDINATOR** | CC | Governance roles, HODs, other PCs | Restricted to courses within assigned batch |
| **COURSE COORDINATOR** | *None (Cannot initiate hierarchical suggestions)* | All roles | N/A |

### 6.2 Trustee & School Programme Coordinator Authorization Guardrails
- **`TRUSTEE`:**
  - Has institution-wide visibility identical to `IQAC`.
  - Is strictly **read-only** for all academic CRUD, configurations, and approvals.
  - Has explicit write permission **only** for messaging and suggestions within the permitted hierarchy.
- **`SCHOOL_PROGRAMME_COORDINATOR`:**
  - Has school-level visibility identical to `DIRECTOR`.
  - Is strictly **read-only** for all academic CRUD, configurations, and approvals.
  - Has explicit write permission **only** for messaging and suggestions directed down the hierarchy.

---

## 7. Feature Three: Workflow Notifications

### 7.1 Source of Truth & Authoritative Events
Notifications are generated **strictly** by consuming state transitions from the existing `ApprovalService` and `ApprovalRequest` entity.

**Eligible Workflow Transitions:**
1. **Submission Created / Pending Review:**
   - Event: `ApprovalStatus` transitions to `PENDING` / `SUBMITTED`.
   - Recipient: Authorized approver (e.g., HOD for Course CO/Targets; Director for Department Governance).
2. **Submission Approved:**
   - Event: `ApprovalStatus` transitions to `APPROVED`.
   - Recipient: Submitter (`submittedBy`).
3. **Revision Requested:**
   - Event: `ApprovalStatus` transitions to `REVISION_REQUESTED` or `NEEDS_REVISION`.
   - Recipient: Submitter (`submittedBy`).
   - Details: Captures reviewer remarks.
4. **Resubmitted After Revision:**
   - Event: `ApprovalStatus` re-enters `PENDING` with prior revision history.
   - Recipient: Reviewer.

### 7.2 Deduplication & Reliability
- To prevent duplicate notifications on retry or re-save, each notification record includes an idempotency key:  
  `idempotency_key = "APPROVAL:" + approvalRequestId + ":" + status + ":" + updatedAt.toEpochMilli()`
- Database unique constraint on `idempotency_key` guarantees that reprocessing the same logical event produces no duplicate inbox entries.

### 7.3 Notification Inbox Experience
- **Attributes:** Title, message summary, event type badge (`CO_TARGETS`, `COURSE_ATR`, `ATTAINMENT_SETTINGS`), relative time, read/unread state.
- **Navigation:** Clicking the item validates the user's current role and deep-links directly to the relevant approval workspace (`/hod/approvals`, `/director/approvals`, or `/coordinator/review`).
- **Bulk Action:** *"Mark all notifications as read"* endpoint for quick inbox clearance.

---

## 8. Proposed Database Model

The messaging subsystem will be introduced via Flyway migration script `V24__create_emmu_messenger_schema.sql` without altering any existing academic tables.

```text
┌──────────────────────────────┐          ┌──────────────────────────────┐
│        conversations         │          │   conversation_participants  │
├──────────────────────────────┤          ├──────────────────────────────┤
│ id: VARCHAR(36) [PK]         │◀───┐      │ conversation_id: VARCHAR(36) │
│ created_at: TIMESTAMPTZ      │    │      │ user_id: BIGINT              │
│ updated_at: TIMESTAMPTZ      │    └─────│ last_read_at: TIMESTAMPTZ    │
│ last_message_at: TIMESTAMPTZ │           │ unread_count: INT            │
└──────────────────────────────┘           │ PRIMARY KEY (conv_id, user)  │
               │                           └──────────────────────────────┘
               ▼
┌──────────────────────────────┐
│           messages           │
├──────────────────────────────┤
│ id: VARCHAR(36) [PK]         │
│ conversation_id: VARCHAR(36) │
│ sender_id: BIGINT            │
│ content: TEXT                │
│ created_at: TIMESTAMPTZ      │
│ read_at: TIMESTAMPTZ         │
└──────────────────────────────┘

┌──────────────────────────────┐          ┌──────────────────────────────┐
│         suggestions          │          │        notifications         │
├──────────────────────────────┤          ├──────────────────────────────┤
│ id: VARCHAR(36) [PK]         │          │ id: VARCHAR(36) [PK]         │
│ sender_id: BIGINT            │          │ recipient_id: BIGINT         │
│ sender_role: VARCHAR(50)     │          │ title: VARCHAR(255)          │
│ recipient_id: BIGINT         │          │ message: TEXT                │
│ recipient_role: VARCHAR(50)  │          │ event_type: VARCHAR(50)      │
│ master_programme_id: VARCHAR │          │ source_id: VARCHAR(100)      │
│ programme_batch_id: VARCHAR  │          │ deep_link: VARCHAR(255)      │
│ batch_course_id: VARCHAR     │          │ read_at: TIMESTAMPTZ         │
│ content: TEXT                │          │ idempotency_key: VARCHAR(190)│
│ status: VARCHAR(30)          │          │ created_at: TIMESTAMPTZ      │
│ created_at: TIMESTAMPTZ      │          └──────────────────────────────┘
│ read_at: TIMESTAMPTZ         │
└──────────────────────────────┘
```

### 8.1 DDL Specification (`V24__create_emmu_messenger_schema.sql`)

```sql
-- 1. Direct Conversations
CREATE TABLE conversations (
    id VARCHAR(36) PRIMARY KEY,
    user_one_id BIGINT NOT NULL REFERENCES users(id),
    user_two_id BIGINT NOT NULL REFERENCES users(id),
    last_message_preview VARCHAR(255),
    last_message_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_conversations_user_pair UNIQUE (user_one_id, user_two_id),
    CONSTRAINT ck_conversations_user_order CHECK (user_one_id < user_two_id)
);

CREATE INDEX idx_conversations_user_one ON conversations(user_one_id);
CREATE INDEX idx_conversations_user_two ON conversations(user_two_id);
CREATE INDEX idx_conversations_last_msg ON conversations(last_message_at DESC);

-- 2. Direct Messages
CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    content TEXT NOT NULL,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_messages_conversation ON messages(conversation_id, created_at ASC);
CREATE INDEX idx_messages_unread ON messages(conversation_id, read_at) WHERE read_at IS NULL;

-- 3. Academic Suggestions
CREATE TABLE suggestions (
    id VARCHAR(36) PRIMARY KEY,
    sender_id BIGINT NOT NULL REFERENCES users(id),
    sender_role VARCHAR(50) NOT NULL,
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    recipient_role VARCHAR(50) NOT NULL,
    school_id VARCHAR(50),
    department_id VARCHAR(50),
    master_programme_id VARCHAR(50),
    programme_batch_id VARCHAR(50),
    programme_batch_course_id VARCHAR(50),
    context_type VARCHAR(50) NOT NULL, -- 'PROGRAMME_BATCH' | 'COURSE' | 'SCHOOL' | 'DEPARTMENT'
    category VARCHAR(50) DEFAULT 'GENERAL',
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DELIVERED', -- 'DELIVERED' | 'ACKNOWLEDGED' | 'RESOLVED'
    read_at TIMESTAMP WITH TIME ZONE,
    acknowledged_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_suggestions_recipient ON suggestions(recipient_id, read_at);
CREATE INDEX idx_suggestions_sender ON suggestions(sender_id, created_at DESC);
CREATE INDEX idx_suggestions_batch ON suggestions(programme_batch_id);
CREATE INDEX idx_suggestions_course ON suggestions(programme_batch_course_id);

-- 4. Workflow Notifications
CREATE TABLE notifications (
    id VARCHAR(36) PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    source_entity_type VARCHAR(50) NOT NULL, -- 'APPROVAL_REQUEST'
    source_entity_id VARCHAR(100) NOT NULL,
    deep_link VARCHAR(255),
    idempotency_key VARCHAR(190) UNIQUE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_id, read_at);
CREATE INDEX idx_notifications_created ON notifications(created_at DESC);
```

---

## 9. Proposed API Contracts

All endpoints return the standard DYPIU API envelope:
```json
{
  "success": true,
  "message": "Operation description",
  "data": { ... }
}
```

### 9.1 REST Endpoints Specification

| Domain | Method | Endpoint | Request Body / Params | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Directory** | `GET` | `/api/v1/messenger/users` | `?query=&role=&page=&size=` | Search eligible users for chatting |
| **Chats** | `GET` | `/api/v1/messenger/conversations` | `?page=0&size=20` | Get current user's conversation list |
| **Chats** | `POST` | `/api/v1/messenger/conversations/direct` | `{"peerUserId": 12}` | Get or create canonical conversation |
| **Chats** | `GET` | `/api/v1/messenger/conversations/{id}/messages`| `?cursor=&size=30` | Get paginated message history |
| **Chats** | `POST` | `/api/v1/messenger/conversations/{id}/messages`| `{"content": "Hello"}` | Send a text message |
| **Chats** | `PUT` | `/api/v1/messenger/conversations/{id}/read` | None | Mark conversation messages as read |
| **Suggestions** | `GET` | `/api/v1/suggestions/received` | `?page=&size=&unreadOnly=` | View suggestions received by caller |
| **Suggestions** | `GET` | `/api/v1/suggestions/sent` | `?page=&size=` | View suggestions sent by caller |
| **Suggestions** | `POST` | `/api/v1/suggestions` | `CreateSuggestionDto` | Post contextual suggestion from Analytics |
| **Suggestions** | `PUT` | `/api/v1/suggestions/{id}/read` | None | Mark suggestion as read |
| **Suggestions** | `PUT` | `/api/v1/suggestions/{id}/acknowledge` | None | Acknowledge suggestion (if enabled) |
| **Notifications** | `GET` | `/api/v1/notifications` | `?page=&size=&unreadOnly=` | Get workflow alerts feed |
| **Notifications** | `PUT` | `/api/v1/notifications/{id}/read`| None | Mark single notification as read |
| **Notifications** | `PUT` | `/api/v1/notifications/read-all` | None | Mark all notifications as read |
| **Unread Summary**| `GET` | `/api/v1/messenger/badges` | None | Returns `{chats: 2, suggestions: 1, notifications: 4, total: 7}` |

---

## 10. Real-Time Delivery Strategy

### 10.1 Investigation of Current Architecture
- The backend already includes Spring WebSocket support (`@EnableWebSocket`) with `JwtWebSocketHandshakeInterceptor` at `/ws/chat`.
- The frontend includes `chatSocket.js` with auto-reconnection and token passing.
- It is currently used exclusively for streaming raw AI tokens from Ollama.

### 10.2 Recommended Phased Approach
1. **Phase A (Initial Release — Rock Solid & Reliable):**
   - **REST + Polling Fallback:** All chat sending, suggestion submissions, and notification operations use authoritative REST endpoints with transactional database persistence.
   - **Badge Polling:** Frontend polls `/api/v1/messenger/badges` at a gentle interval (e.g., 20–30 seconds) while the browser tab is focused, updating category counts.
   - **Conversation Polling:** Active conversation polls every 3–4 seconds for new incoming bubbles while chat modal is open.
   - *Advantage:* Zero dependency risks, works flawlessly behind reverse proxies/firewalls, eliminates complex state synchronization during initial launch.
2. **Phase B (Enhanced Real-Time Delivery):**
   - Extend the existing WebSocket handler or adopt lightweight STOMP over WebSocket to broadcast user-specific events:
     - `USER_EVENT:{userId} -> {type: "NEW_MESSAGE", conversationId, senderId}`
     - `USER_EVENT:{userId} -> {type: "NEW_SUGGESTION", suggestionId}`
     - `USER_EVENT:{userId} -> {type: "NEW_NOTIFICATION", notificationId}`
   - The database remains the immutable source of truth; WebSocket acts strictly as a low-latency trigger.

---

## 11. Open Decisions Requiring Product Approval

The following items are unresolved business policies that cannot be resolved solely from codebase inspection. They are presented here with options, impact analysis, and our recommended defaults.

### Decision 1: Normal Messaging Eligibility Scope
- **The Question:** Can any user message any other user in the university, or is normal messaging restricted by hierarchy/school?
- **Codebase Finding:** Users are assigned to `schoolId` and `departmentId`. Governance roles have institution-wide scope.
- **Options:**
  - *Option A:* Open directory — Any active authenticated user can message any other active user.
  - *Option B (Recommended):* Scoped directory — Faculty/Coordinators can only message peers within their department/school or governance heads; Governance (`IQAC`, `TRUSTEE`, `DIRECTOR`) can message anyone.
  - *Option C:* Strictly vertical — Users can only message direct managers or direct reports.
- **Impact:** Option B prevents cross-department noise while enabling necessary horizontal collaboration.
- **Blocks Implementation?** No. Can implement Option B as secure default.

### Decision 2: Multiple Assigned Coordinators
- **The Question:** If a batch or course has multiple assigned coordinators, does a suggestion go to all of them or a designated primary?
- **Codebase Finding:** `ProgrammeBatch` has single `coordinatorId`/`coordinatorEmail`. `ProgrammeBatchCourse` has `courseCoordinatorId` and comma-separated `assignedFaculty`.
- **Options:**
  - *Option A (Recommended):* Primary coordinator receives it (resolved from `courseCoordinatorId` / `coordinatorId`).
  - *Option B:* Broadcast suggestion to all assigned faculty for that course instance.
- **Impact:** Option A avoids duplicate uncoordinated responses.
- **Blocks Implementation?** No. Option A is fully supported by existing schema.

### Decision 3: School-Wide and Programme-Wide Suggestions Routing
- **The Question:** When an authorized user is viewing top-level School or Programme analytics (without a batch selected), who receives the suggestion?
- **Codebase Finding:** Schools have a `directorId`/`directorEmail`. Master Programmes belong to Departments which have `hodEmail`.
- **Options:**
  - *Option A (Recommended):* School-level suggestions route to the School Director; Programme-level suggestions route to the Department HOD.
  - *Option B:* Disable suggestions when no batch or course context is selected.
- **Impact:** Option A enables holistic feedback; Option B limits feedback strictly to operational levels.
- **Blocks Implementation?** No. We recommend enabling Batch and Course suggestions first, adding School/HOD in Phase 3.

### Decision 4: Suggestion Lifecycle
- **The Question:** Are suggestions simple read/unread notes, or should recipients formally acknowledge or mark them resolved?
- **Codebase Finding:** The system already uses `PENDING` -> `APPROVED` -> `REVISION_REQUESTED` for approvals, but suggestions must remain lightweight.
- **Options:**
  - *Option A (Recommended):* Two states: `DELIVERED` (unread) and `READ` (read).
  - *Option B:* Three states: `DELIVERED` -> `ACKNOWLEDGED` (one-click "Acknowledge" button) -> `RESOLVED`.
- **Impact:** Option A is the cleanest MVP. Option B adds clear accountability. We specify database support for `ACKNOWLEDGED` while starting UI with Option A.
- **Blocks Implementation?** No.

### Decision 5: Suggestion Replies vs Chats
- **The Question:** Should suggestions support inline threaded replies, or should follow-ups transition into regular Chats?
- **Options:**
  - *Option A (Recommended):* One-way suggestion with a *"Reply via Chat"* button that opens a normal direct conversation with context pre-quoted.
  - *Option B:* Build a separate full threaded reply engine inside the Suggestions tab.
- **Impact:** Option A avoids duplicating the entire chat engine inside Suggestions.
- **Blocks Implementation?** No.

### Decision 6: Message Editing and Deletion
- **The Question:** Can users edit or delete sent chat messages?
- **Options:**
  - *Option A (Recommended):* No edit/delete in initial release (preserves audit integrity in an academic compliance platform).
  - *Option B:* Soft deletion ("This message was deleted") within 5 minutes.
- **Impact:** Option A simplifies compliance and caching.
- **Blocks Implementation?** No.

### Decision 7: AI Chat Reactivation Strategy
- **The Question:** How should the dormant AI assistant be exposed if reactivated?
- **Options:**
  - *Option A (Recommended):* Kept dormant behind a feature flag or a dedicated "Ask AI" button in the header, preserving existing backend without cluttering the messenger.
  - *Option B:* Displayed as a permanent virtual contact ("Emmu AI Bot") at the top of the Chats list.
- **Impact:** Option A keeps the human messenger experience clean.
- **Blocks Implementation?** No.

### Decision 8: User Identity Display in Directory
- **The Question:** What fields should appear in the user directory search cards?
- **Recommendation:** Display Name, Role Badge, and School/Department abbreviation. Do not expose personal mobile numbers or employee ID.

### Decision 9: Group Chats & Attachments
- **The Question:** Are group channels or attachments required?
- **Recommendation:** Exclude from MVP. Maintain focus on 1-to-1 text messaging and contextual suggestions.

### Decision 10: Notification Retention / Archival
- **The Question:** How long are workflow notifications retained?
- **Recommendation:** Retain notifications for active academic cycle (1 year), auto-archiving items older than 365 days.

---

## 12. Implementation Roadmap & Phases

```text
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 0: Discovery & Architecture Finalization (THIS STEP - COMPLETE)   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 1: Role, Scope & Security Foundation                             │
│ - Finalize Trustee & School PC read-only scoping in auth service       │
│ - Flyway migration V24 (Conversations, Messages, Suggestions, Alerts)   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 2: Normal One-to-One Messaging (Chats)                           │
│ - Backend Conversation & Message services, DTOs, Controllers           │
│ - Frontend Chats list, user search directory, message bubble stream    │
│ - Unread badge tracking                                                │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 3: Contextual Suggestions & Analytics Integration                │
│ - Suggestion routing engine (Batch -> PC, Course -> CC)                │
│ - Suggestion submission API with strict scope verification             │
│ - Frontend "Have suggestions? Drop them" composer in Analytics         │
│ - Suggestions Inbox (Received & Sent) with back-links                  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 4: Workflow Notifications & Approval Integration                 │
│ - Event listener on ApprovalService state transitions                  │
│ - Notification generator with idempotency deduplication                │
│ - Notifications tab in Emmu with deep-link redirection                 │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│ PHASE 5: Profile Settings, UI Refinements & Real-Time Sync             │
│ - Profile Modal redesign (Info & Settings tabs only)                   │
│ - Emmu toggle (On/Off) & Icon choice (Mascot vs Messenger FAB)         │
│ - Transparent aperture launcher styling option                         │
│ - Polling / WebSocket event integration                                │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 13. Acceptance Test Plan

### 13.1 Normal Messaging (Chats)
- [ ] User can search for eligible colleagues by name or role.
- [ ] Clicking a search result creates or retrieves the single canonical conversation between the two users.
- [ ] Sending a text message persists it to the database with server timestamp.
- [ ] History loads in correct chronological order with smooth pagination.
- [ ] Message read state transitions from unread to read when peer opens conversation.
- [ ] An unauthenticated or unauthorized user attempting to read another user's conversation receives `403 Forbidden`.

### 13.2 Contextual Suggestions
- [ ] `TRUSTEE` can successfully submit suggestions to Director, School PC, HOD, PC, and CC.
- [ ] `TRUSTEE` attempting to perform academic write operations (e.g., approve ATR) is rejected with `403 Forbidden`.
- [ ] `SCHOOL_PROGRAMME_COORDINATOR` can send suggestions down their school hierarchy but is blocked from academic writes.
- [ ] Suggestion submitted from Programme Batch analytics resolves accurately to the assigned Programme Coordinator.
- [ ] Suggestion submitted from Course analytics resolves accurately to the assigned Course Coordinator.
- [ ] Client supplying an arbitrary `recipientId` that violates the hierarchy or assignment is rejected by the backend.
- [ ] Cross-school suggestion submission by non-governance roles is rejected with `403 Forbidden`.
- [ ] Submitting a suggestion causes zero side-effects to attainment calculations, marks, or approval states.
- [ ] Recipient clicking *"View in Analytics"* navigates directly to the matching analytics dashboard with parameters pre-loaded.

### 13.3 Workflow Notifications
- [ ] Course Coordinator submitting CO Targets generates a pending review notification for the assigned reviewer.
- [ ] Approver approving a submission generates an approval notification for the submitter.
- [ ] Approver requesting revision generates a revision notification containing reviewer remarks.
- [ ] Reprocessing or re-saving the same approval transition does not produce duplicate notifications.
- [ ] Clicking a notification deep-links directly to the relevant approval workspace.

### 13.4 UI & Profile Settings
- [ ] Profile modal renders **only** two tabs: **Info** and **Settings** (Security and Notifications tabs are absent).
- [ ] Turning off the Emmu Messenger toggle in Settings immediately hides the floating launcher.
- [ ] Switching between "Emmu Mascot" and "Standard Messenger Icon" immediately swaps the launcher UI and persists across page reloads.
- [ ] Enabling "Transparent" launcher mode removes the dark radial circle and border while preserving smooth character eye-tracking and peeking animations.
- [ ] The existing Three.js WebGL Genie animation continues to function seamlessly when opening/closing the chat modal.
- [ ] Total unread count badge on the launcher accurately reflects the sum of unread chats, suggestions, and notifications.

---

## 14. Confirmation of Non-Execution

In strict compliance with instructions:
- **No production code files have been modified.**
- **No database schemas or migrations have been applied.**
- **No dependencies have been installed.**
- **Existing AI integrations (`OllamaAiClient`, `EmmuEvidenceService`, `/ws/chat`) remain completely intact and functional.**
- **This document serves exclusively as the architectural blueprint for the subsequent implementation task.**
