package hr.elektropregled.service;

import hr.elektropregled.config.JwtProvider;
import hr.elektropregled.dto.LoginRequest;
import hr.elektropregled.dto.LoginResponse;
import hr.elektropregled.exception.NotFoundException;
import hr.elektropregled.model.Korisnik;
import hr.elektropregled.repository.KorisnikRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final KorisnikRepository korisnikRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    public AuthService(KorisnikRepository korisnikRepository,
                       PasswordEncoder passwordEncoder,
                       JwtProvider jwtProvider) {
        this.korisnikRepository = korisnikRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public LoginResponse login(LoginRequest request) {
        Korisnik korisnik = korisnikRepository.findByKorisnickoIme(request.getKorisnickoIme())
                .orElseThrow(() -> new NotFoundException("Korisničko ime ili lozinka nisu ispravni"));

        if (!passwordEncoder.matches(request.getLozinka(), korisnik.getLozinka())) {
            throw new NotFoundException("Korisničko ime ili lozinka nisu ispravni");
        }

        String token = jwtProvider.generateToken(korisnik.getKorisnickoIme());
        return new LoginResponse(token, "Bearer", jwtExpiration / 1000, korisnik.getKorisnickoIme());
    }
}