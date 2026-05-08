import { db } from './database';
import { addCustomer, addTransaction } from './db-operations';

export const seedDummyData = async () => {
  // Check if already seeded
  const existingCustomers = await db.customers.count();
  if (existingCustomers > 0) {
    console.log('Database already has data, skipping seed');
    return;
  }

  console.log('Seeding dummy data...');

  // Add sample customers
  const customers = [
    { name: 'Ramesh Kumar', phone: '9876543210' },
    { name: 'Lakshmi Devi', phone: '9876543211' },
    { name: 'Suresh Naik', phone: '9876543212' },
    { name: 'Manjula Bai', phone: '9876543213' },
    { name: 'Prakash Gowda', phone: '' },
  ];

  const customerIds: number[] = [];
  for (const customer of customers) {
    const id = await addCustomer(customer);
    customerIds.push(id);
  }

  // Add sample transactions
  const now = Date.now();
  const oneDay = 24 * 60 * 60 * 1000;

  const transactions = [
    // Ramesh - has outstanding
    { customerId: customerIds[0], type: 'credit' as const, amount: 500, date: now - 5 * oneDay },
    { customerId: customerIds[0], type: 'credit' as const, amount: 300, date: now - 3 * oneDay },
    { customerId: customerIds[0], type: 'payment' as const, amount: 200, date: now - 1 * oneDay },

    // Lakshmi - cleared
    { customerId: customerIds[1], type: 'credit' as const, amount: 1000, date: now - 7 * oneDay },
    { customerId: customerIds[1], type: 'payment' as const, amount: 1000, date: now - 2 * oneDay },

    // Suresh - high outstanding
    { customerId: customerIds[2], type: 'credit' as const, amount: 800, date: now - 10 * oneDay },
    { customerId: customerIds[2], type: 'credit' as const, amount: 400, date: now - 6 * oneDay },
    { customerId: customerIds[2], type: 'credit' as const, amount: 200, date: now - 2 * oneDay },
    { customerId: customerIds[2], type: 'payment' as const, amount: 300, date: now - 1 * oneDay },

    // Manjula - recent activity
    { customerId: customerIds[3], type: 'credit' as const, amount: 250, date: now },
    { customerId: customerIds[3], type: 'credit' as const, amount: 150, date: now - 1 * oneDay },

    // Prakash - paid in advance
    { customerId: customerIds[4], type: 'payment' as const, amount: 500, date: now - 4 * oneDay },
    { customerId: customerIds[4], type: 'credit' as const, amount: 300, date: now - 2 * oneDay },
  ];

  for (const txn of transactions) {
    await addTransaction(txn);
  }

  console.log('Dummy data seeded successfully!');
};
