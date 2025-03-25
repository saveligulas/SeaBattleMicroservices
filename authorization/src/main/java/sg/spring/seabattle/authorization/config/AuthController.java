package sg.spring.seabattle.authorization.config;

import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/validate")
    public Boolean isValid(@RequestParam("userId") String userId) {
        return authService.validateUserId(userId);
    }

    @PostMapping("/register")
    public void registerUser(@RequestParam("userId") String userId) {
        authService.register(userId);
    }
}
