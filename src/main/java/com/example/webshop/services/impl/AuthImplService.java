package com.example.webshop.services.impl;

import com.example.webshop.exceptions.NotFoundException;
import com.example.webshop.exceptions.UnauthorizedException;
import com.example.webshop.models.dto.JwtUser;
import com.example.webshop.models.dto.LoginResponse;
import com.example.webshop.models.entities.KorisnikEntity;
import com.example.webshop.models.enums.UserStatus;
import com.example.webshop.models.requests.AccountActivationRequest;
import com.example.webshop.models.requests.LoginRequest;
import com.example.webshop.repositories.UserRepository;
import com.example.webshop.services.AuthService;
import com.example.webshop.services.EmailService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthImplService implements AuthService {

    private final AuthenticationManager authenticationManager;

    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final EmailService emailService;

    private Map<String,String> codes=new HashMap<>();

    @Value("${authorization.token.expiration-time}")
    private String tokenExpirationTime;
    @Value("${authorization.token.secret}")
    private String tokenSecret;


    @Override
    public LoginResponse login(LoginRequest request) {
        LoginResponse response = null;
        try {
            Authentication authenticate = authenticationManager
                    .authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.getKorisnickoIme(), request.getLozinka()
                            )
                    );
            KorisnikEntity userEntity = userRepository.findByKorisnickoIme(request.getKorisnickoIme()).orElseThrow(NotFoundException::new);
            if (userEntity.getStatus().equals(UserStatus.ACTIVE)) {
                JwtUser user = (JwtUser) authenticate.getPrincipal();
                response = modelMapper.map(userEntity, LoginResponse.class);
                response.setToken(generateJwt(user));
                return response;
            } else {
                 sendActivationCode(userEntity.getKorisnickoIme(),userEntity.getEmail());
            }
        } catch (Exception ex) {
            //  LoggingUtil.logException(ex, getClass());
            throw new UnauthorizedException();
        }
        return response;
    }
    @Override
    public void sendActivationCode(String username,String mail) {
        SecureRandom secureRandom = new SecureRandom();
        String activationCode=String.valueOf(secureRandom.nextInt(9000)+1000);
        codes.put(username,mail);
        emailService.sendEmail(mail,activationCode);

    }

    @Override
    public boolean activateAccount(AccountActivationRequest request) {
        return codes.containsKey(request.getKorisnickoIme()) && codes.get(request.getKorisnickoIme()).equals(request.getCode());
    }
    private String generateJwt(JwtUser user) {
        return Jwts.builder()
                .setId(user.getId().toString())
                .setSubject(user.getUsername())
                .claim("role", user.getRole().name())
                .setExpiration(new Date(System.currentTimeMillis() + Long.parseLong(tokenExpirationTime)))
                .signWith(SignatureAlgorithm.HS512, tokenSecret)
                .compact();
    }
}
