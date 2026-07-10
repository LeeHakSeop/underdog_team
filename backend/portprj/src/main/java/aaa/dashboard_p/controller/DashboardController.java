package aaa.dashboard_p.controller;

import aaa.dashboard_p.model.DashboardDTO;
import aaa.dashboard_p.service.DashboardService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Resource
    DashboardService service;

    @GetMapping("/admin")
    public DashboardDTO admin() {
        return service.admin();
    }
}
