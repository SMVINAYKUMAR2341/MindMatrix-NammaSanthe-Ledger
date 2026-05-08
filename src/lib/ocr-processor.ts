import Tesseract from 'tesseract.js';

export interface ExtractedInvoiceData {
  items: Array<{ name: string; amount: number }>;
  total: number;
  rawText: string;
}

export const processImageForOCR = async (
  imageFile: File | Blob,
  onProgress?: (progress: number) => void
): Promise<ExtractedInvoiceData> => {
  try {
    const result = await Tesseract.recognize(imageFile, 'eng+kan', {
      logger: (m) => {
        if (m.status === 'recognizing text' && onProgress) {
          onProgress(Math.round(m.progress * 100));
        }
      }
    });

    const text = result.data.text;
    return parseInvoiceText(text);
  } catch (error) {
    console.error('OCR Error:', error);
    throw new Error('Failed to process image');
  }
};

const parseInvoiceText = (text: string): ExtractedInvoiceData => {
  const lines = text.split('\n').filter(line => line.trim());
  const items: Array<{ name: string; amount: number }> = [];
  let total = 0;

  // Pattern to match: item name followed by amount (₹500, Rs 500, 500, etc.)
  const amountPattern = /(?:₹|Rs\.?|INR)?\s*(\d+(?:\.\d{2})?)/i;
  const totalPattern = /total|grand\s*total|amount|sum/i;

  for (const line of lines) {
    const match = line.match(amountPattern);
    if (!match) continue;

    const amount = parseFloat(match[1]);
    const name = line.replace(match[0], '').trim();

    if (totalPattern.test(name)) {
      total = amount;
    } else if (name && amount > 0) {
      items.push({ name, amount });
    }
  }

  // If no total found, calculate from items
  if (total === 0 && items.length > 0) {
    total = items.reduce((sum, item) => sum + item.amount, 0);
  }

  return {
    items,
    total,
    rawText: text
  };
};

// Compress image before saving
export const compressImage = async (
  file: File,
  maxWidth: number = 400,
  quality: number = 0.8
): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        const canvas = document.createElement('canvas');
        let width = img.width;
        let height = img.height;

        if (width > maxWidth) {
          height = (height * maxWidth) / width;
          width = maxWidth;
        }

        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext('2d')!;
        ctx.drawImage(img, 0, 0, width, height);

        resolve(canvas.toDataURL('image/jpeg', quality));
      };
      img.onerror = reject;
      img.src = e.target?.result as string;
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
};
