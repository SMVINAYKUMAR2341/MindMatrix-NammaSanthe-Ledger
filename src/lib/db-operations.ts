import { db } from './database';
import { Customer, Transaction, CustomerBalance, DailySummary } from '../types';

// Customer operations
export const addCustomer = async (customer: Omit<Customer, 'id' | 'createdAt'>) => {
  return await db.customers.add({
    ...customer,
    createdAt: Date.now()
  });
};

export const updateCustomer = async (id: number, updates: Partial<Customer>) => {
  return await db.customers.update(id, updates);
};

export const deleteCustomer = async (id: number) => {
  await db.transactions.where('customerId').equals(id).delete();
  return await db.customers.delete(id);
};

export const getCustomer = async (id: number) => {
  return await db.customers.get(id);
};

export const getAllCustomers = async () => {
  return await db.customers.orderBy('name').toArray();
};

export const searchCustomers = async (query: string) => {
  const lowerQuery = query.toLowerCase();
  return await db.customers
    .filter(c =>
      c.name.toLowerCase().includes(lowerQuery) ||
      c.phone.includes(query)
    )
    .toArray();
};

// Transaction operations
export const addTransaction = async (transaction: Omit<Transaction, 'id' | 'createdAt'>) => {
  return await db.transactions.add({
    ...transaction,
    createdAt: Date.now()
  });
};

export const getCustomerTransactions = async (customerId: number) => {
  return await db.transactions
    .where('customerId')
    .equals(customerId)
    .reverse()
    .sortBy('date');
};

export const getAllTransactions = async () => {
  return await db.transactions.orderBy('date').reverse().toArray();
};

// Balance calculations
export const getCustomerBalance = async (customerId: number): Promise<number> => {
  const transactions = await getCustomerTransactions(customerId);
  return transactions.reduce((balance, txn) => {
    return txn.type === 'credit' ? balance + txn.amount : balance - txn.amount;
  }, 0);
};

export const getAllCustomerBalances = async (): Promise<CustomerBalance[]> => {
  const customers = await getAllCustomers();
  const balances: CustomerBalance[] = [];

  for (const customer of customers) {
    const transactions = await getCustomerTransactions(customer.id!);
    const totalCredit = transactions
      .filter(t => t.type === 'credit')
      .reduce((sum, t) => sum + t.amount, 0);
    const totalPayment = transactions
      .filter(t => t.type === 'payment')
      .reduce((sum, t) => sum + t.amount, 0);
    const balance = totalCredit - totalPayment;

    if (balance !== 0 || transactions.length > 0) {
      balances.push({
        customerId: customer.id!,
        customerName: customer.name,
        customerPhone: customer.phone,
        customerPhoto: customer.photoPath,
        totalCredit,
        totalPayment,
        balance,
        lastTransactionDate: transactions[0]?.date
      });
    }
  }

  return balances.sort((a, b) => b.balance - a.balance);
};

export const getTotalOutstanding = async (): Promise<number> => {
  const balances = await getAllCustomerBalances();
  return balances.reduce((sum, b) => sum + b.balance, 0);
};

// Reports
export const getDailySummaries = async (days: number = 30): Promise<DailySummary[]> => {
  const now = Date.now();
  const startDate = now - (days * 24 * 60 * 60 * 1000);

  const transactions = await db.transactions
    .where('date')
    .above(startDate)
    .toArray();

  const summaryMap = new Map<string, DailySummary>();

  transactions.forEach(txn => {
    const dateKey = new Date(txn.date).toLocaleDateString('en-CA');
    const existing = summaryMap.get(dateKey) || {
      date: dateKey,
      totalCredit: 0,
      totalPayment: 0,
      transactionCount: 0
    };

    if (txn.type === 'credit') {
      existing.totalCredit += txn.amount;
    } else {
      existing.totalPayment += txn.amount;
    }
    existing.transactionCount++;

    summaryMap.set(dateKey, existing);
  });

  return Array.from(summaryMap.values()).sort((a, b) =>
    a.date.localeCompare(b.date)
  );
};

// Image helpers
export const saveCustomerPhoto = async (customerId: number, dataUrl: string): Promise<string> => {
  const photoPath = `customer_${customerId}_${Date.now()}`;
  localStorage.setItem(photoPath, dataUrl);
  await updateCustomer(customerId, { photoPath });
  return photoPath;
};

export const getCustomerPhoto = (photoPath?: string): string | null => {
  if (!photoPath) return null;
  return localStorage.getItem(photoPath);
};
