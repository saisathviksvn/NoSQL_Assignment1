
CREATE TABLE Student (
    student_id VARCHAR(50) PRIMARY KEY, -- Format: IMT_2023_001
    name VARCHAR(100),
    age INT,
    email VARCHAR(100)
);


CREATE TABLE Grade (
    student_id VARCHAR(50),
    course_id VARCHAR(20),
    score INT,
    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES Student(student_id)
);


CREATE TABLE Course (
    course_id VARCHAR(20) PRIMARY KEY,
    course_name VARCHAR(100),
    department VARCHAR(50)
);

INSERT INTO Course (course_id, course_name, department) VALUES
('CS101', 'Intro to NoSQL', 'CS'),
('CS102', 'Operating Systems', 'CS'),
('MA101', 'Calculus I', 'Math'),
('MA102', 'Linear Algebra', 'Math'),
('PH101', 'Physics I', 'Physics');
