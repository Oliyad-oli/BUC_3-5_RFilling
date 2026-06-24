package com.itas.taxfiling.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "schedules")
@Getter
@Setter
public class ScheduleJpaEntity {
    @Id
    private String id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_return_id")
    private TaxReturnJpaEntity taxReturn;
    
    private String code;
    private String name;
    
    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineItemJpaEntity> lineItems = new ArrayList<>();
}
