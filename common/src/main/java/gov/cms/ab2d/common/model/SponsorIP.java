package gov.cms.ab2d.common.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity(name = "sponsor_ip")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class SponsorIP {

    @EmbeddedId
    private SponsorIPID sponsorIPID;
}
