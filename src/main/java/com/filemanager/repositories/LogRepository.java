package com.filemanager.repositories;

import com.filemanager.enums.Level;
import com.filemanager.models.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

public interface LogRepository  extends JpaRepository<Log, Long> , PagingAndSortingRepository<Log, Long> {
    Page<Log> findAllByOrderByDateDesc(Pageable pageable);
    int countAllByMessageContaining(String message);
    @Transactional
    @Modifying(clearAutomatically = true)
    void deleteAllByLevel(Level level);
}
