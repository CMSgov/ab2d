# AB2D Filter

This library implements the logic behind the ExplanationOfBenefit filtering and trimming associated with CMS
data access rules for PDPs participating in AB2D. These rules include:

1. Trimming the ExplanationOfBenefit data to the fields allowed for PDPs

1. Ensuring that the billing period is after the "earliest" defined date access time - this will be
01/01/2020.
   
1. After the date that the contract holder attested to HPMS. 

1. During a period in which the beneficiary was a member of the PDPs contract.

## Using the Library

There is really only one entry point for this library:

```
Optional<IBaseResource> FilterEob.filter(IBaseResource resource, 
    List<FilterOutByDate.DateRange> dateRanges,
    Date earliestDate, 
    Date attTime, 
    boolean skipBillablePeriodCheck)

```

| parameter | description |
| ----------|-------------|
| ``` resource``` | The ExplanationOfBenefit resource object. This can be any version (current supports FHIR STU3/R4) |
| ``` dateRanges ``` | The date ranges in which the beneficiary was a member of the contract. This will be used to compare against the ```billablePeriod``` in the resource |
| ``` earliestDate ``` | The earliest date that data can be returned. Generally, for AB2D this will be Jan 1, 2020. |
| ``` attTime ``` | When the contract was attested |
| ``` skipBillablePeriodCheck ``` | This bypasses all date checks (although it does validate that attestion date is not null). This should only be used for testing. Most of the test data is before the ```earliest_date``` so without this, all searches would return no data |

The method returns an ```Optional<IBaseResource>```. If the ExplanationOfBenefit has passed the check, ```ifPresent()``` resolves
to true and the resource can be obtained with a ```.get()```, otherwise ```isEmpty()``` resolves to true. It's important
to retrieve the object data using the ```.get()``` instead of using the original passwd object since this method removes
data that should not be given to the PDP.