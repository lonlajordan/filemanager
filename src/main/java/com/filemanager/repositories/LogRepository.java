package com.filemanager.repositories;

import com.filemanager.models.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface LogRepository  extends JpaRepository<Log, Integer> , PagingAndSortingRepository<Log, Integer> {
    Page<Log> findAllByOrderByDateDesc(Pageable pageable);
    int countAllByMessageContaining(String message);
}
