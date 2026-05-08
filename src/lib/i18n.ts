import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

const resources = {
  en: {
    translation: {
      // App
      appName: 'Namma Santhe Ledger',

      // Navigation
      home: 'Home',
      customers: 'Customers',
      quick: 'Quick',
      scan: 'Scan',
      reports: 'Reports',

      // Home Dashboard
      totalOutstanding: 'Total Outstanding',
      todayCredit: "Today's Credit",
      todayPayment: "Today's Payment",
      recentTransactions: 'Recent Transactions',
      transactions: 'transactions',
      quickEntry: 'Quick Entry',

      // Customers
      searchCustomers: 'Search customers...',
      addCustomer: 'Add Customer',
      noCustomers: 'No customers yet. Add one to get started!',
      noCustomersFound: 'No customers found',
      customerName: 'Customer Name',
      phone: 'Phone',
      pending: 'pending',
      advance: 'advance',
      clear: 'clear',

      // Ledger
      creditGiven: 'Credit Given',
      paymentReceived: 'Payment Received',
      addTransaction: 'Add Transaction',
      noTransactions: 'No transactions yet',
      credit: 'Credit',
      udari: 'Udari',
      payment: 'Payment',
      amount: 'Amount',
      note: 'Note',
      optional: 'optional',
      enterAmount: 'Enter amount',
      addNote: 'Add a note',

      // Profile
      profile: 'Profile',
      customerDetails: 'Customer Details',
      totalCreditGiven: 'Total Credit Given',
      totalPaymentsReceived: 'Total Payments Received',
      balance: 'Balance',
      lastTransaction: 'Last Transaction',
      editCustomer: 'Edit Customer',
      deleteCustomer: 'Delete Customer',

      // Quick Entry
      voiceInput: 'Voice Input',
      save: 'Save',
      clear: 'Clear',

      // Reports
      dailySales: 'Daily Sales',
      salesTrend: 'Sales Trend',
      outstandingVsCollected: 'Outstanding vs Collected',
      summary: 'Summary',
      days: 'days',
      totalTransactions: 'Total Transactions',
      totalCreditGivenPeriod: 'Total Credit Given',
      totalPaymentsReceivedPeriod: 'Total Payments Received',
      outstanding: 'Outstanding',
      collected: 'Collected',

      // OCR Scanner
      scanInvoice: 'Scan Invoice',
      captureBill: 'Capture Bill',
      camera: 'Camera',
      gallery: 'Gallery',
      processingImage: 'Processing image...',
      extractedData: 'Extracted Data',
      items: 'Items',
      totalAmount: 'Total Amount',
      viewRawText: 'View raw OCR text',
      selectCustomer: 'Select Customer',
      saveCredit: 'Save Credit',
      cancel: 'Cancel',

      // Messages
      enterValidAmount: 'Enter valid amount',
      selectOrEnterCustomer: 'Select or enter customer name',
      transactionSaved: 'Transaction saved!',
      customerAdded: 'Customer added successfully',

      // Invoice
      invoice: 'Invoice',
      invoiceNumber: 'Invoice No',
      date: 'Date',
      billTo: 'Bill To',
      description: 'Description',
      qty: 'Qty',
      rate: 'Rate',
      total: 'Total',
      subtotal: 'Subtotal',
      grandTotal: 'Grand Total',

      // Common
      back: 'Back',
      add: 'Add',
      edit: 'Edit',
      delete: 'Delete',
      close: 'Close',
      confirm: 'Confirm',
      type: 'Type',
      at: 'at'
    }
  },
  hi: {
    translation: {
      appName: 'नम्मा संथे खाता',

      home: 'होम',
      customers: 'ग्राहक',
      quick: 'तेज़',
      scan: 'स्कैन',
      reports: 'रिपोर्ट',

      totalOutstanding: 'कुल बाकी',
      todayCredit: 'आज का उधार',
      todayPayment: 'आज का भुगतान',
      recentTransactions: 'हाल के लेनदेन',
      transactions: 'लेनदेन',
      quickEntry: 'तेज़ एंट्री',

      searchCustomers: 'ग्राहक खोजें...',
      addCustomer: 'ग्राहक जोड़ें',
      noCustomers: 'अभी तक कोई ग्राहक नहीं। शुरू करने के लिए एक जोड़ें!',
      noCustomersFound: 'कोई ग्राहक नहीं मिला',
      customerName: 'ग्राहक का नाम',
      phone: 'फ़ोन',
      pending: 'बाकी',
      advance: 'अग्रिम',
      clear: 'साफ़',

      creditGiven: 'उधार दिया',
      paymentReceived: 'भुगतान मिला',
      addTransaction: 'लेनदेन जोड़ें',
      noTransactions: 'अभी तक कोई लेनदेन नहीं',
      credit: 'उधार',
      udari: 'उधारी',
      payment: 'भुगतान',
      amount: 'राशि',
      note: 'नोट',
      optional: 'वैकल्पिक',
      enterAmount: 'राशि दर्ज करें',
      addNote: 'एक नोट जोड़ें',

      profile: 'प्रोफ़ाइल',
      customerDetails: 'ग्राहक विवरण',
      totalCreditGiven: 'कुल उधार दिया',
      totalPaymentsReceived: 'कुल भुगतान प्राप्त',
      balance: 'शेष',
      lastTransaction: 'अंतिम लेनदेन',
      editCustomer: 'ग्राहक संपादित करें',
      deleteCustomer: 'ग्राहक हटाएं',

      voiceInput: 'आवाज़ इनपुट',
      save: 'सहेजें',
      clear: 'साफ़ करें',

      dailySales: 'दैनिक बिक्री',
      salesTrend: 'बिक्री प्रवृत्ति',
      outstandingVsCollected: 'बकाया बनाम एकत्रित',
      summary: 'सारांश',
      days: 'दिन',
      totalTransactions: 'कुल लेनदेन',
      totalCreditGivenPeriod: 'कुल उधार दिया',
      totalPaymentsReceivedPeriod: 'कुल भुगतान प्राप्त',
      outstanding: 'बकाया',
      collected: 'एकत्रित',

      scanInvoice: 'चालान स्कैन करें',
      captureBill: 'बिल कैप्चर करें',
      camera: 'कैमरा',
      gallery: 'गैलरी',
      processingImage: 'छवि संसाधित हो रही है...',
      extractedData: 'निकाला गया डेटा',
      items: 'आइटम',
      totalAmount: 'कुल राशि',
      viewRawText: 'कच्चा OCR टेक्स्ट देखें',
      selectCustomer: 'ग्राहक चुनें',
      saveCredit: 'उधार सहेजें',
      cancel: 'रद्द करें',

      enterValidAmount: 'मान्य राशि दर्ज करें',
      selectOrEnterCustomer: 'ग्राहक का नाम चुनें या दर्ज करें',
      transactionSaved: 'लेनदेन सहेजा गया!',
      customerAdded: 'ग्राहक सफलतापूर्वक जोड़ा गया',

      invoice: 'चालान',
      invoiceNumber: 'चालान नंबर',
      date: 'तारीख',
      billTo: 'बिल प्राप्तकर्ता',
      description: 'विवरण',
      qty: 'मात्रा',
      rate: 'दर',
      total: 'कुल',
      subtotal: 'उप-योग',
      grandTotal: 'कुल योग',

      back: 'पीछे',
      add: 'जोड़ें',
      edit: 'संपादित करें',
      delete: 'हटाएं',
      close: 'बंद करें',
      confirm: 'पुष्टि करें',
      type: 'प्रकार',
      at: 'पर'
    }
  },
  kn: {
    translation: {
      appName: 'ನಮ್ಮ ಸಂತೆ ಖಾತೆ',

      home: 'ಮುಖಪುಟ',
      customers: 'ಗ್ರಾಹಕರು',
      quick: 'ತ್ವರಿತ',
      scan: 'ಸ್ಕ್ಯಾನ್',
      reports: 'ವರದಿಗಳು',

      totalOutstanding: 'ಒಟ್ಟು ಬಾಕಿ',
      todayCredit: 'ಇಂದಿನ ಸಾಲ',
      todayPayment: 'ಇಂದಿನ ಪಾವತಿ',
      recentTransactions: 'ಇತ್ತೀಚಿನ ವಹಿವಾಟುಗಳು',
      transactions: 'ವಹಿವಾಟುಗಳು',
      quickEntry: 'ತ್ವರಿತ ನಮೂದು',

      searchCustomers: 'ಗ್ರಾಹಕರನ್ನು ಹುಡುಕಿ...',
      addCustomer: 'ಗ್ರಾಹಕರನ್ನು ಸೇರಿಸಿ',
      noCustomers: 'ಇನ್ನೂ ಯಾವುದೇ ಗ್ರಾಹಕರಿಲ್ಲ. ಪ್ರಾರಂಭಿಸಲು ಒಬ್ಬರನ್ನು ಸೇರಿಸಿ!',
      noCustomersFound: 'ಯಾವುದೇ ಗ್ರಾಹಕರು ಸಿಗಲಿಲ್ಲ',
      customerName: 'ಗ್ರಾಹಕರ ಹೆಸರು',
      phone: 'ದೂರವಾಣಿ',
      pending: 'ಬಾಕಿ',
      advance: 'ಮುಂಗಡ',
      clear: 'ತೆರವು',

      creditGiven: 'ಸಾಲ ನೀಡಲಾಗಿದೆ',
      paymentReceived: 'ಪಾವತಿ ಸ್ವೀಕರಿಸಲಾಗಿದೆ',
      addTransaction: 'ವಹಿವಾಟು ಸೇರಿಸಿ',
      noTransactions: 'ಇನ್ನೂ ಯಾವುದೇ ವಹಿವಾಟುಗಳಿಲ್ಲ',
      credit: 'ಸಾಲ',
      udari: 'ಉಧಾರಿ',
      payment: 'ಪಾವತಿ',
      amount: 'ಮೊತ್ತ',
      note: 'ಟಿಪ್ಪಣಿ',
      optional: 'ಐಚ್ಛಿಕ',
      enterAmount: 'ಮೊತ್ತ ನಮೂದಿಸಿ',
      addNote: 'ಟಿಪ್ಪಣಿ ಸೇರಿಸಿ',

      profile: 'ಪ್ರೊಫೈಲ್',
      customerDetails: 'ಗ್ರಾಹಕರ ವಿವರಗಳು',
      totalCreditGiven: 'ಒಟ್ಟು ಸಾಲ ನೀಡಲಾಗಿದೆ',
      totalPaymentsReceived: 'ಒಟ್ಟು ಪಾವತಿಗಳು ಸ್ವೀಕರಿಸಲಾಗಿದೆ',
      balance: 'ಬಾಕಿ',
      lastTransaction: 'ಕೊನೆಯ ವಹಿವಾಟು',
      editCustomer: 'ಗ್ರಾಹಕರನ್ನು ಸಂಪಾದಿಸಿ',
      deleteCustomer: 'ಗ್ರಾಹಕರನ್ನು ಅಳಿಸಿ',

      voiceInput: 'ಧ್ವನಿ ಇನ್‌ಪುಟ್',
      save: 'ಉಳಿಸಿ',
      clear: 'ತೆರವುಗೊಳಿಸಿ',

      dailySales: 'ದೈನಂದಿನ ಮಾರಾಟ',
      salesTrend: 'ಮಾರಾಟ ಪ್ರವೃತ್ತಿ',
      outstandingVsCollected: 'ಬಾಕಿ ಮತ್ತು ಸಂಗ್ರಹಿಸಿದ',
      summary: 'ಸಾರಾಂಶ',
      days: 'ದಿನಗಳು',
      totalTransactions: 'ಒಟ್ಟು ವಹಿವಾಟುಗಳು',
      totalCreditGivenPeriod: 'ಒಟ್ಟು ಸಾಲ ನೀಡಲಾಗಿದೆ',
      totalPaymentsReceivedPeriod: 'ಒಟ್ಟು ಪಾವತಿಗಳು ಸ್ವೀಕರಿಸಲಾಗಿದೆ',
      outstanding: 'ಬಾಕಿ',
      collected: 'ಸಂಗ್ರಹಿಸಿದ',

      scanInvoice: 'ಬಿಲ್ ಸ್ಕ್ಯಾನ್ ಮಾಡಿ',
      captureBill: 'ಬಿಲ್ ಕ್ಯಾಪ್ಚರ್ ಮಾಡಿ',
      camera: 'ಕ್ಯಾಮೆರಾ',
      gallery: 'ಗ್ಯಾಲರಿ',
      processingImage: 'ಚಿತ್ರವನ್ನು ಸಂಸ್ಕರಿಸಲಾಗುತ್ತಿದೆ...',
      extractedData: 'ಹೊರತೆಗೆದ ಡೇಟಾ',
      items: 'ವಸ್ತುಗಳು',
      totalAmount: 'ಒಟ್ಟು ಮೊತ್ತ',
      viewRawText: 'ಕಚ್ಚಾ OCR ಪಠ್ಯವನ್ನು ನೋಡಿ',
      selectCustomer: 'ಗ್ರಾಹಕರನ್ನು ಆಯ್ಕೆಮಾಡಿ',
      saveCredit: 'ಸಾಲ ಉಳಿಸಿ',
      cancel: 'ರದ್ದುಗೊಳಿಸಿ',

      enterValidAmount: 'ಮಾನ್ಯ ಮೊತ್ತವನ್ನು ನಮೂದಿಸಿ',
      selectOrEnterCustomer: 'ಗ್ರಾಹಕರ ಹೆಸರನ್ನು ಆಯ್ಕೆಮಾಡಿ ಅಥವಾ ನಮೂದಿಸಿ',
      transactionSaved: 'ವಹಿವಾಟು ಉಳಿಸಲಾಗಿದೆ!',
      customerAdded: 'ಗ್ರಾಹಕರನ್ನು ಯಶಸ್ವಿಯಾಗಿ ಸೇರಿಸಲಾಗಿದೆ',

      invoice: 'ಬಿಲ್',
      invoiceNumber: 'ಬಿಲ್ ಸಂಖ್ಯೆ',
      date: 'ದಿನಾಂಕ',
      billTo: 'ಬಿಲ್ ಸ್ವೀಕರಿಸುವವರು',
      description: 'ವಿವರಣೆ',
      qty: 'ಪ್ರಮಾಣ',
      rate: 'ದರ',
      total: 'ಒಟ್ಟು',
      subtotal: 'ಉಪ-ಒಟ್ಟು',
      grandTotal: 'ಮಹಾ ಒಟ್ಟು',

      back: 'ಹಿಂದೆ',
      add: 'ಸೇರಿಸಿ',
      edit: 'ಸಂಪಾದಿಸಿ',
      delete: 'ಅಳಿಸಿ',
      close: 'ಮುಚ್ಚಿ',
      confirm: 'ದೃಢೀಕರಿಸಿ',
      type: 'ಪ್ರಕಾರ',
      at: 'ಸಮಯ'
    }
  }
};

i18n
  .use(initReactI18next)
  .init({
    resources,
    lng: 'en',
    fallbackLng: 'en',
    interpolation: {
      escapeValue: false
    }
  });

export default i18n;
