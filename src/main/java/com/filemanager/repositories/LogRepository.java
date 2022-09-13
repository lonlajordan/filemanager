package com.filemanager.repositories;

import com.filemanager.models.Log;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LogRepository  extends JpaRepository<Log, Integer> {
    List<Log> findAllByOrderByDateDesc();
}
