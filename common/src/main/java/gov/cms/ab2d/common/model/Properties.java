package gov.cms.ab2d.common.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Entity
@Getter
@Setter
public class Properties {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    private String key;

    @NotNull
    private String value;
}
