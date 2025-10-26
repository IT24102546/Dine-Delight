package org.example.dinedelightsystems.web;

import org.example.dine_delight.service.ReportService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

    private final ReportService reportService;

    public AdminReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String reports(@RequestParam(name = "months", defaultValue = "6") int months,
                          Model model) {
        Map<String, Object> summary = reportService.getMonthlySummary(Math.max(1, Math.min(24, months)));
        model.addAttribute("summary", summary);
        return "admin/reports";
    }

    @GetMapping(path = "/data", produces = "application/json")
    @ResponseBody
    public Map<String, Object> reportsData(@RequestParam(name = "months", defaultValue = "6") int months) {
        return reportService.getMonthlySummary(Math.max(1, Math.min(24, months)));
    }
}



