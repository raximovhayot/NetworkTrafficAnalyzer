# Tarmoq Trafigi Tahlilchisi

Tarmoq trafigini ushlab olish va tahlil qilish uchun Java dasturi, DDoS hujumlarini va boshqa tarmoq anomaliyalarini aniqlash uchun. Endi sinov maqsadlarida oqim xususiyatlarini o'z ichiga olgan parquet fayllarini yuklash uchun API ham o'z ichiga oladi.

## Tavsif

Ushbu dastur ikki asosiy komponentdan iborat:

1. **Tarmoq Trafigi Tahlilchisi**: Tarmoq paketlarini ushlab oladi, ularni oqimlarga qayta ishlaydi, bu oqimlardan statistik xususiyatlarni ajratib oladi va tahlil va tasniflash uchun xususiyatlarni tashqi API ga yuboradi. U DDoS aniqlash tizimining bir qismi sifatida ishlatilishi mumkin.

2. **Parquet Yuklash API**: Sinov maqsadlarida oqim xususiyatlarini o'z ichiga olgan parquet fayllarini yuklash uchun veb-interfeys va REST API ni taqdim etadi. Bu ayniqsa CICDDOS2019 kabi ma'lumotlar to'plamlari bilan sinov o'tkazish uchun foydalidir.

## Xususiyatlar

### Tarmoq Trafigi Tahlilchisi
- pcap4j yordamida tarmoq paketlarini ushlab oladi
- Paketlarni ikki yo'nalishli oqimlarga qayta ishlaydi
- Har bir oqimdan 30 dan ortiq statistik xususiyatlarni ajratib oladi
- Oqim xususiyatlarini tahlil qilish uchun tashqi API ga yuboradi
- Sozlanuvchi tarmoq interfeysi, thread pool hajmi, timeoutlar va boshqalar
- Muvaffaqiyatsiz API so'rovlari uchun qayta urinish mexanizmi

### Parquet Yuklash API
- Parquet fayllarini yuklash uchun veb-interfeys
- Dasturiy yuklashlar uchun REST API endpointi
- CICDDOS2019 ma'lumotlar to'plami formatini qo'llab-quvvatlash
- Yuklangan fayllar uchun oddiy fayl saqlash tizimi

## Talablar

- Java 21 yoki undan yuqori
- Maven 3.6 yoki undan yuqori
- libpcap (paketlarni ushlab olish uchun)

## O'rnatish

1. libpcap o'rnatish:
   - Ubuntu/Debian: `sudo apt-get install libpcap-dev`
   - CentOS/RHEL: `sudo yum install libpcap-devel`
   - macOS: `brew install libpcap`
   - Windows: [WinPcap](https://www.winpcap.org/) yoki [Npcap](https://nmap.org/npcap/) o'rnating

2. Repozitoriyani klonlash:
   ```
   git clone https://github.com/yourusername/NetworkTraffiicAnalyzer.git
   cd NetworkTraffiicAnalyzer
   ```

3. Dasturni build qilish:
   ```
   mvn clean package
   ```

   Bu target direktoriyasida ikkita JAR fayl yaratadi:
   - `NetworkTraffiicAnalyzer-1.0-SNAPSHOT.jar`: Asosiy JAR fayl (bog'liqliklarisiz)
   - `NetworkTraffiicAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar`: Barcha bog'liqliklarni o'z ichiga olgan fat JAR

## Konfiguratsiya

Dastur `application.properties` fayli yordamida sozlanishi mumkin:

```properties
# Paketlarni ushlab olish uchun tarmoq interfeysi
# Birinchi mavjud interfeysdan ushlab olish uchun 'any' dan foydalaning yoki ma'lum bir interfeys nomini ko'rsating
network.interface=any

# Oqim xususiyatlarini yuborish uchun API endpointi
api.url=http://localhost:5000/api/network-data

# Paketlarni qayta ishlash uchun thread pool hajmi
thread.pool.size=4

# Oqim timeoutlari millisekundlarda
flow.timeout=60000

# HTTP so'rov timeoutlari millisekundlarda
http.timeout=5000

# Muvaffaqiyatsiz HTTP so'rovlarini qayta urinish soni
http.retry.count=3
```

## Foydalanish

### Tarmoq Trafigi Tahlilchisini Ishga Tushirish

Paket sniffer dasturini quyidagicha ishga tushiring:

```
java -jar target/NetworkTraffiicAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Eslatma: Barcha kerakli kutubxonalarni o'z ichiga olgan jar-with-dependencies versiyasidan foydalaning.

Dastur quyidagilarni bajaradi:
1. Sozlangan tarmoq interfeysidan paketlarni ushlab oladi
2. Paketlarni oqimlarga qayta ishlaydi
3. Tugallangan oqimlardan xususiyatlarni ajratib oladi
4. Xususiyatlarni sozlangan API endpointiga yuboradi

### Parquet Yuklash API-ni Ishga Tushirish

API serverni quyidagicha ishga tushiring:

```
java -cp target/NetworkTraffiicAnalyzer-1.0-SNAPSHOT-jar-with-dependencies.jar api.Application
```

Server 8080 portida quyidagi endpointlar bilan ishga tushadi:
- `http://localhost:8080/` - Parquet fayllarini yuklash uchun veb-interfeys
- `http://localhost:8080/api/upload` - Fayllarni yuklash uchun REST API endpointi

### Parquet Yuklash API-dan Foydalanish

#### Veb-interfeys
1. Veb-brauzer oching va `http://localhost:8080/` manziliga o'ting
2. Parquet faylini tanlash va yuklash uchun formadan foydalaning
3. Fayl `uploads` direktoriyasida noyob nom bilan saqlanadi

#### REST API
Siz REST API yordamida dasturiy ravishda fayllarni yuklashingiz mumkin:

```
POST /api/upload
Content-Type: multipart/form-data

file=@path/to/your/file.parquet
```

curl yordamida misol:
```
curl -X POST -F "file=@DNS-testing.parquet" http://localhost:8080/api/upload
```

#### Misol Ma'lumotlar To'plami
Ushbu API CICDDOS2019 ma'lumotlar to'plami bilan ishlash uchun mo'ljallangan, xususan DNS-testing.parquet fayli, uni quyidagi manzilda topish mumkin:
https://www.kaggle.com/datasets/dhoogla/cicddos2019?select=DNS-testing.parquet

## Arxitektura

Dastur bir nechta asosiy komponentlardan iborat:

### Tarmoq Trafigi Tahlilchisi Komponentlari
- **PacketSniffer**: Paketlarni ushlab oluvchi va oqimlarni boshqaruvchi asosiy klass
- **Flow**: Tarmoq oqimini ifodalaydi va statistikani to'playdi
- **FlowFeatures**: Oqimdan ajratib olingan statistik xususiyatlarni o'z ichiga oladi
- **FeatureSender**: Oqim xususiyatlarini tashqi API ga yuboradi
- **AppConfig**: Dastur konfiguratsiyasini boshqaradi

### Parquet Yuklash API Komponentlari
- **Application**: API server uchun asosiy kirish nuqtasi
- **FileUploadHandler**: Fayl yuklash so'rovlarini qayta ishlaydi va yuklangan fayllarni saqlaydi
- **FileUploadFormHandler**: Fayl yuklash uchun HTML formani taqdim etadi
- **uploads/**: Yuklangan parquet fayllar saqlanadigan direktoriya
