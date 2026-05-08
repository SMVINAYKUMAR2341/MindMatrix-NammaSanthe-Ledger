# Namma-Santhe Ledger - Technical Documentation

> **Platform**: Android (Kotlin, Jetpack Compose)  
> **Architecture**: MVVM + Clean Architecture  
> **Type**: Offline-first digital ledger app for rural vendors

---

## PART 1: SYSTEM OVERVIEW

### Brief Description
Namma-Santhe Ledger is an Android application designed for small-scale vendors in rural Indian markets ("santhe") to digitize their credit tracking operations. The app replaces traditional paper-based "udari" (credit) tracking with a mobile-first, offline-capable solution.

### Problem Statement
- **Pain Point**: Rural vendors maintain handwritten ledgers for credit transactions
- **Challenges**: 
  - Lost/damaged paper records
  - Difficulty tracking outstanding payments
  - No backup mechanism
  - Calculation errors
  - Delayed payments due to poor record visibility

### Solution
An offline-first Android app with:
- Digital ledger with automatic balance calculation
- Customer photo capture for identification
- QR-based transaction confirmation
- OCR bill scanning for quick entry
- Cloud backup via Firebase (optional)
- PDF export for sharing

### Key Features

| Feature | Description | Tech Stack |
|---------|-------------|------------|
| Quick Entry | Numeric keypad for <5 sec transaction entry | Jetpack Compose |
| Customer Mgmt | Photo capture, search, profile | CameraX, Coil |
| Credit Tracking | Auto-calculated outstanding balances | Room DB |
| QR Confirmation | Tamper-proof transaction verification | ZXing, Custom Hash |
| OCR Bill Scan | Extract text from handwritten bills | Gemini ML Kit |
| Cloud Sync | Firebase Auth + Firestore backup | Firebase |
| PDF Export | Complete ledger as shareable PDF | iText7 |
| Offline Mode | Full functionality without internet | Room DB |

---

## PART 2: SYSTEM ARCHITECTURE DIAGRAM

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              PRESENTATION LAYER                              │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │   HomeScreen    │  │ CustomerScreen  │  │  LedgerScreen   │              │
│  │   (Dashboard)   │  │  (Add/Edit)     │  │  (Transactions) │              │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘              │
│           │                    │                    │                       │
│  ┌────────▼────────┐  ┌────────▼────────┐  ┌────────▼────────┐              │
│  │  QuickEntry     │  │  ProfileScreen  │  │ QrDisplayScreen │              │
│  │   (Numeric)     │  │ (Export/Backup) │  │  (Generate QR)  │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
│                                                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │ QrScannerScreen │  │  OcrScreen      │  │ InvoiceScreen   │              │
│  │ (Scan & Verify) │  │ (Bill Capture)  │  │ (PDF Preview)   │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
│                                                                              │
└──────────────────────────────────┬────────────────────────────────────────────┘
                                   │
                          Jetpack Compose UI
                                   │
┌──────────────────────────────────▼────────────────────────────────────────────┐
│                           VIEWMODEL LAYER (MVVM)                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │   LedgerVM      │  │   ProfileVM     │  │   OcrViewModel  │              │
│  │                 │  │                 │  │                 │              │
│  │ • StateFlow     │  │ • Auth state    │  │ • Gemini OCR    │              │
│  │ • BusinessLogic │  │ • Export PDF    │  │ • Text Parse    │              │
│  └────────┬────────┘  └────────┬────────┘  └────────┬────────┘              │
│           │                    │                    │                       │
│  ┌────────▼────────┐  ┌────────▼────────┐  ┌────────▼────────┐              │
│  │  ConfirmationVM │  │  GeminiSetupVM  │  │  MainViewModel  │              │
│  │                 │  │                 │  │                 │              │
│  │ • QR Generation │  │ • API Config    │  │ • Navigation    │              │
│  │ • Trust Level   │  │ • Prompt Editor │  │ • App State     │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
│                                                                              │
└──────────────────────────────────┬────────────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼────────────────────────────────────────────┐
│                           REPOSITORY LAYER                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │ LedgerRepository│  │ SyncRepository  │  │ ExportRepository│              │
│  │                 │  │                 │  │                 │              │
│  │ • CRUD ops      │  │ • Firebase Auth │  │ • PDF Gen       │              │
│  │ • Local cache   │  │ • Firestore     │  │ • File share    │              │
│  └────────┬────────┘  └─────────────────┘  └─────────────────┘              │
│           │                                                                  │
│  ┌────────▼────────┐  ┌─────────────────┐                                  │
│  │   Room DAOs     │  │  PhotoManager     │                                  │
│  │                 │  │                 │                                  │
│  │ • CustomerDao   │  │ • Image compress  │                                │
│  │ • TransactionDao│  │ • File storage    │                                │
│  │ • SettingsDao   │  └─────────────────┘                                  │
│  └─────────────────┘                                                          │
│                                                                              │
└──────────────────────────────────┬────────────────────────────────────────────┘
                                   │
┌──────────────────────────────────▼────────────────────────────────────────────┐
│                           DATA LAYER                                         │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐              │
│  │  Room Database  │  │  Firebase       │  │  Local Files    │              │
│  │                 │  │                 │  │                 │              │
│  │ • SQLite        │  │ • Auth          │  │ • Photos        │              │
│  │ • Offline cache │  │ • Firestore     │  │ • PDFs          │              │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘              │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────┐            │
│  │                    EXTERNAL SERVICES                         │            │
│  ├─────────────────────────────────────────────────────────────┤            │
│  │                                                             │            │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │            │
│  │  │  Gemini ML  │  │   ZXing     │  │   iText7    │         │            │
│  │  │   (OCR)     │  │  (QR Code)  │  │  (PDF)      │         │            │
│  │  └─────────────┘  └─────────────┘  └─────────────┘         │            │
│  │                                                             │            │
│  └─────────────────────────────────────────────────────────────┘            │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## PART 3: UML CLASS DIAGRAM

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                            ENTITY CLASSES                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────┐                                                  │
│  │       Customer          │                                                  │
│  ├─────────────────────────┤                                                  │
│  │ - id: Long              │                                                  │
│  │ - name: String          │                                                  │
│  │ - phone: String         │                                                  │
│  │ - address: String       │                                                  │
│  │ - photoPath: String?    │                                                  │
│  │ - createdAt: Long       │                                                  │
│  │ - lastUpdated: Long     │                                                  │
│  │                         │                                                  │
│  │ + getInitials(): String │                                                │
│  │ + hasPhoto(): Boolean   │                                                  │
│  └───────────┬─────────────┘                                                  │
│              │ 1                                                              │
│              │                                                                │
│              │ *                                                              │
│              ▼                                                                │
│  ┌─────────────────────────┐                                                  │
│  │     Transaction         │                                                  │
│  ├─────────────────────────┤                                                  │
│  │ - id: Long              │                                                  │
│  │ - customerId: Long      │                                                  │
│  │ - amount: Double        │                                                  │
│  │ - type: TxnType         │  [CREDIT, PAYMENT]                               │
│  │ - note: String          │                                                  │
│  │ - date: Long            │                                                  │
│  │ - photoPath: String?    │                                                  │
│  │ - confirmed: Boolean    │                                                  │
│  │ - qrHash: String?       │                                                  │
│  │ - createdAt: Long         │                                                  │
│  │                         │                                                  │
│  │ + isCredit(): Boolean   │                                                  │
│  │ + isPayment(): Boolean  │                                                  │
│  └─────────────────────────┘                                                  │
│                                                                              │
│  ┌─────────────────────────┐     ┌─────────────────────────┐                │
│  │       Settings          │     │      BusinessProfile    │                │
│  ├─────────────────────────┤     ├─────────────────────────┤                │
│  │ - id: Int               │     │ - businessName: String  │                │
│  │ - notifications: Bool │     │ - ownerName: String     │                │
│  │ - dailySummaryTime: Str│    │ - phone: String         │                │
│  │ - overdueThreshold: Int│    │ - address: String       │                │
│  │ - overdueDays: Int      │     │ - gstNumber: String?    │                │
│  │ - pinEnabled: Bool      │     │ - terms: String         │                │
│  │ - pinHash: String?      │     │                         │                │
│  │ - biometric: Bool       │     │ + getHeader(): String   │                │
│  │ - lastBackup: Long      │     │ + getFooter(): String   │                │
│  └─────────────────────────┘     └─────────────────────────┘                │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                           QR SECURITY CLASSES                                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────┐                                                  │
│  │      QrPayload          │                                                  │
│  ├─────────────────────────┤                                                  │
│  │ - txnId: Long           │                                                  │
│  │ - amount: Double        │                                                  │
│  │ - type: String          │                                                  │
│  │ - customerId: Long      │                                                  │
│  │ - nonce: String         │                                                  │
│  │ - timestamp: Long         │                                                  │
│  │ - expiresAt: Long         │                                                  │
│  │ - hash: String          │                                                  │
│  │                         │                                                  │
│  │ + toJson(): String      │                                                  │
│  │ + isExpired(): Boolean  │                                                  │
│  │ + verifyHash(): Boolean │                                                  │
│  └─────────────────────────┘                                                  │
│                                                                              │
│  ┌─────────────────────────┐     ┌─────────────────────────┐                │
│  │    TrustAssessment      │     │   ScanConfirmation      │                │
│  ├─────────────────────────┤     ├─────────────────────────┤                │
│  │ - trustLevel: Enum      │     │ - scanId: Long          │                │
│  │ - confidenceScore: Int  │     │ - txnId: Long           │                │
│  │ - deviceMatch: Boolean  │     │ - scannedAt: Long       │                │
│  │ - timingCheck: Boolean  │     │ - scannerDeviceId: Str  │                │
│  │ - nonceValid: Boolean   │     │ - trustLevel: Enum      │                │
│  │                         │     │ - payloadHash: String   │                │
│  │ + isTrusted(): Boolean  │     │                         │                │
│  │ + getBadge(): String    │     │ + verifyIntegrity()     │                │
│  └─────────────────────────┘     └─────────────────────────┘                │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                          REPOSITORY CLASSES                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────┐                                                  │
│  │    LedgerRepository     │                                                  │
│  ├─────────────────────────┤                                                  │
│  │ - customerDao           │                                                  │
│  │ - transactionDao        │                                                  │
│  │ - settingsDao           │                                                  │
│  │                         │                                                  │
│  │ + insertCustomer()      │                                                  │
│  │ + updateCustomer()      │                                                  │
│  │ + getCustomerById()     │                                                  │
│  │ + searchCustomers()     │                                                  │
│  │ + insertTransaction()   │                                                  │
│  │ + getTransactionsFor()  │                                                  │
│  │ + getBalanceFor()       │                                                  │
│  │ + getAllTransactions()  │                                                  │
│  └─────────────────────────┘                                                  │
│                                                                              │
│  ┌─────────────────────────┐     ┌─────────────────────────┐                │
│  │   FirebaseAuthManager   │     │   FirebaseSyncManager   │                │
│  ├─────────────────────────┤     ├─────────────────────────┤                │
│  │ - firebaseAuth          │     │ - firestore             │                │
│  │                         │     │                         │                │
│  │ + signIn(email, pass)   │     │ + syncToCloud()         │                │
│  │ + signUp(email, pass)   │     │ + restoreFromCloud()    │                │
│  │ + signOut()             │     │ + syncProfile()         │                │
│  │ + getCurrentUser()      │     │ + syncCustomers()       │                │
│  │ + isAuthenticated()     │     │ + syncTransactions()    │                │
│  └─────────────────────────┘     └─────────────────────────┘                │
│                                                                              │
│  ┌─────────────────────────┐                                                  │
│  │    DataExportManager    │                                                  │
│  ├─────────────────────────┤                                                  │
│  │                         │                                                  │
│  │ + exportLedgerToPdf()   │                                                  │
│  │ + generateInvoice()     │                                                  │
│  │ + sharePdf()            │                                                  │
│  │ + getExportPath()       │                                                  │
│  └─────────────────────────┘                                                  │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                           VIEWMODEL CLASSES                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────┐     ┌─────────────────────────┐                │
│  │      LedgerViewModel    │     │    ConfirmationViewModel│                │
│  ├─────────────────────────┤     ├─────────────────────────┤                │
│  │ - ledgerRepository      │     │ - confirmRepository     │                │
│  │                         │     │ - ledgerRepository      │                │
│  │ ┌ StateFlows ─────────┐ │     │ - vendorDeviceId        │                │
│  │ │ customers: List     │ │     │                         │                │
│  │ │ transactions: List  │ │     │ ┌ StateFlows ─────────┐ │                │
│  │ │ balance: Double     │ │     │ │ qrState: QrDisplay  │ │                │
│  │ └─────────────────────┘ │     │ │ scanState: ScanState│ │                │
│  │                         │     │ └─────────────────────┘ │                │
│  │ + loadCustomers()       │     │                         │                │
│  │ + addTransaction()      │     │ + requestQrForTxn()     │                │
│  │ + searchCustomers()     │     │ + onQrScanned()         │                │
│  │ + getBalance()          │     │ + validateScan()        │                │
│  │ + updateTxnPhoto()      │     │ + confirmTransaction()  │                │
│  └─────────────────────────┘     └─────────────────────────┘                │
│                                                                              │
│  ┌─────────────────────────┐     ┌─────────────────────────┐                │
│  │     ProfileViewModel    │     │      OcrViewModel       │                │
│  ├─────────────────────────┤     ├─────────────────────────┤                │
│  │ - ledgerRepository      │     │ - geminiService         │                │
│  │ - authManager           │     │ - photoManager          │                │
│  │ - syncManager           │     │                         │                │
│  │                         │     │ ┌ StateFlows ─────────┐ │                │
│  │ ┌ StateFlows ─────────┐ │     │ │ scanResult: OcrResult│ │                │
│  │ │ authState: AuthState│ │     │ │ isProcessing: Bool  │ │                │
│  │ │ syncStatus: SyncStatus│    │ │ extractedItems: List│ │                │
│  │ └─────────────────────┘ │     │ └─────────────────────┘ │                │
│  │                         │     │                         │                │
│  │ + exportToPdf()         │     │ + captureBill()         │                │
│  │ + backupToCloud()       │     │ + performOcr()          │                │
│  │ + restoreFromCloud()    │     │ + parseToInvoice()      │                │
│  │ + signIn()              │     │ + saveToTransaction()   │                │
│  └─────────────────────────┘     └─────────────────────────┘                │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                           SERVICE CLASSES                                     │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────────────────┐     ┌─────────────────────────┐                │
│  │      GeminiService      │     │       QrGenerator       │                │
│  ├─────────────────────────┤     ├─────────────────────────┤                │
│  │ - apiKey: String        │     │ - QR_TTL_MS: Long       │                │
│  │ - baseUrl: String       │     │                         │                │
│  │                         │     │ + buildPayload(txn)     │                │
│  │ + performOcr()          │     │ + generateBitmap()      │                │
│  │ + extractItems()        │     │ + generateHash()        │                │
│  │ + parseKannadaText()    │     │ + generateUserFriendly()  │                │
│  └─────────────────────────┘     └─────────────────────────┘                │
│                                                                              │
│  ┌─────────────────────────┐     ┌─────────────────────────┐                │
│  │       QrValidator       │     │     QrScannerAnalyzer   │                │
│  ├─────────────────────────┤     ├─────────────────────────┤                │
│  │                         │     │ - scanner: BarcodeScan  │                │
│  │ + parse(rawJson)        │     │                         │                │
│  │ + validate(payload)     │     │ + analyze(image)          │                │
│  │ + checkExpiry()         │     │ + decodeQr()              │                │
│  │ + verifyHash()          │     │ + onQrDetected()          │                │
│  │ + assessTrust()         │     │                         │                │
│  └─────────────────────────┘     └─────────────────────────┘                │
│                                                                              │
│  ┌─────────────────────────┐                                                  │
│  │      PhotoManager       │                                                  │
│  ├─────────────────────────┤                                                  │
│  │ - context: Context      │                                                  │
│  │ - photoDir: File        │                                                  │
│  │                         │                                                  │
│  │ + capturePhoto()        │                                                  │
│  │ + compressImage()       │                                                  │
│  │ + saveToPrivate()       │                                                  │
│  │ + deletePhoto()         │                                                  │
│  │ + getPhotoUri()         │                                                  │
│  └─────────────────────────┘                                                  │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘

RELATIONSHIPS SUMMARY:
━━━━━━━━━━━━━━━━━━━━━━
Customer 1 ────────<*> Transaction (One-to-Many)
ViewModel ────────> Repository (Dependency)
Repository ────────> DAO (Dependency)
Repository ────────> Firebase (Dependency)
QrGenerator ────> QrPayload (Creates)
QrValidator ────> QrPayload (Validates)
ConfirmationVM ──> QrGenerator (Uses)
OcrViewModel ────> GeminiService (Uses)
```

---

## PART 4: USE CASE DIAGRAM

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                            USE CASE DIAGRAM                                   │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│                           ┌─────────────┐                                    │
│                           │   VENDOR    │                                    │
│                           │  (Primary)  │                                    │
│                           └──────┬──────┘                                    │
│                                  │                                           │
│         ┌────────────────────────┼────────────────────────┐                   │
│         │                        │                        │                   │
│         ▼                        ▼                        ▼                   │
│  ┌─────────────┐          ┌─────────────┐          ┌─────────────┐           │
│  │  Manage     │          │  Process    │          │   Backup    │           │
│  │  Customers  │          │ Transactions│          │   & Export  │           │
│  └──────┬──────┘          └──────┬──────┘          └──────┬──────┘           │
│         │                        │                        │                   │
│    ┌────┴────┐              ┌────┴────┐              ┌────┴────┐             │
│    │         │              │         │              │         │             │
│    ▼         ▼              ▼         ▼              ▼         ▼             │
│ ┌──────┐  ┌──────┐      ┌──────┐  ┌──────┐      ┌──────┐  ┌──────┐         │
│ │Add   │  │Edit  │      │Quick │  │View  │      │Export│  │Cloud │         │
│ │Cust  │  │Cust  │      │Entry │  │Ledger│      │PDF   │  │Sync  │         │
│ └──────┘  └──────┘      └──────┘  └──────┘      └──────┘  └──────┘         │
│    │         │              │         │              │         │             │
│    ▼         ▼              ▼         ▼              ▼         ▼             │
│ ┌──────┐  ┌──────┐      ┌──────┐  ┌──────┐      ┌──────┐  ┌──────┐         │
│ │Photo │  │Search│      │OCR   │  │QR    │      │Share │  │Restore│        │
│ │Capture│  │      │      │Scan  │  │Confirm│      │WhatsApp│       │        │
│ └──────┘  └──────┘      └──────┘  └──────┘      └──────┘  └──────┘         │
│                                                                              │
│                                                                              │
│                           ┌─────────────┐                                    │
│                           │   CUSTOMER  │                                    │
│                           │ (Secondary) │                                    │
│                           └──────┬──────┘                                    │
│                                  │                                           │
│                                  ▼                                           │
│                           ┌─────────────┐                                    │
│                           │  Scan QR    │                                    │
│                           │  to Confirm │                                    │
│                           │ Transaction │                                    │
│                           └─────────────┘                                    │
│                                                                              │
│ ┌─────────────────────────────────────────────────────────────────────────┐  │
│ │                          SYSTEM USE CASES                               │  │
│ ├─────────────────────────────────────────────────────────────────────────┤  │
│ │                                                                         │  │
│ │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │  │
│ │  │  Generate   │  │   Send      │  │  Validate   │  │   Detect    │   │  │
│ │  │   QR Code   │  │ Notification│  │  QR Scan    │  │  Tampering  │   │  │
│ │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │  │
│ │                                                                         │  │
│ │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │  │
│ │  │  Calculate  │  │   OCR       │  │   Sync      │  │    Hash     │   │  │
│ │  │   Balance   │  │ Processing  │  │  to Cloud   │  │   Chain     │   │  │
│ │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │  │
│ │                                                                         │  │
│ └─────────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│ RELATIONSHIPS:                                                               │
│ ─────────────────                                                            │
│ • Vendor ──<<include>>──> Calculate Balance (auto on transaction)            │
│ • Vendor ──<<include>>──> Generate QR (on transaction save)                  │
│ • Vendor ──<<extend>>──> OCR Scan (optional entry method)                  │
│ • Vendor ──<<extend>>──> Send Reminder (optional notification)              │
│ • Customer ──<<include>>──> Validate QR (required for confirmation)           │
│ • Cloud Sync ──<<include>>──> Hash Chain Validation                         │
│ • Export PDF ──<<include>>──> Calculate Balance                             │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## PART 5: SEQUENCE DIAGRAMS

### 5.1 Add Transaction Flow

```
┌─────────┐     ┌──────────┐     ┌─────────────┐     ┌──────────┐     ┌─────────┐
│  User   │     │  Entry   │     │  LedgerVM   │     │ Repository│     │ Room DB │
│ (Vendor)│     │  Screen  │     │             │     │           │     │         │
└────┬────┘     └────┬─────┘     └──────┬──────┘     └─────┬─────┘     └────┬────┘
     │               │                  │                  │                │
     │ 1. Enter amt  │                  │                  │                │
     │────────────────>                  │                  │                │
     │               │                  │                  │                │
     │ 2. Tap Save   │                  │                  │                │
     │────────────────>                  │                  │                │
     │               │ 3. onSave()      │                  │                │
     │               │─────────────────>│                  │                │
     │               │                  │ 4. insertTxn()   │                │
     │               │                  │─────────────────>│                │
     │               │                  │                  │5. insert()     │
     │               │                  │                  │───────────────>│
     │               │                  │                  │                │
     │               │                  │                  │6. Success     │
     │               │                  │                  │<───────────────│
     │               │                  │7. Result         │                │
     │               │                  │<─────────────────│                │
     │               │                  │                  │                │
     │               │                  │ 8. updateBalance()│                │
     │               │                  │─────────────────>│                │
     │               │                  │                  │9. Query sum   │
     │               │                  │                  │───────────────>│
     │               │                  │                  │10. Return     │
     │               │                  │                  │<───────────────│
     │               │ 11. UI Update    │                  │                │
     │               │<─────────────────│                  │                │
     │ 12. Show Conf │                  │                  │                │
     │<───────────────│                  │                  │                │
     │               │                  │                  │                │
     │               │                  │ 13. generateQr() │                │
     │               │                  │──────────────────────> (QR Gen)   │
     │               │                  │                  │                │
     │ 14. Display QR│                  │                  │                │
     │<───────────────────────────────────│                  │                │
     │               │                  │                  │                │
```

### 5.2 OCR Invoice Flow

```
┌─────────┐     ┌──────────┐     ┌─────────────┐     ┌─────────────┐     ┌──────────┐
│  User   │     │  Camera  │     │  OcrScreen  │     │ GeminiService│     │  Parser  │
│ (Vendor)│     │  (X)     │     │             │     │   (ML Kit)   │     │ (Logic)  │
└────┬────┘     └────┬─────┘     └──────┬──────┘     └──────┬──────┘     └────┬─────┘
     │               │                  │                   │                │
     │ 1. Tap OCR    │                  │                   │                │
     │────────────────>                  │                   │                │
     │               │                  │                   │                │
     │               │ 2. Open Camera   │                   │                │
     │               │<─────────────────│                   │                │
     │               │                  │                   │                │
     │ 3. Capture Bill│                 │                   │                │
     │───────────────>│                  │                   │                │
     │               │                  │                   │                │
     │ 4. Photo Taken│                  │                   │                │
     │<───────────────│                  │                   │                │
     │               │                  │                   │                │
     │               │ 5. onPhotoTaken()│                   │                │
     │               │─────────────────>│                   │                │
     │               │                  │                   │                │
     │               │                  │ 6. performOcr()   │                │
     │               │                  │──────────────────>│                │
     │               │                  │                   │                │
     │               │                  │                   │ 7. API Call    │
     │               │                  │                   │───────┐        │
     │               │                  │                   │       │        │
     │               │                  │                   │<──────┘        │
     │               │                  │                   │ 8. Raw Text    │
     │               │                  │                   │                │
     │               │                  │ 9. extractItems() │                │
     │               │                  │───────────────────────────────────>│
     │               │                  │                   │                │
     │               │                  │                   │                │10. Parse
     │               │                  │                   │                │─────┐
     │               │                  │                   │                │     │
     │               │                  │                   │                │<────┘
     │               │                  │                   │11. JSON Items  │
     │               │                  │<─────────────────────────────────────│
     │               │                  │                   │                │
     │               │                  │ 12. Update UI     │                │
     │               │                  │                   │                │
     │               │ 13. Show Items   │                   │                │
     │               │<─────────────────│                   │                │
     │ 14. Review/Edit│                 │                   │                │
     │<───────────────│                  │                   │                │
     │               │                  │                   │                │
     │ 15. Save to Txn                  │                   │                │
     │────────────────────────────────────────────────────────────────────────>│
     │               │                  │                   │                │
```

### 5.3 QR Confirmation Flow

```
┌─────────┐     ┌──────────┐     ┌─────────────┐     ┌─────────────┐     ┌──────────┐
│ Vendor  │     │ QrDisplay │     │ Confirmation│     │   Customer  │     │ QrScanner│
│  App    │     │  Screen   │     │     VM      │     │    Phone    │     │  (Scan)  │
└────┬────┘     └────┬─────┘     └──────┬──────┘     └──────┬──────┘     └────┬─────┘
     │               │                  │                   │                │
     │ 1. Save Txn   │                  │                   │                │
     │────────────────>                  │                   │                │
     │               │                  │                   │                │
     │               │ 2. requestQr()   │                   │                │
     │               │─────────────────>│                   │                │
     │               │                  │                   │                │
     │               │                  │ 3. buildPayload() │                │
     │               │                  │───────┐           │                │
     │               │                  │       │           │                │
     │               │                  │<──────┘           │                │
     │               │                  │                   │                │
     │               │                  │ 4. generateHash() │                │
     │               │                  │───────┐           │                │
     │               │                  │       │           │                │
     │               │                  │<──────┘           │                │
     │               │                  │                   │                │
     │               │                  │ 5. createBitmap() │                │
     │               │                  │───────┐           │                │
     │               │                  │       │           │                │
     │               │                  │<──────┘           │                │
     │               │ 6. Display QR    │                   │                │
     │               │<─────────────────│                   │                │
     │               │                  │                   │                │
     │ 7. Show QR    │                  │                   │                │
     │<───────────────│                  │                   │                │
     │               │                  │                   │                │
     │═══════════════│══════════════════│═══════════════════│ 8. Scan QR     │
     │               │                  │                   │───────────────>│
     │               │                  │                   │                │
     │               │                  │                   │                │ 9. Decode
     │               │                  │                   │                │─────┐
     │               │                  │                   │                │     │
     │               │                  │                   │                │<────┘
     │               │                  │                   │10. Payload     │
     │               │                  │                   │<───────────────│
     │               │                  │                   │                │
     │═══════════════│══════════════════│═══════════════════│11. Send JSON   │
     │               │                  │                   │────────────────>│
     │               │                  │                   │                │
     │               │                  │ 12. onQrScanned() │                │
     │               │                  │<─────────────────────────────────│
     │               │                  │                   │                │
     │               │                  │ 13. parse()       │                │
     │               │                  │───────┐           │                │
     │               │                  │       │           │                │
     │               │                  │<──────┘           │                │
     │               │                  │                   │                │
     │               │                  │ 14. validate()    │                │
     │               │                  │───────┐           │                │
     │               │                  │       │           │                │
     │               │                  │<──────┘           │                │
     │               │                  │                   │                │
     │               │                  │ 15. assessTrust() │                │
     │               │                  │───────┐           │                │
     │               │                  │       │           │                │
     │               │                  │<──────┘           │                │
     │               │                  │                   │                │
     │               │                  │ 16. confirmTxn()  │                │
     │               │                  │─────────────────────────────────>│
     │               │                  │                   │                │
     │               │ 17. Show Status  │                   │                │
     │               │<─────────────────│                   │                │
     │ 18. Trust Badge│                 │                   │                │
     │<───────────────│                  │                   │                │
     │               │                  │                   │                │
```

---

## PART 6: DATA FLOW DIAGRAM (DFD)

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                        DATA FLOW DIAGRAM (Level 0)                            │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│     ┌──────────┐         ┌──────────────┐         ┌──────────┐              │
│     │  VENDOR  │         │              │         │ CLOUD    │              │
│     │  (User)  │────────>│   NAMMA-    │────────>│ FIREBASE │              │
│     │          │         │   SANTHE    │         │          │              │
│     └──────────┘         │   LEDGER    │         └──────────┘              │
│           ▲              │   (System)   │              ▲                    │
│           │              │              │              │                    │
│           │              └──────┬───────┘              │                    │
│           │                     │                      │                    │
│           │              ┌──────┴───────┐              │                    │
│           └──────────────│   LOCAL DB   │──────────────┘                    │
│                          │   (SQLite)   │                                   │
│                          └──────────────┘                                   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────────────────┐
│                        DATA FLOW DIAGRAM (Level 1)                            │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  INPUT PROCESSES:                                                            │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                                                                      │   │
│  │  ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐   ┌─────────┐ │   │
│  │  │Numeric  │   │Camera   │   │OCR      │   │QR Scan  │   │Voice    │ │   │
│  │  │Keypad   │   │Capture  │   │Image    │   │Reader   │   │Input    │ │   │
│  │  └────┬────┘   └────┬────┘   └────┬────┘   └────┬────┘   └────┬────┘ │   │
│  │       │             │             │             │             │      │   │
│  │       └─────────────┴─────────────┴─────────────┴─────────────┘      │   │
│  │                         │                                            │   │
│  │                         ▼                                            │   │
│  │                ┌────────────────┐                                   │   │
│  │                │  INPUT PARSER    │                                   │   │
│  │                │  (Validation)    │                                   │   │
│  │                └────────┬───────┘                                   │   │
│  │                         │                                            │   │
│  └─────────────────────────┼────────────────────────────────────────────┘   │
│                            │                                                 │
│                            ▼                                                 │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                     PROCESSING LAYER                                  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │   │
│  │  │ Transaction │  │   OCR       │  │  Balance    │  │  QR Gen/    │   │   │
│  │  │  Processor  │  │  Pipeline   │  │ Calculator  │  │  Validator  │   │   │
│  │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘   │   │
│  │         │                │                │                │         │   │
│  │         └────────────────┴────────────────┴────────────────┘         │   │
│  │                           │                                            │   │
│  └───────────────────────────┼────────────────────────────────────────────┘   │
│                              │                                                 │
│                              ▼                                                 │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        STORAGE LAYER                                  │   │
│  │                                                                       │   │
│  │    ┌──────────────┐      ┌──────────────┐      ┌──────────────┐       │   │
│  │    │   Customer   │      │ Transaction  │      │   Settings   │       │   │
│  │    │    Table     │      │    Table     │      │    Table     │       │   │
│  │    └──────────────┘      └──────────────┘      └──────────────┘       │   │
│  │                                                                       │   │
│  │    ┌──────────────┐      ┌──────────────┐      ┌──────────────┐       │   │
│  │    │    Photo     │      │   QR Hash    │      │   Audit Log  │       │   │
│  │    │   Storage    │      │    Store     │      │   (Append)   │       │   │
│  │    └──────────────┘      └──────────────┘      └──────────────┘       │   │
│  │                                                                       │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                              │                                                 │
│                              ▼                                                 │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                        OUTPUT LAYER                                     │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐   │   │
│  │  │  Compose UI │  │  PDF Gen    │  │ Notification│  │  Firebase   │   │   │
│  │  │  (Screens)  │  │  (iText7)   │  │  (Local)    │  │  (Sync)     │   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘   │   │
│  │                                                                       │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                   │   │
│  │  │  QR Display │  │  WhatsApp   │  │  File Share │                   │   │
│  │  │  (Bitmap)   │  │  Share      │  │  (Intent)   │                   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                   │   │
│  │                                                                       │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  DATA STORES:                                                                │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │  D1: Customer DB  │  D2: Transaction DB  │  D3: Photo Files             │   │
│  │  D4: Settings DB  │  D5: QR Hash Store   │  D6: Audit Log               │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## PART 7: ACTIVITY / FLOW DIAGRAM

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                       APP NAVIGATION FLOW DIAGRAM                             │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│    [START]                                                                   │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────┐                                                                 │
│  │  Splash │                                                                 │
│  │ Screen  │                                                                 │
│  └────┬────┘                                                                 │
│       │                                                                      │
│       ▼                                                                      │
│  ┌─────────────┐                                                             │
│  │  MainActivity│                                                             │
│  │  (Nav Setup) │                                                             │
│  └──────┬──────┘                                                             │
│         │                                                                    │
│         ▼                                                                    │
│  ┌─────────────────────────────────────────────────────────────────┐       │
│  │                         BOTTOM NAV BAR                           │       │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐          │       │
│  │  │  HOME    │  │CUSTOMERS │  │  ENTRY   │  │ PROFILE  │          │       │
│  │  │  (🏠)    │  │  (👥)    │  │  (+)     │  │  (👤)    │          │       │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘          │       │
│  │       │            │            │            │                │       │
│  └───────┼────────────┼────────────┼────────────┼────────────────┘       │
│          │            │            │            │                        │
│          ▼            │            │            │                        │
│     ┌─────────┐       │            │            │                        │
│     │HomeScreen│       │            │            │                        │
│     │(Dashboard)      │            │            │                        │
│     └────┬────┘       │            │            │                        │
│          │            │            │            │                        │
│          ├────────────┼────────────┤            │                        │
│          ▼            ▼            ▼            ▼                        │
│     ┌─────────┐ ┌────────────┐ ┌───────────┐ ┌─────────────┐             │
│     │Quick Stats│ │CustomerList│ │QuickEntry │ │ProfileScreen│             │
│     │         │ │            │ │  Screen   │ │             │             │
│     │ • Total │ │ • Search   │ │           │ │ • Export    │             │
│     │ • Today │ │ • Filter   │ │• NumPad  │ │ • Backup    │             │
│     │ • Overdue│ │ • Add New │ │• Camera  │ │ • Settings  │             │
│     │         │ │            │ │• OCR     │ │             │             │
│     └────┬────┘ └─────┬──────┘ └─────┬─────┘ └──────┬──────┘             │
│          │            │            │            │                         │
│          │            │            │            │                         │
│          │            ▼            │            │                         │
│          │      ┌──────────┐       │            │                         │
│          │      │ Customer │       │            │                         │
│          │      │ Detail   │       │            │                         │
│          │      │ Screen   │       │            │                         │
│          │      │          │       │            │                         │
│          │      │• Profile │       │            │                         │
│          │      │• Photo   │       │            │                         │
│          │      │• History │───────┘            │                         │
│          │      └────┬─────┘                    │                         │
│          │           │                          │                         │
│          │           ▼                          │                         │
│          │      ┌──────────┐                    │                         │
│          └─────>│ Customer │                   │                         │
│                 │ Ledger   │                   │                         │
│                 │ Screen   │                   │                         │
│                 │          │                   │                         │
│                 │• Txn List│                   │                         │
│                 │• Balance │                   │                         │
│                 │• Add Txn │───────────────────┘                         │
│                 └────┬─────┘                                             │
│                      │                                                    │
│                      ▼                                                    │
│                 ┌──────────┐                                             │
│                 │ Txn Entry│                                             │
│                 │ Form     │                                             │
│                 │          │                                             │
│                 │• Amount  │                                             │
│                 │• Type    │                                             │
│                 │• Note    │                                             │
│                 │• Photo    │                                             │
│                 └────┬─────┘                                             │
│                      │                                                    │
│                      ▼                                                    │
│                 ┌──────────┐                                             │
│                 │  SAVE    │                                             │
│                 │  TXN     │                                             │
│                 └────┬─────┘                                             │
│                      │                                                    │
│                      ▼                                                    │
│                 ┌──────────┐                                             │
│                 │QR Display│                                             │
│                 │ Screen   │                                             │
│                 │          │                                             │
│                 │• QR Code │                                             │
│                 │• Timer   │                                             │
│                 │• Regenerate                                             │
│                 └──────────┘                                             │
│                      │                                                    │
│                      │ (Customer scans)                                  │
│                      ▼                                                    │
│                 ┌──────────┐                                             │
│                 │QR Scanner│                                             │
│                 │ Screen   │                                             │
│                 │          │                                             │
│                 │• Camera  │                                             │
│                 │• Validate│                                             │
│                 │• Confirm │                                             │
│                 └────┬─────┘                                             │
│                      │                                                    │
│                      ▼                                                    │
│                 ┌──────────┐                                             │
│                 │ Confirmation│                                          │
│                 │ Success   │                                            │
│                 └────┬─────┘                                             │
│                      │                                                    │
│                      ▼                                                    │
│                 [BACK TO LEDGER]                                          │
│                                                                              │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                     ADDITIONAL FLOWS                                 │   │
│  ├──────────────────────────────────────────────────────────────────────┤   │
│  │                                                                     │   │
│  │  OCR FLOW:                                                          │   │
│  │  Entry → OCR Screen → Camera → Gemini OCR → Parse → Invoice → Save │   │
│  │                                                                     │   │
│  │  EXPORT FLOW:                                                       │   │
│  │  Profile → Export → Select Range → Generate PDF → Share             │   │
│  │                                                                     │   │
│  │  CLOUD SYNC FLOW:                                                   │   │
│  │  Profile → Sign In → Backup Now → Sync to Firestore → Success       │   │
│  │                                                                     │   │
│  └──────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## PART 8: OCR PIPELINE DIAGRAM

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                         OCR PIPELINE ARCHITECTURE                              │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   STAGE 1: INPUT                                                             │
│   ┌───────────────────────────────────────────────────────────────────┐     │
│   │  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐      │     │
│   │  │ Handwritten│  │  Printed  │  │  Mixed    │  │  Kannada  │      │     │
│   │  │   Bill    │  │   Bill    │  │ Lang Bill │  │   Text    │      │     │
│   │  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘      │     │
│   │        └───────────────┴───────────────┴───────────────┘          │     │
│   │                          │                                        │     │
│   │                          ▼                                        │     │
│   │                   ┌──────────────┐                                │     │
│   │                   │ CameraX      │                                │     │
│   │                   │ Capture      │                                │     │
│   │                   │ (ImageProxy) │                                │     │
│   │                   └──────┬───────┘                                │     │
│   └──────────────────────────┼─────────────────────────────────────────┘     │
│                              │                                               │
│   STAGE 2: PREPROCESSING     │                                               │
│   ┌──────────────────────────┼─────────────────────────────────────────┐     │
│   │                          ▼                                         │     │
│   │                   ┌──────────────┐                                 │     │
│   │                   │ Image        │                                 │     │
│   │                   │ Preprocessor │                                 │     │
│   │                   │              │                                 │     │
│   │                   │ • Resize     │                                 │     │
│   │                   │ • Normalize  │                                 │     │
│   │                   │ • Enhance    │                                 │     │
│   │                   └──────┬───────┘                                 │     │
│   │                          │                                         │     │
│   │                          ▼                                         │     │
│   │                   ┌──────────────┐                                 │     │
│   │                   │ Base64       │                                 │     │
│   │                   │ Encoder      │                                 │     │
│   │                   └──────┬───────┘                                 │     │
│   └──────────────────────────┼─────────────────────────────────────────┘     │
│                              │                                               │
│   STAGE 3: ML PROCESSING     │                                               │
│   ┌──────────────────────────┼─────────────────────────────────────────┐     │
│   │                          ▼                                         │     │
│   │                   ┌──────────────┐                                 │     │
│   │                   │ Gemini ML    │                                 │     │
│   │                   │ API Call     │                                 │     │
│   │                   │              │                                 │     │
│   │                   │ Model:       │                                 │     │
│   │                   │ gemini-2.5   │                                 │     │
│   │                   │ flash        │                                 │     │
│   │                   └──────┬───────┘                                 │     │
│   │                          │                                         │     │
│   │                          ▼                                         │     │
│   │                   ┌──────────────┐                                 │     │
│   │                   │ OCR Prompt   │                                 │     │
│   │                   │              │                                 │     │
│   │                   │ "Extract all  │                                 │     │
│   │                   │ text items   │                                 │     │
│   │                   │ with amounts │                                 │     │
│   │                   │ from Kannada │                                 │     │
│   │                   │ bill image"  │                                 │     │
│   │                   └──────┬───────┘                                 │     │
│   │                          │                                         │     │
│   │                          ▼                                         │     │
│   │                   ┌──────────────┐                                 │     │
│   │                   │ Raw Text     │                                 │     │
│   │                   │ Response     │                                 │     │
│   │                   │              │                                 │     │
│   │                   │ "1. Rice 5kg │                                 │     │
│   │                   │   Rs. 250    │                                 │     │
│   │                   │ 2. Dal 1kg   │                                 │     │
│   │                   │   Rs. 120"   │                                 │     │
│   │                   └──────┬───────┘                                 │     │
│   └──────────────────────────┼─────────────────────────────────────────┘     │
│                              │                                               │
│   STAGE 4: PARSING           │                                               │
│   ┌──────────────────────────┼─────────────────────────────────────────┐     │
│   │                          ▼                                         │     │
│   │                   ┌──────────────┐                                 │     │
│   │                   │ Text         │                                 │     │
│   │                   │ Normalizer   │                                 │     │
│   │                   │              │                                 │     │
│   │                   │ • Clean      │                                 │     │
│   │                   │ • Fix OCR    │                                 │     │
│   │                   │   errors     │                                 │     │
│   │                   │ • Detect     │                                 │     │
│   │                   │   language   │                                 │     │
│   │                   └──────┬───────┘                                 │     │
│   │                          │                                         │     │
│   │                          ▼                                         │     │
│   │                   ┌──────────────┐                                 │     │
│   │                   │ Line Parser  │                                 │     │
│   │                   │              │                                 │     │
│   │                   │ Regex:       │                                 │     │
│   │                   │ (item) +     │                                 │     │
│   │                   │ (qty) +      │                                 │     │
│   │                   │ (price)      │                                 │     │
│   │                   └──────┬───────┘                                 │     │
│   │                          │                                         │     │
│   │                          ▼                                         │     │
│   │                   ┌──────────────┐                                 │     │
│   │                   │ Amount       │                                 │     │
│   │                   │ Extractor    │                                 │     │
│   │                   │              │                                 │     │
│   │                   │ • Rs. 250 →  │                                 │     │
│   │                   │   250.0      │                                 │     │
│   │                   │ • Handles    │                                 │     │
│   │                   │   Kannada    │                                 │     │
│   │                   │   numerals   │                                 │     │
│   │                   └──────┬───────┘                                 │     │
│   └──────────────────────────┼─────────────────────────────────────────┘     │
│                              │                                               │
│   STAGE 5: OUTPUT            │                                               │
│   ┌──────────────────────────┼─────────────────────────────────────────┐     │
│   │                          ▼                                         │     │
│   │                   ┌──────────────┐                                 │     │
│   │                   │ JSON         │                                 │     │
│   │                   │ Formatter    │                                 │     │
│   │                   │              │                                 │     │
│   │                   │ {            │                                 │     │
│   │                   │   "items": [ │                                 │     │
│   │                   │     {        │                                 │     │
│   │                   │       "name":│                                 │     │
│   │                   │       "Rice",│                                 │     │
│   │                   │       "qty": │                                 │     │
│   │                   │       "5kg", │                                 │     │
│   │                   │       "price":                                 │     │
│   │                   │       250.0  │                                 │     │
│   │                   │     }        │                                 │     │
│   │                   │   ],         │                                 │     │
│   │                   │   "total":   │                                 │     │
│   │                   │   370.0      │                                 │     │
│   │                   │ }            │                                 │     │
│   │                   └──────┬───────┘                                 │     │
│   │                          │                                         │     │
│   │                          ▼                                         │     │
│   │                   ┌──────────────┐                                 │     │
│   │                   │ Invoice      │                                 │     │
│   │                   │ Object       │                                 │     │
│   │                   │              │                                 │     │
│   │                   │ • items: List│                                 │     │
│   │                   │ • total:     │                                 │     │
│   │                   │   Double     │                                 │     │
│   │                   │ • date: Long │                                 │     │
│   │                   └──────┬───────┘                                 │     │
│   │                          │                                         │     │
│   │                          ▼                                         │     │
│   │                   ┌──────────────┐                                 │     │
│   │                   │ UI Display   │                                 │     │
│   │                   │              │                                 │     │
│   │                   │ • Editable   │                                 │     │
│   │                   │ • Line items │                                 │     │
│   │                   │ • Total calc │                                 │     │
│   │                   └──────┬───────┘                                 │     │
│   │                          │                                         │     │
│   │                          ▼                                         │     │
│   │                   ┌──────────────┐                                 │     │
│   │                   │ Save as Txn  │                                 │     │
│   │                   │              │                                 │     │
│   │                   │ Convert to   │                                 │     │
│   │                   │ Transaction  │                                 │     │
│   │                   │ entity       │                                 │     │
│   │                   └──────────────┘                                 │     │
│   │                                                                    │     │
│   └────────────────────────────────────────────────────────────────────┘     │
│                                                                              │
│   ERROR HANDLING:                                                            │
│   ┌──────────────────────────────────────────────────────────────────┐      │
│   │  ┌─────────────┐    ┌─────────────┐    ┌─────────────┐             │      │
│   │  │ API Fail    │───>│ Manual Entry│    │ Retry/Skip  │             │      │
│   │  │ Parse Error │───>│ Fallback    │───>│ Options     │             │      │
│   │  └─────────────┘    └─────────────┘    └─────────────┘             │      │
│   └──────────────────────────────────────────────────────────────────┘      │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## PART 9: QR CONFIRMATION FLOW DIAGRAM

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      QR CONFIRMATION SYSTEM ARCHITECTURE                       │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                    QR GENERATION FLOW (Vendor App)                     │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │                                                                        │ │
│  │   ┌─────────────┐                                                      │ │
│  │   │ Transaction │                                                      │ │
│  │   │  Saved      │                                                      │ │
│  │   └──────┬──────┘                                                      │ │
│  │          │                                                             │ │
│  │          ▼                                                             │ │
│  │   ┌─────────────┐     ┌─────────────┐                                  │ │
│  │   │  Generate   │────>│   Nonce     │                                  │ │
│  │   │   Payload   │     │  Generator  │                                  │ │
│  │   │             │     │             │                                  │ │
│  │   │ • txnId     │     │ • UUID v4   │                                  │ │
│  │   │ • amount    │     │ • 16 chars  │                                  │ │
│  │   │ • type      │     │ • Unique    │                                  │ │
│  │   │ • timestamp │     └──────┬──────┘                                  │ │
│  │   │ • nonce     │              │                                       │ │
│  │   └──────┬──────┘              │                                       │ │
│  │          │                     │                                       │ │
│  │          └─────────────────────┘                                       │ │
│  │                     │                                                  │ │
│  │                     ▼                                                  │ │
│  │   ┌─────────────────────────────────┐                                   │ │
│  │   │      HASH CALCULATION           │                                 │ │
│  │   ├─────────────────────────────────┤                                 │ │
│  │   │                                 │                                 │ │
│  │   │  SHA-256(                      │                                 │ │
│  │   │    txnId +                     │                                 │ │
│  │   │    amount +                    │                                 │ │
│  │   │    type +                      │                                 │ │
│  │   │    customerId +                │                                 │ │
│  │   │    timestamp +                 │                                 │ │
│  │   │    nonce +                     │                                 │ │
│  │   │    SECRET_KEY                  │                                 │ │
│  │   │  )                             │                                 │ │
│  │   │                                 │                                 │ │
│  │   │  → 64-char hex hash           │                                 │ │
│  │   │                                 │                                 │ │
│  │   └──────────────────┬──────────────┘                                │ │
│  │                      │                                                 │ │
│  │                      ▼                                                 │ │
│  │   ┌─────────────────────────────────┐                                 │ │
│  │   │      EXPIRY CALCULATION         │                                 │ │
│  │   ├─────────────────────────────────┤                                 │ │
│  │   │                                 │                                 │ │
│  │   │  expiresAt =                   │                                 │ │
│  │   │    timestamp +                 │                                 │ │
│  │   │    QR_TTL_MS                   │                                 │ │
│  │   │    (60 seconds)                │                                 │ │
│  │   │                                 │                                 │ │
│  │   └──────────────────┬──────────────┘                                │ │
│  │                      │                                                 │ │
│  │                      ▼                                                 │ │
│  │   ┌─────────────────────────────────┐                                 │ │
│  │   │      JSON PAYLOAD               │                                 │ │
│  │   ├─────────────────────────────────┤                                 │ │
│  │   │  {                             │                                 │ │
│  │   │    "txnId": 12345,             │                                 │ │
│  │   │    "amount": 250.00,           │                                 │ │
│  │   │    "type": "CREDIT",           │                                 │ │
│  │   │    "customerId": 789,          │                                 │ │
│  │   │    "timestamp": 1699999999,    │                                 │ │
│  │   │    "nonce": "a1b2c3d4...",     │                                 │ │
│  │   │    "expiresAt": 1700000059,  │                                 │ │
│  │   │    "hash": "f8a9b2c1..."       │                                 │ │
│  │   │  }                             │                                 │ │
│  │   └──────────────────┬──────────────┘                                │ │
│  │                      │                                                 │ │
│  │                      ▼                                                 │ │
│  │   ┌─────────────────────────────────┐                                 │ │
│  │   │      USER-FRIENDLY TEXT         │                                 │ │
│  │   ├─────────────────────────────────┤                                 │ │
│  │   │  "Namma Santhe CREDIT          │                                 │ │
│  │   │   Amount: Rs. 250.00           │                                 │ │
│  │   │   ID: 12345                    │                                 │ │
│  │   │   [JSON_PAYLOAD]"              │                                 │ │
│  │   │                                 │                                 │ │
│  │   └──────────────────┬──────────────┘                                │ │
│  │                      │                                                 │ │
│  │                      ▼                                                 │ │
│  │   ┌─────────────────────────────────┐                                 │ │
│  │   │      QR BITMAP GENERATION       │                                 │ │
│  │   ├─────────────────────────────────┤                                 │ │
│  │   │                                 │                                 │ │
│  │   │  ZXing QR Code                  │                                 │ │
│  │   │  • Size: 512x512 px             │                                 │ │
│  │   │  • Encoding: UTF-8              │                                 │ │
│  │   │  • Error Correction: H (30%)    │                                 │ │
│  │   │                                 │                                 │ │
│  │   └─────────────────────────────────┘                                 │ │
│  │                                                                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                    QR VALIDATION FLOW (Customer Scan)                  │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │                                                                        │ │
│  │   ┌─────────────┐                                                      │ │
│  │   │ QR Scanned  │                                                      │ │
│  │   │ by Customer │                                                      │ │
│  │   └──────┬──────┘                                                      │ │
│  │          │                                                             │ │
│  │          ▼                                                             │ │
│  │   ┌─────────────┐                                                      │ │
│  │   │  Extract    │                                                      │ │
│  │   │ JSON from   │                                                      │ │
│  │   │ QR Text     │                                                      │ │
│  │   └──────┬──────┘                                                      │ │
│  │          │                                                             │ │
│  │          ▼                                                             │ │
│  │   ┌─────────────────────────────────────────────────────────────┐     │ │
│  │   │                      VALIDATION CHECKS                       │     │ │
│  │   ├─────────────────────────────────────────────────────────────┤     │ │
│  │   │                                                              │     │ │
│  │   │  ┌───────────────┐                                          │     │ │
│  │   │  │ 1. EXPIRY    │  Is currentTime > expiresAt?            │     │ │
│  │   │  │    CHECK     │  → REJECT if expired                     │     │ │
│  │   │  └───────────────┘                                          │     │ │
│  │   │           │                                                │     │ │
│  │   │           ▼                                                │     │ │
│  │   │  ┌───────────────┐                                          │     │ │
│  │   │  │ 2. PAYLOAD    │  Can we parse all required fields?      │     │ │
│  │   │  │   PARSE       │  → REJECT if malformed                 │     │ │
│  │   │  └───────────────┘                                          │     │ │
│  │   │           │                                                │     │ │
│  │   │           ▼                                                │     │ │
│  │   │  ┌───────────────┐                                          │     │ │
│  │   │  │ 3. HASH       │  Recalculate hash with SECRET_KEY        │     │ │
│  │   │  │   VERIFY      │  → REJECT if mismatch (tampered!)      │     │ │
│  │   │  └───────────────┘                                          │     │ │
│  │   │           │                                                │     │ │
│  │   │           ▼                                                │     │ │
│  │   │  ┌───────────────┐                                          │     │ │
│  │   │  │ 4. TXN EXISTS │  Does txnId exist in DB?               │     │ │
│  │   │  │   CHECK       │  → REJECT if not found                 │     │ │
│  │   │  └───────────────┘                                          │     │ │
│  │   │           │                                                │     │ │
│  │   │           ▼                                                │     │ │
│  │   │  ┌───────────────┐                                          │     │ │
│  │   │  │ 5. AMOUNT     │  Does amount match txn record?          │     │ │
│  │   │  │   MATCH       │  → REJECT if mismatch                  │     │ │
│  │   │  └───────────────┘                                          │     │ │
│  │   │           │                                                │     │ │
│  │   │           ▼                                                │     │ │
│  │   │  ┌───────────────┐                                          │     │ │
│  │   │  │ 6. REUSE      │  Has this QR been scanned before?       │     │ │
│  │   │  │   CHECK       │  → REJECT if duplicate nonce            │     │ │
│  │   │  └───────────────┘                                          │     │ │
│  │   │                                                              │     │ │
│  │   └─────────────────────────────────────────────────────────────┘     │ │
│  │                      │                                                 │ │
│  │                      ▼                                                 │ │
│  │   ┌─────────────────────────────────────────────────────────────┐     │ │
│  │   │                   TRUST ASSESSMENT                           │     │ │
│  │   ├─────────────────────────────────────────────────────────────┤     │ │
│  │   │                                                              │     │ │
│  │   │  ┌──────────────────────────────────────────────┐            │     │ │
│  │   │  │ TRUST LEVEL CALCULATION                      │            │     │ │
│  │   │  ├──────────────────────────────────────────────┤            │     │ │
│  │   │  │                                              │            │     │ │
│  │   │  │  deviceMatch: scanner == vendorDevice?      │            │     │ │
│  │   │  │  timingCheck: scanned within expiry?        │            │     │ │
│  │   │  │  nonceValid:  nonce not reused?             │            │     │ │
│  │   │  │  hashValid:   hash verified?                │            │     │ │
│  │   │  │                                              │            │     │ │
│  │   │  │  if all true →  TRUSTED (Green)            │            │ │
│  │   │  │  if 3/4 true →  VERIFIED (Blue)            │            │ │
│  │   │  │  if 2/4 true →  CAUTION (Yellow)           │            │ │
│  │   │  │  if <2 true  →  UNTRUSTED (Red)            │            │ │
│  │   │  │                                              │            │     │ │
│  │   │  └──────────────────────────────────────────────┘            │     │ │
│  │   │                                                              │     │ │
│  │   └─────────────────────────────────────────────────────────────┘     │ │
│  │                      │                                                 │ │
│  │                      ▼                                                 │ │
│  │   ┌─────────────────────────────────────────────────────────────┐     │ │
│  │   │                   CONFIRMATION STORAGE                       │     │ │
│  │   ├─────────────────────────────────────────────────────────────┤     │ │
│  │   │                                                              │     │ │
│  │   │  Save to Database:                                           │     │ │
│  │   │  • scanId (auto)                                           │     │ │
│  │   │  • txnId (from QR)                                         │     │ │
│  │   │  • scannedAt (timestamp)                                   │     │ │
│  │   │  • scannerDeviceId (unique ID)                             │     │ │
│  │   │  • trustLevel (calculated)                                 │     │ │
│  │   │  • payloadHash (for audit)                                 │     │ │
│  │   │                                                              │     │ │
│  │   │  Update Transaction:                                       │     │ │
│  │   │  • confirmed = true                                        │     │ │
│  │   │  • confirmationTime = now                                  │     │ │
│  │   │                                                              │     │ │
│  │   └─────────────────────────────────────────────────────────────┘     │ │
│  │                      │                                                 │ │
│  │                      ▼                                                 │ │
│  │   ┌─────────────────────────────────────────────────────────────┐     │ │
│  │   │                      UI DISPLAY                              │     │ │
│  │   ├─────────────────────────────────────────────────────────────┤     │ │
│  │   │                                                              │     │ │
│  │   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐         │     │ │
│  │   │  │  Trust      │  │ Transaction │  │   Confirm   │         │     │ │
│  │   │  │  Badge      │  │   Details   │  │   Button    │         │     │ │
│  │   │  │             │  │             │  │             │         │     │ │
│  │   │  │ [GREEN]     │  │ • Amount    │  │ Tap to      │         │     │ │
│  │   │  │ [BLUE]      │  │ • Customer  │  │ finalize    │         │     │ │
│  │   │  │ [YELLOW]    │  │ • Type      │  │             │         │     │ │
│  │   │  │ [RED]       │  │ • Time      │  │             │         │     │ │
│  │   │  └─────────────┘  └─────────────┘  └─────────────┘         │     │ │
│  │   │                                                              │     │ │
│  │   └─────────────────────────────────────────────────────────────┘     │ │
│  │                                                                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐ │
│  │                      SECURITY FEATURES                                 │ │
│  ├────────────────────────────────────────────────────────────────────────┤ │
│  │                                                                        │ │
│  │  • SECRET_KEY: Stored in Android Keystore (never in code)            │ │
│  │  • QR TTL: 60 seconds (configurable)                                   │ │
│  │  • Nonce tracking: Prevents replay attacks                             │ │
│  │  • Device binding: Links scan to specific device                       │ │
│  │  • Hash chain: Each QR builds on previous for audit trail              │ │
│  │                                                                        │ │
│  └────────────────────────────────────────────────────────────────────────┘ │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## PART 10: SECURITY / TAMPER FLOW

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                      SECURITY & TAMPER PROTECTION ARCHITECTURE                 │
├──────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐   │
│  │                    THREAT MODEL                                       │   │
│  ├────────────────────────────────────────────────────────────────────────┤   │
│  │                                                                        │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐ │   │
│  │  │  Data Tamper │  │  Replay      │  │  Unauthorized│  │   Device   │ │   │
│  │  │  (Modify txns│  │  (Reuse QR)  │  │  Access      │  │   Theft    │ │   │
│  │  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └─────┬──────┘ │   │
│  │         │                 │                 │                │        │   │
│  │         └─────────────────┴─────────────────┴────────────────┘        │   │
│  │                              │                                          │   │
│  └──────────────────────────────┼──────────────────────────────────────────┘   │
│                                 │                                              │
│                                 ▼                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐   │
│  │                    DEFENSE MECHANISMS                                 │   │
│  ├────────────────────────────────────────────────────────────────────────┤   │
│  │                                                                        │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │  1. APPEND-ONLY LEDGER                                          │   │   │
│  │  ├─────────────────────────────────────────────────────────────────┤   │   │
│  │  │                                                                  │   │   │
│  │  │   Principle: Transactions are NEVER deleted or modified         │   │   │
│  │  │                                                                  │   │   │
│  │  │   • Credit → Creates positive entry                             │   │   │
│  │  │   • Payment → Creates negative entry (not deletion)               │   │   │
│  │  │   • Correction → New correcting transaction (audit trail)         │   │   │
│  │  │                                                                  │   │   │
│  │  │   Table: transactions                                           │   │   │
│  │  │   ┌────┬──────────┬────────┬────────┬──────────┐                │   │   │
│  │  │   │ id │ customer │ amount │ type   │ note     │                │   │   │
│  │  │   ├────┼──────────┼────────┼────────┼──────────┤                │   │   │
│  │  │   │ 1  │ Ramesh   │ 500    │ CREDIT │ Initial  │                │   │   │
│  │  │   │ 2  │ Ramesh   │ 200    │ PAYMENT│ Partial  │                │   │   │
│  │  │   │ 3  │ Ramesh   │ 100    │ CREDIT │ Add more │                │   │   │
│  │  │   │ 4  │ Ramesh   │ -50    │ CORRECT│ Fix #3   │                │   │   │
│  │  │   └────┴──────────┴────────┴────────┴──────────┘                │   │   │
│  │  │                                                                  │   │   │
│  │  │   Balance = SUM(all amounts) = 500 - 200 + 100 - 50 = 350     │   │   │
│  │  │                                                                  │   │   │
│  │  └─────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                        │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │  2. HASH CHAIN (Blockchain-style)                               │   │   │
│  │  ├─────────────────────────────────────────────────────────────────┤   │   │
│  │  │                                                                  │   │   │
│  │  │   Each transaction references previous transaction hash         │   │   │
│  │  │                                                                  │   │   │
│  │  │   ┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐     │   │   │
│  │  │   │ Txn #1  │───>│ Txn #2  │───>│ Txn #3  │───>│ Txn #4  │     │   │   │
│  │  │   │         │    │ prevHash│    │ prevHash│    │ prevHash│     │   │   │
│  │  │   │ hash:A  │───>│ =A      │───>│ =B      │───>│ =C      │     │   │   │
│  │  │   │         │    │ hash:B  │───>│ hash:C  │───>│ hash:D  │     │   │   │
│  │  │   └─────────┘    └─────────┘    └─────────┘    └─────────┘     │   │   │
│  │  │                                                                  │   │   │
│  │  │   If Txn #2 is modified, hash:B changes, breaking chain       │   │   │
│  │  │   → Detection on next app launch or sync                        │   │   │
│  │  │                                                                  │   │   │
│  │  │   Hash includes:                                              │   │   │
│  │  │   • Previous hash                                             │   │   │
│  │  │   • Transaction data (amount, type, customer)                 │   │   │
│  │  │   • Timestamp                                                 │   │   │
│  │  │   • Device ID                                                 │   │   │
│  │  │                                                                  │   │   │
│  │  └─────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                        │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │  3. AUDIT LOG                                                   │   │   │
│  │  ├─────────────────────────────────────────────────────────────────┤   │   │
│  │  │                                                                  │   │   │
│  │  │   Table: audit_log                                              │   │   │
│  │  │   ┌──────────┬────────┬───────────┬──────────┬──────────┐    │   │   │
│  │  │   │ timestamp│ action │ entity    │ oldValue │ newValue │    │   │   │
│  │  │   ├──────────┼────────┼───────────┼──────────┼──────────┤    │   │   │
│  │  │   │ 16999... │ CREATE │ txn:123   │ null     │ {amount: │    │   │   │
│  │  │   │          │        │           │          │  250}    │    │   │   │
│  │  │   │ 17000... │ UPDATE │ cust:45   │ {phone:  │ {phone:  │    │   │   │
│  │  │   │          │        │           │  123}    │  999}    │    │   │   │
│  │  │   │ 17000... │ DELETE │ photo:1   │ /path/   │ null     │    │   │   │
│  │  │   └──────────┴────────┴───────────┴──────────┴──────────┘    │   │   │
│  │  │                                                                  │   │   │
│  │  │   • Immutable (no delete, no modify)                          │   │   │
│  │  │   • Exportable for external audit                             │   │   │
│  │  │   • Synced to cloud with encryption                           │   │   │
│  │  │                                                                  │   │   │
│  │  └─────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                        │   │
│  │  ┌─────────────────────────────────────────────────────────────────┐   │   │
│  │  │  4. SUSPICIOUS ACTIVITY DETECTION                               │   │   │
│  │  ├─────────────────────────────────────────────────────────────────┤   │   │
│  │  │                                                                  │   │   │
│  │  │   Detection Rules:                                            │   │   │
│  │  │                                                                  │   │   │
│  │  │   ┌────────────────────────────────────────────────────────┐    │   │   │
│  │  │   │ 1. Amount Anomaly                                      │    │   │   │
│  │  │   │    IF amount > 3x average txn for customer             │    │   │   │
│  │  │   │    → FLAG: "Unusually large transaction"               │    │   │   │
│  │  │   └────────────────────────────────────────────────────────┘    │   │   │
│  │  │                                                                  │   │   │
│  │  │   ┌────────────────────────────────────────────────────────┐    │   │   │
│  │  │   │ 2. Time Anomaly                                        │    │   │   │
│  │  │   │    IF txn outside business hours (8PM-6AM)             │    │   │   │
│  │  │   │    → FLAG: "Off-hours transaction"                     │    │   │   │
│  │  │   └────────────────────────────────────────────────────────┘    │   │   │
│  │  │                                                                  │   │   │
│  │  │   ┌────────────────────────────────────────────────────────┐    │   │   │
│  │  │   │ 3. Frequency Anomaly                                   │    │   │   │
│  │  │   │    IF >10 txns in 5 minutes from same device           │    │   │   │
│  │  │   │    → FLAG: "Burst activity - possible bot"           │    │   │   │
│  │  │   └────────────────────────────────────────────────────────┘    │   │   │
│  │  │                                                                  │   │   │
│  │  │   ┌────────────────────────────────────────────────────────┐    │   │   │
│  │  │   │ 4. Hash Mismatch                                       │    │   │   │
│  │  │   │    IF recalculated hash != stored hash                 │    │   │   │
│  │  │   │    → ALERT: "Data tampering detected!"               │    │   │   │
│  │  │   │    → DISABLE sync, notify user                       │    │   │   │
│  │  │   └────────────────────────────────────────────────────────┘    │   │   │
│  │  │                                                                  │   │   │
│  │  │   ┌────────────────────────────────────────────────────────┐    │   │   │
│  │  │   │ 5. QR Replay Attempt                                   │    │   │   │
│  │  │   │    IF nonce already in confirmation table              │    │   │   │
│  │  │   │    → REJECT: "QR already used"                         │    │   │   │
│  │  │   │    → LOG: Potential replay attack                      │    │   │   │
│  │  │   └────────────────────────────────────────────────────────┘    │   │   │
│  │  │                                                                  │   │   │
│  │  └─────────────────────────────────────────────────────────────────┘   │   │
│  │                                                                        │   │
│  └────────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐   │
│  │                    ENCRYPTION & STORAGE                               │   │
│  ├────────────────────────────────────────────────────────────────────────┤   │
│  │                                                                        │   │
│  │   ┌────────────────────────────────────────────────────────────┐    │   │
│  │   │  DATA AT REST                                               │    │   │
│  │   ├────────────────────────────────────────────────────────────┤    │   │
│  │   │                                                               │    │   │
│  │   │  • Room Database: SQLite with SQLCipher encryption           │    │   │
│  │   │  • Photos: AES-256 encrypted in private app storage        │    │   │
│  │   │  • Keys: Android Keystore (hardware-backed if available)   │    │   │
│  │   │  • Backup: Encrypted with user password-derived key          │    │   │
│  │   │                                                               │    │   │
│  │   └────────────────────────────────────────────────────────────┘    │   │
│  │                                                                        │   │
│  │   ┌────────────────────────────────────────────────────────────┐    │   │
│  │   │  DATA IN TRANSIT                                            │    │   │
│  │   ├────────────────────────────────────────────────────────────┤    │   │
│  │   │                                                               │    │   │
│  │   │  • Firebase: TLS 1.3 (automatic)                           │    │   │
│  │   │  • QR Payload: Signed with HMAC-SHA256                     │    │   │
│  │   │  • API Calls: HTTPS with certificate pinning               │    │   │
│  │   │                                                               │    │   │
│  │   └────────────────────────────────────────────────────────────┘    │   │
│  │                                                                        │   │
│  │   ┌────────────────────────────────────────────────────────────┐    │   │
│  │   │  PIN / BIOMETRIC                                            │    │   │
│  │   ├────────────────────────────────────────────────────────────┤    │   │
│  │   │                                                               │    │   │
│  │   │  • PIN: Argon2id hash (memory-hard)                          │    │   │
│  │   │  • Biometric: Android BiometricPrompt API                     │    │   │
│  │   │  • Fallback: Security questions (optional)                 │    │   │
│  │   │                                                               │    │   │
│  │   └────────────────────────────────────────────────────────────┘    │   │
│  │                                                                        │   │
│  └────────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
│  ┌────────────────────────────────────────────────────────────────────────┐   │
│  │                    TAMPER DETECTION FLOW                              │   │
│  ├────────────────────────────────────────────────────────────────────────┤   │
│  │                                                                        │   │
│  │   App Launch                                                            │   │
│  │       │                                                                 │   │
│  │       ▼                                                                 │   │
│  │   ┌─────────────┐                                                      │   │
│  │   │ Verify Hash │                                                      │   │
│  │   │   Chain     │                                                      │   │
│  │   └──────┬──────┘                                                      │   │
│  │          │                                                              │   │
│  │          ▼                                                              │   │
│  │   ┌──────────────┐                                                     │   │
│  │   │  Checksum    │                                                     │   │
│  │   │  Validation  │                                                     │   │
│  │   └──────┬───────┘                                                     │   │
│  │          │                                                              │   │
│  │     ┌────┴────┐                                                         │   │
│  │     │         │                                                         │   │
│  │  VALID    INVALID                                                       │   │
│  │     │         │                                                         │   │
│  │     ▼         ▼                                                         │   │
│  │  ┌──────┐  ┌──────────────┐                                            │   │
│  │  │Normal│  │  ALERT USER  │                                            │   │
│  │  │Launch│  │  ├─ Show warning                                           │   │
│  │  └──────┘  │  ├─ Disable cloud sync                                     │   │
│  │            │  ├─ Suggest restore from backup                           │   │
│  │            │  └─ Log incident                                          │   │
│  │            └──────────────┘                                            │   │
│  │                                                                        │   │
│  └────────────────────────────────────────────────────────────────────────┘   │
│                                                                              │
└──────────────────────────────────────────────────────────────────────────────┘
```

---

## APPENDIX: PROJECT STRUCTURE

```
app/
├── src/main/java/com/nammasanthe/ledger/
│   ├── MainActivity.kt              # Entry point, Navigation setup
│   ├── data/
│   │   ├── database/
│   │   │   ├── AppDatabase.kt       # Room database
│   │   │   ├── Converters.kt        # Type converters
│   │   │   └── migrations/          # Migration scripts
│   │   ├── entity/
│   │   │   ├── Customer.kt          # Customer entity
│   │   │   ├── TxnEntity.kt         # Transaction entity
│   │   │   ├── BusinessProfile.kt   # Settings entity
│   │   │   └── Confirmation.kt      # QR confirmation entity
│   │   └── dao/
│   │       ├── CustomerDao.kt       # Customer queries
│   │       ├── TransactionDao.kt    # Transaction queries
│   │       └── SettingsDao.kt       # Settings queries
│   ├── repository/
│   │   ├── LedgerRepository.kt      # Main data operations
│   │   ├── CustomerRepository.kt    # Customer operations
│   │   └── DataExportManager.kt     # PDF export logic
│   ├── viewmodel/
│   │   ├── LedgerViewModel.kt       # Ledger screen logic
│   │   ├── ProfileViewModel.kt      # Profile & auth logic
│   │   ├── ConfirmationViewModel.kt # QR generation/scan
│   │   └── OcrViewModel.kt          # OCR processing
│   ├── ui/
│   │   ├── screens/
│   │   │   ├── HomeScreen.kt        # Dashboard
│   │   │   ├── CustomerScreen.kt    # Add/edit customer
│   │   │   ├── CustomerListScreen.kt # Customer list
│   │   │   ├── CustomerLedgerScreen.kt # Transaction history
│   │   │   ├── EntryScreen.kt       # Quick transaction entry
│   │   │   ├── ProfileScreen.kt     # Settings & export
│   │   │   ├── QrDisplayScreen.kt   # Show QR code
│   │   │   ├── QrScannerConfirmScreen.kt # Scan QR
│   │   │   └── OcrScreen.kt         # Bill scanning
│   │   ├── components/
│   │   │   ├── CustomerCard.kt      # Customer list item
│   │   │   ├── TransactionItem.kt   # Transaction row
│   │   │   ├── BalanceHeader.kt     # Balance display
│   │   │   └── QrCameraView.kt      # Camera preview
│   │   ├── nav/
│   │   │   └── Routes.kt            # Navigation routes
│   │   └── theme/
│   │       ├── Color.kt             # App colors
│   │       ├── Theme.kt               # App theme
│   │       └── Type.kt              # Typography
│   ├── sync/
│   │   ├── FirebaseAuthManager.kt   # Firebase auth
│   │   └── FirebaseSyncManager.kt   # Cloud sync
│   ├── security/
│   │   ├── QrGenerator.kt           # QR generation
│   │   ├── QrValidator.kt           # QR validation
│   │   └── QrScannerAnalyzer.kt     # QR scanning
│   ├── gemini/
│   │   └── GeminiService.kt         # OCR API
│   └── util/
│       ├── PhotoManager.kt          # Photo handling
│       └── PdfGenerator.kt            # PDF generation
├── src/main/res/
│   ├── drawable/                    # Icons & images
│   ├── values/                      # Colors, strings
│   └── xml/                         # File paths config
└── build.gradle.kts                 # App dependencies
```

---

## TECHNOLOGY STACK SUMMARY

| Layer | Technology | Purpose |
|-------|-----------|---------|
| UI | Jetpack Compose | Modern declarative UI |
| State | Kotlin StateFlow | Reactive state management |
| Architecture | MVVM + Repository | Clean separation of concerns |
| Database | Room (SQLite) | Local data persistence |
| OCR | Gemini ML Kit | Text recognition from images |
| QR | ZXing | QR code generation/scanning |
| PDF | iText7 | PDF invoice generation |
| Auth | Firebase Auth | User authentication |
| Cloud | Firestore | Cloud data backup |
| Images | CameraX + Coil | Camera and image loading |
| DI | Manual (no Hilt) | Dependency injection |

---

*Documentation generated for academic and evaluation purposes.*  
*Aligns with actual implementation in the Namma-Santhe Ledger Android application.*
