import Dexie, { Table } from 'dexie';
import { Customer, Transaction, AppSettings } from '../types';

export class NammaSantheDB extends Dexie {
  customers!: Table<Customer>;
  transactions!: Table<Transaction>;
  settings!: Table<AppSettings>;

  constructor() {
    super('NammaSantheDB');

    this.version(1).stores({
      customers: '++id, name, phone, createdAt',
      transactions: '++id, customerId, type, date, createdAt',
      settings: 'id'
    });
  }
}

export const db = new NammaSantheDB();

// Initialize default settings
export const initializeSettings = async () => {
  const existing = await db.settings.get(1);
  if (!existing) {
    await db.settings.add({
      id: 1,
      notificationsEnabled: true,
      dailySummaryTime: '20:00',
      overdueThreshold: 500,
      overdueDays: 2,
      pinEnabled: false,
      biometricEnabled: false
    });
  }
};
