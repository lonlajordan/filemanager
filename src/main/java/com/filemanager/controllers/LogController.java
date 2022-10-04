package com.filemanager.controllers;

import com.filemanager.enums.Level;
import com.filemanager.models.Log;
import com.filemanager.models.Notification;
import com.filemanager.repositories.LogRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/log")
public class LogController {
    @PersistenceContext
    private EntityManager em;

    private final LogRepository logRepository;

    public LogController(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @GetMapping(value="list")
    public String getAll(@RequestParam(required = false, defaultValue = "1") int p, Model model){
        Pageable pageable = PageRequest.of(p  - 1, 50);
        Page<Log> logs = logRepository.findAllByOrderByDateDesc(pageable);
        model.addAttribute("logs", logs.get().collect(Collectors.toList()));
        model.addAttribute("totalPages", logs.getTotalPages());
        model.addAttribute("currentPage", p);
        model.addAttribute("filtered", false);
        return "logs";
    }

    @GetMapping(value="details/{id}")
    @ResponseBody
    public String getDetails(@PathVariable long id){
        Log log = logRepository.findById(id).orElse(null);
        return log == null || log.getDetails() == null ? "" : log.getDetails();
    }

    @PostMapping(value="delete")
    public String deleteLogs(@RequestParam ArrayList<Long> ids, RedirectAttributes attributes){
        try {
            logRepository.deleteAllById(ids);
            attributes.addFlashAttribute("notification", new Notification("success", "Opération terminée avec succès."));
        }catch (Exception e){
            attributes.addFlashAttribute("notification", new Notification("error", "Une erreur est survenue lors de cette opération."));
        }
        return "redirect:/log/list";
    }

    @PostMapping(value="search")
    public String search(@RequestParam(required = false, defaultValue = "") String level,
                         @RequestParam(required = false, defaultValue = "") String message,
                         @RequestParam(required = false, defaultValue = "1970-01-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date start,
                         @RequestParam(required = false, defaultValue = "1970-01-01") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date end,
                         Model model){
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Log> cq = cb.createQuery(Log.class);
        Root<Log> log = cq.from(Log.class);
        List<Predicate> predicates = new ArrayList<>();
        if(StringUtils.isNotEmpty(message)) predicates.add(cb.like(log.get("message"), "%" + message + "%"));
        if(StringUtils.isNotEmpty(level)) predicates.add(cb.equal(log.get("level"), Level.valueOf(level)));
        if(start.toInstant().getEpochSecond() > 0) predicates.add(cb.greaterThanOrEqualTo(log.get("date"), start));
        if(end.toInstant().getEpochSecond() > 0) predicates.add(cb.lessThanOrEqualTo(log.get("date"), end));
        cq.where(predicates.toArray(new Predicate[0]));
        TypedQuery<Log> query = em.createQuery(cq).setMaxResults(1000);
        List<Log> logs = query.getResultList();
        model.addAttribute("logs", logs);
        model.addAttribute("totalPages", 1);
        model.addAttribute("currentPage", 0);
        model.addAttribute("filtered", true);
        return "logs";
    }
}
