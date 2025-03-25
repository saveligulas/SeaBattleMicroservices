package sg.spring.seabattle.authorization.config;

import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UsernameRepository usernameRepository;

    public AuthService(UsernameRepository usernameRepository) {
        this.usernameRepository = usernameRepository;
    }

    public void register(String userId) {
        usernameRepository.save(new Username(userId));
    }

    public boolean validateUserId(String userId) {
        //just checking if the username exists
        usernameRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return true;
    }

}
