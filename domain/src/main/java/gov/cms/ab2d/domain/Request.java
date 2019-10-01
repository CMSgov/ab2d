package gov.cms.ab2d.domain;


import javax.persistence.*;
import javax.validation.constraints.Pattern;
import java.util.Date;
import java.util.Objects;
import java.util.Set;

@Entity
public class Request {

    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String requestId;

    @ManyToOne
    private User user;

    @OneToMany(
            mappedBy = "request",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private Set<Output> output;

    @OneToMany(
            mappedBy = "request",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER
    )
    private Set<Error> errors;

    private Date transactionTime;
    private String requestURL;
    private int statusCode;
    private String statusMessage;

    @Pattern(regexp = "^(((Patient)|(ExplanationOfBenefits)),?)*$",
            message = "_type should contain a string of comma-delimited FHIR resource types; "
                    + "currently limited to Patient and ExplanationOfBenefits")
    private String requestedResourceTypes;

    private Date expires;
    private Date lastPollTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Date getTransactionTime() {
        return transactionTime;
    }

    public void setTransactionTime(Date transactionTime) {
        this.transactionTime = transactionTime;
    }

    public String getRequestURL() {
        return requestURL;
    }

    public void setRequestURL(String requestURL) {
        this.requestURL = requestURL;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    public Date getExpires() {
        return expires;
    }

    public void setExpires(Date expires) {
        this.expires = expires;
    }

    public Date getLastPollTime() {
        return lastPollTime;
    }

    public void setLastPollTime(Date lastPollTime) {
        this.lastPollTime = lastPollTime;
    }

    public String getRequestedResourceTypes() {
        return requestedResourceTypes;
    }

    public void setRequestedResourceTypes(String requestedResourceTypes) {
        this.requestedResourceTypes = requestedResourceTypes;
    }

    public Set<Output> getOutput() {
        return output;
    }

    public void setOutput(Set<Output> output) {
        this.output = output;
    }

    public Set<Error> getErrors() {
        return errors;
    }

    public void setErrors(Set<Error> errors) {
        this.errors = errors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Request request = (Request) o;
        return Objects.equals(id, request.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
