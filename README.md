# NoSQL Assignment 1 — Horizontal Database Fragmentation (Sharding)

A Java implementation of **hash-based horizontal sharding** across multiple PostgreSQL database fragments, simulating the distributed storage model used in NoSQL systems. The system routes student and grade records across 3 shards using a consistent hashing strategy and executes a 4900+ line workload of mixed read/write operations.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Schema](#schema)
- [Project Structure](#project-structure)
- [Setup & Prerequisites](#setup--prerequisites)
- [Database Initialization](#database-initialization)
- [Build & Run](#build--run)
- [Supported Operations](#supported-operations)
- [Output & Accuracy Evaluation](#output--accuracy-evaluation)
- [Implementation Notes](#implementation-notes)

---

## Overview

This project demonstrates **horizontal fragmentation** — a core principle in distributed and NoSQL databases where data is partitioned across multiple nodes based on a shard key. Here, students and their grades are spread across 3 PostgreSQL databases (`fragment0`, `fragment1`, `fragment2`) using hash-based routing on `student_id`.

The system handles the full CRUD lifecycle:
- Insert/update/delete students and grades
- Read individual student profiles
- Aggregate queries (avg score per department, top students by course count) across shards

---

## Architecture

```
workload.txt
     │
     ▼
  Driver.java
     │
     ▼
FragmentClient.java
     │
     ├── Router.java  ──→  hash(student_id) % 3  ──→  fragment_id
     │
     ├──▶ fragment0 (PostgreSQL DB)
     ├──▶ fragment1 (PostgreSQL DB)
     └──▶ fragment2 (PostgreSQL DB)
```

**Routing logic** (`Router.java`):
```java
public int getFragmentId(String key) {
    return Math.abs(key.hashCode()) % numFragments;
}
```

Every operation on a student (insert, update, delete, read) is routed to the same shard deterministically via the hash of `student_id`. The `Course` table (a small static reference table) is replicated across all shards to support join queries locally.

---

## Schema

The schema is defined in `Assignment1/scripts.sql` and deployed identically on all 3 fragments.

```sql
CREATE TABLE Student (
    student_id VARCHAR(50) PRIMARY KEY,  -- e.g. IMT_2023_001
    name       VARCHAR(100),
    age        INT,
    email      VARCHAR(100)
);

CREATE TABLE Grade (
    student_id VARCHAR(50),
    course_id  VARCHAR(20),
    score      INT,
    PRIMARY KEY (student_id, course_id),
    FOREIGN KEY (student_id) REFERENCES Student(student_id)
);

CREATE TABLE Course (
    course_id   VARCHAR(20) PRIMARY KEY,
    course_name VARCHAR(100),
    department  VARCHAR(50)
);
```

Pre-seeded courses across all fragments:

| course_id | course_name      | department |
|-----------|------------------|------------|
| CS101     | Intro to NoSQL   | CS         |
| CS102     | Operating Systems| CS         |
| MA101     | Calculus I       | Math       |
| MA102     | Linear Algebra   | Math       |
| PH101     | Physics I        | Physics    |

---

## Project Structure

```
NoSQL_Assignment1/
├── Assignment1/
│   ├── scripts.sql                          # Schema + Course seed data
│   └── project/
│       ├── pom.xml                          # Maven build config (Java 17)
│       ├── src/
│       │   └── main/
│       │       ├── java/
│       │       │   ├── Driver.java          # Entry point; reads workload, writes output
│       │       │   └── fragment/
│       │       │       ├── FragmentClient.java  # All shard operations (CRUD + queries)
│       │       │       └── Router.java          # Hash-based shard routing
│       │       └── resources/
│       │           └── workload.txt         # ~4928 line workload of mixed commands
│       └── output/
│           ├── output.txt                   # Generated output from the run
│           ├── expected_output.txt          # Ground truth for evaluation
│           └── accuracy.py                  # Line-by-line accuracy checker
└── SQL_assign1.pdf                          # Assignment specification
```

---

## Setup & Prerequisites

**Requirements:**
- Java 17+
- Maven 3.6+
- PostgreSQL (running on `localhost:5432`)

**PostgreSQL user:**

```sql
CREATE USER simufrag WITH PASSWORD 'simufrag123';
```

---

## Database Initialization

Create and initialize all 3 fragment databases:

```bash
psql -U postgres -c "CREATE DATABASE fragment0 OWNER simufrag;"
psql -U postgres -c "CREATE DATABASE fragment1 OWNER simufrag;"
psql -U postgres -c "CREATE DATABASE fragment2 OWNER simufrag;"

# Apply schema + seed data to each fragment
for db in fragment0 fragment1 fragment2; do
    psql -U simufrag -d $db -f Assignment1/scripts.sql
done
```

> **Note:** The `Course` table must be seeded on **all 3 fragments** since aggregate queries (avg score by department) are issued against a single shard and require the join to be resolvable locally.

---

## Build & Run

```bash
cd Assignment1/project

# Build the fat JAR
mvn clean package

# Run
java -jar target/driver-with-shard-1.0.0.jar
```

The driver reads `workload.txt` from the classpath and writes all query results to `output.txt` in the working directory. It also prints total execution time on completion.

---

## Supported Operations

The workload file uses a CSV format, one operation per line:

| Command                | Format                                          | Description                                      |
|------------------------|-------------------------------------------------|--------------------------------------------------|
| `INSERT_STUDENT`       | `INSERT_STUDENT,id,name,age,email`              | Inserts a student into the correct shard         |
| `INSERT_GRADE`         | `INSERT_GRADE,student_id,course_id,score`       | Inserts a grade into the correct shard           |
| `UPDATE_GRADE`         | `UPDATE_GRADE,student_id,course_id,new_score`   | Updates an existing grade                        |
| `DELETE_STUDENT_COURSE`| `DELETE_STUDENT_COURSE,student_id,course_id`    | Deletes a grade record                           |
| `READ_PROFILE`         | `READ_PROFILE,student_id`                       | Returns `name,email` for a student               |
| `READ_SCORE`           | `READ_SCORE`                                    | Returns avg score per department (`dept:avg;...`)|
| `READ_ALL`             | `READ_ALL`                                      | Returns student IDs with the most courses taken  |

**Sample workload excerpt:**
```
INSERT_STUDENT,IMT_2023_001,Student_1,24,student_1@imt.edu
INSERT_GRADE,IMT_2023_001,CS101,85
UPDATE_GRADE,IMT_2023_001,CS101,90
READ_PROFILE,IMT_2023_001
READ_SCORE
READ_ALL
```

**Sample output:**
```
Student_1,student_1@imt.edu
Math:75.2;CS:79.0;Physics:75.2
IMT_2023_030;IMT_2023_015;IMT_2023_084
```

---

## Output & Accuracy Evaluation

After running, compare your `output.txt` against the ground truth:

```bash
cd Assignment1/project/output
python3 accuracy.py
```

The script reports:

```
Total lines:   <N>
Different lines: <K>
Accuracy: XX.XX %
```

It performs a strict line-by-line string comparison (after stripping whitespace) between `output.txt` and `expected_output.txt`.

---

## Implementation Notes

- **Conflict handling:** Both `INSERT_STUDENT` and `INSERT_GRADE` use `ON CONFLICT ... DO NOTHING` to safely handle duplicate inserts in the workload.
- **Aggregate queries (`READ_SCORE`, `READ_ALL`):** These are routed to a randomly selected fragment since the `Course` table is replicated and each fragment only holds its own subset of grades. In a full distributed setup, these would require a scatter-gather across all shards.
- **Connection pooling:** Each fragment gets one persistent JDBC connection maintained in a `HashMap<Integer, Connection>` for the lifetime of the workload run.
- **Shard key:** `student_id` (format: `IMT_2023_NNN`) is the shard key for all student and grade operations, ensuring that all data for a given student co-locates on one shard.
