package pl.arimr.statemachinedemo.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pl.arimr.statemachinedemo.enums.ApplicationStatus;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@Getter
@Setter
@Table(name = "APPLICATION")
public class Application extends BaseEntity {

    private String name;
    private LocalDateTime createdDate;
    private String organizationUnit;
    private BigDecimal amount;

    @Column(nullable = false, updatable = false, name = "STATUS")
    @Setter(AccessLevel.PRIVATE)
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status = ApplicationStatus.ENTERED;

    public Application(String name, String organizationUnit, BigDecimal amount) {
        this.name = name;
        this.organizationUnit = organizationUnit;
        this.amount = amount;
    }
}
