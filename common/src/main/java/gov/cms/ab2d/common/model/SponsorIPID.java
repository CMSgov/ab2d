package gov.cms.ab2d.common.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.hibernate.annotations.TypeDefs;

import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Embeddable
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@TypeDefs(value = { @TypeDef(name = "pgInet", typeClass = PgInetType.class) })
public class SponsorIPID implements Serializable {

    @ManyToOne
    @JoinColumn(name = "sponsor_id")
    @NotNull
    private Sponsor sponsor;

    @Type(type = "pgInet")
    private PGInet ipAddress;
}
