const fs = require('fs');
const path = require('path');
const puppeteer = require('puppeteer');
const { Document, Packer, Paragraph, TextRun, HeadingLevel, Table, TableRow, TableCell, BorderStyle, WidthType, AlignmentType, UnderlineType } = require('docx');

const basePath = path.join(__dirname, '..', 'android', 'app', 'src', 'main', 'java', 'com', 'nammasanthe', 'ledger');

const sections = [
    {
        title: "1. Data Models (Kotlin Data Classes)",
        description: "All application entities are represented as Kotlin data classes. These map directly to the Room database and Firestore documents.",
        subsections: [
            {
                title: "1.1 Core Entities",
                files: [
                    { name: "Customer.kt", path: path.join(basePath, 'data', 'entity', 'Customer.kt') },
                    { name: "Transaction.kt", path: path.join(basePath, 'data', 'entity', 'Transaction.kt') },
                    { name: "ScanConfirmation.kt", path: path.join(basePath, 'data', 'entity', 'ScanConfirmation.kt') }
                ]
            }
        ]
    },
    {
        title: "2. Database Layer (Room & DAO)",
        description: "Room Database implementation for offline-first capabilities, handling all local CRUD operations.",
        subsections: [
            {
                title: "2.1 Room Database & DAOs",
                files: [
                    { name: "AppDatabase.kt", path: path.join(basePath, 'data', 'database', 'AppDatabase.kt') },
                    { name: "CustomerDao.kt", path: path.join(basePath, 'data', 'dao', 'CustomerDao.kt') },
                    { name: "TransactionDao.kt", path: path.join(basePath, 'data', 'dao', 'TransactionDao.kt') }
                ]
            }
        ]
    },
    {
        title: "3. OCR Pipeline & Document Scanning",
        description: "Dual-engine OCR implementation using Gemini ML and Tesseract for Kannada text parsing.",
        subsections: [
            {
                title: "3.1 OCR Orchestrator & Parsers",
                files: [
                    { name: "OcrPipeline.kt", path: path.join(basePath, 'ocr', 'OcrPipeline.kt') },
                    { name: "BillParser.kt", path: path.join(basePath, 'ocr', 'BillParser.kt') }
                ]
            }
        ]
    },
    {
        title: "4. Security & Synchronization",
        description: "QR-based verification system and Firebase integration for cloud sync.",
        subsections: [
            {
                title: "4.1 QR Generator & Sync Manager",
                files: [
                    { name: "QrGenerator.kt", path: path.join(basePath, 'security', 'QrGenerator.kt') },
                    { name: "FirebaseSyncManager.kt", path: path.join(basePath, 'sync', 'FirebaseSyncManager.kt') }
                ]
            }
        ]
    },
    {
        title: "5. UI Architecture (ViewModels)",
        description: "MVVM implementation using Jetpack Compose StateFlow for reactive UI.",
        subsections: [
            {
                title: "5.1 Core ViewModels",
                files: [
                    { name: "LedgerViewModel.kt", path: path.join(basePath, 'viewmodel', 'LedgerViewModel.kt') },
                    { name: "OcrViewModel.kt", path: path.join(basePath, 'viewmodel', 'OcrViewModel.kt') }
                ]
            }
        ]
    }
];

// Helper to escape HTML
function escapeHtml(unsafe) {
    return (unsafe || '').replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
}

async function generatePDF(outPath) {
    console.log('Generating PDF...');
    let html = `<!DOCTYPE html><html><head><meta charset="utf-8"><style>
        body { font-family: 'Times New Roman', serif; font-size: 14px; margin: 40px; }
        .header { text-align: left; font-size: 14px; border-bottom: 2px solid #000; padding-bottom: 5px; margin-bottom: 30px; color: #555; }
        .header-title { text-decoration: underline; color: #cc0000; }
        h1 { font-size: 18px; font-weight: bold; margin-top: 30px; margin-bottom: 15px; }
        h2 { font-size: 16px; font-weight: bold; margin-top: 25px; margin-bottom: 10px; }
        h3 { font-size: 15px; font-weight: bold; margin-top: 20px; margin-bottom: 10px; }
        p { margin-bottom: 15px; line-height: 1.5; }
        .code-container { border: 1px solid #70AD47; padding: 10px; margin-bottom: 20px; page-break-inside: avoid; }
        .code-container pre { font-family: 'Courier New', Courier, monospace; font-size: 11px; white-space: pre-wrap; margin: 0; }
    </style></head><body>`;

    html += `<div class="header"><span class="header-title">Namma-Santhe Ledger App</span></div>`;
    html += `<h1>Code:</h1>`;

    for (const sec of sections) {
        html += `<h2>${sec.title}</h2>`;
        html += `<p>${sec.description}</p>`;
        
        for (const sub of sec.subsections) {
            html += `<h3>${sub.title}</h3>`;
            for (const file of sub.files) {
                if (fs.existsSync(file.path)) {
                    const content = fs.readFileSync(file.path, 'utf8');
                    html += `<div class="code-container"><pre>// ${file.name}\n${escapeHtml(content)}</pre></div>`;
                } else {
                    html += `<div class="code-container"><pre>// ${file.name} not found</pre></div>`;
                }
            }
        }
    }

    html += `</body></html>`;

    const browser = await puppeteer.launch({ headless: true, args: ['--no-sandbox'] });
    const page = await browser.newPage();
    await page.setContent(html, { waitUntil: 'networkidle0' });
    await page.pdf({
        path: outPath,
        format: 'A4',
        margin: { top: '20mm', bottom: '20mm', left: '20mm', right: '20mm' },
        displayHeaderFooter: true,
        headerTemplate: '<div style="font-size:10px; width:100%; text-align:left; padding-left: 20mm; color: #cc0000; text-decoration: underline; border-bottom: 1px solid #000; padding-bottom: 5px;">Namma-Santhe Ledger App</div>',
        footerTemplate: '<div style="font-size:10px; width:100%; text-align:center;">Page <span class="pageNumber"></span></div>'
    });
    await browser.close();
    console.log('PDF Generated:', outPath);
}

async function generateDocx(outPath) {
    console.log('Generating DOCX...');
    const docChildren = [];

    // Header paragraph
    docChildren.push(new Paragraph({
        children: [
            new TextRun({
                text: "Namma-Santhe Ledger App",
                color: "CC0000",
                underline: { type: UnderlineType.SINGLE }
            })
        ],
        border: { bottom: { color: "000000", space: 1, style: BorderStyle.SINGLE, size: 6 } },
        spacing: { after: 400 }
    }));

    docChildren.push(new Paragraph({
        text: "Code:",
        heading: HeadingLevel.HEADING_1,
        spacing: { before: 400, after: 200 }
    }));

    for (const sec of sections) {
        docChildren.push(new Paragraph({
            text: sec.title,
            heading: HeadingLevel.HEADING_2,
            spacing: { before: 300, after: 100 }
        }));
        docChildren.push(new Paragraph({
            text: sec.description,
            spacing: { after: 200 }
        }));

        for (const sub of sec.subsections) {
            docChildren.push(new Paragraph({
                text: sub.title,
                heading: HeadingLevel.HEADING_3,
                spacing: { before: 200, after: 100 }
            }));

            for (const file of sub.files) {
                let codeText = `// ${file.name}\n`;
                if (fs.existsSync(file.path)) {
                    codeText += fs.readFileSync(file.path, 'utf8');
                } else {
                    codeText += "File not found.";
                }

                // Add Table for code block to mimic border
                const table = new Table({
                    width: { size: 100, type: WidthType.PERCENTAGE },
                    borders: {
                        top: { style: BorderStyle.SINGLE, size: 4, color: "70AD47" },
                        bottom: { style: BorderStyle.SINGLE, size: 4, color: "70AD47" },
                        left: { style: BorderStyle.SINGLE, size: 4, color: "70AD47" },
                        right: { style: BorderStyle.SINGLE, size: 4, color: "70AD47" }
                    },
                    rows: [
                        new TableRow({
                            children: [
                                new TableCell({
                                    children: codeText.split('\\n').map(line => new Paragraph({
                                        children: [new TextRun({ text: line, font: "Courier New", size: 18 })], // size is half-points, 18 = 9pt
                                        spacing: { after: 0, before: 0, line: 240 } // line spacing
                                    })),
                                    margins: { top: 100, bottom: 100, left: 100, right: 100 }
                                })
                            ]
                        })
                    ]
                });
                
                docChildren.push(table);
                docChildren.push(new Paragraph({ text: "", spacing: { after: 200 } })); // Spacing after table
            }
        }
    }

    const doc = new Document({
        sections: [{
            properties: {},
            children: docChildren
        }]
    });

    const buffer = await Packer.toBuffer(doc);
    fs.writeFileSync(outPath, buffer);
    console.log('DOCX Generated:', outPath);
}

(async () => {
    try {
        const pdfPath = path.join(__dirname, '..', 'Namma_Santhe_Code_Snippets.pdf');
        const docxPath = path.join(__dirname, '..', 'Namma_Santhe_Code_Snippets.docx');
        
        await generatePDF(pdfPath);
        await generateDocx(docxPath);
        
        console.log('Successfully generated both Word and PDF snippet documents!');
    } catch (e) {
        console.error('Error:', e);
    }
})();
