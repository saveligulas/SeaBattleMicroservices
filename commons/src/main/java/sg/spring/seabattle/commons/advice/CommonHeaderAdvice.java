package sg.spring.seabattle.commons.advice;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.context.request.RequestAttributes;

public class CommonHeaderAdvice {
    @ModelAttribute
    public void addUserIdAttribute(@RequestHeader(name = "user", required = false) String userId, Model model) {
        model.addAttribute("userId", userId);
    }
}
