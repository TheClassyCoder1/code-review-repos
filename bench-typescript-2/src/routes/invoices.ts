import { Router } from 'express';
import { asyncHandler } from '../middleware/asyncHandler';
import { findInvoice, getInvoiceWithLines, listInvoicesForAccount } from '../repositories/invoiceRepo';
import { createCustomer } from '../services/customerService';
import {
  addAdjustment,
  getInvoiceStats,
  previewAdjustedTotal,
  refundInvoice,
  voidInvoice,
} from '../services/invoiceService';
import { chargeInvoice } from '../services/paymentGateway';
import { Invoice } from '../types';
import { adjustmentSchema, parsePagination } from '../validators';

const STATUSES: ReadonlyArray<Invoice['status']> = ['draft', 'open', 'paid', 'void', 'refunded'];

export const invoiceRouter = Router();

invoiceRouter.get(
  '/',
  asyncHandler(async (req, res) => {
    const { limit, offset } = parsePagination(req.query);
    const status = STATUSES.find((s) => s === req.query.status);
    res.json({ data: await listInvoicesForAccount(req.account!.id, status, limit, offset), limit, offset });
  }),
);

invoiceRouter.get('/stats', asyncHandler(async (req, res) => res.json(await getInvoiceStats(req.account!.id))));

invoiceRouter.get('/:invoiceId', asyncHandler(async (req, res) => res.json(await getInvoiceWithLines(req.account!.id, req.params.invoiceId))));

invoiceRouter.post(
  '/:invoiceId/pay',
  asyncHandler(async (req, res) => {
    const invoice = await findInvoice(req.account!.id, req.params.invoiceId);
    if (!invoice || invoice.status !== 'open') {
      res.status(409).json({ error: 'invoice is not payable' });
      return;
    }
    res.status(202).json(await chargeInvoice(invoice, String(req.body.paymentMethodId)));
  }),
);

invoiceRouter.post('/:invoiceId/void', asyncHandler(async (req, res) => res.json(await voidInvoice(req.account!.id, req.params.invoiceId))));

invoiceRouter.post('/:invoiceId/refund', asyncHandler(async (req, res) => res.json(await refundInvoice(req.account!.id, req.params.invoiceId))));

invoiceRouter.post(
  '/:invoiceId/adjustments',
  asyncHandler(async (req, res) => {
    const invoice = await findInvoice(req.account!.id, req.params.invoiceId);
    if (!invoice || invoice.status !== 'draft') {
      res.status(409).json({ error: 'only draft invoices can be adjusted' });
      return;
    }
    const adjustments = adjustmentSchema.array().min(1).max(20).parse(req.body.adjustments);
    if (req.query.preview === 'true') {
      res.json({ totalCents: previewAdjustedTotal(invoice, adjustments) });
      return;
    }
    for (const adjustment of adjustments) {
      await addAdjustment(invoice.id, adjustment.reason, adjustment.amount);
    }
    res.status(201).json({ added: adjustments.length });
  }),
);

export const customerRouter = Router();

customerRouter.post('/', asyncHandler(async (req, res) => res.status(201).json(await createCustomer(req.account!.id, req.body))));
