package com.filemanager.repositories;

import com.filemanager.models.Log;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LogRepository  extends JpaRepository<Log, Integer> {
}
