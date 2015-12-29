package org.tiltedwindmills.fantasy.zealots;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {

    @RequestMapping("/")
    public final String index() {
        return "index";
    }
}
