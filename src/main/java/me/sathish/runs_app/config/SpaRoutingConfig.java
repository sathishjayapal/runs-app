package me.sathish.runs_app.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Controller
public class SpaRoutingConfig {

    @GetMapping(value = {"/user-logout", "/runAppUsers", "/runAppUsers/**", "/garminRuns", "/garminRuns/**",
            "/shedlocks", "/fileNameTrackers", "/fileNameTrackers/**",
            "/stravaRuns", "/stravaRuns/**", "/error"}, produces = "text/html")
    @ResponseBody
    public String forwardToIndex(final HttpServletRequest request) throws IOException {
        final Resource resource = new ClassPathResource("static/index.html");
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

}
