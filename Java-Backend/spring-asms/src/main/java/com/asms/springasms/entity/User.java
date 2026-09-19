package com.asms.springasms.entity;

import com.asms.springasms.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "users",
        uniqueConstraints = @UniqueConstraint(name = "IX_users_Email", columnNames = "Email"))
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "`Id`")
    private UUID id;

    @Column(name = "`FullName`", nullable = false, length = 200)
    private String fullName;

    @Column(name = "`Email`", nullable = false, length = 320)
    private String email;

    @Column(name = "`Roll`", length = 100)
    private String roll;

    @Column(name = "`PasswordHash`", nullable = false, length = 500)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "`Role`", nullable = false, length = 32)
    private UserRole role = UserRole.Admin;

    @Column(name = "`IsActive`", nullable = false)
    private boolean isActive = true;

    @Column(name = "`AuthVersion`", nullable = false)
    private int authVersion = 1;
}
