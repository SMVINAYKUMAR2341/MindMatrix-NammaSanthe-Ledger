export interface Customer {
  id?: number;
  name: string;
  phone: string;
  photoPath?: string;
  createdAt: number;
}

export interface Transaction {
  id?: number;
  customerId: number;
  type: 'credit' | 'payment';
  amount: number;
  note?: string;
  date: number;
  createdAt: number;
}

export interface CustomerBalance {
  customerId: number;
  customerName: string;
  customerPhone: string;
  customerPhoto?: string;
  totalCredit: number;
  totalPayment: number;
  balance: number;
  lastTransactionDate?: number;
}

export interface DailySummary {
  date: string;
  totalCredit: number;
  totalPayment: number;
  transactionCount: number;
}

export interface AppSettings {
  id: number;
  notificationsEnabled: boolean;
  dailySummaryTime: string;
  overdueThreshold: number;
  overdueDays: number;
  pinEnabled: boolean;
  pinHash?: string;
  biometricEnabled: boolean;
  lastBackup?: number;
}
