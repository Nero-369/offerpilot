package com.offerpilot.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name="app_users")
public class AppUser {
    @Id private UUID id;
    @Column(nullable=false,unique=true,length=50) private String username;
    @Column(nullable=false,length=80) private String displayName;
    @Column(nullable=false,length=100) private String passwordHash;
    @Column(nullable=false) private Instant createdAt;
    protected AppUser() {}
    public AppUser(String username,String displayName,String passwordHash){this.id=UUID.randomUUID();this.username=username;this.displayName=displayName;this.passwordHash=passwordHash;this.createdAt=Instant.now();}
    public UUID getId(){return id;} public String getUsername(){return username;} public String getDisplayName(){return displayName;} public String getPasswordHash(){return passwordHash;} public Instant getCreatedAt(){return createdAt;}
}
