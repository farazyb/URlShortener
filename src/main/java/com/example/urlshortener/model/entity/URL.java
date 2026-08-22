package com.example.urlshortener.model.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SourceType;

import java.time.Instant;

@Entity
@Table
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class URL {
    @Id
    @SequenceGenerator(
            name = "user_id_seq",
            sequenceName = "url_sequence",
            initialValue = 100000,   // The starting number
            allocationSize = 50      // Increment by step
    )
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "user_id_seq"
    )
    @Column(name = "id")
    private Long id;
    private String originalUrl;
    private String shortUrl;
    @CreationTimestamp(source = SourceType.VM)
    private Instant createdAT;
}
