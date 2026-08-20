--
-- PostgreSQL database dump
--

\restrict 1EogM61dvlBPHI7HcnW8nSK0MaadbjiDlYO4QlQLzvi7gX3kq0vrQVj2tcTZQfo

-- Dumped from database version 18.4 (Homebrew)
-- Dumped by pg_dump version 18.4 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


--
-- Name: EXTENSION pgcrypto; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pgcrypto IS 'cryptographic functions';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: approval_history; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.approval_history (
    id character varying(50) NOT NULL,
    approval_request_id character varying(50) NOT NULL,
    actor_id bigint,
    actor_name character varying(150) NOT NULL,
    actor_role character varying(50) NOT NULL,
    action character varying(50) NOT NULL,
    comments text,
    "timestamp" timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.approval_history OWNER TO rajshaikh;

--
-- Name: approval_requests; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.approval_requests (
    id character varying(50) NOT NULL,
    type character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    resource_id character varying(50) NOT NULL,
    school_id character varying(50),
    department_id character varying(50),
    programme_id character varying(50),
    batch_id character varying(50),
    course_id character varying(50),
    course_offering_id character varying(50),
    submitted_by character varying(150) NOT NULL,
    submitted_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    status character varying(30) DEFAULT 'PENDING'::character varying NOT NULL,
    approved_by character varying(150),
    approved_at timestamp with time zone,
    remarks text,
    details text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.approval_requests OWNER TO rajshaikh;

--
-- Name: attainment_configurations; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.attainment_configurations (
    id character varying(50) NOT NULL,
    course_offering_id character varying(50) NOT NULL,
    direct_weight numeric(5,2) DEFAULT 80.00 NOT NULL,
    indirect_weight numeric(5,2) DEFAULT 20.00 NOT NULL,
    direct_threshold numeric(5,2) DEFAULT 60.00 NOT NULL,
    indirect_threshold numeric(5,2) DEFAULT 60.00 NOT NULL,
    status character varying(30) DEFAULT 'DRAFT'::character varying NOT NULL,
    submitted_by character varying(150),
    submitted_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    direct_levels_json text,
    indirect_levels_json text
);


ALTER TABLE public.attainment_configurations OWNER TO rajshaikh;

--
-- Name: attainment_levels; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.attainment_levels (
    id character varying(50) NOT NULL,
    config_id character varying(50) NOT NULL,
    type character varying(20) NOT NULL,
    level_val integer NOT NULL,
    min_percentage numeric(5,2) NOT NULL,
    max_percentage numeric(5,2) NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_attainment_level_type CHECK (((type)::text = ANY ((ARRAY['DIRECT'::character varying, 'INDIRECT'::character varying])::text[]))),
    CONSTRAINT chk_attainment_level_value CHECK (((level_val >= 1) AND (level_val <= 3))),
    CONSTRAINT chk_attainment_percentage_range CHECK (((min_percentage >= (0)::numeric) AND (max_percentage <= (100)::numeric) AND (max_percentage >= min_percentage)))
);


ALTER TABLE public.attainment_levels OWNER TO rajshaikh;

--
-- Name: batches; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.batches (
    id character varying(50) NOT NULL,
    programme_id character varying(50) NOT NULL,
    programme_code character varying(20),
    programme_name character varying(255),
    duration_years integer DEFAULT 4 NOT NULL,
    name character varying(255) NOT NULL,
    start_year integer NOT NULL,
    end_year integer NOT NULL,
    previous_batch_id character varying(50),
    year_level character varying(100),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_batch_duration CHECK (((end_year - start_year) = duration_years)),
    CONSTRAINT chk_batch_year_range CHECK ((end_year > start_year))
);


ALTER TABLE public.batches OWNER TO rajshaikh;

--
-- Name: calculation_runs; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.calculation_runs (
    id character varying(50) NOT NULL,
    course_offering_id character varying(50),
    programme_id character varying(50),
    batch_id character varying(50),
    run_type character varying(30) NOT NULL,
    run_date timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    status character varying(30) DEFAULT 'COMPLETED'::character varying NOT NULL,
    executed_by character varying(150),
    CONSTRAINT chk_calculation_run_scope CHECK (((((run_type)::text = 'COURSE_CO'::text) AND (course_offering_id IS NOT NULL)) OR (((run_type)::text = 'PROGRAMME_PO_PSO'::text) AND (programme_id IS NOT NULL) AND (batch_id IS NOT NULL)))),
    CONSTRAINT chk_calculation_run_type CHECK (((run_type)::text = ANY ((ARRAY['COURSE_CO'::character varying, 'PROGRAMME_PO_PSO'::character varying])::text[])))
);


ALTER TABLE public.calculation_runs OWNER TO rajshaikh;

--
-- Name: cc_setup_progress; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.cc_setup_progress (
    id character varying(50) NOT NULL,
    course_offering_id character varying(50) NOT NULL,
    coordinator_email character varying(150),
    current_step integer DEFAULT 1 NOT NULL,
    overall_status character varying(50) DEFAULT 'IN_PROGRESS'::character varying NOT NULL,
    completed_steps character varying(500) DEFAULT ''::character varying,
    pending_steps character varying(500) DEFAULT 'cos,co_targets,co_mapping,direct,indirect,attainment,course_atr'::character varying,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.cc_setup_progress OWNER TO rajshaikh;

--
-- Name: co_po_mappings; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.co_po_mappings (
    id character varying(50) NOT NULL,
    course_outcome_id character varying(50) NOT NULL,
    po_code character varying(20) NOT NULL,
    mapping_level integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_co_po_mapping_level CHECK (((mapping_level >= 0) AND (mapping_level <= 3)))
);


ALTER TABLE public.co_po_mappings OWNER TO rajshaikh;

--
-- Name: co_pso_mappings; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.co_pso_mappings (
    id character varying(50) NOT NULL,
    course_outcome_id character varying(50) NOT NULL,
    pso_code character varying(20) NOT NULL,
    mapping_level integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_co_pso_mapping_level CHECK (((mapping_level >= 0) AND (mapping_level <= 3)))
);


ALTER TABLE public.co_pso_mappings OWNER TO rajshaikh;

--
-- Name: course_atrs; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.course_atrs (
    id character varying(50) NOT NULL,
    course_offering_id character varying(50) NOT NULL,
    co_code character varying(30) NOT NULL,
    title character varying(255),
    target_score numeric(4,2) NOT NULL,
    actual_score numeric(4,2) NOT NULL,
    pct_achieved numeric(5,2) NOT NULL,
    status character varying(50) NOT NULL,
    statement text,
    actions_json text,
    submitted_by character varying(150),
    submitted_at timestamp with time zone,
    verification_comments text,
    verified_at timestamp with time zone,
    verified_by character varying(150),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.course_atrs OWNER TO rajshaikh;

--
-- Name: course_co_targets; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.course_co_targets (
    id character varying(50) NOT NULL,
    course_id character varying(50) NOT NULL,
    co_code character varying(30) NOT NULL,
    target_value numeric(4,2) DEFAULT 2.50 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_course_co_target CHECK (((target_value >= (0)::numeric) AND (target_value <= (3)::numeric)))
);


ALTER TABLE public.course_co_targets OWNER TO rajshaikh;

--
-- Name: course_end_surveys; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.course_end_surveys (
    id character varying(50) NOT NULL,
    course_offering_id character varying(50) NOT NULL,
    total_respondents integer DEFAULT 0,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.course_end_surveys OWNER TO rajshaikh;

--
-- Name: course_mapping_keywords; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.course_mapping_keywords (
    id character varying(50) NOT NULL,
    course_offering_id character varying(50) NOT NULL,
    keyword_type character varying(20) NOT NULL,
    keywords_json text NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_keyword_type CHECK (((keyword_type)::text = ANY ((ARRAY['PO'::character varying, 'PSO'::character varying])::text[])))
);


ALTER TABLE public.course_mapping_keywords OWNER TO rajshaikh;

--
-- Name: course_offerings; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.course_offerings (
    id character varying(50) NOT NULL,
    course_id character varying(50) NOT NULL,
    batch_id character varying(50) NOT NULL,
    semester integer NOT NULL,
    course_coordinator_id bigint,
    course_coordinator_name character varying(255),
    assigned_faculty text,
    status character varying(30) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_offering_semester CHECK ((semester >= 1))
);


ALTER TABLE public.course_offerings OWNER TO rajshaikh;

--
-- Name: course_outcomes; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.course_outcomes (
    id character varying(50) NOT NULL,
    course_offering_id character varying(50) NOT NULL,
    code character varying(30) NOT NULL,
    statement text NOT NULL,
    target_level numeric(4,2) DEFAULT 2.50 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    blooms_level character varying(50) DEFAULT 'L3 - Apply'::character varying,
    CONSTRAINT chk_co_target_level CHECK (((target_level >= (0)::numeric) AND (target_level <= (3)::numeric)))
);


ALTER TABLE public.course_outcomes OWNER TO rajshaikh;

--
-- Name: courses; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.courses (
    id character varying(50) NOT NULL,
    code character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    programme_id character varying(50) NOT NULL,
    credits integer DEFAULT 4 NOT NULL,
    course_type character varying(50) DEFAULT 'CORE'::character varying NOT NULL,
    status character varying(30) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.courses OWNER TO rajshaikh;

--
-- Name: departments; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.departments (
    id character varying(50) NOT NULL,
    school_id character varying(50) NOT NULL,
    code character varying(20) NOT NULL,
    name character varying(255) NOT NULL,
    hod character varying(150),
    hod_email character varying(150),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.departments OWNER TO rajshaikh;

--
-- Name: direct_co_attainments; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.direct_co_attainments (
    id character varying(50) NOT NULL,
    run_id character varying(50) NOT NULL,
    course_offering_id character varying(50) NOT NULL,
    co_code character varying(30) NOT NULL,
    students_attempted integer DEFAULT 0 NOT NULL,
    students_attained integer DEFAULT 0 NOT NULL,
    percentage_attained numeric(5,2) DEFAULT 0.00 NOT NULL,
    attainment_level integer DEFAULT 1 NOT NULL,
    attainment_score numeric(4,2) DEFAULT 1.00 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.direct_co_attainments OWNER TO rajshaikh;

--
-- Name: director_setup_progress; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.director_setup_progress (
    id character varying(50) NOT NULL,
    school_id character varying(50) NOT NULL,
    current_step integer DEFAULT 1 NOT NULL,
    current_step_enum character varying(30) DEFAULT 'SCHOOL'::character varying NOT NULL,
    overall_status character varying(30) DEFAULT 'IN_PROGRESS'::character varying NOT NULL,
    completed_steps character varying(500) DEFAULT ''::character varying,
    pending_steps character varying(500) DEFAULT 'school,department,programme,review'::character varying,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.director_setup_progress OWNER TO rajshaikh;

--
-- Name: end_sem_marks_uploads; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.end_sem_marks_uploads (
    id character varying(50) NOT NULL,
    course_offering_id character varying(50) NOT NULL,
    file_name character varying(255) NOT NULL,
    file_path character varying(500) NOT NULL,
    uploaded_by character varying(150) NOT NULL,
    uploaded_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    record_count integer DEFAULT 0,
    status character varying(30) DEFAULT 'COMPLETED'::character varying NOT NULL
);


ALTER TABLE public.end_sem_marks_uploads OWNER TO rajshaikh;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


ALTER TABLE public.flyway_schema_history OWNER TO rajshaikh;

--
-- Name: hod_setup_progress; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.hod_setup_progress (
    id character varying(50) NOT NULL,
    department_id character varying(50) NOT NULL,
    hod_email character varying(150),
    current_step integer DEFAULT 1 NOT NULL,
    overall_status character varying(30) DEFAULT 'IN_PROGRESS'::character varying NOT NULL,
    completed_steps character varying(500) DEFAULT ''::character varying,
    pending_steps character varying(500) DEFAULT 'batch,outcomes,coordinators,review'::character varying,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.hod_setup_progress OWNER TO rajshaikh;

--
-- Name: indirect_co_attainments; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.indirect_co_attainments (
    id character varying(50) NOT NULL,
    run_id character varying(50) NOT NULL,
    course_offering_id character varying(50) NOT NULL,
    co_code character varying(30) NOT NULL,
    total_responses integer DEFAULT 0 NOT NULL,
    avg_survey_score numeric(4,2) DEFAULT 2.50 NOT NULL,
    percentage_attained numeric(5,2) DEFAULT 0.00 NOT NULL,
    attainment_level integer DEFAULT 1 NOT NULL,
    attainment_score numeric(4,2) DEFAULT 1.00 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.indirect_co_attainments OWNER TO rajshaikh;

--
-- Name: overall_co_attainments; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.overall_co_attainments (
    id character varying(50) NOT NULL,
    run_id character varying(50) NOT NULL,
    course_offering_id character varying(50) NOT NULL,
    co_code character varying(30) NOT NULL,
    direct_score numeric(4,2) DEFAULT 0.00 NOT NULL,
    indirect_score numeric(4,2) DEFAULT 0.00 NOT NULL,
    overall_attainment numeric(4,2) DEFAULT 0.00 NOT NULL,
    target_score numeric(4,2) DEFAULT 2.50 NOT NULL,
    is_target_achieved boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.overall_co_attainments OWNER TO rajshaikh;

--
-- Name: pc_setup_progress; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.pc_setup_progress (
    id character varying(50) NOT NULL,
    programme_id character varying(50) NOT NULL,
    batch_id character varying(50) NOT NULL,
    coordinator_email character varying(150),
    current_step integer DEFAULT 1 NOT NULL,
    overall_status character varying(50) DEFAULT 'IN_PROGRESS'::character varying NOT NULL,
    completed_steps character varying(500) DEFAULT ''::character varying,
    pending_steps character varying(500) DEFAULT 'courses,targets,review'::character varying,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.pc_setup_progress OWNER TO rajshaikh;

--
-- Name: peo_outcomes; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.peo_outcomes (
    id character varying(50) NOT NULL,
    programme_id character varying(50) NOT NULL,
    code character varying(20) NOT NULL,
    statement text NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.peo_outcomes OWNER TO rajshaikh;

--
-- Name: po_attainments; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.po_attainments (
    id character varying(50) NOT NULL,
    run_id character varying(50) NOT NULL,
    programme_id character varying(50) NOT NULL,
    batch_id character varying(50) NOT NULL,
    po_code character varying(20) NOT NULL,
    direct_attainment numeric(4,2) DEFAULT 0.00 NOT NULL,
    indirect_attainment numeric(4,2) DEFAULT 0.00 NOT NULL,
    final_attainment numeric(4,2) DEFAULT 0.00 NOT NULL,
    target_attainment numeric(4,2) DEFAULT 2.50 NOT NULL,
    is_target_achieved boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.po_attainments OWNER TO rajshaikh;

--
-- Name: po_competencies; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.po_competencies (
    id character varying(50) NOT NULL,
    po_id character varying(50) NOT NULL,
    code character varying(30) NOT NULL,
    statement text NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.po_competencies OWNER TO rajshaikh;

--
-- Name: programme_atrs; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.programme_atrs (
    id character varying(50) NOT NULL,
    programme_id character varying(50) NOT NULL,
    batch_id character varying(50) NOT NULL,
    status character varying(30) DEFAULT 'DRAFT'::character varying NOT NULL,
    submitted_by character varying(150),
    submitted_at timestamp with time zone,
    approved_by character varying(150),
    approved_at timestamp with time zone,
    verified_by character varying(150),
    verified_at timestamp with time zone,
    verification_comments text,
    observations_json text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.programme_atrs OWNER TO rajshaikh;

--
-- Name: programme_exit_surveys; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.programme_exit_surveys (
    id character varying(50) NOT NULL,
    programme_id character varying(50) NOT NULL,
    batch_id character varying(50) NOT NULL,
    total_respondents integer DEFAULT 0,
    avg_exit_score numeric(4,2) DEFAULT 2.50,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.programme_exit_surveys OWNER TO rajshaikh;

--
-- Name: programme_outcomes; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.programme_outcomes (
    id character varying(50) NOT NULL,
    programme_id character varying(50) NOT NULL,
    code character varying(20) NOT NULL,
    statement text NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.programme_outcomes OWNER TO rajshaikh;

--
-- Name: programme_specific_outcomes; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.programme_specific_outcomes (
    id character varying(50) NOT NULL,
    programme_id character varying(50) NOT NULL,
    code character varying(20) NOT NULL,
    statement text NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.programme_specific_outcomes OWNER TO rajshaikh;

--
-- Name: programme_targets; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.programme_targets (
    id character varying(50) NOT NULL,
    batch_id character varying(50) NOT NULL,
    outcome_type character varying(10) DEFAULT 'PO'::character varying NOT NULL,
    outcome_code character varying(20) NOT NULL,
    target_value numeric(4,2) DEFAULT 2.50 NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_programme_target CHECK (((target_value >= (0)::numeric) AND (target_value <= (3)::numeric)))
);


ALTER TABLE public.programme_targets OWNER TO rajshaikh;

--
-- Name: programmes; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.programmes (
    id character varying(50) NOT NULL,
    department_id character varying(50) NOT NULL,
    code character varying(20) NOT NULL,
    name character varying(255) NOT NULL,
    duration_years integer DEFAULT 4 NOT NULL,
    department_name character varying(255),
    coordinator character varying(150),
    coordinator_email character varying(150),
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.programmes OWNER TO rajshaikh;

--
-- Name: pso_attainments; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.pso_attainments (
    id character varying(50) NOT NULL,
    run_id character varying(50) NOT NULL,
    programme_id character varying(50) NOT NULL,
    batch_id character varying(50) NOT NULL,
    pso_code character varying(20) NOT NULL,
    direct_attainment numeric(4,2) DEFAULT 0.00 NOT NULL,
    indirect_attainment numeric(4,2) DEFAULT 0.00 NOT NULL,
    final_attainment numeric(4,2) DEFAULT 0.00 NOT NULL,
    target_attainment numeric(4,2) DEFAULT 2.50 NOT NULL,
    is_target_achieved boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.pso_attainments OWNER TO rajshaikh;

--
-- Name: pso_competencies; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.pso_competencies (
    id character varying(50) NOT NULL,
    pso_id character varying(50) NOT NULL,
    code character varying(30) NOT NULL,
    statement text NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.pso_competencies OWNER TO rajshaikh;

--
-- Name: schools; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.schools (
    id character varying(50) NOT NULL,
    code character varying(20) NOT NULL,
    name character varying(255) NOT NULL,
    director_id bigint,
    director_name character varying(255),
    director character varying(150),
    director_email character varying(150),
    est_year character varying(10),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.schools OWNER TO rajshaikh;

--
-- Name: semesters; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.semesters (
    id character varying(50) NOT NULL,
    batch_id character varying(50) NOT NULL,
    semester_num integer NOT NULL,
    name character varying(50) NOT NULL,
    status character varying(20) DEFAULT 'ACTIVE'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_semester_number CHECK ((semester_num >= 1))
);


ALTER TABLE public.semesters OWNER TO rajshaikh;

--
-- Name: student_co_marks; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.student_co_marks (
    id character varying(50) NOT NULL,
    upload_id character varying(50),
    course_offering_id character varying(50) NOT NULL,
    student_id character varying(50) NOT NULL,
    prn character varying(50) NOT NULL,
    student_name character varying(150),
    co_code character varying(30) NOT NULL,
    marks_obtained numeric(8,2) NOT NULL,
    max_marks numeric(8,2) DEFAULT 100.00 NOT NULL,
    percentage numeric(5,2),
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.student_co_marks OWNER TO rajshaikh;

--
-- Name: students; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.students (
    id character varying(50) NOT NULL,
    batch_id character varying(50) NOT NULL,
    prn character varying(50) NOT NULL,
    name character varying(150) NOT NULL,
    email character varying(150) NOT NULL,
    status character varying(20) DEFAULT 'ENROLLED'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.students OWNER TO rajshaikh;

--
-- Name: survey_response_details; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.survey_response_details (
    id character varying(50) NOT NULL,
    response_id character varying(50) NOT NULL,
    co_code character varying(30) NOT NULL,
    rating integer NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_survey_rating CHECK (((rating >= 1) AND (rating <= 3)))
);


ALTER TABLE public.survey_response_details OWNER TO rajshaikh;

--
-- Name: survey_responses; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.survey_responses (
    id character varying(50) NOT NULL,
    survey_id character varying(50) NOT NULL,
    student_id character varying(50),
    prn character varying(50),
    submitted_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.survey_responses OWNER TO rajshaikh;

--
-- Name: uploaded_documents; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.uploaded_documents (
    id character varying(255) NOT NULL,
    batch_id character varying(50) NOT NULL,
    course_offering_id character varying(50),
    document_type character varying(50) NOT NULL,
    file_name character varying(255) NOT NULL,
    saved_file_name character varying(255) NOT NULL,
    saved_path character varying(500) NOT NULL,
    file_size bigint,
    records_processed integer,
    threshold_percentage numeric(5,2),
    uploaded_by character varying(150),
    uploaded_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.uploaded_documents OWNER TO rajshaikh;

--
-- Name: users; Type: TABLE; Schema: public; Owner: rajshaikh
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    username character varying(100) NOT NULL,
    email character varying(150) NOT NULL,
    password_hash character varying(255) NOT NULL,
    name character varying(150) NOT NULL,
    role character varying(50) NOT NULL,
    department character varying(255),
    programme character varying(255),
    school_id character varying(50),
    department_id character varying(50),
    programme_id character varying(50),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.users OWNER TO rajshaikh;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: rajshaikh
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.users_id_seq OWNER TO rajshaikh;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: rajshaikh
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Data for Name: approval_history; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.approval_history (id, approval_request_id, actor_id, actor_name, actor_role, action, comments, "timestamp") FROM stdin;
\.


--
-- Data for Name: approval_requests; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.approval_requests (id, type, title, resource_id, school_id, department_id, programme_id, batch_id, course_id, course_offering_id, submitted_by, submitted_at, status, approved_by, approved_at, remarks, details, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: attainment_configurations; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.attainment_configurations (id, course_offering_id, direct_weight, indirect_weight, direct_threshold, indirect_threshold, status, submitted_by, submitted_at, created_at, updated_at, direct_levels_json, indirect_levels_json) FROM stdin;
\.


--
-- Data for Name: attainment_levels; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.attainment_levels (id, config_id, type, level_val, min_percentage, max_percentage, created_at) FROM stdin;
\.


--
-- Data for Name: batches; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.batches (id, programme_id, programme_code, programme_name, duration_years, name, start_year, end_year, previous_batch_id, year_level, status, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: calculation_runs; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.calculation_runs (id, course_offering_id, programme_id, batch_id, run_type, run_date, status, executed_by) FROM stdin;
\.


--
-- Data for Name: cc_setup_progress; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.cc_setup_progress (id, course_offering_id, coordinator_email, current_step, overall_status, completed_steps, pending_steps, updated_at) FROM stdin;
\.


--
-- Data for Name: co_po_mappings; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.co_po_mappings (id, course_outcome_id, po_code, mapping_level, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: co_pso_mappings; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.co_pso_mappings (id, course_outcome_id, pso_code, mapping_level, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: course_atrs; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.course_atrs (id, course_offering_id, co_code, title, target_score, actual_score, pct_achieved, status, statement, actions_json, submitted_by, submitted_at, verification_comments, verified_at, verified_by, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: course_co_targets; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.course_co_targets (id, course_id, co_code, target_value, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: course_end_surveys; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.course_end_surveys (id, course_offering_id, total_respondents, created_at) FROM stdin;
\.


--
-- Data for Name: course_mapping_keywords; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.course_mapping_keywords (id, course_offering_id, keyword_type, keywords_json, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: course_offerings; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.course_offerings (id, course_id, batch_id, semester, course_coordinator_id, course_coordinator_name, assigned_faculty, status, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: course_outcomes; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.course_outcomes (id, course_offering_id, code, statement, target_level, created_at, updated_at, blooms_level) FROM stdin;
\.


--
-- Data for Name: courses; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.courses (id, code, name, programme_id, credits, course_type, status, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: departments; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.departments (id, school_id, code, name, hod, hod_email, status, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: direct_co_attainments; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.direct_co_attainments (id, run_id, course_offering_id, co_code, students_attempted, students_attained, percentage_attained, attainment_level, attainment_score, created_at) FROM stdin;
\.


--
-- Data for Name: director_setup_progress; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.director_setup_progress (id, school_id, current_step, current_step_enum, overall_status, completed_steps, pending_steps, updated_at) FROM stdin;
\.


--
-- Data for Name: end_sem_marks_uploads; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.end_sem_marks_uploads (id, course_offering_id, file_name, file_path, uploaded_by, uploaded_at, record_count, status) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	0	<< Flyway Baseline >>	BASELINE	<< Flyway Baseline >>	\N	rajshaikh	2026-08-19 17:42:26.773684	0	t
2	1	create batch centric academic schema	SQL	V1__create_batch_centric_academic_schema.sql	1661571450	rajshaikh	2026-08-19 17:45:00.636401	85	t
3	2	create outcome and mapping schema	SQL	V2__create_outcome_and_mapping_schema.sql	-1929578907	rajshaikh	2026-08-19 17:45:00.772829	46	t
4	3	create attainment and assessment schema	SQL	V3__create_attainment_and_assessment_schema.sql	-1998607352	rajshaikh	2026-08-19 17:45:00.828439	75	t
5	4	create atr approval and workflow schema	SQL	V4__create_atr_approval_and_workflow_schema.sql	449196016	rajshaikh	2026-08-19 17:45:00.913192	21	t
6	5	add blooms level and verification enhancements	SQL	V5__add_blooms_level_and_verification_enhancements.sql	-663298917	rajshaikh	2026-08-19 17:45:00.941952	2	t
\.


--
-- Data for Name: hod_setup_progress; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.hod_setup_progress (id, department_id, hod_email, current_step, overall_status, completed_steps, pending_steps, updated_at) FROM stdin;
\.


--
-- Data for Name: indirect_co_attainments; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.indirect_co_attainments (id, run_id, course_offering_id, co_code, total_responses, avg_survey_score, percentage_attained, attainment_level, attainment_score, created_at) FROM stdin;
\.


--
-- Data for Name: overall_co_attainments; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.overall_co_attainments (id, run_id, course_offering_id, co_code, direct_score, indirect_score, overall_attainment, target_score, is_target_achieved, created_at) FROM stdin;
\.


--
-- Data for Name: pc_setup_progress; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.pc_setup_progress (id, programme_id, batch_id, coordinator_email, current_step, overall_status, completed_steps, pending_steps, updated_at) FROM stdin;
\.


--
-- Data for Name: peo_outcomes; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.peo_outcomes (id, programme_id, code, statement, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: po_attainments; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.po_attainments (id, run_id, programme_id, batch_id, po_code, direct_attainment, indirect_attainment, final_attainment, target_attainment, is_target_achieved, created_at) FROM stdin;
\.


--
-- Data for Name: po_competencies; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.po_competencies (id, po_id, code, statement, created_at) FROM stdin;
\.


--
-- Data for Name: programme_atrs; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.programme_atrs (id, programme_id, batch_id, status, submitted_by, submitted_at, approved_by, approved_at, verified_by, verified_at, verification_comments, observations_json, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: programme_exit_surveys; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.programme_exit_surveys (id, programme_id, batch_id, total_respondents, avg_exit_score, created_at) FROM stdin;
\.


--
-- Data for Name: programme_outcomes; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.programme_outcomes (id, programme_id, code, statement, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: programme_specific_outcomes; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.programme_specific_outcomes (id, programme_id, code, statement, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: programme_targets; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.programme_targets (id, batch_id, outcome_type, outcome_code, target_value, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: programmes; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.programmes (id, department_id, code, name, duration_years, department_name, coordinator, coordinator_email, status, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: pso_attainments; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.pso_attainments (id, run_id, programme_id, batch_id, pso_code, direct_attainment, indirect_attainment, final_attainment, target_attainment, is_target_achieved, created_at) FROM stdin;
\.


--
-- Data for Name: pso_competencies; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.pso_competencies (id, pso_id, code, statement, created_at) FROM stdin;
\.


--
-- Data for Name: schools; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.schools (id, code, name, director_id, director_name, director, director_email, est_year, created_at, updated_at) FROM stdin;
sch-dypiu-1787113986131	DYPIU	DY Patil International University	\N	\N	\N	\N	2020	2026-08-19 10:03:06.220707+05:30	2026-08-19 10:03:06.220707+05:30
sch-08089940	SOEMR	School of Engineering	1	Dr. Raj Shaikh	Dr. Raj Shaikh	director1@gmail.com	2021	2026-08-18 19:04:23.6569+05:30	2026-08-18 19:29:50.143893+05:30
sch-dypiemr	DYP	DY Patil Institute of Engineering Management and Research	2	adam	adam	director2@gmail.com	2012	2026-08-19 03:33:02.227955+05:30	2026-08-19 03:33:02.227955+05:30
\.


--
-- Data for Name: semesters; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.semesters (id, batch_id, semester_num, name, status, created_at) FROM stdin;
\.


--
-- Data for Name: student_co_marks; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.student_co_marks (id, upload_id, course_offering_id, student_id, prn, student_name, co_code, marks_obtained, max_marks, percentage, created_at) FROM stdin;
\.


--
-- Data for Name: students; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.students (id, batch_id, prn, name, email, status, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: survey_response_details; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.survey_response_details (id, response_id, co_code, rating, created_at) FROM stdin;
\.


--
-- Data for Name: survey_responses; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.survey_responses (id, survey_id, student_id, prn, submitted_at) FROM stdin;
\.


--
-- Data for Name: uploaded_documents; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.uploaded_documents (id, batch_id, course_offering_id, document_type, file_name, saved_file_name, saved_path, file_size, records_processed, threshold_percentage, uploaded_by, uploaded_at) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: rajshaikh
--

COPY public.users (id, username, email, password_hash, name, role, department, programme, school_id, department_id, programme_id, is_active, created_at, updated_at) FROM stdin;
2	director2	director2@gmail.com	$2a$10$8R6k7ZBaP1DdXrDUxGpVCet91yk0YNP.ar6W5WFIxI6.frFTfJGs6	adam	DIRECTOR	\N	\N	sch-dypiemr	\N	\N	t	2026-08-18 18:49:32.858466+05:30	2026-08-19 03:33:18.802882+05:30
16232	admin	admin@dypiu.ac.in	$2a$10$uqoq388T/Q1LTqjY7MyWq.1U.J6RLk9TeU3q6US98lx2hY2DRkkhK	System Administrator	ADMIN	\N	\N	\N	\N	\N	t	2026-08-19 03:35:55.834667+05:30	2026-08-19 03:35:55.834667+05:30
5	pc1	pc1@gmail.com	$2a$10$RfTAJkxsyY6D1uuSipRe8eP7KkUmO38TWeM3WUn4qELp1CtWyhYVm	prasad	PROGRAMME_COORDINATOR	Dept of Computer Science	B.Tech Computer Science and Engineering	sch-08089940	dept-ecac9e2f	prog-mtech-cse-01	t	2026-08-18 18:50:43.745567+05:30	2026-08-18 18:50:43.745567+05:30
19488	hod3	hod3@gmail.com	$2a$10$ACD/Z0Tp5VeZGdA.stdaoemOq6NHIjFq.eex2pN9NLienEqqDHone	Krish	HOD	\N	\N	sch-dypiu-1787113986131	\N	\N	t	2026-08-19 10:07:06.127045+05:30	2026-08-19 10:07:06.127045+05:30
4	hod2	hod2@gmail.com	$2a$10$OB/Y5tnXMpA9gwJdS05m4e9zQgtipgK7qnZ39cnuQ4vflkFqNE8oi	prag	HOD	Dept of Artificial Intelligence	\N	sch-08089940	\N	\N	t	2026-08-18 18:50:16.537734+05:30	2026-08-18 18:50:16.537734+05:30
7	cc1	cc1@gmail.com	$2a$10$aZWykqqntBKZFqV91E2Dbe211PY0U/4wnoms8OT7YOAysdQ3ylITW	ayush	FACULTY	\N	\N	\N	\N	\N	t	2026-08-18 18:52:03.087598+05:30	2026-08-18 18:52:03.087598+05:30
8	cc2	cc2@gmail.com	$2a$10$4H.30LxxMKWPDoHHxfvNauYR8gFpB30qKDTcL5kq1CEQvaAUajLoi	ruhan	FACULTY	\N	\N	\N	\N	\N	t	2026-08-18 18:52:27.238648+05:30	2026-08-18 18:52:27.238648+05:30
20791	pc3	pc3@gmail.com	$2a$10$HYhw3xHzRFIJkBCui6H6Quh/9/Y.2BTQEhhSKTP/yeBpiohYgcoCe	POZA	PROGRAMME_COORDINATOR	\N	\N	sch-dypiu-1787113986131	\N	\N	t	2026-08-19 10:29:01.634982+05:30	2026-08-19 10:29:01.634982+05:30
1	director1	director1@gmail.com	$2a$10$8DmgEqh9Dq2uJf4R896Fs.0T26/IsV4IMNPzDqtdeY.gys15.VWBy	raj	DIRECTOR	\N	\N	sch-08089940	\N	\N	t	2026-08-18 18:49:07.217467+05:30	2026-08-18 18:49:07.217467+05:30
6	pc2	pc2@gmail.com	$2a$10$YrqSz/lNYqTfBjZZ179qUuO1lW5FCImha.PZ8QyIqGFCjSL7fEHjG	sujal	PROGRAMME_COORDINATOR	Dept of Artificial Intelligence	B.Tech Artificial Intelligence and Data Science	sch-08089940	dept-3796a613	prog-btech-aids-01	t	2026-08-18 18:51:40.439262+05:30	2026-08-18 18:51:40.439262+05:30
3	hod1	hod1@gmail.com	$2a$10$OgXd6GHzOsdNo2l3vlP1Q.LxzWIXc7Fn03OEJansctxW.9XIV8OXe	sam	HOD	Dept of Computer Science	\N	sch-08089940	dept-ecac9e2f	\N	t	2026-08-18 18:49:56.235522+05:30	2026-08-18 18:49:56.235522+05:30
\.


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: rajshaikh
--

SELECT pg_catalog.setval('public.users_id_seq', 25999, true);


--
-- Name: approval_history approval_history_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.approval_history
    ADD CONSTRAINT approval_history_pkey PRIMARY KEY (id);


--
-- Name: approval_requests approval_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.approval_requests
    ADD CONSTRAINT approval_requests_pkey PRIMARY KEY (id);


--
-- Name: attainment_configurations attainment_configurations_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.attainment_configurations
    ADD CONSTRAINT attainment_configurations_pkey PRIMARY KEY (id);


--
-- Name: attainment_levels attainment_levels_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.attainment_levels
    ADD CONSTRAINT attainment_levels_pkey PRIMARY KEY (id);


--
-- Name: batches batches_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.batches
    ADD CONSTRAINT batches_pkey PRIMARY KEY (id);


--
-- Name: calculation_runs calculation_runs_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.calculation_runs
    ADD CONSTRAINT calculation_runs_pkey PRIMARY KEY (id);


--
-- Name: cc_setup_progress cc_setup_progress_course_offering_id_key; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.cc_setup_progress
    ADD CONSTRAINT cc_setup_progress_course_offering_id_key UNIQUE (course_offering_id);


--
-- Name: cc_setup_progress cc_setup_progress_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.cc_setup_progress
    ADD CONSTRAINT cc_setup_progress_pkey PRIMARY KEY (id);


--
-- Name: co_po_mappings co_po_mappings_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.co_po_mappings
    ADD CONSTRAINT co_po_mappings_pkey PRIMARY KEY (id);


--
-- Name: co_pso_mappings co_pso_mappings_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.co_pso_mappings
    ADD CONSTRAINT co_pso_mappings_pkey PRIMARY KEY (id);


--
-- Name: course_atrs course_atrs_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_atrs
    ADD CONSTRAINT course_atrs_pkey PRIMARY KEY (id);


--
-- Name: course_co_targets course_co_targets_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_co_targets
    ADD CONSTRAINT course_co_targets_pkey PRIMARY KEY (id);


--
-- Name: course_end_surveys course_end_surveys_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_end_surveys
    ADD CONSTRAINT course_end_surveys_pkey PRIMARY KEY (id);


--
-- Name: course_mapping_keywords course_mapping_keywords_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_mapping_keywords
    ADD CONSTRAINT course_mapping_keywords_pkey PRIMARY KEY (id);


--
-- Name: course_offerings course_offerings_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_offerings
    ADD CONSTRAINT course_offerings_pkey PRIMARY KEY (id);


--
-- Name: course_outcomes course_outcomes_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_outcomes
    ADD CONSTRAINT course_outcomes_pkey PRIMARY KEY (id);


--
-- Name: courses courses_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_pkey PRIMARY KEY (id);


--
-- Name: departments departments_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_pkey PRIMARY KEY (id);


--
-- Name: direct_co_attainments direct_co_attainments_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.direct_co_attainments
    ADD CONSTRAINT direct_co_attainments_pkey PRIMARY KEY (id);


--
-- Name: director_setup_progress director_setup_progress_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.director_setup_progress
    ADD CONSTRAINT director_setup_progress_pkey PRIMARY KEY (id);


--
-- Name: director_setup_progress director_setup_progress_school_id_key; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.director_setup_progress
    ADD CONSTRAINT director_setup_progress_school_id_key UNIQUE (school_id);


--
-- Name: end_sem_marks_uploads end_sem_marks_uploads_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.end_sem_marks_uploads
    ADD CONSTRAINT end_sem_marks_uploads_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: hod_setup_progress hod_setup_progress_department_id_key; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.hod_setup_progress
    ADD CONSTRAINT hod_setup_progress_department_id_key UNIQUE (department_id);


--
-- Name: hod_setup_progress hod_setup_progress_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.hod_setup_progress
    ADD CONSTRAINT hod_setup_progress_pkey PRIMARY KEY (id);


--
-- Name: indirect_co_attainments indirect_co_attainments_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.indirect_co_attainments
    ADD CONSTRAINT indirect_co_attainments_pkey PRIMARY KEY (id);


--
-- Name: overall_co_attainments overall_co_attainments_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.overall_co_attainments
    ADD CONSTRAINT overall_co_attainments_pkey PRIMARY KEY (id);


--
-- Name: pc_setup_progress pc_setup_progress_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.pc_setup_progress
    ADD CONSTRAINT pc_setup_progress_pkey PRIMARY KEY (id);


--
-- Name: peo_outcomes peo_outcomes_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.peo_outcomes
    ADD CONSTRAINT peo_outcomes_pkey PRIMARY KEY (id);


--
-- Name: po_attainments po_attainments_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.po_attainments
    ADD CONSTRAINT po_attainments_pkey PRIMARY KEY (id);


--
-- Name: po_competencies po_competencies_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.po_competencies
    ADD CONSTRAINT po_competencies_pkey PRIMARY KEY (id);


--
-- Name: programme_atrs programme_atrs_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_atrs
    ADD CONSTRAINT programme_atrs_pkey PRIMARY KEY (id);


--
-- Name: programme_exit_surveys programme_exit_surveys_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_exit_surveys
    ADD CONSTRAINT programme_exit_surveys_pkey PRIMARY KEY (id);


--
-- Name: programme_outcomes programme_outcomes_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_outcomes
    ADD CONSTRAINT programme_outcomes_pkey PRIMARY KEY (id);


--
-- Name: programme_specific_outcomes programme_specific_outcomes_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_specific_outcomes
    ADD CONSTRAINT programme_specific_outcomes_pkey PRIMARY KEY (id);


--
-- Name: programme_targets programme_targets_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_targets
    ADD CONSTRAINT programme_targets_pkey PRIMARY KEY (id);


--
-- Name: programmes programmes_code_key; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programmes
    ADD CONSTRAINT programmes_code_key UNIQUE (code);


--
-- Name: programmes programmes_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programmes
    ADD CONSTRAINT programmes_pkey PRIMARY KEY (id);


--
-- Name: pso_attainments pso_attainments_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.pso_attainments
    ADD CONSTRAINT pso_attainments_pkey PRIMARY KEY (id);


--
-- Name: pso_competencies pso_competencies_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.pso_competencies
    ADD CONSTRAINT pso_competencies_pkey PRIMARY KEY (id);


--
-- Name: schools schools_code_key; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.schools
    ADD CONSTRAINT schools_code_key UNIQUE (code);


--
-- Name: schools schools_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.schools
    ADD CONSTRAINT schools_pkey PRIMARY KEY (id);


--
-- Name: semesters semesters_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.semesters
    ADD CONSTRAINT semesters_pkey PRIMARY KEY (id);


--
-- Name: student_co_marks student_co_marks_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.student_co_marks
    ADD CONSTRAINT student_co_marks_pkey PRIMARY KEY (id);


--
-- Name: students students_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_pkey PRIMARY KEY (id);


--
-- Name: students students_prn_key; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_prn_key UNIQUE (prn);


--
-- Name: survey_response_details survey_response_details_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.survey_response_details
    ADD CONSTRAINT survey_response_details_pkey PRIMARY KEY (id);


--
-- Name: survey_responses survey_responses_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.survey_responses
    ADD CONSTRAINT survey_responses_pkey PRIMARY KEY (id);


--
-- Name: course_offerings uk_batch_course_sem; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_offerings
    ADD CONSTRAINT uk_batch_course_sem UNIQUE (batch_id, course_id, semester);


--
-- Name: course_atrs uk_offering_co_atr; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_atrs
    ADD CONSTRAINT uk_offering_co_atr UNIQUE (course_offering_id, co_code);


--
-- Name: course_outcomes uk_offering_co_code; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_outcomes
    ADD CONSTRAINT uk_offering_co_code UNIQUE (course_offering_id, code);


--
-- Name: pc_setup_progress uk_pc_setup_programme_batch; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.pc_setup_progress
    ADD CONSTRAINT uk_pc_setup_programme_batch UNIQUE (programme_id, batch_id);


--
-- Name: programme_atrs uk_programme_batch_atr; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_atrs
    ADD CONSTRAINT uk_programme_batch_atr UNIQUE (programme_id, batch_id);


--
-- Name: uploaded_documents uploaded_documents_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.uploaded_documents
    ADD CONSTRAINT uploaded_documents_pkey PRIMARY KEY (id);


--
-- Name: attainment_configurations uq_attainment_config_offering; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.attainment_configurations
    ADD CONSTRAINT uq_attainment_config_offering UNIQUE (course_offering_id);


--
-- Name: attainment_levels uq_attainment_level; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.attainment_levels
    ADD CONSTRAINT uq_attainment_level UNIQUE (config_id, type, level_val);


--
-- Name: programme_targets uq_batch_outcome_target; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_targets
    ADD CONSTRAINT uq_batch_outcome_target UNIQUE (batch_id, outcome_type, outcome_code);


--
-- Name: semesters uq_batch_semester; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.semesters
    ADD CONSTRAINT uq_batch_semester UNIQUE (batch_id, semester_num);


--
-- Name: co_po_mappings uq_co_po; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.co_po_mappings
    ADD CONSTRAINT uq_co_po UNIQUE (course_outcome_id, po_code);


--
-- Name: co_pso_mappings uq_co_pso; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.co_pso_mappings
    ADD CONSTRAINT uq_co_pso UNIQUE (course_outcome_id, pso_code);


--
-- Name: course_co_targets uq_course_co_target; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_co_targets
    ADD CONSTRAINT uq_course_co_target UNIQUE (course_id, co_code);


--
-- Name: course_end_surveys uq_course_end_survey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_end_surveys
    ADD CONSTRAINT uq_course_end_survey UNIQUE (course_offering_id);


--
-- Name: courses uq_course_programme_code; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT uq_course_programme_code UNIQUE (programme_id, code);


--
-- Name: departments uq_department_school_code; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT uq_department_school_code UNIQUE (school_id, code);


--
-- Name: course_mapping_keywords uq_offering_keyword_type; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_mapping_keywords
    ADD CONSTRAINT uq_offering_keyword_type UNIQUE (course_offering_id, keyword_type);


--
-- Name: po_competencies uq_po_competency; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.po_competencies
    ADD CONSTRAINT uq_po_competency UNIQUE (po_id, code);


--
-- Name: programme_exit_surveys uq_programme_exit_survey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_exit_surveys
    ADD CONSTRAINT uq_programme_exit_survey UNIQUE (programme_id, batch_id);


--
-- Name: peo_outcomes uq_programme_peo; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.peo_outcomes
    ADD CONSTRAINT uq_programme_peo UNIQUE (programme_id, code);


--
-- Name: programme_outcomes uq_programme_po; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_outcomes
    ADD CONSTRAINT uq_programme_po UNIQUE (programme_id, code);


--
-- Name: programme_specific_outcomes uq_programme_pso; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_specific_outcomes
    ADD CONSTRAINT uq_programme_pso UNIQUE (programme_id, code);


--
-- Name: pso_competencies uq_pso_competency; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.pso_competencies
    ADD CONSTRAINT uq_pso_competency UNIQUE (pso_id, code);


--
-- Name: direct_co_attainments uq_run_direct_co; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.direct_co_attainments
    ADD CONSTRAINT uq_run_direct_co UNIQUE (run_id, co_code);


--
-- Name: indirect_co_attainments uq_run_indirect_co; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.indirect_co_attainments
    ADD CONSTRAINT uq_run_indirect_co UNIQUE (run_id, co_code);


--
-- Name: overall_co_attainments uq_run_overall_co; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.overall_co_attainments
    ADD CONSTRAINT uq_run_overall_co UNIQUE (run_id, co_code);


--
-- Name: po_attainments uq_run_po; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.po_attainments
    ADD CONSTRAINT uq_run_po UNIQUE (run_id, po_code);


--
-- Name: pso_attainments uq_run_pso; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.pso_attainments
    ADD CONSTRAINT uq_run_pso UNIQUE (run_id, pso_code);


--
-- Name: student_co_marks uq_student_co_mark; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.student_co_marks
    ADD CONSTRAINT uq_student_co_mark UNIQUE (course_offering_id, student_id, co_code);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_approval_history_request; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_approval_history_request ON public.approval_history USING btree (approval_request_id);


--
-- Name: idx_approval_requests_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_approval_requests_offering ON public.approval_requests USING btree (course_offering_id);


--
-- Name: idx_approval_requests_programme_batch; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_approval_requests_programme_batch ON public.approval_requests USING btree (programme_id, batch_id);


--
-- Name: idx_approval_requests_resource; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_approval_requests_resource ON public.approval_requests USING btree (resource_id);


--
-- Name: idx_approval_requests_status; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_approval_requests_status ON public.approval_requests USING btree (status);


--
-- Name: idx_attainment_config_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_attainment_config_offering ON public.attainment_configurations USING btree (course_offering_id);


--
-- Name: idx_batches_previous; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_batches_previous ON public.batches USING btree (previous_batch_id);


--
-- Name: idx_batches_programme; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_batches_programme ON public.batches USING btree (programme_id);


--
-- Name: idx_calc_runs_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_calc_runs_offering ON public.calculation_runs USING btree (course_offering_id);


--
-- Name: idx_calc_runs_programme_batch; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_calc_runs_programme_batch ON public.calculation_runs USING btree (programme_id, batch_id);


--
-- Name: idx_cc_setup_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_cc_setup_offering ON public.cc_setup_progress USING btree (course_offering_id);


--
-- Name: idx_co_po_mapping_co; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_co_po_mapping_co ON public.co_po_mappings USING btree (course_outcome_id);


--
-- Name: idx_co_pso_mapping_co; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_co_pso_mapping_co ON public.co_pso_mappings USING btree (course_outcome_id);


--
-- Name: idx_course_atr_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_course_atr_offering ON public.course_atrs USING btree (course_offering_id);


--
-- Name: idx_course_co_targets_course; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_course_co_targets_course ON public.course_co_targets USING btree (course_id);


--
-- Name: idx_course_offerings_batch; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_course_offerings_batch ON public.course_offerings USING btree (batch_id);


--
-- Name: idx_course_offerings_coordinator; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_course_offerings_coordinator ON public.course_offerings USING btree (course_coordinator_id);


--
-- Name: idx_course_offerings_course; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_course_offerings_course ON public.course_offerings USING btree (course_id);


--
-- Name: idx_course_outcomes_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_course_outcomes_offering ON public.course_outcomes USING btree (course_offering_id);


--
-- Name: idx_course_surveys_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_course_surveys_offering ON public.course_end_surveys USING btree (course_offering_id);


--
-- Name: idx_courses_programme; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_courses_programme ON public.courses USING btree (programme_id);


--
-- Name: idx_departments_school; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_departments_school ON public.departments USING btree (school_id);


--
-- Name: idx_direct_co_attainment_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_direct_co_attainment_offering ON public.direct_co_attainments USING btree (course_offering_id);


--
-- Name: idx_director_setup_school; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_director_setup_school ON public.director_setup_progress USING btree (school_id);


--
-- Name: idx_hod_setup_department; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_hod_setup_department ON public.hod_setup_progress USING btree (department_id);


--
-- Name: idx_indirect_co_attainment_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_indirect_co_attainment_offering ON public.indirect_co_attainments USING btree (course_offering_id);


--
-- Name: idx_mapping_keywords_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_mapping_keywords_offering ON public.course_mapping_keywords USING btree (course_offering_id);


--
-- Name: idx_marks_upload_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_marks_upload_offering ON public.end_sem_marks_uploads USING btree (course_offering_id);


--
-- Name: idx_overall_co_attainment_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_overall_co_attainment_offering ON public.overall_co_attainments USING btree (course_offering_id);


--
-- Name: idx_pc_setup_programme_batch; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_pc_setup_programme_batch ON public.pc_setup_progress USING btree (programme_id, batch_id);


--
-- Name: idx_peo_outcomes_programme; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_peo_outcomes_programme ON public.peo_outcomes USING btree (programme_id);


--
-- Name: idx_po_attainment_programme_batch; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_po_attainment_programme_batch ON public.po_attainments USING btree (programme_id, batch_id);


--
-- Name: idx_po_competencies_po; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_po_competencies_po ON public.po_competencies USING btree (po_id);


--
-- Name: idx_programme_atr_programme_batch; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_programme_atr_programme_batch ON public.programme_atrs USING btree (programme_id, batch_id);


--
-- Name: idx_programme_exit_survey_batch; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_programme_exit_survey_batch ON public.programme_exit_surveys USING btree (programme_id, batch_id);


--
-- Name: idx_programme_outcomes_programme; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_programme_outcomes_programme ON public.programme_outcomes USING btree (programme_id);


--
-- Name: idx_programme_pso_programme; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_programme_pso_programme ON public.programme_specific_outcomes USING btree (programme_id);


--
-- Name: idx_programme_targets_batch; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_programme_targets_batch ON public.programme_targets USING btree (batch_id);


--
-- Name: idx_programmes_department; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_programmes_department ON public.programmes USING btree (department_id);


--
-- Name: idx_pso_attainment_programme_batch; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_pso_attainment_programme_batch ON public.pso_attainments USING btree (programme_id, batch_id);


--
-- Name: idx_pso_competencies_pso; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_pso_competencies_pso ON public.pso_competencies USING btree (pso_id);


--
-- Name: idx_semesters_batch; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_semesters_batch ON public.semesters USING btree (batch_id);


--
-- Name: idx_student_co_marks_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_student_co_marks_offering ON public.student_co_marks USING btree (course_offering_id);


--
-- Name: idx_student_co_marks_student; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_student_co_marks_student ON public.student_co_marks USING btree (student_id);


--
-- Name: idx_students_batch; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_students_batch ON public.students USING btree (batch_id);


--
-- Name: idx_uploaded_docs_batch; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_uploaded_docs_batch ON public.uploaded_documents USING btree (batch_id);


--
-- Name: idx_uploaded_docs_offering; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_uploaded_docs_offering ON public.uploaded_documents USING btree (course_offering_id);


--
-- Name: idx_users_email; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_users_email ON public.users USING btree (email);


--
-- Name: idx_users_scope; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_users_scope ON public.users USING btree (school_id, department_id, programme_id);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: rajshaikh
--

CREATE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: approval_history approval_history_actor_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.approval_history
    ADD CONSTRAINT approval_history_actor_id_fkey FOREIGN KEY (actor_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: approval_history approval_history_approval_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.approval_history
    ADD CONSTRAINT approval_history_approval_request_id_fkey FOREIGN KEY (approval_request_id) REFERENCES public.approval_requests(id) ON DELETE CASCADE;


--
-- Name: approval_requests approval_requests_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.approval_requests
    ADD CONSTRAINT approval_requests_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.batches(id) ON DELETE CASCADE;


--
-- Name: approval_requests approval_requests_course_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.approval_requests
    ADD CONSTRAINT approval_requests_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE CASCADE;


--
-- Name: approval_requests approval_requests_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.approval_requests
    ADD CONSTRAINT approval_requests_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: approval_requests approval_requests_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.approval_requests
    ADD CONSTRAINT approval_requests_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE CASCADE;


--
-- Name: approval_requests approval_requests_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.approval_requests
    ADD CONSTRAINT approval_requests_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE CASCADE;


--
-- Name: approval_requests approval_requests_school_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.approval_requests
    ADD CONSTRAINT approval_requests_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id) ON DELETE CASCADE;


--
-- Name: attainment_configurations attainment_configurations_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.attainment_configurations
    ADD CONSTRAINT attainment_configurations_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: attainment_levels attainment_levels_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.attainment_levels
    ADD CONSTRAINT attainment_levels_config_id_fkey FOREIGN KEY (config_id) REFERENCES public.attainment_configurations(id) ON DELETE CASCADE;


--
-- Name: batches batches_previous_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.batches
    ADD CONSTRAINT batches_previous_batch_id_fkey FOREIGN KEY (previous_batch_id) REFERENCES public.batches(id) ON DELETE SET NULL;


--
-- Name: batches batches_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.batches
    ADD CONSTRAINT batches_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE CASCADE;


--
-- Name: calculation_runs calculation_runs_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.calculation_runs
    ADD CONSTRAINT calculation_runs_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.batches(id) ON DELETE CASCADE;


--
-- Name: calculation_runs calculation_runs_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.calculation_runs
    ADD CONSTRAINT calculation_runs_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: calculation_runs calculation_runs_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.calculation_runs
    ADD CONSTRAINT calculation_runs_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE CASCADE;


--
-- Name: cc_setup_progress cc_setup_progress_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.cc_setup_progress
    ADD CONSTRAINT cc_setup_progress_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: co_po_mappings co_po_mappings_course_outcome_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.co_po_mappings
    ADD CONSTRAINT co_po_mappings_course_outcome_id_fkey FOREIGN KEY (course_outcome_id) REFERENCES public.course_outcomes(id) ON DELETE CASCADE;


--
-- Name: co_pso_mappings co_pso_mappings_course_outcome_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.co_pso_mappings
    ADD CONSTRAINT co_pso_mappings_course_outcome_id_fkey FOREIGN KEY (course_outcome_id) REFERENCES public.course_outcomes(id) ON DELETE CASCADE;


--
-- Name: course_atrs course_atrs_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_atrs
    ADD CONSTRAINT course_atrs_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: course_co_targets course_co_targets_course_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_co_targets
    ADD CONSTRAINT course_co_targets_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE CASCADE;


--
-- Name: course_end_surveys course_end_surveys_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_end_surveys
    ADD CONSTRAINT course_end_surveys_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: course_mapping_keywords course_mapping_keywords_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_mapping_keywords
    ADD CONSTRAINT course_mapping_keywords_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: course_offerings course_offerings_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_offerings
    ADD CONSTRAINT course_offerings_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.batches(id) ON DELETE CASCADE;


--
-- Name: course_offerings course_offerings_course_coordinator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_offerings
    ADD CONSTRAINT course_offerings_course_coordinator_id_fkey FOREIGN KEY (course_coordinator_id) REFERENCES public.users(id) ON DELETE SET NULL;


--
-- Name: course_offerings course_offerings_course_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_offerings
    ADD CONSTRAINT course_offerings_course_id_fkey FOREIGN KEY (course_id) REFERENCES public.courses(id) ON DELETE CASCADE;


--
-- Name: course_outcomes course_outcomes_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.course_outcomes
    ADD CONSTRAINT course_outcomes_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: courses courses_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.courses
    ADD CONSTRAINT courses_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE CASCADE;


--
-- Name: departments departments_school_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id) ON DELETE CASCADE;


--
-- Name: direct_co_attainments direct_co_attainments_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.direct_co_attainments
    ADD CONSTRAINT direct_co_attainments_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: direct_co_attainments direct_co_attainments_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.direct_co_attainments
    ADD CONSTRAINT direct_co_attainments_run_id_fkey FOREIGN KEY (run_id) REFERENCES public.calculation_runs(id) ON DELETE CASCADE;


--
-- Name: director_setup_progress director_setup_progress_school_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.director_setup_progress
    ADD CONSTRAINT director_setup_progress_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id) ON DELETE CASCADE;


--
-- Name: end_sem_marks_uploads end_sem_marks_uploads_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.end_sem_marks_uploads
    ADD CONSTRAINT end_sem_marks_uploads_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: hod_setup_progress hod_setup_progress_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.hod_setup_progress
    ADD CONSTRAINT hod_setup_progress_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE CASCADE;


--
-- Name: indirect_co_attainments indirect_co_attainments_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.indirect_co_attainments
    ADD CONSTRAINT indirect_co_attainments_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: indirect_co_attainments indirect_co_attainments_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.indirect_co_attainments
    ADD CONSTRAINT indirect_co_attainments_run_id_fkey FOREIGN KEY (run_id) REFERENCES public.calculation_runs(id) ON DELETE CASCADE;


--
-- Name: overall_co_attainments overall_co_attainments_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.overall_co_attainments
    ADD CONSTRAINT overall_co_attainments_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: overall_co_attainments overall_co_attainments_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.overall_co_attainments
    ADD CONSTRAINT overall_co_attainments_run_id_fkey FOREIGN KEY (run_id) REFERENCES public.calculation_runs(id) ON DELETE CASCADE;


--
-- Name: pc_setup_progress pc_setup_progress_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.pc_setup_progress
    ADD CONSTRAINT pc_setup_progress_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.batches(id) ON DELETE CASCADE;


--
-- Name: pc_setup_progress pc_setup_progress_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.pc_setup_progress
    ADD CONSTRAINT pc_setup_progress_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE CASCADE;


--
-- Name: peo_outcomes peo_outcomes_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.peo_outcomes
    ADD CONSTRAINT peo_outcomes_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE CASCADE;


--
-- Name: po_attainments po_attainments_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.po_attainments
    ADD CONSTRAINT po_attainments_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.batches(id) ON DELETE CASCADE;


--
-- Name: po_attainments po_attainments_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.po_attainments
    ADD CONSTRAINT po_attainments_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE CASCADE;


--
-- Name: po_attainments po_attainments_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.po_attainments
    ADD CONSTRAINT po_attainments_run_id_fkey FOREIGN KEY (run_id) REFERENCES public.calculation_runs(id) ON DELETE CASCADE;


--
-- Name: po_competencies po_competencies_po_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.po_competencies
    ADD CONSTRAINT po_competencies_po_id_fkey FOREIGN KEY (po_id) REFERENCES public.programme_outcomes(id) ON DELETE CASCADE;


--
-- Name: programme_atrs programme_atrs_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_atrs
    ADD CONSTRAINT programme_atrs_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.batches(id) ON DELETE CASCADE;


--
-- Name: programme_atrs programme_atrs_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_atrs
    ADD CONSTRAINT programme_atrs_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE CASCADE;


--
-- Name: programme_exit_surveys programme_exit_surveys_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_exit_surveys
    ADD CONSTRAINT programme_exit_surveys_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.batches(id) ON DELETE CASCADE;


--
-- Name: programme_exit_surveys programme_exit_surveys_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_exit_surveys
    ADD CONSTRAINT programme_exit_surveys_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE CASCADE;


--
-- Name: programme_outcomes programme_outcomes_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_outcomes
    ADD CONSTRAINT programme_outcomes_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE CASCADE;


--
-- Name: programme_specific_outcomes programme_specific_outcomes_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_specific_outcomes
    ADD CONSTRAINT programme_specific_outcomes_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE CASCADE;


--
-- Name: programme_targets programme_targets_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programme_targets
    ADD CONSTRAINT programme_targets_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.batches(id) ON DELETE CASCADE;


--
-- Name: programmes programmes_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.programmes
    ADD CONSTRAINT programmes_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE CASCADE;


--
-- Name: pso_attainments pso_attainments_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.pso_attainments
    ADD CONSTRAINT pso_attainments_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.batches(id) ON DELETE CASCADE;


--
-- Name: pso_attainments pso_attainments_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.pso_attainments
    ADD CONSTRAINT pso_attainments_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE CASCADE;


--
-- Name: pso_attainments pso_attainments_run_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.pso_attainments
    ADD CONSTRAINT pso_attainments_run_id_fkey FOREIGN KEY (run_id) REFERENCES public.calculation_runs(id) ON DELETE CASCADE;


--
-- Name: pso_competencies pso_competencies_pso_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.pso_competencies
    ADD CONSTRAINT pso_competencies_pso_id_fkey FOREIGN KEY (pso_id) REFERENCES public.programme_specific_outcomes(id) ON DELETE CASCADE;


--
-- Name: semesters semesters_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.semesters
    ADD CONSTRAINT semesters_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.batches(id) ON DELETE CASCADE;


--
-- Name: student_co_marks student_co_marks_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.student_co_marks
    ADD CONSTRAINT student_co_marks_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: student_co_marks student_co_marks_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.student_co_marks
    ADD CONSTRAINT student_co_marks_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id) ON DELETE CASCADE;


--
-- Name: student_co_marks student_co_marks_upload_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.student_co_marks
    ADD CONSTRAINT student_co_marks_upload_id_fkey FOREIGN KEY (upload_id) REFERENCES public.end_sem_marks_uploads(id) ON DELETE CASCADE;


--
-- Name: students students_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.students
    ADD CONSTRAINT students_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.batches(id) ON DELETE CASCADE;


--
-- Name: survey_response_details survey_response_details_response_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.survey_response_details
    ADD CONSTRAINT survey_response_details_response_id_fkey FOREIGN KEY (response_id) REFERENCES public.survey_responses(id) ON DELETE CASCADE;


--
-- Name: survey_responses survey_responses_student_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.survey_responses
    ADD CONSTRAINT survey_responses_student_id_fkey FOREIGN KEY (student_id) REFERENCES public.students(id) ON DELETE SET NULL;


--
-- Name: survey_responses survey_responses_survey_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.survey_responses
    ADD CONSTRAINT survey_responses_survey_id_fkey FOREIGN KEY (survey_id) REFERENCES public.course_end_surveys(id) ON DELETE CASCADE;


--
-- Name: uploaded_documents uploaded_documents_batch_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.uploaded_documents
    ADD CONSTRAINT uploaded_documents_batch_id_fkey FOREIGN KEY (batch_id) REFERENCES public.batches(id) ON DELETE CASCADE;


--
-- Name: uploaded_documents uploaded_documents_course_offering_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.uploaded_documents
    ADD CONSTRAINT uploaded_documents_course_offering_id_fkey FOREIGN KEY (course_offering_id) REFERENCES public.course_offerings(id) ON DELETE CASCADE;


--
-- Name: users users_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id) ON DELETE SET NULL NOT VALID;


--
-- Name: users users_programme_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_programme_id_fkey FOREIGN KEY (programme_id) REFERENCES public.programmes(id) ON DELETE SET NULL NOT VALID;


--
-- Name: users users_school_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: rajshaikh
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_school_id_fkey FOREIGN KEY (school_id) REFERENCES public.schools(id) ON DELETE SET NULL;


--
-- PostgreSQL database dump complete
--

\unrestrict 1EogM61dvlBPHI7HcnW8nSK0MaadbjiDlYO4QlQLzvi7gX3kq0vrQVj2tcTZQfo

