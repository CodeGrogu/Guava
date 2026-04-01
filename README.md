# Guava
Mobile Application Development group project.

This group assignment of four requires an application for Valentine’s Garage that
allows truck check-in for repairs. At check-in, the condition of the vehicles and the
number of kilometers driven should be captured to prevent misuse in the garage.
During truck servicing and repairs, mechanics should collaboratively tick off and

Requirements Specification: Valentine's Garage Application

1. Project Overview

This document outlines the software requirements for a truck check-in and repair management application designed specifically for Valentine's Garage. The system aims to streamline vehicle intake, enhance accountability among mechanics during collaborative repairs, and provide comprehensive oversight for management. This system is to be developed as a group assignment by a team of four.

2. User Roles

Mechanic: Responsible for logging vehicle conditions, recording mileage, ticking off completed tasks, and adding service notes during repairs.
Manager (Valentine): Requires access to oversight reports regarding employee performance and vehicle intake states.

3. Functional Requirements

3.1. Vehicle Check-In Module

FR-1.1: The system must allow users to initiate a new check-in record for a truck arriving at the garage.
FR-1.2: The system must capture and securely store the physical condition of the vehicle at the exact time of check-in.
FR-1.3: The system must record the exact number of kilometres driven by the vehicle at check-in to prevent and monitor for unauthorised use.

3.2. Servicing and Repairs Module (Collaborative Workflow)

FR-2.1: The system must provide a checklist of repair tasks for each checked-in vehicle.
FR-2.2: The system must allow mechanics to collaboratively tick off completed tasks on the same vehicle.
FR-2.3: The system must allow mechanics to add written notes detailing the specific work they performed on the vehicle.
FR-2.4: The system must explicitly track and display which specific mechanic completed a task or added a note to prevent tasks from going undone due to miscommunication.

3.3. Reporting and Oversight Module

FR-3.1: The system must generate reports detailing the specific work and tasks completed by each individual employee.
FR-3.2: The system must provide reports showing the initial condition and recorded kilometres of vehicles from their check-in phase.

4. Non-Functional Requirements

NFR-1 (Auditability): All actions, including task completions and notes, must be strictly linked to the authenticated user who performed them to maintain accountability.
NFR-2 (Concurrency): The system must handle simultaneous updates reliably, as multiple mechanics may be viewing and updating the same vehicle's repair checklist concurrently.

5. Project Constraints and Assessment

Team Size: The application must be developed collaboratively by a team of exactly four members.

Assessment: The final project deliverable will be evaluated against the provided assessment rubric (referenced as Table 1 in the assignment brief).
