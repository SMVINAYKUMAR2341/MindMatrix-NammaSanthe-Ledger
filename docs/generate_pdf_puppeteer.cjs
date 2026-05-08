const puppeteer = require('puppeteer');
const fs = require('fs');
const path = require('path');

const imgDir = path.join(__dirname, 'images');

function imgB64(name) {
  const p = path.join(imgDir, name);
  if (!fs.existsSync(p)) return '';
  const buf = fs.readFileSync(p);
  return 'data:image/png;base64,' + buf.toString('base64');
}

const html = `<!DOCTYPE html>
<html><head><meta charset="utf-8">
<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap');
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Inter', 'Segoe UI', sans-serif; color: #111827; line-height: 1.65; }

/* Cover */
.cover { height: 100vh; display: flex; flex-direction: column; justify-content: center; align-items: center; background: linear-gradient(145deg, #0f172a 0%, #1e293b 40%, #0f172a 100%); color: white; text-align: center; page-break-after: always; }
.cover h1 { font-size: 48px; font-weight: 800; letter-spacing: -1px; margin-bottom: 6px; }
.cover h2 { font-size: 20px; font-weight: 300; opacity: 0.85; margin-bottom: 40px; }
.cover .tags { margin: 20px 0; }
.cover .tag { display: inline-block; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); padding: 5px 16px; border-radius: 16px; margin: 3px; font-size: 12px; font-weight: 500; }
.cover .meta { font-size: 13px; opacity: 0.6; line-height: 2; margin-top: 30px; }

/* Page layout */
.page { padding: 30px 40px; page-break-after: always; }
.page-img { padding: 20px 30px; page-break-after: always; }

/* Typography */
h1 { font-size: 26px; font-weight: 800; color: #0f172a; border-bottom: 3px solid #3b82f6; padding-bottom: 8px; margin: 0 0 18px 0; }
h2 { font-size: 19px; font-weight: 700; color: #1e3a5f; margin: 20px 0 10px 0; }
h3 { font-size: 15px; font-weight: 600; color: #4338ca; margin: 14px 0 8px 0; }
p { font-size: 12.5px; margin: 6px 0; }
ul, ol { font-size: 12.5px; margin: 6px 0 6px 20px; }
li { margin: 3px 0; }
strong { font-weight: 700; }

/* Tables */
table { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 11.5px; }
th { background: #1e293b; color: white; padding: 8px 10px; text-align: left; font-weight: 600; font-size: 11px; }
td { padding: 6px 10px; border: 1px solid #e2e8f0; }
tr:nth-child(even) { background: #f8fafc; }

/* Diagrams - FULL WIDTH, HIGH QUALITY */
img.diagram { width: 100%; height: auto; margin: 12px 0; border: 1px solid #e2e8f0; border-radius: 6px; image-rendering: -webkit-optimize-contrast; }
.diagram-caption { font-size: 11px; color: #64748b; text-align: center; font-style: italic; margin: 4px 0 16px 0; }

/* Code blocks */
.code { background: #0f172a; color: #e2e8f0; padding: 14px 16px; border-radius: 6px; font-family: 'Cascadia Code', 'Consolas', monospace; font-size: 10px; white-space: pre; line-height: 1.5; margin: 10px 0; overflow: hidden; }

/* Notes */
.note { background: #eff6ff; border-left: 3px solid #3b82f6; padding: 8px 12px; margin: 10px 0; border-radius: 0 4px 4px 0; font-size: 11.5px; }

/* TOC */
.toc { columns: 2; font-size: 13px; }
.toc li { margin: 5px 0; }

footer { text-align: center; color: #94a3b8; font-size: 10px; padding: 20px 0; }
</style></head><body>

<!-- ═══════════════════ COVER ═══════════════════ -->
<div class="cover">
  <div style="font-size:64px;margin-bottom:16px;">📗</div>
  <h1>Namma-Santhe Ledger</h1>
  <h2>Technical Documentation Report</h2>
  <div style="width:50px;height:3px;background:#3b82f6;margin:16px auto;border-radius:2px;"></div>
  <div class="tags">
    <span class="tag">Android • Kotlin</span>
    <span class="tag">Jetpack Compose</span>
    <span class="tag">MVVM + Clean Architecture</span>
    <span class="tag">Offline-First</span>
    <span class="tag">Firebase Cloud Sync</span>
    <span class="tag">Gemini OCR</span>
    <span class="tag">QR Verification</span>
  </div>
  <div class="meta">Prepared for Academic &amp; Project Evaluation<br>May 2026</div>
</div>

<!-- ═══════════════════ TOC ═══════════════════ -->
<div class="page">
<h1>Table of Contents</h1>
<ol class="toc" style="margin-top:20px;">
  <li>System Overview</li>
  <li>System Architecture Diagram</li>
  <li>UML Class Diagram</li>
  <li>Use Case Diagram</li>
  <li>Sequence Diagram</li>
  <li>Data Flow Diagram</li>
  <li>App Navigation Flow</li>
  <li>OCR Pipeline Architecture</li>
  <li>QR Confirmation System</li>
  <li>Security &amp; Tamper Protection</li>
  <li>Entity-Relationship Diagram</li>
  <li>Project Structure</li>
  <li>Technology Stack</li>
</ol>
</div>

<!-- ═══════════════════ 1. OVERVIEW ═══════════════════ -->
<div class="page">
<h1>1. System Overview</h1>

<h2>Problem Statement</h2>
<p>Rural vendors in Indian markets maintain handwritten ledgers for credit ("udari") tracking. This leads to lost/damaged records, calculation errors, no backups, and delayed payments.</p>

<h2>Solution</h2>
<p>Namma-Santhe Ledger is an <strong>offline-first Android application</strong> that digitizes credit tracking with automatic balance calculation, customer photo identification, QR-based transaction confirmation, OCR bill scanning, cloud backup, and PDF export.</p>

<h2>Key Features</h2>
<table>
  <tr><th>Feature</th><th>Description</th><th>Technology</th></tr>
  <tr><td>Quick Entry</td><td>Numeric keypad for &lt;5 sec transaction entry</td><td>Jetpack Compose</td></tr>
  <tr><td>Customer Management</td><td>Photo capture, search, profiles</td><td>CameraX, Coil</td></tr>
  <tr><td>Credit Tracking</td><td>Auto-calculated outstanding balances</td><td>Room DB</td></tr>
  <tr><td>QR Confirmation</td><td>Tamper-proof transaction verification</td><td>ZXing, HMAC-SHA256</td></tr>
  <tr><td>OCR Bill Scan</td><td>Extract text from handwritten/printed bills</td><td>Gemini ML + Tesseract</td></tr>
  <tr><td>Cloud Sync</td><td>Firebase Auth + Firestore backup</td><td>Firebase</td></tr>
  <tr><td>PDF Export</td><td>Shareable ledger reports</td><td>iText7</td></tr>
  <tr><td>Offline Mode</td><td>Full functionality without internet</td><td>Room + SQLite</td></tr>
</table>
</div>

<!-- ═══════════════════ 2. ARCHITECTURE ═══════════════════ -->
<div class="page-img">
<h1>2. System Architecture</h1>
<p>The application follows a <strong>4-layer MVVM + Clean Architecture</strong> pattern:</p>
<ul>
  <li><strong>Presentation Layer</strong> — Jetpack Compose declarative UI screens</li>
  <li><strong>ViewModel Layer</strong> — MVVM state management with Kotlin StateFlow</li>
  <li><strong>Repository Layer</strong> — Data access abstraction (local DB vs cloud)</li>
  <li><strong>Data Layer</strong> — Room DB, Firebase, Gemini ML, ZXing, iText7</li>
</ul>
<img class="diagram" src="${imgB64('system_architecture.png')}" alt="System Architecture Diagram">
<div class="diagram-caption">Figure 2.1 — Four-layer system architecture with dependency flow</div>
</div>

<!-- ═══════════════════ 3. UML CLASS ═══════════════════ -->
<div class="page-img">
<h1>3. UML Class Diagram</h1>
<p>Core entity classes with relationships, QR security classes, and repository pattern.</p>
<img class="diagram" src="${imgB64('uml_class_diagram.png')}" alt="UML Class Diagram">
<div class="diagram-caption">Figure 3.1 — UML Class Diagram showing entity relationships</div>
<h3>Key Relationships</h3>
<table>
  <tr><th>From</th><th>To</th><th>Type</th><th>Description</th></tr>
  <tr><td>Customer</td><td>Transaction</td><td>1 → *</td><td>One customer has many transactions</td></tr>
  <tr><td>Transaction</td><td>ScanConfirmation</td><td>1 → 0..1</td><td>Optional QR confirmation</td></tr>
  <tr><td>ViewModel</td><td>Repository</td><td>Dependency</td><td>Data access through abstraction</td></tr>
  <tr><td>Repository</td><td>Room DAO</td><td>Dependency</td><td>Database queries via DAOs</td></tr>
</table>
</div>

<!-- ═══════════════════ 4. USE CASE ═══════════════════ -->
<div class="page-img">
<h1>4. Use Case Diagram</h1>
<p>Two actors: <strong>Vendor</strong> (primary) and <strong>Customer</strong> (secondary).</p>
<img class="diagram" src="${imgB64('use_case_diagram.png')}" alt="Use Case Diagram">
<div class="diagram-caption">Figure 4.1 — Use Case Diagram with actor-system interactions</div>
<h3>Actor Responsibilities</h3>
<table>
  <tr><th>Actor</th><th>Use Cases</th></tr>
  <tr><td>Vendor</td><td>Manage Customers, Process Transactions, Quick Entry, OCR Scan, Generate QR, Export PDF, Cloud Sync</td></tr>
  <tr><td>Customer</td><td>Scan QR to Confirm Transaction</td></tr>
</table>
</div>

<!-- ═══════════════════ 5. SEQUENCE ═══════════════════ -->
<div class="page-img">
<h1>5. Sequence Diagram — Add Transaction</h1>
<img class="diagram" src="${imgB64('sequence_diagram.png')}" alt="Sequence Diagram">
<div class="diagram-caption">Figure 5.1 — Sequence diagram for Add Transaction flow</div>
<h3>Flow Summary</h3>
<ol>
  <li>User enters amount and taps Save</li>
  <li>EntryScreen calls <code>onSave()</code> on LedgerViewModel</li>
  <li>LedgerVM inserts transaction via Repository → Room DB</li>
  <li>Balance recalculated via <code>SELECT SUM(amount)</code></li>
  <li>UI updates with new balance via StateFlow</li>
  <li>QR code generated with SHA-256 hash</li>
  <li>QR displayed for customer confirmation (60s TTL)</li>
</ol>
</div>

<!-- ═══════════════════ 6. DFD ═══════════════════ -->
<div class="page-img">
<h1>6. Data Flow Diagram (DFD Level 1)</h1>
<img class="diagram" src="${imgB64('data_flow_diagram.png')}" alt="Data Flow Diagram">
<div class="diagram-caption">Figure 6.1 — Level 1 Data Flow Diagram</div>
<h3>Data Stores</h3>
<table>
  <tr><th>ID</th><th>Name</th><th>Description</th></tr>
  <tr><td>D1</td><td>Customer DB</td><td>Customer profiles and metadata</td></tr>
  <tr><td>D2</td><td>Transaction DB</td><td>All credit/payment records (append-only)</td></tr>
  <tr><td>D3</td><td>Photo Storage</td><td>Compressed customer/bill images</td></tr>
  <tr><td>D4</td><td>Settings</td><td>App configuration and preferences</td></tr>
  <tr><td>D5</td><td>QR Hash Store</td><td>Used nonces and hash records</td></tr>
  <tr><td>D6</td><td>Audit Log</td><td>Immutable append-only activity log</td></tr>
</table>
</div>

<!-- ═══════════════════ 7. NAV FLOW ═══════════════════ -->
<div class="page-img">
<h1>7. App Navigation Flow</h1>
<img class="diagram" src="${imgB64('app_navigation_flow.png')}" alt="Navigation Flow">
<div class="diagram-caption">Figure 7.1 — App navigation architecture and screen flow</div>
<h3>Navigation Structure</h3>
<table>
  <tr><th>Tab</th><th>Screen</th><th>Features</th></tr>
  <tr><td>🏠 Home</td><td>Dashboard</td><td>Quick stats, total outstanding, today's transactions</td></tr>
  <tr><td>👥 Customers</td><td>Customer List</td><td>Search, filter → Detail → Ledger</td></tr>
  <tr><td>📷 Scan</td><td>Scanner</td><td>OCR bill scanning, camera capture</td></tr>
  <tr><td>📊 Reports</td><td>Reports</td><td>Analytics, charts, export</td></tr>
</table>
</div>

<!-- ═══════════════════ 8. OCR PIPELINE ═══════════════════ -->
<div class="page-img">
<h1>8. OCR Pipeline Architecture</h1>
<img class="diagram" src="${imgB64('ocr_pipeline.png')}" alt="OCR Pipeline">
<div class="diagram-caption">Figure 8.1 — Multi-engine OCR pipeline with preprocessing stages</div>
<h3>Pipeline Stages</h3>
<table>
  <tr><th>Stage</th><th>Component</th><th>Function</th></tr>
  <tr><td>1. Input</td><td>CameraX</td><td>Capture bill image (handwritten, printed, Kannada, mixed)</td></tr>
  <tr><td>2. Preprocessing</td><td>Image Processor</td><td>Scale to 1200px, grayscale, adaptive threshold, Otsu binarization</td></tr>
  <tr><td>3. Recognition</td><td>Dual Engine</td><td>ML Kit (Latin/numbers) + Tesseract (Kannada script) → result merging</td></tr>
  <tr><td>4. Parsing</td><td>LineTokenizer + BillParser</td><td>Quantity, price, item extraction with GroceryDictionary fuzzy match</td></tr>
  <tr><td>5. Output</td><td>Structured Data</td><td>BillItem[] → editable UI → save as transaction</td></tr>
</table>
</div>

<!-- ═══════════════════ 9. QR SYSTEM ═══════════════════ -->
<div class="page-img">
<h1>9. QR Confirmation System</h1>
<img class="diagram" src="${imgB64('qr_confirmation_flow.png')}" alt="QR Confirmation System">
<div class="diagram-caption">Figure 9.1 — QR generation and validation architecture</div>
<h3>Validation Checks</h3>
<table>
  <tr><th>#</th><th>Check</th><th>Reject Condition</th></tr>
  <tr><td>1</td><td>Expiry</td><td>currentTime > expiresAt (60s TTL)</td></tr>
  <tr><td>2</td><td>Payload Parse</td><td>Malformed JSON payload</td></tr>
  <tr><td>3</td><td>Hash Verify</td><td>SHA-256 recalculation ≠ stored hash</td></tr>
  <tr><td>4</td><td>Txn Exists</td><td>txnId not found in local database</td></tr>
  <tr><td>5</td><td>Amount Match</td><td>QR amount ≠ DB stored amount</td></tr>
  <tr><td>6</td><td>Reuse Check</td><td>Nonce already consumed</td></tr>
</table>
<h3>Trust Assessment Levels</h3>
<table>
  <tr><th>Level</th><th>Condition</th><th>Badge</th></tr>
  <tr><td>TRUSTED</td><td>All checks pass</td><td style="color:#059669;">🟢 Green</td></tr>
  <tr><td>VERIFIED</td><td>3/4 checks pass</td><td style="color:#2563eb;">🔵 Blue</td></tr>
  <tr><td>CAUTION</td><td>2/4 checks pass</td><td style="color:#d97706;">🟡 Yellow</td></tr>
  <tr><td>UNTRUSTED</td><td>&lt;2 checks pass</td><td style="color:#dc2626;">🔴 Red</td></tr>
</table>
</div>

<!-- ═══════════════════ 10. SECURITY ═══════════════════ -->
<div class="page-img">
<h1>10. Security &amp; Tamper Protection</h1>
<img class="diagram" src="${imgB64('security_architecture.png')}" alt="Security Architecture">
<div class="diagram-caption">Figure 10.1 — Security architecture and defense mechanisms</div>
<h3>Defense Mechanisms</h3>
<table>
  <tr><th>Mechanism</th><th>Description</th></tr>
  <tr><td>Append-Only Ledger</td><td>Transactions never deleted; corrections create new entries</td></tr>
  <tr><td>Hash Chain</td><td>Blockchain-style: each txn hash includes previous hash. Modification breaks chain.</td></tr>
  <tr><td>Audit Log</td><td>Immutable log of all CREATE/UPDATE/DELETE with old/new values</td></tr>
  <tr><td>Anomaly Detection</td><td>Amount >3× avg, off-hours, >10 txns/5min, hash mismatch, QR replay</td></tr>
</table>
<h3>Encryption</h3>
<table>
  <tr><th>Layer</th><th>Technology</th></tr>
  <tr><td>Data at Rest</td><td>SQLCipher AES-256, Android Keystore (hardware-backed)</td></tr>
  <tr><td>Data in Transit</td><td>TLS 1.3, HMAC-SHA256 signed payloads, Certificate Pinning</td></tr>
  <tr><td>Authentication</td><td>Argon2id memory-hard PIN hash, BiometricPrompt API</td></tr>
</table>
</div>

<!-- ═══════════════════ 11. ER DIAGRAM ═══════════════════ -->
<div class="page-img">
<h1>11. Entity-Relationship Diagram</h1>
<img class="diagram" src="${imgB64('er_diagram.png')}" alt="ER Diagram">
<div class="diagram-caption">Figure 11.1 — Database entity-relationship diagram</div>
<h3>Table Summary</h3>
<table>
  <tr><th>Table</th><th>Key Fields</th><th>Relationships</th></tr>
  <tr><td>Customer</td><td>id (PK), name, phone, address, photoPath</td><td>1 → * Transaction</td></tr>
  <tr><td>Transaction</td><td>id (PK), customerId (FK), amount, type, confirmed</td><td>1 → 0..1 ScanConfirmation</td></tr>
  <tr><td>Settings</td><td>id (PK), notifications, pinEnabled, lastBackup</td><td>Standalone</td></tr>
  <tr><td>BusinessProfile</td><td>businessName, ownerName, phone, gstNumber</td><td>Standalone</td></tr>
  <tr><td>ScanConfirmation</td><td>scanId (PK), txnId (FK), trustLevel, payloadHash</td><td>Belongs to Transaction</td></tr>
</table>
</div>

<!-- ═══════════════════ 12. PROJECT STRUCTURE ═══════════════════ -->
<div class="page">
<h1>12. Project Structure</h1>
<div class="code">app/src/main/java/com/nammasanthe/ledger/
├── MainActivity.kt                 # Entry point, Navigation host
├── NammaSantheApp.kt               # Application class
│
├── data/
│   ├── database/AppDatabase.kt     # Room database definition
│   ├── entity/                     # Customer, TxnEntity, Confirmation
│   └── dao/                        # CustomerDao, TransactionDao
│
├── repository/
│   ├── LedgerRepository.kt         # Main data operations (CRUD)
│   └── DataExportManager.kt        # PDF export + file sharing
│
├── viewmodel/
│   ├── LedgerViewModel.kt          # Ledger screen business logic
│   ├── ProfileViewModel.kt         # Profile, auth, settings
│   ├── ConfirmationViewModel.kt    # QR generation and scanning
│   └── OcrViewModel.kt             # OCR processing pipeline
│
├── ui/
│   ├── screens/                    # HomeScreen, CustomerScreen, etc.
│   ├── components/                 # Reusable Compose components
│   ├── nav/Routes.kt              # Navigation route constants
│   └── theme/                     # Colors, Typography, Theme
│
├── sync/
│   ├── FirebaseAuthManager.kt     # Sign up, Sign in, Auth state
│   └── FirebaseSyncManager.kt     # Cloud sync (Firestore)
│
├── security/
│   ├── QrGenerator.kt             # QR code generation + signing
│   ├── QrValidator.kt             # QR validation + trust assessment
│   └── QrScannerAnalyzer.kt       # CameraX QR scanning
│
├── ocr/
│   ├── OcrPipeline.kt             # Orchestrates ML Kit + Tesseract
│   ├── MlKitOcr.kt                # Google ML Kit text recognition
│   ├── TesseractOcr.kt            # Tesseract for Kannada script
│   ├── BillParser.kt              # Multi-line bill parsing
│   ├── BillLineParser.kt          # Single-line item extraction
│   ├── LineTokenizer.kt           # Token-based line analysis
│   └── GroceryDictionary.kt       # 150+ item multilingual dictionary
│
├── gemini/
│   ├── GeminiService.kt           # Gemini API for advanced OCR
│   └── GeminiSettingsStore.kt     # API key + settings storage
│
└── util/
    ├── PhotoManager.kt            # Photo capture + compression
    └── PdfGenerator.kt            # iText7 PDF generation</div>

<h1 style="margin-top:30px;">13. Technology Stack</h1>
<table>
  <tr><th>Layer</th><th>Technology</th><th>Purpose</th></tr>
  <tr><td>UI Framework</td><td>Jetpack Compose</td><td>Modern declarative Android UI</td></tr>
  <tr><td>State Management</td><td>Kotlin StateFlow</td><td>Reactive UI state updates</td></tr>
  <tr><td>Architecture</td><td>MVVM + Repository</td><td>Clean separation of concerns</td></tr>
  <tr><td>Local Database</td><td>Room (SQLite)</td><td>Offline-first data persistence</td></tr>
  <tr><td>OCR Engine</td><td>Tesseract + ML Kit</td><td>Dual-engine text recognition</td></tr>
  <tr><td>AI/ML</td><td>Gemini 2.5 Flash</td><td>Advanced OCR + translation</td></tr>
  <tr><td>QR Codes</td><td>ZXing</td><td>QR generation and scanning</td></tr>
  <tr><td>PDF Export</td><td>iText7</td><td>Invoice/report generation</td></tr>
  <tr><td>Auth</td><td>Firebase Auth</td><td>Email/password authentication</td></tr>
  <tr><td>Cloud Sync</td><td>Firestore</td><td>Real-time cloud database</td></tr>
  <tr><td>Camera</td><td>CameraX</td><td>Photo capture + QR scanning</td></tr>
  <tr><td>Image Loading</td><td>Coil</td><td>Async image loading + caching</td></tr>
</table>

<footer style="margin-top:40px;"><br>Documentation generated for academic and project evaluation purposes.<br>© 2026 Namma-Santhe Ledger Project</footer>
</div>

</body></html>`;

(async () => {
  try {
    console.log('Launching browser...');
    const browser = await puppeteer.launch({
      headless: true,
      args: ['--no-sandbox', '--disable-setuid-sandbox', '--disable-gpu', '--disable-dev-shm-usage']
    });
    const page = await browser.newPage();
    console.log('Setting content...');
    await page.setContent(html, { waitUntil: 'networkidle0', timeout: 30000 });
    const outPath = path.join(__dirname, '..', 'Namma_Santhe_Technical_Documentation.pdf');
    console.log('Generating PDF...');
    await page.pdf({
      path: outPath,
      format: 'A4',
      printBackground: true,
      margin: { top: '10mm', bottom: '12mm', left: '10mm', right: '10mm' },
      displayHeaderFooter: true,
      headerTemplate: '<div></div>',
      footerTemplate: '<div style="font-size:8px;width:100%;text-align:center;color:#94a3b8;padding:4px;">Page <span class="pageNumber"></span> of <span class="totalPages"></span></div>',
      preferCSSPageSize: false
    });
    await browser.close();
    console.log('PDF generated: ' + outPath);
  } catch (err) {
    console.error('Error:', err.message);
    process.exit(1);
  }
})();
