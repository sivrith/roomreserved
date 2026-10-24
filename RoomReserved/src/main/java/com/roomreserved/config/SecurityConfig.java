package com.roomreserved.config;
import com.roomreserved.repo.UserRepository; import org.springframework.context.annotation.*; import org.springframework.security.config.annotation.web.builders.HttpSecurity; import org.springframework.security.core.userdetails.User; import org.springframework.security.core.userdetails.UserDetailsService; import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; import org.springframework.security.crypto.password.PasswordEncoder; import org.springframework.security.web.SecurityFilterChain;
@Configuration
public class SecurityConfig {
 @Bean PasswordEncoder passwordEncoder(){return new BCryptPasswordEncoder();}
 @Bean UserDetailsService userDetailsService(UserRepository users){return email->users.findByEmail(email).map(u->User.withUsername(u.getEmail()).password(u.getPassword()).roles(u.getRole()).build()).orElseThrow(()->new org.springframework.security.core.userdetails.UsernameNotFoundException("User not found"));}
 @Bean SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
  http.authorizeHttpRequests(a->a.requestMatchers("/","/rooms/**","/register","/css/**","/js/**","/images/**","/availability", "/webhooks/paymongo").permitAll().requestMatchers("/admin/**").hasRole("ADMIN").anyRequest().authenticated())
   .formLogin(f->f.loginPage("/login").defaultSuccessUrl("/",true).permitAll()).logout(l->l.logoutSuccessUrl("/")).csrf(c->c.ignoringRequestMatchers("/availability", "/webhooks/paymongo"));
  return http.build();
 }
}
