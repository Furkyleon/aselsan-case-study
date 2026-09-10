# Queue Monitor Frontend

Queue Monitor backend'ini yöneten ve saniyelik metriklerini görselleştiren React
dashboard'udur. Uygulama Vite geliştirme sunucusunda çalışır ve varsayılan olarak
`/api` isteklerini `http://localhost:8080` adresine proxy'ler.

Projenin genel kurulumu ve mimarisi için [ana README'ye](../README.md), ayrıntılı
endpoint sözleşmesi için [backend README'ye](../backend/README.md) bakın.

## Başlangıç

Backend çalışırken:

```bash
npm install
npm run dev
```

Dashboard `http://localhost:5173` adresinde açılır.

API başka bir adreste çalışıyorsa tam simulation endpoint'i verilebilir:

```bash
VITE_API_BASE_URL=http://localhost:8081/api/simulation npm run dev
```

## Scriptler

| Komut | Açıklama |
| --- | --- |
| `npm run dev` | Vite geliştirme sunucusunu başlatır |
| `npm run build` | Optimize production çıktısını `dist/` altında üretir |
| `npm run preview` | Production build'i yerel olarak sunar |
| `npm run lint` | JavaScript ve JSX dosyalarını ESLint ile kontrol eder |
| `npm test` | Tüm Vitest testlerini bir kez çalıştırır |
| `npm run test:watch` | Testleri izleme modunda çalıştırır |

## Dashboard bölümleri

- **Queue durumu:** Doluluk oranı, kapasite ve bekleyen mesaj sayısı
- **Mesaj akışı:** Toplam gönderilen/alınan mesaj ve saniyelik hızlar
- **Simülasyon kontrolü:** Başlangıç worker sayıları ve queue kapasitesi
- **Worker kartları:** JVM state dağılımı, ekleme/azaltma ve grup priority kontrolü
- **Worker registry:** UUID, JVM state, aktivite, priority, mesaj sayısı ve tekil durdurma
- **Telemetri:** Son 60 ölçümde queue doluluğu, throughput ve aktif worker grafikleri

## Veri akışı

`useSimulation` hook'u:

1. Uygulama açıldığında `/api/simulation/status` isteği yapar.
2. Durumu varsayılan olarak her 1 saniyede yeniler.
3. Aynı anda birden fazla polling isteğinin açılmasını engeller.
4. Mutation cevaplarını doğrudan ekrana uygular.
5. Son 60 benzersiz ölçümü tarayıcı belleğinde tutar.
6. Ardışık toplam mesaj sayaçlarından üretim/tüketim hızlarını hesaplar.
7. Simülasyon durduğunda son ölçümü korur, yeni simülasyonda geçmişi sıfırlar.

API erişimi ve hata dönüşümü `src/api/simulationApi.js`, polling ve dashboard
state'i `src/hooks/useSimulation.js` içinde bulunur.

## Priority davranışı

Worker kartındaki slider server'ın döndürdüğü uygulanmış değerden başlar. Yeni bir
değer seçildiğinde mevcut değer ile seçilen değer ayrı gösterilir. “Uygula” işlemi
seçilen tipteki tüm aktif worker'ları günceller. Backend grup priority'sini
sakladığı için sonradan eklenen worker aynı değeri devralır.

Priority bir scheduler ipucudur; doğrudan hız kontrolü olarak değerlendirilmemelidir.

## Hata ve işlem durumları

- Bağlantı hataları üst banner'da gösterilir ve yeniden deneme sunulur.
- Backend validation hataları ilgili form alanının altında gösterilir.
- Başarılı mutation işlemleri geçici bildirim üretir.
- Her işlem kendi bekleme durumunu gösterir.
- Tüm worker'ları durdurma işlemi onay dialog'u gerektirir.

## Testler

Test altyapısı Vitest, React Testing Library, user-event, jest-dom ve jsdom
kullanır.

```bash
npm test
```

Mevcut test kapsamı:

- API payload'ları ve RFC 9457 hata dönüşümü
- Başlangıç formunun client/server validation davranışı
- Stop confirmation klavye ve busy durumları
- Backend kaynaklı priority değeri ve mutation payload'ı
- Worker registry açma, boş durum ve tekil durdurma
- Mesaj hızlarının hesaplanması
- 60 ölçüm sınırı, idle durumun korunması ve yeni simülasyonda reset

Yeni testler ilgili kaynak dosyasının yanında `*.test.jsx` veya `*.test.js`
adıyla tutulmalıdır. Ortak test kurulumu `src/test/setup.js` içindedir.

## Dizin yapısı

```text
src/
├── api/
│   └── simulationApi.js
├── components/
│   ├── QueueCard.jsx
│   ├── StartSimulationForm.jsx
│   ├── TelemetryPanel.jsx
│   ├── WorkerCard.jsx
│   └── WorkerDetailsPanel.jsx
├── hooks/
│   └── useSimulation.js
├── test/
│   └── setup.js
├── App.jsx
├── App.css
└── main.jsx
```

## Bilinen sınırlar

- Grafik geçmişi sayfa yenilendiğinde kaybolur.
- Backend erişilemezse son başarılı status ekranda kalabilir ve hata banner'ı görünür.
- Canlı veri polling ile alınır; bağlantı başına saniyede yaklaşık bir status isteği oluşur.
- UI aynı anda tek bir mutation çalıştırır.
