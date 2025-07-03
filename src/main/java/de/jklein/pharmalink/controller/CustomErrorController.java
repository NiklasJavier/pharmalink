package de.jklein.pharmalink.controller;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import de.jklein.pharmalink.constants.ErrorMessages;

@Controller
public class CustomErrorController implements ErrorController {

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object error = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object path = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        if (status != null) {
            model.addAttribute("status", status);
        }
        model.addAttribute("error", error);
        model.addAttribute("message", message);
        model.addAttribute("path", path);
        model.addAttribute("timestamp", new java.util.Date());

        if (exception instanceof Throwable && (Boolean) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE + ".trace")) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            ((Throwable) exception).printStackTrace(pw);
            model.addAttribute("trace", sw.toString());
        }

        return "errors/error";
    }

    @GetMapping("/web/error/{code}")
    public String showSpecificErrorPage(@PathVariable int code, Model model) {
        model.addAttribute("status", code);

        model.addAttribute("titleOverride", ErrorMessages.getTitleForCode(code));
        model.addAttribute("messageOverride", ErrorMessages.getMessageForCode(code));

        model.addAttribute("path", "/web/error/" + code);
        model.addAttribute("timestamp", new java.util.Date());

        return "errors/error";
    }

    public String getErrorPath() {
        return "/error";
    }
}