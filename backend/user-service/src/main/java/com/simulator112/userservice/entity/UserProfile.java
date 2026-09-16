package com.simulator112.userservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Data
@Table(name = "user_profiles")
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {
  @Id
  @Column(nullable = false)
  private UUID userId;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String surname;

  @UpdateTimestamp private Instant updatedAt;
}
