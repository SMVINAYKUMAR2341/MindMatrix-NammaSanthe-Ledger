const { mdToPdf } = require('md-to-pdf');
const path = require('path');

(async () => {
  try {
    const pdf = await mdToPdf(
      { path: path.join(__dirname, 'TECHNICAL_REPORT.md') },
      {
        dest: path.join(__dirname, 'Namma_Santhe_Technical_Documentation.pdf'),
        launch_options: { args: ['--no-sandbox', '--disable-setuid-sandbox'] },
        pdf_options: {
          format: 'A4',
          margin: { top: '25mm', bottom: '25mm', left: '20mm', right: '20mm' },
          printBackground: true,
        },
        stylesheet: path.join(__dirname, 'style.css'),
      }
    );
    console.log('PDF created successfully!');
    console.log('Output:', pdf.filename);
  } catch (err) {
    console.error('Error:', err.message);
  }
})();
