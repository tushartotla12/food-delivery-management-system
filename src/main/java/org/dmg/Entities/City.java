package org.dmg.Entities;

import jakarta.persistence.*;
import lombok.*;
import org.dmg.Entities.Base.AuditableEntity;

import java.util.ArrayList;
import java.util.List;

@Builder
@AllArgsConstructor
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "cities")
public class City extends AuditableEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "city", fetch = FetchType.LAZY)
    private List<Restaurant> restaurants = new ArrayList<>();

}

