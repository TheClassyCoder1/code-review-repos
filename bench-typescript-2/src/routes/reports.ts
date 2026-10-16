import { Router } from 'express';
import { asyncHandler } from '../middleware/asyncHandler';
import { acquireReportClient, monthlyRevenue } from '../repositories/reportRepo';

export const reportRouter = Router();

reportRouter.get(
  '/revenue',
  asyncHandler(async (req, res) => {
    const months = Number(req.query.months ?? 12);
    const client = await acquireReportClient();
    try {
      const rows = await monthlyRevenue(client, req.account!.id, Number.isInteger(months) && months > 0 ? months : 12);
      res.json({ data: rows });
    } finally {
      client.release(true);
    }
  }),
);
