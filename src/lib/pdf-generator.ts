import jsPDF from 'jspdf';
import { Customer, Transaction } from '../types';

export const generateInvoicePDF = (
  customer: Customer,
  transactions: Transaction[],
  balance: number
): Blob => {
  const doc = new jsPDF();

  // Brand color
  const primaryColor: [number, number, number] = [249, 115, 22]; // Orange
  const secondaryColor: [number, number, number] = [239, 68, 68]; // Red

  // Header with colored background
  doc.setFillColor(...primaryColor);
  doc.rect(0, 0, 210, 40, 'F');

  // Company name
  doc.setTextColor(255, 255, 255);
  doc.setFontSize(24);
  doc.setFont('helvetica', 'bold');
  doc.text('Namma Santhe', 20, 20);

  doc.setFontSize(10);
  doc.setFont('helvetica', 'normal');
  doc.text('Your trusted digital khata', 20, 28);

  // Invoice title
  doc.setFontSize(16);
  doc.setFont('helvetica', 'bold');
  doc.text('TAX INVOICE', 150, 20);

  // Invoice number and date
  doc.setFontSize(9);
  doc.setFont('helvetica', 'normal');
  const invoiceNo = `INV-${Date.now().toString().slice(-8)}`;
  doc.text(`Invoice No: ${invoiceNo}`, 150, 28);
  doc.text(`Date: ${new Date().toLocaleDateString('en-IN')}`, 150, 34);

  // Reset text color
  doc.setTextColor(0, 0, 0);

  // Bill To section
  doc.setFontSize(10);
  doc.setFont('helvetica', 'bold');
  doc.text('BILL TO:', 20, 55);

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(11);
  doc.text(customer.name, 20, 62);
  if (customer.phone) {
    doc.setFontSize(9);
    doc.text(`Phone: ${customer.phone}`, 20, 68);
  }

  // From section
  doc.setFontSize(10);
  doc.setFont('helvetica', 'bold');
  doc.text('FROM:', 150, 55);

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9);
  doc.text('Your Shop Name', 150, 62);
  doc.text('Village Market', 150, 68);

  // Table header background
  let y = 85;
  doc.setFillColor(245, 245, 245);
  doc.rect(15, y - 5, 180, 10, 'F');

  // Table headers
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(9);
  doc.setTextColor(60, 60, 60);
  doc.text('DATE', 20, y);
  doc.text('DESCRIPTION', 55, y);
  doc.text('TYPE', 115, y);
  doc.text('AMOUNT', 145, y, { align: 'right' });
  doc.text('BALANCE', 180, y, { align: 'right' });

  // Reset text color
  doc.setTextColor(0, 0, 0);

  // Table line
  doc.setDrawColor(200, 200, 200);
  doc.line(15, y + 2, 195, y + 2);

  // Transactions
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9);
  y += 10;

  let runningBalance = 0;
  transactions.forEach((txn, index) => {
    if (y > 260) {
      doc.addPage();
      y = 20;
    }

    // Alternate row colors
    if (index % 2 === 0) {
      doc.setFillColor(250, 250, 250);
      doc.rect(15, y - 4, 180, 8, 'F');
    }

    runningBalance += txn.type === 'credit' ? txn.amount : -txn.amount;

    doc.text(new Date(txn.date).toLocaleDateString('en-IN', { day: '2-digit', month: 'short' }), 20, y);

    const description = txn.note || (txn.type === 'credit' ? 'Credit Transaction' : 'Payment Received');
    doc.text(description.substring(0, 30), 55, y);

    // Type badge
    if (txn.type === 'credit') {
      doc.setTextColor(...secondaryColor);
      doc.text('CREDIT', 115, y);
    } else {
      doc.setTextColor(34, 197, 94);
      doc.text('PAYMENT', 115, y);
    }
    doc.setTextColor(0, 0, 0);

    doc.text(`₹${txn.amount.toFixed(2)}`, 145, y, { align: 'right' });

    // Balance color coding
    if (runningBalance > 0) {
      doc.setTextColor(...secondaryColor);
    } else if (runningBalance < 0) {
      doc.setTextColor(34, 197, 94);
    }
    doc.text(`₹${Math.abs(runningBalance).toFixed(2)}`, 180, y, { align: 'right' });
    doc.setTextColor(0, 0, 0);

    y += 8;
  });

  // Summary section
  y += 5;
  doc.setDrawColor(200, 200, 200);
  doc.line(15, y, 195, y);

  y += 10;
  doc.setFillColor(249, 250, 251);
  doc.rect(120, y - 5, 75, 25, 'F');

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(10);
  doc.text('Subtotal:', 125, y);

  const totalCredit = transactions.filter(t => t.type === 'credit').reduce((s, t) => s + t.amount, 0);
  const totalPayment = transactions.filter(t => t.type === 'payment').reduce((s, t) => s + t.amount, 0);

  doc.text(`₹${totalCredit.toFixed(2)}`, 185, y, { align: 'right' });

  y += 7;
  doc.text('Payments:', 125, y);
  doc.setTextColor(34, 197, 94);
  doc.text(`-₹${totalPayment.toFixed(2)}`, 185, y, { align: 'right' });
  doc.setTextColor(0, 0, 0);

  y += 2;
  doc.setDrawColor(...primaryColor);
  doc.setLineWidth(0.5);
  doc.line(125, y, 190, y);

  y += 7;
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(12);
  doc.text('Outstanding:', 125, y);

  if (balance > 0) {
    doc.setTextColor(...secondaryColor);
    doc.text(`₹${balance.toFixed(2)}`, 185, y, { align: 'right' });
  } else if (balance < 0) {
    doc.setTextColor(34, 197, 94);
    doc.text(`-₹${Math.abs(balance).toFixed(2)}`, 185, y, { align: 'right' });
  } else {
    doc.setTextColor(100, 100, 100);
    doc.text('₹0.00', 185, y, { align: 'right' });
  }

  // Footer
  doc.setTextColor(100, 100, 100);
  doc.setFont('helvetica', 'italic');
  doc.setFontSize(8);
  doc.text('This is a computer-generated invoice and does not require a signature', 105, 280, { align: 'center' });
  doc.text('Generated by Namma Santhe Ledger | namma-santhe.app', 105, 285, { align: 'center' });

  // Decorative elements
  doc.setDrawColor(...primaryColor);
  doc.setLineWidth(2);
  doc.line(0, 295, 210, 295);

  return doc.output('blob');
};

export const downloadPDF = (blob: Blob, filename: string) => {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
};

export const sharePDFViaWhatsApp = async (blob: Blob, customerName: string) => {
  if (navigator.share && navigator.canShare({ files: [new File([blob], 'invoice.pdf')] })) {
    const file = new File([blob], `${customerName}_invoice.pdf`, { type: 'application/pdf' });
    await navigator.share({
      files: [file],
      title: `Invoice for ${customerName}`,
      text: `Ledger statement for ${customerName}`
    });
  } else {
    // Fallback: download
    downloadPDF(blob, `${customerName}_invoice.pdf`);
  }
};
