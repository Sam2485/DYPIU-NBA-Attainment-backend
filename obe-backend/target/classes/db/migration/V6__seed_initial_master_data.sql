-- V6__seed_initial_master_data.sql
-- Seed Initial Master Data matching frontend default state

-- 1. Schools
INSERT INTO schools (id, code, name, dean, est_year, email) VALUES
('sch-1', 'SET', 'School of Engineering & Technology', 'Dr. R. K. Deshmukh', '2019', 'set.director@dypiu.ac.in'),
('sch-2', 'SOM', 'School of Management Studies', 'Dr. P. S. Mehta', '2020', 'som.director@dypiu.ac.in');

-- 2. Departments
INSERT INTO departments (id, school_id, code, name, hod, hod_email, status) VALUES
('dept-1', 'sch-1', 'CSE', 'Department of Computer Science & Engineering', 'Dr. Raj Shaikh', 'raj.shaikh@dypiu.ac.in', 'ACTIVE'),
('dept-2', 'sch-1', 'ENTC', 'Department of Electronics & Telecommunication', 'Prof. Ananya Roy', 'ananya.roy@dypiu.ac.in', 'ACTIVE'),
('dept-3', 'sch-1', 'IT', 'Department of Information Technology', 'Dr. Vikram Joshi', 'vikram.joshi@dypiu.ac.in', 'ACTIVE'),
('dept-4', 'sch-2', 'MGMT', 'Department of Management Studies', 'Dr. Sameer Khan', 'sameer.khan@dypiu.ac.in', 'ACTIVE');

-- 3. Programmes
INSERT INTO programmes (id, department_id, code, name, duration_years, department_name, coordinator, coordinator_email, status) VALUES
('prog-1', 'dept-1', 'BE-COMP', 'B.Tech Computer Science & Engineering', 4, 'Department of Computer Science & Engineering', 'Dr. A. K. Sharma', 'ak.sharma@dypiu.ac.in', 'ACTIVE'),
('prog-2', 'dept-1', 'BE-AI', 'B.Tech AI & Data Science', 4, 'Department of Computer Science & Engineering', 'Prof. R. V. Patel', 'rv.patel@dypiu.ac.in', 'ACTIVE'),
('prog-3', 'dept-4', 'MBA', 'Master of Business Administration', 2, 'Department of Management Studies', 'Dr. S. N. Deshmukh', 'sn.deshmukh@dypiu.ac.in', 'ACTIVE'),
('prog-4', 'dept-2', 'BE-ENTC', 'B.Tech Electronics & Telecommunication', 4, 'Department of Electronics & Telecommunication', 'Prof. Ananya Roy', 'ananya.roy@dypiu.ac.in', 'ACTIVE'),
('prog-5', 'dept-1', 'ME-COMP', 'M.Tech Computer Science & Engineering', 2, 'Department of Computer Science & Engineering', 'Dr. Vikram Joshi', 'vikram.joshi@dypiu.ac.in', 'ACTIVE');

-- 4. Batches
INSERT INTO batches (id, programme_id, programme_code, programme_name, duration_years, name, start_year, end_year, year_level, status) VALUES
('batch-comp-2025-29', 'prog-1', 'BE-COMP', 'B.Tech Computer Science & Engineering', 4, 'Batch 2025-29 (BE-COMP) — AY 2025-26 to 2028-29', '2025-26', '2028-29', 'Year 1 (Freshmen)', 'ACTIVE'),
('batch-comp-2024-28', 'prog-1', 'BE-COMP', 'B.Tech Computer Science & Engineering', 4, 'Batch 2024-28 (BE-COMP) — AY 2024-25 to 2027-28', '2024-25', '2027-28', 'Year 2 (Sophomores)', 'ACTIVE'),
('batch-comp-2023-27', 'prog-1', 'BE-COMP', 'B.Tech Computer Science & Engineering', 4, 'Batch 2023-27 (BE-COMP) — AY 2023-24 to 2026-27', '2023-24', '2026-27', 'Year 3 (Juniors)', 'ACTIVE'),
('batch-comp-2022-26', 'prog-1', 'BE-COMP', 'B.Tech Computer Science & Engineering', 4, 'Batch 2022-26 (BE-COMP) — AY 2022-23 to 2025-26', '2022-23', '2025-26', 'Year 4 (Seniors / Final Year)', 'ACTIVE'),
('batch-mba-2025-27', 'prog-3', 'MBA', 'Master of Business Administration', 2, 'Batch 2025-27 (MBA) — AY 2025-26 to 2026-27', '2025-26', '2026-27', 'Year 1 (Junior Batch)', 'ACTIVE'),
('batch-mba-2024-26', 'prog-3', 'MBA', 'Master of Business Administration', 2, 'Batch 2024-26 (MBA) — AY 2024-25 to 2025-26', '2024-25', '2025-26', 'Year 2 (Senior Batch)', 'ACTIVE');

-- 5. Courses
INSERT INTO courses (id, code, name, programme_id, semester, coordinator, faculty, assigned_faculty, academic_year) VALUES
('crs-1', '310244', 'Computer Network and Security', 'prog-1', 'Sem I', 'Dr. Raj Shaikh', 'Dr. Raj Shaikh / Prof. XYZ', '["Dr. Raj Shaikh", "Prof. XYZ"]', '2025-26'),
('crs-2', 'CS301', 'Data Structures & Algorithms', 'prog-1', 'Sem III', 'Prof. Ananya Roy', 'Dr. Raj Shaikh / Prof. Ananya Roy', '["Dr. Raj Shaikh", "Prof. Ananya Roy"]', '2025-26'),
('crs-3', 'AI201', 'Machine Learning Fundamentals', 'prog-2', 'Sem IV', 'Dr. Vikram Joshi', 'Dr. Vikram Joshi', '["Dr. Vikram Joshi"]', '2025-26'),
('crs-4', 'MBA101', 'Organizational Behavior', 'prog-3', 'Sem I', 'Dr. Sameer Khan', 'Dr. Sameer Khan', '["Dr. Sameer Khan"]', '2025-26');

-- 6. Students
INSERT INTO students (id, batch_id, prn, name, email, status) VALUES
('std-1', 'batch-comp-2025-29', '1032250101', 'Aarav Sharma', 'aarav.sharma@dypiu.edu.in', 'ENROLLED'),
('std-2', 'batch-comp-2025-29', '1032250102', 'Ananya Deshmukh', 'ananya.d@dypiu.edu.in', 'ENROLLED'),
('std-3', 'batch-comp-2025-29', '1032250103', 'Rohan Patel', 'rohan.patel@dypiu.edu.in', 'ENROLLED'),
('std-4', 'batch-comp-2025-29', '1032250104', 'Sneha Kulkarni', 'sneha.k@dypiu.edu.in', 'ENROLLED'),
('std-5', 'batch-comp-2025-29', '1032250105', 'Aditya Verma', 'aditya.v@dypiu.edu.in', 'ENROLLED');

-- 7. Initial Users (PasswordsBCrypt for "password123": $2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymY0vBchFuvLqK49L.O/8G)
INSERT INTO users (username, email, password_hash, name, role, department, programme, is_active) VALUES
('director', 'director@dypiu.ac.in', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymY0vBchFuvLqK49L.O/8G', 'Dr. R. K. Deshmukh', 'DIRECTOR', 'School of Engineering & Technology', 'All Programmes', TRUE),
('hod_cse', 'raj.shaikh@dypiu.ac.in', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymY0vBchFuvLqK49L.O/8G', 'Dr. Raj Shaikh', 'HOD', 'Computer Science & Engineering', 'B.Tech CSE', TRUE),
('pc_cse', 'ak.sharma@dypiu.ac.in', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymY0vBchFuvLqK49L.O/8G', 'Dr. A. K. Sharma', 'PROGRAMME_COORDINATOR', 'Computer Science & Engineering', 'B.Tech CSE', TRUE),
('faculty_raj', 'faculty.raj@dypiu.ac.in', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymY0vBchFuvLqK49L.O/8G', 'Dr. Raj Shaikh', 'FACULTY', 'Computer Science & Engineering', 'B.Tech CSE', TRUE),
('iqac_admin', 'iqac@dypiu.ac.in', '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymY0vBchFuvLqK49L.O/8G', 'IQAC Coordinator', 'IQAC', 'IQAC Central Cell', 'All Programmes', TRUE);

-- 8. Course Outcomes for Course 310244 (crs-1)
INSERT INTO course_outcomes (id, course_id, code, statement) VALUES
('co-1-1', 'crs-1', 'C321.1', 'Interpret fundamental concepts of Computer Networks, architectures, protocols and technologies'),
('co-1-2', 'crs-1', 'C321.2', 'Demonstrate the working and functions of data link layer for flow and error control'),
('co-1-3', 'crs-1', 'C321.3', 'Analyze the working of different routing protocols and mechanisms for transmission of data'),
('co-1-4', 'crs-1', 'C321.4', 'Implement client-server applications using sockets'),
('co-1-5', 'crs-1', 'C321.5', 'Analyze role of application layer with its protocols, client-server architectures'),
('co-1-6', 'crs-1', 'C321.6', 'Interpret the basics of Network Security for secured communication');

-- 9. Attainment Configuration for crs-1
INSERT INTO attainment_configurations (id, course_id, course_code, course_name, direct_weight, indirect_weight, direct_threshold, indirect_threshold, status, submitted_by, submitted_at) VALUES
('cfg-crs-1', 'crs-1', '310244', 'Computer Network and Security', 80.00, 20.00, 60.00, 60.00, 'VERIFIED', 'Dr. Raj Shaikh', CURRENT_TIMESTAMP);
