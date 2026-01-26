package hr.elektropregled.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        System.out.println("Hash za 'pass123': " + encoder.encode("pass123"));
        System.out.println("Hash za 'admin123': " + encoder.encode("admin123"));
    }
}