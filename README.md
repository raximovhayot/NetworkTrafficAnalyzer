# Network Traffic Analyzer | Tarmoq Trafigi Tahlilchisi

A Java application for capturing and analyzing network traffic to detect DDoS attacks and other network anomalies. Now includes an API for uploading parquet files containing flow features for testing purposes.

*Tarmoq trafigini ushlab olish va tahlil qilish uchun Java dasturi, DDoS hujumlarini va boshqa tarmoq anomaliyalarini aniqlash uchun. Endi sinov maqsadlarida oqim xususiyatlarini o'z ichiga olgan parquet fayllarini yuklash uchun API ham o'z ichiga oladi.*

## Description | Tavsif

This application has two main components:

*Ushbu dastur ikki asosiy komponentdan iborat:*

1. **Network Traffic Analyzer**: Captures network packets, processes them into flows, extracts statistical features from these flows, and sends the features to an external API for analysis and classification. It can be used as part of a DDoS detection system.

   ***Tarmoq Trafigi Tahlilchisi**: Tarmoq paketlarini ushlab oladi, ularni oqimlarga qayta ishlaydi, bu oqimlardan statistik xususiyatlarni ajratib oladi va tahlil va tasniflash uchun xususiyatlarni tashqi API ga yuboradi. U DDoS aniqlash tizimining bir qismi sifatida ishlatilishi mumkin.*

2. **Parquet Upload API**: Provides a web interface and REST API for uploading parquet files containing flow features for testing purposes. This is particularly useful for testing with datasets like CICDDOS2019.

   ***Parquet Yuklash API**: Sinov maqsadlarida oqim xususiyatlarini o'z ichiga olgan parquet fayllarini yuklash uchun veb-interfeys va REST API ni taqdim etadi. Bu ayniqsa CICDDOS2019 kabi ma'lumotlar to'plamlari bilan sinov o'tkazish uchun foydalidir.*

## Features | Xususiyatlar

### Network Traffic Analyzer | Tarmoq Trafigi Tahlilchisi
- Captures network packets using pcap4j
  *pcap4j yordamida tarmoq paketlarini ushlab oladi*
- Processes packets into bidirectional flows
  *Paketlarni ikki yo'nalishli oqimlarga qayta ishlaydi*
- Extracts over 30 statistical features from each flow
  *Har bir oqimdan 30 dan ortiq statistik xususiyatlarni ajratib oladi*
- Sends flow features to an external API for analysis
  *Oqim xususiyatlarini tahlil qilish uchun tashqi API ga yuboradi*
- Configurable network interface, thread pool size, timeouts, etc.
  *Sozlanuvchi tarmoq interfeysi, thread pool hajmi, timeoutlar va boshqalar*
- Retry mechanism for failed API requests
  *Muvaffaqiyatsiz API so'rovlari uchun qayta urinish mexanizmi*

### Parquet Upload API | Parquet Yuklash API
- Web interface for uploading parquet files
  *Parquet fayllarini yuklash uchun veb-interfeys*
- REST API endpoint for programmatic uploads
  *Dasturiy yuklashlar uchun REST API endpointi*
- Support for the CICDDOS2019 dataset format
  *CICDDOS2019 ma'lumotlar to'plami formatini qo'llab-quvvatlash*
- Simple file storage system for uploaded files
  *Yuklangan fayllar uchun oddiy fayl saqlash tizimi*

## Requirements | Talablar

- Java 21 or higher
  *Java 21 yoki undan yuqori*
- Maven 3.6 or higher
  *Maven 3.6 yoki undan yuqori*
- libpcap (for packet capturing)
  *libpcap (paketlarni ushlab olish uchun)*

## Installation | O'rnatish

1. Install libpcap:
   *libpcap o'rnatish:*
   - Ubuntu/Debian: `sudo apt-get install libpcap-dev`
   - CentOS/RHEL: `sudo yum install libpcap-devel`
   - macOS: `brew install libpcap`
   - Windows: Install [WinPcap](https://www.winpcap.org/) or [Npcap](https://nmap.org/npcap/)
     *Windows: [WinPcap](https://www.winpcap.org/) yoki [Npcap](https://nmap.org/npcap/) o'rnating*

2. Clone the repository:
   *Repozitoriyani klonlash:*
   ```
   git clone https://github.com/yourusername/NetworkTraffiicAnalyzer.git
   cd NetworkTraffiicAnalyzer
   ```

3. Build the application:
   *Dasturni build qilish:*
   ```
   mvn clean package
   ```

   This will create two JAR files in the target directory:
   *Bu target direktoriyasida ikkita JAR fayl yaratadi:*
   - `NetworkTraffiicAnalyzer-1.0-SNAPSHOT.jar`: The main JAR file (without dependencies)
     *Asosiy JAR fayl (bog'liqliklarisiz)*
   - `NetworkTraffiicAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar`: A fat JAR with all dependencies included
     *Barcha bog'liqliklarni o'z ichiga olgan fat JAR*

## Configuration | Konfiguratsiya

The application can be configured using the `application.properties` file:
*Dastur `application.properties` fayli yordamida sozlanishi mumkin:*

```properties
# Network interface to capture packets from
# Use 'any' to capture from the first available interface, or specify a particular interface name
# Paketlarni ushlab olish uchun tarmoq interfeysi
# Birinchi mavjud interfeysdan ushlab olish uchun 'any' dan foydalaning yoki ma'lum bir interfeys nomini ko'rsating
network.interface=any

# API endpoint to send flow features to
# Oqim xususiyatlarini yuborish uchun API endpointi
api.url=http://localhost:5000/api/network-data

# Thread pool size for packet processing
# Paketlarni qayta ishlash uchun thread pool hajmi
thread.pool.size=4

# Flow timeout in milliseconds
# Oqim timeoutlari millisekundlarda
flow.timeout=60000

# HTTP request timeout in milliseconds
# HTTP so'rov timeoutlari millisekundlarda
http.timeout=5000

# Number of times to retry failed HTTP requests
# Muvaffaqiyatsiz HTTP so'rovlarini qayta urinish soni
http.retry.count=3
```

## Usage | Foydalanish

### Running the Network Traffic Analyzer | Tarmoq Trafigi Tahlilchisini Ishga Tushirish

Run the packet sniffer application with:
*Paket sniffer dasturini quyidagicha ishga tushiring:*

```
java -jar target/NetworkTraffiicAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Note: Use the jar-with-dependencies version as it includes all required libraries.
*Eslatma: Barcha kerakli kutubxonalarni o'z ichiga olgan jar-with-dependencies versiyasidan foydalaning.*

The application will:
*Dastur quyidagilarni bajaradi:*
1. Capture packets from the configured network interface
   *Sozlangan tarmoq interfeysidan paketlarni ushlab oladi*
2. Process the packets into flows
   *Paketlarni oqimlarga qayta ishlaydi*
3. Extract features from completed flows
   *Tugallangan oqimlardan xususiyatlarni ajratib oladi*
4. Send the features to the configured API endpoint
   *Xususiyatlarni sozlangan API endpointiga yuboradi*

### Running the Parquet Upload API | Parquet Yuklash API-ni Ishga Tushirish

Run the API server with:
*API serverni quyidagicha ishga tushiring:*

```
java -cp target/NetworkTraffiicAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar api.Application
```

The server will start on port 8080 with the following endpoints:
*Server 8080 portida quyidagi endpointlar bilan ishga tushadi:*
- `http://localhost:8080/` - Web interface for uploading parquet files
  *Parquet fayllarini yuklash uchun veb-interfeys*
- `http://localhost:8080/api/upload` - REST API endpoint for file uploads
  *Fayllarni yuklash uchun REST API endpointi*

### Using the Parquet Upload API | Parquet Yuklash API-dan Foydalanish

#### Web Interface | Veb-interfeys
1. Open a web browser and navigate to `http://localhost:8080/`
   *Veb-brauzer oching va `http://localhost:8080/` manziliga o'ting*
2. Use the form to select and upload a parquet file
   *Parquet faylini tanlash va yuklash uchun formadan foydalaning*
3. The file will be stored in the `uploads` directory with a unique name
   *Fayl `uploads` direktoriyasida noyob nom bilan saqlanadi*

#### REST API
You can also upload files programmatically using the REST API:
*Siz REST API yordamida dasturiy ravishda fayllarni yuklashingiz mumkin:*

```
POST /api/upload
Content-Type: multipart/form-data

file=@path/to/your/file.parquet
```

Example using curl:
*curl yordamida misol:*
```
curl -X POST -F "file=@DNS-testing.parquet" http://localhost:8080/api/upload
```

#### Example Dataset | Misol Ma'lumotlar To'plami
This API is designed to work with the CICDDOS2019 dataset, specifically the DNS-testing.parquet file, which can be found at:
*Ushbu API CICDDOS2019 ma'lumotlar to'plami bilan ishlash uchun mo'ljallangan, xususan DNS-testing.parquet fayli, uni quyidagi manzilda topish mumkin:*
https://www.kaggle.com/datasets/dhoogla/cicddos2019?select=DNS-testing.parquet

## Architecture | Arxitektura

The application consists of several main components:
*Dastur bir nechta asosiy komponentlardan iborat:*

### Network Traffic Analyzer Components | Tarmoq Trafigi Tahlilchisi Komponentlari
- **PacketSniffer**: The main class that captures packets and manages flows
  *Paketlarni ushlab oluvchi va oqimlarni boshqaruvchi asosiy klass*
- **Flow**: Represents a network flow and collects statistics
  *Tarmoq oqimini ifodalaydi va statistikani to'playdi*
- **FlowFeatures**: Contains the statistical features extracted from a flow
  *Oqimdan ajratib olingan statistik xususiyatlarni o'z ichiga oladi*
- **FeatureSender**: Sends flow features to the external API
  *Oqim xususiyatlarini tashqi API ga yuboradi*
- **AppConfig**: Manages application configuration
  *Dastur konfiguratsiyasini boshqaradi*

### Parquet Upload API Components | Parquet Yuklash API Komponentlari
- **Application**: The main entry point for the API server
  *API server uchun asosiy kirish nuqtasi*
- **FileUploadHandler**: Handles file upload requests and stores uploaded files
  *Fayl yuklash so'rovlarini qayta ishlaydi va yuklangan fayllarni saqlaydi*
- **FileUploadFormHandler**: Serves the HTML form for file uploads
  *Fayl yuklash uchun HTML formani taqdim etadi*
- **uploads/**: Directory where uploaded parquet files are stored
  *Yuklangan parquet fayllar saqlanadigan direktoriya*
