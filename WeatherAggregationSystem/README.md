Prerequisites
Before setting up and running the Weather Aggregation System, ensure that your system meets the following prerequisites:

Operating System: Linux, macOS, or Windows.
    Maven: Build automation tool for Java projects.
    cURL (Optional): Command-line tool for sending HTTP requests.

Once you have Maven installed
    In the Main Directory
        - mvn clean
        - mvn compile
        - mvn package (runs the tests aswell)

Manual Testing
Runs the aggregation server
    - java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.AggregationServer [port_number]
    java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.AggregationServer 4567

Runs the content server
    - java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.ContentServer http://localhost:4567 data/weather_data.txt

Runs the GET client
Gets all clients
    - java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.GETClient http://localhost:4567

Gets a specific client
    - java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.GETClient http://localhost:4567 [ID NUMBER]

PUT command
curl -X PUT http://localhost:4567/weather.json \
-H "Content-Type: application/json" \
-d '{"id":"IDS60901","name":"Adelaide (West Terrace /  ngayirdapira)","state":"SA","time_zone":"CST","lat":-34.9,"lon":138.6,"local_date_time":"15/04:00pm","local_date_time_full":"20230715160000","air_temp":13.3,"apparent_t":9.5,"cloud":"Partly cloudy","dewpt":5.7,"press":1023.9,"rel_hum":60,"wind_dir":"S","wind_spd_kmh":15,"wind_spd_kt":8}'


What Are Surefire Reports?
The Maven Surefire Plugin is used during the test phase of the build lifecycle to execute your project's unit tests. It generates detailed reports about the test execution, including which tests passed, failed, or were skipped, as well as any errors or failures encountered.

These reports are valuable for:

Diagnosing Test Failures: Understanding why tests are failing by providing stack traces and error messages.
Improving Test Coverage: Identifying which areas of the code may need more testing.
Continuous Integration: Integrating test results into CI/CD pipelines for automated builds.

Where to Find Surefire Reports
After running tests using Maven commands like mvn test or mvn package, the Surefire reports are generated in the following directory:
target/surefire-reports/

The reports include:
Plain Text Files (*.txt): Contain detailed information about each test case.
XML Files (*.xml): Structured data that can be used by CI tools or transformed into HTML.
Output Logs: Captured stdout and stderr outputs from the tests.

How to Use Surefire Reports
Navigate to the Directory:
cd target/surefire-reports/

Review the Reports:
Text Files:
Open the *.txt files to read detailed test results and error messages.
cat com.weather.aggregation.ContentServerTest.txt

XML Files:
Use an XML viewer or editor to examine the structured test results
less com.weather.aggregation.ContentServerTest.xml

Analyze Failures and Errors:
Look for <failure> or <error> tags in the XML files.
Read stack traces and error messages to identify issues.
