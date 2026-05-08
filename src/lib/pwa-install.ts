// PWA installation and service worker registration
export const registerServiceWorker = async () => {
  // Service worker disabled in Figma Make environment
  // Enable this in production deployment
  if ('serviceWorker' in navigator && false) {
    try {
      const registration = await navigator.serviceWorker.register('/sw.js');
      console.log('Service Worker registered:', registration);
    } catch (error) {
      console.error('Service Worker registration failed:', error);
    }
  }
};

// Request notification permission
export const requestNotificationPermission = async (): Promise<boolean> => {
  if (!('Notification' in window)) {
    return false;
  }

  if (Notification.permission === 'granted') {
    return true;
  }

  if (Notification.permission !== 'denied') {
    const permission = await Notification.requestPermission();
    return permission === 'granted';
  }

  return false;
};

// Show notification
export const showNotification = (title: string, options?: NotificationOptions) => {
  if (Notification.permission === 'granted') {
    new Notification(title, {
      icon: '/icon-192.png',
      badge: '/icon-192.png',
      ...options
    });
  }
};

// Daily summary notification (would be triggered by WorkManager in real Android app)
export const scheduleDailySummary = async (totalOutstanding: number, transactionCount: number) => {
  await showNotification('Namma Santhe - Daily Summary', {
    body: `Today: ${transactionCount} transactions. Outstanding: ₹${totalOutstanding.toFixed(0)}`,
    tag: 'daily-summary',
    requireInteraction: false
  });
};
