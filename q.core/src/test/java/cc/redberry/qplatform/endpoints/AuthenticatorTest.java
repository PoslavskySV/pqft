package cc.redberry.qplatform.endpoints;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.Test;

import java.security.Key;

public class AuthenticatorTest {
    @Test
    public void test1() {
        Key key = Keys.hmacShaKeyFor("E52A533D-CF4F-43AF-9F04-C86EDF1FAB52".getBytes());
        String jws = Jwts.builder().setSubject("Joe").signWith(key, SignatureAlgorithm.HS256).compact();
        String jwt = Jwts.builder().setSubject("Joe").compact();
        System.out.println(jws);
        System.out.println(jwt);
        // Claims body = Jwts.parser().setSigningKey(key).parseClaimsJws(jws).getBody();
        jws = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJKb3F3ZWVlIn0.2xVjkwnjUxMETVFkZ_BRXJtlXAWoxr1U03KB7PWTyCM";
        System.out.println(Jwts.parser().setSigningKey(key).parseClaimsJws(jws).getBody());
        // System.out.println(Jwts.parser().setSigningKey(key).parseClaimsJws(jwt).getBody());
        // System.out.println(Jwts.parser().setSigningKey(key).parsePlaintextJwt(jwt).getBody());
    }
}
