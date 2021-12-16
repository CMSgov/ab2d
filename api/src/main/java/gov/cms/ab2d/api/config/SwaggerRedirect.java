package gov.cms.ab2d.api.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Controller
public class SwaggerRedirect {

    @RequestMapping(value = "/swagger-ui.html")
    public void swagger(HttpServletResponse httpResponse) throws IOException {
        httpResponse.sendRedirect("/swagger-ui/index.html");
    }

}
