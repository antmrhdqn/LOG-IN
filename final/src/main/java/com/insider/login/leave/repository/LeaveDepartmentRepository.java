package com.insider.login.leave.repository;

import com.insider.login.department.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveDepartmentRepository extends JpaRepository<Department, Integer> {
}
