package com.projects.instagram.security;


import com.projects.instagram.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AppUserDetails implements UserDetails {

    private Long id;
    private String username;
    private String userPassword;
    private String email;
    private String  mobileNumber;
    private Collection<? extends GrantedAuthority> authorities;

    public AppUserDetails(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.userPassword = user.getUserPassword();
        this.mobileNumber = user.getMobileNumber();
        this.authorities = List.of(new SimpleGrantedAuthority(user.getRole()));
    }

    @Override
    public String getUsername() {
        return username;
    }
////
////    @Override
//    public String getEmail() {
//        return email;
//    }
    @Override
    public String getPassword() {
        return userPassword;
    }
//
//    @Override
//    public String getMobileNumber() {
//        return mobileNumber;
//    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // Custom getter
    public Long getId() {
        return id;
    }
}
