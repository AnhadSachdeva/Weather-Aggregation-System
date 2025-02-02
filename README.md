# Weather Aggregation System

## Prerequisites

Before setting up and running the Weather Aggregation System, ensure your system meets the following requirements:

- Operating System: Linux, macOS, or Windows
- Maven: Build automation tool for Java projects
- cURL (Optional): Command-line tool for sending HTTP requests

## Building the Project

In the main directory, run the following Maven commands:

```bash
mvn clean
mvn compile
mvn package    # This also runs the tests
```

## Manual Testing

### Run the Aggregation Server
To run the aggregation server, use the command:

```bash
java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.AggregationServer [port_number]
```

Example:
```bash
java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.AggregationServer 4567
```

## Runs the content server
```bash
java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.ContentServer http://localhost:4567 data/weather_data.txt
```

## Runs the GET client
Gets all clients
```bash
java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.GETClient http://localhost:4567
```

Gets a specific client
```bash
java -cp target/WeatherAggregationSystem-1.0-SNAPSHOT-shaded.jar com.weather.aggregation.GETClient http://localhost:4567 [ID NUMBER]
```

## PUT command
```bash
curl -X PUT http://localhost:4567/weather.json \
-H "Content-Type: application/json" \
-d '{"id":"IDS60901","name":"Adelaide (West Terrace /  ngayirdapira)","state":"SA","time_zone":"CST","lat":-34.9,"lon":138.6,"local_date_time":"15/04:00pm","local_date_time_full":"20230715160000","air_temp":13.3,"apparent_t":9.5,"cloud":"Partly cloudy","dewpt":5.7,"press":1023.9,"rel_hum":60,"wind_dir":"S","wind_spd_kmh":15,"wind_spd_kt":8}'
```
