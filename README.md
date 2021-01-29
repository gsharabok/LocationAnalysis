# LocationAnalysis

The program takes url of a JSON file and distance as input, and outputs a CSV file containing
the approximate home location of a user, first entry date, UID, time spent outside of home.
All the downloaded data is stored in the data folder inside the current directory for quick use.
If the folder is empty, the data will be automatically downloaded, otherwise the data inside the folder is used.
The data can also be stored directly into the data folder in JSON format other than using a link.
<br>
MAX_DISTANCE - Distance that will be used to measure home location in meters
URL - link from which the user data will be extracted (without the key value)
