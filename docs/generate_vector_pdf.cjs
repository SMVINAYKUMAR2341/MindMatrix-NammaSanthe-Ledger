const puppeteer = require('puppeteer');
const path = require('path');

const html = `<!DOCTYPE html><html><head><meta charset="utf-8">
<script src="https://cdn.jsdelivr.net/npm/mermaid@11/dist/mermaid.min.js"></script>
<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;600;700;800&display=swap');
*{margin:0;padding:0;box-sizing:border-box}
body{font-family:'Inter',sans-serif;color:#111827;line-height:1.6}
.cover{height:100vh;display:flex;flex-direction:column;justify-content:center;align-items:center;background:linear-gradient(145deg,#0f172a,#1e293b);color:#fff;text-align:center;page-break-after:always}
.cover h1{font-size:44px;font-weight:800;letter-spacing:-1px}.cover h2{font-size:18px;font-weight:300;opacity:.8;margin:8px 0 30px}
.tag{display:inline-block;background:rgba(255,255,255,.1);border:1px solid rgba(255,255,255,.2);padding:4px 14px;border-radius:14px;margin:3px;font-size:11px}
.page{padding:28px 36px;page-break-after:always}
h1{font-size:24px;font-weight:800;color:#0f172a;border-bottom:3px solid #3b82f6;padding-bottom:6px;margin:0 0 14px}
h2{font-size:17px;font-weight:700;color:#1e3a5f;margin:16px 0 8px}
h3{font-size:14px;font-weight:600;color:#4338ca;margin:12px 0 6px}
p{font-size:12px;margin:5px 0}
ul,ol{font-size:12px;margin:5px 0 5px 18px}
li{margin:2px 0}
table{width:100%;border-collapse:collapse;margin:8px 0;font-size:11px}
th{background:#1e293b;color:#fff;padding:6px 8px;text-align:left;font-weight:600}
td{padding:5px 8px;border:1px solid #e2e8f0}
tr:nth-child(even){background:#f8fafc}
.mermaid{margin:12px auto;text-align:center}
.mermaid svg{max-width:100%!important;height:auto!important}
.caption{font-size:10px;color:#64748b;text-align:center;font-style:italic;margin:4px 0 12px}
.code{background:#0f172a;color:#e2e8f0;padding:12px;border-radius:6px;font-family:Consolas,monospace;font-size:9.5px;white-space:pre;line-height:1.4;margin:8px 0;overflow:hidden}
footer{text-align:center;color:#94a3b8;font-size:9px;padding:16px}
</style>
</head><body>

<!-- COVER -->
<div class="cover">
<div style="font-size:56px;margin-bottom:12px">📗</div>
<h1>Namma-Santhe Ledger</h1>
<h2>Technical Documentation Report</h2>
<div style="width:40px;height:3px;background:#3b82f6;margin:12px auto;border-radius:2px"></div>
<div style="margin:16px 0">
<span class="tag">Android • Kotlin</span><span class="tag">Jetpack Compose</span><span class="tag">MVVM + Clean Architecture</span>
<span class="tag">Offline-First</span><span class="tag">Firebase</span><span class="tag">Gemini OCR</span><span class="tag">QR Verification</span>
</div>
<div style="font-size:12px;opacity:.5;margin-top:20px">Prepared for Academic & Project Evaluation — May 2026</div>
</div>

<!-- TOC -->
<div class="page">
<h1>Table of Contents</h1>
<ol style="font-size:13px;columns:2;margin-top:16px">
<li>System Overview</li><li>System Architecture</li><li>UML Class Diagram</li><li>Use Case Diagram</li>
<li>Sequence Diagram</li><li>Data Flow Diagram</li><li>App Navigation Flow</li><li>OCR Pipeline</li>
<li>QR Confirmation System</li><li>Security Architecture</li><li>ER Diagram</li><li>Project Structure</li><li>Technology Stack</li>
</ol>
</div>

<!-- 1. OVERVIEW -->
<div class="page">
<h1>1. System Overview</h1>
<h2>Problem Statement</h2>
<p>Rural vendors in Indian markets maintain handwritten ledgers for credit tracking, leading to lost records, calculation errors, and no backups.</p>
<h2>Solution</h2>
<p>An <strong>offline-first Android app</strong> that digitizes credit tracking with automatic balance calculation, QR confirmation, OCR scanning, cloud backup, and PDF export.</p>
<h2>Key Features</h2>
<table><tr><th>Feature</th><th>Description</th><th>Technology</th></tr>
<tr><td>Quick Entry</td><td>Numeric keypad for &lt;5 sec entry</td><td>Jetpack Compose</td></tr>
<tr><td>Customer Mgmt</td><td>Photo capture, search, profiles</td><td>CameraX, Coil</td></tr>
<tr><td>Credit Tracking</td><td>Auto-calculated balances</td><td>Room DB</td></tr>
<tr><td>QR Confirmation</td><td>Tamper-proof verification</td><td>ZXing, HMAC-SHA256</td></tr>
<tr><td>OCR Bill Scan</td><td>Handwritten/printed bill extraction</td><td>Gemini ML + Tesseract</td></tr>
<tr><td>Cloud Sync</td><td>Firebase Auth + Firestore</td><td>Firebase</td></tr>
<tr><td>PDF Export</td><td>Shareable ledger reports</td><td>iText7</td></tr>
<tr><td>Offline Mode</td><td>Full functionality offline</td><td>Room + SQLite</td></tr>
</table>
</div>

<!-- 2. ARCHITECTURE -->
<div class="page">
<h1>2. System Architecture</h1>
<p>4-layer <strong>MVVM + Clean Architecture</strong> with dependency flow top-to-bottom.</p>
<div class="mermaid">
graph TB
  subgraph PL["🎨 PRESENTATION LAYER — Jetpack Compose"]
    H[HomeScreen] --- CS[CustomerScreen] --- LS[LedgerScreen] --- SS[ScannerScreen] --- PS[ProfileScreen] --- QS[QR Screens]
  end
  subgraph VL["⚙️ VIEWMODEL LAYER — MVVM + StateFlow"]
    LVM[LedgerVM] --- PVM[ProfileVM] --- OVM[OcrVM] --- CVM[ConfirmationVM] --- MVM[MainVM]
  end
  subgraph RL["📦 REPOSITORY LAYER — Data Abstraction"]
    LR[LedgerRepository] --- SR[SyncRepository] --- EM[ExportManager] --- PM[PhotoManager]
  end
  subgraph DL["💾 DATA LAYER — Persistence & Services"]
    RDB[(Room DB)] --- FA[Firebase Auth] --- FS[Firestore] --- GM[Gemini ML] --- ZX[ZXing QR] --- IT[iText7 PDF]
  end
  PL ==> VL ==> RL ==> DL
  style PL fill:#dbeafe,stroke:#2563eb,stroke-width:2px
  style VL fill:#d1fae5,stroke:#059669,stroke-width:2px
  style RL fill:#fef3c7,stroke:#d97706,stroke-width:2px
  style DL fill:#ede9fe,stroke:#7c3aed,stroke-width:2px
</div>
<div class="caption">Figure 2.1 — Four-layer system architecture with dependency flow</div>
</div>

<!-- 3. UML CLASS -->
<div class="page">
<h1>3. UML Class Diagram</h1>
<div class="mermaid">
classDiagram
  class Customer {
    +Long id
    +String name
    +String phone
    +String address
    +String? photoPath
    +Long createdAt
    +getInitials() String
  }
  class Transaction {
    +Long id
    +Long customerId
    +Double amount
    +TxnType type
    +String? note
    +Long date
    +Boolean confirmed
    +String? qrHash
    +isCredit() Boolean
  }
  class QrPayload {
    +Long txnId
    +Double amount
    +String nonce
    +Long timestamp
    +Long expiresAt
    +String hash
  }
  class ScanConfirmation {
    +Long scanId
    +Long txnId
    +TrustLevel trustLevel
    +String payloadHash
  }
  class LedgerRepository {
    +insertCustomer()
    +insertTransaction()
    +getBalance()
    +searchCustomers()
  }
  Customer "1" --> "*" Transaction : has many
  Transaction "1" --> "0..1" ScanConfirmation : may have
  LedgerRepository --> Customer : manages
  LedgerRepository --> Transaction : manages
</div>
<div class="caption">Figure 3.1 — UML Class Diagram with entity relationships</div>
<h3>Key Relationships</h3>
<table><tr><th>From</th><th>To</th><th>Type</th></tr>
<tr><td>Customer</td><td>Transaction</td><td>One-to-Many</td></tr>
<tr><td>Transaction</td><td>ScanConfirmation</td><td>One-to-Optional</td></tr>
<tr><td>ViewModel</td><td>Repository</td><td>Dependency</td></tr>
</table>
</div>

<!-- 4. USE CASE -->
<div class="page">
<h1>4. Use Case Diagram</h1>
<div class="mermaid">
graph LR
  V((👤 VENDOR)) --> MC[Manage Customers]
  V --> PT[Process Transactions]
  V --> BE[Backup & Export]
  C((👥 CUSTOMER)) --> SQ[Scan QR to Confirm]
  MC --> AC[Add Customer]
  MC --> EC[Edit Customer]
  MC --> PC[Photo Capture]
  MC --> SC[Search]
  PT --> QE[Quick Entry]
  PT --> VL[View Ledger]
  PT --> OS[OCR Scan]
  PT --> GQ[Generate QR]
  BE --> EP[Export PDF]
  BE --> CS[Cloud Sync]
  BE --> SW[Share WhatsApp]
  SQ --> VQ[Validate QR]
  VQ --> DT[Detect Tampering]
  GQ -.->|include| CB[Calculate Balance]
  OS -.->|extend| PT
  style V fill:#dbeafe,stroke:#2563eb
  style C fill:#d1fae5,stroke:#059669
</div>
<div class="caption">Figure 4.1 — Use Case Diagram: Vendor (primary) and Customer (secondary) actors</div>
</div>

<!-- 5. SEQUENCE -->
<div class="page">
<h1>5. Sequence Diagram — Add Transaction</h1>
<div class="mermaid">
sequenceDiagram
  actor U as User/Vendor
  participant ES as EntryScreen
  participant VM as LedgerVM
  participant R as Repository
  participant DB as Room DB
  U->>ES: Enter amount, Tap Save
  ES->>VM: onSave(amount, type)
  VM->>R: insertTransaction(txn)
  R->>DB: INSERT INTO transactions
  DB-->>R: Success
  R-->>VM: Result TxnEntity
  VM->>R: getBalance(customerId)
  R->>DB: SELECT SUM(amount)
  DB-->>R: balance: Double
  VM-->>ES: StateFlow UI Update
  ES-->>U: Show updated balance
  VM->>VM: generateQR(txnId)
  VM-->>ES: QR Bitmap ready
  ES-->>U: Display QR Code (60s TTL)
</div>
<div class="caption">Figure 5.1 — Sequence diagram for the Add Transaction flow</div>
</div>

<!-- 6. DFD -->
<div class="page">
<h1>6. Data Flow Diagram (Level 1)</h1>
<div class="mermaid">
graph TB
  VE[/"👤 VENDOR"/] -->|Amount, Type| P1((P1: Transaction<br/>Processor))
  VE -->|Bill Image| P2((P2: OCR<br/>Pipeline))
  P2 -->|Parsed Items| P1
  P1 -->|Store| D2[(D2: Transaction DB)]
  P1 -->|Trigger| P3((P3: Balance<br/>Calculator))
  P3 -->|Query SUM| D2
  P1 -->|Generate| P4((P4: QR Gen/<br/>Validator))
  P4 -->|Store Hash| D5[(D5: QR Hash Store)]
  P1 -->|Log| D6[(D6: Audit Log)]
  VE -->|Export| P5((P5: PDF Export))
  P5 -->|Query| D2
  P5 -->|PDF File| VE
  P1 -->|Sync| FC[/"☁️ FIREBASE"/]
  VE -->|Profile| D1[(D1: Customer DB)]
  VE -->|Photos| D3[(D3: Photo Storage)]
  style VE fill:#dbeafe,stroke:#2563eb
  style FC fill:#fef3c7,stroke:#d97706
  style D1 fill:#f0fdf4,stroke:#059669
  style D2 fill:#f0fdf4,stroke:#059669
  style D3 fill:#f0fdf4,stroke:#059669
  style D5 fill:#f0fdf4,stroke:#059669
  style D6 fill:#fef2f2,stroke:#dc2626
</div>
<div class="caption">Figure 6.1 — Level 1 Data Flow Diagram with data stores</div>
</div>

<!-- 7. NAV FLOW -->
<div class="page">
<h1>7. App Navigation Flow</h1>
<div class="mermaid">
graph TB
  SP[Splash Screen] --> OB{Onboarding<br/>Complete?}
  OB -->|No| ON[Onboarding Screen]
  OB -->|Yes| AU{Signed In?}
  ON --> LG[Login Screen]
  AU -->|No| LG
  AU -->|Yes| PIN{PIN Set?}
  LG --> SU[Signup Screen]
  SU --> HM
  LG --> HM
  PIN -->|Yes| PG[PIN Gate]
  PIN -->|No| HM
  PG --> HM
  subgraph NAV["Bottom Navigation Bar"]
    HM[🏠 Home<br/>Dashboard]
    CL[👥 Customers<br/>List]
    SC[📷 Scanner<br/>OCR]
    RP[📊 Reports]
  end
  CL --> CD[Customer Detail] --> CLD[Customer Ledger]
  CLD --> TE[Txn Entry] --> QD[QR Display]
  QD --> QSC[QR Scanner Confirm]
  SC --> SR[Scan Result]
  HM --> PR[Profile Screen] --> GS[Gemini Settings]
  style NAV fill:#eff6ff,stroke:#3b82f6,stroke-width:2px
  style HM fill:#dbeafe,stroke:#2563eb
  style CL fill:#d1fae5,stroke:#059669
  style SC fill:#fef3c7,stroke:#d97706
  style RP fill:#ede9fe,stroke:#7c3aed
</div>
<div class="caption">Figure 7.1 — Complete app navigation flow with auth gates</div>
</div>

<!-- 8. OCR PIPELINE -->
<div class="page">
<h1>8. OCR Pipeline Architecture</h1>
<div class="mermaid">
graph TB
  subgraph INPUT["📷 STAGE 1: INPUT"]
    CAM[CameraX Capture] --> IMG[ImageProxy]
    HW[Handwritten] & PR[Printed] & KN[Kannada] & MX[Mixed Lang] --> CAM
  end
  subgraph PREPROCESS["🔧 STAGE 2: PREPROCESSING"]
    IMG --> SC1[Scale to 1200px]
    SC1 --> GR[Grayscale Convert]
    GR --> AT[Adaptive Threshold<br/>Integral Image]
    GR --> OT[Otsu Binarization]
  end
  subgraph ML["🧠 STAGE 3: DUAL-ENGINE OCR"]
    AT --> TS[Tesseract<br/>Kannada Script]
    OT --> TS
    IMG --> MK[ML Kit<br/>Latin + Numbers]
    TS --> MG[Result Merger<br/>Best Score Selection]
    MK --> MG
  end
  subgraph PARSE["📊 STAGE 4: PARSING"]
    MG --> TK[LineTokenizer]
    TK --> BLP[BillLineParser]
    BLP --> GD[GroceryDictionary<br/>150+ items, Levenshtein]
  end
  subgraph OUT["✅ STAGE 5: OUTPUT"]
    GD --> BI["BillItem[]"]
    BI --> UI[Editable UI Display]
    UI --> SV[Save as Transaction]
  end
  style INPUT fill:#dbeafe,stroke:#2563eb,stroke-width:2px
  style PREPROCESS fill:#d1fae5,stroke:#059669,stroke-width:2px
  style ML fill:#fef3c7,stroke:#d97706,stroke-width:2px
  style PARSE fill:#ede9fe,stroke:#7c3aed,stroke-width:2px
  style OUT fill:#fef2f2,stroke:#dc2626,stroke-width:2px
</div>
<div class="caption">Figure 8.1 — Multi-engine OCR pipeline with preprocessing and parsing</div>
</div>

<!-- 9. QR SYSTEM -->
<div class="page">
<h1>9. QR Confirmation System</h1>
<div class="mermaid">
graph TB
  subgraph GEN["🔵 QR GENERATION — Vendor"]
    TS2[Transaction Saved] --> BP[Build Payload<br/>txnId+amount+type+nonce]
    BP --> HS[SHA-256 Hash<br/>with SECRET_KEY]
    HS --> EX[Set Expiry<br/>timestamp + 60s TTL]
    EX --> QR[ZXing Encode<br/>512x512 QR Bitmap]
  end
  subgraph VAL["🟢 QR VALIDATION — Customer"]
    SN[Scan QR Code] --> PJ[Parse JSON Payload]
    PJ --> V1{1. Expiry<br/>Check}
    V1 -->|Pass| V2{2. Hash<br/>Verify}
    V2 -->|Pass| V3{3. Txn<br/>Exists}
    V3 -->|Pass| V4{4. Amount<br/>Match}
    V4 -->|Pass| V5{5. Nonce<br/>Reuse}
    V5 -->|Pass| TA[Trust Assessment]
  end
  subgraph TRUST["Trust Levels"]
    T1["🟢 TRUSTED — All pass"]
    T2["🔵 VERIFIED — 3/4 pass"]
    T3["🟡 CAUTION — 2/4 pass"]
    T4["🔴 UNTRUSTED — <2 pass"]
  end
  TA --> TRUST
  QR -.->|Customer Scans| SN
  style GEN fill:#dbeafe,stroke:#2563eb,stroke-width:2px
  style VAL fill:#d1fae5,stroke:#059669,stroke-width:2px
  style TRUST fill:#fefce8,stroke:#ca8a04,stroke-width:2px
</div>
<div class="caption">Figure 9.1 — QR generation and multi-step validation flow</div>
</div>

<!-- 10. SECURITY -->
<div class="page">
<h1>10. Security & Tamper Protection</h1>
<div class="mermaid">
graph TB
  subgraph THREATS["🔴 THREAT MODEL"]
    T1[Data Tampering] & T2[Replay Attack] & T3[Unauthorized Access] & T4[Device Theft]
  end
  subgraph DEFENSE["🟢 DEFENSE MECHANISMS"]
    D1["Append-Only Ledger<br/>Txns never deleted"]
    D2["Hash Chain<br/>Blockchain-style linking"]
    D3["Audit Log<br/>Immutable action log"]
    D4["Anomaly Detection<br/>Amount/Time/Frequency"]
  end
  subgraph CRYPTO["🔵 ENCRYPTION"]
    E1["Data at Rest<br/>SQLCipher AES-256"]
    E2["Data in Transit<br/>TLS 1.3 + HMAC"]
    E3["Auth<br/>Argon2id + Biometric"]
    E4["Key Storage<br/>Android Keystore"]
  end
  T1 -->|mitigated by| D1
  T1 -->|mitigated by| D2
  T2 -->|mitigated by| D4
  T3 -->|mitigated by| E3
  T4 -->|mitigated by| E1
  style THREATS fill:#fef2f2,stroke:#dc2626,stroke-width:2px
  style DEFENSE fill:#f0fdf4,stroke:#059669,stroke-width:2px
  style CRYPTO fill:#dbeafe,stroke:#2563eb,stroke-width:2px
</div>
<div class="caption">Figure 10.1 — Security threat model with defense and encryption layers</div>
</div>

<!-- 11. ER DIAGRAM -->
<div class="page">
<h1>11. Entity-Relationship Diagram</h1>
<div class="mermaid">
erDiagram
  CUSTOMER {
    Long id PK
    String name
    String phone
    String address
    String photoPath
    Long createdAt
  }
  TRANSACTION {
    Long id PK
    Long customerId FK
    Double amount
    TxnType type
    String note
    Long date
    Boolean confirmed
    String qrHash
  }
  SETTINGS {
    Int id PK
    Boolean notifications
    Boolean pinEnabled
    Boolean biometricEnabled
    Long lastBackup
  }
  BUSINESS_PROFILE {
    String businessName
    String ownerName
    String phone
    String gstNumber
  }
  SCAN_CONFIRMATION {
    Long scanId PK
    Long txnId FK
    TrustLevel trustLevel
    String payloadHash
    Long scannedAt
  }
  CUSTOMER ||--o{ TRANSACTION : "has many"
  TRANSACTION ||--o| SCAN_CONFIRMATION : "may have"
</div>
<div class="caption">Figure 11.1 — Database entity-relationship diagram with crow's foot notation</div>
<h3>Table Summary</h3>
<table><tr><th>Table</th><th>Key Fields</th><th>Relationships</th></tr>
<tr><td>Customer</td><td>id (PK), name, phone, photoPath</td><td>1 → * Transaction</td></tr>
<tr><td>Transaction</td><td>id (PK), customerId (FK), amount, type</td><td>1 → 0..1 ScanConfirmation</td></tr>
<tr><td>Settings</td><td>id (PK), pinEnabled, lastBackup</td><td>Standalone</td></tr>
<tr><td>ScanConfirmation</td><td>scanId (PK), txnId (FK), trustLevel</td><td>Belongs to Transaction</td></tr>
</table>
</div>

<!-- 12. PROJECT STRUCTURE + TECH STACK -->
<div class="page">
<h1>12. Project Structure</h1>
<div class="code">app/src/main/java/com/nammasanthe/ledger/
├── MainActivity.kt                 # Entry point, Navigation host
├── data/
│   ├── database/AppDatabase.kt     # Room database
│   ├── entity/                     # Customer, TxnEntity, Confirmation
│   └── dao/                        # CustomerDao, TransactionDao
├── repository/
│   ├── LedgerRepository.kt         # Main CRUD operations
│   └── DataExportManager.kt        # PDF export + sharing
├── viewmodel/
│   ├── LedgerViewModel.kt          # Ledger business logic
│   ├── OcrViewModel.kt             # OCR pipeline control
│   └── ConfirmationViewModel.kt    # QR generation/scanning
├── ui/screens/                     # All Compose screens
├── sync/
│   ├── FirebaseAuthManager.kt      # Auth (signup/signin)
│   └── FirebaseSyncManager.kt      # Firestore cloud sync
├── security/                       # QR Gen, Validate, Scan
├── ocr/                            # Pipeline, Tesseract, ML Kit
│   ├── OcrPipeline.kt              # Dual-engine orchestrator
│   ├── TesseractOcr.kt             # Kannada OCR
│   ├── BillParser.kt               # Bill item extraction
│   └── GroceryDictionary.kt        # 150+ multilingual items
├── gemini/GeminiService.kt         # Gemini API OCR
└── util/                           # Photo, PDF utilities</div>
<h1 style="margin-top:24px">13. Technology Stack</h1>
<table><tr><th>Layer</th><th>Technology</th><th>Purpose</th></tr>
<tr><td>UI</td><td>Jetpack Compose</td><td>Declarative Android UI</td></tr>
<tr><td>State</td><td>Kotlin StateFlow</td><td>Reactive state management</td></tr>
<tr><td>Architecture</td><td>MVVM + Repository</td><td>Clean separation of concerns</td></tr>
<tr><td>Database</td><td>Room (SQLite)</td><td>Offline-first persistence</td></tr>
<tr><td>OCR</td><td>Tesseract + ML Kit</td><td>Dual-engine text recognition</td></tr>
<tr><td>AI/ML</td><td>Gemini 2.5 Flash</td><td>Advanced OCR + translation</td></tr>
<tr><td>QR</td><td>ZXing</td><td>QR generation/scanning</td></tr>
<tr><td>PDF</td><td>iText7</td><td>Report generation</td></tr>
<tr><td>Auth</td><td>Firebase Auth</td><td>Email/password auth</td></tr>
<tr><td>Cloud</td><td>Firestore</td><td>Cloud database sync</td></tr>
<tr><td>Camera</td><td>CameraX</td><td>Photo + QR scanning</td></tr>
</table>
<footer style="margin-top:30px"><br>Documentation for academic and project evaluation — © 2026 Namma-Santhe Ledger</footer>
</div>

<script>mermaid.initialize({startOnLoad:true,theme:'base',themeVariables:{fontSize:'13px',fontFamily:'Inter',primaryColor:'#dbeafe',primaryBorderColor:'#2563eb',primaryTextColor:'#0f172a',lineColor:'#64748b',secondaryColor:'#d1fae5',tertiaryColor:'#fef3c7'},flowchart:{curve:'basis',padding:12},sequence:{actorMargin:60,messageMargin:30}});</script>
</body></html>`;

(async () => {
  console.log('Launching browser...');
  const browser = await puppeteer.launch({
    headless: true,
    args: ['--no-sandbox','--disable-setuid-sandbox','--disable-gpu','--disable-dev-shm-usage']
  });
  const page = await browser.newPage();
  console.log('Rendering Mermaid diagrams...');
  await page.setContent(html, { waitUntil: 'networkidle0', timeout: 45000 });
  // Wait for Mermaid to finish rendering
  await page.waitForFunction(() => document.querySelectorAll('.mermaid svg').length >= 9, { timeout: 30000 });
  await new Promise(r => setTimeout(r, 2000)); // Extra buffer
  const outPath = path.join(__dirname, '..', 'Namma_Santhe_Technical_Report_Vector.pdf');
  console.log('Generating vector PDF...');
  await page.pdf({
    path: outPath,
    format: 'A4',
    printBackground: true,
    margin: { top: '8mm', bottom: '10mm', left: '10mm', right: '10mm' },
    displayHeaderFooter: true,
    headerTemplate: '<div></div>',
    footerTemplate: '<div style="font-size:7px;width:100%;text-align:center;color:#94a3b8;padding:3px">Page <span class="pageNumber"></span> of <span class="totalPages"></span></div>'
  });
  await browser.close();
  console.log('Vector PDF generated: ' + outPath);
})();
