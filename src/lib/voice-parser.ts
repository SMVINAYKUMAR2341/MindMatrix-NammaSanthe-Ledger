// Simple voice command parser for inputs like "Ramesh 200 credit"
export interface VoiceCommand {
  name: string;
  amount: number;
  type: 'credit' | 'payment';
}

const CREDIT_KEYWORDS = ['credit', 'udari', 'gave', 'lent', 'udhaar'];
const PAYMENT_KEYWORDS = ['payment', 'paid', 'received', 'got'];

export const parseVoiceCommand = (text: string): VoiceCommand | null => {
  const cleanText = text.toLowerCase().trim();

  // Extract amount (any number)
  const amountMatch = cleanText.match(/\d+/);
  if (!amountMatch) return null;
  const amount = parseInt(amountMatch[0]);

  // Determine type
  let type: 'credit' | 'payment' = 'credit';
  if (PAYMENT_KEYWORDS.some(keyword => cleanText.includes(keyword))) {
    type = 'payment';
  } else if (CREDIT_KEYWORDS.some(keyword => cleanText.includes(keyword))) {
    type = 'credit';
  }

  // Extract name (everything before the number)
  const nameMatch = text.match(/^([^\d]+)/);
  if (!nameMatch) return null;

  const name = nameMatch[1]
    .replace(new RegExp(`(${[...CREDIT_KEYWORDS, ...PAYMENT_KEYWORDS].join('|')})`, 'gi'), '')
    .trim();

  if (!name || amount <= 0) return null;

  return { name, amount, type };
};

// Start voice recognition
export const startVoiceRecognition = (
  onResult: (text: string) => void,
  onError?: (error: string) => void
): (() => void) => {
  if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
    onError?.('Voice recognition not supported in this browser');
    return () => {};
  }

  const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
  const recognition = new SpeechRecognition();

  recognition.continuous = false;
  recognition.interimResults = false;
  recognition.lang = 'en-IN'; // Indian English (handles mixed Kannada)

  recognition.onresult = (event: any) => {
    const text = event.results[0][0].transcript;
    onResult(text);
  };

  recognition.onerror = (event: any) => {
    onError?.(event.error);
  };

  recognition.start();

  return () => recognition.stop();
};
