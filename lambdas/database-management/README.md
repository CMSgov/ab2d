Ab2D uses [Liquibase](https://www.liquibase.org/) to manage database changes. For traditional applications Liguibase runs once during app startup.
Lambdas work best when they run as fast as possible so sunning liquibase each time a lambda is invoked isn't idea. 
This lambda bridges that gap. During deploys the ops terraform triggers this lambda once. 

This project also acts as a small smoke test. 
If it successfully runs then any lambda that interfaces with the database should also work since they all share the same AWS settings.   

Finally, this project should be a used as library from any other lambda that needs database access.
To use in another lambda project add this to that projects build.gradle
`implementation project(':database-management')`
Request a connection to the database
`Connection connection = DatabaseUtil.getConnection();`
Once a connection is created use Java's built in PreparedStatement to interact with database objects.  
